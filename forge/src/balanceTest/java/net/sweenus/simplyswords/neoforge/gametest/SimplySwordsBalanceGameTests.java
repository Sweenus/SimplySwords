package net.sweenus.simplyswords.neoforge.gametest;

import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.sweenus.simplyswords.gametest.BalanceGameTestSuite;

public final class SimplySwordsBalanceGameTests {

    @GameTest(templateName = "balance_empty", tickLimit = 50000000)
    @EmptyTemplate(value = "16x8x16")
    @TestHolder("simplyswords.balance")
    public static void balanceHarnessLoads(TestContext context) {
        BalanceGameTestSuite.run(context);
    }
}
