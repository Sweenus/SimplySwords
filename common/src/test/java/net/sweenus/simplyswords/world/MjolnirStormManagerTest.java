package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MjolnirStormManagerTest {

    private static final int CONFIG_DURATION = 200;
    private static final int CONFIG_FREQUENCY = 10;
    private static final int CONFIG_RADIUS = 10;
    private static final int CONFIG_CONDUCTIVE = 120;
    private static final int CONFIG_FINAL_BOLTS = 3;
    private static final double CONFIG_FINAL_RADIUS = 6.0;

    private static Phase6AbilityTuning tuning() {
        return Phase6AbilityTuning.EMPTY;
    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }

    @Test
    void swellingFrontAddsTwoBlocksToTheConfiguredRadiusRatherThanToALiteral() {
        Phase6AbilityTuning owned = tuning().with(s("MJOLNIR_STORM_RADIUS_BONUS"), 2);
        assertEquals(12, MjolnirStormManager.stormRadius(owned, CONFIG_RADIUS), 1.0E-6);
        assertEquals(CONFIG_RADIUS, MjolnirStormManager.stormRadius(tuning(), CONFIG_RADIUS), 1.0E-6);
        assertEquals(16, MjolnirStormManager.stormRadius(owned, 14), 1.0E-6);
    }

    @Test
    void suddenTempestCutsARealQuarterAndComposesWithSwellingFront() {
        Phase6AbilityTuning capstone = tuning().with(s("MJOLNIR_STORM_RADIUS_MULTIPLIER"), .75);
        assertEquals(7.5, MjolnirStormManager.stormRadius(capstone, CONFIG_RADIUS), 1.0E-6);
        assertEquals(9, MjolnirStormManager.stormRadius(
                capstone.with(s("MJOLNIR_STORM_RADIUS_BONUS"), 2), CONFIG_RADIUS), 1.0E-6);
    }

    @Test
    void stormRadiusAndFinalRadiusNoLongerShareOneKey() {
        Phase6AbilityTuning skybreaker = tuning().with(s("MJOLNIR_FINAL_RADIUS_BONUS"), 1.5);
        assertEquals(7.5, MjolnirStormManager.finalRadius(skybreaker, CONFIG_FINAL_RADIUS), 1.0E-6);
        assertEquals(CONFIG_RADIUS, MjolnirStormManager.stormRadius(skybreaker, CONFIG_RADIUS), 1.0E-6);

        Phase6AbilityTuning swelling = tuning().with(s("MJOLNIR_STORM_RADIUS_BONUS"), 2);
        assertEquals(CONFIG_FINAL_RADIUS, MjolnirStormManager.finalRadius(swelling, CONFIG_FINAL_RADIUS), 1.0E-6);
    }

    @Test
    void lingeringCloudAndEndlessSquallComposeAgainstTheConfiguredDuration() {
        Phase6AbilityTuning opening = tuning().with(s("MJOLNIR_DURATION_BONUS_TICKS"), 40);
        assertEquals(240, MjolnirStormManager.resolveDuration(opening, CONFIG_DURATION));
        assertEquals(340, MjolnirStormManager.resolveDuration(opening, 300));

        Phase6AbilityTuning squall = opening.with(s("MJOLNIR_DURATION_MULTIPLIER"), 2);
        assertEquals(480, MjolnirStormManager.resolveDuration(squall, CONFIG_DURATION));

        Phase6AbilityTuning tempest = squall.with(s("MJOLNIR_DURATION_TICKS"), 100);
        assertEquals(100, MjolnirStormManager.resolveDuration(tempest, CONFIG_DURATION));
    }

    @Test
    void conductiveRainAddsTwoSecondsToTheConfiguredConductiveDuration() {
        Phase6AbilityTuning owned = tuning().with(s("MJOLNIR_CONDUCTIVE_BONUS_TICKS"), 40);
        assertEquals(160, MjolnirStormManager.conductiveDuration(owned, CONFIG_CONDUCTIVE));
        assertEquals(CONFIG_CONDUCTIVE, MjolnirStormManager.conductiveDuration(tuning(), CONFIG_CONDUCTIVE));
    }

    @Test
    void stormstrideSpeedDurationCannotReachTheConductiveDuration() {
        Phase6AbilityTuning stormstride = tuning().with(s("MJOLNIR_SPEED_DURATION_TICKS"), 100);
        assertEquals(CONFIG_CONDUCTIVE, MjolnirStormManager.conductiveDuration(stormstride, CONFIG_CONDUCTIVE));
    }

    @Test
    void forkedBoltNoLongerChangesTheFinalBoltCount() {
        Phase6AbilityTuning fork = tuning().with(s("MJOLNIR_FORK_COUNT"), 4);
        assertEquals(CONFIG_FINAL_BOLTS, MjolnirStormManager.finalBoltCount(fork, CONFIG_FINAL_BOLTS));

        Phase6AbilityTuning judgment = fork.with(s("MJOLNIR_FINAL_BOLT_COUNT"), 4);
        assertEquals(4, MjolnirStormManager.finalBoltCount(judgment, CONFIG_FINAL_BOLTS));
    }

    @Test
    void hammerfallAndThunderWakeRadiiCannotShrinkTheStorm() {
        Phase6AbilityTuning combat = tuning()
                .with(s("MJOLNIR_ENTRY_RADIUS"), 2.5)
                .with(s("MJOLNIR_WAKE_RADIUS"), 2)
                .with(s("MJOLNIR_ANCHOR_RADIUS"), 3);
        assertEquals(CONFIG_RADIUS, MjolnirStormManager.stormRadius(combat, CONFIG_RADIUS), 1.0E-6);
        assertEquals(CONFIG_FINAL_RADIUS, MjolnirStormManager.finalRadius(combat, CONFIG_FINAL_RADIUS), 1.0E-6);
    }

    @Test
    void chargedFinaleCountsConductiveTargetsAndStopsAtItsCap() {
        Phase6AbilityTuning finale = tuning()
                .with(s("MJOLNIR_FINALE_PER_TARGET_MULTIPLIER"), .05)
                .with(s("MJOLNIR_FINALE_TARGET_CAP"), 6);
        assertEquals(100, MjolnirStormManager.finaleDamage(finale, 100, 0), 1.0E-4);
        assertEquals(115, MjolnirStormManager.finaleDamage(finale, 100, 3), 1.0E-4);
        assertEquals(130, MjolnirStormManager.finaleDamage(finale, 100, 6), 1.0E-4);
        assertEquals(130, MjolnirStormManager.finaleDamage(finale, 100, 20), 1.0E-4);
    }

    @Test
    void endlessSquallSuppressesTheClapInsteadOfMultiplyingItsDamageByZero() {
        Phase6AbilityTuning squall = tuning().with(s("MODE"), MjolnirStormManager.MODE_ENDLESS_SQUALL);
        assertTrue(MjolnirStormManager.suppressesFinalClap(squall));
        assertFalse(MjolnirStormManager.suppressesFinalClap(tuning()));
    }

    @Test
    void wrathOfThunderSuppressesDefensiveGrantsAndKeepsSkybreakerComposed() {
        Phase6AbilityTuning wrath = tuning().with(s("MODE"), MjolnirStormManager.MODE_WRATH_OF_THUNDER);
        assertFalse(MjolnirStormManager.grantsDefensiveBuffs(wrath));
        assertTrue(MjolnirStormManager.grantsDefensiveBuffs(tuning()));

        Phase6AbilityTuning both = tuning()
                .multiply(s("MJOLNIR_FINAL_DAMAGE_MULTIPLIER"), 1.25, 1)
                .multiply(s("MJOLNIR_FINAL_DAMAGE_MULTIPLIER"), 2, 1);
        assertEquals(2.5, both.get(s("MJOLNIR_FINAL_DAMAGE_MULTIPLIER"), 1), 1.0E-6);
    }

    @Test
    void rapidThunderAndSuddenTempestSetRealCadences() {
        assertEquals(CONFIG_FREQUENCY, MjolnirStormManager.pulseInterval(tuning(), CONFIG_FREQUENCY));
        assertEquals(9, MjolnirStormManager.pulseInterval(
                tuning().with(s("MJOLNIR_PULSE_INTERVAL_TICKS"), 9), CONFIG_FREQUENCY));
        assertEquals(6, MjolnirStormManager.pulseInterval(
                tuning().with(s("MJOLNIR_PULSE_INTERVAL_TICKS"), 6), CONFIG_FREQUENCY));
    }

    @Test
    void genericKeysStillReachTheirScopedConsumersForAddons() {
        assertEquals(8, MjolnirStormManager.stormRadius(tuning().with(s("RADIUS"), 8), CONFIG_RADIUS), 1.0E-6);
        assertEquals(48, MjolnirStormManager.conductiveDuration(
                tuning().with(s("STATUS_DURATION_TICKS"), 48), CONFIG_CONDUCTIVE));
        assertEquals(5, MjolnirStormManager.finalBoltCount(tuning().with(s("COUNT"), 5), CONFIG_FINAL_BOLTS));
        assertEquals(7, MjolnirStormManager.pulseInterval(
                tuning().with(s("INTERVAL_TICKS"), 7), CONFIG_FREQUENCY));
    }
}
