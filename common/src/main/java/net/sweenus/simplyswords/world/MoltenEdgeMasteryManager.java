package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MoltenEdgeMasteryManager {
    private static final Map<UUID, Snapshot> VENTS = new HashMap<>();

    private MoltenEdgeMasteryManager() {
    }

    public static FireForgeMasteryTuning heat(ServerWorld world, ItemStack stack, LivingEntity actor) {
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginPassive(FireForgeMasteryAbilities.MOLTEN_EDGE_HEAT,
                world, stack, actor, null);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
        return tuning;
    }

    public static Snapshot beginVent(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginActive(FireForgeMasteryAbilities.MOLTEN_EDGE_VENT,
                context, Config.uniqueEffects.molten_edge.cooldown);
        UniqueAbilityApi.start(execution);
        Snapshot snapshot = new Snapshot(execution, FireForgeMasteryAbilities.tuning(execution));
        VENTS.put(context.actor().getUuid(), snapshot);
        return snapshot;
    }

    public static Snapshot beginRupture(ServerWorld world, ItemStack stack, LivingEntity actor) {
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginPassive(FireForgeMasteryAbilities.MOLTEN_EDGE_RUPTURE,
                world, stack, actor, null);
        return new Snapshot(execution, FireForgeMasteryAbilities.tuning(execution));
    }

    public static FireForgeMasteryTuning vent(UUID ownerId) {
        Snapshot snapshot = VENTS.get(ownerId);
        return snapshot == null ? FireForgeMasteryTuning.EMPTY : snapshot.tuning;
    }

    public static void finish(UUID ownerId, int hits) {
        Snapshot snapshot = VENTS.remove(ownerId);
        if (snapshot != null) UniqueAbilityApi.finish(snapshot.execution, FireForgeMasteryAbilities.FINISH, hits);
    }

    public static void cancel(UUID ownerId) {
        Snapshot snapshot = VENTS.remove(ownerId);
        if (snapshot != null) UniqueAbilityApi.cancel(snapshot.execution);
    }

    public static void finishRupture(Snapshot snapshot, int hits) {
        if (snapshot != null && !snapshot.execution.isTerminal()) {
            UniqueAbilityApi.finish(snapshot.execution, FireForgeMasteryAbilities.FINISH, hits);
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

    public record Snapshot(UniqueAbilityExecution execution, FireForgeMasteryTuning tuning) {
    }
}
