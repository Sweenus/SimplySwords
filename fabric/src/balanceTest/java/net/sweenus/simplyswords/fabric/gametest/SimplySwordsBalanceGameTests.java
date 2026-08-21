package net.sweenus.simplyswords.fabric.gametest;

import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.gametest.BalanceGameTestSuite;
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
}
