package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WeaponAbilityCooldownManagerTest {

    @Test
    void reducingACooldownShortensTheRemainingTimeAndNeverLengthensIt() {
        assertEquals(1080, WeaponAbilityCooldownManager.reducedEndTick(1100, 100, 20));
        assertEquals(700, WeaponAbilityCooldownManager.reducedEndTick(1100, 700, 900));
        assertEquals(1100, WeaponAbilityCooldownManager.reducedEndTick(1100, 700, 0));
    }

    @Test
    void anExpiredCooldownIsLeftAlone() {
        assertEquals(500, WeaponAbilityCooldownManager.reducedEndTick(500, 500, 20));
        assertEquals(500, WeaponAbilityCooldownManager.reducedEndTick(500, 900, 20));
    }
}
