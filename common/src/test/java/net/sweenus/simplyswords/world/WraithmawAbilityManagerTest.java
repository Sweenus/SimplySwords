package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WraithmawAbilityManagerTest {

    @Test
    void storedMaliceScalesWithOrbitingCutlassesAndRespectsItsCap() {
        assertEquals(1.0, WraithmawAbilityManager.storedMaliceMultiplier(.04, .24, 0), 1.0E-6);
        assertEquals(1.12, WraithmawAbilityManager.storedMaliceMultiplier(.04, .24, 3), 1.0E-6);
        assertEquals(1.24, WraithmawAbilityManager.storedMaliceMultiplier(.04, .24, 6), 1.0E-6);
        assertEquals(1.24, WraithmawAbilityManager.storedMaliceMultiplier(.04, .24, 11), 1.0E-6);
    }

    @Test
    void storedMaliceIsNeutralWithoutBothTunedValues() {
        assertEquals(1.0, WraithmawAbilityManager.storedMaliceMultiplier(.15, 0, 4), 1.0E-6);
        assertEquals(1.0, WraithmawAbilityManager.storedMaliceMultiplier(0, .24, 4), 1.0E-6);
    }

    @Test
    void everyOrbitingCutlassClaimsADistinctSlot() {
        boolean[] occupied = new boolean[12];

        for (int expected = 0; expected < occupied.length; expected++) {
            int slot = WraithmawAbilityManager.nextFreeSlot(occupied);
            assertEquals(expected, slot);
            occupied[slot] = true;
        }
        assertEquals(-1, WraithmawAbilityManager.nextFreeSlot(occupied));
    }

    @Test
    void loneExecutionerLockoutBlocksRecoveryUntilItExpires() {
        assertFalse(WraithmawAbilityManager.recoveryLocked(100, null));
        assertTrue(WraithmawAbilityManager.recoveryLocked(100, 180L));
        assertFalse(WraithmawAbilityManager.recoveryLocked(180, 180L));
        assertFalse(WraithmawAbilityManager.recoveryLocked(181, 180L));
    }

    @Test
    void mausoleumBurstOnlyDetonatesInsideItsWindow() {
        assertTrue(WraithmawAbilityManager.withinBurstWindow(1000, 1000, 200));
        assertTrue(WraithmawAbilityManager.withinBurstWindow(1200, 1000, 200));
        assertFalse(WraithmawAbilityManager.withinBurstWindow(1201, 1000, 200));
        assertFalse(WraithmawAbilityManager.withinBurstWindow(999, 1000, 200));
        assertFalse(WraithmawAbilityManager.withinBurstWindow(1000, 1000, 0));
    }
}
