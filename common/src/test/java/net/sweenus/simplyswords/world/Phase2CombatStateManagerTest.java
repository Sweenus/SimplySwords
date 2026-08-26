package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Phase2CombatStateManagerTest {

    @Test
    void warningFlickerOnlyFiresOnTheTickThatCrossesTheThreshold() {
        assertTrue(Phase2CombatStateManager.crossedLowHealth(20.0F, 40.0F, 8.0F, 30));
        assertFalse(Phase2CombatStateManager.crossedLowHealth(20.0F, 40.0F, 2.0F, 30));
        assertFalse(Phase2CombatStateManager.crossedLowHealth(10.0F, 40.0F, 2.0F, 30));
        assertFalse(Phase2CombatStateManager.crossedLowHealth(20.0F, 40.0F, 8.0F, 0));
        assertFalse(Phase2CombatStateManager.crossedLowHealth(20.0F, 0.0F, 8.0F, 30));
    }

    @Test
    void armorIgnoreCompensatesExactlyForTheArmorItSkips() {
        assertEquals(1.4F, Phase2CombatStateManager.armorIgnoreMultiplier(10.0F, 20.0F, 0.0F, 4.0F), 1.0E-4F);
        assertEquals(1.0F, Phase2CombatStateManager.armorIgnoreMultiplier(10.0F, 20.0F, 0.0F, 0.0F), 1.0E-4F);
        assertEquals(1.0F, Phase2CombatStateManager.armorIgnoreMultiplier(10.0F, 0.0F, 0.0F, 4.0F), 1.0E-4F);
    }

    @Test
    void armorIgnoreNeverReducesDamageAndSaturatesOnLightArmor() {
        for (float armor = 1.0F; armor <= 20.0F; armor += 1.0F) {
            float multiplier = Phase2CombatStateManager.armorIgnoreMultiplier(12.0F, armor, 2.0F, 4.0F);
            assertTrue(multiplier >= 1.0F, "armor " + armor);
            assertTrue(multiplier <= 3.0F, "armor " + armor);
        }
    }
}
