package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase9AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase9UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Phase9CombatManager {
    private static final Map<ServerWorld, List<DelayedFinish>> FINISHES = new HashMap<>();
    private Phase9CombatManager() {
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown) {
        return beginActive(definition, context, cooldown, Phase9AbilityTuning.EMPTY);
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown,
                                                     Phase9AbilityTuning base) {
        Phase9AbilityTuning seeded = base.with(Phase9AbilityTuning.Setting.COOLDOWN_TICKS, cooldown);
        return UniqueAbilityApi.begin(definition, UniqueAbilityContext.active(context), tuning -> tuning
                .set(Phase9UniqueAbilities.TUNING, seeded)
                .set(Phase9UniqueAbilities.COOLDOWN_TICKS, cooldown));
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target) {
        return beginPassive(definition, world, stack, actor, target, Phase9AbilityTuning.EMPTY);
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target,
                                                      Phase9AbilityTuning base) {
        UniqueAbilityExecution outer = UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null),
                tuning -> tuning.set(Phase9UniqueAbilities.TUNING, base));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        if (outer != null) UniqueAbilityApi.publishStartedExecution(outer);
        return execution;
    }

    public static void finish(UniqueAbilityExecution execution, int affectedTargets) {
        if (execution != null) UniqueAbilityApi.finish(execution, Phase9UniqueAbilities.FINISH, affectedTargets);
    }

    public static void clear(ServerWorld world) {
        FINISHES.remove(world);
    }

    public static void clearAll() {
        FINISHES.clear();
    }

    public static void scheduleFinish(ServerWorld world, UniqueAbilityExecution execution, int delay,
                                      int affectedTargets) {
        if (execution == null) return;
        FINISHES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new DelayedFinish(
                execution, world.getTime() + Math.max(1, delay), affectedTargets));
    }

    public static boolean hasScheduled(ServerWorld world) {
        return !FINISHES.getOrDefault(world, List.of()).isEmpty();
    }

    public static void tick(ServerWorld world) {
        List<DelayedFinish> finishes = FINISHES.get(world);
        if (finishes == null) return;
        finishes.removeIf(finish -> {
            if (world.getTime() < finish.at) return false;
            finish(finish.execution, finish.affectedTargets);
            return true;
        });
        if (finishes.isEmpty()) FINISHES.remove(world);
    }

    private record DelayedFinish(UniqueAbilityExecution execution, long at, int affectedTargets) {
    }
}
