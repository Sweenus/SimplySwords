package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WickpiercerMasteryStateManagerTest {

    @Test
    void warningFlickerOnlyFiresOnTheTickThatCrossesTheThreshold() {
        assertTrue(WickpiercerMasteryStateManager.crossedLowHealth(20.0F, 40.0F, 8.0F, 30));
        assertFalse(WickpiercerMasteryStateManager.crossedLowHealth(20.0F, 40.0F, 2.0F, 30));
        assertFalse(WickpiercerMasteryStateManager.crossedLowHealth(10.0F, 40.0F, 2.0F, 30));
        assertFalse(WickpiercerMasteryStateManager.crossedLowHealth(20.0F, 40.0F, 8.0F, 0));
        assertFalse(WickpiercerMasteryStateManager.crossedLowHealth(20.0F, 0.0F, 8.0F, 30));
    }

    @Test
    void armorIgnoreCompensatesExactlyForTheArmorItSkips() {
        assertEquals(1.4F, WickpiercerMasteryStateManager.armorIgnoreMultiplier(10.0F, 20.0F, 0.0F, 4.0F), 1.0E-4F);
        assertEquals(1.0F, WickpiercerMasteryStateManager.armorIgnoreMultiplier(10.0F, 20.0F, 0.0F, 0.0F), 1.0E-4F);
        assertEquals(1.0F, WickpiercerMasteryStateManager.armorIgnoreMultiplier(10.0F, 0.0F, 0.0F, 4.0F), 1.0E-4F);
    }

    @Test
    void armorIgnoreNeverReducesDamageAndSaturatesOnLightArmor() {
        for (float armor = 1.0F; armor <= 20.0F; armor += 1.0F) {
            float multiplier = WickpiercerMasteryStateManager.armorIgnoreMultiplier(12.0F, armor, 2.0F, 4.0F);
            assertTrue(multiplier >= 1.0F, "armor " + armor);
            assertTrue(multiplier <= 3.0F, "armor " + armor);
        }
    }
}
