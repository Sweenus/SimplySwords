package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueAbilityDiagnosticsTest {

    @Test
    void defaultSinkIsInertAndReplaceable() {
        assertSame(UniqueAbilityDiagnostics.NONE, UniqueAbilityApi.diagnostics());
        UniqueAbilityDiagnostics sink = new UniqueAbilityDiagnostics() {
        };
        try {
            UniqueAbilityApi.setDiagnostics(sink);
            assertSame(sink, UniqueAbilityApi.diagnostics());
            UniqueAbilityApi.setDiagnostics(null);
            assertSame(UniqueAbilityDiagnostics.NONE, UniqueAbilityApi.diagnostics());
        } finally {
            UniqueAbilityApi.setDiagnostics(null);
        }
    }

    @Test
    void reportRollIgnoresMissingActorAndInertSink() {
        UniqueAbilityApi.reportRoll(null, Identifier.of("simplyswords", "test"), "CHANCE", 20, 63, false);
    }

    @Test
    void cohortTuningExposesOnlyAssignedEntries() {
        StormSoulMasteryTuning tuning = StormSoulMasteryTuning.EMPTY
                .with(StormSoulMasteryTuning.Setting.CHANCE, 20)
                .add(StormSoulMasteryTuning.Setting.ECHO_CHANCE, 5, 0);
        Map<String, Double> entries = tuning.entries();
        assertEquals(2, entries.size());
        assertEquals(20.0, entries.get("CHANCE"));
        assertEquals(5.0, entries.get("ECHO_CHANCE"));
        assertTrue(StormSoulMasteryTuning.EMPTY.entries().isEmpty());
    }

    @Test
    void everyCohortTuningIsASnapshot() {
        assertNotNull(StormSoulMasteryTuning.EMPTY.entries());
        assertNotNull(FireForgeMasteryTuning.EMPTY.entries());
        assertNotNull(DeathShadowBloodMasteryTuning.EMPTY.entries());
        assertNotNull(ArcaneCosmicMasteryTuning.EMPTY.entries());
        assertNotNull(NatureSwarmMasteryTuning.EMPTY.entries());
        assertNotNull(StormFrostWaterMasteryTuning.EMPTY.entries());
        assertNotNull(MartialCommandEldritchMasteryTuning.EMPTY.entries());
        assertNotNull(LongPathFinalFormsMasteryTuning.EMPTY.entries());
        assertNotNull(AbyssalSpectralMasteryTuning.EMPTY.entries());
    }
}
