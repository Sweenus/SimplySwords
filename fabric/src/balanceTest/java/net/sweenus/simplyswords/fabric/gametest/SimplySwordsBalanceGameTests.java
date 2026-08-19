package net.sweenus.simplyswords.fabric.gametest;

import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.sweenus.simplyswords.gametest.BalanceGameTestSuite;

public final class SimplySwordsBalanceGameTests {

    @GameTest(templateName = "simplyswords:balance_empty", tickLimit = 50000000)
    public void balanceHarnessLoads(TestContext context) {
        BalanceGameTestSuite.run(context);
    }
}
