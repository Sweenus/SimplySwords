package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Phase4StandardManagerTest {

    @Test
    void scorchingGroundCountsOnlyPulsesInsideItsWindow() {
        assertEquals(1, Phase4StandardManager.nextPulseCount(0, 0, 100, 40));
        assertEquals(2, Phase4StandardManager.nextPulseCount(1, 100, 110, 40));
        assertEquals(3, Phase4StandardManager.nextPulseCount(2, 110, 120, 40));
        assertEquals(1, Phase4StandardManager.nextPulseCount(2, 100, 141, 40));
        assertEquals(3, Phase4StandardManager.nextPulseCount(2, 100, 140, 40));
    }

    @Test
    void rallyingStandardFiresOnceAtItsTunedAllyCount() {
        assertTrue(Phase4StandardManager.rallyTriggers(true, 3, 3, false));
        assertTrue(Phase4StandardManager.rallyTriggers(true, 9, 3, false));
        assertFalse(Phase4StandardManager.rallyTriggers(true, 2, 3, false));
        assertFalse(Phase4StandardManager.rallyTriggers(true, 3, 3, true));
        assertFalse(Phase4StandardManager.rallyTriggers(false, 9, 3, false));
    }

    @Test
    void cooldownRefundsAreBoundedAndNeverNegative() {
        assertEquals(40, Phase4StandardManager.refundTicks(0, 40, 100));
        assertEquals(60, Phase4StandardManager.refundTicks(40, 80, 100));
        assertEquals(0, Phase4StandardManager.refundTicks(100, 40, 100));
        assertEquals(0, Phase4StandardManager.refundTicks(120, 40, 100));
        assertEquals(0, Phase4StandardManager.refundTicks(0, -5, 100));
    }

    @Test
    void solarPillarShortensTheStandardLifetime() {
        assertEquals(500, Phase4StandardManager.standardLifetime(1));
        assertEquals(325, Phase4StandardManager.standardLifetime(.65));
    }
}
