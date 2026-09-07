package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WraithfangAbilityManagerTest {

    @Test
    void alternationBuildsToFourAndRepeatingOrExpiryClearsIt() {
        assertEquals(1, WraithfangAbilityManager.nextAlternationStacks(0, true, true));
        assertEquals(4, WraithfangAbilityManager.nextAlternationStacks(3, true, true));
        assertEquals(4, WraithfangAbilityManager.nextAlternationStacks(4, true, true));
        assertEquals(0, WraithfangAbilityManager.nextAlternationStacks(3, true, false));
        assertEquals(0, WraithfangAbilityManager.nextAlternationStacks(3, false, true));
    }
}
