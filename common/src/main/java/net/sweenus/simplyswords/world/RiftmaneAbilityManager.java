package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase10AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase10UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.RiftmaneChargerEntity;
import net.sweenus.simplyswords.entity.RiftmaneRiftVisualEntity;
import net.sweenus.simplyswords.item.custom.RiftmaneSwordItem;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RiftmaneAbilityManager {

    private static final DustColorTransitionParticleEffect RIFT_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.03F, 0.55F, 0.58F),
                    new Vector3f(0.72F, 1.0F, 0.96F), 1.3F);
    private static final int LOCKOUT_PRUNE_INTERVAL = 200;
    private static final int LIFETIME_MARGIN = 4;
    private static final int RIFT_TRAILING_TICKS = 8;
    private static final int RIFT_MINIMUM_LIFETIME = 16;
    private static final float RIFT_HEIGHT = 2.6F;
    private static final int SUPPORT_SCAN_UP = 4;
    private static final int SUPPORT_SCAN_DOWN = 12;

    private static final Map<ServerWorld, Map<UUID, Long>> LAST_PASSIVE = new HashMap<>();

    private RiftmaneAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && context.stack().isOf(ItemsRegistry.RIFTMANE.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1;
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        ServerWorld world = context.world();
        LivingEntity owner = context.actor();
        RiftmaneSwordItem.EffectSettings settings = Config.uniqueEffects.riftmane;
        UniqueAbilityExecution execution = Phase10CombatManager.beginActive(
                Phase10UniqueAbilities.RIFTMANE_RANK, context, settings.cooldown);
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);

        Vec3d forward = horizontal(context.facing(), owner.getYaw());
        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);
        int count = Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.COUNT, settings.chargerCount));
        double spacing = count > 1 ? Math.max(0.5, tuning.get(
                Phase10AbilityTuning.Setting.WIDTH, settings.rankWidth)) / (count - 1) : 0.0;
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.ARCANE, owner, context.stack(),
                (float) settings.damageScaling, (float) settings.spellScaling);
        damage *= (float) tuning.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);

        boolean waterWalk = settings.waterWalking && !owner.isSubmergedInWater();
        ServerPlayerEntity rider = context.actor() instanceof ServerPlayerEntity player
                && player.isSprinting() && !player.hasVehicle() ? player : null;
        int mountIndex = rider == null ? -1 : count / 2;

        int spawned = 0;
        for (int index = 0; index < count; index++) {
            double lateral = (index - (count - 1) * 0.5) * spacing;
            Vec3d lane = owner.getPos().add(side.multiply(lateral));
            Vec3d ahead = lane.add(forward.multiply(Math.max(0.0, settings.spawnOffset)));
            double distanceMultiplier = index == mountIndex
                    ? Math.max(1.0, tuning.get(Phase10AbilityTuning.Setting.SECONDARY_RADIUS,
                    settings.mountedDistanceMultiplier)) : 1.0;
            boolean audioLead = spawned == 0;
            RiftmaneChargerEntity charger = summon(world, owner, context.stack(), ahead, forward,
                    damage, settings, waterWalk, distanceMultiplier, audioLead, tuning);
            if (charger == null) {
                charger = summon(world, owner, context.stack(), lane, forward,
                        damage, settings, waterWalk, distanceMultiplier, audioLead, tuning);
            }
            if (charger == null) {
                continue;
            }
            spawned++;
            if (index == mountIndex) {
                rider.startRiding(charger, true);
            }
        }
        if (spawned == 0) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }

        if (rider != null && tuning.has(Phase10AbilityTuning.Setting.STATUS_AMPLIFIER))
            rider.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.RESISTANCE,
                    Math.max(20, settings.rearDuration + 20),
                    tuning.integer(Phase10AbilityTuning.Setting.STATUS_AMPLIFIER, 1)), owner);

        spawnActivationEffects(world, owner, forward);
        Phase10CombatManager.finish(execution, spawned);
        return true;
    }

    public static void onSwing(ItemStack stack, ServerWorld world, LivingEntity owner) {
        if (stack == null || !stack.isOf(ItemsRegistry.RIFTMANE.get()) || owner == null || !owner.isAlive()) {
            return;
        }
        RiftmaneSwordItem.EffectSettings settings = Config.uniqueEffects.riftmane;
        UniqueAbilityExecution execution = Phase10CombatManager.beginPassive(
                Phase10UniqueAbilities.RIFTMANE_HARRIER, world, stack, owner, null);
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        long now = world.getTime();
        Map<UUID, Long> lockouts = LAST_PASSIVE.get(world);
        Long nextEligible = lockouts == null ? null : lockouts.get(owner.getUuid());
        if (nextEligible != null && now < nextEligible) {
            Phase10CombatManager.finish(execution, 0);
            return;
        }
        if (owner.getRandom().nextInt(100) >= AwakeningApi.scaleChance(stack,
                tuning.integer(Phase10AbilityTuning.Setting.CHANCE, settings.passiveChance))) {
            Phase10CombatManager.finish(execution, 0);
            return;
        }
        LivingEntity target = findPassiveTarget(world, owner, settings, tuning);
        if (target == null) {
            Phase10CombatManager.finish(execution, 0);
            return;
        }

        boolean waterWalk = settings.waterWalking && !owner.isSubmergedInWater();
        Vec3d forward = horizontal(target.getPos().subtract(owner.getPos()), owner.getYaw());
        Vec3d position = owner.getPos().add(forward.multiply(Math.max(0.0, settings.spawnOffset)));
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.ARCANE, owner, stack,
                (float) settings.damageScaling, (float) settings.spellScaling);
        damage *= (float) tuning.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
        int count = Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.COUNT, 1));
        int spawned = 0;
        Vec3d side = new Vec3d(-forward.z, 0, forward.x);
        for (int i = 0; i < count; i++) {
            Vec3d offset = position.add(side.multiply((i - (count - 1) * .5) * .8));
            if (summon(world, owner, stack, offset, forward, damage, settings, waterWalk, 1.0,
                    spawned == 0, tuning) != null) spawned++;
        }
        if (spawned == 0) {
            Phase10CombatManager.finish(execution, 0);
            return;
        }

        LAST_PASSIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(
                owner.getUuid(), now + SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                        stack, owner, tuning.integer(Phase10AbilityTuning.Setting.LOCKOUT_TICKS,
                                settings.passiveLockout)));
        world.spawnParticles(RIFT_DUST, position.x, position.y + 0.9, position.z, 20, 0.5, 0.5, 0.5, 0.03);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DISTORTION_ARC_01.get(),
                SoundCategory.PLAYERS, 0.4F, 1.15F + world.random.nextFloat() * 0.15F);
        Phase10CombatManager.finish(execution, spawned);
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, Long> lockouts = LAST_PASSIVE.get(world);
        return lockouts != null && !lockouts.isEmpty();
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % LOCKOUT_PRUNE_INTERVAL != 0L) {
            return;
        }
        Map<UUID, Long> lockouts = LAST_PASSIVE.get(world);
        if (lockouts == null) {
            return;
        }
        long now = world.getTime();
        lockouts.values().removeIf(tick -> tick <= now);
        if (lockouts.isEmpty()) {
            LAST_PASSIVE.remove(world);
        }
    }

    @Nullable
    private static RiftmaneChargerEntity summon(ServerWorld world, LivingEntity owner, ItemStack stack,
                                                Vec3d position, Vec3d forward, float damage,
                                                RiftmaneSwordItem.EffectSettings settings,
                                                boolean waterWalk, double distanceMultiplier,
                                                boolean audioLead) {
        return summon(world, owner, stack, position, forward, damage, settings, waterWalk,
                distanceMultiplier, audioLead, Phase10AbilityTuning.EMPTY);
    }

    @Nullable
    private static RiftmaneChargerEntity summon(ServerWorld world, LivingEntity owner, ItemStack stack,
                                                Vec3d position, Vec3d forward, float damage,
                                                RiftmaneSwordItem.EffectSettings settings,
                                                boolean waterWalk, double distanceMultiplier,
                                                boolean audioLead, Phase10AbilityTuning tuning) {
        double speed = Math.max(0.05, settings.chargeSpeed
                * tuning.get(Phase10AbilityTuning.Setting.SPEED, 1));
        double distance = Math.max(1.0, tuning.get(Phase10AbilityTuning.Setting.RANGE,
                settings.chargeDistance)) * Math.max(1.0, distanceMultiplier);
        int rearTicks = Math.max(0, tuning.integer(Phase10AbilityTuning.Setting.WINDUP_TICKS,
                settings.rearDuration));
        int lifetime = rearTicks + (int) Math.ceil(distance / speed) + LIFETIME_MARGIN;
        double groundY = findSupportTopY(world, position.x, position.z, owner.getY() + 1.0, waterWalk);
        int seed = owner.getRandom().nextInt(4096);

        RiftmaneChargerEntity charger = new RiftmaneChargerEntity(EntityRegistry.RIFTMANE_CHARGER.get(), world);
        charger.setWaterWalk(waterWalk);
        charger.setAudioLead(audioLead);
        float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(-forward.x, forward.z)));
        charger.refreshPositionAndAngles(position.x, groundY, position.z, yaw, 0.0F);
        if (!world.isSpaceEmpty(charger, charger.getBoundingBox())) {
            charger.discard();
            return null;
        }
        charger.initializeCharge(owner, stack, forward, lifetime, rearTicks, seed, damage,
                tuning.get(Phase10AbilityTuning.Setting.KNOCKBACK,
                        AwakeningApi.scaleEffect(stack, settings.knockbackStrength)),
                speed, tuning.get(Phase10AbilityTuning.Setting.RADIUS, settings.hitRadius),
                tuning.get(Phase10AbilityTuning.Setting.HEIGHT, settings.stepHeight));
        if (!world.spawnEntity(charger)) {
            charger.discard();
            return null;
        }

        spawnRift(world, position, groundY, yaw, rearTicks, seed, audioLead);
        world.spawnParticles(RIFT_DUST, position.x, groundY + 0.8, position.z, 16, 0.4, 0.5, 0.4, 0.04);
        return charger;
    }

    private static double findSupportTopY(ServerWorld world, double x, double z, double centerY,
                                          boolean waterWalk) {
        if (!waterWalk) {
            return LivyatanWaveManager.findGroundTopY(world, x, z, centerY);
        }
        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        int startY = MathHelper.floor(centerY) + SUPPORT_SCAN_UP;
        int minY = Math.max(world.getBottomY(), MathHelper.floor(centerY) - SUPPORT_SCAN_DOWN);
        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            BlockState state = world.getBlockState(pos);
            if (state.isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
            if (state.getFluidState().isIn(FluidTags.WATER)
                    && !world.getBlockState(pos.up()).getFluidState().isIn(FluidTags.WATER)) {
                return y + 1.0;
            }
        }
        return LivyatanWaveManager.findGroundTopY(world, x, z, centerY);
    }

    private static void spawnRift(ServerWorld world, Vec3d position, double groundY, float chargeYaw,
                                  int rearTicks, int seed, boolean audioLead) {
        int lifetime = Math.max(RIFT_MINIMUM_LIFETIME, rearTicks + RIFT_TRAILING_TICKS);
        RiftmaneRiftVisualEntity rift = new RiftmaneRiftVisualEntity(world,
                position.x, groundY, position.z,
                MathHelper.wrapDegrees(chargeYaw + 180.0F), lifetime, RIFT_HEIGHT, seed);
        world.spawnEntity(rift);
        if (audioLead) {
            world.playSound(null, rift.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                    SoundCategory.PLAYERS, 0.9F, 0.58F);
            world.playSound(null, rift.getBlockPos(), SoundRegistry.CAELESTIS_CREATURE_ARRIVAL.get(),
                    SoundCategory.PLAYERS, 0.45F, 1.25F);
        }
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, position.x, groundY + RIFT_HEIGHT * 0.5, position.z,
                40, 0.45, 0.8, 0.45, 0.11);
    }

    private static LivingEntity findPassiveTarget(ServerWorld world, LivingEntity owner,
                                                  RiftmaneSwordItem.EffectSettings settings,
                                                  Phase10AbilityTuning tuning) {
        double minimum = Math.max(0.0, tuning.get(Phase10AbilityTuning.Setting.SECONDARY_RADIUS,
                settings.passiveMinRange));
        double maximum = Math.max(minimum + 0.1, tuning.get(Phase10AbilityTuning.Setting.RANGE,
                settings.passiveMaxRange));
        double minimumSquared = minimum * minimum;
        double maximumSquared = maximum * maximum;
        double threshold = Math.cos(Math.toRadians(
                MathHelper.clamp(tuning.get(Phase10AbilityTuning.Setting.ANGLE,
                        settings.passiveConeDegrees), 1.0, 180.0) * 0.5));
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
                .min((first, second) -> {
                    if (tuning.flag(1 << 8)) return Double.compare(owner.squaredDistanceTo(second),
                            owner.squaredDistanceTo(first));
                    return Comparator.comparingDouble((LivingEntity entity) -> {
                    double alignment = directionTo(owner, entity).dotProduct(look);
                    return (1.0 - alignment) * 100.0 + owner.squaredDistanceTo(entity) * 0.02;
                    }).compare(first, second);
                })
                .orElse(null);
    }

    private static Vec3d directionTo(LivingEntity owner, LivingEntity target) {
        Vec3d offset = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0).subtract(owner.getEyePos());
        return offset.lengthSquared() < 1.0E-6 ? owner.getRotationVec(1.0F) : offset.normalize();
    }

    private static Vec3d horizontal(Vec3d source, float fallbackYaw) {
        if (source == null || source.horizontalLengthSquared() < 1.0E-6) {
            return Vec3d.fromPolar(0.0F, fallbackYaw).normalize();
        }
        return new Vec3d(source.x, 0.0, source.z).normalize();
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity owner, Vec3d forward) {
        Vec3d origin = owner.getPos().add(forward.multiply(0.6));
        world.spawnParticles(RIFT_DUST, origin.x, origin.y + 1.0, origin.z, 48, 1.2, 0.6, 1.2, 0.08);
        world.spawnParticles(ParticleTypes.END_ROD, origin.x, origin.y + 0.9, origin.z, 22, 1.0, 0.4, 1.0, 0.05);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.ACTIVATE_PLINTH_03.get(),
                SoundCategory.PLAYERS, 0.55F, 0.72F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DISTORTION_ARC_02.get(),
                SoundCategory.PLAYERS, 0.85F, 0.9F + world.random.nextFloat() * 0.15F);
        world.playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_WARDEN_EMERGE,
                SoundCategory.PLAYERS, 0.4F, 1.4F);
    }
}
