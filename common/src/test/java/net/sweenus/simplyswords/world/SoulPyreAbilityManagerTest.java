package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoulPyreAbilityManagerTest {

    @Test
    void closedCircleAcceleratesPulsesWithoutShorteningTheTether() {
        assertEquals(80, SoulPyreAbilityManager.closedPulseInterval(600, 6, .8));
        assertEquals(100, SoulPyreAbilityManager.closedPulseInterval(600, 6, 1));
        assertEquals(1, SoulPyreAbilityManager.closedPulseInterval(1, 100, .8));
    }

    @Test
    void fullVolleyAndLegionComposeToTwelveWisps() {
        assertEquals(6, SoulPyreAbilityManager.volleySize(6, false, 2));
        assertEquals(12, SoulPyreAbilityManager.volleySize(6, true, 2));
        assertEquals(10, SoulPyreAbilityManager.volleySize(5, true, 2));
    }

    @Test
    void requiemUsesAllHarvestedSoulsIndependentlyOfStoredSouls() {
        assertEquals(1.7F, SoulPyreAbilityManager.finalSoulMultiplier(
                2, .05, 5, .12, 10, true), 1.0E-6);
        assertEquals(2.2F, SoulPyreAbilityManager.finalSoulMultiplier(
                0, .05, 14, .12, 10, true), 1.0E-6);
        assertEquals(1.1F, SoulPyreAbilityManager.finalSoulMultiplier(
                2, .05, 14, .12, 10, false), 1.0E-6);
    }

    @Test
    void soulLanceCapsDamageAtTenSouls() {
        assertEquals(0, SoulPyreAbilityManager.lanceDamageMultiplier(0, 10, .25), 1.0E-6);
        assertEquals(1.5F, SoulPyreAbilityManager.lanceDamageMultiplier(6, 10, .25), 1.0E-6);
        assertEquals(2.5F, SoulPyreAbilityManager.lanceDamageMultiplier(16, 10, .25), 1.0E-6);
    }

    @Test
    void ashenMantleTriggersOnlyOnCumulativeHarvestMilestones() {
        assertFalse(SoulPyreAbilityManager.isHarvestMilestone(0, 5));
        assertFalse(SoulPyreAbilityManager.isHarvestMilestone(4, 5));
        assertTrue(SoulPyreAbilityManager.isHarvestMilestone(5, 5));
        assertTrue(SoulPyreAbilityManager.isHarvestMilestone(10, 5));
    }

    @Test
    void ashenMantleNeverReducesAbsorptionFromAnotherSource() {
        assertEquals(6, SoulPyreAbilityManager.boundedAbsorptionAfterGrant(4, 2, 8), 1.0E-6);
        assertEquals(8, SoulPyreAbilityManager.boundedAbsorptionAfterGrant(7, 2, 8), 1.0E-6);
        assertEquals(12, SoulPyreAbilityManager.boundedAbsorptionAfterGrant(12, 2, 8), 1.0E-6);
    }
}
