package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.DeathShadowBloodMasteryTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeathShadowBloodWeaponRepairTest {
    @Test
    void plagueBonusesComposeAgainstConfiguration() {
        DeathShadowBloodMasteryTuning tuning = DeathShadowBloodMasteryTuning.EMPTY
                .with(s("PLAGUE_CONVERSION_CHANCE_BONUS"), 10)
                .with(s("PLAGUE_FEVER_DURATION_BONUS_TICKS"), 40)
                .with(s("PLAGUE_CONVERSION_FEVER_BONUS"), 2)
                .with(s("PLAGUE_TOLL_RADIUS_BONUS"), .75)
                .with(s("PLAGUE_FEVER_SPREAD_BONUS"), 1);
        assertEquals(47, PlagueMasteryTuningMath.conversionChance(37, tuning));
        assertEquals(240, PlagueMasteryTuningMath.feverDuration(200, tuning));
        assertEquals(7, PlagueMasteryTuningMath.conversionFever(5, tuning));
        assertEquals(7.25, PlagueMasteryTuningMath.tollRadius(6.5, tuning), 1.0E-6);
        assertEquals(4, PlagueMasteryTuningMath.feverSpread(3, tuning));
    }

    @Test
    void outbreakCapsAndRevisitsKeepPrimaryTollBudgetIndependent() {
        DeathShadowBloodMasteryTuning epidemic = DeathShadowBloodMasteryTuning.EMPTY
                .with(s("MODE"), (1 << 20) | (1 << 24))
                .with(s("PLAGUE_CASCADE_TOLL_BONUS"), 3)
                .with(s("PLAGUE_CASCADE_TOLL_CAP"), 9);
        assertEquals(9, PlagueMasteryTuningMath.maximumTolls(6, epidemic));

        DeathShadowBloodMasteryTuning rolling = epidemic
                .with(s("MODE"), epidemic.integer(s("MODE"), 0) | (1 << 25))
                .with(s("PLAGUE_REVISIT_TOLL_PENALTY"), 2);
        assertEquals(7, PlagueMasteryTuningMath.maximumTolls(6, rolling));

        DeathShadowBloodMasteryTuning quarantine = rolling
                .with(s("MODE"), rolling.integer(s("MODE"), 0) | (1 << 26));
        assertEquals(1, PlagueMasteryTuningMath.maximumTolls(40, quarantine));
    }

    @Test
    void carriedAilmentsAndCriticalConditionUseTheirActualState() {
        DeathShadowBloodMasteryTuning pestilence = DeathShadowBloodMasteryTuning.EMPTY
                .with(s("PLAGUE_CARRIED_DURATION_BONUS"), .1);
        DeathShadowBloodMasteryTuning outbreak = DeathShadowBloodMasteryTuning.EMPTY
                .with(s("PLAGUE_CARRIED_DURATION_BONUS"), .15);
        assertEquals(.75, PlagueMasteryTuningMath.carriedDurationMultiplier(.5F,
                pestilence, outbreak), 1.0E-6);
        assertFalse(PlagueMasteryTuningMath.criticalCondition(3, 5, .8));
        assertTrue(PlagueMasteryTuningMath.criticalCondition(4, 5, .8));
    }

    @Test
    void soulkeeperBonusesComposeAndCapstonesRetainDistinctCounts() {
        DeathShadowBloodMasteryTuning tuning = DeathShadowBloodMasteryTuning.EMPTY
                .with(s("SOUL_ORBIT_RADIUS_BONUS"), .35)
                .with(s("SOUL_ORBIT_RADIUS_MULTIPLIER"), 1.6)
                .with(s("SOUL_SPEED_GAIN_MULTIPLIER"), 1.25)
                .with(s("SOUL_SPEED_DECAY_MULTIPLIER"), .8)
                .with(s("SOUL_MAX_SPEED_BONUS"), .75);
        assertEquals(5.36, SoulkeeperLanternManager.orbitRadius(3, tuning), 1.0E-6);
        assertEquals(.375, SoulkeeperLanternManager.speedGain(.3, tuning), 1.0E-6);
        assertEquals(.16, SoulkeeperLanternManager.speedDecay(.2, tuning), 1.0E-6);
        assertEquals(7.75, SoulkeeperLanternManager.maximumSpeed(7, tuning), 1.0E-6);

        DeathShadowBloodMasteryTuning grand = tuning.with(s("MODE"), 1 << 25)
                .with(s("SOUL_GRAND_LANTERN_COUNT"), 6)
                .with(s("SOUL_GRAND_DURATION_TICKS"), 180)
                .with(s("SOUL_EXTRA_DURATION_BONUS_TICKS"), 60);
        assertEquals(6, SoulkeeperLanternManager.activeLanternCount(grand, true));
        assertEquals(240, SoulkeeperLanternManager.extraLanternDuration(500, grand));

        DeathShadowBloodMasteryTuning lone = tuning.with(s("MODE"), 1 << 26)
                .with(s("SOUL_LONE_LANTERN_COUNT"), 3)
                .with(s("SOUL_LONE_DURATION_TICKS"), 220)
                .with(s("SOUL_EXTRA_DURATION_BONUS_TICKS"), 60);
        assertEquals(3, SoulkeeperLanternManager.activeLanternCount(lone, true));
        assertEquals(280, SoulkeeperLanternManager.extraLanternDuration(500, lone));
    }

    private static DeathShadowBloodMasteryTuning.Setting s(String name) {
        return DeathShadowBloodMasteryTuning.Setting.valueOf(name);
    }
}
