package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BattleStandardMasteryManagerTest {

    @Test
    void scorchingGroundCountsOnlyPulsesInsideItsWindow() {
        assertEquals(1, BattleStandardMasteryManager.nextPulseCount(0, 0, 100, 40));
        assertEquals(2, BattleStandardMasteryManager.nextPulseCount(1, 100, 110, 40));
        assertEquals(3, BattleStandardMasteryManager.nextPulseCount(2, 110, 120, 40));
        assertEquals(1, BattleStandardMasteryManager.nextPulseCount(2, 100, 141, 40));
        assertEquals(3, BattleStandardMasteryManager.nextPulseCount(2, 100, 140, 40));
    }

    @Test
    void rallyingStandardFiresOnceAtItsTunedAllyCount() {
        assertTrue(BattleStandardMasteryManager.rallyTriggers(true, 3, 3, false));
        assertTrue(BattleStandardMasteryManager.rallyTriggers(true, 9, 3, false));
        assertFalse(BattleStandardMasteryManager.rallyTriggers(true, 2, 3, false));
        assertFalse(BattleStandardMasteryManager.rallyTriggers(true, 3, 3, true));
        assertFalse(BattleStandardMasteryManager.rallyTriggers(false, 9, 3, false));
    }

    @Test
    void cooldownRefundsAreBoundedAndNeverNegative() {
        assertEquals(40, BattleStandardMasteryManager.refundTicks(0, 40, 100));
        assertEquals(60, BattleStandardMasteryManager.refundTicks(40, 80, 100));
        assertEquals(0, BattleStandardMasteryManager.refundTicks(100, 40, 100));
        assertEquals(0, BattleStandardMasteryManager.refundTicks(120, 40, 100));
        assertEquals(0, BattleStandardMasteryManager.refundTicks(0, -5, 100));
    }

    @Test
    void solarPillarShortensTheStandardLifetime() {
        assertEquals(500, BattleStandardMasteryManager.standardLifetime(1));
        assertEquals(325, BattleStandardMasteryManager.standardLifetime(.65));
    }
}
