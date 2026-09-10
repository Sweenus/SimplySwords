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
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
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
    private static final Map<ServerWorld, Map<UUID, Long>> LAST_CLEAVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, StrideTracking>> TRACKING = new HashMap<>();
    private static final long STALE_TRACKING_TICKS = 400L;

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
        TRACKING.computeIfAbsent(world, ignored -> new HashMap<>())
                .put(owner.getUuid(), new StrideTracking(execution, now));
        spawnActivation(world, owner);
        return true;
    }

    public static void onSwing(ItemStack stack, ServerWorld world, LivingEntity owner, Hand hand) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
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
                (float) (Math.max(1.0F, HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, owner,
                        active.stackSnapshot,
                        Config.uniqueEffects.soulstalker.strikeDamageScaling,
                        Config.uniqueEffects.soulstalker.strikeSpellScaling))
                        * active.tuning.get(StormSoulMasteryTuning.Setting.CLEAVE_DAMAGE_MULTIPLIER, 1)
                        * meteorCleavePenalty(stride, active)
                        * momentumMultiplier(stride, active)),
                (float) Math.max(0.25, active.tuning.get(StormSoulMasteryTuning.Setting.CLEAVE_INITIAL_WIDTH,
                        Config.uniqueEffects.soulstalker.cleaveInitialWidth)),
                (float) Math.max(0.25, active.tuning.get(StormSoulMasteryTuning.Setting.CLEAVE_FINAL_WIDTH,
                        Config.uniqueEffects.soulstalker.cleaveFinalWidth)),
                active.tuning.integer(StormSoulMasteryTuning.Setting.CLEAVE_TARGET_CAP,
                        Config.uniqueEffects.soulstalker.cleaveTargetCap),
                active.strideId);
        if (!world.spawnEntity(cleave)) {
            cleave.releaseTracking();
            return;
        }
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
    }

    private static boolean isCleaveReady(ServerWorld world, LivingEntity user, ItemStack stack,
                                         StormSoulMasteryTuning tuning) {
        long now = world.getTime();
        Map<UUID, Long> swings = LAST_CLEAVE.computeIfAbsent(world, ignored -> new HashMap<>());
        if (now % 200L == 0L) {
            swings.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        int cooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, user,
                getCleaveCooldownTicks(user, tuning));
        if (RunicSlashManager.isIgnoringAttackReady()) {
            swings.put(user.getUuid(), now + cooldown);
            return true;
        }
        Long nextEligible = swings.get(user.getUuid());
        if (nextEligible != null && now < nextEligible) {
            return false;
        }
        swings.put(user.getUuid(), now + cooldown);
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
        double scaled = 20.0 / value
                * Math.max(0.0, tuning.get(StormSoulMasteryTuning.Setting.CLEAVE_COOLDOWN_MULTIPLIER, 1));
        return Math.max(minimum, (int) Math.ceil(scaled));
    }

    // Predatory Momentum: distance ridden without stopping raises cleave and footfall damage.
    private static double momentumMultiplier(SoulstalkerStrideEntity stride, ActiveStride active) {
        double bonus = active.tuning.get(StormSoulMasteryTuning.Setting.MOMENTUM_BONUS, 0);
        if (bonus <= 0 || stride == null || !stride.isMomentumActive()) return 1.0;
        return 1.0 + bonus;
    }

    private static double meteorCleavePenalty(SoulstalkerStrideEntity stride, ActiveStride active) {
        double penalty = active.tuning.get(StormSoulMasteryTuning.Setting.METEOR_CLEAVE_PENALTY, 1);
        return stride != null && stride.isMeteorPenaltyActive() ? Math.max(0.0, penalty) : 1.0;
    }

    public static void tickHeldPassive(LivingEntity owner, ItemStack stack) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
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
        UniqueAbilityExecution execution = UniqueAbilityApi.preparePassive(StormSoulMasteryAbilities.SOULSTALKER_TENDRIL,
                UniqueAbilityContext.passive(world, stack, owner, null, heldHand(owner, stack)), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
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
        float baseDamage = (float) (Math.max(1.0F, HelperMethods.abilityScaledDamage(
                SpellScalingProfile.SOUL, owner, stack,
                Config.uniqueEffects.soulstalker.strikeDamageScaling,
                Config.uniqueEffects.soulstalker.strikeSpellScaling))
                * tuning.get(StormSoulMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1));
        Volley volley = new Volley(execution);
        for (LivingEntity target : targets) {
            SoulstalkerTentacleVisualEntity visual = new SoulstalkerTentacleVisualEntity(
                    world, owner, target, PASSIVE_IMPACT_DELAY, PASSIVE_VISUAL_LIFETIME);
            if (!world.spawnEntity(visual)) {
                continue;
            }
            volley.outstanding++;
            volley.reserved.add(target.getUuid());
            PENDING_STRIKES.computeIfAbsent(world, ignored -> new ArrayList<>())
                    .add(new PendingStrike(owner.getUuid(), target.getUuid(), visual.getUuid(),
                            stack.copy(), baseDamage, now + PASSIVE_IMPACT_DELAY, tuning,
                            volley, target.getPos()));
        }
        if (volley.outstanding == 0) {
            UniqueAbilityApi.cancel(execution);
            return;
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
    }

    public static boolean hasActive(ServerWorld world) {
        return !ACTIVE.getOrDefault(world, Map.of()).isEmpty()
                || !PENDING_STRIKES.getOrDefault(world, List.of()).isEmpty()
                || !TRACKING.getOrDefault(world, Map.of()).isEmpty()
                || world.getTime() % 40L == 0L;
    }

    public static void retainStride(ServerWorld world, UUID ownerId) {
        StrideTracking tracking = TRACKING.getOrDefault(world, Map.of()).get(ownerId);
        if (tracking == null) {
            return;
        }
        tracking.outstanding++;
        tracking.touchedAt = world.getTime();
    }

    public static void releaseStride(ServerWorld world, UUID ownerId) {
        StrideTracking tracking = TRACKING.getOrDefault(world, Map.of()).get(ownerId);
        if (tracking == null) {
            return;
        }
        tracking.outstanding = Math.max(0, tracking.outstanding - 1);
        tracking.touchedAt = world.getTime();
        settleStride(world, ownerId, tracking);
    }

    public static void reportStrideHit(ServerWorld world, LivingEntity owner,
                                       LivingEntity target, float damage) {
        if (world == null || owner == null || target == null) {
            return;
        }
        StrideTracking tracking = TRACKING.getOrDefault(world, Map.of()).get(owner.getUuid());
        if (tracking == null) {
            return;
        }
        tracking.hits++;
        tracking.touchedAt = world.getTime();
        UniqueAbilityApi.emit(tracking.execution, UniqueAbilityPhase.HIT,
                StormSoulMasteryAbilities.HIT, target, 1, damage);
    }

    private static void settleStride(ServerWorld world, UUID ownerId, StrideTracking tracking) {
        if (!tracking.completed || tracking.outstanding > 0) {
            return;
        }
        Map<UUID, StrideTracking> tracked = TRACKING.get(world);
        if (tracked != null) {
            tracked.remove(ownerId);
            if (tracked.isEmpty()) {
                TRACKING.remove(world);
            }
        }
        UniqueAbilityApi.finish(tracking.execution, StormSoulMasteryAbilities.FINISH, tracking.hits);
    }

    private static void sweepTracking(ServerWorld world) {
        Map<UUID, StrideTracking> tracked = TRACKING.get(world);
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<Map.Entry<UUID, StrideTracking>> iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, StrideTracking> entry = iterator.next();
            StrideTracking tracking = entry.getValue();
            if (!tracking.completed || now - tracking.touchedAt < STALE_TRACKING_TICKS) {
                continue;
            }
            iterator.remove();
            UniqueAbilityApi.finish(tracking.execution, StormSoulMasteryAbilities.FINISH, tracking.hits);
        }
        if (tracked.isEmpty()) {
            TRACKING.remove(world);
        }
    }

    public static void tick(ServerWorld world) {
        tickStrides(world);
        tickPendingStrikes(world);
        if (world.getTime() % 40L == 0L) {
            purgeOrphans(world);
            sweepTracking(world);
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
            try (var ignored = CombatProvenanceApi.scope(
                    strike.volley.execution.provenance())) {
            observeTarget(world, strike);
            if (now < strike.impactTick) {
                continue;
            }
            LivingEntity owner = resolveLiving(world, strike.ownerId);
            LivingEntity target = resolveLiving(world, strike.targetId);
            double range = Math.max(1.0, strike.tuning.get(StormSoulMasteryTuning.Setting.TENDRIL_RANGE,
                    Config.uniqueEffects.soulstalker.passiveRange)) + 2.0;
            if (owner != null && target == null && strike.targetDied) {
                target = redirectStrike(world, owner, strike);
                if (target != null) {
                    strike.redirected = true;
                    strike.volley.reserved.add(target.getUuid());
                    retargetVisual(world, strike.visualId, target);
                }
            }
            if (owner != null && target != null
                    && HelperMethods.isHoldingItem(ItemsRegistry.SOULSTALKER.get(), owner)
                    && (strike.redirected || owner.squaredDistanceTo(target) <= range * range)
                    && owner.canSee(target) && HelperMethods.checkAbilityTarget(target, owner)
                    && strike.volley.victims.add(target.getUuid())) {
                float damage = strike.damage;
                double gloamBonus = strike.tuning.get(StormSoulMasteryTuning.Setting.GLOAM_DAMAGE_BONUS, 0);
                if (gloamBonus > 0 && target.isOnGround() && GloamStainManager.isOnAnyGloam(world, target)) {
                    damage *= (float) (1.0 + gloamBonus);
                }
                if (SimplySwordsAPI.applyEntityWeaponHit(strike.stack, target, owner, damage)) {
                    createImpactStain(world, owner, target.getPos(), strike.tuning);
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
                    strike.volley.hits++;
                    UniqueAbilityApi.emit(strike.volley.execution, UniqueAbilityPhase.HIT,
                            StormSoulMasteryAbilities.HIT, target, 1, damage);
                }
            } else {
                discard(world, strike.visualId);
            }
            settleVolley(strike.volley);
            iterator.remove();
            }
        }
        if (pending.isEmpty()) {
            PENDING_STRIKES.remove(world);
        }
    }

    private static void observeTarget(ServerWorld world, PendingStrike strike) {
        Entity entity = strike.targetId == null ? null : world.getEntity(strike.targetId);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (living.isAlive() && !living.isRemoved()) {
            strike.lastKnownPosition = living.getPos();
        } else {
            strike.targetDied = true;
        }
    }

    private static void settleVolley(Volley volley) {
        volley.outstanding = Math.max(0, volley.outstanding - 1);
        if (volley.outstanding > 0 || volley.finished) {
            return;
        }
        volley.finished = true;
        if (volley.hits > 0) {
            UniqueAbilityApi.finish(volley.execution, StormSoulMasteryAbilities.FINISH, volley.hits);
        } else {
            UniqueAbilityApi.cancel(volley.execution);
        }
    }

    private static void retargetVisual(ServerWorld world, UUID visualId, LivingEntity target) {
        Entity entity = visualId == null ? null : world.getEntity(visualId);
        if (entity instanceof SoulstalkerTentacleVisualEntity visual) {
            visual.setTarget(target);
        }
    }

    private static List<LivingEntity> findPassiveTargets(ServerWorld world, LivingEntity owner,
                                                        double configuredRange, int count,
                                                        boolean lowestHealth) {
        double range = Math.max(1.0, configuredRange);
        Box search = owner.getBoundingBox().expand(range);
        List<LivingEntity> candidates = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && owner.squaredDistanceTo(entity) <= range * range
                        && owner.canSee(entity) && HelperMethods.checkAbilityTarget(entity, owner)));
        candidates.sort(lowestHealth
                ? Comparator.comparingDouble(LivingEntity::getHealth)
                        .thenComparingDouble(owner::squaredDistanceTo)
                : Comparator.comparingDouble(owner::squaredDistanceTo));
        int limit = lowestHealth ? 1 : Math.max(1, count);
        return candidates.size() > limit ? new ArrayList<>(candidates.subList(0, limit)) : candidates;
    }

    // Seeking Root: a tendril whose target died lands on the nearest enemy instead.
    private static LivingEntity redirectStrike(ServerWorld world, LivingEntity owner, PendingStrike strike) {
        double range = strike.tuning.get(StormSoulMasteryTuning.Setting.SEEKING_ROOT_RANGE, 0);
        if (range <= 0) return null;
        Vec3d origin = strike.lastKnownPosition == null ? owner.getPos() : strike.lastKnownPosition;
        Box search = Box.of(origin, range * 2.0, range * 2.0, range * 2.0);
        return world.getEntitiesByClass(LivingEntity.class, search,
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                                && !strike.volley.reserved.contains(entity.getUuid())
                                && entity.squaredDistanceTo(origin.x, origin.y, origin.z) <= range * range
                                && HelperMethods.checkAbilityTarget(entity, owner)
                                && owner.canSee(entity))
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
        SoulstalkerGloam.createPatch(world, owner.getUuid(), tuning, point, radius, 0);
    }

    private static void createImpactStain(ServerWorld world, LivingEntity owner, Vec3d position,
                                          StormSoulMasteryTuning tuning) {
        SoulstalkerGloam.createPatch(world, owner.getUuid(), tuning, position,
                Math.max(0.25, Config.uniqueEffects.soulstalker.stainRadius), 0);
    }

    private static void finishStride(ServerWorld world, LivingEntity owner, SoulstalkerStrideEntity stride,
                                     ActiveStride state) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(state == null || state.execution == null ? null : state.execution.provenance())) {
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
        StrideTracking tracking = TRACKING.getOrDefault(world, Map.of()).get(state.ownerId);
        if (tracking == null) {
            UniqueAbilityApi.finish(state.execution, StormSoulMasteryAbilities.FINISH, 0);
            return;
        }
        tracking.completed = true;
        tracking.touchedAt = world.getTime();
        settleStride(world, state.ownerId, tracking);
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
            }
        }
        Map<UUID, StrideTracking> tracked = TRACKING.remove(world);
        if (tracked != null) {
            for (StrideTracking tracking : tracked.values()) UniqueAbilityApi.cancel(tracking.execution);
        }
        List<PendingStrike> pending = PENDING_STRIKES.remove(world);
        if (pending != null) {
            for (PendingStrike strike : pending) {
                discard(world, strike.visualId);
                cancelVolley(strike.volley);
            }
        }
        LAST_PASSIVE_CHECK.remove(world);
        PASSIVE_LOCKOUT.remove(world);
        SNARE_LOCKOUT.remove(world);
        LAST_CLEAVE.remove(world);
    }

    public static void clearAll() {
        for (Map<UUID, StrideTracking> tracked : TRACKING.values()) {
            for (StrideTracking tracking : tracked.values()) UniqueAbilityApi.cancel(tracking.execution);
        }
        for (List<PendingStrike> pending : PENDING_STRIKES.values()) {
            for (PendingStrike strike : pending) cancelVolley(strike.volley);
        }
        ACTIVE.clear();
        TRACKING.clear();
        PENDING_STRIKES.clear();
        LAST_PASSIVE_CHECK.clear();
        PASSIVE_LOCKOUT.clear();
        SNARE_LOCKOUT.clear();
        LAST_CLEAVE.clear();
    }

    public static void clearActor(LivingEntity owner) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)
                || ACTIVE.isEmpty() && PENDING_STRIKES.isEmpty() && TRACKING.isEmpty()
                && LAST_PASSIVE_CHECK.isEmpty() && PASSIVE_LOCKOUT.isEmpty() && LAST_CLEAVE.isEmpty()) {
            return;
        }
        UUID ownerId = owner.getUuid();
        Map<UUID, ActiveStride> active = ACTIVE.get(world);
        ActiveStride state = active == null ? null : active.remove(ownerId);
        if (state != null) {
            SoulstalkerStrideEntity stride = resolveStride(world, state.strideId);
            if (stride != null) {
                stride.removeAllPassengers();
                stride.discard();
            }
            if (active.isEmpty()) {
                ACTIVE.remove(world);
            }
        }
        Map<UUID, StrideTracking> tracked = TRACKING.get(world);
        StrideTracking tracking = tracked == null ? null : tracked.remove(ownerId);
        if (tracking != null) {
            UniqueAbilityApi.cancel(tracking.execution);
            if (tracked.isEmpty()) {
                TRACKING.remove(world);
            }
        }
        List<PendingStrike> pending = PENDING_STRIKES.get(world);
        if (pending != null) {
            Iterator<PendingStrike> iterator = pending.iterator();
            while (iterator.hasNext()) {
                PendingStrike strike = iterator.next();
                if (!ownerId.equals(strike.ownerId)) {
                    continue;
                }
                discard(world, strike.visualId);
                cancelVolley(strike.volley);
                iterator.remove();
            }
            if (pending.isEmpty()) {
                PENDING_STRIKES.remove(world);
            }
        }
        removeOwner(LAST_PASSIVE_CHECK, world, ownerId);
        removeOwner(PASSIVE_LOCKOUT, world, ownerId);
        removeOwner(LAST_CLEAVE, world, ownerId);
    }

    private static void removeOwner(Map<ServerWorld, Map<UUID, Long>> source,
                                    ServerWorld world, UUID ownerId) {
        Map<UUID, Long> entries = source.get(world);
        if (entries == null) {
            return;
        }
        entries.remove(ownerId);
        if (entries.isEmpty()) {
            source.remove(world);
        }
    }

    private static void cancelVolley(Volley volley) {
        if (volley == null || volley.finished) {
            return;
        }
        volley.finished = true;
        volley.outstanding = 0;
        UniqueAbilityApi.cancel(volley.execution);
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

    private static final class PendingStrike {
        private final UUID ownerId;
        private final UUID targetId;
        private final UUID visualId;
        private final ItemStack stack;
        private final float damage;
        private final long impactTick;
        private final StormSoulMasteryTuning tuning;
        private final Volley volley;
        private Vec3d lastKnownPosition;
        private boolean targetDied;
        private boolean redirected;

        private PendingStrike(UUID ownerId, UUID targetId, UUID visualId, ItemStack stack,
                              float damage, long impactTick, StormSoulMasteryTuning tuning,
                              Volley volley, Vec3d lastKnownPosition) {
            this.ownerId = ownerId;
            this.targetId = targetId;
            this.visualId = visualId;
            this.stack = stack;
            this.damage = damage;
            this.impactTick = impactTick;
            this.tuning = tuning;
            this.volley = volley;
            this.lastKnownPosition = lastKnownPosition;
        }
    }

    private static final class Volley {
        private final UniqueAbilityExecution execution;
        private final Set<UUID> reserved = new HashSet<>();
        private final Set<UUID> victims = new HashSet<>();
        private int outstanding;
        private int hits;
        private boolean finished;

        private Volley(UniqueAbilityExecution execution) {
            this.execution = execution;
        }
    }

    private static final class StrideTracking {
        private final UniqueAbilityExecution execution;
        private int outstanding;
        private int hits;
        private boolean completed;
        private long touchedAt;

        private StrideTracking(UniqueAbilityExecution execution, long touchedAt) {
            this.execution = execution;
            this.touchedAt = touchedAt;
        }
    }
}
