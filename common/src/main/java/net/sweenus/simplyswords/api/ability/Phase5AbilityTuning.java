package net.sweenus.simplyswords.api.ability;

import java.util.EnumMap;
import java.util.Map;

public final class Phase5AbilityTuning {
    public enum Setting {
        COOLDOWN_TICKS(0, 72000), DURATION_TICKS(0, 72000), COLLAPSE_DURATION_TICKS(0, 72000),
        INTERVAL_TICKS(1, 72000), LOCKOUT_TICKS(0, 72000), REFUND_TICKS(0, 72000),
        CHANCE(0, 100), DAMAGE_MULTIPLIER(0, 10), PERIODIC_DAMAGE_MULTIPLIER(0, 10),
        FINAL_DAMAGE_MULTIPLIER(0, 10), SECONDARY_DAMAGE_MULTIPLIER(0, 10),
        HEAL_MULTIPLIER(0, 10), DAMAGE_REDUCTION(0, 1), INCOMING_MULTIPLIER(0, 10),
        OUTGOING_MULTIPLIER(0, 10), PER_STACK_MULTIPLIER(0, 10), GENERATION_MULTIPLIER(0, 10),
        RADIUS(0, 64), RANGE(0, 128), RETARGET_RANGE(0, 128), MIN_LENGTH(0, 64),
        PULL_STRENGTH(0, 16), RADIUS_GROWTH(0, 16), RUPTURE_LENGTH(0, 128),
        RUPTURE_WIDTH(0, 64), RUPTURE_KNOCK_UP(0, 16), SPEED(0, 16),
        TARGET_CAP(0, 64), SEARCH_CAP(0, 64), COUNT(0, 64), STACK_CAP(0, 64),
        SPREAD_CAP(0, 64), GENERATION_CAP(0, 64), PULSE_COUNT(0, 64), WISP_COUNT(0, 64),
        HEAT_GAIN(0, 100), HEAT_MAX(1, 100), VENT_DRAIN(1, 100), MAX_PRESSURE(1, 10000),
        FIRE_TICKS(0, 72000), STATUS_DURATION_TICKS(0, 72000), STATUS_AMPLIFIER(0, 10),
        ABSORPTION(0, 40), MODE(0, Integer.MAX_VALUE);

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

    public static final Phase5AbilityTuning EMPTY = new Phase5AbilityTuning(Map.of());
    private final EnumMap<Setting, Double> values;

    private Phase5AbilityTuning(Map<Setting, Double> values) {
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

    public Phase5AbilityTuning with(Setting setting, double value) {
        EnumMap<Setting, Double> updated = new EnumMap<>(values);
        updated.put(setting, setting.validate(value));
        return new Phase5AbilityTuning(updated);
    }

    public Phase5AbilityTuning add(Setting setting, double amount, double fallback) {
        return with(setting, get(setting, fallback) + amount);
    }

    public Phase5AbilityTuning multiply(Setting setting, double factor, double fallback) {
        return with(setting, get(setting, fallback) * factor);
    }
}
