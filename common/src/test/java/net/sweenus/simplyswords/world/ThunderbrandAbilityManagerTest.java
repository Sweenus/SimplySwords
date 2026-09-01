package net.sweenus.simplyswords.world;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ThunderbrandAbilityManagerTest {
    @Test
    void sustainedAndRollingComposeAgainstConfiguredDashDuration() {
        assertEquals(36, ThunderbrandAbilityManager.configuredDashDuration(15, 3, 2, 0));
        assertEquals(56, ThunderbrandAbilityManager.configuredDashDuration(25, 3, 2, 0));
        assertEquals(8, ThunderbrandAbilityManager.configuredDashDuration(25, 3, 2, 8));
    }

    @Test
    void forkedBrandAddsOneTargetAndHonoursItsSixTargetCap() {
        assertEquals(5, ThunderbrandAbilityManager.forkedTargetCap(4, 1, 6));
        assertEquals(6, ThunderbrandAbilityManager.forkedTargetCap(6, 1, 6));
        assertEquals(9, ThunderbrandAbilityManager.forkedTargetCap(8, 1, 0));
    }

    @Test
    void chainSchedulingStaysBoundedInsideTheDash() {
        assertEquals(1, ThunderbrandAbilityManager.scheduledDashTick(0, 8, 18));
        assertEquals(16, ThunderbrandAbilityManager.scheduledDashTick(7, 8, 18));
        assertEquals(9, ThunderbrandAbilityManager.scheduledDashTick(0, 1, 18));
    }

    @Test
    void steeringUsesTheShortestTurnAndHonoursItsLimit() {
        Vec3d east = new Vec3d(1, 0, 0);
        Vec3d north = new Vec3d(0, 0, 1);
        Vec3d turned = ThunderbrandAbilityManager.turnToward(east, north, Math.toRadians(12));
        assertEquals(Math.cos(Math.toRadians(12)), turned.x, 1.0E-6);
        assertEquals(Math.sin(Math.toRadians(12)), turned.z, 1.0E-6);
        assertEquals(east, ThunderbrandAbilityManager.turnToward(east, north, 0));
    }

    @Test
    void railboltUsesDistanceToTheFiniteSegment() {
        Vec3d start = Vec3d.ZERO;
        Vec3d segment = new Vec3d(10, 0, 0);
        assertEquals(.25, ThunderbrandAbilityManager.distanceToSegmentSquared(
                new Vec3d(5, .5, 0), start, segment, segment.lengthSquared()), 1.0E-6);
        assertEquals(4, ThunderbrandAbilityManager.distanceToSegmentSquared(
                new Vec3d(12, 0, 0), start, segment, segment.lengthSquared()), 1.0E-6);
    }
}
