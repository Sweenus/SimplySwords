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
import net.sweenus.simplyswords.api.ability.MartialCommandEldritchMasteryTuning;
import net.sweenus.simplyswords.api.ability.MartialCommandEldritchMasteryAbilities;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    private static final int MAX_RANK = 12;

    private static final Map<ServerWorld, Map<UUID, Long>> LAST_PASSIVE = new HashMap<>();
    private static final Map<ServerWorld, List<PendingRank>> PENDING_RANKS = new HashMap<>();

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
        UniqueAbilityExecution execution = MartialCommandEldritchMasteryCombatManager.beginActive(
                MartialCommandEldritchMasteryAbilities.RIFTMANE_RANK, context, settings.cooldown,
                rankBase(settings, context.stack()));
        MartialCommandEldritchMasteryTuning tuning = MartialCommandEldritchMasteryAbilities.tuning(execution);

        Vec3d forward = horizontal(context.facing(), owner.getYaw());
        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);
        int count = Math.clamp(tuning.integer(MartialCommandEldritchMasteryTuning.Setting.COUNT, settings.chargerCount), 1, MAX_RANK);
        double spacing = count > 1 ? Math.max(0.5, tuning.get(
                MartialCommandEldritchMasteryTuning.Setting.WIDTH, settings.rankWidth)) / (count - 1) : 0.0;
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.ARCANE, owner, context.stack(),
                (float) settings.damageScaling, (float) settings.spellScaling);
        float rankDamage = damage * (float) tuning.get(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);

        boolean waterWalk = settings.waterWalking && !owner.isSubmergedInWater();
        ServerPlayerEntity rider = context.actor() instanceof ServerPlayerEntity player
                && player.isSprinting() && !player.hasVehicle() ? player : null;
        int mountIndex = rider == null ? -1 : count / 2;
        UniqueAbilityExecution riderExecution = rider == null ? null
                : MartialCommandEldritchMasteryCombatManager.beginPassive(MartialCommandEldritchMasteryAbilities.RIFTMANE_RIDER, world,
                        context.stack(), owner, null, riderBase(settings, context.stack()));
        MartialCommandEldritchMasteryTuning riderTuning = riderExecution == null
                ? MartialCommandEldritchMasteryTuning.EMPTY : MartialCommandEldritchMasteryAbilities.tuning(riderExecution);

        int spawned = 0;
        for (int index = 0; index < count; index++) {
            boolean mounted = index == mountIndex;
            MartialCommandEldritchMasteryTuning applied = mounted ? riderTuning : tuning;
            double lateral = (index - (count - 1) * 0.5) * spacing;
            Vec3d lane = owner.getPos().add(side.multiply(lateral));
            Vec3d ahead = lane.add(forward.multiply(Math.max(0.0, settings.spawnOffset)));
            double distanceMultiplier = mounted
                    ? Math.max(1.0, riderTuning.get(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_RADIUS,
                    settings.mountedDistanceMultiplier)) : 1.0;
            float applance = mounted
                    ? damage * (float) riderTuning.get(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                    : rankDamage;
            boolean audioLead = spawned == 0;
            RiftmaneChargerEntity charger = summon(world, owner, context.stack(), ahead, forward,
                    applance, settings, waterWalk, distanceMultiplier, audioLead, applied);
            if (charger == null) {
                charger = summon(world, owner, context.stack(), lane, forward,
                        applance, settings, waterWalk, distanceMultiplier, audioLead, applied);
            }
            if (charger == null) {
                continue;
            }
            spawned++;
            if (mounted) {
                configureRider(charger, riderTuning);
                rider.startRiding(charger, true);
            }
        }
        if (spawned == 0) {
            if (riderExecution != null) MartialCommandEldritchMasteryCombatManager.finish(riderExecution, 0);
            UniqueAbilityApi.cancel(execution);
            return false;
        }

        if (rider != null && riderTuning.has(MartialCommandEldritchMasteryTuning.Setting.STATUS_AMPLIFIER))
            rider.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.RESISTANCE,
                    Math.max(20, settings.rearDuration + 20),
                    riderTuning.integer(MartialCommandEldritchMasteryTuning.Setting.STATUS_AMPLIFIER, 1)), owner);
        if (tuning.has(MartialCommandEldritchMasteryTuning.Setting.DELAY_TICKS)) {
            PENDING_RANKS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new PendingRank(
                    owner.getUuid(), context.stack().copy(), forward,
                    world.getTime() + Math.max(1, tuning.integer(MartialCommandEldritchMasteryTuning.Setting.DELAY_TICKS, 10)),
                    Math.clamp(tuning.integer(MartialCommandEldritchMasteryTuning.Setting.TARGET_CAP, 3), 1, MAX_RANK),
                    damage * (float) tuning.get(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .55),
                    spacing, tuning));
        }

        spawnActivationEffects(world, owner, forward);
        if (riderExecution != null) MartialCommandEldritchMasteryCombatManager.finish(riderExecution, 1);
        MartialCommandEldritchMasteryCombatManager.finish(execution, spawned);
        return true;
    }

    private static void configureRider(RiftmaneChargerEntity charger, MartialCommandEldritchMasteryTuning tuning) {
        charger.setPhasing(tuning.flag(1 << 25));
        charger.setRiderGuard(tuning.has(MartialCommandEldritchMasteryTuning.Setting.STATUS_AMPLIFIER)
                        ? tuning.integer(MartialCommandEldritchMasteryTuning.Setting.STATUS_AMPLIFIER, 1) : -1,
                tuning.integer(MartialCommandEldritchMasteryTuning.Setting.STATUS_DURATION_TICKS, 20));
        charger.setImpactBonus(tuning.get(MartialCommandEldritchMasteryTuning.Setting.OUTGOING_MULTIPLIER, 1),
                tuning.integer(MartialCommandEldritchMasteryTuning.Setting.TARGET_CAP, 0),
                tuning.get(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1));
    }

    private static void tickPendingRanks(ServerWorld world) {
        List<PendingRank> pending = PENDING_RANKS.get(world);
        if (pending == null || pending.isEmpty()) {
            return;
        }
        RiftmaneSwordItem.EffectSettings settings = Config.uniqueEffects.riftmane;
        Iterator<PendingRank> iterator = pending.iterator();
        while (iterator.hasNext()) {
            PendingRank rank = iterator.next();
            if (world.getTime() < rank.readyTick) continue;
            iterator.remove();
            if (!(world.getEntity(rank.ownerId) instanceof LivingEntity owner) || !owner.isAlive()) continue;
            boolean waterWalk = settings.waterWalking && !owner.isSubmergedInWater();
            Vec3d side = new Vec3d(-rank.forward.z, 0.0, rank.forward.x);
            for (int index = 0; index < rank.count; index++) {
                double lateral = (index - (rank.count - 1) * 0.5) * Math.max(0.5, rank.spacing);
                Vec3d lane = owner.getPos().add(side.multiply(lateral));
                Vec3d ahead = lane.add(rank.forward.multiply(Math.max(0.0, settings.spawnOffset)));
                summon(world, owner, rank.stack, ahead, rank.forward, rank.damage, settings,
                        waterWalk, 1.0, index == 0, rank.tuning);
            }
        }
        if (pending.isEmpty()) PENDING_RANKS.remove(world);
    }

    public static void onChargerKill(ServerWorld world, LivingEntity owner, ItemStack stack, LivingEntity victim,
                                     double radius, double damageMultiplier, int refundTicks) {
        if (owner == null || !owner.isAlive() || stack == null || stack.isEmpty()) {
            return;
        }
        Map<UUID, Long> lockouts = LAST_PASSIVE.get(world);
        if (lockouts != null && refundTicks > 0) {
            Long next = lockouts.get(owner.getUuid());
            if (next != null) lockouts.put(owner.getUuid(),
                    Math.max(world.getTime(), next - refundTicks));
        }
        if (radius <= 0) return;
        RiftmaneSwordItem.EffectSettings settings = Config.uniqueEffects.riftmane;
        LivingEntity next = world.getEntitiesByClass(LivingEntity.class,
                        victim.getBoundingBox().expand(radius),
                        candidate -> candidate != victim && candidate.isAlive()
                                && HelperMethods.checkAbilityTarget(candidate, owner))
                .stream().min(Comparator.comparingDouble(victim::squaredDistanceTo)).orElse(null);
        if (next == null) return;
        Vec3d forward = horizontal(next.getPos().subtract(owner.getPos()), owner.getYaw());
        Vec3d position = owner.getPos().add(forward.multiply(Math.max(0.0, settings.spawnOffset)));
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.ARCANE, owner, stack,
                (float) settings.damageScaling, (float) settings.spellScaling);
        summon(world, owner, stack, position, forward, damage * (float) damageMultiplier, settings,
                settings.waterWalking && !owner.isSubmergedInWater(), 1.0, true,
                harrierBase(settings, stack).with(MartialCommandEldritchMasteryTuning.Setting.SEARCH_RADIUS, 0));
    }

    public static MartialCommandEldritchMasteryTuning chargerBase(double chargeDistance, double knockback, double hitRadius,
                                                  double stepHeight, int rearDuration) {
        return MartialCommandEldritchMasteryTuning.EMPTY
                .with(MartialCommandEldritchMasteryTuning.Setting.RANGE, chargeDistance)
                .with(MartialCommandEldritchMasteryTuning.Setting.SPEED, 1)
                .with(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                .with(MartialCommandEldritchMasteryTuning.Setting.KNOCKBACK, knockback)
                .with(MartialCommandEldritchMasteryTuning.Setting.RADIUS, hitRadius)
                .with(MartialCommandEldritchMasteryTuning.Setting.HEIGHT, stepHeight)
                .with(MartialCommandEldritchMasteryTuning.Setting.WINDUP_TICKS, rearDuration);
    }

    public static MartialCommandEldritchMasteryTuning harrierBase(MartialCommandEldritchMasteryTuning charger, int chance, int lockout,
                                                  double maxRange, double minRange, double cone) {
        return charger
                .with(MartialCommandEldritchMasteryTuning.Setting.CHANCE, chance)
                .with(MartialCommandEldritchMasteryTuning.Setting.LOCKOUT_TICKS, lockout)
                .with(MartialCommandEldritchMasteryTuning.Setting.SEARCH_RANGE, maxRange)
                .with(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_RADIUS, minRange)
                .with(MartialCommandEldritchMasteryTuning.Setting.ANGLE, cone)
                .with(MartialCommandEldritchMasteryTuning.Setting.COUNT, 1);
    }

    public static MartialCommandEldritchMasteryTuning rankBase(MartialCommandEldritchMasteryTuning charger, int count, double width) {
        return charger.with(MartialCommandEldritchMasteryTuning.Setting.COUNT, count).with(MartialCommandEldritchMasteryTuning.Setting.WIDTH, width);
    }

    public static MartialCommandEldritchMasteryTuning riderBase(MartialCommandEldritchMasteryTuning charger, double mountedMultiplier) {
        return charger.with(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_RADIUS, mountedMultiplier);
    }

    private static MartialCommandEldritchMasteryTuning chargerBase(RiftmaneSwordItem.EffectSettings settings, ItemStack stack) {
        return chargerBase(settings.chargeDistance,
                AwakeningApi.scaleEffect(stack, settings.knockbackStrength),
                settings.hitRadius, settings.stepHeight, settings.rearDuration);
    }

    private static MartialCommandEldritchMasteryTuning harrierBase(RiftmaneSwordItem.EffectSettings settings, ItemStack stack) {
        return harrierBase(chargerBase(settings, stack), settings.passiveChance, settings.passiveLockout,
                settings.passiveMaxRange, settings.passiveMinRange, settings.passiveConeDegrees);
    }

    private static MartialCommandEldritchMasteryTuning rankBase(RiftmaneSwordItem.EffectSettings settings, ItemStack stack) {
        return rankBase(chargerBase(settings, stack), settings.chargerCount, settings.rankWidth);
    }

    private static MartialCommandEldritchMasteryTuning riderBase(RiftmaneSwordItem.EffectSettings settings, ItemStack stack) {
        return riderBase(chargerBase(settings, stack), settings.mountedDistanceMultiplier);
    }

    public static void onSwing(ItemStack stack, ServerWorld world, LivingEntity owner) {
        if (stack == null || !stack.isOf(ItemsRegistry.RIFTMANE.get()) || owner == null || !owner.isAlive()) {
            return;
        }
        RiftmaneSwordItem.EffectSettings settings = Config.uniqueEffects.riftmane;
        UniqueAbilityExecution execution = MartialCommandEldritchMasteryCombatManager.beginPassive(
                MartialCommandEldritchMasteryAbilities.RIFTMANE_HARRIER, world, stack, owner, null,
                harrierBase(settings, stack));
        MartialCommandEldritchMasteryTuning tuning = MartialCommandEldritchMasteryAbilities.tuning(execution);
        long now = world.getTime();
        Map<UUID, Long> lockouts = LAST_PASSIVE.get(world);
        Long nextEligible = lockouts == null ? null : lockouts.get(owner.getUuid());
        if (nextEligible != null && now < nextEligible) {
            MartialCommandEldritchMasteryCombatManager.finish(execution, 0);
            return;
        }
        if (owner.getRandom().nextInt(100) >= AwakeningApi.scaleChance(stack,
                tuning.integer(MartialCommandEldritchMasteryTuning.Setting.CHANCE, settings.passiveChance))) {
            MartialCommandEldritchMasteryCombatManager.finish(execution, 0);
            return;
        }
        LivingEntity target = findPassiveTarget(world, owner, settings, tuning);
        if (target == null) {
            MartialCommandEldritchMasteryCombatManager.finish(execution, 0);
            return;
        }

        boolean waterWalk = settings.waterWalking && !owner.isSubmergedInWater();
        Vec3d forward = horizontal(target.getPos().subtract(owner.getPos()), owner.getYaw());
        Vec3d position = owner.getPos().add(forward.multiply(Math.max(0.0, settings.spawnOffset)));
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.ARCANE, owner, stack,
                (float) settings.damageScaling, (float) settings.spellScaling);
        damage *= (float) tuning.get(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
        int count = Math.max(1, tuning.integer(MartialCommandEldritchMasteryTuning.Setting.COUNT, 1));
        int spawned = 0;
        Vec3d side = new Vec3d(-forward.z, 0, forward.x);
        for (int i = 0; i < count; i++) {
            Vec3d offset = position.add(side.multiply((i - (count - 1) * .5) * .8));
            if (summon(world, owner, stack, offset, forward, damage, settings, waterWalk, 1.0,
                    spawned == 0, tuning) != null) spawned++;
        }
        if (spawned == 0) {
            MartialCommandEldritchMasteryCombatManager.finish(execution, 0);
            return;
        }

        LAST_PASSIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(
                owner.getUuid(), now + SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                        stack, owner, tuning.integer(MartialCommandEldritchMasteryTuning.Setting.LOCKOUT_TICKS,
                                settings.passiveLockout)));
        world.spawnParticles(RIFT_DUST, position.x, position.y + 0.9, position.z, 20, 0.5, 0.5, 0.5, 0.03);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DISTORTION_ARC_01.get(),
                SoundCategory.PLAYERS, 0.4F, 1.15F + world.random.nextFloat() * 0.15F);
        MartialCommandEldritchMasteryCombatManager.finish(execution, spawned);
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, Long> lockouts = LAST_PASSIVE.get(world);
        List<PendingRank> pending = PENDING_RANKS.get(world);
        return lockouts != null && !lockouts.isEmpty() || pending != null && !pending.isEmpty();
    }

    public static void tick(ServerWorld world) {
        tickPendingRanks(world);
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

    public static void clear(ServerWorld world) {
        LAST_PASSIVE.remove(world);
        PENDING_RANKS.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        LAST_PASSIVE.values().forEach(lockouts -> lockouts.remove(actor.getUuid()));
        PENDING_RANKS.values().forEach(pending ->
                pending.removeIf(rank -> rank.ownerId.equals(actor.getUuid())));
    }

    public static void clearAll() {
        LAST_PASSIVE.clear();
        PENDING_RANKS.clear();
    }

    private record PendingRank(UUID ownerId, ItemStack stack, Vec3d forward, long readyTick,
                               int count, float damage, double spacing, MartialCommandEldritchMasteryTuning tuning) {
    }

    @Nullable
    private static RiftmaneChargerEntity summon(ServerWorld world, LivingEntity owner, ItemStack stack,
                                                Vec3d position, Vec3d forward, float damage,
                                                RiftmaneSwordItem.EffectSettings settings,
                                                boolean waterWalk, double distanceMultiplier,
                                                boolean audioLead) {
        return summon(world, owner, stack, position, forward, damage, settings, waterWalk,
                distanceMultiplier, audioLead, MartialCommandEldritchMasteryTuning.EMPTY);
    }

    @Nullable
    private static RiftmaneChargerEntity summon(ServerWorld world, LivingEntity owner, ItemStack stack,
                                                Vec3d position, Vec3d forward, float damage,
                                                RiftmaneSwordItem.EffectSettings settings,
                                                boolean waterWalk, double distanceMultiplier,
                                                boolean audioLead, MartialCommandEldritchMasteryTuning tuning) {
        double speed = Math.max(0.05, settings.chargeSpeed
                * tuning.get(MartialCommandEldritchMasteryTuning.Setting.SPEED, 1));
        double distance = Math.max(1.0, tuning.get(MartialCommandEldritchMasteryTuning.Setting.RANGE,
                settings.chargeDistance)) * Math.max(1.0, distanceMultiplier);
        int rearTicks = Math.max(0, tuning.integer(MartialCommandEldritchMasteryTuning.Setting.WINDUP_TICKS,
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
                tuning.get(MartialCommandEldritchMasteryTuning.Setting.KNOCKBACK,
                        AwakeningApi.scaleEffect(stack, settings.knockbackStrength)),
                speed, tuning.get(MartialCommandEldritchMasteryTuning.Setting.RADIUS, settings.hitRadius),
                tuning.get(MartialCommandEldritchMasteryTuning.Setting.HEIGHT, settings.stepHeight));
        charger.setKillFollowUp(tuning.get(MartialCommandEldritchMasteryTuning.Setting.SEARCH_RADIUS, 0),
                tuning.get(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .5),
                tuning.integer(MartialCommandEldritchMasteryTuning.Setting.REFUND_TICKS, 0));
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
                                                  MartialCommandEldritchMasteryTuning tuning) {
        double minimum = Math.max(0.0, tuning.get(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_RADIUS,
                settings.passiveMinRange));
        double maximum = Math.max(minimum + 0.1, tuning.get(MartialCommandEldritchMasteryTuning.Setting.SEARCH_RANGE,
                settings.passiveMaxRange));
        double minimumSquared = minimum * minimum;
        double maximumSquared = maximum * maximum;
        double threshold = Math.cos(Math.toRadians(
                MathHelper.clamp(tuning.get(MartialCommandEldritchMasteryTuning.Setting.ANGLE,
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
