package net.sweenus.simplyswords.world;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EmberlashAbilityManagerTest {

    @Test
    void configuredStackCapIsAnExactStackCount() {
        assertEquals(1, EmberlashAbilityManager.nextStackCount(0, 1, 5));
        assertEquals(5, EmberlashAbilityManager.nextStackCount(4, 2, 5));
        assertEquals(6, EmberlashAbilityManager.nextStackCount(5, 2, 6));
        assertEquals(3, EmberlashAbilityManager.nextStackCount(3, 1, 3));
    }

    @Test
    void spitefireScalesFromOnlyLiveReprisalCharges() {
        assertEquals(5, EmberlashAbilityManager.liveReprisalCharges(5, 200, 199));
        assertEquals(0, EmberlashAbilityManager.liveReprisalCharges(5, 200, 200));
        assertEquals(10F, EmberlashAbilityManager.reprisalIncoming(10F, 0, 1.04), 1.0E-4F);
        assertEquals(11.6986F, EmberlashAbilityManager.reprisalIncoming(10F, 4, 1.04), 1.0E-4F);
        assertEquals(12.1665F, EmberlashAbilityManager.reprisalIncoming(10F, 5, 1.04), 1.0E-4F);
    }

    @Test
    void detonationAndAshDamageComposeWithLiveCoal() {
        assertEquals(19.8F, EmberlashAbilityManager.perStackDamage(10F, 6, 1.1, .3), 1.0E-4F);
        assertEquals(5.28F, EmberlashAbilityManager.perStackDamage(10F, 6, 1.1, .08), 1.0E-4F);
    }

    @Test
    void endlessSmoulderEchoUsesAnInclusiveThreeSecondCadence() {
        assertTrue(EmberlashAbilityManager.withinCadence(100, 160, 60));
        assertFalse(EmberlashAbilityManager.withinCadence(100, 161, 60));
        assertFalse(EmberlashAbilityManager.withinCadence(101, 100, 60));
    }

    @Test
    void phoenixPathUsesTheBoundedSegmentRatherThanAnInfiniteLine() {
        Vec3d start = new Vec3d(0, 64, 0);
        Vec3d end = new Vec3d(0, 64, -3);

        assertEquals(0, EmberlashAbilityManager.segmentDistanceSquared(
                new Vec3d(0, 64, -1.5), start, end), 1.0E-8);
        assertEquals(1, EmberlashAbilityManager.segmentDistanceSquared(
                new Vec3d(1, 64, -1.5), start, end), 1.0E-8);
        assertEquals(4, EmberlashAbilityManager.segmentDistanceSquared(
                new Vec3d(0, 64, -5), start, end), 1.0E-8);
    }
}
