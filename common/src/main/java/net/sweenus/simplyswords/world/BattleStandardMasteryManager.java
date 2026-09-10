package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.ability.LongPathFinalFormsMasteryTuning;
import net.sweenus.simplyswords.api.ability.LongPathFinalFormsMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BattleStandardMasteryManager {
    private static final Identifier STANDARD_GUARD_ID = Identifier.of("simplyswords", "sunfire_standard_guard");
    private static final Map<UUID, StandardState> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> SUNFIRE_CLEANSE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> SUNFIRE_GUARDIAN = new HashMap<>();

    private BattleStandardMasteryManager() {
    }

    public static void registerSunfire(BattleStandardEntity standard, UniqueAbilityExecution execution, ItemStack stack) {
        if (standard.ownerEntity != null) ACTIVE.put(standard.getUuid(), new StandardState(
                standard.ownerEntity.getUuid(), standard.getUuid(), true, execution, stack));
    }

    public static void registerHarbinger(BattleStandardDarkEntity standard, UniqueAbilityExecution execution, ItemStack stack) {
        if (standard.ownerEntity != null) ACTIVE.put(standard.getUuid(), new StandardState(
                standard.ownerEntity.getUuid(), standard.getUuid(), false, execution, stack));
    }

    private static StandardState nearestHarbinger(LivingEntity owner, Vec3d position) {
        return ACTIVE.values().stream().filter(state -> !state.sunfire && state.ownerId.equals(owner.getUuid())
                && !state.execution.isTerminal() && state.execution.context().world() == owner.getWorld()
                && state.execution.context().world().getEntity(state.entityId) instanceof BattleStandardDarkEntity banner
                && banner.isAlive() && !banner.isRemoved() && banner.ownerEntity == owner && owner.isAlive())
                .min(Comparator.comparingDouble((StandardState state) -> state.execution.context().world()
                        .getEntity(state.entityId).getPos().squaredDistanceTo(position))
                        .thenComparing(state -> state.entityId.toString())).orElse(null);
    }

    public static Vec3d standardPosition(LivingEntity owner, Vec3d position) {
        StandardState state = nearestHarbinger(owner, position);
        return state == null ? null : state.execution.context().world().getEntity(state.entityId).getPos();
    }

    public static ItemStack harbingerStack(LivingEntity owner) {
        StandardState state = nearestHarbinger(owner, owner.getPos());
        return state == null ? ItemStack.EMPTY : state.stack;
    }

    public static void refundProphecy(LivingEntity owner, int ticks, int cap) {
        StandardState state = nearestHarbinger(owner, owner.getPos());
        if (state == null) return;
        int applied = refundTicks(state.refunded, ticks, cap);
        state.refunded += applied;
        SimplySwordsAPI.reduceWeaponCooldown(owner, state.stack,
                state.execution.cooldownTicks(Config.uniqueEffects.harbinger.cooldown), applied);
    }

    public static float solitaryBonus(LivingEntity owner) {
        return ACTIVE.values().stream().filter(state -> !state.sunfire && state.ownerId.equals(owner.getUuid())
                && !state.execution.isTerminal() && state.execution.context().world() == owner.getWorld()
                && LongPathFinalFormsMasteryAbilities.tuning(state.execution).flag(2048)
                && !LongPathFinalFormsMasteryAbilities.tuning(state.execution).flag(1024)
                && state.execution.context().world().getEntity(state.entityId) instanceof BattleStandardDarkEntity banner
                && banner.isAlive() && !banner.isRemoved() && owner.squaredDistanceTo(banner) <= 36)
                .findAny().isPresent() ? .25F : 0;
    }

    public static boolean tickSunfire(BattleStandardEntity standard, UniqueAbilityExecution execution,
                                      ItemStack stack) {
        if (execution == null || execution.isTerminal() || !(standard.getWorld() instanceof ServerWorld world)
                || standard.ownerEntity == null || !standard.ownerEntity.isAlive() || standard.ownerEntity.isRemoved()
                || standard.ownerEntity.getWorld() != standard.getWorld()) {
            terminate(execution, false, 0);
            standard.discard();
            return true;
        }
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
        StandardState state = ACTIVE.computeIfAbsent(standard.getUuid(), ignored ->
                new StandardState(standard.ownerEntity.getUuid(), standard.getUuid(), true, execution, stack));
        MasteryAbsorptionTracker.sweep(world);
        applyStandardGuard(standard, standard.ownerEntity, tuning);
        int life = standardLifetime(tuning.get(s("LIFETIME_MULTIPLIER"), 1));
        if (standard.age >= life) {
            terminate(execution, true, state.hits);
            standard.discard();
            return true;
        }
        if (standard.isOnGround() && !state.landed) {
            state.landed = true;
            state.hits += sunfireLanding(world, standard, state, tuning);
        }
        if (state.landed) moveSunfire(standard, tuning);
        int interval = tuning.integer(s("INTERVAL_TICKS"), 10);
        if (standard.age % interval == 0) state.hits += sunfireHostilePulse(world, standard, state, tuning);
        int supportInterval = tuning.integer(s("SUPPORT_INTERVAL_TICKS"), 80);
        if (standard.age % supportInterval == 0) sunfireSupportPulse(world, standard, state, tuning);
        return true;
    }

    public static boolean tickHarbinger(BattleStandardDarkEntity standard, UniqueAbilityExecution execution,
                                        ItemStack stack) {
        if (execution == null || execution.isTerminal() || !(standard.getWorld() instanceof ServerWorld world)
                || standard.ownerEntity == null || !standard.ownerEntity.isAlive() || standard.ownerEntity.isRemoved()
                || standard.ownerEntity.getWorld() != standard.getWorld()) {
            terminate(execution, false, 0);
            standard.discard();
            return true;
        }
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
        StandardState state = ACTIVE.computeIfAbsent(standard.getUuid(), ignored ->
                new StandardState(standard.ownerEntity.getUuid(), standard.getUuid(), false, execution, stack));
        moveHarbinger(world, standard, tuning);
        int life = standardLifetime(tuning.get(s("LIFETIME_MULTIPLIER"), 1));
        if (standard.age >= life) {
            terminate(execution, true, state.hits);
            standard.discard();
            return true;
        }
        if (standard.isOnGround() && !state.landed) {
            state.landed = true;
            state.hits += harbingerLanding(world, standard, state, tuning);
        }
        int interval = tuning.integer(s("INTERVAL_TICKS"), 10);
        if (standard.age % interval == 0) state.hits += harbingerHostilePulse(world, standard, state, tuning);
        int supportInterval = tuning.integer(s("SUPPORT_INTERVAL_TICKS"), 80);
        if (standard.age % supportInterval == 0) harbingerSupportPulse(world, standard, state, tuning);
        harbingerOwnerAura(standard, tuning);
        return true;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        double reduction = ownedSunfire(target).stream().mapToDouble(state -> {
            LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(state.execution);
            Entity standard = state.execution.context().world().getEntity(state.entityId);
            double range = tuning.get(s("GUARD_RANGE"), 7);
            return tuning.flag(16384) && standard != null && target.squaredDistanceTo(standard) <= range * range
                    ? tuning.get(s("DAMAGE_REDUCTION"), .15) : 0;
        }).max().orElse(0);
        return amount * (float) (1 - Math.clamp(reduction, 0, 1));
    }

    public static boolean tryPhoenixStandard(LivingEntity owner, DamageSource source) {
        if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        StandardState state = ownedSunfire(owner).stream()
                .filter(candidate -> LongPathFinalFormsMasteryAbilities.tuning(candidate.execution).flag(65536))
                .min(Comparator.comparingDouble((StandardState candidate) ->
                        owner.squaredDistanceTo(candidate.execution.context().world().getEntity(candidate.entityId)))
                        .thenComparing(candidate -> candidate.entityId.toString())).orElse(null);
        if (state == null) return false;
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(state.execution);
        ServerWorld world = state.execution.context().world();
        Entity standard = world.getEntity(state.entityId);
        Vec3d standardPosition = standard.getPos();
        standard.discard();
        owner.setHealth(1);
        int duration = tuning.integer(s("PHOENIX_DURATION_TICKS"), 80);
        LongPathFinalFormsMasteryCombatManager.grantSunfireRegeneration(owner, duration, 2);
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1));
        terminate(state.execution, true, state.hits);
        phoenixFeedback(world, owner, standardPosition, state.stack);
        return true;
    }

    private static void phoenixFeedback(ServerWorld world, LivingEntity owner, Vec3d standardPosition, ItemStack stack) {
        var gold = new net.minecraft.particle.DustParticleEffect(new org.joml.Vector3f(1.0F, 0.68F, 0.16F), 1.2F);
        world.spawnParticles(ParticleTypes.FLAME, standardPosition.x, standardPosition.y + 1, standardPosition.z,
                18, .35, .6, .35, .04);
        world.spawnParticles(gold, standardPosition.x, standardPosition.y + 1, standardPosition.z,
                20, .4, .6, .4, .02);
        world.spawnParticles(ParticleTypes.FLAME, owner.getX(), owner.getBodyY(.5), owner.getZ(),
                48, .6, .8, .6, .08);
        world.spawnParticles(gold, owner.getX(), owner.getBodyY(.5), owner.getZ(),
                60, .7, .9, .7, .04);
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2 / 24;
            world.spawnParticles(ParticleTypes.END_ROD, owner.getX() + Math.cos(angle) * 1.25,
                    owner.getY() + .2, owner.getZ() + Math.sin(angle) * 1.25, 0, 0, .12, 0, 1);
        }
        world.playSoundFromEntity(null, owner, SoundRegistry.ELEMENTAL_SWORD_HOLY_ATTACK_03.get(),
                owner.getSoundCategory(), .8F, 1.1F);
        if (owner instanceof net.minecraft.server.network.ServerPlayerEntity player) {
            dev.architectury.networking.NetworkManager.sendToPlayer(player,
                    new net.sweenus.simplyswords.network.PhoenixStandardPacket(world.getRegistryKey().getValue(), stack));
        }
    }

    private static List<StandardState> ownedSunfire(LivingEntity owner) {
        return ACTIVE.values().stream().filter(state -> state.sunfire && state.ownerId.equals(owner.getUuid())
                && !state.execution.isTerminal() && state.execution.context().world() == owner.getWorld()
                && state.execution.context().world().getEntity(state.entityId) instanceof BattleStandardEntity standard
                && standard.isAlive() && !standard.isRemoved()).toList();
    }

    private static int sunfireHostilePulse(ServerWorld world, BattleStandardEntity standard,
                                            StandardState state, LongPathFinalFormsMasteryTuning tuning) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(state == null ? null : CombatProvenanceApi.from(state.stack, null))) {
        if (tuning.flag(512)) return 0;
        double radius = tuning.get(s("RADIUS"), 6);
        int cap = tuning.integer(s("TARGET_CAP"), 32);
        List<LivingEntity> targets = targets(world, standard, standard.ownerEntity, radius, cap, true);
        float base = HelperMethods.abilityScaledDamage(SpellScalingComponents.component(standard.spellScalingOwner, "damage"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.sunfire.damageScaling,
                Config.uniqueEffects.sunfire.spellScaling);
        double multiplier = tuning.get(s("DAMAGE_MULTIPLIER"), 1);
        if (tuning.flag(4) && standard.age <= tuning.integer(s("EARLY_WINDOW_TICKS"), 100)) {
            multiplier *= tuning.get(s("EARLY_DAMAGE_MULTIPLIER"), 1.2);
        }
        int pulse = ++state.pulses;
        boolean cycle = tuning.flag(2)
                && pulse % Math.max(1, tuning.integer(s("CYCLE_PULSE_COUNT"), 4)) == 0;
        if (cycle) multiplier *= tuning.get(s("CYCLE_DAMAGE_MULTIPLIER"), 1.4);
        long now = world.getTime();
        int window = tuning.integer(s("WEAKNESS_WINDOW_TICKS"), 40);
        int required = Math.max(1, tuning.integer(s("WEAKNESS_PULSE_COUNT"), 3));
        int affected = 0;
        for (LivingEntity target : targets) {
            float damage = base * (float) multiplier;
            if (deal(world, standard.ownerEntity, state.stack, target, damage)) {
                affected++;
                var history = state.sunfirePulses.computeIfAbsent(target.getUuid(), ignored -> new java.util.ArrayDeque<Long>());
                history.removeIf(tick -> now - tick > window);
                history.addLast(now);
                while (history.size() > required) history.removeFirst();
                int count = history.size();
                target.setOnFireFor(Math.max(1, tuning.integer(s("FIRE_TICKS"), 20) / 20));
                if (!tuning.flag(16)) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), standard);
                if (tuning.flag(1) && count >= required) target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WEAKNESS, tuning.integer(s("WEAKNESS_DURATION_TICKS"), 60), 0), standard);
                if (cycle) target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING,
                        tuning.integer(s("GLOWING_DURATION_TICKS"), 60), 0), standard);
                UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, LongPathFinalFormsMasteryAbilities.PULSE,
                        target, 1, damage);
            }
        }
        spawnAuraParticles(world, standard);
        state.sunfirePulses.entrySet().removeIf(entry -> entry.getValue().isEmpty()
                || now - entry.getValue().getLast() > window);
        return affected;
        }
    }

    private static int sunfireLanding(ServerWorld world, BattleStandardEntity standard,
                                       StandardState state, LongPathFinalFormsMasteryTuning tuning) {
        spawnLandingParticles(world, standard);
        if (tuning.flag(512)) return 0;
        double radius = tuning.get(s("LANDING_RADIUS"), 1);
        int cap = tuning.integer(s("LANDING_TARGET_CAP"), 64);
        float base = HelperMethods.abilityScaledDamage(SpellScalingComponents.component(standard.spellScalingOwner, "damage"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.sunfire.damageScaling,
                Config.uniqueEffects.sunfire.spellScaling);
        double landing = tuning.get(s("LANDING_DAMAGE_MULTIPLIER"), 3) * tuning.get(s("DAMAGE_MULTIPLIER"), 1);
        int affected = 0;
        for (LivingEntity target : targets(world, standard, standard.ownerEntity, radius, cap, true)) {
            if (deal(world, standard.ownerEntity, state.stack, target, base * (float) landing)) {
                target.setOnFireFor(Math.max(1, tuning.integer(s("FIRE_TICKS"), 20) / 20));
                target.setVelocity((target.getX() - standard.getX()) / 4, .5, (target.getZ() - standard.getZ()) / 4);
                target.velocityModified = true;
                affected++;
            }
        }
        return affected;
    }

    private static void sunfireSupportPulse(ServerWorld world, BattleStandardEntity standard,
                                             StandardState state, LongPathFinalFormsMasteryTuning tuning) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(state == null ? null : CombatProvenanceApi.from(state.stack, null))) {
        double radius = tuning.get(s("SUPPORT_RADIUS"), 6);
        List<LivingEntity> allies = targets(world, standard, standard.ownerEntity, radius,
                tuning.integer(s("SUPPORT_TARGET_CAP"), 16), false);
        float heal = HelperMethods.abilityScaledDamage(SpellScalingComponents.component(standard.spellScalingOwner, "healing"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.sunfire.healScaling,
                Config.uniqueEffects.sunfire.spellScalingHeal) * (float) tuning.get(s("HEAL_MULTIPLIER"), 1);
        Map<UUID, Long> cleanseLocks = SUNFIRE_CLEANSE.computeIfAbsent(world, ignored -> new HashMap<>());
        Map<UUID, Long> guardianLocks = SUNFIRE_GUARDIAN.computeIfAbsent(world, ignored -> new HashMap<>());
        boolean healing = !tuning.flag(8) && !tuning.flag(1024);
        int affected = 0;
        for (LivingEntity ally : allies) {
            boolean lowHealth = ally.getHealth() / ally.getMaxHealth() < tuning.get(s("GUARDIAN_THRESHOLD"), .35);
            float applied = tuning.flag(512) && ally == standard.ownerEntity ? heal * .5F : heal;
            if (healing) ally.heal(applied);
            if (tuning.flag(512)) {
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                        tuning.integer(s("SANCTUARY_RESISTANCE_TICKS"), 60), 0), standard);
            }
            int duration = tuning.integer(s("STRENGTH_DURATION_TICKS"), 90);
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration,
                    tuning.flag(1024) ? 2 : 1), standard);
            if (healing && tuning.flag(64)) LongPathFinalFormsMasteryCombatManager.grantSunfireRegeneration(ally,
                    tuning.integer(s("ALLY_REGEN_TICKS"), 60), 0);
            if (tuning.flag(1024)) LongPathFinalFormsMasteryCombatManager.supportSunfireAlly(ally,
                    tuning.integer(s("ALLY_CHARGE_TICKS"), 60), tuning.integer(s("FIRE_TICKS"), 40), world.getTime());
            if (tuning.flag(128)
                    && lowHealth
                    && guardianLocks.getOrDefault(ally.getUuid(), 0L) <= world.getTime()) {
                float before = ally.getAbsorptionAmount();
                MasteryAbsorptionTracker.grant(ally, "warglaive/guardian", (float) tuning.get(s("GUARDIAN_ABSORPTION"), 4),
                        tuning.integer(s("GUARDIAN_ABSORPTION_TICKS"), 100),
                        (float) tuning.get(s("GUARDIAN_ABSORPTION"), 4));
                if (ally.getAbsorptionAmount() > before) guardianLocks.put(ally.getUuid(),
                        world.getTime() + tuning.integer(s("GUARDIAN_LOCKOUT_TICKS"), 200));
            }
            if (tuning.flag(32) && cleanseLocks.getOrDefault(ally.getUuid(), 0L) <= world.getTime()
                    && removeHarmful(ally)) {
                cleanseLocks.put(ally.getUuid(),
                        world.getTime() + tuning.integer(s("CLEANSE_LOCKOUT_TICKS"), 160));
            }
            affected++;
        }
        guardianLocks.entrySet().removeIf(entry -> entry.getValue() <= world.getTime());
        cleanseLocks.entrySet().removeIf(entry -> entry.getValue() <= world.getTime());
        if (rallyTriggers(tuning.flag(256), affected, tuning.integer(s("RALLY_ALLY_COUNT"), 3),
                state.supportRefunded)) {
            state.supportRefunded = true;
            SimplySwordsAPI.reduceWeaponCooldown(standard.ownerEntity, state.stack,
                    state.execution.cooldownTicks(Config.uniqueEffects.sunfire.cooldown), tuning.integer(s("RALLY_REFUND_TICKS"), 40));
        }
        UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, LongPathFinalFormsMasteryAbilities.SUPPORT,
                null, affected, heal);
        }
    }

    private static int harbingerHostilePulse(ServerWorld world, BattleStandardDarkEntity standard,
                                              StandardState state, LongPathFinalFormsMasteryTuning tuning) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(state == null ? null : CombatProvenanceApi.from(state.stack, null))) {
        if (tuning.flag(1024)) return 0;
        double radius = tuning.flag(8) ? 4 : tuning.flag(2048) ? 5 : tuning.get(s("RADIUS"), 6);
        List<LivingEntity> targets = targets(world, standard, standard.ownerEntity, radius,
                tuning.integer(s("TARGET_CAP"), 32), true);
        float base = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("harbinger"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.harbinger.damageScaling,
                Config.uniqueEffects.harbinger.spellScaling);
        int pulse = ++state.pulses;
        boolean cycle = tuning.flag(4)
                && pulse % Math.max(1, tuning.integer(s("CYCLE_PULSE_COUNT"), 5)) == 0;
        long now = world.getTime();
        int window = tuning.integer(s("WEAKNESS_WINDOW_TICKS"), 40);
        int required = Math.max(1, tuning.integer(s("WEAKNESS_PULSE_COUNT"), 3));
        double core = tuning.get(s("CORE_RANGE"), 2);
        int affected = 0;
        for (LivingEntity target : targets) {
            double multiplier = tuning.get(s("DAMAGE_MULTIPLIER"), 1);
            if (tuning.flag(2) && target.squaredDistanceTo(standard) <= core * core) {
                multiplier *= tuning.get(s("NEAR_DAMAGE_MULTIPLIER"), 1.2);
            }
            if (cycle) multiplier *= tuning.get(s("CYCLE_DAMAGE_MULTIPLIER"), 1.4);
            multiplier *= harbingerConditionalMultiplier(tuning, standard.ownerEntity, target);
            float damage = base * (float) multiplier;
            target.timeUntilRegen = 0;
            boolean landed = deal(world, standard.ownerEntity, state.stack, target, damage);
            target.timeUntilRegen = 0;
            if (landed) {
                affected++;
                var history = state.sunfirePulses.computeIfAbsent(target.getUuid(), ignored -> new java.util.ArrayDeque<Long>());
                history.removeIf(tick -> now - tick > window);
                history.addLast(now);
                while (history.size() > required) history.removeFirst();
                int count = history.size();
                if (tuning.flag(1) && count >= required) HarbingerMasteryState.applyWeakness(standard.ownerEntity, target,
                        tuning.integer(s("WEAKNESS_DURATION_TICKS"), 80), 0, tuning.flag(131072), false);
                if (tuning.flag(65536)) HarbingerMasteryState.applyWeakness(standard.ownerEntity, target,
                        tuning.integer(s("PLAGUE_WEAKNESS_TICKS"), 120), 0, tuning.flag(131072), false);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 0), standard);
                applyHarbingerPull(target, standard, tuning, radius, cycle);
                UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, LongPathFinalFormsMasteryAbilities.PULSE,
                        target, 1, damage);
            }
        }
        HelperMethods.spawnParticle(world, ParticleTypes.SCULK_SOUL,
                standard.getX(), standard.getY(), standard.getZ(), 0, 0, 0);
        state.sunfirePulses.entrySet().removeIf(entry -> entry.getValue().isEmpty()
                || now - entry.getValue().getLast() > window);
        return affected;
        }
    }

    private static int harbingerLanding(ServerWorld world, BattleStandardDarkEntity standard,
                                         StandardState state, LongPathFinalFormsMasteryTuning tuning) {
        HelperMethods.spawnParticle(world, ParticleTypes.SOUL_FIRE_FLAME,
                standard.getX(), standard.getY(), standard.getZ(), 0, .3, 0);
        HelperMethods.spawnParticle(world, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                standard.getX(), standard.getY(), standard.getZ(), 0, 0, 0);
        if (tuning.flag(1024)) return 0;
        double radius = tuning.get(s("LANDING_RADIUS"), 1);
        int cap = tuning.integer(s("LANDING_TARGET_CAP"), 64);
        float base = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("harbinger"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.harbinger.damageScaling,
                Config.uniqueEffects.harbinger.spellScaling);
        int affected = 0;
        for (LivingEntity target : targets(world, standard, standard.ownerEntity, radius, cap, true)) {
            if (deal(world, standard.ownerEntity, state.stack, target,
                    base * (float) (tuning.get(s("LANDING_DAMAGE_MULTIPLIER"), 3)
                            * tuning.get(s("DAMAGE_MULTIPLIER"), 1)
                            * harbingerConditionalMultiplier(tuning, standard.ownerEntity, target)))) {
                target.setVelocity((target.getX() - standard.getX()) / 4, .5, (target.getZ() - standard.getZ()) / 4);
                target.velocityModified = true;
                affected++;
            }
        }
        return affected;
    }

    private static void harbingerSupportPulse(ServerWorld world, BattleStandardDarkEntity standard,
                                               StandardState state, LongPathFinalFormsMasteryTuning tuning) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(state == null ? null : CombatProvenanceApi.from(state.stack, null))) {
        if (tuning.flag(2048)) return;
        List<LivingEntity> allies = targets(world, standard, standard.ownerEntity,
                tuning.get(s("SUPPORT_RADIUS"), 6), tuning.integer(s("SUPPORT_TARGET_CAP"), 16), false);
        int affected = 0;
        for (LivingEntity ally : allies) {
            int amplifier = tuning.flag(1024)
                    ? (ally == standard.ownerEntity ? tuning.integer(s("OWNER_HASTE_AMPLIFIER"), 1)
                            : tuning.integer(s("STATUS_AMPLIFIER"), 3))
                    : 2;
            if (!tuning.flag(8) || ally == standard.ownerEntity) ally.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,
                    tuning.integer(s("HASTE_DURATION_TICKS"), 90), amplifier), standard);
            affected++;
            if (tuning.flag(1024) && ally == standard.ownerEntity) continue;
            if (tuning.flag(64) || tuning.flag(1024)) ally.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, tuning.integer(s("ALLY_SPEED_TICKS"), 80),
                    tuning.flag(1024) ? 1 : 0), standard);
            HarbingerMasteryState.support(standard, ally, tuning);
        }
        world.playSoundFromEntity(null, standard, SoundRegistry.DARK_SWORD_WHOOSH_01.get(),
                standard.getSoundCategory(), .1F, .6F);
        if (rallyTriggers(tuning.flag(256), affected, tuning.integer(s("RALLY_ALLY_COUNT"), 3),
                state.supportRefunded)) {
            state.supportRefunded = true;
            SimplySwordsAPI.reduceWeaponCooldown(standard.ownerEntity, state.stack,
                    state.execution.cooldownTicks(Config.uniqueEffects.harbinger.cooldown), tuning.integer(s("RALLY_REFUND_TICKS"), 40));
        }
        UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, LongPathFinalFormsMasteryAbilities.SUPPORT,
                null, affected, 0);
        }
    }

    private static void harbingerOwnerAura(BattleStandardDarkEntity standard, LongPathFinalFormsMasteryTuning tuning) {
        double range = tuning.flag(2048) ? 6 : tuning.flag(32) ? tuning.get(s("OWNER_AURA_RANGE"), 7) : 3;
        if (standard.ownerEntity.squaredDistanceTo(standard) <= range * range && standard.age % 20 == 0) {
            int ticks = tuning.integer(s("OWNER_HASTE_TICKS"), 30);
            int amplifier = tuning.flag(1024) ? 1 : tuning.flag(2048) ? 2 : 0;
            standard.ownerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, ticks, amplifier), standard);
        }
    }

    private static void moveSunfire(BattleStandardEntity standard, LongPathFinalFormsMasteryTuning tuning) {
        if (!tuning.flag(8)) return;
        standard.setMobileStandard(true);
        Vec3d forward = Vec3d.fromPolar(0, standard.ownerEntity.getYaw());
        Vec3d side = new Vec3d(-forward.z, 0, forward.x).multiply(1.5);
        for (Vec3d offset : List.of(side, side.multiply(-1), forward.multiply(-1.5))) {
            Vec3d position = standard.ownerEntity.getPos().add(offset);
            Box box = standard.getBoundingBox().offset(position.subtract(standard.getPos()));
            if (!standard.getWorld().isChunkLoaded(net.minecraft.util.math.BlockPos.ofFloored(position))
                    || !standard.getWorld().getWorldBorder().contains(box)
                    || position.y < standard.getWorld().getBottomY()
                    || box.maxY >= standard.getWorld().getTopY()
                    || !standard.getWorld().isSpaceEmpty(standard, box)) continue;
            standard.refreshPositionAndAngles(position.x, position.y, position.z, standard.ownerEntity.getYaw(), 0);
            break;
        }
        standard.setVelocity(Vec3d.ZERO);
    }

    private static void moveHarbinger(ServerWorld world, BattleStandardDarkEntity standard,
                                       LongPathFinalFormsMasteryTuning tuning) {
        if (!tuning.flag(8) || !standard.isOnGround()) return;
        double range = tuning.get(s("PURSUIT_RANGE"), 12);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, standard.getBoundingBox().expand(range),
                target -> target.isAlive() && target != standard.ownerEntity
                        && !(target instanceof BattleStandardEntity) && !(target instanceof BattleStandardDarkEntity)
                        && target.squaredDistanceTo(standard) <= range * range
                        && HelperMethods.checkAbilityTarget(target, standard.ownerEntity)).stream()
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.squaredDistanceTo(standard))
                        .thenComparing(target -> target.getUuid().toString())).limit(1).toList();
        if (!targets.isEmpty()) moveToward(standard, targets.getFirst().getPos(), tuning.get(s("MOVEMENT_SPEED"), .3));
        else standard.setVelocity(0, standard.getVelocity().y, 0);
    }

    private static void moveToward(LivingEntity entity, Vec3d target, double speed) {
        Vec3d delta = target.subtract(entity.getPos());
        Vec3d horizontal = new Vec3d(delta.x, 0, delta.z);
        if (horizontal.lengthSquared() > .01) {
            Vec3d velocity = horizontal.normalize().multiply(speed);
            entity.setVelocity(velocity.x, entity.getVelocity().y, velocity.z);
            entity.velocityModified = true;
        } else entity.setVelocity(0, entity.getVelocity().y, 0);
    }

    private static List<LivingEntity> targets(ServerWorld world, LivingEntity origin, LivingEntity owner,
                                               double radius, int cap, boolean hostile) {
        Box box = new Box(origin.getX() + radius, origin.getY() + radius / 3, origin.getZ() + radius,
                origin.getX() - radius, origin.getY() - radius / 3, origin.getZ() - radius);
        return world.getOtherEntities(origin, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(entity -> (!hostile || entity != owner) && !(entity instanceof BattleStandardEntity)
                        && !(entity instanceof BattleStandardDarkEntity))
                .filter(entity -> hostile ? HelperMethods.checkAbilityTarget(entity, owner)
                        : !HelperMethods.checkFriendlyFire(entity, owner))
                .filter(entity -> entity.getPos().subtract(origin.getPos()).horizontalLengthSquared() <= radius * radius)
                .sorted(Comparator.comparingDouble((LivingEntity entity) -> entity.squaredDistanceTo(origin))
                        .thenComparing(entity -> entity.getUuid().toString()))
                .limit(Math.clamp(cap, 0, 64)).toList();
    }

    private static boolean deal(ServerWorld world, LivingEntity owner, ItemStack stack,
                                LivingEntity target, float amount) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
        return CombatProvenanceApi.damage(stack, (source).getAttacker(), target, source, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, amount));
        }
    }

    private static void pull(LivingEntity target, Vec3d center, double strength) {
        Vec3d delta = center.subtract(target.getPos());
        if (delta.lengthSquared() < .001) return;
        Vec3d velocity = delta.normalize().multiply(strength);
        target.setVelocity(velocity.x, Math.clamp(velocity.y, -.4, .4), velocity.z);
        target.velocityModified = true;
    }

    private static boolean removeHarmful(LivingEntity ally) {
        for (StatusEffectInstance instance : new ArrayList<>(ally.getStatusEffects())) {
            RegistryEntry<StatusEffect> type = instance.getEffectType();
            if (!type.value().isBeneficial() && !type.equals(EffectRegistry.getReference(EffectRegistry.BATTLE_FATIGUE))) {
                return ally.removeStatusEffect(type);
            }
        }
        return false;
    }

    private static void terminate(UniqueAbilityExecution execution, boolean finish, int hits) {
        if (execution == null || execution.isTerminal()) return;
        if (finish) UniqueAbilityApi.finish(execution, LongPathFinalFormsMasteryAbilities.FINISH, hits);
        else UniqueAbilityApi.cancel(execution);
    }

    private static void applyStandardGuard(BattleStandardEntity standard, LivingEntity owner,
                                           LongPathFinalFormsMasteryTuning tuning) {
        double resistance = ownedSunfire(owner).stream().mapToDouble(state -> {
            LongPathFinalFormsMasteryTuning current = LongPathFinalFormsMasteryAbilities.tuning(state.execution);
            Entity entity = state.execution.context().world().getEntity(state.entityId);
            double range = current.get(s("GUARD_RANGE"), 7);
            return current.flag(16384) && owner.squaredDistanceTo(entity) <= range * range
                    ? current.get(s("KNOCKBACK_RESISTANCE"), 0) : 0;
        }).max().orElse(0);
        EntityAttributeInstance attribute = owner.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        attribute.removeModifier(STANDARD_GUARD_ID);
        if (resistance > 0) attribute.addTemporaryModifier(new EntityAttributeModifier(STANDARD_GUARD_ID, resistance,
                EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeStandardGuard(LivingEntity owner) {
        EntityAttributeInstance attribute = owner.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute != null) attribute.removeModifier(STANDARD_GUARD_ID);
    }

    static int nextPulseCount(int previous, long lastTick, long now, int window) {
        return previous <= 0 || now - lastTick > window ? 1 : previous + 1;
    }

    static int refundTicks(int alreadyRefunded, int requested, int cap) {
        if (alreadyRefunded >= cap) return 0;
        return Math.max(0, Math.min(requested, cap - alreadyRefunded));
    }

    static boolean rallyTriggers(boolean owned, int allies, int required, boolean alreadyRefunded) {
        return owned && !alreadyRefunded && allies >= Math.max(1, required);
    }

    static int standardLifetime(double lifetimeMultiplier) {
        return (int) Math.round(500 * lifetimeMultiplier);
    }

    private static void spawnAuraParticles(ServerWorld world, LivingEntity standard) {
        HelperMethods.spawnParticle(world, ParticleTypes.LAVA,
                standard.getX(), standard.getY(), standard.getZ(), 0, 0, 0);
    }

    private static void spawnLandingParticles(ServerWorld world, LivingEntity standard) {
        HelperMethods.spawnParticle(world, ParticleTypes.LAVA,
                standard.getX(), standard.getY(), standard.getZ(), 0, .3, 0);
        HelperMethods.spawnParticle(world, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                standard.getX(), standard.getY(), standard.getZ(), 0, .1, 0);
    }

    // Ends the execution for a standard that died by decay or removal rather than by its own lifetime.
    public static void onStandardRemoved(UUID entityId) {
        HarbingerMasteryState.removeBanner(entityId);
        StandardState state = ACTIVE.remove(entityId);
        if (state == null) return;
        Entity owner = state.execution.context().world().getEntity(state.ownerId);
        if (owner instanceof LivingEntity living) applyStandardGuard(null, living, LongPathFinalFormsMasteryTuning.EMPTY);
        terminate(state.execution, true, state.hits);
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        ACTIVE.entrySet().removeIf(entry -> {
            StandardState state = entry.getValue();
            if (state.execution.context().world() != world) return false;
            Entity owner = world.getEntity(state.ownerId);
            if (owner instanceof LivingEntity living) removeStandardGuard(living);
            terminate(state.execution, false, 0);
            return true;
        });
        SUNFIRE_CLEANSE.remove(world);
        SUNFIRE_GUARDIAN.remove(world);
        MasteryAbsorptionTracker.clear(world);
    }

    public static void clearAll() {
        ACTIVE.values().forEach(state -> terminate(state.execution, false, 0));
        ACTIVE.clear();
        SUNFIRE_CLEANSE.clear();
        SUNFIRE_GUARDIAN.clear();
        MasteryAbsorptionTracker.clearAll();
    }

    static double finalOmenMultiplier(LongPathFinalFormsMasteryTuning tuning, LivingEntity target) {
        if (!tuning.flag(32768) || !target.hasStatusEffect(StatusEffects.WEAKNESS)
                || target.getHealth() / target.getMaxHealth() >= tuning.get(s("LOW_HEALTH_THRESHOLD"), .25)) return 1;
        if (WatcherAbilityManager.isExecutionImmune(target)) {
            return tuning.get(s("BOSS_DAMAGE_MULTIPLIER"), 1.1);
        }
        return target.getHealth() / target.getMaxHealth() < tuning.get(s("LOW_HEALTH_THRESHOLD"), .25)
                ? tuning.get(s("LOW_HEALTH_DAMAGE_MULTIPLIER"), 1.2) : 1;
    }

    private static double harbingerConditionalMultiplier(LongPathFinalFormsMasteryTuning tuning,
                                                         LivingEntity owner, LivingEntity target) {
        double multiplier = finalOmenMultiplier(tuning, target);
        if (tuning.flag(131072) && LongPathFinalFormsMasteryCombatManager.isExecutionOmen(owner, target))
            multiplier *= tuning.get(s("EXECUTION_DAMAGE_MULTIPLIER"), 1.4);
        if (tuning.flag(65536) && target.hasStatusEffect(StatusEffects.WEAKNESS))
            multiplier *= tuning.get(s("PLAGUE_DAMAGE_MULTIPLIER"), .8);
        return multiplier;
    }

    private static void applyHarbingerPull(LivingEntity target, LivingEntity standard,
                                           LongPathFinalFormsMasteryTuning tuning, double radius, boolean cycle) {
        if (cycle) {
            pull(target, standard.getPos(), tuning.get(s("CYCLE_PULL_STRENGTH"), 1.5));
            return;
        }
        if (tuning.flag(16)) {
            pull(target, standard.getPos(), tuning.get(s("CONSTANT_PULL_STRENGTH"), 1.5));
            return;
        }
        if (target.distanceTo(standard) <= radius - 1) return;
        double scale = tuning.get(s("PULL_STRENGTH"), .25) / .25;
        target.setVelocity((standard.getX() - target.getX()) / 4 * scale,
                (standard.getY() - target.getY()) / 4 * scale,
                (standard.getZ() - target.getZ()) / 4 * scale);
        target.velocityModified = true;
    }

    private static LongPathFinalFormsMasteryTuning.Setting s(String name) {
        return LongPathFinalFormsMasteryTuning.Setting.valueOf(name);
    }

    private static final class StandardState {
        private final UUID ownerId;
        private final UUID entityId;
        private final boolean sunfire;
        private final UniqueAbilityExecution execution;
        private final ItemStack stack;
        private final Map<UUID, java.util.ArrayDeque<Long>> sunfirePulses = new HashMap<>();
        private final Map<UUID, Long> allyLocks = new HashMap<>();
        private final Map<UUID, Long> cleanseLocks = new HashMap<>();
        private boolean landed;
        private boolean supportRefunded;
        private int pulses;
        private int hits;
        private int refunded;

        private StandardState(UUID ownerId, UUID entityId, boolean sunfire,
                              UniqueAbilityExecution execution, ItemStack stack) {
            this.ownerId = ownerId;
            this.entityId = entityId;
            this.sunfire = sunfire;
            this.execution = execution;
            this.stack = stack.copy();
        }
    }
}
