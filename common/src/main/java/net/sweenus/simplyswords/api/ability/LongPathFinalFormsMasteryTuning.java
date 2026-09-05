package net.sweenus.simplyswords.api.ability;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LongPathFinalFormsMasteryTuning implements MasteryTuningSnapshot {
    public enum Setting {
        COOLDOWN_TICKS(0, 72000), INTERVAL_TICKS(1, 72000), DURATION_TICKS(0, 72000),
        CHANCE(0, 100), RADIUS(0, 64), RANGE(0, 128), TARGET_CAP(0, 64),
        DAMAGE_MULTIPLIER(0, 10), HEAL_MULTIPLIER(0, 10), HEAL_AMOUNT(0, 64),
        LANDING_DAMAGE_MULTIPLIER(0, 10), CYCLE_DAMAGE_MULTIPLIER(0, 10),
        LANDING_RADIUS(0, 64), LANDING_TARGET_CAP(0, 64),
        EARLY_DAMAGE_MULTIPLIER(0, 10), DEATH_BURST_DAMAGE_MULTIPLIER(0, 10),
        DEATH_BURST_RADIUS(0, 64), DEATH_BURST_TARGET_CAP(0, 64),
        NEAR_DAMAGE_MULTIPLIER(0, 10), LOW_HEALTH_DAMAGE_MULTIPLIER(0, 10),
        WEAKENED_DAMAGE_MULTIPLIER(0, 10), MELEE_DAMAGE_MULTIPLIER(0, 10),
        FLARE_DAMAGE_MULTIPLIER(0, 10), SUPPORT_DAMAGE_BONUS(0, 10), DAMAGE_REDUCTION(0, 1),
        ACQUISITION_RANGE(0, 128), RETARGET_RANGE(0, 128), RETARGET_CAP(0, 64),
        CHARGE_LOCKOUT_TICKS(0, 72000), TEMP_ABSORPTION_CAP(0, 40),
        RECALL_RADIUS(0, 64), RECALL_DAMAGE_MULTIPLIER(0, 10),
        ABSORPTION(0, 40), ABSORPTION_CAP(0, 40), STATUS_DURATION_TICKS(0, 72000),
        STATUS_AMPLIFIER(0, 10), SPEED(0, 16), PULL_STRENGTH(0, 16),
        THRESHOLD(0, 1), COUNT(0, 64), LOCKOUT_TICKS(0, 72000), REFUND_TICKS(0, 72000),
        PER_TARGET_BONUS(0, 10), BONUS_CAP(0, 10), FIRE_TICKS(0, 72000),
        SUPPORT_RADIUS(0, 64), SUPPORT_INTERVAL_TICKS(1, 72000), SUPPORT_TARGET_CAP(0, 64),
        LIFETIME_MULTIPLIER(0, 10), MOVEMENT_SPEED(0, 16),
        COOLDOWN_BASE_TICKS(0, 72000), COOLDOWN_MULTIPLIER(0, 10),
        AURA_INTERVAL_TICKS(1, 72000), CLOUD_MOVE_INTERVAL_TICKS(1, 72000),
        FAST_PULSE_INTERVAL_TICKS(1, 72000), FAST_PULSE_AFTER_TICKS(0, 72000),
        SLOW_DURATION_TICKS(0, 72000), REPEAT_WINDOW_TICKS(0, 72000),
        CHARGE_PER_HIT(0, 64), RESISTANCE_CHARGE_STEP(1, 64),
        RESISTANCE_STEP_TICKS(0, 72000), RESISTANCE_DURATION_CAP_TICKS(0, 72000),
        OVERHEAL_ABSORPTION_TICKS(0, 72000), INTEREST_CHARGE_STEP(1, 64),
        INTEREST_PER_STEP(0, 10), INTEREST_CAP(0, 10),
        BASTION_CHARGE_PER_ABSORPTION(1, 64), BASTION_ABSORPTION_TICKS(0, 72000),
        COOLDOWN_PER_SIPHON_TICKS(0, 72000), COOLDOWN_PENALTY_CAP_TICKS(0, 72000),
        INTERRUPT_RESISTANCE_TICKS(0, 72000), RETARGET_WINDOW_TICKS(0, 72000),
        RETARGET_DAMAGE_PENALTY(0, 1), RETARGET_DAMAGE_FLOOR(0, 1),
        MOVEMENT_MULTIPLIER(0, 16), RECALL_COOLDOWN_TICKS(0, 72000),
        WEAKNESS_PULSE_COUNT(1, 64), WEAKNESS_WINDOW_TICKS(0, 72000),
        WEAKNESS_DURATION_TICKS(0, 72000), CYCLE_PULSE_COUNT(1, 64),
        GLOWING_DURATION_TICKS(0, 72000), EARLY_WINDOW_TICKS(0, 72000),
        STRENGTH_DURATION_TICKS(0, 72000), CLEANSE_LOCKOUT_TICKS(0, 72000),
        ALLY_REGEN_TICKS(0, 72000), GUARDIAN_THRESHOLD(0, 1), GUARDIAN_ABSORPTION(0, 40),
        GUARDIAN_ABSORPTION_TICKS(0, 72000), GUARDIAN_LOCKOUT_TICKS(0, 72000),
        RALLY_ALLY_COUNT(1, 64), RALLY_REFUND_TICKS(0, 72000),
        SANCTUARY_RESISTANCE_TICKS(0, 72000), ALLY_CHARGE_TICKS(0, 72000),
        FIRE_RESISTANCE_TICKS(0, 72000), RESERVE_CAP(0, 40), RESERVE_THRESHOLD(0, 1),
        RESERVE_ABSORPTION_TICKS(0, 72000), REPRISAL_LOCKOUT_TICKS(0, 72000),
        REPRISAL_FIRE_TICKS(0, 72000), GUARD_RANGE(0, 128), KNOCKBACK_RESISTANCE(0, 1),
        REKINDLE_THRESHOLD(0, 1), REKINDLE_DURATION_TICKS(0, 72000),
        REKINDLE_ABSORPTION(0, 40), REKINDLE_LOCKOUT_TICKS(0, 72000),
        PHOENIX_DURATION_TICKS(0, 72000), PHOENIX_COOLDOWN_TICKS(0, 72000),
        COMBO_COUNT(1, 64), COMBO_WINDOW_TICKS(0, 72000), FLARE_LOCKOUT_TICKS(0, 72000),
        CYCLE_PULL_STRENGTH(0, 16), CONSTANT_PULL_STRENGTH(0, 16), CORE_RANGE(0, 128),
        PURSUIT_RANGE(0, 128), OWNER_AURA_RANGE(0, 128), OWNER_HASTE_AMPLIFIER(0, 10),
        OWNER_HASTE_TICKS(0, 72000), HASTE_DURATION_TICKS(0, 72000), ALLY_SPEED_TICKS(0, 72000),
        ALLY_WEAKNESS_TICKS(0, 72000), OMEN_UPGRADE_COUNT(1, 64), OMEN_UPGRADE_TICKS(0, 72000),
        OMEN_WINDOW_TICKS(0, 72000), DOOM_PULL_RANGE(0, 128), DOOM_PULL_LOCKOUT_TICKS(0, 72000),
        PROPHECY_REFUND_CAP_TICKS(0, 72000), LOW_HEALTH_THRESHOLD(0, 1),
        BOSS_DAMAGE_MULTIPLIER(0, 10), PLAGUE_DAMAGE_MULTIPLIER(0, 10),
        PLAGUE_WEAKNESS_TICKS(0, 72000), EXECUTION_DAMAGE_MULTIPLIER(0, 10),
        EXECUTION_COOLDOWN_TICKS(0, 72000),
        MODE(0, 1048575);

        private final double minimum;
        private final double maximum;

        Setting(double minimum, double maximum) {
            this.minimum = minimum;
            this.maximum = maximum;
        }

        double validate(double value) {
            return Math.clamp(Double.isFinite(value) ? value : minimum, minimum, maximum);
        }
    }

    public static final LongPathFinalFormsMasteryTuning EMPTY = new LongPathFinalFormsMasteryTuning(Map.of());
    private final EnumMap<Setting, Double> values;

    private LongPathFinalFormsMasteryTuning(Map<Setting, Double> values) {
        this.values = new EnumMap<>(Setting.class);
        this.values.putAll(values);
    }

    public double get(Setting setting, double fallback) {
        return values.getOrDefault(setting, setting.validate(fallback));
    }

    public int integer(Setting setting, int fallback) {
        return (int) Math.round(get(setting, fallback));
    }

    public boolean has(Setting setting) {
        return values.containsKey(setting);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean flag(int bit) {
        return (integer(Setting.MODE, 0) & bit) != 0;
    }

    public LongPathFinalFormsMasteryTuning with(Setting setting, double value) {
        EnumMap<Setting, Double> updated = new EnumMap<>(values);
        updated.put(setting, setting.validate(value));
        return new LongPathFinalFormsMasteryTuning(updated);
    }

    public LongPathFinalFormsMasteryTuning add(Setting setting, double amount, double fallback) {
        return with(setting, get(setting, fallback) + amount);
    }

    public LongPathFinalFormsMasteryTuning multiply(Setting setting, double factor, double fallback) {
        return with(setting, get(setting, fallback) * factor);
    }

    @Override
    public Map<String, Double> entries() {
        Map<String, Double> entries = new LinkedHashMap<>();
        values.forEach((setting, value) -> entries.put(setting.name(), value));
        return Collections.unmodifiableMap(entries);
    }
}
