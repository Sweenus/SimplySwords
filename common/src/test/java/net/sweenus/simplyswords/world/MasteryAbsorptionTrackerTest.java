package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MasteryAbsorptionTrackerTest {

    @Test
    void abilityCapacityFillsOnlyTheMissingMaximumAbsorption() {
        assertEquals(20.0, MasteryAbsorptionTracker.requiredCapacity(0.0, 20.0F), 1.0E-6);
        assertEquals(16.0, MasteryAbsorptionTracker.requiredCapacity(4.0, 20.0F), 1.0E-6);
        assertEquals(0.0, MasteryAbsorptionTracker.requiredCapacity(24.0, 20.0F), 1.0E-6);
        assertEquals(0.0, MasteryAbsorptionTracker.requiredCapacity(0.0, 0.0F), 1.0E-6);
    }
}
