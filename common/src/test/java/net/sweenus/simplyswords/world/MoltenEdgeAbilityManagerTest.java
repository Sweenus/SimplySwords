package net.sweenus.simplyswords.world;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MoltenEdgeAbilityManagerTest {

    @Test
    void heatSinkPaysOnlyWhenAnActualThresholdIsCrossed() {
        assertFalse(MoltenEdgeAbilityManager.crossedHeatThreshold(0, 19, 20));
        assertTrue(MoltenEdgeAbilityManager.crossedHeatThreshold(19, 20, 20));
        assertFalse(MoltenEdgeAbilityManager.crossedHeatThreshold(20, 39, 20));
        assertTrue(MoltenEdgeAbilityManager.crossedHeatThreshold(39, 46, 20));
        assertFalse(MoltenEdgeAbilityManager.crossedHeatThreshold(60, 60, 20));
    }

    @Test
    void stableFurnaceDecaysOneHeatEveryTwoTicks() {
        assertEquals(50, MoltenEdgeAbilityManager.decayedUnwieldedHeat(50, 1, 2));
        assertEquals(49, MoltenEdgeAbilityManager.decayedUnwieldedHeat(50, 2, 2));
        assertEquals(0, MoltenEdgeAbilityManager.decayedUnwieldedHeat(0, 2, 2));
    }

    @Test
    void temperedHeartTreatsSeventyFiveAsAFullShockwave() {
        assertEquals(.5F, MoltenEdgeAbilityManager.shockwaveHeatFraction(50, 100), 1.0E-6);
        assertEquals(1, MoltenEdgeAbilityManager.shockwaveHeatFraction(75, 75), 1.0E-6);
        assertEquals(1, MoltenEdgeAbilityManager.shockwaveHeatFraction(100, 75), 1.0E-6);
    }

    @Test
    void furnaceRhythmRefundNeverCrossesItsThreeTickFloor() {
        assertEquals(109, MoltenEdgeAbilityManager.refundedDeadline(100, 110, 1, 3));
        assertEquals(103, MoltenEdgeAbilityManager.refundedDeadline(100, 103, 1, 3));
        assertEquals(100, MoltenEdgeAbilityManager.refundedDeadline(100, 100, 1, 3));
    }

    @Test
    void seismicSequenceStartsOnTheSecondHitAndCapsAtThirtyPercent() {
        assertEquals(1, MoltenEdgeAbilityManager.sequenceDamageMultiplier(0, .1), 1.0E-6);
        assertEquals(1.1F, MoltenEdgeAbilityManager.sequenceDamageMultiplier(1, .1), 1.0E-6);
        assertEquals(1.3F, MoltenEdgeAbilityManager.sequenceDamageMultiplier(3, .1), 1.0E-6);
    }

    @Test
    void pursuingFaultNeverTurnsMoreThanTwentyDegrees() {
        Vec3d turned = MoltenEdgeAbilityManager.turnToward(new Vec3d(1, 0, 0),
                new Vec3d(0, 0, 1), 20);
        assertEquals(20, Math.toDegrees(Math.atan2(turned.z, turned.x)), 1.0E-6);
        assertEquals(new Vec3d(0, 0, 1), MoltenEdgeAbilityManager.turnToward(
                new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 90));
    }

    @Test
    void fissureSuppressesForksAndShatterfieldUsesFiveSymmetricLanes() {
        assertFalse(MoltenEdgeAbilityManager.shouldFork(3, 3, true));
        assertFalse(MoltenEdgeAbilityManager.shouldFork(2, 3, false));
        assertTrue(MoltenEdgeAbilityManager.shouldFork(3, 3, false));
        assertEquals(-36, MoltenEdgeAbilityManager.laneOffset(0, 5, 18), 1.0E-6);
        assertEquals(0, MoltenEdgeAbilityManager.laneOffset(2, 5, 18), 1.0E-6);
        assertEquals(36, MoltenEdgeAbilityManager.laneOffset(4, 5, 18), 1.0E-6);
    }
}
