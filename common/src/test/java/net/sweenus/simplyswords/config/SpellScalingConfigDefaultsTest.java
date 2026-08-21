package net.sweenus.simplyswords.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SpellScalingConfigDefaultsTest {

    @Test
    void globalDefaultsAreValid() {
        assertAll(
                () -> assertEquals(2.17F, CompatibilityConfig.DEFAULT_IRONS_BASE_POWER),
                () -> assertEquals(0.50F, CompatibilityConfig.DEFAULT_SPELL_POWER_API_SCALING_MULTIPLIER),
                () -> assertEquals(1.40F, CompatibilityConfig.DEFAULT_DIMINISHING_RETURNS_START),
                () -> assertEquals(10.0F, CompatibilityConfig.DEFAULT_DIMINISHING_RETURNS_STRENGTH),
                () -> assertFalse(CompatibilityConfig.defaultWeaponManaCosts().containsKey(
                        net.minecraft.util.Identifier.of("simplyswords", "dreadtide")))
        );
    }
}
