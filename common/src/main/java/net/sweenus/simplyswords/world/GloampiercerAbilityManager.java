package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.GloampiercerCloneVisualEntity;
import net.sweenus.simplyswords.entity.GloampiercerSpearEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GloampiercerAbilityManager {
    private static final double GOLDEN_ANGLE = 2.399963229728653;
    private static final int FIRE_START_TICK = 12;
    private static final int FIRE_END_MARGIN = 12;
    private static final DustColorTransitionParticleEffect GLOAM_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.025F, 0.008F, 0.07F),
                    new Vector3f(0.14F, 0.94F, 0.96F), 1.35F);
    private static final Map<ServerWorld, Map<UUID, ActiveChannel>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, List<PendingPassiveStrike>> PASSIVE_STRIKES = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> LAST_PASSIVE = new HashMap<>();

    private GloampiercerAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && context.stack().isOf(ItemsRegistry.GLOAMPIERCER.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !isActive(context.actor())
                && resolveCenter(context) != null;
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        Vec3d center = resolveCenter(context);
        if (center == null) {
            return false;
        }
        ServerWorld world = context.world();
        LivingEntity owner = context.actor();
        int duration = MathHelper.clamp(Config.uniqueEffects.gloampiercer.channelDuration, 20, 120);
        int cloneCount = MathHelper.clamp(Config.uniqueEffects.gloampiercer.cloneCount, 1, 8);
        double lift = findLiftHeight(world, owner, Math.max(0.0, Config.uniqueEffects.gloampiercer.liftHeight));
        ActiveChannel channel = new ActiveChannel(owner.getUuid(), context.stack().copy(), context.hand(),
                owner.getPos(), center, owner.getY() + lift, world.getTime(), duration,
                Math.max(1.0F, (float) HelperMethods.getEntityAttackDamage(owner)));
        spawnActiveClones(world, owner, channel, cloneCount);
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(owner.getUuid(), channel);
        spawnActivationEffects(world, owner, center);
        return true;
    }

    public static void onSwing(ItemStack stack, ServerWorld world, LivingEntity owner) {
        if (stack == null || !stack.isOf(ItemsRegistry.GLOAMPIERCER.get())
                || owner == null || !owner.isAlive() || isActive(owner)) {
            return;
        }
        long now = world.getTime();
        Map<UUID, Long> cooldowns = LAST_PASSIVE.computeIfAbsent(world, ignored -> new HashMap<>());
        Long previous = cooldowns.get(owner.getUuid());
        if (previous != null && now - previous < Math.max(1, Config.uniqueEffects.gloampiercer.passiveCooldown)) {
            return;
        }
        LivingEntity target = findPassiveTarget(world, owner);
        if (target == null) {
            return;
        }
        int seed = owner.getRandom().nextInt();
        Vec3d clonePosition = passiveClonePosition(owner, target, seed);
        float yaw = yawToward(clonePosition, target.getPos());
        int throwTick = 9;
        GloampiercerCloneVisualEntity clone = new GloampiercerCloneVisualEntity(
                world, clonePosition.x, clonePosition.y, clonePosition.z, yaw, 22, throwTick, 0, seed);
        world.spawnEntity(clone);
        PASSIVE_STRIKES.computeIfAbsent(world, ignored -> new ArrayList<>())
                .add(new PendingPassiveStrike(owner.getUuid(), target.getUuid(), clone.getUuid(),
                        stack.copy(), cloneHandOrigin(clonePosition, target.getPos()), now + throwTick,
                        Math.max(1.0F, (float) HelperMethods.getEntityAttackDamage(owner))));
        cooldowns.put(owner.getUuid(), now);
        spawnCloneMaterialization(world, clonePosition);
    }

    public static boolean isActive(LivingEntity owner) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, ActiveChannel> channels = ACTIVE.get(world);
        return channels != null && channels.containsKey(owner.getUuid());
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveChannel> channels = ACTIVE.get(world);
        List<PendingPassiveStrike> strikes = PASSIVE_STRIKES.get(world);
        Map<UUID, Long> cooldowns = LAST_PASSIVE.get(world);
        return channels != null && !channels.isEmpty()
                || strikes != null && !strikes.isEmpty()
                || cooldowns != null && !cooldowns.isEmpty();
    }

    public static void tick(ServerWorld world) {
        tickChannels(world);
        tickPassiveStrikes(world);
        if (world.getTime() % 200L == 0L) {
            Map<UUID, Long> cooldowns = LAST_PASSIVE.get(world);
            if (cooldowns != null) {
                long now = world.getTime();
                cooldowns.values().removeIf(tick -> now - tick > 1200L);
                if (cooldowns.isEmpty()) {
                    LAST_PASSIVE.remove(world);
                }
            }
        }
    }

    private static void tickChannels(ServerWorld world) {
        Map<UUID, ActiveChannel> channels = ACTIVE.get(world);
        if (channels == null || channels.isEmpty()) {
            return;
        }
        Iterator<ActiveChannel> iterator = channels.values().iterator();
        while (iterator.hasNext()) {
            ActiveChannel channel = iterator.next();
            Entity entity = world.getEntity(channel.ownerId);
            if (!(entity instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || !isStillWielding(owner, channel)) {
                cancel(world, channel);
                iterator.remove();
                continue;
            }
            long age = world.getTime() - channel.startedAt;
            guideOwner(owner, channel, age);
            fireScheduledSpears(world, owner, channel, age);
            if (age >= channel.duration) {
                owner.setVelocity(owner.getVelocity().x, Math.min(owner.getVelocity().y, -0.04), owner.getVelocity().z);
                owner.velocityModified = true;
                iterator.remove();
            }
        }
        if (channels.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static void tickPassiveStrikes(ServerWorld world) {
        List<PendingPassiveStrike> strikes = PASSIVE_STRIKES.get(world);
        if (strikes == null || strikes.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<PendingPassiveStrike> iterator = strikes.iterator();
        while (iterator.hasNext()) {
            PendingPassiveStrike strike = iterator.next();
            if (now < strike.triggerAt) {
                continue;
            }
            Entity ownerEntity = world.getEntity(strike.ownerId);
            Entity targetEntity = world.getEntity(strike.targetId);
            if (ownerEntity instanceof LivingEntity owner && owner.isAlive() && !owner.isRemoved()
                    && targetEntity instanceof LivingEntity target && target.isAlive() && !target.isRemoved()
                    && HelperMethods.checkAbilityTarget(target, owner)) {
                launchSpear(world, owner, strike.stack, strike.origin,
                        target.getPos().add(0.0, target.getHeight() * 0.55, 0.0), target, strike.damage);
                world.playSound(null, strike.origin.x, strike.origin.y, strike.origin.z,
                        SoundRegistry.DARK_SWORD_WHOOSH_02.get(), SoundCategory.PLAYERS,
                        0.56F, 1.35F + world.random.nextFloat() * 0.12F);
            } else {
                discard(world, strike.cloneId);
            }
            iterator.remove();
        }
        if (strikes.isEmpty()) {
            PASSIVE_STRIKES.remove(world);
        }
    }

    private static void guideOwner(LivingEntity owner, ActiveChannel channel, long age) {
        int liftTicks = Math.min(10, Math.max(4, channel.duration / 4));
        int releaseTick = Math.max(liftTicks, channel.duration - 8);
        double retention = MathHelper.clamp(Config.uniqueEffects.gloampiercer.movementRetention, 0.0, 1.0);
        Vec3d velocity = owner.getVelocity();
        if (age < releaseTick) {
            double targetY;
            if (age < liftTicks) {
                double progress = MathHelper.clamp((double) (age + 1) / liftTicks, 0.0, 1.0);
                double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
                targetY = MathHelper.lerp(eased, channel.start.y, channel.hoverY);
            } else {
                targetY = channel.hoverY + Math.sin((age - liftTicks) * 0.22) * 0.05;
            }
            owner.setVelocity(velocity.x * retention,
                    MathHelper.clamp((targetY - owner.getY()) * 0.36, -0.18, 0.46),
                    velocity.z * retention);
            owner.fallDistance = 0.0F;
            owner.velocityModified = true;
        } else {
            owner.setVelocity(velocity.x * retention, Math.min(velocity.y, -0.03), velocity.z * retention);
            owner.velocityModified = true;
        }
    }

    private static void fireScheduledSpears(ServerWorld world, LivingEntity owner,
                                             ActiveChannel channel, long age) {
        int count = MathHelper.clamp(Config.uniqueEffects.gloampiercer.spearCount, 3, 36);
        int endTick = Math.max(FIRE_START_TICK + 1, channel.duration - FIRE_END_MARGIN);
        if (age < FIRE_START_TICK) {
            return;
        }
        double progress = MathHelper.clamp((double) (age - FIRE_START_TICK + 1)
                / Math.max(1, endTick - FIRE_START_TICK), 0.0, 1.0);
        int expected = Math.min(count, (int) Math.floor(progress * count));
        while (channel.fired < expected) {
            fireSpear(world, owner, channel, channel.fired, count);
            channel.fired++;
        }
    }

    private static void fireSpear(ServerWorld world, LivingEntity owner,
                                  ActiveChannel channel, int index, int count) {
        int sourceCount = channel.clonePositions.size() + 1;
        int sourceIndex = Math.floorMod(index, sourceCount);
        Vec3d origin = sourceIndex == 0
                ? owner.getPos().add(0.0, owner.getHeight() * 0.68, 0.0)
                : cloneHandOrigin(channel.clonePositions.get(sourceIndex - 1), channel.center);
        LivingEntity target = index % 3 == 2 ? null : selectBarrageTarget(world, owner, channel, index);
        Vec3d destination = target == null
                ? groundStrikePosition(world, channel.center, index, count)
                : target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        launchSpear(world, owner, channel.stack, origin, destination, target, channel.damage);
        if (sourceIndex == 0) {
            Hand hand = channel.hand == null ? Hand.MAIN_HAND : channel.hand;
            RunicSlashManager.runSuppressed(() -> owner.swingHand(hand, true));
        }
        world.spawnParticles(GLOAM_DUST, origin.x, origin.y, origin.z,
                7, 0.12, 0.12, 0.12, 0.025);
        world.playSound(null, origin.x, origin.y, origin.z, SoundRegistry.DARK_SWORD_WHOOSH_01.get(),
                SoundCategory.PLAYERS, 0.4F, 1.25F + world.random.nextFloat() * 0.18F);
    }

    private static void launchSpear(ServerWorld world, LivingEntity owner, ItemStack stack,
                                    Vec3d origin, Vec3d destination, LivingEntity target, float damage) {
        GloampiercerSpearEntity spear = new GloampiercerSpearEntity(world, owner, stack,
                origin, destination, target, damage,
                Math.max(0.1, Config.uniqueEffects.gloampiercer.projectileSpeed));
        world.spawnEntity(spear);
    }

    private static LivingEntity selectBarrageTarget(ServerWorld world, LivingEntity owner,
                                                     ActiveChannel channel, int index) {
        double radius = Math.max(1.0, Config.uniqueEffects.gloampiercer.barrageRadius);
        Box box = Box.of(channel.center, radius * 2.0, 8.0, radius * 2.0);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box,
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                                && HelperMethods.checkAbilityTarget(entity, owner)
                                && horizontalDistanceSquared(entity.getPos(), channel.center) <= radius * radius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(channel.center)))
                .toList();
        return targets.isEmpty() ? null : targets.get(Math.floorMod(index / 3 + index, targets.size()));
    }

    private static Vec3d groundStrikePosition(ServerWorld world, Vec3d center, int index, int count) {
        double radius = Math.max(1.0, Config.uniqueEffects.gloampiercer.barrageRadius);
        double fraction = Math.sqrt((index + 0.5) / Math.max(1, count));
        double angle = index * GOLDEN_ANGLE + Math.floorMod(index * 31, 17) * 0.037;
        double distance = radius * fraction * (0.78 + Math.floorMod(index * 13, 19) / 90.0);
        double x = center.x + Math.cos(angle) * distance;
        double z = center.z + Math.sin(angle) * distance;
        double y = LivyatanWaveManager.findGroundTopY(world, x, z, center.y + 4.0);
        return new Vec3d(x, y + 0.05, z);
    }

    private static LivingEntity findPassiveTarget(ServerWorld world, LivingEntity owner) {
        double minimum = Math.max(0.0, Config.uniqueEffects.gloampiercer.passiveMinRange);
        double maximum = Math.max(minimum + 0.1, Config.uniqueEffects.gloampiercer.passiveMaxRange);
        double minimumSquared = minimum * minimum;
        double maximumSquared = maximum * maximum;
        double threshold = Math.cos(Math.toRadians(
                MathHelper.clamp(Config.uniqueEffects.gloampiercer.passiveConeDegrees, 1.0, 180.0) * 0.5));
        Vec3d look = owner.getRotationVec(1.0F).normalize();
        return world.getEntitiesByClass(LivingEntity.class, owner.getBoundingBox().expand(maximum),
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                                && HelperMethods.checkAbilityTarget(entity, owner)
                                && entity.squaredDistanceTo(owner) >= minimumSquared
                                && entity.squaredDistanceTo(owner) <= maximumSquared
                                && owner.canSee(entity))
                .stream()
                .filter(entity -> directionTo(owner, entity).dotProduct(look) >= threshold)
                .min(Comparator.comparingDouble(entity -> {
                    double alignment = directionTo(owner, entity).dotProduct(look);
                    return (1.0 - alignment) * 100.0 + owner.squaredDistanceTo(entity) * 0.02;
                }))
                .orElse(null);
    }

    private static Vec3d directionTo(LivingEntity owner, LivingEntity target) {
        Vec3d offset = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0).subtract(owner.getEyePos());
        return offset.lengthSquared() < 1.0E-6 ? owner.getRotationVec(1.0F) : offset.normalize();
    }

    private static Vec3d passiveClonePosition(LivingEntity owner, LivingEntity target, int seed) {
        Vec3d forward = target.getPos().subtract(owner.getPos());
        forward = forward.horizontalLengthSquared() < 1.0E-6
                ? Vec3d.fromPolar(0.0F, owner.getYaw())
                : new Vec3d(forward.x, 0.0, forward.z).normalize();
        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);
        double sign = (seed & 1) == 0 ? 1.0 : -1.0;
        double lateral = 1.8 + Math.floorMod(seed, 9) * 0.1;
        double backward = 0.6 + Math.floorMod(seed >>> 4, 7) * 0.08;
        double height = target.getHeight() + 1.0 + Math.floorMod(seed >>> 8, 8) * 0.1;
        return target.getPos().add(side.multiply(sign * lateral)).subtract(forward.multiply(backward)).add(0.0, height, 0.0);
    }

    private static void spawnActiveClones(ServerWorld world, LivingEntity owner,
                                          ActiveChannel channel, int cloneCount) {
        Vec3d facing = channel.center.subtract(channel.start);
        facing = facing.horizontalLengthSquared() < 1.0E-6
                ? Vec3d.fromPolar(0.0F, owner.getYaw())
                : new Vec3d(facing.x, 0.0, facing.z).normalize();
        Vec3d side = new Vec3d(-facing.z, 0.0, facing.x);
        for (int index = 0; index < cloneCount; index++) {
            double centered = index - (cloneCount - 1) * 0.5;
            double lateral = centered * 1.45;
            double depth = 0.9 + Math.abs(centered) * 0.28 + (index % 2) * 0.55;
            double height = index % 3 == 1 ? 0.65 : index % 3 == 2 ? 1.15 : 0.15;
            Vec3d position = channel.start.add(side.multiply(lateral)).subtract(facing.multiply(depth))
                    .add(0.0, height, 0.0);
            channel.clonePositions.add(position);
            int seed = owner.getRandom().nextInt();
            int spearCount = MathHelper.clamp(Config.uniqueEffects.gloampiercer.spearCount, 3, 36);
            int sourceCount = cloneCount + 1;
            int firstSpear = index + 1;
            int firstThrow = firstSpear < spearCount
                    ? scheduledFireTick(firstSpear, spearCount, channel.duration)
                    : channel.duration + 8;
            int nextSpear = firstSpear + sourceCount;
            int throwInterval = nextSpear < spearCount
                    ? scheduledFireTick(nextSpear, spearCount, channel.duration) - firstThrow
                    : 0;
            GloampiercerCloneVisualEntity clone = new GloampiercerCloneVisualEntity(
                    world, position.x, position.y, position.z, yawToward(position, channel.center),
                    channel.duration + 4, firstThrow, throwInterval, seed);
            world.spawnEntity(clone);
            channel.cloneIds.add(clone.getUuid());
            spawnCloneMaterialization(world, position);
        }
    }

    private static int scheduledFireTick(int spearIndex, int spearCount, int duration) {
        int endTick = Math.max(FIRE_START_TICK + 1, duration - FIRE_END_MARGIN);
        int span = Math.max(1, endTick - FIRE_START_TICK);
        return FIRE_START_TICK - 1
                + (int) Math.ceil((spearIndex + 1) * span / (double) Math.max(1, spearCount));
    }

    private static Vec3d cloneHandOrigin(Vec3d clonePosition, Vec3d targetPosition) {
        Vec3d forward = targetPosition.subtract(clonePosition);
        forward = forward.horizontalLengthSquared() < 1.0E-6
                ? new Vec3d(0.0, 0.0, 1.0)
                : new Vec3d(forward.x, 0.0, forward.z).normalize();
        return clonePosition.add(forward.multiply(0.56)).add(0.0, 1.36, 0.0);
    }

    private static double findLiftHeight(ServerWorld world, LivingEntity owner, double requested) {
        if (requested <= 0.0) {
            return 0.0;
        }
        double available = 0.0;
        Box box = owner.getBoundingBox();
        for (double offset = 0.25; offset <= requested + 1.0E-4; offset += 0.25) {
            if (!world.isSpaceEmpty(owner, box.offset(0.0, offset, 0.0))) {
                break;
            }
            available = offset;
        }
        return Math.min(requested, available);
    }

    private static Vec3d resolveCenter(WeaponAbilityContext context) {
        LivingEntity owner = context.actor();
        double range = Math.max(1.0, Config.uniqueEffects.gloampiercer.castRange);
        if (context.target() != null && context.target().isAlive()
                && context.target().squaredDistanceTo(owner) <= range * range
                && HelperMethods.checkAbilityTarget(context.target(), owner)) {
            LivingEntity target = context.target();
            return new Vec3d(target.getX(),
                    LivyatanWaveManager.findGroundTopY(context.world(), target.getX(), target.getZ(), target.getY() + 3.0),
                    target.getZ());
        }
        Vec3d direction = context.facing().lengthSquared() < 1.0E-6
                ? owner.getRotationVec(1.0F) : context.facing().normalize();
        Vec3d start = owner.getEyePos();
        Vec3d end = start.add(direction.multiply(range));
        BlockHitResult hit = context.world().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
        Vec3d point = hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
        return new Vec3d(point.x,
                LivyatanWaveManager.findGroundTopY(context.world(), point.x, point.z, point.y + 4.0),
                point.z);
    }

    private static boolean isStillWielding(LivingEntity owner, ActiveChannel channel) {
        if (channel.hand != null) {
            return owner.getStackInHand(channel.hand).isOf(ItemsRegistry.GLOAMPIERCER.get());
        }
        return owner.getMainHandStack().isOf(ItemsRegistry.GLOAMPIERCER.get())
                || owner.getOffHandStack().isOf(ItemsRegistry.GLOAMPIERCER.get());
    }

    private static void cancel(ServerWorld world, ActiveChannel channel) {
        for (UUID cloneId : channel.cloneIds) {
            discard(world, cloneId);
        }
    }

    private static void discard(ServerWorld world, UUID entityId) {
        Entity entity = entityId == null ? null : world.getEntity(entityId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity owner, Vec3d center) {
        Vec3d chest = owner.getPos().add(0.0, owner.getHeight() * 0.62, 0.0);
        world.spawnParticles(GLOAM_DUST, chest.x, chest.y, chest.z,
                38, 0.7, 0.9, 0.7, 0.05);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, chest.x, chest.y, chest.z,
                10, 0.45, 0.65, 0.45, 0.025);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.15, center.z,
                34, 1.8, 0.12, 1.8, 0.1);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                SoundCategory.PLAYERS, 0.88F, 0.68F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.78F, 0.9F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR,
                SoundCategory.PLAYERS, 0.62F, 0.72F);
    }

    private static void spawnCloneMaterialization(ServerWorld world, Vec3d position) {
        world.spawnParticles(GLOAM_DUST, position.x, position.y + 0.9, position.z,
                18, 0.28, 0.65, 0.28, 0.035);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, position.x, position.y + 0.8, position.z,
                9, 0.2, 0.55, 0.2, 0.05);
        world.playSound(null, position.x, position.y, position.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.25F, 1.48F + world.random.nextFloat() * 0.12F);
    }

    private static float yawToward(Vec3d from, Vec3d to) {
        Vec3d direction = to.subtract(from);
        return (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
    }

    private static double horizontalDistanceSquared(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private static final class ActiveChannel {
        private final UUID ownerId;
        private final ItemStack stack;
        private final Hand hand;
        private final Vec3d start;
        private final Vec3d center;
        private final double hoverY;
        private final long startedAt;
        private final int duration;
        private final float damage;
        private final List<Vec3d> clonePositions = new ArrayList<>();
        private final List<UUID> cloneIds = new ArrayList<>();
        private int fired;

        private ActiveChannel(UUID ownerId, ItemStack stack, Hand hand, Vec3d start, Vec3d center,
                              double hoverY, long startedAt, int duration, float damage) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.hand = hand;
            this.start = start;
            this.center = center;
            this.hoverY = hoverY;
            this.startedAt = startedAt;
            this.duration = duration;
            this.damage = damage;
        }
    }

    private record PendingPassiveStrike(UUID ownerId, UUID targetId, UUID cloneId, ItemStack stack,
                                        Vec3d origin, long triggerAt, float damage) {
    }
}
