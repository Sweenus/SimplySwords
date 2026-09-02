package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase8UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DeathKnellVisualEntity;
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

public final class DeathKnellAbilityManager {

    private static final int TOLL_WINDUP_TICKS = 6;
    private static final int CASCADE_WINDUP_TICKS = 4;
    private static final int TOLL_VISUAL_LIFETIME = 22;
    private static final DustColorTransitionParticleEffect PLAGUE_DUST =
            new DustColorTransitionParticleEffect(
                    new Vector3f(0.34F, 0.92F, 0.20F),
                    new Vector3f(0.02F, 0.34F, 0.28F),
                    1.15F
            );
    private static final List<ConversionMapping> CONVERSIONS = List.of(
            new ConversionMapping(StatusEffects.HASTE, StatusEffects.MINING_FATIGUE, 1, false, true),
            new ConversionMapping(StatusEffects.REGENERATION, StatusEffects.WITHER, 1, false, true),
            new ConversionMapping(StatusEffects.STRENGTH, StatusEffects.WEAKNESS, 1, false, true),
            new ConversionMapping(StatusEffects.SPEED, StatusEffects.SLOWNESS, 1, false, true),
            new ConversionMapping(StatusEffects.INVISIBILITY, StatusEffects.GLOWING, 1, false, true),
            new ConversionMapping(StatusEffects.RESISTANCE, StatusEffects.NAUSEA, 1, false, true),
            new ConversionMapping(StatusEffects.SATURATION, StatusEffects.HUNGER, 1, false, true),
            new ConversionMapping(StatusEffects.FIRE_RESISTANCE, StatusEffects.POISON, 1, false, true),
            new ConversionMapping(StatusEffects.ABSORPTION, StatusEffects.INSTANT_DAMAGE, 2, true, false)
    );

    private static final Map<ServerWorld, Map<FeverKey, FeverState>> FEVER_STATES = new HashMap<>();
    private static final Map<ServerWorld, List<Outbreak>> ACTIVE_OUTBREAKS = new HashMap<>();
    private static final Map<ServerWorld, Map<FeverKey, Long>> INCUBATION_LOCKOUTS = new HashMap<>();
    private static final Map<ServerWorld, Map<FeverKey, Long>> PATIENT_ZERO_LOCKOUTS = new HashMap<>();

    private DeathKnellAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<FeverKey, FeverState> states = FEVER_STATES.get(world);
        List<Outbreak> outbreaks = ACTIVE_OUTBREAKS.get(world);
        return states != null && !states.isEmpty()
                || outbreaks != null && !outbreaks.isEmpty();
    }

    public static boolean isManagedVisual(ServerWorld world, UUID visualId) {
        if (world == null || visualId == null) {
            return false;
        }
        Map<FeverKey, FeverState> states = FEVER_STATES.get(world);
        if (states != null) {
            for (FeverState state : states.values()) {
                if (visualId.equals(state.visualId)) {
                    return true;
                }
            }
        }
        List<Outbreak> outbreaks = ACTIVE_OUTBREAKS.get(world);
        if (outbreaks != null) {
            for (Outbreak outbreak : outbreaks) {
                for (PendingToll pending : outbreak.pendingTolls) {
                    if (visualId.equals(pending.visualId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void tick(ServerWorld world) {
        tickFeverStates(world);
        tickOutbreaks(world);
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack,
                                  LivingEntity reportedAttacker, LivingEntity target) {
        if (world == null
                || stack == null
                || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.TOXIC_LONGSWORD.get())
                || reportedAttacker == null
                || target == null
                || !target.isAlive()) {
            return;
        }

        DelegatedWeaponHitContext delegated = SimplySwordsAPI.getDelegatedWeaponHitContext();
        LivingEntity actor = delegated == null ? reportedAttacker : delegated.actor();
        ServerPlayerEntity sourcePlayer = delegated == null ? null : delegated.owner();
        if (actor == null
                || !actor.isAlive()
                || actor.getWorld() != world
                || target.getWorld() != world
                || !isValidTarget(target, actor, sourcePlayer)) {
            return;
        }

        long now = world.getTime();
        UniqueAbilityExecution pestilenceExecution = Phase8CombatManager.beginPassive(
                Phase8UniqueAbilities.PLAGUE_PESTILENCE, world, stack, actor, target);
        Phase8AbilityTuning pestilence = Phase8UniqueAbilities.tuning(pestilenceExecution);
        UniqueAbilityExecution knellExecution = Phase8CombatManager.beginPassive(
                Phase8UniqueAbilities.PLAGUE_DEATH_KNELL, world, stack, actor, target);
        Phase8AbilityTuning knell = Phase8UniqueAbilities.tuning(knellExecution);
        UniqueAbilityExecution outbreakExecution = Phase8CombatManager.beginPassive(
                Phase8UniqueAbilities.PLAGUE_OUTBREAK, world, stack, actor, target);
        Phase8AbilityTuning outbreakTuning = Phase8UniqueAbilities.tuning(outbreakExecution);
        FeverKey key = new FeverKey(actor.getUuid(), target.getUuid());
        if (pestilence.flag(1 << 8) && PATIENT_ZERO_LOCKOUTS.getOrDefault(world, Map.of())
                .getOrDefault(key, 0L) > now) {
            finishAll(pestilenceExecution, knellExecution, outbreakExecution);
            return;
        }
        List<CarriedAilment> converted = tryConvertEffects(world, actor, target, now, pestilence);
        PendingToll armedToll = findPendingToll(world, key);
        if (armedToll != null) {
            mergeAilments(armedToll.ailments, converted);
            spawnConversionEffects(world, actor, target, converted.size());
            finishAll(pestilenceExecution, knellExecution, outbreakExecution);
            return;
        }

        Map<FeverKey, FeverState> states =
                FEVER_STATES.computeIfAbsent(world, ignored -> new HashMap<>());
        boolean firstFever = !states.containsKey(key);
        FeverState state = states.computeIfAbsent(key, ignored -> new FeverState());
        state.stack = stack.copy();
        state.pestilence = pestilence;
        state.knell = knell;
        state.outbreak = outbreakTuning;
        state.sourcePlayerId = sourcePlayer == null ? null : sourcePlayer.getUuid();
        state.expiresAt = now + feverDuration(
                Config.uniqueEffects.toxic_longsword.feverDuration, pestilence);
        if (firstFever && pestilence.flag(1 << 5)) {
            Map<FeverKey, Long> lockouts = INCUBATION_LOCKOUTS.computeIfAbsent(world, ignored -> new HashMap<>());
            if (lockouts.getOrDefault(key, 0L) <= now) {
                state.expiresAt += pestilence.integer(
                        Phase8AbilityTuning.Setting.PLAGUE_INCUBATION_DURATION_BONUS_TICKS, 80);
                lockouts.put(key, now + pestilence.integer(
                        Phase8AbilityTuning.Setting.PLAGUE_INCUBATION_LOCKOUT_TICKS, 100));
            }
        }
        mergeAilments(state.ailments, converted);

        int feverGain = Math.max(0, Config.uniqueEffects.toxic_longsword.feverPerHit);
        if (!converted.isEmpty()) {
            feverGain += conversionFever(Config.uniqueEffects.toxic_longsword.conversionFeverBonus, pestilence);
            feverGain += pestilence.integer(
                    Phase8AbilityTuning.Setting.PLAGUE_APOTHEOSIS_FEVER_BONUS, 0);
        }
        int threshold = feverThreshold();
        if (pestilence.flag(1 << 8) && state.stacks == 0) {
            feverGain = threshold;
            PATIENT_ZERO_LOCKOUTS.computeIfAbsent(world, ignored -> new HashMap<>())
                    .put(key, now + pestilence.integer(
                            Phase8AbilityTuning.Setting.PLAGUE_PATIENT_ZERO_LOCKOUT_TICKS, 200));
        }
        state.stacks = Math.min(threshold, state.stacks + feverGain);
        if (pestilence.flag(1 << 4)) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    pestilence.integer(Phase8AbilityTuning.Setting.PLAGUE_SYMPTOM_DURATION_TICKS, 30), 0), actor);
        }
        if (pestilence.flag(1 << 6) && criticalCondition(state.stacks, threshold,
                pestilence.get(Phase8AbilityTuning.Setting.PLAGUE_CRITICAL_FEVER_THRESHOLD, .8))) {
            DamageSource source = SimplySwordsAPI.getWeaponDamageSource(actor);
            float bonus = (float) (HelperMethods.getEntityAttackDamage(actor)
                    * (pestilence.get(Phase8AbilityTuning.Setting.PLAGUE_CRITICAL_DAMAGE_MULTIPLIER, 1) - 1));
            WeaponImplicitRegistry.runSuppressed(() -> HelperMethods.damageThroughIframes(target, source, bonus));
        }

        if (state.stacks >= threshold) {
            states.remove(key);
            discardVisual(world, state.visualId);
            startOutbreak(world, key, state, actor, target);
        } else if (state.stacks > 0) {
            updateFeverVisual(world, state, target, threshold);
            spawnFeverGainEffects(world, target, state.stacks, threshold, !converted.isEmpty());
        }
        spawnConversionEffects(world, actor, target, converted.size());

        if (states.isEmpty()) {
            FEVER_STATES.remove(world);
        }
        finishAll(pestilenceExecution, knellExecution, outbreakExecution);
    }

    private static List<CarriedAilment> tryConvertEffects(ServerWorld world, LivingEntity actor,
                                                           LivingEntity target, long now,
                                                           Phase8AbilityTuning tuning) {
        int chance = conversionChance(Config.uniqueEffects.toxic_longsword.chance, tuning);
        if (chance <= 0 || actor.getRandom().nextInt(100) >= chance) {
            return List.of();
        }

        List<CarriedAilment> converted = new ArrayList<>();
        for (ConversionMapping mapping : CONVERSIONS) {
            StatusEffectInstance current = target.getStatusEffect(mapping.positive);
            if (current == null) {
                continue;
            }

            int sourceDuration = current.getDuration();
            int amplifier = Math.max(0, current.getAmplifier() / mapping.amplifierDivisor);
            int appliedDuration = mapping.instant ? 0 : sourceDuration;
            target.removeStatusEffect(mapping.positive);
            target.addStatusEffect(
                    new StatusEffectInstance(mapping.negative, appliedDuration, amplifier),
                    actor
            );

            if (mapping.copyOnToll) {
                long expiresAt = sourceDuration < 0 ? Long.MAX_VALUE : now + Math.max(1, sourceDuration);
                converted.add(new CarriedAilment(mapping.negative, amplifier, expiresAt));
            }
        }
        return converted;
    }

    private static void startOutbreak(ServerWorld world, FeverKey key, FeverState state,
                                      LivingEntity actor, LivingEntity target) {
        int maximumTolls = maximumTolls(Config.uniqueEffects.toxic_longsword.maxCascadeTolls,
                state.outbreak);
        Outbreak outbreak = new Outbreak(
                actor.getUuid(),
                state.sourcePlayerId,
                state.stack.copy(),
                maximumTolls,
                state.knell,
                state.outbreak,
                state.pestilence
        );
        ACTIVE_OUTBREAKS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(outbreak);
        scheduleToll(world, outbreak, key, target, state.ailments, 0,
                outbreak.knell.integer(Phase8AbilityTuning.Setting.PLAGUE_FIRST_TOLL_WINDUP_TICKS,
                        TOLL_WINDUP_TICKS));
        if (outbreak.knell.flag(1 << 16)) {
            scheduleRepeatedToll(world, outbreak, key, target, state.ailments, 0,
                    outbreak.knell.integer(Phase8AbilityTuning.Setting.PLAGUE_FIRST_TOLL_WINDUP_TICKS,
                            TOLL_WINDUP_TICKS)
                            + outbreak.knell.integer(Phase8AbilityTuning.Setting.PLAGUE_PAIRED_DELAY_TICKS, 6),
                    outbreak.knell.get(Phase8AbilityTuning.Setting.PLAGUE_PAIRED_DAMAGE_MULTIPLIER, .65),
                    outbreak.knell.get(Phase8AbilityTuning.Setting.PLAGUE_PAIRED_RADIUS_MULTIPLIER, .8));
        }
    }

    private static void scheduleToll(ServerWorld world, Outbreak outbreak, FeverKey key,
                                     LivingEntity target,
                                     Map<RegistryEntry<StatusEffect>, CarriedAilment> ailments,
                                     int depth, int windupTicks) {
        if (outbreak.scheduledTolls >= outbreak.maximumTolls
                || !outbreak.queuedTargetIds.add(target.getUuid())) {
            return;
        }

        DeathKnellVisualEntity visual = DeathKnellVisualEntity.toll(
                world,
                target,
                (float) tollRadius(Config.uniqueEffects.toxic_longsword.tollRadius, outbreak.knell),
                Math.max(1, windupTicks),
                TOLL_VISUAL_LIFETIME,
                depth
        );
        UUID visualId = world.spawnEntity(visual) ? visual.getUuid() : null;
        outbreak.pendingTolls.add(new PendingToll(
                key,
                target.getUuid(),
                world.getTime() + Math.max(1, windupTicks),
                depth,
                new HashMap<>(ailments),
                visualId
        ));
        outbreak.scheduledTolls++;
    }

    private static void scheduleRepeatedToll(ServerWorld world, Outbreak outbreak, FeverKey key,
                                             LivingEntity target,
                                             Map<RegistryEntry<StatusEffect>, CarriedAilment> ailments,
                                             int depth, int windupTicks, double damageMultiplier,
                                             double radiusMultiplier) {
        DeathKnellVisualEntity visual = DeathKnellVisualEntity.toll(world, target,
                Math.max(.1F, (float) (tollRadius(Config.uniqueEffects.toxic_longsword.tollRadius,
                        outbreak.knell) * radiusMultiplier)), Math.max(1, windupTicks),
                TOLL_VISUAL_LIFETIME, depth);
        UUID visualId = world.spawnEntity(visual) ? visual.getUuid() : null;
        outbreak.pendingTolls.add(new PendingToll(key, target.getUuid(), world.getTime() + Math.max(1, windupTicks),
                depth, new HashMap<>(ailments), visualId, damageMultiplier, radiusMultiplier, true));
    }

    private static void tickFeverStates(ServerWorld world) {
        pruneLockouts(world, INCUBATION_LOCKOUTS);
        pruneLockouts(world, PATIENT_ZERO_LOCKOUTS);
        Map<FeverKey, FeverState> states = FEVER_STATES.get(world);
        if (states == null || states.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<FeverKey, FeverState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<FeverKey, FeverState> entry = iterator.next();
            FeverKey key = entry.getKey();
            FeverState state = entry.getValue();
            Entity actorEntity = world.getEntity(key.actorId);
            Entity targetEntity = world.getEntity(key.targetId);
            if (!(actorEntity instanceof LivingEntity actor)
                    || !(targetEntity instanceof LivingEntity target)
                    || !actor.isAlive()
                    || !target.isAlive()
                    || now >= state.expiresAt) {
                discardVisual(world, state.visualId);
                iterator.remove();
                continue;
            }
            pruneExpiredAilments(state.ailments, now);
            updateFeverVisual(world, state, target, feverThreshold());
        }

        if (states.isEmpty()) {
            FEVER_STATES.remove(world);
        }
    }

    private static void pruneLockouts(ServerWorld world, Map<ServerWorld, Map<FeverKey, Long>> ledgers) {
        Map<FeverKey, Long> entries = ledgers.get(world);
        if (entries == null) return;
        entries.values().removeIf(expiry -> expiry <= world.getTime());
        if (entries.isEmpty()) ledgers.remove(world);
    }

    private static void tickOutbreaks(ServerWorld world) {
        List<Outbreak> outbreaks = ACTIVE_OUTBREAKS.get(world);
        if (outbreaks == null || outbreaks.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Outbreak> outbreakIterator = outbreaks.iterator();
        while (outbreakIterator.hasNext()) {
            Outbreak outbreak = outbreakIterator.next();
            Entity actorEntity = world.getEntity(outbreak.actorId);
            if (!(actorEntity instanceof LivingEntity actor) || !actor.isAlive()) {
                discardPendingVisuals(world, outbreak);
                outbreakIterator.remove();
                continue;
            }

            List<PendingToll> due = new ArrayList<>();
            Iterator<PendingToll> pendingIterator = outbreak.pendingTolls.iterator();
            while (pendingIterator.hasNext()) {
                PendingToll pending = pendingIterator.next();
                if (now >= pending.executeAt) {
                    due.add(pending);
                    pendingIterator.remove();
                }
            }
            due.sort(Comparator.comparingInt(pending -> pending.depth));
            for (PendingToll pending : due) {
                executeToll(world, outbreak, actor, pending);
            }

            Iterator<PendingRelapse> relapseIterator = outbreak.pendingRelapses.iterator();
            while (relapseIterator.hasNext()) {
                PendingRelapse relapse = relapseIterator.next();
                if (now < relapse.at) continue;
                relapseIterator.remove();
                if (world.getEntity(relapse.targetId) instanceof LivingEntity target && target.isAlive()) {
                    addSpreadFever(world, outbreak, actor, resolveSourcePlayer(world, outbreak.sourcePlayerId),
                            target, relapse.fever, 0);
                }
            }

            if (outbreak.pendingTolls.isEmpty() && outbreak.pendingRelapses.isEmpty()) {
                outbreakIterator.remove();
            }
        }

        if (outbreaks.isEmpty()) {
            ACTIVE_OUTBREAKS.remove(world);
        }
    }

    private static void executeToll(ServerWorld world, Outbreak outbreak,
                                    LivingEntity actor, PendingToll pending) {
        Entity targetEntity = world.getEntity(pending.targetId);
        if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
            discardVisual(world, pending.visualId);
            return;
        }

        ServerPlayerEntity sourcePlayer = resolveSourcePlayer(world, outbreak.sourcePlayerId);
        if (!isValidTarget(target, actor, sourcePlayer)) {
            discardVisual(world, pending.visualId);
            return;
        }

        outbreak.tolledTargetIds.add(target.getUuid());
        float radius = (float) (tollRadius(Config.uniqueEffects.toxic_longsword.tollRadius,
                outbreak.knell) * pending.radiusMultiplier);
        boolean quarantine = outbreak.outbreak.flag(1 << 26) && outbreak.executedTolls == 0;
        if (quarantine) radius = (float) outbreak.outbreak.get(
                Phase8AbilityTuning.Setting.PLAGUE_QUARANTINE_RADIUS, 7);
        double candidateRadius = radius * (outbreak.knell.flag(1 << 15)
                ? outbreak.knell.get(Phase8AbilityTuning.Setting.PLAGUE_FINAL_RADIUS_MULTIPLIER, 1.3) : 1);
        double verticalRadius = Math.max(2.0, candidateRadius * 0.65);
        Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        double cascadeRadius = candidateRadius + outbreak.outbreak.get(
                Phase8AbilityTuning.Setting.PLAGUE_CASCADE_RANGE_BONUS, 0);
        Box searchBox = Box.of(center, cascadeRadius * 2.0, verticalRadius * 2.0, cascadeRadius * 2.0);
        List<LivingEntity> cascadeCandidates = world.getEntitiesByClass(LivingEntity.class, searchBox,
                candidate -> candidate.isAlive() && EntityPredicates.VALID_LIVING_ENTITY.test(candidate)
                        && isValidTarget(candidate, actor, sourcePlayer));
        cascadeCandidates.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(center)));
        int feverSpread = feverSpread(Config.uniqueEffects.toxic_longsword.tollFeverSpread,
                outbreak.outbreak);
        boolean finalChime = !pending.secondary && outbreak.knell.flag(1 << 15)
                && outbreak.pendingTolls.stream().noneMatch(other -> !other.secondary)
                && !canSchedulePrimary(world, outbreak, actor, sourcePlayer, target,
                cascadeCandidates, feverSpread);
        if (finalChime) radius *= outbreak.knell.get(
                Phase8AbilityTuning.Setting.PLAGUE_FINAL_RADIUS_MULTIPLIER, 1.3);
        final float pulseRadius = radius;
        List<LivingEntity> affected = world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                candidate -> candidate.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(candidate)
                        && isValidTarget(candidate, actor, sourcePlayer)
                        && isInsidePulse(candidate, center, pulseRadius, verticalRadius)
        );
        affected.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(center)));

        float baseDamage = HelperMethods.abilityScaledDamage(
                "soul", actor, outbreak.stack,
                Math.max(0.0F, Config.uniqueEffects.toxic_longsword.tollDamageScaling),
                Math.max(0.0F, Config.uniqueEffects.toxic_longsword.tollSpellScaling));
        baseDamage *= (float) outbreak.knell.get(
                Phase8AbilityTuning.Setting.PLAGUE_TOLL_DAMAGE_MULTIPLIER, 1);
        baseDamage *= (float) outbreak.pestilence.get(
                Phase8AbilityTuning.Setting.PLAGUE_TOLL_DAMAGE_MULTIPLIER, 1);
        baseDamage *= pending.damageMultiplier;
        if (outbreak.knell.flag(1 << 14)) baseDamage *= 1 + Math.min(
                outbreak.knell.integer(Phase8AbilityTuning.Setting.PLAGUE_CASCADE_STEP_CAP, 5), pending.depth)
                * outbreak.knell.get(Phase8AbilityTuning.Setting.PLAGUE_DAMAGE_PER_CASCADE_STEP, .05);
        if (finalChime) baseDamage *= outbreak.knell.get(
                Phase8AbilityTuning.Setting.PLAGUE_FINAL_DAMAGE_MULTIPLIER, 1.25);
        int targetCap = outbreak.knell.flag(1 << 17)
                ? outbreak.knell.integer(Phase8AbilityTuning.Setting.PLAGUE_SINGLE_TARGET_CAP, 1)
                : Integer.MAX_VALUE;
        int damagedCount = 0;
        for (LivingEntity candidate : affected) {
            if (damagedCount < targetCap && (pending.secondary
                    || outbreak.damagedTargetIds.add(candidate.getUuid()))) {
                float candidateDamage = baseDamage;
                if (candidate.getHealth() / candidate.getMaxHealth() < outbreak.knell.get(
                        Phase8AbilityTuning.Setting.PLAGUE_LOW_HEALTH_THRESHOLD, 0)) {
                    candidateDamage *= outbreak.knell.get(
                            Phase8AbilityTuning.Setting.PLAGUE_LOW_HEALTH_DAMAGE_MULTIPLIER, 1);
                }
                damageTarget(world, actor, outbreak.stack, candidate, candidateDamage);
                if (outbreak.knell.flag(1 << 13)) candidate.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.WEAKNESS, outbreak.knell.integer(
                                Phase8AbilityTuning.Setting.PLAGUE_WEAKNESS_DURATION_TICKS, 40), 0), actor);
                damagedCount++;
                if (candidate != target) {
                    applyCarriedAilments(world, actor, candidate, pending.ailments,
                            outbreak.pestilence, outbreak.outbreak);
                }
            }
            spawnSpreadTrail(world, center, candidate.getPos().add(0.0, candidate.getHeight() * 0.5, 0.0));
        }
        outbreak.executedTolls++;
        if (target.isAlive() && outbreak.outbreak.flag(1 << 23)
                && outbreak.relapsedTargetIds.add(target.getUuid())) {
            outbreak.pendingRelapses.add(new PendingRelapse(target.getUuid(), world.getTime()
                    + outbreak.outbreak.integer(Phase8AbilityTuning.Setting.PLAGUE_RELAPSE_DELAY_TICKS, 40),
                    outbreak.outbreak.integer(Phase8AbilityTuning.Setting.PLAGUE_RELAPSE_FEVER, 1)));
        }
        if (target.isAlive() && outbreak.outbreak.flag(1 << 25)
                && outbreak.revisitedTargetIds.add(target.getUuid())) {
            scheduleRepeatedToll(world, outbreak, pending.key, target, pending.ailments, pending.depth,
                    outbreak.outbreak.integer(Phase8AbilityTuning.Setting.PLAGUE_REVISIT_DELAY_TICKS, 20),
                    outbreak.outbreak.get(Phase8AbilityTuning.Setting.PLAGUE_REVISIT_DAMAGE_MULTIPLIER, .5), 1);
        }
        if (outbreak.outbreak.flag(1 << 22)) {
            double wakeRadius = outbreak.outbreak.get(Phase8AbilityTuning.Setting.PLAGUE_WAKE_RADIUS, 2.5);
            world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(wakeRadius),
                            candidate -> isValidTarget(candidate, actor, sourcePlayer))
                    .stream().sorted(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(target)))
                    .limit(outbreak.outbreak.integer(Phase8AbilityTuning.Setting.PLAGUE_WAKE_TARGET_CAP, 6))
                    .forEach(candidate -> candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,
                            outbreak.outbreak.integer(Phase8AbilityTuning.Setting.PLAGUE_WAKE_DURATION_TICKS, 30),
                            0), actor));
        }

        playTollSound(world, center, pending.depth);
        spawnTollEffects(world, center, radius, pending.depth);

        if (quarantine) {
            affected.stream().limit(outbreak.outbreak.integer(
                            Phase8AbilityTuning.Setting.PLAGUE_QUARANTINE_TARGET_CAP, 12))
                    .forEach(candidate -> addSpreadFever(world, outbreak, actor, sourcePlayer, candidate,
                            outbreak.outbreak.integer(Phase8AbilityTuning.Setting.PLAGUE_QUARANTINE_FEVER, 3), 1));
            return;
        }
        if (outbreak.knell.flag(1 << 17)) return;
        if (feverSpread <= 0) {
            return;
        }
        for (LivingEntity candidate : cascadeCandidates) {
            if (candidate == target
                    || outbreak.tolledTargetIds.contains(candidate.getUuid())
                    || outbreak.queuedTargetIds.contains(candidate.getUuid())) {
                continue;
            }
            addSpreadFever(world, outbreak, actor, sourcePlayer, candidate, feverSpread, pending.depth + 1);
        }
    }

    private static void addSpreadFever(ServerWorld world, Outbreak outbreak,
                                       LivingEntity actor, ServerPlayerEntity sourcePlayer,
                                       LivingEntity target, int feverGain, int depth) {
        FeverKey key = new FeverKey(actor.getUuid(), target.getUuid());
        if (findPendingToll(world, key) != null) {
            return;
        }

        Map<FeverKey, FeverState> states =
                FEVER_STATES.computeIfAbsent(world, ignored -> new HashMap<>());
        FeverState state = states.computeIfAbsent(key, ignored -> new FeverState());
        state.stack = outbreak.stack.copy();
        state.sourcePlayerId = sourcePlayer == null ? null : sourcePlayer.getUuid();
        state.expiresAt = world.getTime() + feverDuration(
                Config.uniqueEffects.toxic_longsword.feverDuration, outbreak.pestilence);
        state.pestilence = outbreak.pestilence;
        state.knell = outbreak.knell;
        state.outbreak = outbreak.outbreak;
        int threshold = feverThreshold();
        state.stacks = Math.min(threshold, state.stacks + feverGain);

        if (state.stacks >= threshold) {
            if (outbreak.scheduledTolls < outbreak.maximumTolls) {
                states.remove(key);
                discardVisual(world, state.visualId);
                scheduleToll(world, outbreak, key, target, state.ailments, depth, CASCADE_WINDUP_TICKS);
            } else {
                state.stacks = Math.max(0, threshold - 1);
                updateFeverVisual(world, state, target, threshold);
            }
        } else if (state.stacks > 0) {
            updateFeverVisual(world, state, target, threshold);
            spawnFeverGainEffects(world, target, state.stacks, threshold, false);
        }

        if (states.isEmpty()) {
            FEVER_STATES.remove(world);
        }
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity actor, ItemStack stack,
                                        LivingEntity target, float baseDamage) {
        DamageSource source = world.getDamageSources().indirectMagic(actor, actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(
                world,
                stack,
                target,
                source,
                baseDamage
        );
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage)
        );
        return damaged[0];
    }

    private static void applyCarriedAilments(ServerWorld world, LivingEntity actor,
                                              LivingEntity target,
                                              Map<RegistryEntry<StatusEffect>, CarriedAilment> ailments,
                                              Phase8AbilityTuning pestilence,
                                              Phase8AbilityTuning outbreak) {
        if (ailments.isEmpty()) {
            return;
        }
        long now = world.getTime();
        float multiplier = carriedDurationMultiplier(
                Config.uniqueEffects.toxic_longsword.copiedAilmentDurationMultiplier,
                pestilence, outbreak);
        if (multiplier <= 0.0F) {
            return;
        }

        for (CarriedAilment ailment : ailments.values()) {
            int duration;
            if (ailment.expiresAt == Long.MAX_VALUE) {
                duration = -1;
            } else {
                long remaining = ailment.expiresAt - now;
                duration = (int) Math.floor(remaining * multiplier);
                if (duration <= 0) {
                    continue;
                }
            }
            target.addStatusEffect(
                    new StatusEffectInstance(
                            ailment.effect,
                            duration,
                            ailment.amplifier,
                            false,
                            true,
                            true
                    ),
                    actor
            );
        }
    }

    private static boolean isInsidePulse(LivingEntity target, Vec3d center,
                                         double radius, double verticalRadius) {
        Vec3d targetCenter = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
        double horizontalRadius = radius + targetRadius;
        double dx = targetCenter.x - center.x;
        double dz = targetCenter.z - center.z;
        return dx * dx + dz * dz <= horizontalRadius * horizontalRadius
                && Math.abs(targetCenter.y - center.y) <= verticalRadius + target.getHeight() * 0.5;
    }

    private static boolean isValidTarget(LivingEntity target, LivingEntity actor,
                                         ServerPlayerEntity sourcePlayer) {
        return target != actor
                && HelperMethods.checkAbilityTarget(target, actor)
                && (sourcePlayer == null
                || target != sourcePlayer
                && HelperMethods.checkAbilityTarget(target, sourcePlayer));
    }

    private static void updateFeverVisual(ServerWorld world, FeverState state,
                                          LivingEntity target, int threshold) {
        Entity existing = state.visualId == null ? null : world.getEntity(state.visualId);
        DeathKnellVisualEntity visual;
        if (existing instanceof DeathKnellVisualEntity deathKnell
                && deathKnell.getMode() == DeathKnellVisualEntity.MODE_FEVER) {
            visual = deathKnell;
        } else {
            visual = DeathKnellVisualEntity.fever(world, target, state.stacks, threshold);
            if (!world.spawnEntity(visual)) {
                state.visualId = null;
                return;
            }
            state.visualId = visual.getUuid();
        }
        visual.setTargetId(target.getId());
        visual.setStacks(state.stacks);
        visual.setMaxStacks(threshold);
        visual.setPosition(target.getX(), target.getY(), target.getZ());
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        if (visualId == null) {
            return;
        }
        Entity visual = world.getEntity(visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static void discardPendingVisuals(ServerWorld world, Outbreak outbreak) {
        for (PendingToll pending : outbreak.pendingTolls) {
            discardVisual(world, pending.visualId);
        }
    }

    private static PendingToll findPendingToll(ServerWorld world, FeverKey key) {
        List<Outbreak> outbreaks = ACTIVE_OUTBREAKS.get(world);
        if (outbreaks == null) {
            return null;
        }
        for (Outbreak outbreak : outbreaks) {
            for (PendingToll pending : outbreak.pendingTolls) {
                if (pending.key.equals(key)) {
                    return pending;
                }
            }
        }
        return null;
    }

    private static ServerPlayerEntity resolveSourcePlayer(ServerWorld world, UUID sourcePlayerId) {
        if (sourcePlayerId == null) {
            return null;
        }
        Entity entity = world.getEntity(sourcePlayerId);
        return entity instanceof ServerPlayerEntity player ? player : null;
    }

    private static int feverThreshold() {
        return Math.max(1, Config.uniqueEffects.toxic_longsword.feverThreshold);
    }

    public static int conversionChance(int configured, Phase8AbilityTuning tuning) {
        return Phase8TuningMath.conversionChance(configured, tuning);
    }

    public static int feverDuration(int configured, Phase8AbilityTuning tuning) {
        return Phase8TuningMath.feverDuration(configured, tuning);
    }

    public static int conversionFever(int configured, Phase8AbilityTuning tuning) {
        return Phase8TuningMath.conversionFever(configured, tuning);
    }

    public static double tollRadius(double configured, Phase8AbilityTuning tuning) {
        return Phase8TuningMath.tollRadius(configured, tuning);
    }

    public static int feverSpread(int configured, Phase8AbilityTuning tuning) {
        return Phase8TuningMath.feverSpread(configured, tuning);
    }

    public static int maximumTolls(int configured, Phase8AbilityTuning tuning) {
        return Phase8TuningMath.maximumTolls(configured, tuning);
    }

    public static float carriedDurationMultiplier(float configured, Phase8AbilityTuning pestilence,
                                                   Phase8AbilityTuning outbreak) {
        return Phase8TuningMath.carriedDurationMultiplier(configured, pestilence, outbreak);
    }

    public static boolean criticalCondition(int stacks, int threshold, double fraction) {
        return Phase8TuningMath.criticalCondition(stacks, threshold, fraction);
    }

    private static boolean canSchedulePrimary(ServerWorld world, Outbreak outbreak,
                                              LivingEntity actor, ServerPlayerEntity sourcePlayer,
                                              LivingEntity origin, List<LivingEntity> candidates,
                                              int feverGain) {
        if (feverGain <= 0 || outbreak.scheduledTolls >= outbreak.maximumTolls) return false;
        Map<FeverKey, FeverState> states = FEVER_STATES.getOrDefault(world, Map.of());
        for (LivingEntity candidate : candidates) {
            if (candidate == origin || outbreak.tolledTargetIds.contains(candidate.getUuid())
                    || outbreak.queuedTargetIds.contains(candidate.getUuid())
                    || !isValidTarget(candidate, actor, sourcePlayer)) continue;
            FeverKey key = new FeverKey(actor.getUuid(), candidate.getUuid());
            FeverState state = states.get(key);
            int stacks = state == null ? 0 : state.stacks;
            if (stacks + feverGain >= feverThreshold()) return true;
        }
        return false;
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        Map<FeverKey, FeverState> states = FEVER_STATES.remove(world);
        if (states != null) states.values().forEach(state -> discardVisual(world, state.visualId));
        List<Outbreak> outbreaks = ACTIVE_OUTBREAKS.remove(world);
        if (outbreaks != null) outbreaks.forEach(outbreak -> discardPendingVisuals(world, outbreak));
        INCUBATION_LOCKOUTS.remove(world);
        PATIENT_ZERO_LOCKOUTS.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        UUID actorId = actor.getUuid();
        Map<FeverKey, FeverState> states = FEVER_STATES.get(world);
        if (states != null) {
            states.entrySet().removeIf(entry -> {
                if (!entry.getKey().actorId.equals(actorId) && !entry.getKey().targetId.equals(actorId)) return false;
                discardVisual(world, entry.getValue().visualId);
                return true;
            });
            if (states.isEmpty()) FEVER_STATES.remove(world);
        }
        List<Outbreak> outbreaks = ACTIVE_OUTBREAKS.get(world);
        if (outbreaks != null) {
            outbreaks.removeIf(outbreak -> {
                if (!outbreak.actorId.equals(actorId)) return false;
                discardPendingVisuals(world, outbreak);
                return true;
            });
            if (outbreaks.isEmpty()) ACTIVE_OUTBREAKS.remove(world);
        }
        for (Map<ServerWorld, Map<FeverKey, Long>> ledgers : List.of(
                INCUBATION_LOCKOUTS, PATIENT_ZERO_LOCKOUTS)) {
            Map<FeverKey, Long> entries = ledgers.get(world);
            if (entries != null) {
                entries.keySet().removeIf(key -> key.actorId.equals(actorId) || key.targetId.equals(actorId));
                if (entries.isEmpty()) ledgers.remove(world);
            }
        }
    }

    public static void clearAll() {
        Set<ServerWorld> worlds = new HashSet<>(FEVER_STATES.keySet());
        worlds.addAll(ACTIVE_OUTBREAKS.keySet());
        worlds.forEach(DeathKnellAbilityManager::clear);
        INCUBATION_LOCKOUTS.clear();
        PATIENT_ZERO_LOCKOUTS.clear();
    }

    private static void finishAll(UniqueAbilityExecution... executions) {
        for (UniqueAbilityExecution execution : executions) {
            UniqueAbilityApi.finish(execution, Phase8UniqueAbilities.FINISH, 1);
        }
    }

    private static void mergeAilments(Map<RegistryEntry<StatusEffect>, CarriedAilment> destination,
                                      List<CarriedAilment> additions) {
        for (CarriedAilment addition : additions) {
            destination.merge(
                    addition.effect,
                    addition,
                    (current, incoming) -> new CarriedAilment(
                            current.effect,
                            Math.max(current.amplifier, incoming.amplifier),
                            Math.max(current.expiresAt, incoming.expiresAt)
                    )
            );
        }
    }

    private static void pruneExpiredAilments(Map<RegistryEntry<StatusEffect>, CarriedAilment> ailments,
                                             long now) {
        ailments.values().removeIf(ailment ->
                ailment.expiresAt != Long.MAX_VALUE && ailment.expiresAt <= now);
    }

    private static void spawnFeverGainEffects(ServerWorld world, LivingEntity target,
                                               int stacks, int threshold, boolean converted) {
        Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        int count = 1 + Math.min(3, (stacks + 1) / 2);
        world.spawnParticles(
                PLAGUE_DUST,
                center.x,
                center.y,
                center.z,
                count,
                target.getWidth() * 0.35,
                target.getHeight() * 0.25,
                target.getWidth() * 0.35,
                0.02
        );
        world.spawnParticles(
                ParticleTypes.SPORE_BLOSSOM_AIR,
                center.x,
                center.y,
                center.z,
                converted ? 4 : 1,
                target.getWidth() * 0.22,
                target.getHeight() * 0.16,
                target.getWidth() * 0.22,
                0.015
        );
        float progress = stacks / (float) Math.max(1, threshold);
        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(),
                SoundCategory.PLAYERS,
                converted ? 0.30F : 0.16F,
                0.72F + progress * 0.42F
        );
    }

    private static void spawnConversionEffects(ServerWorld world, LivingEntity actor,
                                                LivingEntity target, int conversionCount) {
        if (conversionCount <= 0) {
            return;
        }
        Vec3d start = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        Vec3d end = actor.getPos().add(0.0, actor.getHeight() * 0.55, 0.0);
        int points = 8 + Math.min(8, conversionCount * 2);
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Vec3d position = start.lerp(end, t).add(
                    0.0,
                    Math.sin(t * Math.PI) * 0.35,
                    0.0
            );
            world.spawnParticles(
                    i % 3 == 0 ? ParticleTypes.WITCH : PLAGUE_DUST,
                    position.x,
                    position.y,
                    position.z,
                    1,
                    0.025,
                    0.025,
                    0.025,
                    0.01
            );
        }
    }

    private static void spawnTollEffects(ServerWorld world, Vec3d center,
                                         float radius, int depth) {
        world.spawnParticles(
                ParticleTypes.SMOKE,
                center.x,
                center.y,
                center.z,
                24,
                Math.min(1.3, radius * 0.25),
                0.65,
                Math.min(1.3, radius * 0.25),
                0.055
        );
        world.spawnParticles(
                ParticleTypes.SPORE_BLOSSOM_AIR,
                center.x,
                center.y,
                center.z,
                32,
                Math.min(1.7, radius * 0.32),
                0.8,
                Math.min(1.7, radius * 0.32),
                0.075
        );
        world.spawnParticles(
                PLAGUE_DUST,
                center.x,
                center.y,
                center.z,
                18 + depth * 3,
                0.75,
                0.65,
                0.75,
                0.12
        );
    }

    private static void spawnSpreadTrail(ServerWorld world, Vec3d start, Vec3d end) {
        double distance = start.distanceTo(end);
        int points = Math.clamp((int) Math.ceil(distance * 2.2), 2, 14);
        for (int i = 1; i < points; i++) {
            double t = i / (double) points;
            Vec3d position = start.lerp(end, t).add(
                    0.0,
                    Math.sin(t * Math.PI) * 0.22,
                    0.0
            );
            world.spawnParticles(
                    PLAGUE_DUST,
                    position.x,
                    position.y,
                    position.z,
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.015
            );
        }
    }

    private static void playTollSound(ServerWorld world, Vec3d center, int depth) {
        float pitch = Math.clamp(0.54F + depth * 0.065F, 0.54F, 0.88F);
        float volume = Math.max(0.65F, 1.25F - depth * 0.08F);
        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.BLOCK_BELL_USE,
                SoundCategory.PLAYERS,
                volume,
                pitch
        );
        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.BLOCK_BELL_RESONATE,
                SoundCategory.PLAYERS,
                volume * 0.75F,
                pitch * 0.82F
        );
        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_02.get(),
                SoundCategory.PLAYERS,
                0.45F,
                0.62F + depth * 0.035F
        );
    }

    private record FeverKey(UUID actorId, UUID targetId) {
    }

    private record ConversionMapping(RegistryEntry<StatusEffect> positive,
                                     RegistryEntry<StatusEffect> negative,
                                     int amplifierDivisor,
                                     boolean instant,
                                     boolean copyOnToll) {
    }

    private record CarriedAilment(RegistryEntry<StatusEffect> effect,
                                  int amplifier,
                                  long expiresAt) {
    }

    private static final class FeverState {
        private int stacks;
        private long expiresAt;
        private UUID sourcePlayerId;
        private UUID visualId;
        private ItemStack stack = ItemStack.EMPTY;
        private Phase8AbilityTuning knell = Phase8AbilityTuning.EMPTY;
        private Phase8AbilityTuning outbreak = Phase8AbilityTuning.EMPTY;
        private Phase8AbilityTuning pestilence = Phase8AbilityTuning.EMPTY;
        private final Map<RegistryEntry<StatusEffect>, CarriedAilment> ailments = new HashMap<>();
    }

    private static final class PendingToll {
        private final FeverKey key;
        private final UUID targetId;
        private final long executeAt;
        private final int depth;
        private final Map<RegistryEntry<StatusEffect>, CarriedAilment> ailments;
        private final UUID visualId;
        private final double damageMultiplier;
        private final double radiusMultiplier;
        private final boolean secondary;

        private PendingToll(FeverKey key, UUID targetId, long executeAt, int depth,
                            Map<RegistryEntry<StatusEffect>, CarriedAilment> ailments,
                            UUID visualId) {
            this(key, targetId, executeAt, depth, ailments, visualId, 1, 1, false);
        }

        private PendingToll(FeverKey key, UUID targetId, long executeAt, int depth,
                            Map<RegistryEntry<StatusEffect>, CarriedAilment> ailments,
                            UUID visualId, double damageMultiplier, double radiusMultiplier,
                            boolean secondary) {
            this.key = key;
            this.targetId = targetId;
            this.executeAt = executeAt;
            this.depth = depth;
            this.ailments = ailments;
            this.visualId = visualId;
            this.damageMultiplier = damageMultiplier;
            this.radiusMultiplier = radiusMultiplier;
            this.secondary = secondary;
        }
    }

    private static final class Outbreak {
        private final UUID actorId;
        private final UUID sourcePlayerId;
        private final ItemStack stack;
        private final int maximumTolls;
        private final Phase8AbilityTuning knell;
        private final Phase8AbilityTuning outbreak;
        private final Phase8AbilityTuning pestilence;
        private int scheduledTolls;
        private int executedTolls;
        private final List<PendingToll> pendingTolls = new ArrayList<>();
        private final Set<UUID> queuedTargetIds = new HashSet<>();
        private final Set<UUID> tolledTargetIds = new HashSet<>();
        private final Set<UUID> damagedTargetIds = new HashSet<>();
        private final Set<UUID> relapsedTargetIds = new HashSet<>();
        private final Set<UUID> revisitedTargetIds = new HashSet<>();
        private final List<PendingRelapse> pendingRelapses = new ArrayList<>();

        private Outbreak(UUID actorId, UUID sourcePlayerId, ItemStack stack, int maximumTolls,
                         Phase8AbilityTuning knell, Phase8AbilityTuning outbreak,
                         Phase8AbilityTuning pestilence) {
            this.actorId = actorId;
            this.sourcePlayerId = sourcePlayerId;
            this.stack = stack;
            this.maximumTolls = maximumTolls;
            this.knell = knell;
            this.outbreak = outbreak;
            this.pestilence = pestilence;
        }
    }

    private record PendingRelapse(UUID targetId, long at, int fever) {
    }
}
