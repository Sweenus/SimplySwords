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
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
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
    private static final Map<ServerWorld, Map<UUID, Long>> SNARE_LOCKOUT = new HashMap<>();
    private static final Map<UUID, Long> LAST_CLEAVE = new HashMap<>();

    private static final int MODE_HUNGERING = 16;

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
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.SOULSTALKER_STRIDE,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY)
                        .set(StormSoulMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.soulstalker.cooldown));
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
        Hand hand = context.hand() == null ? Hand.MAIN_HAND : context.hand();
        ItemStack heldStack = owner.getStackInHand(hand);
        SoulstalkerStrideEntity stride = new SoulstalkerStrideEntity(EntityRegistry.SOULSTALKER_STRIDE.get(), world);
        stride.setOwner(owner);
        stride.setTuning(tuning);
        stride.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), owner.getYaw(), 0.0F);
        EntityAttributeInstance movement = stride.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        EntityAttributeInstance step = stride.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (movement != null) {
            movement.setBaseValue(MathHelper.clamp(tuning.get(StormSoulMasteryTuning.Setting.MOVEMENT_SPEED,
                    Config.uniqueEffects.soulstalker.movementSpeed), 0.05, 1.0));
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
                now + Math.max(20, tuning.integer(StormSoulMasteryTuning.Setting.STRIDE_DURATION_TICKS,
                        Config.uniqueEffects.soulstalker.duration)),
                context.activationSource() == WeaponAbilityActivationSource.MOB ? now : Long.MIN_VALUE,
                owner.getPos(), tuning, execution);
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
        if (!isCleaveReady(world, owner, active.stackSnapshot, active.tuning)) {
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
                Math.max(1.0, active.tuning.get(StormSoulMasteryTuning.Setting.CLEAVE_RANGE,
                        Config.uniqueEffects.soulstalker.cleaveRange)),
                Math.max(0.05, Config.uniqueEffects.soulstalker.cleaveSpeed),
                (float) Math.max(1.0F, HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, owner, active.stackSnapshot,
                        Config.uniqueEffects.soulstalker.strikeDamageScaling,
                        Config.uniqueEffects.soulstalker.strikeSpellScaling)
                        * active.tuning.get(StormSoulMasteryTuning.Setting.CLEAVE_DAMAGE_MULTIPLIER, 1)
                        * momentumMultiplier(world, owner, active)),
                (float) Math.max(0.25, Config.uniqueEffects.soulstalker.cleaveInitialWidth),
                (float) Math.max(0.25, active.tuning.get(StormSoulMasteryTuning.Setting.CLEAVE_FINAL_WIDTH,
                        Config.uniqueEffects.soulstalker.cleaveFinalWidth)),
                active.tuning.integer(StormSoulMasteryTuning.Setting.CLEAVE_TARGET_CAP,
                        Config.uniqueEffects.soulstalker.cleaveTargetCap),
                active.strideId);
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

    private static boolean isCleaveReady(ServerWorld world, LivingEntity user, ItemStack stack,
                                         StormSoulMasteryTuning tuning) {
        long now = world.getTime();
        if (now % 200L == 0L) {
            LAST_CLEAVE.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        int cooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, user,
                getCleaveCooldownTicks(user, tuning));
        if (RunicSlashManager.isIgnoringAttackReady()) {
            LAST_CLEAVE.put(user.getUuid(), now + cooldown);
            return true;
        }
        Long nextEligible = LAST_CLEAVE.get(user.getUuid());
        if (nextEligible != null && now < nextEligible) {
            return false;
        }
        LAST_CLEAVE.put(user.getUuid(), now + cooldown);
        return true;
    }

    private static int getCleaveCooldownTicks(LivingEntity user, StormSoulMasteryTuning tuning) {
        EntityAttributeInstance attackSpeed = user.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        double value = attackSpeed == null ? 4.0 : attackSpeed.getValue();
        if (value <= 0.0) {
            value = 4.0;
        }
        int minimum = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.CLEAVE_SWING_COOLDOWN_TICKS,
                Config.uniqueEffects.soulstalker.cleaveMinimumSwingCooldownTicks));
        return Math.max(minimum, (int) Math.ceil(20.0 / value));
    }

    // Predatory Momentum: distance ridden without stopping raises cleave and footfall damage.
    private static double momentumMultiplier(ServerWorld world, LivingEntity owner, ActiveStride active) {
        double required = active.tuning.get(StormSoulMasteryTuning.Setting.MOMENTUM_DISTANCE, 0);
        double bonus = active.tuning.get(StormSoulMasteryTuning.Setting.MOMENTUM_BONUS, 0);
        if (required <= 0 || bonus <= 0) return 1.0;
        return active.momentum >= required ? 1.0 + bonus : 1.0;
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
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.SOULSTALKER_TENDRIL,
                UniqueAbilityContext.passive(world, stack, owner, null, heldHand(owner, stack)), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
        int interval = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.INTERVAL_TICKS,
                Config.uniqueEffects.soulstalker.passiveCheckInterval));
        if (Math.floorMod(now + owner.getId(), interval) != 0L) {
            UniqueAbilityApi.cancel(execution);
            return;
        }
        Map<UUID, Long> lockouts = PASSIVE_LOCKOUT.computeIfAbsent(world, ignored -> new HashMap<>());
        if (now < lockouts.getOrDefault(owner.getUuid(), Long.MIN_VALUE)) {
            UniqueAbilityApi.cancel(execution);
            return;
        }
        int chance = Math.clamp(tuning.integer(StormSoulMasteryTuning.Setting.CHANCE,
                Config.uniqueEffects.soulstalker.passiveChance), 0, 100);
        int roll = owner.getRandom().nextInt(100);
        boolean passed = chance > 0 && roll < chance;
        UniqueAbilityApi.reportRoll(owner, StormSoulMasteryAbilities.SOULSTALKER_TENDRIL.id(),
                "CHANCE", chance, roll, passed);
        if (!passed) {
            UniqueAbilityApi.cancel(execution);
            return;
        }
        double range = tuning.get(StormSoulMasteryTuning.Setting.TENDRIL_RANGE,
                Config.uniqueEffects.soulstalker.passiveRange);
        int count = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.TENDRIL_COUNT, 1));
        List<LivingEntity> targets = findPassiveTargets(world, owner, range, count,
                (tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0) & MODE_HUNGERING) != 0);
        if (targets.isEmpty()) {
            UniqueAbilityApi.cancel(execution);
            return;
        }
        float baseDamage = (float) Math.max(1.0F, HelperMethods.abilityScaledDamage(
                SpellScalingProfile.SOUL, owner, stack,
                Config.uniqueEffects.soulstalker.strikeDamageScaling,
                Config.uniqueEffects.soulstalker.strikeSpellScaling)
                * tuning.get(StormSoulMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1));
        for (LivingEntity target : targets) {
            SoulstalkerTentacleVisualEntity visual = new SoulstalkerTentacleVisualEntity(
                    world, owner, target, PASSIVE_IMPACT_DELAY, PASSIVE_VISUAL_LIFETIME);
            world.spawnEntity(visual);
            PENDING_STRIKES.computeIfAbsent(world, ignored -> new ArrayList<>())
                    .add(new PendingStrike(owner.getUuid(), target.getUuid(), visual.getUuid(),
                            stack.copy(), baseDamage, now + PASSIVE_IMPACT_DELAY, tuning,
                            target == targets.getFirst() ? execution : null));
        }
        lockouts.put(owner.getUuid(), now + SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                stack, owner, tuning.integer(StormSoulMasteryTuning.Setting.TENDRIL_LOCKOUT_TICKS,
                        Config.uniqueEffects.soulstalker.passiveLockout)));
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
                finishStride(world, owner, stride, state);
                iterator.remove();
                continue;
            }
            trackMomentum(stride, state);
            tickRiftStride(world, owner, stride, state);
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
            double range = Math.max(1.0, strike.tuning.get(StormSoulMasteryTuning.Setting.TENDRIL_RANGE,
                    Config.uniqueEffects.soulstalker.passiveRange)) + 2.0;
            if (owner != null && (target == null || !target.isAlive())) {
                target = redirectStrike(world, owner, strike);
            }
            if (owner != null && target != null
                    && HelperMethods.isHoldingItem(ItemsRegistry.SOULSTALKER.get(), owner)
                    && owner.squaredDistanceTo(target) <= range * range
                    && owner.canSee(target) && HelperMethods.checkAbilityTarget(target, owner)) {
                float damage = strike.damage;
                double gloamBonus = strike.tuning.get(StormSoulMasteryTuning.Setting.GLOAM_DAMAGE_BONUS, 0);
                if (gloamBonus > 0 && GloamStainManager.isOnOwnerGloam(world, owner.getUuid(), target)) {
                    damage *= (float) (1.0 + gloamBonus);
                }
                if (SimplySwordsAPI.applyEntityWeaponHit(strike.stack, target, owner, damage)) {
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
                    int statusTicks = strike.tuning.integer(StormSoulMasteryTuning.Setting.TENDRIL_SLOW_TICKS, 0);
                    if (statusTicks > 0) target.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS, statusTicks, 0), owner);
                    applySnare(world, owner, target, strike.tuning);
                    if (!target.isAlive()) refundTendrilLockout(world, owner, strike.tuning);
                    if (strike.execution != null) {
                        UniqueAbilityApi.emit(strike.execution, UniqueAbilityPhase.HIT,
                                StormSoulMasteryAbilities.HIT, target, 1, damage);
                        UniqueAbilityApi.finish(strike.execution, StormSoulMasteryAbilities.FINISH, 1);
                    }
                } else if (strike.execution != null) UniqueAbilityApi.cancel(strike.execution);
            } else {
                discard(world, strike.visualId);
                if (strike.execution != null) UniqueAbilityApi.cancel(strike.execution);
            }
            iterator.remove();
        }
        if (pending.isEmpty()) {
            PENDING_STRIKES.remove(world);
        }
    }

    private static List<LivingEntity> findPassiveTargets(ServerWorld world, LivingEntity owner,
                                                        double configuredRange, int count,
                                                        boolean lowestHealth) {
        double range = Math.max(1.0, configuredRange);
        Box search = owner.getBoundingBox().expand(range, Math.max(2.0, range * 0.65), range);
        List<LivingEntity> candidates = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && owner.squaredDistanceTo(entity) <= range * range
                        && owner.canSee(entity) && HelperMethods.checkAbilityTarget(entity, owner)));
        candidates.sort(lowestHealth
                ? Comparator.comparingDouble(LivingEntity::getHealth)
                : Comparator.comparingDouble(owner::squaredDistanceTo));
        int limit = lowestHealth ? 1 : Math.max(1, count);
        return candidates.size() > limit ? new ArrayList<>(candidates.subList(0, limit)) : candidates;
    }

    // Seeking Root: a tendril whose target died lands on the nearest enemy instead.
    private static LivingEntity redirectStrike(ServerWorld world, LivingEntity owner, PendingStrike strike) {
        double range = strike.tuning.get(StormSoulMasteryTuning.Setting.SEEKING_ROOT_RANGE, 0);
        if (range <= 0) return null;
        Entity previous = strike.targetId == null ? null : world.getEntity(strike.targetId);
        Vec3d origin = previous == null ? owner.getPos() : previous.getPos();
        Box search = Box.of(origin, range * 2.0, range * 2.0, range * 2.0);
        return world.getEntitiesByClass(LivingEntity.class, search,
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                                && entity.squaredDistanceTo(origin.x, origin.y, origin.z) <= range * range
                                && HelperMethods.checkAbilityTarget(entity, owner))
                .stream()
                .min(Comparator.comparingDouble(entity ->
                        entity.squaredDistanceTo(origin.x, origin.y, origin.z)))
                .orElse(null);
    }

    // Tangled Prey: a heavy, short snare with its own per-enemy lockout.
    private static void applySnare(ServerWorld world, LivingEntity owner, LivingEntity target,
                                   StormSoulMasteryTuning tuning) {
        int ticks = tuning.integer(StormSoulMasteryTuning.Setting.SNARE_SLOW_TICKS, 0);
        if (ticks <= 0 || !target.isAlive()) return;
        Map<UUID, Long> snares = SNARE_LOCKOUT.computeIfAbsent(world, ignored -> new HashMap<>());
        long now = world.getTime();
        if (now < snares.getOrDefault(target.getUuid(), Long.MIN_VALUE)) return;
        snares.put(target.getUuid(), now + Math.max(1,
                tuning.integer(StormSoulMasteryTuning.Setting.SNARE_LOCKOUT_TICKS, 80)));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks,
                Math.max(0, tuning.integer(StormSoulMasteryTuning.Setting.SNARE_SLOW_AMPLIFIER, 0)),
                false, true, true), owner);
    }

    // Hungering Tendril: a kill returns part of the tendril lockout.
    private static void refundTendrilLockout(ServerWorld world, LivingEntity owner,
                                             StormSoulMasteryTuning tuning) {
        int refund = tuning.integer(StormSoulMasteryTuning.Setting.TENDRIL_REFUND_TICKS, 0);
        if (refund <= 0) return;
        Map<UUID, Long> lockouts = PASSIVE_LOCKOUT.get(world);
        if (lockouts == null) return;
        Long next = lockouts.get(owner.getUuid());
        if (next == null || next <= world.getTime()) return;
        lockouts.put(owner.getUuid(), Math.max(world.getTime(), next - refund));
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
        createTunedPatch(world, owner, point,
                Math.max(0.25, state.tuning.get(StormSoulMasteryTuning.Setting.TRAIL_STAIN_WIDTH,
                        Config.uniqueEffects.soulstalker.stainTrailWidth) * 0.72), state.tuning);
        state.lastTrailPoint = stride.getPos();
        world.spawnParticles(GLOAM_DUST, point.x, point.y + 0.08, point.z,
                3, 0.2, 0.05, 0.2, 0.01);
    }

    private static void createTunedPatch(ServerWorld world, LivingEntity owner, Vec3d point,
                                         double radius, StormSoulMasteryTuning tuning) {
        int slowAmplifier = Math.clamp(tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_SLOW_AMPLIFIER,
                Config.uniqueEffects.soulstalker.stainSlowAmplifier), 0, 4);
        int slowTicks = tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_SLOW_DURATION_TICKS, 0);
        GloamStainManager.PatchBehavior behavior = slowTicks > 0
                ? new GloamStainManager.PatchBehavior(ItemStack.EMPTY, 0, slowTicks, true, 0, 0, 0, 0, 0)
                : GloamStainManager.PatchBehavior.NONE;
        GloamStainManager.createPatch(world, owner.getUuid(), point, radius,
                Math.max(20, tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_STAIN_DURATION_TICKS,
                        Config.uniqueEffects.soulstalker.stainDuration)),
                Math.max(1, Config.uniqueEffects.soulstalker.stainFadeDuration),
                slowAmplifier, behavior);
    }

    private static void trackMomentum(SoulstalkerStrideEntity stride, ActiveStride state) {
        if (state.tuning.get(StormSoulMasteryTuning.Setting.MOMENTUM_DISTANCE, 0) <= 0) return;
        Vec3d position = stride.getPos();
        if (state.lastMomentumPoint == null) {
            state.lastMomentumPoint = position;
            return;
        }
        double moved = position.distanceTo(state.lastMomentumPoint);
        state.lastMomentumPoint = position;
        state.momentum = moved < 0.02 ? 0.0 : state.momentum + moved;
        stride.setMomentumActive(
                state.momentum >= state.tuning.get(StormSoulMasteryTuning.Setting.MOMENTUM_DISTANCE, 0));
    }

    // Rift Stride: a fully charged leap opens a short teleport along the rider's aim.
    private static void tickRiftStride(ServerWorld world, LivingEntity owner,
                                       SoulstalkerStrideEntity stride, ActiveStride state) {
        double range = state.tuning.get(StormSoulMasteryTuning.Setting.RIFT_RANGE, 0);
        if (range <= 0 || !stride.consumeChargedLeap()) return;
        long now = world.getTime();
        if (now < state.riftReady) return;
        state.riftReady = now + Math.max(1,
                state.tuning.integer(StormSoulMasteryTuning.Setting.RIFT_LOCKOUT_TICKS, 60));
        Vec3d aim = owner.getRotationVec(1.0F).multiply(1.0, 0.0, 1.0);
        if (aim.horizontalLengthSquared() < 1.0E-4) return;
        aim = aim.normalize();
        Vec3d from = stride.getPos();
        Vec3d destination = from;
        for (double step = 1.0; step <= range; step += 1.0) {
            Vec3d candidate = from.add(aim.multiply(step));
            Box box = stride.getBoundingBox().offset(candidate.subtract(from));
            if (!world.isSpaceEmpty(stride, box)) break;
            destination = candidate;
        }
        if (destination.squaredDistanceTo(from) < 1.0) return;
        stride.refreshPositionAfterTeleport(destination.x, destination.y, destination.z);
        double stainRadius = state.tuning.get(StormSoulMasteryTuning.Setting.RIFT_STAIN_RADIUS, 0);
        if (stainRadius > 0) {
            double y = LivyatanWaveManager.findGroundTopY(world, from.x, from.z, from.y + 1.5);
            createTunedPatch(world, owner, new Vec3d(from.x, y, from.z), stainRadius, state.tuning);
            double toY = LivyatanWaveManager.findGroundTopY(world,
                    destination.x, destination.z, destination.y + 1.5);
            createTunedPatch(world, owner, new Vec3d(destination.x, toY, destination.z),
                    stainRadius, state.tuning);
        }
        world.spawnParticles(GLOAM_DUST, destination.x, destination.y + 0.6, destination.z,
                28, 0.6, 0.5, 0.6, 0.06);
        world.playSound(null, stride.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                SoundCategory.PLAYERS, 0.6F, 1.1F);
    }

    private static void createImpactStain(ServerWorld world, LivingEntity owner, Vec3d position) {
        double y = LivyatanWaveManager.findGroundTopY(world, position.x, position.z, position.y + 1.5);
        GloamStainManager.createPatch(world, owner.getUuid(),
                new Vec3d(position.x, y, position.z),
                Math.max(0.25, Config.uniqueEffects.soulstalker.stainRadius),
                Math.max(20, Config.uniqueEffects.soulstalker.stainDuration),
                Math.max(1, Config.uniqueEffects.soulstalker.stainFadeDuration),
                Math.clamp(Config.uniqueEffects.soulstalker.stainSlowAmplifier, 0, 4));
    }

    private static void finishStride(ServerWorld world, LivingEntity owner, SoulstalkerStrideEntity stride,
                                     ActiveStride state) {
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
        UniqueAbilityApi.finish(state.execution, StormSoulMasteryAbilities.FINISH, 0);
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

    private static Hand heldHand(LivingEntity owner, ItemStack stack) {
        if (owner.getMainHandStack() == stack) return Hand.MAIN_HAND;
        if (owner.getOffHandStack() == stack) return Hand.OFF_HAND;
        return null;
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

    public static void clear(ServerWorld world) {
        Map<UUID, ActiveStride> active = ACTIVE.remove(world);
        if (active != null) {
            for (ActiveStride state : active.values()) {
                SoulstalkerStrideEntity stride = resolveStride(world, state.strideId);
                if (stride != null) {
                    stride.removeAllPassengers();
                    stride.discard();
                }
                UniqueAbilityApi.cancel(state.execution);
            }
        }
        List<PendingStrike> pending = PENDING_STRIKES.remove(world);
        if (pending != null) {
            for (PendingStrike strike : pending) {
                discard(world, strike.visualId);
                if (strike.execution != null) UniqueAbilityApi.cancel(strike.execution);
            }
        }
        LAST_PASSIVE_CHECK.remove(world);
        PASSIVE_LOCKOUT.remove(world);
        SNARE_LOCKOUT.remove(world);
    }

    public static void clearAll() {
        for (Map<UUID, ActiveStride> active : ACTIVE.values()) {
            for (ActiveStride state : active.values()) UniqueAbilityApi.cancel(state.execution);
        }
        for (List<PendingStrike> pending : PENDING_STRIKES.values()) {
            for (PendingStrike strike : pending) {
                if (strike.execution != null) UniqueAbilityApi.cancel(strike.execution);
            }
        }
        ACTIVE.clear();
        PENDING_STRIKES.clear();
        LAST_PASSIVE_CHECK.clear();
        PASSIVE_LOCKOUT.clear();
        SNARE_LOCKOUT.clear();
        LAST_CLEAVE.clear();
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
        Map<UUID, Long> snares = SNARE_LOCKOUT.get(world);
        if (snares != null) {
            snares.entrySet().removeIf(entry -> entry.getValue() <= now);
            if (snares.isEmpty()) {
                SNARE_LOCKOUT.remove(world);
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
        private double momentum;
        private Vec3d lastMomentumPoint;
        private long riftReady = Long.MIN_VALUE;
        private long lastCleaveTick = Long.MIN_VALUE;
        private final StormSoulMasteryTuning tuning;
        private final UniqueAbilityExecution execution;

        private ActiveStride(UUID ownerId, UUID strideId, Hand hand,
                             ItemStack stackReference, ItemStack stackSnapshot,
                             long startedAt, long expiresAt, long suppressedSwingTick,
                             Vec3d lastTrailPoint, StormSoulMasteryTuning tuning,
                             UniqueAbilityExecution execution) {
            this.ownerId = ownerId;
            this.strideId = strideId;
            this.hand = hand;
            this.stackReference = stackReference;
            this.stackSnapshot = stackSnapshot;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
            this.suppressedSwingTick = suppressedSwingTick;
            this.lastTrailPoint = lastTrailPoint;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private record PendingStrike(UUID ownerId, UUID targetId, UUID visualId,
                                 ItemStack stack, float damage, long impactTick,
                                 StormSoulMasteryTuning tuning, UniqueAbilityExecution execution) {
    }
}
