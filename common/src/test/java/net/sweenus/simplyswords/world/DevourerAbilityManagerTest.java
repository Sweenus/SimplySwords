package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DevourerAbilityManagerTest {

    @Test
    void pulseIntervalUsesTheTunedBaseUntilTheAccelerationThreshold() {
        assertEquals(20, DevourerAbilityManager.pulseInterval(0, 400L, 16, 20));
        assertEquals(30, DevourerAbilityManager.pulseInterval(0, 400L, 16, 30));
        assertEquals(30, DevourerAbilityManager.pulseInterval(100, 40L, 16, 30));
        assertEquals(16, DevourerAbilityManager.pulseInterval(100, 100L, 16, 30));
        assertEquals(16, DevourerAbilityManager.pulseInterval(100, 400L, 16, 20));
    }

    @Test
    void killExtensionsStayWithinTheirPerCastCap() {
        long base = 1000L;
        long end = base;
        for (int kill = 0; kill < 8; kill++) {
            end = DevourerAbilityManager.extendedEnd(end, base, 20, 100);
        }

        assertEquals(base + 100L, end);
        assertEquals(base, DevourerAbilityManager.extendedEnd(base, base, 0, 100));
        assertEquals(base, DevourerAbilityManager.extendedEnd(base, base, 20, 0));
    }

    @Test
    void cooldownRefundSubtractsElapsedTimeAndTheAccumulatedRefund() {
        assertEquals(280, DevourerAbilityManager.refundedCooldown(1200, 900L, 20));
        assertEquals(140, DevourerAbilityManager.refundedCooldown(1200, 900L, 160));
        assertEquals(0, DevourerAbilityManager.refundedCooldown(1200, 1200L, 160));
    }
}
