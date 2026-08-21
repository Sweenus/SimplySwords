package net.sweenus.simplyswords.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelperMethodsSpellDamageScalingTest {

    private static final float START = 1.40F;
    private static final float STRENGTH = 10.0F;
    private static final float ATTACK_DAMAGE = 100.0F;

    @Test
    void matchesDefaultCurveReferenceValues() {
        assertAdjustedDamage(100.0F, 100.0F);
        assertAdjustedDamage(130.0F, 130.0F);
        assertAdjustedDamage(140.0F, 140.0F);
        assertAdjustedDamage(150.0F, 146.93F);
        assertAdjustedDamage(195.0F, 158.72F);
        assertAdjustedDamage(300.0F, 168.33F);
        assertAdjustedDamage(1000.0F, 184.66F);
    }

    @Test
    void leavesDamageUnchangedWhenDisabled() {
        assertEquals(300.0F, adjust(300.0F, ATTACK_DAMAGE, START, 0.0F));
    }

    @Test
    void leavesDamageAtOrBelowTheKneeUnchanged() {
        assertEquals(80.0F, adjust(80.0F, ATTACK_DAMAGE, START, STRENGTH));
        assertEquals(130.0F, adjust(130.0F, ATTACK_DAMAGE, START, STRENGTH));
        assertEquals(140.0F, adjust(140.0F, ATTACK_DAMAGE, START, STRENGTH));
    }

    @Test
    void safelyHandlesNonPositiveInputs() {
        assertEquals(195.0F, adjust(195.0F, 0.0F, START, STRENGTH));
        assertEquals(195.0F, adjust(195.0F, -10.0F, START, STRENGTH));
        assertEquals(0.0F, adjust(0.0F, ATTACK_DAMAGE, START, STRENGTH));
        assertEquals(-10.0F, adjust(-10.0F, ATTACK_DAMAGE, START, STRENGTH));
    }

    @Test
    void remainsMonotonicAndNeverIncreasesRawDamage() {
        float[] rawDamages = {141.0F, 150.0F, 195.0F, 200.0F, 300.0F, 500.0F, 1000.0F, 10000.0F};
        float previousAdjusted = Float.NEGATIVE_INFINITY;

        for (float rawDamage : rawDamages) {
            float adjusted = adjust(rawDamage, ATTACK_DAMAGE, START, STRENGTH);
            assertTrue(adjusted >= previousAdjusted);
            assertTrue(adjusted <= rawDamage);
            previousAdjusted = adjusted;
        }
    }

    private static void assertAdjustedDamage(float spellDamage, float expected) {
        assertEquals(expected, adjust(spellDamage, ATTACK_DAMAGE, START, STRENGTH), 0.01F);
    }

    private static float adjust(float spellDamage, float attackDamage, float start, float strength) {
        return HelperMethods.applySpellDamageDiminishingReturns(spellDamage, attackDamage, start, strength);
    }
}
