package net.sweenus.simplyswords.fabric.gametest;

import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.fabric.FabricHelperMethods;
import net.sweenus.simplyswords.gametest.BalanceGameTestSuite;
import net.sweenus.simplyswords.gametest.CompatibilityConfigGameTestSuite;
import net.sweenus.simplyswords.gametest.SpellPowerWeaponAttributeGameTestSuite;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public final class SimplySwordsBalanceGameTests {

    @GameTest(templateName = "simplyswords:balance_empty", tickLimit = 50000000)
    public void balanceHarnessLoads(TestContext context) {
        BalanceGameTestSuite.run(context);
    }

    @GameTest(templateName = "simplyswords:balance_empty", tickLimit = 20)
    public void cooldownReductionIsNoOpWithoutIrons(TestContext context) {
        ZombieEntity actor = context.spawnMob(EntityType.ZOMBIE, 1, 1, 1);
        ItemStack weapon = new ItemStack(ItemsRegistry.FLAMEWIND.get());
        int baseCooldown = 100;

        context.assertTrue(
                SimplySwordsAPI.getEffectiveWeaponCooldownTicks(weapon, actor, baseCooldown) == baseCooldown,
                "Fabric cooldown changed without Iron's Spells");
        context.complete();
    }

    @GameTest(templateName = "simplyswords:balance_empty", tickLimit = 20)
    public void spellPowerWeaponAttributes(TestContext context) {
        SpellPowerWeaponAttributeGameTestSuite.run(context);
    }

    @GameTest(templateName = "simplyswords:balance_empty", tickLimit = 20)
    public void compatibilityConfigWeaponIds(TestContext context) {
        CompatibilityConfigGameTestSuite.run(context);
    }

    @GameTest(templateName = "simplyswords:balance_empty", tickLimit = 20)
    public void spellPowerApiScalingMultiplier(TestContext context) {
        ZombieEntity actor = context.spawnMob(EntityType.ZOMBIE, 1, 1, 1);
        var settings = Config.compatibility.spellPowerApi.getUnconditional();
        float previousMultiplier = settings.scalingMultiplier.get();
        try {
            settings.scalingMultiplier.accept(1.0F);
            float fullValue = FabricHelperMethods.useSpellAttributeScaling(
                    1.0F, actor, SpellScalingProfile.FIRE.registryId());
            settings.scalingMultiplier.accept(0.0F);
            float disabledValue = FabricHelperMethods.useSpellAttributeScaling(
                    1.0F, actor, SpellScalingProfile.FIRE.registryId());

            context.assertTrue(fullValue > 0.0F, "Spell Power API returned no scaling value");
            context.assertTrue(disabledValue == 0.0F,
                    "Spell Power API scaling multiplier did not suppress the scaling value");
            context.complete();
        } finally {
            settings.scalingMultiplier.accept(previousMultiplier);
        }
    }
}
