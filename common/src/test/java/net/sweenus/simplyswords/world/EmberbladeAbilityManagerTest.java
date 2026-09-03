package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EmberbladeAbilityManagerTest {

    @Test
    void channelDurationUsesOnlyTheEmberbladeSetting() {
        FireForgeMasteryTuning unrelated = FireForgeMasteryTuning.EMPTY
                .with(FireForgeMasteryTuning.Setting.DURATION_TICKS, 999);
        FireForgeMasteryTuning siege = unrelated
                .with(FireForgeMasteryTuning.Setting.EMBERBLADE_CHANNEL_TICKS, 100);
        FireForgeMasteryTuning hairTrigger = unrelated
                .with(FireForgeMasteryTuning.Setting.EMBERBLADE_CHANNEL_TICKS, 40);

        assertEquals(80, EmberbladeAbilityManager.channelTicks(unrelated));
        assertEquals(100, EmberbladeAbilityManager.channelTicks(siege));
        assertEquals(40, EmberbladeAbilityManager.channelTicks(hairTrigger));
    }

    @Test
    void chargeUsesTheTunedChannelAndCapsCombinedBanks() {
        assertEquals(.5F, EmberbladeAbilityManager.chargeRatio(40, 80, 0), 1.0E-6);
        assertEquals(.4F, EmberbladeAbilityManager.chargeRatio(40, 100, 0), 1.0E-6);
        assertEquals(1F, EmberbladeAbilityManager.chargeRatio(40, 80, .75), 1.0E-6);
    }

    @Test
    void whiteHeatAndLateGuardUseTheirOwnFinalWindows() {
        assertFalse(EmberbladeAbilityManager.inFullChargeWindow(69, 80, 10));
        assertTrue(EmberbladeAbilityManager.inFullChargeWindow(70, 80, 10));
        assertFalse(EmberbladeAbilityManager.inFullChargeWindow(71, 80, 8));
        assertTrue(EmberbladeAbilityManager.inFullChargeWindow(72, 80, 8));
    }

    @Test
    void interruptedChargeRequiresTwoSecondsAndPreservesHalf() {
        assertEquals(0F, EmberbladeAbilityManager.interruptedBank(39, 80, .5, 40), 1.0E-6);
        assertEquals(.25F, EmberbladeAbilityManager.interruptedBank(40, 80, .5, 40), 1.0E-6);
        assertEquals(.5F, EmberbladeAbilityManager.interruptedBank(80, 80, .5, 40), 1.0E-6);
    }

    @Test
    void kindledIreAddsPercentagePointsWithoutFlatteningChargeScaling() {
        assertEquals(.12, EmberbladeAbilityManager.ireChance(0, 30, 0), 1.0E-6);
        assertEquals(.20, EmberbladeAbilityManager.ireChance(1, 30, 0), 1.0E-6);
        assertEquals(.22, EmberbladeAbilityManager.ireChance(0, 30, 10), 1.0E-6);
        assertEquals(.30, EmberbladeAbilityManager.ireChance(1, 30, 10), 1.0E-6);
    }

    @Test
    void whiteHeatBonusAppliesOnlyInsideTheFinalWindow() {
        assertEquals(1.25, EmberbladeAbilityManager.fullChargeBonus(true, 1.25), 1.0E-6);
        assertEquals(1, EmberbladeAbilityManager.fullChargeBonus(false, 1.25), 1.0E-6);
        assertEquals(1, EmberbladeAbilityManager.fullChargeBonus(true, 1), 1.0E-6);
        assertEquals(1.5, EmberbladeAbilityManager.fullChargeBonus(true, 1.5), 1.0E-6);
    }

    @Test
    void bankDurationsAreIndependentPerSource() {
        FireForgeMasteryTuning both = FireForgeMasteryTuning.EMPTY
                .with(FireForgeMasteryTuning.Setting.EMBERBLADE_BANK_DURATION_TICKS, 60)
                .with(FireForgeMasteryTuning.Setting.EMBERBLADE_FLAME_BANK_DURATION_TICKS, 100);

        assertEquals(60, both.integer(FireForgeMasteryTuning.Setting.EMBERBLADE_BANK_DURATION_TICKS, 0));
        assertEquals(100, both.integer(FireForgeMasteryTuning.Setting.EMBERBLADE_FLAME_BANK_DURATION_TICKS, 0));
    }

    @Test
    void lingeringFuryComposesWithBaseAndIncarnateDurations() {
        assertEquals(150, EmberbladeAbilityManager.ireDuration(150, 0));
        assertEquals(190, EmberbladeAbilityManager.ireDuration(150, 40));
        assertEquals(100, EmberbladeAbilityManager.ireDuration(100, 0));
        assertEquals(140, EmberbladeAbilityManager.ireDuration(100, 40));
    }
}
