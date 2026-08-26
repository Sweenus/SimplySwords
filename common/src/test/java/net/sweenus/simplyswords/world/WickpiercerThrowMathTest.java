package net.sweenus.simplyswords.world;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WickpiercerThrowMathTest {

    @Test
    void searingPointRoundsItsBurnUpToWholeSeconds() {
        assertEquals(0, WickpiercerThrowMath.scaledFireSeconds(0));
        assertEquals(1, WickpiercerThrowMath.scaledFireSeconds(1));
        assertEquals(3, WickpiercerThrowMath.scaledFireSeconds(60));
        assertEquals(5, WickpiercerThrowMath.scaledFireSeconds(100));
    }

    @Test
    void orbitStrikesAreSpacedByTheTunedIntervalAndEndWithTheTarget() {
        assertEquals(40, WickpiercerThrowMath.nextStrikeAge(20, 20));
        assertEquals(21, WickpiercerThrowMath.nextStrikeAge(20, 0));

        assertTrue(WickpiercerThrowMath.orbitContinues(20, 80, true));
        assertTrue(WickpiercerThrowMath.orbitContinues(80, 80, true));
        assertFalse(WickpiercerThrowMath.orbitContinues(81, 80, true));
        assertFalse(WickpiercerThrowMath.orbitContinues(20, 80, false));
    }

    @Test
    void orbitingTaperStrikesThreeTimesAcrossItsDuration() {
        int strikes = 0;
        int next = WickpiercerThrowMath.nextStrikeAge(0, 20);
        for (int age = 1; WickpiercerThrowMath.orbitContinues(age, 60, true); age++) {
            if (age >= next) {
                strikes++;
                next = WickpiercerThrowMath.nextStrikeAge(age, 20);
            }
        }

        assertEquals(3, strikes);
    }

    @Test
    void orbitingTaperCurveCirclesWideAndReturnsToTheTargetForEveryStrike() {
        double phase = 0.42;
        int interval = 20;
        double radius = WickpiercerThrowMath.DEFAULT_ORBIT_RADIUS;

        for (int strikeAge = 0; strikeAge <= 60; strikeAge += interval) {
            assertEquals(Vec3d.ZERO,
                    WickpiercerThrowMath.orbitOffset(strikeAge, interval, phase, radius));
        }

        Vec3d midpoint = WickpiercerThrowMath.orbitOffset(10, interval, phase, radius);
        assertEquals(radius, Math.hypot(midpoint.x, midpoint.z), 1.0E-9);
        assertTrue(midpoint.y >= 0.2 && midpoint.y <= 0.5);
    }

    @Test
    void orbitingTaperCurveIsFiniteAndContinuousAcrossAStrike() {
        double phase = -1.7;
        Vec3d justBefore = WickpiercerThrowMath.orbitOffset(
                19.99, 20, phase, WickpiercerThrowMath.DEFAULT_ORBIT_RADIUS);
        Vec3d strike = WickpiercerThrowMath.orbitOffset(
                20.0, 20, phase, WickpiercerThrowMath.DEFAULT_ORBIT_RADIUS);
        Vec3d justAfter = WickpiercerThrowMath.orbitOffset(
                20.01, 20, phase, WickpiercerThrowMath.DEFAULT_ORBIT_RADIUS);

        assertTrue(Double.isFinite(justBefore.x) && Double.isFinite(justBefore.y)
                && Double.isFinite(justBefore.z));
        assertTrue(Double.isFinite(justAfter.x) && Double.isFinite(justAfter.y)
                && Double.isFinite(justAfter.z));
        assertTrue(justBefore.distanceTo(strike) < 0.06);
        assertTrue(justAfter.distanceTo(strike) < 0.06);
    }
}
