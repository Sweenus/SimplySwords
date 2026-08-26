package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.*;

public final class Phase10CombatManager {
    private Phase10CombatManager() {
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown) {
        return UniqueAbilityApi.begin(definition, UniqueAbilityContext.active(context), tuning -> tuning
                .set(Phase10UniqueAbilities.TUNING, Phase10AbilityTuning.EMPTY)
                .set(Phase10UniqueAbilities.COOLDOWN_TICKS, cooldown));
    }

    public static UniqueAbilityExecution beginDirectActive(UniqueAbilityDefinition definition,
                                                           ServerWorld world, LivingEntity actor,
                                                           ItemStack stack, Hand hand, int cooldown) {
        WeaponAbilityContext context = WeaponAbilityContext.of(world, stack, actor,
                actor instanceof net.minecraft.server.network.ServerPlayerEntity player ? player : null,
                null, hand, WeaponAbilityActivationSource.PLAYER);
        UniqueAbilityExecution execution = beginActive(definition, context, cooldown);
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        return execution;
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null),
                tuning -> tuning.set(Phase10UniqueAbilities.TUNING, Phase10AbilityTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        return execution;
    }

    public static void finish(UniqueAbilityExecution execution, int affectedTargets) {
        if (execution != null) UniqueAbilityApi.finish(execution, Phase10UniqueAbilities.FINISH, affectedTargets);
    }
}
