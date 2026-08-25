package net.sweenus.simplyswords.api.ability;

import java.util.EnumMap;
import java.util.Map;

public final class Phase3AbilityTuning {
    public enum Setting {
        COOLDOWN_TICKS(0, 72000), DURATION_TICKS(0, 72000), CHANCE(0, 100), RANGE(0, 128),
        RADIUS(0, 64), WIDTH(0, 64), HEIGHT(0, 64), DAMAGE_MULTIPLIER(0, 10), SPEED(0, 16),
        DELAY_TICKS(0, 72000), TRAVEL_TICKS(1, 1200), TETHER_RANGE(0, 128),
        GROWTH_PER_HIT(0, 1), GROWTH_CAP(0, 4), PULL_STRENGTH(-16, 16), TARGET_CAP(0, 64),
        STATUS_DURATION_TICKS(0, 72000), STATUS_AMPLIFIER(0, 10), HEAL_RATIO(0, 10), HEAL_CAP(0, 64),
        STACK_CAP(0, 64), STACK_DURATION_TICKS(0, 72000), PER_STACK_BONUS(0, 10), BONUS_CAP(0, 10),
        THRESHOLD(0, 128), LOCKOUT_TICKS(0, 72000), REFUND_TICKS(0, 72000),
        INTERVAL_TICKS(1, 72000), COUNT(0, 64), MOVEMENT_SPEED(0, 16), CLIMB_SPEED(0, 16),
        FINAL_WIDTH(0, 64), IMPACT_RADIUS(0, 64), IMPACT_DAMAGE_MULTIPLIER(0, 10),
        IMPACT_KNOCKBACK(0, 16), IMPACT_LIFT(0, 16), STAIN_RADIUS(0, 64),
        STAIN_DURATION_TICKS(0, 72000), COOLDOWN_MULTIPLIER(0, 10), DAMAGE_REDUCTION(0, 1),
        ABSORPTION(0, 40), ROD_DURATION_TICKS(0, 72000), SHIELD_DURATION_TICKS(0, 72000),
        CORRIDOR_MATERIALIZE_TICKS(0, 1200), CORRIDOR_HOLD_TICKS(0, 1200),
        CORRIDOR_CLOSE_TICKS(0, 1200), BEAM_DURATION_TICKS(0, 72000),
        MARK_DURATION_TICKS(0, 72000), BUFF_DURATION_TICKS(0, 72000),
        STRIDE_DURATION_TICKS(0, 72000), FLICKER_DURATION_TICKS(0, 1200),
        ABSORPTION_DURATION_TICKS(0, 72000), WOUND_DURATION_TICKS(0, 72000),
        WOUND_DAMAGE_MULTIPLIER(0, 10), MELEE_BONUS_PER_STACK(0, 10), MELEE_BONUS_CAP(0, 10),
        SLOW_DURATION_TICKS(0, 72000), WEAKNESS_DURATION_TICKS(0, 72000),
        REPOSITION_RANGE(0, 128), CHAIN_RANGE(0, 64), CENTER_RADIUS(0, 64), ROOT_RADIUS(0, 64),
        REVERSE_STRENGTH(0, 16), EDGE_BONUS(0, 4), CHAIN_TARGET_CAP(0, 64), ROOT_TARGET_CAP(0, 64),
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

    public static final Phase3AbilityTuning EMPTY = new Phase3AbilityTuning(Map.of());
    private final EnumMap<Setting, Double> values;

    private Phase3AbilityTuning(Map<Setting, Double> values) {
        this.values = new EnumMap<>(Setting.class);
        this.values.putAll(values);
    }

    public double get(Setting setting, double fallback) {
        return values.getOrDefault(setting, setting.validate(fallback));
    }

    public int integer(Setting setting, int fallback) {
        return (int) Math.round(get(setting, fallback));
    }

    public boolean flag(Setting setting) {
        return get(setting, 0) >= 1;
    }

    public boolean has(Setting setting) {
        return values.containsKey(setting);
    }

    public Phase3AbilityTuning with(Setting setting, double value) {
        EnumMap<Setting, Double> updated = new EnumMap<>(values);
        updated.put(setting, setting.validate(value));
        return new Phase3AbilityTuning(updated);
    }

    public Phase3AbilityTuning add(Setting setting, double amount, double fallback) {
        return with(setting, get(setting, fallback) + amount);
    }

    public Phase3AbilityTuning multiply(Setting setting, double factor, double fallback) {
        return with(setting, get(setting, fallback) * factor);
    }
}
