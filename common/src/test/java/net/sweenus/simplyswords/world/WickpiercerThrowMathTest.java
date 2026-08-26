package net.sweenus.simplyswords.world;

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
}
