package net.sweenus.simplyswords.world;

import net.minecraft.nbt.NbtCompound;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FrostfallAbilityManagerTest {
    @Test
    void directDamageModifiersStayScopedAndHighArcAddsFrozenBaseDamage() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("DAMAGE_MULTIPLIER"), 1.1)
                .with(s("FROSTFALL_DIRECT_DAMAGE_MULTIPLIER"), 1.12)
                .with(s("FROSTFALL_SKIRMISHER_DAMAGE_MULTIPLIER"), .7)
                .with(s("FROSTFALL_HIGH_ARC_FLIGHT_TICKS"), 10)
                .with(s("FROSTFALL_HIGH_ARC_DAMAGE_MULTIPLIER"), .2)
                .with(s("FROSTFALL_SLOWED_DAMAGE_MULTIPLIER"), 1.15)
                .with(s("FROSTFALL_DEADFALL_DAMAGE_MULTIPLIER"), 1.8);

        assertEquals(1.1 * 1.12 * .7,
                FrostfallAbilityManager.resolveDirectMultiplier(tuning, 9, false, false), 1.0E-6);
        assertEquals((1.1 * 1.12 * .7 + .2) * 1.15 * 1.8,
                FrostfallAbilityManager.resolveDirectMultiplier(tuning, 10, true, true), 1.0E-6);
    }

    @Test
    void pulseFormsUseDeterministicImpactRelativeSchedules() {
        StormFrostWaterMasteryTuning base = StormFrostWaterMasteryTuning.EMPTY;
        assertEquals(5, FrostfallAbilityManager.resolvePulseCount(base));
        assertEquals(100, FrostfallAbilityManager.resolveFieldDuration(base, 5));
        assertEquals(20, FrostfallAbilityManager.pulseDueTick(base, 1, 5, 100));
        assertEquals(100, FrostfallAbilityManager.pulseDueTick(base, 5, 5, 100));

        StormFrostWaterMasteryTuning avalanche = base.with(s("FROSTFALL_FIELD_PULSE_COUNT"), 8)
                .with(s("FROSTFALL_FIELD_DURATION_TICKS"), 100);
        assertEquals(13, FrostfallAbilityManager.pulseDueTick(avalanche, 1, 8, 100));
        assertEquals(100, FrostfallAbilityManager.pulseDueTick(avalanche, 8, 8, 100));

        StormFrostWaterMasteryTuning glacier = base.with(s("FROSTFALL_FIELD_PULSE_COUNT"), 1)
                .with(s("FROSTFALL_GLACIER_DELAY_TICKS"), 30);
        assertEquals(30, FrostfallAbilityManager.pulseDueTick(glacier, 1, 1, 100));
    }

    @Test
    void transformedPulseDamageNeverUsesTheLegacyNegativeFormula() {
        StormFrostWaterMasteryTuning avalanche = StormFrostWaterMasteryTuning.EMPTY
                .with(s("FROSTFALL_AVALANCHE_DAMAGE_MULTIPLIER"), .65);
        assertEquals(.65 / 8, FrostfallAbilityManager.resolvePulseMultiplier(
                StormFrostWaterMasteryTuning.EMPTY, avalanche, 1, 8, false, false), 1.0E-6);
        assertEquals(.65, FrostfallAbilityManager.resolvePulseMultiplier(
                StormFrostWaterMasteryTuning.EMPTY, avalanche, 8, 8, false, false), 1.0E-6);
    }

    @Test
    void radiusCompositionIsConfigRelativeForEveryPulseCount() {
        StormFrostWaterMasteryTuning wide = StormFrostWaterMasteryTuning.EMPTY
                .with(s("FROSTFALL_PULSE_RADIUS_BONUS"), 1);
        assertEquals(4, FrostfallAbilityManager.resolvePulseRadius(wide, 8, 1, 5), 1.0E-6);
        assertEquals(8, FrostfallAbilityManager.resolvePulseRadius(wide, 8, 5, 5), 1.0E-6);
        assertEquals(8, FrostfallAbilityManager.resolvePulseRadius(wide, 8, 1, 1), 1.0E-6);
    }

    @Test
    void tuningSnapshotsRoundTripDedicatedAndGenericAddonValues() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("DAMAGE_MULTIPLIER"), 1.4)
                .with(s("FROSTFALL_RETURN_SPEED_MULTIPLIER"), 1.4375)
                .with(s("FROSTFALL_FIELD_PULSE_COUNT"), 8);
        NbtCompound nbt = tuning.toNbt();
        StormFrostWaterMasteryTuning restored = StormFrostWaterMasteryTuning.fromNbt(nbt);

        assertTrue(restored.has(s("DAMAGE_MULTIPLIER")));
        assertTrue(restored.has(s("FROSTFALL_RETURN_SPEED_MULTIPLIER")));
        assertFalse(restored.has(s("FROSTFALL_GLACIER_DELAY_TICKS")));
        assertEquals(1.4, restored.get(s("DAMAGE_MULTIPLIER"), 1), 1.0E-6);
        assertEquals(1.4375, restored.get(s("FROSTFALL_RETURN_SPEED_MULTIPLIER"), 1), 1.0E-6);
        assertEquals(8, restored.integer(s("FROSTFALL_FIELD_PULSE_COUNT"), 0));
    }

    @Test
    void historicalThrowGenericValuesRemainSingleFallbacksForTheField() {
        StormFrostWaterMasteryTuning throwTuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("DAMAGE_MULTIPLIER"), 1.4)
                .with(s("DURATION_TICKS"), 140)
                .with(s("PULSE_COUNT"), 7)
                .with(s("RADIUS"), 10);
        StormFrostWaterMasteryTuning fieldTuning = StormFrostWaterMasteryTuning.EMPTY;

        assertEquals(7, FrostfallAbilityManager.resolvePulseCount(throwTuning, fieldTuning));
        assertEquals(140, FrostfallAbilityManager.resolveFieldDuration(throwTuning, fieldTuning, 7));
        assertEquals(9, FrostfallAbilityManager.resolvePulseRadius(
                throwTuning, fieldTuning, 8, 7, 7), 1.0E-6);
        assertEquals(1.4, FrostfallAbilityManager.resolvePulseMultiplier(
                throwTuning, fieldTuning, 7, 7, false, false), 1.0E-6);

        StormFrostWaterMasteryTuning duplicateField = fieldTuning.with(s("DAMAGE_MULTIPLIER"), 1.4);
        assertEquals(1.4, FrostfallAbilityManager.resolvePulseMultiplier(
                throwTuning, duplicateField, 7, 7, false, false), 1.0E-6);
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }
}
