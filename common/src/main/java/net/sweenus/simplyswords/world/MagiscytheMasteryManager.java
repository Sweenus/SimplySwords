package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryTuning;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MagiscytheMasteryManager {
    private static final int BASE_STRIKE_INTERVAL = 10;
    private static final int BASE_REFRESH_CHANCE = 5;
    private static final double STRIKE_HEIGHT = 4.0;

    private static final Map<UUID, StormState> STORMS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Map<UUID, Long>>> MARKS = new HashMap<>();

    public static ArcaneCosmicMasteryTuning stormBase(int duration, double radius, int refreshChance) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, duration)
                .with(ArcaneCosmicMasteryTuning.Setting.RADIUS, radius)
                .with(ArcaneCosmicMasteryTuning.Setting.CHANCE, refreshChance)
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                .with(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, 5);
    }

    public static ArcaneCosmicMasteryTuning strikesBase(double radius) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.RADIUS, radius)
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    public static ArcaneCosmicMasteryTuning magewrightBase(double repairChancePercent) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.CHANCE, repairChancePercent);
    }

    private static ArcaneCosmicMasteryTuning stormBase() {
        return stormBase(Config.uniqueEffects.magiscythe.duration, Config.uniqueEffects.magiscythe.radius, BASE_REFRESH_CHANCE);
    }

    private static ArcaneCosmicMasteryTuning strikesBase() {
        return strikesBase(Config.uniqueEffects.magiscythe.radius);
    }

    private static ArcaneCosmicMasteryTuning magewrightBase() {
        return magewrightBase(Config.uniqueEffects.magiscythe.repairChance * 100);
    }

    public static int strikeInterval(ArcaneCosmicMasteryTuning storm, int refreshes) {
        int base = Math.max(3, BASE_STRIKE_INTERVAL - Math.min(6, refreshes * 2));
        return Math.max(3, base - storm.integer(ArcaneCosmicMasteryTuning.Setting.DELAY_TICKS, 0));
    }

    public static void clear(ServerWorld world) {
        STORMS.entrySet().removeIf(entry -> {
            if (entry.getValue().world != world) return false;
            ArcaneCosmicMasteryCombatManager.finish(entry.getValue().execution, entry.getValue().strikes);
            return true;
        });
        MARKS.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        StormState state = STORMS.remove(actor.getUuid());
        if (state != null) ArcaneCosmicMasteryCombatManager.finish(state.execution, state.strikes);
        MARKS.values().forEach(marks -> marks.remove(actor.getUuid()));
    }

    public static void clearAll() {
        STORMS.values().forEach(state -> ArcaneCosmicMasteryCombatManager.finish(state.execution, state.strikes));
        STORMS.clear();
        MARKS.clear();
    }

    private MagiscytheMasteryManager() {
    }

    public static int start(ServerWorld world, LivingEntity owner, ItemStack stack,
                            ArcaneCosmicMasteryTuning tuning, UniqueAbilityExecution execution) {
        int duration = Math.max(1, tuning.integer(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.magiscythe.duration));
        StormState replaced = STORMS.put(owner.getUuid(), new StormState(
                world, tuning, execution, world.getTime() + duration));
        if (replaced != null) ArcaneCosmicMasteryCombatManager.finish(replaced.execution, replaced.strikes);
        if (tuning.flag(1 << 5)) owner.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE, duration, 0), owner);
        return duration;
    }

    public static void tick(LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) return;
        StormState state = STORMS.get(owner.getUuid());
        if (state == null) state = new StormState(world, ArcaneCosmicMasteryTuning.EMPTY, null,
                world.getTime() + Config.uniqueEffects.magiscythe.duration);
        if (world.getTime() >= state.expiresAt || !owner.hasStatusEffect(
                EffectRegistry.getReference(EffectRegistry.MAGISTORM))) {
            ArcaneCosmicMasteryCombatManager.finish(state.execution, state.strikes);
            STORMS.remove(owner.getUuid());
            return;
        }
        int interval = state.tuning.flag(1 << 8)
                ? Math.max(3, state.tuning.integer(ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS,
                        BASE_STRIKE_INTERVAL))
                : strikeInterval(state.tuning, state.refreshes);
        if (state.markCadenceUntil > world.getTime())
            interval = Math.max(3, interval - state.markCadenceBonus);
        if (owner.age % interval != 0) return;
        ItemStack stack = owner.getMainHandStack();
        UniqueAbilityExecution strikeExecution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.MAGISCYTHE_STRIKES, world, stack, owner, null, strikesBase());
        ArcaneCosmicMasteryTuning strikes = ArcaneCosmicMasteryAbilities.tuning(strikeExecution);
        UniqueAbilityExecution wrightExecution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.MAGISCYTHE_MAGEWRIGHT, world, stack, owner, null, magewrightBase());
        ArcaneCosmicMasteryTuning wright = ArcaneCosmicMasteryAbilities.tuning(wrightExecution);
        double radius = strikes.get(ArcaneCosmicMasteryTuning.Setting.RADIUS,
                state.tuning.get(ArcaneCosmicMasteryTuning.Setting.RADIUS, Config.uniqueEffects.magiscythe.radius));
        Map<UUID, Long> marks = MARKS.getOrDefault(world, Map.of()).getOrDefault(owner.getUuid(), Map.of());
        boolean markedOnly = strikes.flag(1 << 17);
        List<LivingEntity> targets = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class,
                owner.getBoundingBox().expand(radius, 1, radius), target -> target != owner
                        && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && HelperMethods.checkAbilityTarget(target, owner)
                        && (!markedOnly || marks.getOrDefault(target.getUuid(), 0L) > world.getTime())));
        if (targets.isEmpty()) {
            ArcaneCosmicMasteryCombatManager.finish(wrightExecution, 0);
            ArcaneCosmicMasteryCombatManager.finish(strikeExecution, 0);
            return;
        }
        if (strikes.flag(1 << 13)) targets.sort(Comparator.comparingDouble(
                target -> -target.getHealth() / target.getMaxHealth()));
        else targets.sort(Comparator.comparingDouble(target -> target.squaredDistanceTo(owner)));
        LivingEntity primary = targets.getFirst();
        if (primary.getUuid().equals(state.lastStruck)) state.repeatStrikes++;
        else state.repeatStrikes = 0;
        state.lastStruck = primary.getUuid();
        float damage = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("magiscythe"), owner, stack,
                Config.uniqueEffects.magiscythe.damageScaling * (float) state.tuning.get(
                        ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                        * (float) strikes.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                        * (float) wright.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                        * (1 + state.refreshes * (float) state.tuning.get(
                        ArcaneCosmicMasteryTuning.Setting.PER_STACK_MULTIPLIER, 0)),
                Config.uniqueEffects.magiscythe.spellScaling);
        if (strikes.flag(1 << 12)) damage *= 1 + Math.min(strikes.integer(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, 4),
                state.repeatStrikes) * (float) strikes.get(ArcaneCosmicMasteryTuning.Setting.PER_STACK_MULTIPLIER, .05);
        if (strikes.flag(1 << 16)) damage *= .8F;
        damageTarget(world, owner, stack, primary, damage);
        world.playSoundFromEntity(null, owner, SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_03.get(),
                SoundCategory.PLAYERS, 0.1f, 1.0f + owner.getRandom().nextFloat());
        if (!primary.isAlive() && wright.flag(1 << 21)
                && world.getTime() >= state.salvageReadyAt) {
            state.salvageReadyAt = world.getTime() + wright.integer(ArcaneCosmicMasteryTuning.Setting.LOCKOUT_TICKS, 40);
            performRepair(world, owner, stack, wright, state);
        }
        if (strikes.flag(1 << 11)) primary.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS, 30, 0), owner);
        if (strikes.flag(1 << 14)) pulse(world, owner, stack, primary, damage * .25F, 2, 4);
        int chainCap = strikes.flag(1 << 16) ? 4 : strikes.flag(1 << 10) && state.strikes % 3 == 2 ? 1 : 0;
        float chainDamage = damage;
        LivingEntity last = primary;
        for (int i = 0; i < chainCap; i++) {
            LivingEntity origin = last;
            LivingEntity next = targets.stream().filter(target -> target != primary && target != origin
                            && target.squaredDistanceTo(origin) <= 9)
                    .findFirst().orElse(null);
            if (next == null) break;
            chainDamage *= strikes.flag(1 << 16) ? i == 0 ? .7F : i == 1 ? .786F : .727F : .55F;
            damageTarget(world, owner, stack, next, chainDamage);
            last = next;
        }
        state.strikes++;
        ArcaneCosmicMasteryCombatManager.finish(strikeExecution, 1 + chainCap);
        if (wright.flag(1 << 25)) performRepair(world, owner, stack, wright, state);
        ArcaneCosmicMasteryCombatManager.finish(wrightExecution, 0);
        attemptRefresh(world, owner, state);
    }

    public static void markStormTarget(ServerWorld world, LivingEntity owner, LivingEntity target,
                                       ArcaneCosmicMasteryTuning strikes, StormState state) {
        int duration = strikes.integer(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, 60);
        Map<UUID, Long> marks = MARKS.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(owner.getUuid(), ignored -> new HashMap<>());
        marks.values().removeIf(expiry -> expiry <= world.getTime());
        marks.put(target.getUuid(), world.getTime() + duration);
        if (marks.size() > 32) marks.entrySet().stream().min(Map.Entry.comparingByValue())
                .ifPresent(entry -> marks.remove(entry.getKey()));
        if (state != null && strikes.flag(1 << 15) && world.getTime() >= state.markCadenceReadyAt) {
            state.markCadenceUntil = world.getTime()
                    + strikes.integer(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, 60);
            state.markCadenceReadyAt = world.getTime()
                    + strikes.integer(ArcaneCosmicMasteryTuning.Setting.LOCKOUT_TICKS, 20);
            state.markCadenceBonus = strikes.integer(ArcaneCosmicMasteryTuning.Setting.DELAY_TICKS, 2);
        }
    }

    public static boolean hasActive(ServerWorld world) {
        return STORMS.values().stream().anyMatch(state -> state.world == world);
    }

    public static void tickWorld(ServerWorld world) {
        STORMS.entrySet().removeIf(entry -> {
            StormState state = entry.getValue();
            if (state.world != world || world.getTime() < state.expiresAt
                    && world.getEntity(entry.getKey()) instanceof LivingEntity living
                    && living.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISTORM))) return false;
            ArcaneCosmicMasteryCombatManager.finish(state.execution, state.strikes);
            return true;
        });
    }

    public static void onWeaponHit(ServerWorld world, LivingEntity owner, ItemStack stack, LivingEntity target) {
        if (!owner.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISTORM))) return;
        world.playSound(null, owner.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                owner.getSoundCategory(), 0.1f, 1.9f);
        UniqueAbilityExecution strikeExecution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.MAGISCYTHE_STRIKES, world, stack, owner, target, strikesBase());
        ArcaneCosmicMasteryTuning strikes = ArcaneCosmicMasteryAbilities.tuning(strikeExecution);
        if (strikes.flag(1 << 15) || strikes.flag(1 << 17))
            markStormTarget(world, owner, target, strikes, STORMS.get(owner.getUuid()));
        ArcaneCosmicMasteryCombatManager.finish(strikeExecution, 1);
        repair(world, owner, stack, false);
    }

    private static void attemptRefresh(ServerWorld world, LivingEntity owner, StormState state) {
        int chance = state.tuning.integer(ArcaneCosmicMasteryTuning.Setting.CHANCE, 5);
        int roll = owner.getRandom().nextInt(100);
        boolean passed = chance > 0 && roll < chance;
        UniqueAbilityApi.reportRoll(owner, ArcaneCosmicMasteryAbilities.MAGISCYTHE_STORM.id(),
                "CHANCE", chance, roll, passed);
        if (!passed) return;
        int cap = state.tuning.integer(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, 5);
        state.refreshes = Math.min(cap, state.refreshes + 1);
        int restored = state.tuning.flag(1 << 7)
                ? state.tuning.integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DURATION_TICKS, 80)
                : Config.uniqueEffects.magiscythe.duration;
        state.expiresAt = Math.max(state.expiresAt, world.getTime()) + restored;
        StatusEffectInstance effect = owner.getStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISTORM));
        owner.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.MAGISTORM),
                Math.max(restored, effect == null ? 0 : effect.getDuration()), Math.min(9, state.refreshes + 1)), owner);
    }

    private static void repair(ServerWorld world, LivingEntity owner, ItemStack weapon, boolean guaranteed) {
        UniqueAbilityExecution execution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.MAGISCYTHE_MAGEWRIGHT, world, weapon, owner, null, magewrightBase());
        ArcaneCosmicMasteryTuning tuning = ArcaneCosmicMasteryAbilities.tuning(execution);
        StormState state = STORMS.computeIfAbsent(owner.getUuid(), ignored -> new StormState(
                world, ArcaneCosmicMasteryTuning.EMPTY, null, world.getTime() + 20));
        float chance = (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.CHANCE,
                Config.uniqueEffects.magiscythe.repairChance * 100)
                + (tuning.flag(1 << 22) ? state.pity : 0);
        float repairRoll = owner.getRandom().nextFloat() * 100;
        boolean repairProc = guaranteed || repairRoll < chance;
        UniqueAbilityApi.reportRoll(owner, ArcaneCosmicMasteryAbilities.MAGISCYTHE_MAGEWRIGHT.id(),
                guaranteed ? "CHANCE (guaranteed)" : "CHANCE", chance, repairRoll, repairProc);
        if (!repairProc) {
            if (tuning.flag(1 << 22)) state.pity = Math.min(
                    tuning.integer(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, 40),
                    state.pity + tuning.integer(ArcaneCosmicMasteryTuning.Setting.PITY_CHANCE, 10));
            ArcaneCosmicMasteryCombatManager.finish(execution, 0);
            return;
        }
        performRepair(world, owner, weapon, tuning, state);
        ArcaneCosmicMasteryCombatManager.finish(execution, 1);
    }

    private static void performRepair(ServerWorld world, LivingEntity owner, ItemStack weapon,
                                      ArcaneCosmicMasteryTuning tuning, StormState state) {
        if (state == null) state = STORMS.computeIfAbsent(owner.getUuid(), ignored -> new StormState(
                world, ArcaneCosmicMasteryTuning.EMPTY, null, world.getTime() + 20));
        state.pity = 0;
        List<ItemStack> eligible = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = owner.getEquippedStack(slot);
            if (!item.isEmpty() && item.isDamageable() && item.getDamage() > 0) eligible.add(item);
        }
        if (eligible.isEmpty()) return;
        ItemStack selected = tuning.flag(1 << 20) ? eligible.stream().max(Comparator.comparingDouble(
                item -> item.getDamage() / (double) item.getMaxDamage())).orElseThrow()
                : eligible.get(owner.getRandom().nextInt(eligible.size()));
        int base = Math.max(1, (int) HelperMethods.getEntityAttackDamage(owner));
        int amount = Math.max(1, base + tuning.integer(ArcaneCosmicMasteryTuning.Setting.REPAIR_AMOUNT, 0));
        amount = Math.max(1, Math.round(amount * (float) tuning.get(
                ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1)));
        if (tuning.flag(1 << 24) && ++state.repairs % Math.max(1,
                tuning.integer(ArcaneCosmicMasteryTuning.Setting.COUNT, 5)) == 0)
            amount = Math.round(amount * (float) tuning.get(
                    ArcaneCosmicMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1.5));
        selected.setDamage(Math.max(0, selected.getDamage() - amount));
        if (tuning.flag(1 << 23)) owner.addStatusEffect(new StatusEffectInstance(
                StatusEffects.ABSORPTION, 40, 0), owner);
        if (tuning.flag(1 << 26)) weapon.damage(tuning.integer(ArcaneCosmicMasteryTuning.Setting.TERTIARY_TARGET_CAP, 2),
                owner, owner.getMainHandStack() == weapon ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    private static void damageTarget(ServerWorld world, LivingEntity owner, ItemStack stack,
                                     LivingEntity target, float damage) {
        var source = world.getDamageSources().indirectMagic(owner, owner);
        target.timeUntilRegen = 0;
        HelperMethods.applyDamageWithoutKnockback(target, source,
                HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, damage));
        target.timeUntilRegen = 0;
        HelperMethods.spawnRainingParticles(world, ParticleTypes.ENCHANT, target, 20, STRIKE_HEIGHT);
        HelperMethods.spawnRainingParticles(world, ParticleTypes.GLOW, target, 4, STRIKE_HEIGHT);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.GLOW, 0.5, 6);
    }

    private static void pulse(ServerWorld world, LivingEntity owner, ItemStack stack, LivingEntity center,
                              float damage, double radius, int cap) {
        world.getEntitiesByClass(LivingEntity.class, center.getBoundingBox().expand(radius),
                        target -> target != center && HelperMethods.checkAbilityTarget(target, owner))
                .stream().limit(cap).forEach(target -> damageTarget(world, owner, stack, target, damage));
    }

    static final class StormState {
        private final ServerWorld world;
        private final ArcaneCosmicMasteryTuning tuning;
        private final UniqueAbilityExecution execution;
        private long expiresAt;
        private int refreshes;
        private int strikes;
        private int pity;
        private int repairs;
        private long markCadenceUntil;
        private long markCadenceReadyAt;
        private UUID lastStruck;
        private int repeatStrikes;
        private long salvageReadyAt;
        private int markCadenceBonus;

        private StormState(ServerWorld world, ArcaneCosmicMasteryTuning tuning,
                           UniqueAbilityExecution execution, long expiresAt) {
            this.world = world;
            this.tuning = tuning;
            this.execution = execution;
            this.expiresAt = expiresAt;
        }
    }
}
