package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.entity.DawnquiverArrowEntity;
import net.sweenus.simplyswords.entity.DawnquiverBowVisualEntity;
import net.sweenus.simplyswords.item.custom.DawnquiverSwordItem;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DawnquiverAbilityManager {

    private static final DustColorTransitionParticleEffect DAWN_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(1.0F, 0.88F, 0.42F),
                    new Vector3f(1.0F, 1.0F, 0.92F), 1.15F);

    private static final int PRUNE_INTERVAL = 200;
    private static final int BOW_TRAILING_TICKS = 8;

    private static final Map<ServerWorld, Map<UUID, ActiveDraw>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> LAST_PASSIVE_TICK = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> PASSIVE_LOCKOUT = new HashMap<>();

    private DawnquiverAbilityManager() {
    }

    public static boolean startDraw(ServerWorld world, LivingEntity owner, ItemStack stack, Hand hand) {
        if (world == null || owner == null || stack == null || stack.isEmpty()
                || hand == null || !stack.isOf(ItemsRegistry.DAWNQUIVER.get())) {
            return false;
        }
        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        cancel(world, owner.getUuid());

        int seed = owner.getRandom().nextInt(4096);
        int lifetime = Math.max(20, settings.drawDuration) + BOW_TRAILING_TICKS;
        LivingEntity target = findActiveTarget(world, owner, settings);
        Vec3d aimDirection = activeAimDirection(owner, target);
        Vec3d anchor = bowAnchor(owner, aimDirection, settings.bowDistance);
        DawnquiverBowVisualEntity bow = new DawnquiverBowVisualEntity(world, owner, hand,
                anchor.x, anchor.y, anchor.z, directionYaw(aimDirection), directionPitch(aimDirection),
                lifetime, 1.0F, seed,
                DawnquiverBowVisualEntity.MODE_ACTIVE);
        bow.setTarget(target);
        if (!world.spawnEntity(bow)) {
            bow.discard();
            return false;
        }

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>())
                .put(owner.getUuid(), new ActiveDraw(bow.getUuid(), hand));

        world.spawnParticles(DAWN_DUST, anchor.x, anchor.y, anchor.z, 30, 0.5, 0.5, 0.5, 0.05);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_VOICE_15.get(),
                SoundCategory.PLAYERS, 0.45F, 1.1F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_CHARGE_LONG_VERSION.get(),
                SoundCategory.PLAYERS, 0.5F, 1.0F);
        return true;
    }

    public static void tickDraw(ServerWorld world, LivingEntity owner, float drawProgress) {
        DawnquiverBowVisualEntity bow = resolveBow(world, owner.getUuid());
        if (bow == null) {
            return;
        }
        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        LivingEntity target = findActiveTarget(world, owner, settings);
        Vec3d aimDirection = activeAimDirection(owner, target);
        Vec3d anchor = bowAnchor(owner, aimDirection, settings.bowDistance);
        bow.refreshPositionAndAngles(anchor.x, anchor.y, anchor.z,
                directionYaw(aimDirection), directionPitch(aimDirection));
        bow.setTarget(target);
        bow.setDrawProgress(drawProgress);

        if (bow.age % 6 == 0) {
            world.spawnParticles(DAWN_DUST, anchor.x, anchor.y, anchor.z,
                    Math.round(2 + 6 * drawProgress), 0.3, 0.3, 0.3, 0.02);
        }
    }

    public static void release(ServerWorld world, LivingEntity owner, ItemStack stack, float chargeRatio) {
        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        DawnquiverBowVisualEntity bow = resolveBow(world, owner.getUuid());
        LivingEntity target = findActiveTarget(world, owner, settings);
        Vec3d aimDirection = activeAimDirection(owner, target);
        Vec3d origin = bowAnchor(owner, aimDirection, settings.bowDistance);
        if (bow != null) {
            bow.refreshPositionAndAngles(origin.x, origin.y, origin.z,
                    directionYaw(aimDirection), directionPitch(aimDirection));
            bow.setTarget(target);
        }
        clear(world, owner.getUuid());

        if (chargeRatio < settings.minimumDraw) {
            if (bow != null) {
                bow.discard();
            }
            world.spawnParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 8, 0.25, 0.25, 0.25, 0.01);
            world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_SHOOT_MISS_02.get(),
                    SoundCategory.PLAYERS, 0.35F, 1.15F);
            return;
        }

        Vec3d direction = target != null
                ? aimPoint(target).subtract(origin).normalize()
                : aimDirection;

        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.HEALING, owner, stack,
                (float) MathHelper.lerp(chargeRatio, (float) settings.initialDamageScaling,
                        (float) settings.maxChargeDamageScaling),
                (float) MathHelper.lerp(chargeRatio, (float) settings.initialSpellScaling,
                        (float) settings.maxChargeSpellScaling));

        DawnquiverArrowEntity arrow = new DawnquiverArrowEntity(world, owner, stack, origin, direction,
                target, damage, settings.arrowSpeed, settings.homingStrength,
                0.7 + chargeRatio * 0.8, AwakeningApi.scaleEffect(stack, settings.impactRadius), 72.0);
        world.spawnEntity(arrow);

        if (bow != null) {
            bow.markReleased();
            bow.setLifetime(bow.age + BOW_TRAILING_TICKS);
        }

        world.spawnParticles(DAWN_DUST, origin.x, origin.y, origin.z, 36, 0.4, 0.4, 0.4, 0.14);
        world.spawnParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_FLYBY_01.get(),
                SoundCategory.PLAYERS, 0.75F, 0.95F + world.random.nextFloat() * 0.1F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_SHOOT_FLYBY_02.get(),
                SoundCategory.PLAYERS, 0.55F, 1.0F);
    }

    public static void cancel(ServerWorld world, UUID ownerUuid) {
        DawnquiverBowVisualEntity bow = resolveBow(world, ownerUuid);
        if (bow != null) {
            bow.discard();
        }
        clear(world, ownerUuid);
    }

    public static void tickHeldPassive(LivingEntity owner, ItemStack stack) {
        if (owner == null || stack == null || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.DAWNQUIVER.get())
                || !AwakeningApi.isAbilityUnlocked(stack)
                || !(owner.getWorld() instanceof ServerWorld world)
                || !owner.isAlive()
                || !HelperMethods.isHolding(stack, owner)) {
            return;
        }

        long now = world.getTime();
        Map<UUID, Long> lastTicks = LAST_PASSIVE_TICK.computeIfAbsent(world, ignored -> new HashMap<>());
        if (lastTicks.getOrDefault(owner.getUuid(), Long.MIN_VALUE) == now) {
            return;
        }
        lastTicks.put(owner.getUuid(), now);

        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        int interval = Math.max(1, settings.passiveInterval);
        if (Math.floorMod(owner.age + owner.getId(), interval) != 0) {
            return;
        }
        Map<UUID, Long> lockouts = PASSIVE_LOCKOUT.computeIfAbsent(world, ignored -> new HashMap<>());
        if (now < lockouts.getOrDefault(owner.getUuid(), Long.MIN_VALUE)) {
            return;
        }

        LivingEntity target = findFurthestTarget(world, owner, settings.passiveRange);
        if (target == null) {
            return;
        }

        Vec3d anchor = shoulderAnchor(owner);
        int seed = owner.getRandom().nextInt(4096);
        Hand hand = owner.getOffHandStack() == stack ? Hand.OFF_HAND : Hand.MAIN_HAND;
        DawnquiverBowVisualEntity bow = new DawnquiverBowVisualEntity(world, owner, hand,
                anchor.x, anchor.y, anchor.z,
                directionYaw(directionTo(owner, target)), directionPitch(directionTo(owner, target)),
                14, (float) settings.passiveBowScale, seed, DawnquiverBowVisualEntity.MODE_PASSIVE);
        bow.setTarget(target);
        bow.setDrawProgress(1.0F);
        bow.markReleased();
        world.spawnEntity(bow);

        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.HEALING, owner, stack,
                (float) settings.passiveDamageScaling, (float) settings.passiveSpellScaling);
        Vec3d direction = aimPoint(target).subtract(anchor).normalize();
        DawnquiverArrowEntity arrow = new DawnquiverArrowEntity(world, owner, stack, anchor, direction,
                target, damage, settings.arrowSpeed * 0.85, settings.homingStrength,
                settings.passiveArrowScale, 0.0, settings.passiveRange * 2.0);
        world.spawnEntity(arrow);

        lockouts.put(owner.getUuid(), now + Math.max(1, settings.passiveLockout));
        world.spawnParticles(DAWN_DUST, anchor.x, anchor.y, anchor.z, 10, 0.2, 0.2, 0.2, 0.03);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_PULL_BACK_SHORT_VERSION_01.get(),
                SoundCategory.PLAYERS, 0.3F, 1.25F);
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveDraw> draws = ACTIVE.get(world);
        Map<UUID, Long> lastTicks = LAST_PASSIVE_TICK.get(world);
        Map<UUID, Long> lockouts = PASSIVE_LOCKOUT.get(world);
        return draws != null && !draws.isEmpty()
                || lastTicks != null && !lastTicks.isEmpty()
                || lockouts != null && !lockouts.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ActiveDraw> draws = ACTIVE.get(world);
        if (draws != null && !draws.isEmpty()) {
            draws.entrySet().removeIf(entry -> {
                Entity owner = world.getEntity(entry.getKey());
                ActiveDraw draw = entry.getValue();
                boolean valid = owner instanceof LivingEntity living && living.isAlive()
                        && living.isUsingItem()
                        && living.getActiveHand() == draw.hand
                        && living.getStackInHand(draw.hand).isOf(ItemsRegistry.DAWNQUIVER.get());
                if (!valid) {
                    DawnquiverBowVisualEntity bow = resolveBow(world, entry.getKey());
                    if (bow != null) {
                        bow.discard();
                    }
                }
                return !valid;
            });
            if (draws.isEmpty()) {
                ACTIVE.remove(world);
            }
        }

        if (world.getTime() % PRUNE_INTERVAL != 0L) {
            return;
        }
        long now = world.getTime();
        prune(LAST_PASSIVE_TICK, world, now, 40L);
        prune(PASSIVE_LOCKOUT, world, now, 1200L);
    }

    private static void prune(Map<ServerWorld, Map<UUID, Long>> source, ServerWorld world,
                              long now, long maximumAge) {
        Map<UUID, Long> entries = source.get(world);
        if (entries == null) {
            return;
        }
        entries.values().removeIf(tick -> now - tick > maximumAge);
        if (entries.isEmpty()) {
            source.remove(world);
        }
    }

    @Nullable
    private static LivingEntity findActiveTarget(ServerWorld world, LivingEntity owner,
                                                 DawnquiverSwordItem.EffectSettings settings) {
        Entity aimed = HelperMethods.getTargetedEntity(owner, settings.activeRange);
        if (aimed instanceof LivingEntity living && isValidTarget(living, owner)) {
            return living;
        }
        double range = Math.max(1.0, settings.activeRange);
        double threshold = Math.cos(Math.toRadians(45.0));
        Vec3d look = owner.getRotationVec(1.0F).normalize();
        return world.getEntitiesByClass(LivingEntity.class, owner.getBoundingBox().expand(range),
                        entity -> isValidTarget(entity, owner)
                                && entity.squaredDistanceTo(owner) <= range * range
                                && owner.canSee(entity))
                .stream()
                .filter(entity -> directionTo(owner, entity).dotProduct(look) >= threshold)
                .min(Comparator.comparingDouble(owner::squaredDistanceTo))
                .orElse(null);
    }

    @Nullable
    private static LivingEntity findFurthestTarget(ServerWorld world, LivingEntity owner, double range) {
        double maximum = Math.max(1.0, range);
        return world.getEntitiesByClass(LivingEntity.class, owner.getBoundingBox().expand(maximum),
                        entity -> isValidTarget(entity, owner)
                                && entity.squaredDistanceTo(owner) <= maximum * maximum
                                && owner.canSee(entity))
                .stream()
                .max(Comparator.comparingDouble(owner::squaredDistanceTo))
                .orElse(null);
    }

    private static boolean isValidTarget(LivingEntity entity, LivingEntity owner) {
        return entity != owner && entity.isAlive() && !entity.isRemoved()
                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                && HelperMethods.checkAbilityTarget(entity, owner);
    }

    private static Vec3d directionTo(LivingEntity owner, LivingEntity target) {
        Vec3d offset = aimPoint(target).subtract(owner.getEyePos());
        return offset.lengthSquared() < 1.0E-6 ? owner.getRotationVec(1.0F) : offset.normalize();
    }

    private static Vec3d aimPoint(LivingEntity target) {
        return target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
    }

    private static Vec3d horizontalOrLook(LivingEntity owner) {
        Vec3d look = owner.getRotationVec(1.0F);
        return look.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 0.0, 1.0) : look.normalize();
    }

    private static Vec3d activeAimDirection(LivingEntity owner, @Nullable LivingEntity target) {
        return target == null ? horizontalOrLook(owner) : directionTo(owner, target);
    }

    private static Vec3d bowAnchor(LivingEntity owner, Vec3d direction, double distance) {
        return owner.getPos()
                .add(0.0, owner.getHeight() * 0.62, 0.0)
                .add(direction.multiply(Math.max(0.0, distance)));
    }

    private static float directionYaw(Vec3d direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    private static float directionPitch(Vec3d direction) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
    }

    private static Vec3d shoulderAnchor(LivingEntity owner) {
        Vec3d look = horizontalOrLook(owner);
        Vec3d side = new Vec3d(-look.z, 0.0, look.x).normalize();
        return owner.getEyePos().add(side.multiply(0.55)).add(0.0, 0.25, 0.0);
    }

    @Nullable
    private static DawnquiverBowVisualEntity resolveBow(ServerWorld world, UUID ownerUuid) {
        Map<UUID, ActiveDraw> draws = ACTIVE.get(world);
        ActiveDraw draw = draws == null ? null : draws.get(ownerUuid);
        if (draw == null) {
            return null;
        }
        Entity entity = world.getEntity(draw.bowUuid);
        return entity instanceof DawnquiverBowVisualEntity bow && !bow.isRemoved() ? bow : null;
    }

    private static void clear(ServerWorld world, UUID ownerUuid) {
        Map<UUID, ActiveDraw> draws = ACTIVE.get(world);
        if (draws == null) {
            return;
        }
        draws.remove(ownerUuid);
        if (draws.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private record ActiveDraw(UUID bowUuid, Hand hand) {
    }
}
