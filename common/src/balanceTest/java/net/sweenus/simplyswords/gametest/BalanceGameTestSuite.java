package net.sweenus.simplyswords.gametest;

import net.minecraft.test.TestContext;
import net.sweenus.simplyswords.config.Config;

public final class BalanceGameTestSuite {

    private BalanceGameTestSuite() {
    }

    public static void run(TestContext context) {
        context.assertTrue(Config.general.ironsSpellBasePower > 0.0F, "Iron's base spell power must be positive");
        context.assertTrue(Config.general.spellScalingDiminishingReturnsStart >= 1.0F, "Diminishing returns must start at or above one");
        context.assertTrue(Config.general.spellScalingDiminishingReturnsStrength >= 0.0F, "Diminishing returns strength must not be negative");
        BalanceHarness harness = new BalanceHarness(context);
        scheduleNextTick(context, harness);
    }

    // runAtEveryTick would register one callback per tick up to the tick limit.
    private static void scheduleNextTick(TestContext context, BalanceHarness harness) {
        context.runAtTick(context.getTick() + 1, () -> {
            if (harness.isFinished()) {
                return;
            }
            scheduleNextTick(context, harness);
            harness.tick();
        });
    }
}
