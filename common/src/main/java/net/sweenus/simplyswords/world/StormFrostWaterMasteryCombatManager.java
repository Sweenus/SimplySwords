package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StormFrostWaterMasteryCombatManager {
    private static final Map<UUID, TempestExecution> TEMPEST = new HashMap<>();
    private StormFrostWaterMasteryCombatManager() {
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown) {
        return UniqueAbilityApi.begin(definition, UniqueAbilityContext.active(context), tuning -> tuning
                .set(StormFrostWaterMasteryAbilities.TUNING, StormFrostWaterMasteryTuning.EMPTY)
                .set(StormFrostWaterMasteryAbilities.COOLDOWN_TICKS, cooldown));
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target) {
        UniqueAbilityExecution execution = preparePassive(definition, world, stack, actor, target);
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        return execution;
    }

    public static UniqueAbilityExecution preparePassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                         ItemStack stack, LivingEntity actor, LivingEntity target) {
        return UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null),
                tuning -> tuning.set(StormFrostWaterMasteryAbilities.TUNING, StormFrostWaterMasteryTuning.EMPTY));
    }

    public static StormFrostWaterMasteryTuning startTempestVortex(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = beginActive(StormFrostWaterMasteryAbilities.TEMPEST_VORTEX, context, 200);
        UniqueAbilityApi.start(execution);
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryAbilities.tuning(execution);
        int duration = tuning.integer(StormFrostWaterMasteryTuning.Setting.DURATION_TICKS, 1200);
        TEMPEST.put(context.actor().getUuid(), new TempestExecution(context.world().getTime() + duration,
                tuning, execution));
        return tuning;
    }

    public static StormFrostWaterMasteryTuning tempestTuning(LivingEntity actor) {
        TempestExecution state = TEMPEST.get(actor.getUuid());
        if (state == null) return StormFrostWaterMasteryTuning.EMPTY;
        if (!(actor.getWorld() instanceof ServerWorld world) || world.getTime() > state.expiresAt) {
            TEMPEST.remove(actor.getUuid());
            UniqueAbilityApi.finish(state.execution, StormFrostWaterMasteryAbilities.FINISH, 0);
            return StormFrostWaterMasteryTuning.EMPTY;
        }
        return state.tuning;
    }

    private record TempestExecution(long expiresAt, StormFrostWaterMasteryTuning tuning,
                                    UniqueAbilityExecution execution) {
    }
}
