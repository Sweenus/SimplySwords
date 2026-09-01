package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase5AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase5UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Phase5MoltenManager {
    private static final Map<UUID, Snapshot> VENTS = new HashMap<>();

    private Phase5MoltenManager() {
    }

    public static Phase5AbilityTuning heat(ServerWorld world, ItemStack stack, LivingEntity actor) {
        UniqueAbilityExecution execution = Phase5CombatManager.beginPassive(Phase5UniqueAbilities.MOLTEN_EDGE_HEAT,
                world, stack, actor, null);
        Phase5AbilityTuning tuning = Phase5UniqueAbilities.tuning(execution);
        UniqueAbilityApi.finish(execution, Phase5UniqueAbilities.FINISH, 0);
        return tuning;
    }

    public static Snapshot beginVent(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = Phase5CombatManager.beginActive(Phase5UniqueAbilities.MOLTEN_EDGE_VENT,
                context, Config.uniqueEffects.molten_edge.cooldown);
        UniqueAbilityApi.start(execution);
        Snapshot snapshot = new Snapshot(execution, Phase5UniqueAbilities.tuning(execution));
        VENTS.put(context.actor().getUuid(), snapshot);
        return snapshot;
    }

    public static Snapshot beginRupture(ServerWorld world, ItemStack stack, LivingEntity actor) {
        UniqueAbilityExecution execution = Phase5CombatManager.beginPassive(Phase5UniqueAbilities.MOLTEN_EDGE_RUPTURE,
                world, stack, actor, null);
        return new Snapshot(execution, Phase5UniqueAbilities.tuning(execution));
    }

    public static Phase5AbilityTuning vent(UUID ownerId) {
        Snapshot snapshot = VENTS.get(ownerId);
        return snapshot == null ? Phase5AbilityTuning.EMPTY : snapshot.tuning;
    }

    public static void finish(UUID ownerId, int hits) {
        Snapshot snapshot = VENTS.remove(ownerId);
        if (snapshot != null) UniqueAbilityApi.finish(snapshot.execution, Phase5UniqueAbilities.FINISH, hits);
    }

    public static void cancel(UUID ownerId) {
        Snapshot snapshot = VENTS.remove(ownerId);
        if (snapshot != null) UniqueAbilityApi.cancel(snapshot.execution);
    }

    public static void finishRupture(Snapshot snapshot, int hits) {
        if (snapshot != null && !snapshot.execution.isTerminal()) {
            UniqueAbilityApi.finish(snapshot.execution, Phase5UniqueAbilities.FINISH, hits);
        }
    }

    public static void cancelRupture(Snapshot snapshot) {
        if (snapshot != null && !snapshot.execution.isTerminal()) UniqueAbilityApi.cancel(snapshot.execution);
    }

    public static void clear(ServerWorld world) {
        VENTS.entrySet().removeIf(entry -> {
            Snapshot snapshot = entry.getValue();
            if (snapshot.execution.context().world() != world) return false;
            UniqueAbilityApi.cancel(snapshot.execution);
            return true;
        });
    }

    public static void clearAll() {
        VENTS.values().forEach(snapshot -> UniqueAbilityApi.cancel(snapshot.execution));
        VENTS.clear();
    }

    public record Snapshot(UniqueAbilityExecution execution, Phase5AbilityTuning tuning) {
    }
}
