package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.api.ability.Phase9AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase9UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
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

    private static final Map<UUID, StormState> STORMS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Map<UUID, Long>>> MARKS = new HashMap<>();

    public static Phase9AbilityTuning stormBase(int duration, double radius, int refreshChance) {
        return Phase9AbilityTuning.EMPTY
                .with(Phase9AbilityTuning.Setting.DURATION_TICKS, duration)
                .with(Phase9AbilityTuning.Setting.RADIUS, radius)
                .with(Phase9AbilityTuning.Setting.CHANCE, refreshChance)
                .with(Phase9AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1)
                .with(Phase9AbilityTuning.Setting.STACK_CAP, 5);
    }

    public static Phase9AbilityTuning strikesBase(double radius) {
        return Phase9AbilityTuning.EMPTY
                .with(Phase9AbilityTuning.Setting.RADIUS, radius)
                .with(Phase9AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    public static Phase9AbilityTuning magewrightBase(double repairChancePercent) {
        return Phase9AbilityTuning.EMPTY
                .with(Phase9AbilityTuning.Setting.CHANCE, repairChancePercent);
    }

    private static Phase9AbilityTuning stormBase() {
        return stormBase(Config.uniqueEffects.magiscythe.duration, Config.uniqueEffects.magiscythe.radius, BASE_REFRESH_CHANCE);
    }

    private static Phase9AbilityTuning strikesBase() {
        return strikesBase(Config.uniqueEffects.magiscythe.radius);
    }

    private static Phase9AbilityTuning magewrightBase() {
        return magewrightBase(Config.uniqueEffects.magiscythe.repairChance * 100);
    }

    public static int strikeInterval(Phase9AbilityTuning storm, int refreshes) {
        int base = Math.max(3, BASE_STRIKE_INTERVAL - Math.min(6, refreshes * 2));
        return Math.max(3, base - storm.integer(Phase9AbilityTuning.Setting.DELAY_TICKS, 0));
    }

    public static void clear(ServerWorld world) {
        STORMS.entrySet().removeIf(entry -> {
            if (entry.getValue().world != world) return false;
            Phase9CombatManager.finish(entry.getValue().execution, entry.getValue().strikes);
            return true;
        });
        MARKS.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        StormState state = STORMS.remove(actor.getUuid());
        if (state != null) Phase9CombatManager.finish(state.execution, state.strikes);
        MARKS.values().forEach(marks -> marks.remove(actor.getUuid()));
    }

    public static void clearAll() {
        STORMS.values().forEach(state -> Phase9CombatManager.finish(state.execution, state.strikes));
        STORMS.clear();
        MARKS.clear();
    }

    private MagiscytheMasteryManager() {
    }

    public static int start(ServerWorld world, LivingEntity owner, ItemStack stack,
                            Phase9AbilityTuning tuning, UniqueAbilityExecution execution) {
        int duration = Math.max(1, tuning.integer(Phase9AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.magiscythe.duration));
        StormState replaced = STORMS.put(owner.getUuid(), new StormState(
                world, tuning, execution, world.getTime() + duration));
        if (replaced != null) Phase9CombatManager.finish(replaced.execution, replaced.strikes);
        if (tuning.flag(1 << 5)) owner.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE, duration, 0), owner);
        return duration;
    }

    public static void tick(LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) return;
        StormState state = STORMS.get(owner.getUuid());
        if (state == null) state = new StormState(world, Phase9AbilityTuning.EMPTY, null,
                world.getTime() + Config.uniqueEffects.magiscythe.duration);
        if (world.getTime() >= state.expiresAt || !owner.hasStatusEffect(
                EffectRegistry.getReference(EffectRegistry.MAGISTORM))) {
            Phase9CombatManager.finish(state.execution, state.strikes);
            STORMS.remove(owner.getUuid());
            return;
        }
        int interval = state.tuning.flag(1 << 8)
                ? Math.max(3, state.tuning.integer(Phase9AbilityTuning.Setting.INTERVAL_TICKS,
                        BASE_STRIKE_INTERVAL))
                : strikeInterval(state.tuning, state.refreshes);
        if (state.markCadenceUntil > world.getTime())
            interval = Math.max(3, interval - state.markCadenceBonus);
        if (owner.age % interval != 0) return;
        ItemStack stack = owner.getMainHandStack();
        UniqueAbilityExecution strikeExecution = Phase9CombatManager.beginPassive(
                Phase9UniqueAbilities.MAGISCYTHE_STRIKES, world, stack, owner, null, strikesBase());
        Phase9AbilityTuning strikes = Phase9UniqueAbilities.tuning(strikeExecution);
        UniqueAbilityExecution wrightExecution = Phase9CombatManager.beginPassive(
                Phase9UniqueAbilities.MAGISCYTHE_MAGEWRIGHT, world, stack, owner, null, magewrightBase());
        Phase9AbilityTuning wright = Phase9UniqueAbilities.tuning(wrightExecution);
        double radius = strikes.get(Phase9AbilityTuning.Setting.RADIUS,
                state.tuning.get(Phase9AbilityTuning.Setting.RADIUS, Config.uniqueEffects.magiscythe.radius));
        Map<UUID, Long> marks = MARKS.getOrDefault(world, Map.of()).getOrDefault(owner.getUuid(), Map.of());
        boolean markedOnly = strikes.flag(1 << 17);
        List<LivingEntity> targets = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class,
                owner.getBoundingBox().expand(radius, 1, radius), target -> target != owner
                        && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && HelperMethods.checkAbilityTarget(target, owner)
                        && (!markedOnly || marks.getOrDefault(target.getUuid(), 0L) > world.getTime())));
        if (targets.isEmpty()) {
            Phase9CombatManager.finish(wrightExecution, 0);
            Phase9CombatManager.finish(strikeExecution, 0);
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
                        Phase9AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1)
                        * (float) strikes.get(Phase9AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1)
                        * (float) wright.get(Phase9AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1)
                        * (1 + state.refreshes * (float) state.tuning.get(
                        Phase9AbilityTuning.Setting.PER_STACK_MULTIPLIER, 0)),
                Config.uniqueEffects.magiscythe.spellScaling);
        if (strikes.flag(1 << 12)) damage *= 1 + Math.min(strikes.integer(Phase9AbilityTuning.Setting.STACK_CAP, 4),
                state.repeatStrikes) * (float) strikes.get(Phase9AbilityTuning.Setting.PER_STACK_MULTIPLIER, .05);
        if (strikes.flag(1 << 16)) damage *= .8F;
        damageTarget(world, owner, stack, primary, damage);
        if (!primary.isAlive() && wright.flag(1 << 21)
                && world.getTime() >= state.salvageReadyAt) {
            state.salvageReadyAt = world.getTime() + wright.integer(Phase9AbilityTuning.Setting.LOCKOUT_TICKS, 40);
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
        Phase9CombatManager.finish(strikeExecution, 1 + chainCap);
        if (wright.flag(1 << 25)) performRepair(world, owner, stack, wright, state);
        Phase9CombatManager.finish(wrightExecution, 0);
        attemptRefresh(world, owner, state);
    }

    public static void markStormTarget(ServerWorld world, LivingEntity owner, LivingEntity target,
                                       Phase9AbilityTuning strikes, StormState state) {
        int duration = strikes.integer(Phase9AbilityTuning.Setting.DURATION_TICKS, 60);
        Map<UUID, Long> marks = MARKS.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(owner.getUuid(), ignored -> new HashMap<>());
        marks.values().removeIf(expiry -> expiry <= world.getTime());
        marks.put(target.getUuid(), world.getTime() + duration);
        if (marks.size() > 32) marks.entrySet().stream().min(Map.Entry.comparingByValue())
                .ifPresent(entry -> marks.remove(entry.getKey()));
        if (state != null && strikes.flag(1 << 15) && world.getTime() >= state.markCadenceReadyAt) {
            state.markCadenceUntil = world.getTime()
                    + strikes.integer(Phase9AbilityTuning.Setting.DURATION_TICKS, 60);
            state.markCadenceReadyAt = world.getTime()
                    + strikes.integer(Phase9AbilityTuning.Setting.LOCKOUT_TICKS, 20);
            state.markCadenceBonus = strikes.integer(Phase9AbilityTuning.Setting.DELAY_TICKS, 2);
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
            Phase9CombatManager.finish(state.execution, state.strikes);
            return true;
        });
    }

    public static void onWeaponHit(ServerWorld world, LivingEntity owner, ItemStack stack, LivingEntity target) {
        if (!owner.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISTORM))) return;
        UniqueAbilityExecution strikeExecution = Phase9CombatManager.beginPassive(
                Phase9UniqueAbilities.MAGISCYTHE_STRIKES, world, stack, owner, target, strikesBase());
        Phase9AbilityTuning strikes = Phase9UniqueAbilities.tuning(strikeExecution);
        if (strikes.flag(1 << 15) || strikes.flag(1 << 17))
            markStormTarget(world, owner, target, strikes, STORMS.get(owner.getUuid()));
        Phase9CombatManager.finish(strikeExecution, 1);
        repair(world, owner, stack, false);
    }

    private static void attemptRefresh(ServerWorld world, LivingEntity owner, StormState state) {
        int chance = state.tuning.integer(Phase9AbilityTuning.Setting.CHANCE, 5);
        if (chance <= 0 || owner.getRandom().nextInt(100) >= chance) return;
        int cap = state.tuning.integer(Phase9AbilityTuning.Setting.STACK_CAP, 5);
        state.refreshes = Math.min(cap, state.refreshes + 1);
        int restored = state.tuning.flag(1 << 7)
                ? state.tuning.integer(Phase9AbilityTuning.Setting.SECONDARY_DURATION_TICKS, 80)
                : Config.uniqueEffects.magiscythe.duration;
        state.expiresAt = Math.max(state.expiresAt, world.getTime()) + restored;
        StatusEffectInstance effect = owner.getStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISTORM));
        owner.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.MAGISTORM),
                Math.max(restored, effect == null ? 0 : effect.getDuration()), Math.min(9, state.refreshes + 1)), owner);
    }

    private static void repair(ServerWorld world, LivingEntity owner, ItemStack weapon, boolean guaranteed) {
        UniqueAbilityExecution execution = Phase9CombatManager.beginPassive(
                Phase9UniqueAbilities.MAGISCYTHE_MAGEWRIGHT, world, weapon, owner, null, magewrightBase());
        Phase9AbilityTuning tuning = Phase9UniqueAbilities.tuning(execution);
        StormState state = STORMS.computeIfAbsent(owner.getUuid(), ignored -> new StormState(
                world, Phase9AbilityTuning.EMPTY, null, world.getTime() + 20));
        float chance = (float) tuning.get(Phase9AbilityTuning.Setting.CHANCE,
                Config.uniqueEffects.magiscythe.repairChance * 100)
                + (tuning.flag(1 << 22) ? state.pity : 0);
        if (!guaranteed && owner.getRandom().nextFloat() * 100 >= chance) {
            if (tuning.flag(1 << 22)) state.pity = Math.min(
                    tuning.integer(Phase9AbilityTuning.Setting.STACK_CAP, 40),
                    state.pity + tuning.integer(Phase9AbilityTuning.Setting.PITY_CHANCE, 10));
            Phase9CombatManager.finish(execution, 0);
            return;
        }
        performRepair(world, owner, weapon, tuning, state);
        Phase9CombatManager.finish(execution, 1);
    }

    private static void performRepair(ServerWorld world, LivingEntity owner, ItemStack weapon,
                                      Phase9AbilityTuning tuning, StormState state) {
        if (state == null) state = STORMS.computeIfAbsent(owner.getUuid(), ignored -> new StormState(
                world, Phase9AbilityTuning.EMPTY, null, world.getTime() + 20));
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
        int amount = Math.max(1, base + tuning.integer(Phase9AbilityTuning.Setting.REPAIR_AMOUNT, 0));
        amount = Math.max(1, Math.round(amount * (float) tuning.get(
                Phase9AbilityTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1)));
        if (tuning.flag(1 << 24) && ++state.repairs % Math.max(1,
                tuning.integer(Phase9AbilityTuning.Setting.COUNT, 5)) == 0)
            amount = Math.round(amount * (float) tuning.get(
                    Phase9AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1.5));
        selected.setDamage(Math.max(0, selected.getDamage() - amount));
        if (tuning.flag(1 << 23)) owner.addStatusEffect(new StatusEffectInstance(
                StatusEffects.ABSORPTION, 40, 0), owner);
        if (tuning.flag(1 << 26)) weapon.damage(tuning.integer(Phase9AbilityTuning.Setting.TERTIARY_TARGET_CAP, 2),
                owner, owner.getMainHandStack() == weapon ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    private static void damageTarget(ServerWorld world, LivingEntity owner, ItemStack stack,
                                     LivingEntity target, float damage) {
        var source = world.getDamageSources().indirectMagic(owner, owner);
        HelperMethods.applyDamageWithoutKnockback(target, source,
                HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, damage));
    }

    private static void pulse(ServerWorld world, LivingEntity owner, ItemStack stack, LivingEntity center,
                              float damage, double radius, int cap) {
        world.getEntitiesByClass(LivingEntity.class, center.getBoundingBox().expand(radius),
                        target -> target != center && HelperMethods.checkAbilityTarget(target, owner))
                .stream().limit(cap).forEach(target -> damageTarget(world, owner, stack, target, damage));
    }

    static final class StormState {
        private final ServerWorld world;
        private final Phase9AbilityTuning tuning;
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

        private StormState(ServerWorld world, Phase9AbilityTuning tuning,
                           UniqueAbilityExecution execution, long expiresAt) {
            this.world = world;
            this.tuning = tuning;
            this.execution = execution;
            this.expiresAt = expiresAt;
        }
    }
}
