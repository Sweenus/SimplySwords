package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class Phase4LichbladeManagerTest {

    @Test
    void choirAndRestlessPhylacteryComposeInsteadOfDiscardingEachOther() {
        assertEquals(700, Phase4LichbladeManager.cooldownTicks(700, 1, 0, 0, 10, 200));
        assertEquals(840, Phase4LichbladeManager.cooldownTicks(700, 1.2, 0, 0, 10, 200));
        assertEquals(600, Phase4LichbladeManager.cooldownTicks(600, 1, 0, 0, 10, 200));
        assertEquals(720, Phase4LichbladeManager.cooldownTicks(600, 1.2, 0, 0, 10, 200));
    }

    @Test
    void soulRecallAndRavenousPhylacteryAddOnTopOfTheTunedCooldown() {
        assertEquals(700, Phase4LichbladeManager.cooldownTicks(600, 1, 100, 0, 10, 200));
        assertEquals(650, Phase4LichbladeManager.cooldownTicks(600, 1, 0, 5, 10, 200));
        assertEquals(800, Phase4LichbladeManager.cooldownTicks(600, 1, 0, 40, 10, 200));
        assertEquals(800, Phase4LichbladeManager.cooldownTicks(600, 1, 0, 999, 10, 200));
    }

    @Test
    void onlyWanderingPhylacteryPaysTheRetargetDamagePenalty() {
        assertEquals(1.0, Phase4LichbladeManager.retargetPenalty(0, true, .15, .4), 1.0E-6);
        assertEquals(1.0, Phase4LichbladeManager.retargetPenalty(1, false, .15, .4), 1.0E-6);
        assertEquals(.85, Phase4LichbladeManager.retargetPenalty(1, true, .15, .4), 1.0E-6);
        assertEquals(.55, Phase4LichbladeManager.retargetPenalty(3, true, .15, .4), 1.0E-6);
        assertEquals(.4, Phase4LichbladeManager.retargetPenalty(4, true, .15, .4), 1.0E-6);
        assertEquals(.4, Phase4LichbladeManager.retargetPenalty(9, true, .15, .4), 1.0E-6);
    }

    @Test
    void unceasingCryOnlyQuickensAfterItsWindowAndLeavesTheCloudCadenceAlone() {
        assertEquals(5, Phase4LichbladeManager.pulseInterval(false, 200, 60, 4));
        assertEquals(5, Phase4LichbladeManager.pulseInterval(true, 59, 60, 4));
        assertEquals(4, Phase4LichbladeManager.pulseInterval(true, 60, 60, 4));
        assertEquals(4, Phase4LichbladeManager.pulseInterval(true, 200, 60, 4));
    }

    @Test
    void auraIntervalSnapsToTheResolveStep() {
        assertEquals(35, Phase4LichbladeManager.auraInterval(35));
        assertEquals(30, Phase4LichbladeManager.auraInterval(30));
        assertEquals(5, Phase4LichbladeManager.auraInterval(1));
    }

    @Test
    void overflowingSpiritOnlyPaysOnChargeBeyondACappedAbsorptionGrant() {
        assertEquals(0, Phase4LichbladeManager.overflowResistanceTicks(10, 6, 40, 4, 40, 120));
        assertEquals(0, Phase4LichbladeManager.overflowResistanceTicks(10, 10, 20, 4, 40, 120));
        assertEquals(40, Phase4LichbladeManager.overflowResistanceTicks(10, 10, 24, 4, 40, 120));
        assertEquals(120, Phase4LichbladeManager.overflowResistanceTicks(10, 10, 40, 4, 40, 120));
        assertEquals(120, Phase4LichbladeManager.overflowResistanceTicks(10, 10, 200, 4, 40, 120));
    }

    @Test
    void bloodlessFeedingConvertsOnlyOverhealAndNeverExceedsItsOwnCap() {
        assertEquals(0f, Phase4LichbladeManager.overhealGrant(1f, 4f, .5f, 4f, 0f), 1.0E-6);
        assertEquals(.5f, Phase4LichbladeManager.overhealGrant(1f, 0f, .5f, 4f, 0f), 1.0E-6);
        assertEquals(.25f, Phase4LichbladeManager.overhealGrant(1f, .5f, .5f, 4f, 0f), 1.0E-6);
        assertEquals(0f, Phase4LichbladeManager.overhealGrant(1f, 0f, .5f, 4f, 4f), 1.0E-6);
        assertEquals(.5f, Phase4LichbladeManager.overhealGrant(2f, 0f, .5f, 4f, 3.5f), 1.0E-6);
    }

    @Test
    void graveInterestStepsEveryTenChargeAndStopsAtItsCap() {
        assertEquals(1.0, Phase4LichbladeManager.interestMultiplier(9, 10, .1, .4), 1.0E-6);
        assertEquals(1.1, Phase4LichbladeManager.interestMultiplier(10, 10, .1, .4), 1.0E-6);
        assertEquals(1.3, Phase4LichbladeManager.interestMultiplier(39, 10, .1, .4), 1.0E-6);
        assertEquals(1.4, Phase4LichbladeManager.interestMultiplier(40, 10, .1, .4), 1.0E-6);
        assertEquals(1.4, Phase4LichbladeManager.interestMultiplier(400, 10, .1, .4), 1.0E-6);
    }

    @Test
    void graveSelectionOnlyChainsInsideItsWindowAndWanderingChainsThroughout() {
        assertEquals(0, Phase4LichbladeManager.retargetMaximum(false, false, 10, 80, 4, 1));
        assertEquals(1, Phase4LichbladeManager.retargetMaximum(false, true, 79, 80, 4, 1));
        assertEquals(0, Phase4LichbladeManager.retargetMaximum(false, true, 80, 80, 4, 1));
        assertEquals(4, Phase4LichbladeManager.retargetMaximum(true, false, 5000, 80, 4, 1));
    }

    @Test
    void returningShadeOnlyQuickensAReturnCausedByLosingEveryTarget() {
        assertEquals(1.0, Phase4LichbladeManager.returnSpeed(false, false, true, 1.5), 1.0E-6);
        assertEquals(1.0, Phase4LichbladeManager.returnSpeed(true, false, true, 1.5), 1.0E-6);
        assertEquals(1.0, Phase4LichbladeManager.returnSpeed(true, true, false, 1.5), 1.0E-6);
        assertEquals(1.5, Phase4LichbladeManager.returnSpeed(true, true, true, 1.5), 1.0E-6);
    }
}
