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
    private static final Map<UUID, StormState> STORMS = new HashMap<>();

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
        int interval = Math.max(3, state.tuning.integer(Phase9AbilityTuning.Setting.INTERVAL_TICKS,
                Math.max(3, 10 - Math.min(6, state.refreshes * 2))));
        if (owner.age % interval != 0) return;
        ItemStack stack = owner.getMainHandStack();
        UniqueAbilityExecution strikeExecution = Phase9CombatManager.beginPassive(
                Phase9UniqueAbilities.MAGISCYTHE_STRIKES, world, stack, owner, null);
        Phase9AbilityTuning strikes = Phase9UniqueAbilities.tuning(strikeExecution);
        double radius = strikes.get(Phase9AbilityTuning.Setting.RADIUS,
                state.tuning.get(Phase9AbilityTuning.Setting.RADIUS, Config.uniqueEffects.magiscythe.radius));
        List<LivingEntity> targets = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class,
                owner.getBoundingBox().expand(radius, 1, radius), target -> target != owner
                        && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && HelperMethods.checkAbilityTarget(target, owner)));
        if (targets.isEmpty()) {
            Phase9CombatManager.finish(strikeExecution, 0);
            return;
        }
        if (strikes.flag(1 << 13)) targets.sort(Comparator.comparingDouble(
                target -> -target.getHealth() / target.getMaxHealth()));
        else targets.sort(Comparator.comparingDouble(target -> target.squaredDistanceTo(owner)));
        LivingEntity primary = targets.getFirst();
        float damage = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("magiscythe"), owner, stack,
                Config.uniqueEffects.magiscythe.damageScaling * (float) state.tuning.get(
                        Phase9AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1)
                        * (1 + state.refreshes * (float) state.tuning.get(
                        Phase9AbilityTuning.Setting.PER_STACK_MULTIPLIER, 0)),
                Config.uniqueEffects.magiscythe.spellScaling);
        if (strikes.flag(1 << 16)) damage *= .8F;
        damageTarget(world, owner, stack, primary, damage);
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
        attemptRefresh(world, owner, state);
        if (state.tuning.flag(1 << 26)) repair(world, owner, stack, true);
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
        repair(world, owner, stack, false);
    }

    private static void attemptRefresh(ServerWorld world, LivingEntity owner, StormState state) {
        int chance = state.tuning.integer(Phase9AbilityTuning.Setting.CHANCE, 5);
        if (chance <= 0 || owner.getRandom().nextInt(100) >= chance) return;
        int cap = state.tuning.integer(Phase9AbilityTuning.Setting.STACK_CAP, 5);
        state.refreshes = Math.min(cap, state.refreshes + 1);
        int restored = state.tuning.flag(1 << 7)
                ? state.tuning.integer(Phase9AbilityTuning.Setting.DURATION_TICKS, 80)
                : Config.uniqueEffects.magiscythe.duration;
        state.expiresAt = Math.max(state.expiresAt, world.getTime()) + restored;
        StatusEffectInstance effect = owner.getStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISTORM));
        owner.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.MAGISTORM),
                Math.max(restored, effect == null ? 0 : effect.getDuration()), Math.min(9, state.refreshes + 1)), owner);
    }

    private static void repair(ServerWorld world, LivingEntity owner, ItemStack weapon, boolean guaranteed) {
        UniqueAbilityExecution execution = Phase9CombatManager.beginPassive(
                Phase9UniqueAbilities.MAGISCYTHE_MAGEWRIGHT, world, weapon, owner, null);
        Phase9AbilityTuning tuning = Phase9UniqueAbilities.tuning(execution);
        StormState state = STORMS.computeIfAbsent(owner.getUuid(), ignored -> new StormState(
                world, Phase9AbilityTuning.EMPTY, null, world.getTime() + 20));
        float chance = (float) tuning.get(Phase9AbilityTuning.Setting.CHANCE,
                Config.uniqueEffects.magiscythe.repairChance * 100) + state.pity;
        if (!guaranteed && owner.getRandom().nextFloat() * 100 >= chance) {
            if (tuning.flag(1 << 22)) state.pity = Math.min(40, state.pity + 10);
            Phase9CombatManager.finish(execution, 0);
            return;
        }
        state.pity = 0;
        List<ItemStack> eligible = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = owner.getEquippedStack(slot);
            if (!item.isEmpty() && item.isDamageable() && item.getDamage() > 0) eligible.add(item);
        }
        if (eligible.isEmpty()) {
            Phase9CombatManager.finish(execution, 0);
            return;
        }
        ItemStack selected = tuning.flag(1 << 20) ? eligible.stream().max(Comparator.comparingDouble(
                item -> item.getDamage() / (double) item.getMaxDamage())).orElseThrow()
                : eligible.get(owner.getRandom().nextInt(eligible.size()));
        int amount = Math.max(1, tuning.integer(Phase9AbilityTuning.Setting.REPAIR_AMOUNT,
                Math.max(1, (int) HelperMethods.getEntityAttackDamage(owner))));
        if (tuning.flag(1 << 24) && ++state.repairs % 5 == 0) amount = Math.round(amount * 1.5F);
        selected.setDamage(Math.max(0, selected.getDamage() - amount));
        if (tuning.flag(1 << 23)) owner.addStatusEffect(new StatusEffectInstance(
                StatusEffects.ABSORPTION, 40, 0), owner);
        if (tuning.flag(1 << 26)) weapon.damage(2, owner,
                owner.getMainHandStack() == weapon ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        Phase9CombatManager.finish(execution, 1);
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

    private static final class StormState {
        private final ServerWorld world;
        private final Phase9AbilityTuning tuning;
        private final UniqueAbilityExecution execution;
        private long expiresAt;
        private int refreshes;
        private int strikes;
        private int pity;
        private int repairs;

        private StormState(ServerWorld world, Phase9AbilityTuning tuning,
                           UniqueAbilityExecution execution, long expiresAt) {
            this.world = world;
            this.tuning = tuning;
            this.execution = execution;
            this.expiresAt = expiresAt;
        }
    }
}
