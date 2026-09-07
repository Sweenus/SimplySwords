package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LichbladeMasteryManagerTest {

    @Test
    void choirAndRestlessPhylacteryComposeInsteadOfDiscardingEachOther() {
        assertEquals(700, LichbladeMasteryManager.cooldownTicks(700, 1, 0, 0, 10, 200));
        assertEquals(840, LichbladeMasteryManager.cooldownTicks(700, 1.2, 0, 0, 10, 200));
        assertEquals(600, LichbladeMasteryManager.cooldownTicks(600, 1, 0, 0, 10, 200));
        assertEquals(720, LichbladeMasteryManager.cooldownTicks(600, 1.2, 0, 0, 10, 200));
    }

    @Test
    void soulRecallAndRavenousPhylacteryAddOnTopOfTheTunedCooldown() {
        assertEquals(700, LichbladeMasteryManager.cooldownTicks(600, 1, 100, 0, 10, 200));
        assertEquals(650, LichbladeMasteryManager.cooldownTicks(600, 1, 0, 5, 10, 200));
        assertEquals(800, LichbladeMasteryManager.cooldownTicks(600, 1, 0, 40, 10, 200));
        assertEquals(800, LichbladeMasteryManager.cooldownTicks(600, 1, 0, 999, 10, 200));
    }

    @Test
    void onlyWanderingPhylacteryPaysTheRetargetDamagePenalty() {
        assertEquals(1.0, LichbladeMasteryManager.retargetPenalty(0, true, .15, .4), 1.0E-6);
        assertEquals(1.0, LichbladeMasteryManager.retargetPenalty(1, false, .15, .4), 1.0E-6);
        assertEquals(.85, LichbladeMasteryManager.retargetPenalty(1, true, .15, .4), 1.0E-6);
        assertEquals(.55, LichbladeMasteryManager.retargetPenalty(3, true, .15, .4), 1.0E-6);
        assertEquals(.4, LichbladeMasteryManager.retargetPenalty(4, true, .15, .4), 1.0E-6);
        assertEquals(.4, LichbladeMasteryManager.retargetPenalty(9, true, .15, .4), 1.0E-6);
    }

    @Test
    void unceasingCryOnlyQuickensAfterItsWindowAndLeavesTheCloudCadenceAlone() {
        assertEquals(5, LichbladeMasteryManager.pulseInterval(false, 200, 60, 4));
        assertEquals(5, LichbladeMasteryManager.pulseInterval(true, 59, 60, 4));
        assertEquals(4, LichbladeMasteryManager.pulseInterval(true, 60, 60, 4));
        assertEquals(4, LichbladeMasteryManager.pulseInterval(true, 200, 60, 4));
    }

    @Test
    void auraIntervalSnapsToTheResolveStep() {
        assertEquals(35, LichbladeMasteryManager.auraInterval(35));
        assertEquals(30, LichbladeMasteryManager.auraInterval(30));
        assertEquals(5, LichbladeMasteryManager.auraInterval(1));
    }

    @Test
    void overflowingSpiritOnlyPaysOnChargeBeyondACappedAbsorptionGrant() {
        assertEquals(0, LichbladeMasteryManager.overflowResistanceTicks(10, 6, 40, 4, 40, 120, 2, 1));
        assertEquals(0, LichbladeMasteryManager.overflowResistanceTicks(10, 10, 20, 4, 40, 120, 2, 1));
        assertEquals(40, LichbladeMasteryManager.overflowResistanceTicks(10, 10, 24, 4, 40, 120, 2, 1));
        assertEquals(120, LichbladeMasteryManager.overflowResistanceTicks(10, 10, 40, 4, 40, 120, 2, 1));
        assertEquals(120, LichbladeMasteryManager.overflowResistanceTicks(10, 10, 200, 4, 40, 120, 2, 1));
    }

    @Test
    void overflowingSpiritCountsFromTheRealCapPointUnderGraveInterestAndSoulBastion() {
        assertEquals(80, LichbladeMasteryManager.overflowResistanceTicks(10, 10, 24, 4, 40, 120, 2, 1.4));
        assertEquals(80, LichbladeMasteryManager.overflowResistanceTicks(16, 16, 40, 4, 40, 120, 2, 1));
    }

    @Test
    void bloodlessFeedingConvertsOnlyOverhealAndNeverExceedsItsOwnCap() {
        assertEquals(0f, LichbladeMasteryManager.overhealGrant(1f, 4f, .5f, 4f, 0f), 1.0E-6);
        assertEquals(.5f, LichbladeMasteryManager.overhealGrant(1f, 0f, .5f, 4f, 0f), 1.0E-6);
        assertEquals(.25f, LichbladeMasteryManager.overhealGrant(1f, .5f, .5f, 4f, 0f), 1.0E-6);
        assertEquals(0f, LichbladeMasteryManager.overhealGrant(1f, 0f, .5f, 4f, 4f), 1.0E-6);
        assertEquals(.5f, LichbladeMasteryManager.overhealGrant(2f, 0f, .5f, 4f, 3.5f), 1.0E-6);
    }

    @Test
    void graveInterestStepsEveryTenChargeAndStopsAtItsCap() {
        assertEquals(1.0, LichbladeMasteryManager.interestMultiplier(9, 10, .1, .4), 1.0E-6);
        assertEquals(1.1, LichbladeMasteryManager.interestMultiplier(10, 10, .1, .4), 1.0E-6);
        assertEquals(1.3, LichbladeMasteryManager.interestMultiplier(39, 10, .1, .4), 1.0E-6);
        assertEquals(1.4, LichbladeMasteryManager.interestMultiplier(40, 10, .1, .4), 1.0E-6);
        assertEquals(1.4, LichbladeMasteryManager.interestMultiplier(400, 10, .1, .4), 1.0E-6);
    }

    @Test
    void graveSelectionOnlyChainsInsideItsWindowAndWanderingChainsThroughout() {
        assertEquals(0, LichbladeMasteryManager.retargetMaximum(false, false, 10, 80, 4, 1));
        assertEquals(1, LichbladeMasteryManager.retargetMaximum(false, true, 79, 80, 4, 1));
        assertEquals(0, LichbladeMasteryManager.retargetMaximum(false, true, 80, 80, 4, 1));
        assertEquals(4, LichbladeMasteryManager.retargetMaximum(true, false, 5000, 80, 4, 1));
    }

    @Test
    void returningShadeOnlyQuickensAReturnCausedByLosingEveryTarget() {
        assertEquals(1.0, LichbladeMasteryManager.returnSpeed(false, false, true, 1.5), 1.0E-6);
        assertEquals(1.0, LichbladeMasteryManager.returnSpeed(true, false, true, 1.5), 1.0E-6);
        assertEquals(1.0, LichbladeMasteryManager.returnSpeed(true, true, false, 1.5), 1.0E-6);
        assertEquals(1.5, LichbladeMasteryManager.returnSpeed(true, true, true, 1.5), 1.0E-6);
    }
}
