package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulstalkerCleaveEntity;
import net.sweenus.simplyswords.entity.SoulstalkerStrideEntity;
import net.sweenus.simplyswords.entity.SoulstalkerTentacleVisualEntity;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SoulstalkerAbilityManager {
    private static final int PASSIVE_IMPACT_DELAY = 8;
    private static final int PASSIVE_VISUAL_LIFETIME = 18;
    private static final int TRAIL_INTERVAL = 5;
    private static final String STRIDE_TAG = "simplyswords_soulstalker_stride";
    private static final DustColorTransitionParticleEffect GLOAM_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.025F, 0.008F, 0.07F),
                    new Vector3f(0.48F, 0.08F, 0.82F), 1.35F);
    private static final Map<ServerWorld, Map<UUID, ActiveStride>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, List<PendingStrike>> PENDING_STRIKES = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> LAST_PASSIVE_CHECK = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> PASSIVE_LOCKOUT = new HashMap<>();
    private static final Map<UUID, Long> LAST_CLEAVE = new HashMap<>();

    private SoulstalkerAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null || context.world() == null || context.actor() == null
                || !context.actor().isAlive() || context.actor().hasVehicle()
                || context.stack() == null || !context.stack().isOf(ItemsRegistry.SOULSTALKER.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || isActive(context.world(), context.actor().getUuid())) {
            return false;
        }
        Hand hand = context.hand() == null ? Hand.MAIN_HAND : context.hand();
        return context.actor().getStackInHand(hand) == context.stack()
                || context.actor().getStackInHand(hand).equals(context.stack());
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        ServerWorld world = context.world();
        LivingEntity owner = context.actor();
        Hand hand = context.hand() == null ? Hand.MAIN_HAND : context.hand();
        ItemStack heldStack = owner.getStackInHand(hand);
        SoulstalkerStrideEntity stride = new SoulstalkerStrideEntity(EntityRegistry.SOULSTALKER_STRIDE.get(), world);
        stride.setOwner(owner);
        stride.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), owner.getYaw(), 0.0F);
        EntityAttributeInstance movement = stride.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        EntityAttributeInstance step = stride.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (movement != null) {
            movement.setBaseValue(MathHelper.clamp(Config.uniqueEffects.soulstalker.movementSpeed, 0.05, 1.0));
        }
        if (step != null) {
            step.setBaseValue(MathHelper.clamp(Config.uniqueEffects.soulstalker.stepHeight, 0.5, 4.0));
        }
        if (!world.isSpaceEmpty(owner, stride.getBoundingBox())) {
            return false;
        }
        stride.addCommandTag(STRIDE_TAG);
        if (!world.spawnEntity(stride) || !owner.startRiding(stride, true)) {
            stride.discard();
            return false;
        }
        long now = world.getTime();
        ActiveStride active = new ActiveStride(owner.getUuid(), stride.getUuid(), hand,
                heldStack, heldStack.copy(), now,
                now + Math.max(20, Config.uniqueEffects.soulstalker.duration),
                context.activationSource() == WeaponAbilityActivationSource.MOB ? now : Long.MIN_VALUE,
                owner.getPos());
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(owner.getUuid(), active);
        spawnActivation(world, owner);
        return true;
    }

    public static void onSwing(ItemStack stack, ServerWorld world, LivingEntity owner, Hand hand) {
        if (stack == null || !stack.isOf(ItemsRegistry.SOULSTALKER.get()) || owner == null || !owner.isAlive()) {
            return;
        }
        ActiveStride active = ACTIVE.getOrDefault(world, Map.of()).get(owner.getUuid());
        if (active == null || active.hand != hand || active.stackReference != stack
                || active.suppressedSwingTick == world.getTime() || active.lastCleaveTick == world.getTime()) {
            return;
        }
        SoulstalkerStrideEntity stride = resolveStride(world, active.strideId);
        if (stride == null || owner.getVehicle() != stride) {
            return;
        }
        if (!isCleaveReady(world, owner)) {
            return;
        }
        Vec3d direction = owner.getRotationVec(1.0F);
        if (direction.lengthSquared() < 1.0E-6) {
            direction = Vec3d.fromPolar(owner.getPitch(), owner.getYaw());
        }
        direction = direction.normalize();
        Vec3d origin = owner.getPos().add(0.0, owner.getHeight() * 0.56, 0.0)
                .add(direction.multiply(0.72));
        SoulstalkerCleaveEntity cleave = new SoulstalkerCleaveEntity(
                world, owner, active.stackSnapshot, origin, direction,
                Math.max(1.0, Config.uniqueEffects.soulstalker.cleaveRange),
                Math.max(0.05, Config.uniqueEffects.soulstalker.cleaveSpeed),
                Math.max(1.0F, (float) HelperMethods.getEntityAttackDamage(owner)),
                (float) Math.max(0.25, Config.uniqueEffects.soulstalker.cleaveInitialWidth),
                (float) Math.max(0.25, Config.uniqueEffects.soulstalker.cleaveFinalWidth));
        world.spawnEntity(cleave);
        active.lastCleaveTick = world.getTime();
        world.spawnParticles(GLOAM_DUST, origin.x, origin.y, origin.z,
                14, 0.28, 0.24, 0.28, 0.035);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, origin.x, origin.y, origin.z,
                1, 0.0, 0.0, 0.0, 0.0);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_SWORD_WHOOSH_03.get(),
                SoundCategory.PLAYERS, 0.8F, 0.68F + world.random.nextFloat() * 0.12F);
        world.playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE,
                SoundCategory.PLAYERS, 0.28F, 1.45F + world.random.nextFloat() * 0.1F);
    }

    private static boolean isCleaveReady(ServerWorld world, LivingEntity user) {
        long now = world.getTime();
        if (now % 200L == 0L) {
            LAST_CLEAVE.entrySet().removeIf(entry -> now - entry.getValue() > 1200L);
        }
        if (RunicSlashManager.isIgnoringAttackReady()) {
            LAST_CLEAVE.put(user.getUuid(), now);
            return true;
        }
        Long last = LAST_CLEAVE.get(user.getUuid());
        if (last != null && now - last < getCleaveCooldownTicks(user)) {
            return false;
        }
        LAST_CLEAVE.put(user.getUuid(), now);
        return true;
    }

    private static int getCleaveCooldownTicks(LivingEntity user) {
        EntityAttributeInstance attackSpeed = user.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        double value = attackSpeed == null ? 4.0 : attackSpeed.getValue();
        if (value <= 0.0) {
            value = 4.0;
        }
        return Math.max(Config.uniqueEffects.soulstalker.cleaveMinimumSwingCooldownTicks,
                (int) Math.ceil(20.0 / value));
    }

    public static void tickHeldPassive(LivingEntity owner, ItemStack stack) {
        if (owner == null || stack == null || !stack.isOf(ItemsRegistry.SOULSTALKER.get())
                || !owner.isAlive() || !AwakeningApi.isAbilityUnlocked(stack)
                || !(owner.getWorld() instanceof ServerWorld world)
                || owner.getMainHandStack() != stack && owner.getOffHandStack() != stack) {
            return;
        }
        long now = world.getTime();
        Map<UUID, Long> checks = LAST_PASSIVE_CHECK.computeIfAbsent(world, ignored -> new HashMap<>());
        Long previousCheck = checks.put(owner.getUuid(), now);
        if (previousCheck != null && previousCheck == now) {
            return;
        }
        int interval = Math.max(1, Config.uniqueEffects.soulstalker.passiveCheckInterval);
        if (Math.floorMod(now + owner.getId(), interval) != 0L) {
            return;
        }
        Map<UUID, Long> lockouts = PASSIVE_LOCKOUT.computeIfAbsent(world, ignored -> new HashMap<>());
        if (now < lockouts.getOrDefault(owner.getUuid(), Long.MIN_VALUE)) {
            return;
        }
        int chance = Math.clamp(Config.uniqueEffects.soulstalker.passiveChance, 0, 100);
        if (chance <= 0 || owner.getRandom().nextInt(100) >= chance) {
            return;
        }
        LivingEntity target = findPassiveTarget(world, owner);
        if (target == null) {
            return;
        }
        SoulstalkerTentacleVisualEntity visual = new SoulstalkerTentacleVisualEntity(
                world, owner, target, PASSIVE_IMPACT_DELAY, PASSIVE_VISUAL_LIFETIME);
        world.spawnEntity(visual);
        PENDING_STRIKES.computeIfAbsent(world, ignored -> new ArrayList<>())
                .add(new PendingStrike(owner.getUuid(), target.getUuid(), visual.getUuid(),
                        stack.copy(), Math.max(1.0F, (float) HelperMethods.getEntityAttackDamage(owner)),
                        now + PASSIVE_IMPACT_DELAY));
        lockouts.put(owner.getUuid(), now + Math.max(1, Config.uniqueEffects.soulstalker.passiveLockout));
        Vec3d root = owner.getPos().add(0.0, owner.getHeight() * 0.68, 0.0);
        world.spawnParticles(GLOAM_DUST, root.x, root.y, root.z,
                10, 0.18, 0.22, 0.18, 0.02);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.46F, 0.72F + world.random.nextFloat() * 0.1F);
    }

    public static boolean hasActive(ServerWorld world) {
        return !ACTIVE.getOrDefault(world, Map.of()).isEmpty()
                || !PENDING_STRIKES.getOrDefault(world, List.of()).isEmpty()
                || world.getTime() % 40L == 0L;
    }

    public static void tick(ServerWorld world) {
        tickStrides(world);
        tickPendingStrikes(world);
        if (world.getTime() % 40L == 0L) {
            purgeOrphans(world);
        }
        if (world.getTime() % 200L == 0L) {
            purgePassiveState(world);
        }
    }

    private static void tickStrides(ServerWorld world) {
        Map<UUID, ActiveStride> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<ActiveStride> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveStride state = iterator.next();
            LivingEntity owner = resolveLiving(world, state.ownerId);
            SoulstalkerStrideEntity stride = resolveStride(world, state.strideId);
            boolean valid = owner != null && stride != null && owner.getVehicle() == stride
                    && owner.getStackInHand(state.hand) == state.stackReference
                    && state.stackReference.isOf(ItemsRegistry.SOULSTALKER.get());
            if (!valid || now >= state.expiresAt) {
                finishStride(world, owner, stride);
                iterator.remove();
                continue;
            }
            if ((now - state.startedAt) % TRAIL_INTERVAL == 0L) {
                createTrailPatch(world, owner, stride, state);
            }
        }
        if (active.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static void tickPendingStrikes(ServerWorld world) {
        List<PendingStrike> pending = PENDING_STRIKES.get(world);
        if (pending == null || pending.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<PendingStrike> iterator = pending.iterator();
        while (iterator.hasNext()) {
            PendingStrike strike = iterator.next();
            if (now < strike.impactTick) {
                continue;
            }
            LivingEntity owner = resolveLiving(world, strike.ownerId);
            LivingEntity target = resolveLiving(world, strike.targetId);
            double range = Math.max(1.0, Config.uniqueEffects.soulstalker.passiveRange) + 2.0;
            if (owner != null && target != null
                    && HelperMethods.isHoldingItem(ItemsRegistry.SOULSTALKER.get(), owner)
                    && owner.squaredDistanceTo(target) <= range * range
                    && owner.canSee(target) && HelperMethods.checkAbilityTarget(target, owner)) {
                if (SimplySwordsAPI.applyEntityWeaponHit(strike.stack, target, owner, strike.damage)) {
                    createImpactStain(world, owner, target.getPos());
                    world.spawnParticles(GLOAM_DUST, target.getX(), target.getBodyY(0.55), target.getZ(),
                            22, 0.38, 0.34, 0.38, 0.055);
                    world.spawnParticles(ParticleTypes.SCULK_SOUL,
                            target.getX(), target.getBodyY(0.55), target.getZ(),
                            7, 0.26, 0.28, 0.26, 0.035);
                    world.playSound(null, target.getBlockPos(), SoundRegistry.DARK_SWORD_ATTACK_02.get(),
                            SoundCategory.PLAYERS, 0.72F, 0.62F + world.random.nextFloat() * 0.1F);
                    world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS,
                            SoundCategory.PLAYERS, 0.42F, 0.8F);
                }
            } else {
                discard(world, strike.visualId);
            }
            iterator.remove();
        }
        if (pending.isEmpty()) {
            PENDING_STRIKES.remove(world);
        }
    }

    private static LivingEntity findPassiveTarget(ServerWorld world, LivingEntity owner) {
        double range = Math.max(1.0, Config.uniqueEffects.soulstalker.passiveRange);
        Box search = owner.getBoundingBox().expand(range, Math.max(2.0, range * 0.65), range);
        return world.getEntitiesByClass(LivingEntity.class, search,
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                                && owner.squaredDistanceTo(entity) <= range * range
                                && owner.canSee(entity) && HelperMethods.checkAbilityTarget(entity, owner))
                .stream()
                .min(Comparator.comparingDouble(owner::squaredDistanceTo))
                .orElse(null);
    }

    private static void createTrailPatch(ServerWorld world, LivingEntity owner,
                                         SoulstalkerStrideEntity stride, ActiveStride state) {
        Vec3d movement = stride.getPos().subtract(state.lastTrailPoint);
        if (!stride.isGroundStrideSurface()) {
            state.lastTrailPoint = stride.getPos();
            return;
        }
        if (movement.lengthSquared() < 0.16) {
            return;
        }
        double y = LivyatanWaveManager.findGroundTopY(world,
                stride.getX(), stride.getZ(), stride.getY() + 1.5);
        Vec3d right = new Vec3d(Math.cos(Math.toRadians(stride.getYaw())), 0.0,
                Math.sin(Math.toRadians(stride.getYaw())));
        double side = ((world.getTime() / TRAIL_INTERVAL) & 1L) == 0L ? -0.45 : 0.45;
        Vec3d point = new Vec3d(stride.getX(), y, stride.getZ()).add(right.multiply(side));
        WraithmawStainManager.createPatch(world, owner.getUuid(), point,
                Math.max(0.25, Config.uniqueEffects.soulstalker.stainTrailWidth * 0.72),
                Math.max(20, Config.uniqueEffects.soulstalker.stainDuration),
                Math.max(1, Config.uniqueEffects.soulstalker.stainFadeDuration),
                Math.clamp(Config.uniqueEffects.soulstalker.stainSlowAmplifier, 0, 4));
        state.lastTrailPoint = stride.getPos();
        world.spawnParticles(GLOAM_DUST, point.x, point.y + 0.08, point.z,
                3, 0.2, 0.05, 0.2, 0.01);
    }

    private static void createImpactStain(ServerWorld world, LivingEntity owner, Vec3d position) {
        double y = LivyatanWaveManager.findGroundTopY(world, position.x, position.z, position.y + 1.5);
        WraithmawStainManager.createPatch(world, owner.getUuid(),
                new Vec3d(position.x, y, position.z),
                Math.max(0.25, Config.uniqueEffects.soulstalker.stainRadius),
                Math.max(20, Config.uniqueEffects.soulstalker.stainDuration),
                Math.max(1, Config.uniqueEffects.soulstalker.stainFadeDuration),
                Math.clamp(Config.uniqueEffects.soulstalker.stainSlowAmplifier, 0, 4));
    }

    private static void finishStride(ServerWorld world, LivingEntity owner, SoulstalkerStrideEntity stride) {
        Vec3d release = stride == null ? null : stride.getPos().add(0.0, 0.1, 0.0);
        if (owner != null && stride != null && owner.getVehicle() == stride) {
            owner.stopRiding();
            if (release != null) {
                Box targetBox = owner.getBoundingBox().offset(release.subtract(owner.getPos()));
                if (world.isSpaceEmpty(owner, targetBox)) {
                    owner.refreshPositionAfterTeleport(release);
                }
            }
            owner.fallDistance = 0.0F;
            owner.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOW_FALLING, 40, 0, false, false, false), owner);
            world.spawnParticles(GLOAM_DUST, owner.getX(), owner.getBodyY(0.5), owner.getZ(),
                    22, 0.65, 0.7, 0.65, 0.045);
            world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                    SoundCategory.PLAYERS, 0.55F, 1.35F);
        }
        if (stride != null) {
            stride.removeAllPassengers();
            stride.discard();
        }
    }

    private static void spawnActivation(ServerWorld world, LivingEntity owner) {
        Vec3d center = owner.getPos().add(0.0, 0.25, 0.0);
        world.spawnParticles(GLOAM_DUST, center.x, center.y, center.z,
                42, 0.85, 0.45, 0.85, 0.065);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                34, 0.8, 0.35, 0.8, 0.12);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.45, center.z,
                12, 0.55, 0.5, 0.55, 0.04);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                SoundCategory.PLAYERS, 0.95F, 0.62F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.9F, 0.72F);
        world.playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_WARDEN_EMERGE,
                SoundCategory.PLAYERS, 0.48F, 0.78F);
    }

    private static boolean isActive(ServerWorld world, UUID ownerId) {
        return ACTIVE.getOrDefault(world, Map.of()).containsKey(ownerId);
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                ? living : null;
    }

    private static SoulstalkerStrideEntity resolveStride(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof SoulstalkerStrideEntity stride && !stride.isRemoved() ? stride : null;
    }

    private static void discard(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void purgeOrphans(ServerWorld world) {
        Set<UUID> activeStrides = new HashSet<>();
        Map<UUID, ActiveStride> active = ACTIVE.get(world);
        if (active != null) {
            active.values().forEach(state -> activeStrides.add(state.strideId));
        }
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof SoulstalkerStrideEntity
                    && entity.getCommandTags().contains(STRIDE_TAG)
                    && !activeStrides.contains(entity.getUuid())) {
                entity.removeAllPassengers();
                entity.discard();
            }
        }
    }

    private static void purgePassiveState(ServerWorld world) {
        long now = world.getTime();
        Map<UUID, Long> checks = LAST_PASSIVE_CHECK.get(world);
        if (checks != null) {
            checks.entrySet().removeIf(entry -> entry.getValue() < now - 40L);
            if (checks.isEmpty()) {
                LAST_PASSIVE_CHECK.remove(world);
            }
        }
        Map<UUID, Long> lockouts = PASSIVE_LOCKOUT.get(world);
        if (lockouts != null) {
            lockouts.entrySet().removeIf(entry -> entry.getValue() < now - 1200L);
            if (lockouts.isEmpty()) {
                PASSIVE_LOCKOUT.remove(world);
            }
        }
    }

    private static final class ActiveStride {
        private final UUID ownerId;
        private final UUID strideId;
        private final Hand hand;
        private final ItemStack stackReference;
        private final ItemStack stackSnapshot;
        private final long startedAt;
        private final long expiresAt;
        private final long suppressedSwingTick;
        private Vec3d lastTrailPoint;
        private long lastCleaveTick = Long.MIN_VALUE;

        private ActiveStride(UUID ownerId, UUID strideId, Hand hand,
                             ItemStack stackReference, ItemStack stackSnapshot,
                             long startedAt, long expiresAt, long suppressedSwingTick,
                             Vec3d lastTrailPoint) {
            this.ownerId = ownerId;
            this.strideId = strideId;
            this.hand = hand;
            this.stackReference = stackReference;
            this.stackSnapshot = stackSnapshot;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
            this.suppressedSwingTick = suppressedSwingTick;
            this.lastTrailPoint = lastTrailPoint;
        }
    }

    private record PendingStrike(UUID ownerId, UUID targetId, UUID visualId,
                                 ItemStack stack, float damage, long impactTick) {
    }
}
