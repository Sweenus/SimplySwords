package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ShadowstingRepairTest {
    @Test
    void cloneChanceComposesBonusAndCapstonePenalty() {
        Phase8AbilityTuning eager = tuning((1 << 0), "SHADOW_CHANCE_BONUS", 8);
        assertEquals(48, ShadowstingShadowDanceManager.cloneChance(40, eager));

        Phase8AbilityTuning mirror = eager.with(s("MODE"), (1 << 0) | (1 << 7))
                .with(s("SHADOW_CHANCE_PENALTY"), 15);
        assertEquals(33, ShadowstingShadowDanceManager.cloneChance(40, mirror));
        assertEquals(3, ShadowstingShadowDanceManager.cloneChance(10, mirror));
    }

    @Test
    void cloneDelayCountAndDamageStayOnTheirOwnChannels() {
        Phase8AbilityTuning quick = tuning(1 << 2, "SHADOW_CLONE_DELAY_BONUS_TICKS", -1);
        assertEquals(2, ShadowstingShadowDanceManager.cloneDelay(3, quick));

        Phase8AbilityTuning sharp = tuning(1 << 1, "SHADOW_CLONE_DAMAGE_MULTIPLIER", 1.12);
        assertEquals(1.12, ShadowstingShadowDanceManager.cloneDamageMultiplier(sharp), 1.0E-6);
        assertEquals(1, ShadowstingShadowDanceManager.cloneCount(sharp));

        Phase8AbilityTuning mirror = sharp.with(s("MODE"), (1 << 1) | (1 << 7))
                .with(s("SHADOW_MIRROR_COUNT"), 3)
                .with(s("SHADOW_MIRROR_DAMAGE_MULTIPLIER"), .45);
        assertEquals(3, ShadowstingShadowDanceManager.cloneCount(mirror));
        assertEquals(.504, ShadowstingShadowDanceManager.cloneDamageMultiplier(mirror), 1.0E-6);

        Phase8AbilityTuning flawless = sharp.with(s("MODE"), (1 << 1) | (1 << 8))
                .with(s("SHADOW_FLAWLESS_DAMAGE_MULTIPLIER"), 1.25);
        assertEquals(1.4, ShadowstingShadowDanceManager.cloneDamageMultiplier(flawless), 1.0E-6);
    }

    @Test
    void danceDurationAndIntervalComposeAgainstConfiguration() {
        Phase8AbilityTuning secondAct = tuning(1 << 9, "SHADOW_DANCE_DURATION_BONUS_TICKS", 20);
        assertEquals(70, ShadowstingShadowDanceManager.danceDuration(50, 4, secondAct));

        Phase8AbilityTuning quickstep = secondAct.with(s("MODE"), (1 << 9) | (1 << 10))
                .with(s("SHADOW_DANCE_INTERVAL_BONUS"), -1)
                .with(s("SHADOW_DANCE_INTERVAL_FLOOR"), 3);
        assertEquals(3, ShadowstingShadowDanceManager.strikeInterval(4, quickstep));

        Phase8AbilityTuning macabre = quickstep.with(s("MODE"), (1 << 9) | (1 << 10) | (1 << 16))
                .with(s("SHADOW_DANCE_DURATION_BONUS_TICKS"), 80)
                .with(s("SHADOW_DANCE_INTERVAL_BONUS"), 1)
                .with(s("SHADOW_MACABRE_DAMAGE_MULTIPLIER"), .65);
        assertEquals(130, ShadowstingShadowDanceManager.danceDuration(50, 4, macabre));
        assertEquals(5, ShadowstingShadowDanceManager.strikeInterval(4, macabre));
        assertEquals(.65, ShadowstingShadowDanceManager.danceStrikeMultiplier(macabre), 1.0E-6);

        Phase8AbilityTuning waltz = secondAct.with(s("MODE"), (1 << 9) | (1 << 17))
                .with(s("SHADOW_DANCE_DURATION_MULTIPLIER"), .5)
                .with(s("SHADOW_WALTZ_DAMAGE_MULTIPLIER"), 1.5);
        assertEquals(35, ShadowstingShadowDanceManager.danceDuration(50, 4, waltz));
        assertEquals(1.5, ShadowstingShadowDanceManager.danceStrikeMultiplier(waltz), 1.0E-6);
    }

    @Test
    void killingStepShortensTheDanceByExactlyTwoStrikes() {
        Phase8AbilityTuning killing = tuning(1 << 26, "SHADOW_KILLING_SKIPPED_STRIKES", 2);
        assertEquals(42, ShadowstingShadowDanceManager.danceDuration(50, 4, killing));
    }

    @Test
    void radiusChainAndArrivalReadDedicatedSettings() {
        Phase8AbilityTuning stage = tuning(1 << 11, "SHADOW_DANCE_RADIUS_BONUS", 2);
        assertEquals(12, ShadowstingShadowDanceManager.danceRadius(10, stage), 1.0E-6);

        Phase8AbilityTuning flourish = stage.with(s("MODE"), (1 << 11) | (1 << 15))
                .with(s("SHADOW_FLOURISH_RADIUS"), 3);
        assertEquals(12, ShadowstingShadowDanceManager.danceRadius(10, flourish), 1.0E-6);

        Phase8AbilityTuning chain = tuning(1 << 12, "SHADOW_CHAIN_DAMAGE_PER_STEP", .05)
                .with(s("SHADOW_CHAIN_STEP_CAP"), 5);
        assertEquals(1, ShadowstingShadowDanceManager.chainMultiplier(0, chain), 1.0E-6);
        assertEquals(1.15, ShadowstingShadowDanceManager.chainMultiplier(3, chain), 1.0E-6);
        assertEquals(1.25, ShadowstingShadowDanceManager.chainMultiplier(9, chain), 1.0E-6);
        assertEquals(1, ShadowstingShadowDanceManager.chainMultiplier(9, Phase8AbilityTuning.EMPTY), 1.0E-6);

        Phase8AbilityTuning stray = tuning(1 << 18, "SHADOW_ARRIVAL_DISTANCE_BONUS", 1);
        assertEquals(2.35, ShadowstingShadowDanceManager.arrivalDistance(stray), 1.0E-6);
        assertEquals(1.35, ShadowstingShadowDanceManager.arrivalDistance(Phase8AbilityTuning.EMPTY), 1.0E-6);

        Phase8AbilityTuning anchor = tuning(1 << 21, "SHADOW_RETURN_BONUS_TICKS", -2);
        assertEquals(6, ShadowstingShadowDanceManager.returnTicks(anchor));
        assertEquals(8, ShadowstingShadowDanceManager.returnTicks(Phase8AbilityTuning.EMPTY));
    }

    private static Phase8AbilityTuning tuning(int mode, String setting, double value) {
        return Phase8AbilityTuning.EMPTY.with(s("MODE"), mode).with(s(setting), value);
    }

    private static Phase8AbilityTuning.Setting s(String name) {
        return Phase8AbilityTuning.Setting.valueOf(name);
    }
}
