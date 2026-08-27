package net.sweenus.simplyswords.world;

import net.minecraft.nbt.NbtCompound;
import net.sweenus.simplyswords.api.ability.Phase2AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase2AbilityTuning.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WraithfangTuningSnapshotTest {

    @Test
    void everyPersistentMasteryFamilySurvivesNbtRoundTrip() {
        Phase2AbilityTuning tuning = Phase2AbilityTuning.EMPTY
                .with(Setting.PROJECTILE_DAMAGE_MULTIPLIER, 2.475)
                .with(Setting.FLIGHT_DAMAGE_PER_TICK, .65)
                .with(Setting.FLIGHT_DAMAGE_CAP_TICKS, 40)
                .with(Setting.PIERCE_COUNT, 4)
                .with(Setting.PIERCE_DAMAGE_MULTIPLIER, .75)
                .with(Setting.BURST_LOCKOUT_TICKS, 20)
                .with(Setting.RETURN_DELAY_TICKS, 80)
                .with(Setting.DASH_CONTACT_TARGET_CAP, 10)
                .with(Setting.DASH_CONTACT_DAMAGE_MULTIPLIER, .5)
                .with(Setting.ARRIVAL_DAMAGE_MULTIPLIER, 1.6)
                .with(Setting.KILL_HASTE_DURATION_TICKS, 100)
                .with(Setting.ALTERNATION_BONUS_CAP, .2)
                .with(Setting.RETURN_MELEE_WINDOW_TICKS, 80)
                .with(Setting.MODE, 1 | 32 | 512 | 1024 | 8192);
        WraithfangTuningSnapshot expected = WraithfangTuningSnapshot.fromTuning(tuning, 67);
        NbtCompound nbt = new NbtCompound();

        expected.write(nbt);

        assertEquals(expected, WraithfangTuningSnapshot.read(nbt));
        assertTrue(expected.hasMode(1));
        assertTrue(expected.hasMode(8192));
    }

    @Test
    void snapshotUsesIndependentDamageAndDurationSettings() {
        WraithfangTuningSnapshot snapshot = WraithfangTuningSnapshot.fromTuning(
                Phase2AbilityTuning.EMPTY
                        .with(Setting.STATUS_DURATION_TICKS, 60)
                        .with(Setting.DASH_STATUS_DURATION_TICKS, 20)
                        .with(Setting.IMPACT_DAMAGE_MULTIPLIER, .35)
                        .with(Setting.ARRIVAL_DAMAGE_MULTIPLIER, .7)
                        .with(Setting.FLIGHT_DAMAGE_PER_TICK, .65)
                        .with(Setting.ALTERNATION_BONUS_PER_STACK, .05), 20);

        assertEquals(60, snapshot.weaknessDurationTicks());
        assertEquals(20, snapshot.dashStatusDurationTicks());
        assertEquals(.35, snapshot.burstDamageMultiplier(), 1.0E-6);
        assertEquals(.7, snapshot.arrivalDamageMultiplier(), 1.0E-6);
        assertEquals(.65, snapshot.flightDamagePerTick(), 1.0E-6);
        assertEquals(.05, snapshot.alternationBonusPerStack(), 1.0E-6);
    }
}
