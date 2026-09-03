package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.*;

public final class MartialCommandEldritchMasteryCombatManager {
    private MartialCommandEldritchMasteryCombatManager() {
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown) {
        return beginActive(definition, context, cooldown, MartialCommandEldritchMasteryTuning.EMPTY);
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown,
                                                     MartialCommandEldritchMasteryTuning base) {
        MartialCommandEldritchMasteryTuning seeded = base.with(MartialCommandEldritchMasteryTuning.Setting.COOLDOWN_TICKS, cooldown);
        return UniqueAbilityApi.begin(definition, UniqueAbilityContext.active(context), tuning -> tuning
                .set(MartialCommandEldritchMasteryAbilities.TUNING, seeded)
                .set(MartialCommandEldritchMasteryAbilities.COOLDOWN_TICKS, cooldown));
    }

    public static UniqueAbilityExecution beginDirectActive(UniqueAbilityDefinition definition,
                                                           ServerWorld world, LivingEntity actor,
                                                           ItemStack stack, Hand hand, int cooldown) {
        return beginDirectActive(definition, world, actor, stack, hand, cooldown, MartialCommandEldritchMasteryTuning.EMPTY);
    }

    public static UniqueAbilityExecution beginDirectActive(UniqueAbilityDefinition definition,
                                                           ServerWorld world, LivingEntity actor,
                                                           ItemStack stack, Hand hand, int cooldown,
                                                           MartialCommandEldritchMasteryTuning base) {
        WeaponAbilityContext context = WeaponAbilityContext.of(world, stack, actor,
                actor instanceof net.minecraft.server.network.ServerPlayerEntity player ? player : null,
                null, hand, WeaponAbilityActivationSource.PLAYER);
        UniqueAbilityExecution execution = beginActive(definition, context, cooldown, base);
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        return execution;
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target) {
        return beginPassive(definition, world, stack, actor, target, MartialCommandEldritchMasteryTuning.EMPTY);
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target,
                                                      MartialCommandEldritchMasteryTuning base) {
        UniqueAbilityExecution outer = UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null),
                tuning -> tuning.set(MartialCommandEldritchMasteryAbilities.TUNING, base));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        if (outer != null) UniqueAbilityApi.publishStartedExecution(outer);
        return execution;
    }

    public static void finish(UniqueAbilityExecution execution, int affectedTargets) {
        if (execution != null) UniqueAbilityApi.finish(execution, MartialCommandEldritchMasteryAbilities.FINISH, affectedTargets);
    }
}
