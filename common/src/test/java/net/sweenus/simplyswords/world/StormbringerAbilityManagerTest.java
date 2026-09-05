package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.item.component.ParryComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StormbringerAbilityManagerTest {

    @Test
    void shortCircuitHalvesTheCombinedGrantAndRoundsUp() {
        assertEquals(4, StormbringerAbilityManager.scaledChargeGain(7, .5));
        assertEquals(2, StormbringerAbilityManager.scaledChargeGain(3, .5));
        assertEquals(1, StormbringerAbilityManager.scaledChargeGain(2, .5));
        assertEquals(0, StormbringerAbilityManager.scaledChargeGain(0, .5));
    }

    @Test
    void forkAddsOneTargetWithoutReducingTheBaseChain() {
        assertEquals(11, StormbringerAbilityManager.chainTargetCount(10, 16, 1, false));
        assertEquals(10, StormbringerAbilityManager.chainTargetCount(10, 10, 1, false));
        assertEquals(1, StormbringerAbilityManager.chainTargetCount(15, 16, 1, true));
    }

    @Test
    void tempestCadenceUsesDistinctHitsAndHonoursItsMinimum() {
        assertEquals(16, StormbringerAbilityManager.resolvedChainCooldown(20, 1, 4, 8));
        assertEquals(12, StormbringerAbilityManager.resolvedChainCooldown(20, 2, 4, 8));
        assertEquals(8, StormbringerAbilityManager.resolvedChainCooldown(20, 3, 4, 8));
        assertEquals(8, StormbringerAbilityManager.resolvedChainCooldown(10, 1, 4, 8));
    }

    @Test
    void rollingThunderLosesEighteenPercentPerJump() {
        assertEquals(1, StormbringerAbilityManager.rollingMultiplier(0, .82), 1.0E-6);
        assertEquals(.82, StormbringerAbilityManager.rollingMultiplier(1, .82), 1.0E-6);
        assertEquals(.6724, StormbringerAbilityManager.rollingMultiplier(2, .82), 1.0E-6);
    }

    @Test
    void fullBatteryTakesPriorityWithoutConsumingTheFreeProc() {
        assertEquals(2, StormbringerAbilityManager.resolvedChargeCost(true, true, 12, 2));
        assertEquals(0, StormbringerAbilityManager.resolvedChargeCost(false, true, 10, 2));
        assertEquals(1, StormbringerAbilityManager.resolvedChargeCost(false, false, 10, 2));
    }

    @Test
    void killRefundAndSupercellScalingStayBounded() {
        assertEquals(2, StormbringerAbilityManager.boundedKillRefund(5, 2));
        assertEquals(0, StormbringerAbilityManager.boundedKillRefund(-1, 2));
        assertEquals(1.6, StormbringerAbilityManager.supercellMultiplier(15, .04), 1.0E-6);
    }

    @Test
    void supercellDecayWaitsUntilCombatGraceExpires() {
        assertEquals(false, StormbringerAbilityManager.shouldDecay(100, 40, 100));
        assertEquals(true, StormbringerAbilityManager.shouldDecay(141, 40, 100));
        assertEquals(true, StormbringerAbilityManager.shouldDecay(40, Long.MIN_VALUE, 100));
    }

    @Test
    void masteryChargeCapsSynchronizeAndClampStoredState() {
        ParryComponent charged = new ParryComponent(false, 10, 10);

        assertEquals(12, StormbringerAbilityManager.normalizedChargeState(charged, 12).stormChargeCapacity());
        assertEquals(15, StormbringerAbilityManager.normalizedChargeState(charged, 15).stormChargeCapacity());
        ParryComponent shortCircuit = StormbringerAbilityManager.normalizedChargeState(charged, 6);
        assertEquals(6, shortCircuit.stormChargeCapacity());
        assertEquals(6, shortCircuit.stormCharges());
    }
}
