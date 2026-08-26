package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WatcherAbilityManagerTest {

    @Test
    void swoopCountRampsWithDreadAndStaysWithinItsMaximum() {
        assertEquals(4, WatcherAbilityManager.totalSwoops(0, 0, 4, 12, 1, 0, 0.0F));
        assertEquals(12, WatcherAbilityManager.totalSwoops(0, 0, 4, 12, 5, 0, 1.0F));
        assertEquals(17, WatcherAbilityManager.totalSwoops(0, 0, 4, 17, 6, 1, 1.0F));
        assertEquals(14, WatcherAbilityManager.totalSwoops(0, 0, 4, 17, 4, 1, 0.5F));
    }

    @Test
    void capstonesOverrideTheSwoopCount() {
        assertEquals(20, WatcherAbilityManager.totalSwoops(64, 20, 4, 12, 5, 0, 1.0F));
        assertEquals(0, WatcherAbilityManager.totalSwoops(128, 0, 4, 12, 5, 0, 1.0F));
        assertEquals(0, WatcherAbilityManager.totalSwoops(64 | 128, 20, 4, 12, 5, 0, 1.0F));
    }

    @Test
    void absoluteSentenceArmsAtTheReachableMaximumDread() {
        assertTrue(WatcherAbilityManager.absoluteReady(false, 1, 6, 5));
        assertTrue(WatcherAbilityManager.absoluteReady(true, 5, 6, 5));
        assertTrue(WatcherAbilityManager.absoluteReady(true, 6, 6, 8));
        assertFalse(WatcherAbilityManager.absoluteReady(true, 5, 6, 8));
        assertTrue(WatcherAbilityManager.absoluteReady(true, 3, 6, 3));
    }

    @Test
    void lowHealthTriggerFiresOnlyOnTheCrossingTick() {
        assertTrue(WatcherAbilityManager.crossedLowHealth(20.0F, 40.0F, 8.0F, 30));
        assertFalse(WatcherAbilityManager.crossedLowHealth(20.0F, 40.0F, 2.0F, 30));
        assertFalse(WatcherAbilityManager.crossedLowHealth(10.0F, 40.0F, 2.0F, 30));
        assertFalse(WatcherAbilityManager.crossedLowHealth(20.0F, 40.0F, 8.0F, 0));
    }

    @Test
    void claimedVitalityBonusScalesPerPointAndCaps() {
        assertEquals(0.15F, WatcherAbilityManager.claimBonus(3, .05, .4), 1.0E-4F);
        assertEquals(0.4F, WatcherAbilityManager.claimBonus(12, .05, .4), 1.0E-4F);
        assertEquals(0.0F, WatcherAbilityManager.claimBonus(0, .05, .4), 1.0E-4F);
    }
}
