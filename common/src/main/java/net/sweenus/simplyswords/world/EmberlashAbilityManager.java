package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EmberlashAbilityManager {
    private static final int SEARING_LASH = 1 << 3;
    private static final int COAL_RAKE = 1 << 4;
    private static final int BELLOWS_RHYTHM = 1 << 5;
    private static final int WHITE_SMOULDER = 1 << 6;
    private static final int ENDLESS_SMOULDER = 1 << 7;
    private static final int ASHEN_BRAND = 1 << 8;
    private static final int SEALED_WOUNDS = 1 << 11;
    private static final int SMOKE_SCREEN = 1 << 12;
    private static final int BACKLASH = 1 << 13;
    private static final int BURNING_PACE = 1 << 14;
    private static final int EMERGENCY_BRAND = 1 << 15;
    private static final int PHOENIX_STEP = 1 << 16;
    private static final int SURGEONS_FLAME = 1 << 17;
    private static final int HOT_BLOOD = 1 << 18;
    private static final int PAIN_TO_FUEL = 1 << 19;
    private static final int CRACKLING_RETORT = 1 << 20;
    private static final int ASH_BURST = 1 << 21;
    private static final int SHARED_EMBERS = 1 << 22;
    private static final int LASHBACK = 1 << 23;
    private static final int FINAL_COAL = 1 << 24;
    private static final int DETONATION_LASH = 1 << 25;
    private static final int SPITEFIRE = 1 << 26;
    private static final Map<ServerWorld, WorldState> STATES = new HashMap<>();

    private EmberlashAbilityManager() {
    }

    public static void onHit(ServerWorld world, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        if (world == null || stack == null || stack.isEmpty() || attacker == null || target == null
                || !target.isAlive() || !AwakeningApi.isAbilityUnlocked(stack)) return;
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginPassive(FireForgeMasteryAbilities.EMBERLASH_SMOULDER,
                world, stack, attacker, target);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        WorldState worldState = state(world);
        OwnerState owner = worldState.owners.computeIfAbsent(attacker.getUuid(), ignored -> new OwnerState());
        long now = world.getTime();
        prune(world, worldState, now);
        prepareApplication(world, worldState, attacker, target, tuning);

        int currentStacks = stackCount(target);
        float smoulderBase = smoulderBase(attacker, stack);
        float sustained = smoulderBase * currentStacks * (float) tuned(tuning,
                s("EMBERLASH_SMOULDER_DAMAGE_MULTIPLIER"), s("PER_STACK_MULTIPLIER"), 1);
        TargetState mark = worldState.targets.get(target.getUuid());
        boolean withinEndlessCadence = tuning.flag(ENDLESS_SMOULDER) && mark != null
                && mark.ownerId.equals(attacker.getUuid())
                && withinCadence(mark.lastHitAt, now, tuning.integer(s("LOCKOUT_TICKS"), 60));

        if (currentStacks > 0 && tuning.flag(DETONATION_LASH)) {
            float detonation = sustained * (float) tuning.get(s("EMBERLASH_DETONATION_DAMAGE_PER_STACK"), .3);
            deal(world, attacker, stack, target, detonation);
            clearSmoulder(worldState, target);
            currentStacks = 0;
        } else if (currentStacks > 0) {
            deal(world, attacker, stack, target, sustained);
            if (withinEndlessCadence) deal(world, attacker, stack, target, sustained
                    * (float) tuning.get(s("EMBERLASH_ECHO_DAMAGE_MULTIPLIER"), .5));
        }

        int added = empoweredStacks(owner, now);
        if (tuning.flag(BELLOWS_RHYTHM)) {
            int window = tuning.integer(s("EMBERLASH_COMBO_WINDOW_TICKS"), 40);
            owner.combo = target.getUuid().equals(owner.comboTarget) && now - owner.lastComboAt <= window
                    ? owner.combo + 1 : 1;
            owner.comboTarget = target.getUuid();
            owner.lastComboAt = now;
            if (owner.combo % tuning.integer(s("EMBERLASH_COMBO_HITS"), 3) == 0) added++;
        }
        int cap = tunedInteger(tuning, s("EMBERLASH_SMOULDER_STACK_CAP"), s("STACK_CAP"),
                Config.uniqueEffects.emberlash.maxStacks);
        int duration = tunedInteger(tuning, s("EMBERLASH_SMOULDER_DURATION_TICKS"), s("DURATION_TICKS"), 100);
        int resultingStacks = applyStacks(world, worldState, attacker, target, added, cap, duration, tuning, now);

        int searingThreshold = tuning.integer(s("COUNT"), 3);
        if (tuning.flag(SEARING_LASH) && currentStacks < searingThreshold
                && resultingStacks >= searingThreshold) {
            int fireTicks = tuning.integer(s("FIRE_TICKS"), 60);
            if (fireTicks > 0) target.setOnFireFor(Math.max(1, fireTicks / 20));
        }
        if (tuning.flag(COAL_RAKE) && !tuning.flag(ASHEN_BRAND)
                && isSweepAttack(attacker) && owner.lastSweepAt != now) {
            owner.lastSweepAt = now;
            int swept = 0;
            for (LivingEntity nearby : targets(world, attacker, target.getPos(),
                    tuning.get(s("EMBERLASH_SWEEP_RADIUS"), 3),
                    tuning.integer(s("EMBERLASH_SWEEP_TARGET_CAP"), 4) + 1)) {
                if (nearby == target) continue;
                applyStacks(world, worldState, attacker, nearby, 1, cap, duration, tuning, now);
                if (++swept >= tuning.integer(s("EMBERLASH_SWEEP_TARGET_CAP"), 4)) break;
            }
        }

        consumeReprisal(world, worldState, owner, execution, tuning, attacker, target, stack, now);
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, FireForgeMasteryAbilities.HIT,
                target, 1, resultingStacks);
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 1);
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (context == null || context.world() == null || context.actor() == null) return false;
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginActive(FireForgeMasteryAbilities.EMBERLASH_CAUTERY,
                context, Config.uniqueEffects.emberlash.cooldown);
        UniqueAbilityApi.start(execution);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        LivingEntity target = context.target();
        WorldState worldState = state(world);
        OwnerState owner = worldState.owners.computeIfAbsent(actor.getUuid(), ignored -> new OwnerState());
        long now = world.getTime();
        prune(world, worldState, now);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),
                actor.getSoundCategory(), .5F, 1F);

        double distance = tuning.flag(SURGEONS_FLAME) ? 0
                : 1.5 * tuned(tuning, s("EMBERLASH_EVADE_DISTANCE_MULTIPLIER"), s("SPEED"), 1);
        Vec3d direction = evadeDirection(actor, target, context.facing());
        if (distance > 0) {
            Vec3d velocity = direction.multiply(distance);
            actor.setVelocity(velocity.x, actor.getVelocity().y, velocity.z);
            actor.velocityModified = true;
        }

        if (tuning.flag(PHOENIX_STEP) && distance > 0) {
            phoenixStep(world, actor, context.stack(), direction, distance, tuning);
        }
        if (tuning.flag(SEALED_WOUNDS)) {
            MasteryAbsorptionTracker.grant(actor, "emberlash/cautery",
                    (float) tuning.get(s("EMBERLASH_CAUTERY_ABSORPTION"), 4),
                    tuning.integer(s("EMBERLASH_CAUTERY_ABSORPTION_TICKS"), 60),
                    (float) tuning.get(s("EMBERLASH_CAUTERY_ABSORPTION"), 4));
        } else if (!tuning.flag(PHOENIX_STEP)) {
            actor.heal(actor.getMaxHealth() * Config.uniqueEffects.emberlash.heal / 100F
                    * (float) tuning.get(s("HEAL_MULTIPLIER"), 1));
        }

        if (tuning.flag(SURGEONS_FLAME)) removeOneHarmfulEffect(actor);
        if (tuning.flag(BACKLASH)) {
            owner.empoweredStacks = tuning.integer(s("EMBERLASH_BACKLASH_STACKS"), 2);
            owner.empoweredUntil = now + tuning.integer(s("EMBERLASH_BACKLASH_DURATION_TICKS"), 80);
        }
        if (tuning.flag(BURNING_PACE) && !tuning.flag(SURGEONS_FLAME)) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,
                    tuning.integer(s("EMBERLASH_BURNING_PACE_DURATION_TICKS"), 40),
                    tuning.integer(s("EMBERLASH_BURNING_PACE_AMPLIFIER"), 1)), actor);
        }
        if (tuning.flag(EMERGENCY_BRAND) && actor.getHealth() < actor.getMaxHealth() * .35F
                && now >= owner.emergencyReadyAt) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    tuning.integer(s("EMBERLASH_EMERGENCY_RESISTANCE_TICKS"), 60), 0), actor);
            owner.emergencyReadyAt = now + tuning.integer(s("EMBERLASH_EMERGENCY_LOCKOUT_TICKS"), 160);
        }
        if (tuning.flag(SMOKE_SCREEN)) {
            int affected = 0;
            int cap = tuning.integer(s("EMBERLASH_BLIND_TARGET_CAP"), 6);
            for (LivingEntity enemy : targets(world, actor, actor.getPos(),
                    tuning.get(s("EMBERLASH_BLIND_RADIUS"), 3), cap)) {
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS,
                        tuning.integer(s("EMBERLASH_BLIND_DURATION_TICKS"), 30), 0), actor);
                if (++affected >= cap) break;
            }
        }
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
        UniqueAbilityApi.publishStartedExecution(execution);
        return true;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world) || source == null || amount <= 0) return amount;
        WorldState worldState = STATES.get(world);
        long now = world.getTime();
        if (worldState != null) {
            prune(world, worldState, now);
            if (isMelee(source) && source.getAttacker() instanceof LivingEntity attacker) {
                TargetState mark = worldState.targets.get(attacker.getUuid());
                if (mark != null && target.getUuid().equals(mark.ownerId) && now < mark.reductionUntil) {
                    amount *= 1F - mark.damageReduction;
                }
            }
        }

        ItemStack stack = heldEmberlash(target);
        if (stack == null || !AwakeningApi.isAbilityUnlocked(stack)) return amount;
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginPassive(FireForgeMasteryAbilities.EMBERLASH_SMOULDER,
                world, stack, target, source.getAttacker() instanceof LivingEntity living ? living : null);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        OwnerState owner = state(world).owners.get(target.getUuid());
        int charges = liveReprisalCharges(owner, now);
        if (charges > 0 && tuning.flag(SPITEFIRE)) {
            amount = reprisalIncoming(amount, charges,
                    tuned(tuning, s("EMBERLASH_INCOMING_PER_CHARGE_MULTIPLIER"),
                            s("INCOMING_MULTIPLIER"), 1.04));
        }
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
        return amount;
    }

    public static void onDamageApplied(LivingEntity target, DamageSource source) {
        if (!(target.getWorld() instanceof ServerWorld world) || source == null) return;
        ItemStack stack = heldEmberlash(target);
        if (stack == null || !AwakeningApi.isAbilityUnlocked(stack)) return;
        LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginPassive(FireForgeMasteryAbilities.EMBERLASH_SMOULDER,
                world, stack, target, attacker);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        WorldState worldState = state(world);
        OwnerState owner = worldState.owners.computeIfAbsent(target.getUuid(), ignored -> new OwnerState());
        long now = world.getTime();
        prune(world, worldState, now);

        if (attacker != null && tuning.flag(HOT_BLOOD) && isMelee(source) && now >= owner.hotBloodReadyAt) {
            applyStacks(world, worldState, target, attacker, 1,
                    tunedInteger(tuning, s("EMBERLASH_SMOULDER_STACK_CAP"), s("STACK_CAP"),
                            Config.uniqueEffects.emberlash.maxStacks),
                    tunedInteger(tuning, s("EMBERLASH_SMOULDER_DURATION_TICKS"), s("DURATION_TICKS"), 100),
                    tuning, now);
            owner.hotBloodReadyAt = now + tuning.integer(s("EMBERLASH_HOT_BLOOD_LOCKOUT_TICKS"), 30);
        }
        if (tuning.flag(PAIN_TO_FUEL)) {
            if (owner.reprisalUntil <= now) owner.reprisalCharges = 0;
            owner.reprisalCharges = Math.min(tunedInteger(tuning,
                            s("EMBERLASH_REPRISAL_STACK_CAP"), s("STACK_CAP"), 3),
                    owner.reprisalCharges + 1);
            owner.reprisalUntil = now + tunedInteger(tuning,
                    s("EMBERLASH_REPRISAL_DURATION_TICKS"), s("DURATION_TICKS"), 100);
        }
        if (attacker != null && tuning.flag(LASHBACK)
                && isAtMarkedCap(worldState, attacker, target.getUuid())) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    tuning.integer(s("EMBERLASH_LASHBACK_DURATION_TICKS"), 40),
                    tuning.integer(s("EMBERLASH_LASHBACK_AMPLIFIER"), 1)), target);
        }
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
    }

    public static void onKill(LivingEntity target, DamageSource source) {
        if (!(target.getWorld() instanceof ServerWorld world) || source == null
                || !(source.getAttacker() instanceof LivingEntity attacker)) return;
        ItemStack stack = heldEmberlash(attacker);
        if (stack == null || !AwakeningApi.isAbilityUnlocked(stack)) return;
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginPassive(FireForgeMasteryAbilities.EMBERLASH_SMOULDER,
                world, stack, attacker, target);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        WorldState worldState = state(world);
        if (tuning.flag(FINAL_COAL) && isAtMarkedCap(worldState, target, attacker.getUuid())) {
            SimplySwordsAPI.reduceWeaponCooldown(attacker, stack,
                    tuning.integer(s("COOLDOWN_TICKS"), Config.uniqueEffects.emberlash.cooldown),
                    tuning.integer(s("EMBERLASH_KILL_REFUND_TICKS"), 20));
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, FireForgeMasteryAbilities.KILL, target, 1, 0);
        }
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        STATES.remove(world);
        EmberlashSmoulderVisualManager.clear(world);
    }

    public static void clear(ServerWorld world, LivingEntity owner) {
        if (world == null || owner == null) return;
        WorldState state = STATES.get(world);
        if (state == null) return;
        UUID ownerId = owner.getUuid();
        state.owners.remove(ownerId);
        state.targets.entrySet().removeIf(entry -> {
            if (!ownerId.equals(entry.getValue().ownerId)) return false;
            if (world.getEntity(entry.getKey()) instanceof LivingEntity target) {
                target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
            }
            return true;
        });
        if (state.owners.isEmpty() && state.targets.isEmpty()) STATES.remove(world);
    }

    public static void clearAll() {
        STATES.clear();
        EmberlashSmoulderVisualManager.clearAll();
    }

    private static int applyStacks(ServerWorld world, WorldState worldState, LivingEntity owner, LivingEntity target,
                                   int added, int cap, int duration, FireForgeMasteryTuning tuning, long now) {
        if (target == null || !target.isAlive()) return 0;
        prepareApplication(world, worldState, owner, target, tuning);
        int stacks = nextStackCount(stackCount(target), added, cap);
        if (stacks <= 0) return 0;
        target.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.SMOULDERING), duration, stacks - 1,
                false, false, true), owner);
        TargetState mark = worldState.targets.computeIfAbsent(target.getUuid(), ignored -> new TargetState());
        mark.ownerId = owner.getUuid();
        mark.lastHitAt = now;
        mark.effectUntil = now + duration;
        mark.stackCap = cap;
        if (tuning.flag(WHITE_SMOULDER) && stacks >= cap) {
            mark.damageReduction = (float) tuning.get(s("EMBERLASH_MAX_DAMAGE_REDUCTION"), .12);
            mark.reductionUntil = now + tuning.integer(s("EMBERLASH_MAX_REDUCTION_DURATION_TICKS"), 60);
        }
        EmberlashSmoulderVisualManager.refresh(world, target);
        return stacks;
    }

    private static void prepareApplication(ServerWorld world, WorldState worldState, LivingEntity owner,
                                           LivingEntity target, FireForgeMasteryTuning tuning) {
        TargetState existingMark = worldState.targets.get(target.getUuid());
        if (existingMark != null && !owner.getUuid().equals(existingMark.ownerId)) {
            clearSmoulder(worldState, target);
        }
        OwnerState ownerState = worldState.owners.computeIfAbsent(owner.getUuid(), ignored -> new OwnerState());
        if (tuning.flag(ASHEN_BRAND)) {
            if (ownerState.ashenTarget != null && !ownerState.ashenTarget.equals(target.getUuid())) {
                removeOwnedSmoulder(world, worldState, ownerState.ashenTarget, owner.getUuid());
            }
            ownerState.ashenTarget = target.getUuid();
        }
    }

    private static void consumeReprisal(ServerWorld world, WorldState worldState, OwnerState owner,
                                        UniqueAbilityExecution execution, FireForgeMasteryTuning tuning,
                                        LivingEntity attacker, LivingEntity target, ItemStack stack, long now) {
        int charges = liveReprisalCharges(owner, now);
        if (!tuning.flag(CRACKLING_RETORT)
                || charges < tuning.integer(s("EMBERLASH_REPRISAL_TRIGGER_COUNT"), 3)) return;
        owner.reprisalCharges = 0;
        owner.reprisalUntil = 0;
        float bonus = HelperMethods.abilityScaledDamage("fire", attacker, stack,
                (float) tuning.get(s("EMBERLASH_REPRISAL_DAMAGE_MULTIPLIER"), .25), 0);
        deal(world, attacker, stack, target, bonus);
        if (tuning.flag(ASH_BURST)) {
            int affected = 0;
            int cap = tuning.integer(s("EMBERLASH_ASH_TARGET_CAP"), 8);
            int smoulderCap = tunedInteger(tuning, s("EMBERLASH_SMOULDER_STACK_CAP"), s("STACK_CAP"),
                    Config.uniqueEffects.emberlash.maxStacks);
            int duration = tunedInteger(tuning, s("EMBERLASH_SMOULDER_DURATION_TICKS"),
                    s("DURATION_TICKS"), 100);
            for (LivingEntity enemy : smoulderingTargets(world, attacker, target.getPos(),
                    tuning.get(s("EMBERLASH_ASH_RADIUS"), 2.5), cap)) {
                int stacks = stackCount(enemy);
                float damage = perStackDamage(smoulderBase(attacker, stack), stacks,
                        tuning.get(s("EMBERLASH_SMOULDER_DAMAGE_MULTIPLIER"), 1),
                        tuning.get(s("EMBERLASH_ASH_DAMAGE_PER_STACK"), .08));
                if (!deal(world, attacker, stack, enemy, damage)) continue;
                affected++;
                if (tuning.flag(SHARED_EMBERS) && !tuning.flag(ASHEN_BRAND)) {
                    applyStacks(world, worldState, attacker, enemy,
                            tuning.integer(s("EMBERLASH_ASH_APPLIED_STACKS"), 1),
                            smoulderCap, duration, tuning, now);
                }
                if (affected >= cap) break;
            }
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, FireForgeMasteryAbilities.PULSE,
                    target, affected, charges);
        }
    }

    private static void phoenixStep(ServerWorld world, LivingEntity actor, ItemStack stack, Vec3d direction,
                                    double distance, FireForgeMasteryTuning tuning) {
        Vec3d start = actor.getPos();
        Vec3d end = start.add(direction.multiply(distance));
        int affected = 0;
        int cap = tuning.integer(s("EMBERLASH_PHOENIX_TARGET_CAP"), 6);
        float damage = (float) actor.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                * (float) tuning.get(s("EMBERLASH_PHOENIX_DAMAGE_MULTIPLIER"), .35);
        Box path = actor.getBoundingBox().stretch(end.subtract(start)).expand(1);
        for (LivingEntity enemy : world.getEntitiesByClass(LivingEntity.class, path,
                candidate -> candidate != actor && EntityPredicates.VALID_LIVING_ENTITY.test(candidate)
                        && HelperMethods.checkAbilityTarget(candidate, actor)
                        && segmentDistanceSquared(candidate.getPos(), start, end) <= 2.25).stream()
                .sorted(Comparator.comparingDouble((LivingEntity enemy) -> actor.squaredDistanceTo(enemy))
                        .thenComparing(enemy -> enemy.getUuid().toString())).toList()) {
            if (!deal(world, actor, stack, enemy, damage)) continue;
            int fireTicks = tuning.integer(s("EMBERLASH_PHOENIX_FIRE_TICKS"), 60);
            if (fireTicks > 0) enemy.setOnFireFor(Math.max(1, (int) Math.ceil(fireTicks / 20D)));
            if (++affected >= cap) break;
        }
    }

    private static void removeOwnedSmoulder(ServerWorld world, WorldState worldState, UUID targetId, UUID ownerId) {
        TargetState mark = worldState.targets.get(targetId);
        if (mark == null || !ownerId.equals(mark.ownerId)) return;
        if (world.getEntity(targetId) instanceof LivingEntity target) {
            target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
        }
        worldState.targets.remove(targetId);
    }

    private static void clearSmoulder(WorldState worldState, LivingEntity target) {
        target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
        worldState.targets.remove(target.getUuid());
    }

    private static void removeOneHarmfulEffect(LivingEntity actor) {
        for (StatusEffectInstance instance : new ArrayList<>(actor.getStatusEffects())) {
            RegistryEntry<StatusEffect> type = instance.getEffectType();
            if (!type.value().isBeneficial()) {
                actor.removeStatusEffect(type);
                return;
            }
        }
    }

    private static int empoweredStacks(OwnerState owner, long now) {
        if (owner.empoweredUntil <= now) {
            owner.empoweredStacks = 0;
            return 1;
        }
        int stacks = Math.max(1, owner.empoweredStacks);
        owner.empoweredStacks = 0;
        owner.empoweredUntil = 0;
        return stacks;
    }

    private static int liveReprisalCharges(OwnerState owner, long now) {
        if (owner == null || liveReprisalCharges(owner.reprisalCharges, owner.reprisalUntil, now) == 0) {
            if (owner != null) {
                owner.reprisalCharges = 0;
                owner.reprisalUntil = 0;
            }
            return 0;
        }
        return owner.reprisalCharges;
    }

    static int nextStackCount(int current, int added, int cap) {
        return Math.min(Math.max(0, cap), Math.max(0, current) + Math.max(0, added));
    }

    static float reprisalIncoming(float amount, int charges, double perChargeMultiplier) {
        return amount * (float) Math.pow(Math.max(0, perChargeMultiplier), Math.max(0, charges));
    }

    static int liveReprisalCharges(int charges, long expiresAt, long now) {
        return charges > 0 && now < expiresAt ? charges : 0;
    }

    static float perStackDamage(float base, int stacks, double smoulderMultiplier, double mechanicMultiplier) {
        return Math.max(0, base) * Math.max(0, stacks) * (float) Math.max(0, smoulderMultiplier)
                * (float) Math.max(0, mechanicMultiplier);
    }

    static boolean withinCadence(long lastHitAt, long now, int cadenceTicks) {
        return cadenceTicks >= 0 && now >= lastHitAt && now - lastHitAt <= cadenceTicks;
    }

    static double segmentDistanceSquared(Vec3d point, Vec3d start, Vec3d end) {
        Vec3d line = end.subtract(start);
        if (line.lengthSquared() < 1.0E-8) return point.squaredDistanceTo(start);
        double fraction = Math.clamp(point.subtract(start).dotProduct(line) / line.lengthSquared(), 0, 1);
        return point.squaredDistanceTo(start.add(line.multiply(fraction)));
    }

    private static int stackCount(LivingEntity target) {
        StatusEffectInstance effect = target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    private static boolean isAtMarkedCap(WorldState worldState, LivingEntity target, UUID requiredOwner) {
        TargetState mark = worldState.targets.get(target.getUuid());
        return mark != null && (requiredOwner == null || requiredOwner.equals(mark.ownerId))
                && mark.stackCap > 0 && stackCount(target) >= mark.stackCap;
    }

    private static float smoulderBase(LivingEntity attacker, ItemStack stack) {
        return HelperMethods.abilityScaledDamage("fire", attacker, stack,
                Config.uniqueEffects.emberlash.smoulderDamageScaling, Config.uniqueEffects.emberlash.spellScaling);
    }

    private static boolean isSweepAttack(LivingEntity attacker) {
        return attacker instanceof PlayerEntity player && player.isOnGround() && !player.isSprinting()
                && player.getAttackCooldownProgress(.5F) > .9F
                && sweepMovement(player.horizontalSpeed, player.prevHorizontalSpeed,
                        player.getMovementSpeed());
    }

    static boolean sweepMovement(float horizontalSpeed, float previousHorizontalSpeed, float movementSpeed) {
        return horizontalSpeed - previousHorizontalSpeed < movementSpeed;
    }

    private static boolean isMelee(DamageSource source) {
        return source.isOf(DamageTypes.PLAYER_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK)
                || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO);
    }

    private static Vec3d evadeDirection(LivingEntity actor, LivingEntity target, Vec3d facing) {
        Vec3d direction = target != null && HelperMethods.checkAbilityTarget(target, actor)
                ? actor.getPos().subtract(target.getPos()) : facing.negate();
        Vec3d horizontal = new Vec3d(direction.x, 0, direction.z);
        return horizontal.lengthSquared() < 1.0E-6
                ? Vec3d.fromPolar(0, actor.getYaw()).negate().normalize() : horizontal.normalize();
    }

    private static boolean deal(ServerWorld world, LivingEntity actor, ItemStack stack,
                                LivingEntity target, float damage) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        if (damage <= 0 || !target.isAlive()) return false;
        DamageSource source = actor instanceof PlayerEntity player
                ? world.getDamageSources().playerAttack(player) : world.getDamageSources().mobAttack(actor);
        float adjusted = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, damage);
        return HelperMethods.damageThroughIframes(target, source, adjusted);
        }
    }

    private static List<LivingEntity> targets(ServerWorld world, LivingEntity actor, Vec3d center,
                                              double radius, int cap) {
        Box box = Box.of(center, radius * 2, radius * 2, radius * 2);
        return world.getEntitiesByClass(LivingEntity.class, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(target -> target != actor && HelperMethods.checkAbilityTarget(target, actor)
                        && target.getPos().squaredDistanceTo(center) <= radius * radius)
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                        .thenComparing(target -> target.getUuid().toString()))
                .limit(Math.min(64, Math.max(0, cap))).toList();
    }

    private static List<LivingEntity> smoulderingTargets(ServerWorld world, LivingEntity actor, Vec3d center,
                                                         double radius, int cap) {
        Box box = Box.of(center, radius * 2, radius * 2, radius * 2);
        return world.getEntitiesByClass(LivingEntity.class, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(target -> target != actor && HelperMethods.checkAbilityTarget(target, actor)
                        && stackCount(target) > 0
                        && target.getPos().squaredDistanceTo(center) <= radius * radius)
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                        .thenComparing(target -> target.getUuid().toString()))
                .limit(Math.min(64, Math.max(0, cap))).toList();
    }

    private static ItemStack heldEmberlash(LivingEntity entity) {
        if (entity.getMainHandStack().isOf(ItemsRegistry.EMBERLASH.get())) return entity.getMainHandStack();
        if (entity.getOffHandStack().isOf(ItemsRegistry.EMBERLASH.get())) return entity.getOffHandStack();
        return null;
    }

    private static WorldState state(ServerWorld world) {
        return STATES.computeIfAbsent(world, ignored -> new WorldState());
    }

    private static void prune(ServerWorld world, WorldState state, long now) {
        state.targets.entrySet().removeIf(entry -> entry.getValue().effectUntil <= now
                && entry.getValue().reductionUntil <= now);
        state.owners.entrySet().removeIf(entry -> {
            OwnerState owner = entry.getValue();
            liveReprisalCharges(owner, now);
            return owner.empoweredUntil <= now && owner.reprisalUntil <= now
                    && owner.hotBloodReadyAt <= now && owner.emergencyReadyAt <= now
                    && now - owner.lastComboAt > 200 && world.getEntity(entry.getKey()) == null;
        });
    }

    private static FireForgeMasteryTuning.Setting s(String name) {
        return FireForgeMasteryTuning.Setting.valueOf(name);
    }

    private static double tuned(FireForgeMasteryTuning tuning, FireForgeMasteryTuning.Setting scoped,
                                FireForgeMasteryTuning.Setting legacy, double fallback) {
        return tuning.has(scoped) ? tuning.get(scoped, fallback) : tuning.get(legacy, fallback);
    }

    private static int tunedInteger(FireForgeMasteryTuning tuning, FireForgeMasteryTuning.Setting scoped,
                                    FireForgeMasteryTuning.Setting legacy, int fallback) {
        return tuning.has(scoped) ? tuning.integer(scoped, fallback) : tuning.integer(legacy, fallback);
    }

    private static final class WorldState {
        private final Map<UUID, OwnerState> owners = new HashMap<>();
        private final Map<UUID, TargetState> targets = new HashMap<>();
    }

    private static final class OwnerState {
        private int combo;
        private UUID comboTarget;
        private long lastComboAt = Long.MIN_VALUE / 2;
        private long lastSweepAt = Long.MIN_VALUE / 2;
        private int empoweredStacks;
        private long empoweredUntil;
        private long hotBloodReadyAt;
        private long emergencyReadyAt;
        private int reprisalCharges;
        private long reprisalUntil;
        private UUID ashenTarget;
    }

    private static final class TargetState {
        private UUID ownerId;
        private int stackCap;
        private long lastHitAt;
        private long effectUntil;
        private float damageReduction;
        private long reductionUntil;
    }
}
