package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.item.custom.IcewhisperSwordItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IcewhisperCometManagerTest {

    @Test
    void auraAndStormRadiiComposeAgainstTheirOwnConfiguredValues() {
        Phase6AbilityTuning spreadingCold = Phase6AbilityTuning.EMPTY
                .add(s("ICEWHISPER_AURA_RADIUS_BONUS"), 1, 0);

        assertEquals(5, IcewhisperSwordItem.auraRadius(spreadingCold, 4), 1.0E-6);
        assertEquals(8, IcewhisperSwordItem.stormRadius(spreadingCold, 8), 1.0E-6);

        Phase6AbilityTuning killingCold = spreadingCold
                .multiply(s("ICEWHISPER_AURA_RADIUS_MULTIPLIER"), .65, 1);
        assertEquals(3.25, IcewhisperSwordItem.auraRadius(killingCold, 4), 1.0E-6);
        assertEquals(8, IcewhisperSwordItem.stormRadius(killingCold, 8), 1.0E-6);
    }

    @Test
    void permafrostSlowStepsUpOnePerPulseAndStopsAtTheCap() {
        assertEquals(0, IcewhisperSwordItem.slowAmplifier(0, false, 2));
        assertEquals(1, IcewhisperSwordItem.slowAmplifier(0, true, 2));
        assertEquals(2, IcewhisperSwordItem.slowAmplifier(1, true, 2));
        assertEquals(2, IcewhisperSwordItem.slowAmplifier(2, true, 2));
        assertEquals(4, IcewhisperSwordItem.slowAmplifier(4, true, 4));
    }

    @Test
    void twinWakeAddsItsCometOnlyOnEveryThirdWave() {
        Phase6AbilityTuning twinWake = Phase6AbilityTuning.EMPTY
                .with(s("MODE"), IcewhisperAbilityManager.MODE_TWIN_WAKE)
                .with(s("ICEWHISPER_EXTRA_COMET_WAVE_INTERVAL"), 3)
                .with(s("ICEWHISPER_EXTRA_COMET_COUNT"), 1);

        assertEquals(2, IcewhisperCometManager.cometCount(twinWake, 2, 0));
        assertEquals(2, IcewhisperCometManager.cometCount(twinWake, 2, 1));
        assertEquals(3, IcewhisperCometManager.cometCount(twinWake, 2, 2));
        assertEquals(2, IcewhisperCometManager.cometCount(twinWake, 2, 3));
    }

    @Test
    void hailstormDoublesTheConfiguredCountAndExtinctionCometSuppressesExtras() {
        Phase6AbilityTuning twinWake = Phase6AbilityTuning.EMPTY
                .with(s("MODE"), IcewhisperAbilityManager.MODE_TWIN_WAKE)
                .with(s("ICEWHISPER_EXTRA_COMET_WAVE_INTERVAL"), 3)
                .with(s("ICEWHISPER_EXTRA_COMET_COUNT"), 1);

        Phase6AbilityTuning hailstorm = twinWake.multiply(s("ICEWHISPER_COMET_COUNT_MULTIPLIER"), 2, 1);
        assertEquals(4, IcewhisperCometManager.cometCount(hailstorm, 2, 0));
        assertEquals(5, IcewhisperCometManager.cometCount(hailstorm, 2, 2));

        Phase6AbilityTuning extinction = twinWake.with(s("ICEWHISPER_COMET_COUNT"), 1);
        assertEquals(1, IcewhisperCometManager.cometCount(extinction, 2, 0));
        assertEquals(1, IcewhisperCometManager.cometCount(extinction, 2, 2));
    }

    @Test
    void cometCountsStayBoundedNoMatterWhatTheTuningAsksFor() {
        Phase6AbilityTuning runaway = Phase6AbilityTuning.EMPTY
                .with(s("ICEWHISPER_COMET_COUNT"), 60);
        assertEquals(IcewhisperCometManager.MAX_COMETS_PER_WAVE,
                IcewhisperCometManager.cometCount(runaway, 2, 0));

        Phase6AbilityTuning multiplied = Phase6AbilityTuning.EMPTY
                .with(s("ICEWHISPER_COMET_COUNT_MULTIPLIER"), 10);
        assertTrue(IcewhisperCometManager.cometCount(multiplied, 2, 0)
                <= IcewhisperCometManager.MAX_COMETS_PER_WAVE);
    }

    @Test
    void cadenceSplashAndDurationKeysComposeAgainstTheConfiguration() {
        Phase6AbilityTuning base = Phase6AbilityTuning.EMPTY;
        assertEquals(14, IcewhisperCometManager.waveInterval(base, 14));
        assertEquals(20, IcewhisperCometManager.fallTicks(base, 20));
        assertEquals(2.5, IcewhisperCometManager.splashRadius(base, 2.5), 1.0E-6);
        assertEquals(200, IcewhisperCometManager.stormDuration(base, 200));

        Phase6AbilityTuning tuned = base
                .add(s("ICEWHISPER_FALL_REDUCTION_TICKS"), 4, 0)
                .add(s("ICEWHISPER_SPLASH_RADIUS_BONUS"), .5, 0)
                .with(s("ICEWHISPER_WAVE_INTERVAL_TICKS"), 12)
                .add(s("ICEWHISPER_STORM_DURATION_BONUS_TICKS"), 40, 0);
        assertEquals(12, IcewhisperCometManager.waveInterval(tuned, 14));
        assertEquals(16, IcewhisperCometManager.fallTicks(tuned, 20));
        assertEquals(3.0, IcewhisperCometManager.splashRadius(tuned, 2.5), 1.0E-6);
        assertEquals(240, IcewhisperCometManager.stormDuration(tuned, 200));

        Phase6AbilityTuning absolute = tuned.with(s("ICEWHISPER_SPLASH_RADIUS"), 4);
        assertEquals(4.0, IcewhisperCometManager.splashRadius(absolute, 2.5), 1.0E-6);
    }

    @Test
    void fallTimeNeverReachesZeroAndTheAuraNeverLosesItsRadius() {
        Phase6AbilityTuning extreme = Phase6AbilityTuning.EMPTY
                .with(s("ICEWHISPER_FALL_REDUCTION_TICKS"), 400)
                .with(s("ICEWHISPER_AURA_RADIUS_MULTIPLIER"), 0);
        assertEquals(1, IcewhisperCometManager.fallTicks(extreme, 20));
        assertEquals(1, IcewhisperSwordItem.auraRadius(extreme, 4), 1.0E-6);
    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }
}
