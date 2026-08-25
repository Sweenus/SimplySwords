package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Phase6CombatManager {
    private static final Map<UUID, TempestExecution> TEMPEST = new HashMap<>();
    private Phase6CombatManager() {
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown) {
        return UniqueAbilityApi.begin(definition, UniqueAbilityContext.active(context), tuning -> tuning
                .set(Phase6UniqueAbilities.TUNING, Phase6AbilityTuning.EMPTY)
                .set(Phase6UniqueAbilities.COOLDOWN_TICKS, cooldown));
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null),
                tuning -> tuning.set(Phase6UniqueAbilities.TUNING, Phase6AbilityTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        return execution;
    }

    public static Phase6AbilityTuning startTempestVortex(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = beginActive(Phase6UniqueAbilities.TEMPEST_VORTEX, context, 200);
        UniqueAbilityApi.start(execution);
        Phase6AbilityTuning tuning = Phase6UniqueAbilities.tuning(execution);
        int duration = tuning.integer(Phase6AbilityTuning.Setting.DURATION_TICKS, 1200);
        TEMPEST.put(context.actor().getUuid(), new TempestExecution(context.world().getTime() + duration,
                tuning, execution));
        return tuning;
    }

    public static Phase6AbilityTuning tempestTuning(LivingEntity actor) {
        TempestExecution state = TEMPEST.get(actor.getUuid());
        if (state == null) return Phase6AbilityTuning.EMPTY;
        if (!(actor.getWorld() instanceof ServerWorld world) || world.getTime() > state.expiresAt) {
            TEMPEST.remove(actor.getUuid());
            UniqueAbilityApi.finish(state.execution, Phase6UniqueAbilities.FINISH, 0);
            return Phase6AbilityTuning.EMPTY;
        }
        return state.tuning;
    }

    private record TempestExecution(long expiresAt, Phase6AbilityTuning tuning,
                                    UniqueAbilityExecution execution) {
    }
}
