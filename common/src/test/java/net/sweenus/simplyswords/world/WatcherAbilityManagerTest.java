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

    @Test
    void mercyDamageIsCappedAfterScalingToPreserveTheVitalityFloor() {
        assertEquals(9.0F, WatcherAbilityManager.capDamageToVitalityFloor(20.0F, 10.0F, 1.0F), 1.0E-4F);
        assertEquals(0.0F, WatcherAbilityManager.capDamageToVitalityFloor(20.0F, 1.0F, 1.0F), 1.0E-4F);
        assertEquals(6.0F, WatcherAbilityManager.capDamageToVitalityFloor(6.0F, 10.0F, 1.0F), 1.0E-4F);
    }

    @Test
    void darkReserveCountsWholeOmenDamageInTenPercentSteps() {
        assertEquals(0.0F, WatcherAbilityManager.darkReserveAbsorption(3.9F, 40.0F, 4.0F), 1.0E-4F);
        assertEquals(3.0F, WatcherAbilityManager.darkReserveAbsorption(12.0F, 40.0F, 4.0F), 1.0E-4F);
        assertEquals(4.0F, WatcherAbilityManager.darkReserveAbsorption(40.0F, 40.0F, 4.0F), 1.0E-4F);
    }

    @Test
    void darkReserveRewardsBothLethalAndNonlethalOmens() {
        assertEquals(1.0F, WatcherAbilityManager.omenAbsorptionRequest(
                256, false, false, 2.0F, 2.0F, 20.0F, 1.0F, 4.0F), 1.0E-4F);
        assertEquals(4.0F, WatcherAbilityManager.omenAbsorptionRequest(
                256, false, false, 20.0F, 20.0F, 20.0F, 1.0F, 4.0F), 1.0E-4F);
    }

    @Test
    void darkReserveComposesWithExecutionAndMercyRewards() {
        assertEquals(8.6F, WatcherAbilityManager.omenAbsorptionRequest(
                256, true, false, 10.0F, 4.0F, 20.0F, 1.15F, 4.0F), 1.0E-4F);
        assertEquals(19.0F, WatcherAbilityManager.omenAbsorptionRequest(
                256 | 8192, false, true, 10.0F, 4.0F, 20.0F, 1.5F, 4.0F), 1.0E-4F);
        assertEquals(0.0F, WatcherAbilityManager.omenAbsorptionRequest(
                256 | 16384, true, false, 10.0F, 4.0F, 20.0F, 0.0F, 4.0F), 1.0E-4F);
    }

    @Test
    void omenAbsorptionAddsRewardsAndRespectsBothCaps() {
        assertEquals(10.0F, WatcherAbilityManager.cappedAbsorption(4.0F, 6.0F, 20.0F, 20.0F), 1.0E-4F);
        assertEquals(12.0F, WatcherAbilityManager.cappedAbsorption(10.0F, 8.0F, 12.0F, 20.0F), 1.0E-4F);
        assertEquals(9.0F, WatcherAbilityManager.cappedAbsorption(8.0F, 8.0F, 20.0F, 9.0F), 1.0E-4F);
        assertEquals(14.0F, WatcherAbilityManager.cappedAbsorption(14.0F, 8.0F, 12.0F, 20.0F), 1.0E-4F);
    }

    @Test
    void omenAbsorptionCannotExceedHalfTheOwnersMaximumHealth() {
        assertEquals(10.0F, WatcherAbilityManager.omenAbsorptionCap(20.0F, 20.0F, 20.0F), 1.0E-4F);
        assertEquals(20.0F, WatcherAbilityManager.omenAbsorptionCap(80.0F, 20.0F, 20.0F), 1.0E-4F);
        assertEquals(6.0F, WatcherAbilityManager.omenAbsorptionCap(40.0F, 6.0F, 20.0F), 1.0E-4F);
        assertEquals(0.0F, WatcherAbilityManager.omenAbsorptionCap(-20.0F, 20.0F, 20.0F), 1.0E-4F);
    }
}
