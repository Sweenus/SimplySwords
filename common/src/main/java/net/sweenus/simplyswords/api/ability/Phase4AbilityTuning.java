package net.sweenus.simplyswords.api.ability;

import java.util.EnumMap;
import java.util.Map;

public final class Phase4AbilityTuning {
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
        LIFETIME_MULTIPLIER(0, 10), MOVEMENT_SPEED(0, 16), MODE(0, 1048575);

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

    public static final Phase4AbilityTuning EMPTY = new Phase4AbilityTuning(Map.of());
    private final EnumMap<Setting, Double> values;

    private Phase4AbilityTuning(Map<Setting, Double> values) {
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

    public Phase4AbilityTuning with(Setting setting, double value) {
        EnumMap<Setting, Double> updated = new EnumMap<>(values);
        updated.put(setting, setting.validate(value));
        return new Phase4AbilityTuning(updated);
    }

    public Phase4AbilityTuning add(Setting setting, double amount, double fallback) {
        return with(setting, get(setting, fallback) + amount);
    }

    public Phase4AbilityTuning multiply(Setting setting, double factor, double fallback) {
        return with(setting, get(setting, fallback) * factor);
    }
}
