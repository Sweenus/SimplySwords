package net.sweenus.simplyswords.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellScalingConfigDefaultsTest {

    @Test
    void globalDefaultsAreValid() {
        GeneralConfig settings = new GeneralConfig();

        assertAll(
                () -> assertEquals(2.17F, settings.ironsSpellBasePower),
                () -> assertEquals(1.40F, settings.spellScalingDiminishingReturnsStart),
                () -> assertEquals(10.0F, settings.spellScalingDiminishingReturnsStrength),
                () -> assertTrue(settings.ironsSpellBasePower > 0.0F),
                () -> assertTrue(settings.spellScalingDiminishingReturnsStart >= 1.0F),
                () -> assertTrue(settings.spellScalingDiminishingReturnsStrength >= 0.0F)
        );
    }
}
