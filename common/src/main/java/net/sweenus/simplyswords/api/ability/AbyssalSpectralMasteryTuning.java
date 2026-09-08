package net.sweenus.simplyswords.api.ability;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AbyssalSpectralMasteryTuning implements MasteryTuningSnapshot {
    public enum Setting {
        COOLDOWN_TICKS(0, 72000), DURATION_TICKS(0, 72000), DAMAGE_MULTIPLIER(0, 10),
        PROJECTILE_DAMAGE_MULTIPLIER(0, 10), PROJECTILE_SPEED(0, 16), PROJECTILE_LIFETIME(0, 1200),
        LOYALTY(0, 10), PIERCE_COUNT(0, 16), IMPACT_RADIUS(0, 64), IMPACT_DAMAGE_MULTIPLIER(0, 10),
        IMPACT_TARGET_CAP(0, 64), FIRE_TICKS(0, 72000), STATUS_DURATION_TICKS(0, 72000),
        STATUS_AMPLIFIER(0, 10), STACK_CAP(0, 32), STACK_DURATION_TICKS(0, 72000),
        MARKED_TARGET_CAP(0, 32), MELEE_BONUS_PER_STACK(0, 2), MELEE_BONUS_CAP(0, 10),
        SWOOP_COUNT_BASE(0, 64), SWOOPS_PER_STACK(0, 16), SWOOP_DAMAGE_MULTIPLIER(0, 10),
        FINAL_DAMAGE_MULTIPLIER(0, 10), FINAL_PER_STACK_MULTIPLIER(0, 10),
        MISSING_HEALTH_BONUS_CAP(0, 10), EXECUTE_THRESHOLD(0, 1), ABSORPTION_MULTIPLIER(0, 10),
        MODE(0, 1048575), RADIUS(0, 64), SCAN_RADIUS(0, 64), TARGET_CAP(0, 64), CANDIDATE_CAP(0, 64),
        PULL_STRENGTH(0, 16), PULSE_INTERVAL_TICKS(1, 72000), LOOSE_TARGET_CAP(0, 256),
        TENDRIL_CAP(0, 32), STAIN_RADIUS(0, 64), STAIN_DURATION_TICKS(0, 72000),
        STAIN_AMPLIFIER(0, 10), STAIN_TARGET_CAP(0, 64), EXTRA_DURATION_CAP(0, 72000),
        HELD_THRESHOLD_TICKS(0, 72000), ACCELERATE_THRESHOLD_TICKS(0, 72000),
        RUPTURE_THRESHOLD_TICKS(0, 72000),
        REPRISAL_RADIUS(0, 64), REPRISAL_TARGET_CAP(0, 64), REPRISAL_PULL(0, 16),
        REPRISAL_DAMAGE_MULTIPLIER(0, 10), DAMAGE_REDUCTION(0, 1), LOCKOUT_TICKS(0, 72000),
        COOLDOWN_REFUND_TICKS(0, 72000),
        PASSIVE_COOLDOWN_TICKS(0, 72000), FIRE_DELAY_TICKS(0, 1200), CONE_DEGREES(0, 180),
        RANGE(0, 128), CLONE_COUNT(0, 32), SPEAR_COUNT(0, 64), CHANNEL_DURATION_TICKS(0, 72000),
        EXPLOSION_RADIUS(0, 64), TRIGGER_RADIUS(0, 64), EMBEDDED_DURATION_TICKS(0, 72000),
        FALL_SPEED(0, 16), MATERIALIZE_TICKS(0, 1200), ORBIT_CAP(0, 32),
        ORBIT_DURATION_TICKS(0, 72000), RECOVERY_RADIUS(0, 64), LAUNCH_SPEED(0, 16),
        LAUNCH_RANGE(0, 128), HOMING_RANGE(0, 128), HOMING_TURN_DEGREES(0, 45),
        DASH_SPEED(0, 16), DASH_DURATION_TICKS(0, 1200), HASTE_DURATION_TICKS(0, 72000),
        HASTE_AMPLIFIER(0, 10), REVIVE_HEALTH_MULTIPLIER(0, 1), REVIVE_COOLDOWN_MULTIPLIER(0, 10),
        REVIVE_ABSORPTION(0, 40), SECONDARY_DAMAGE_MULTIPLIER(0, 10), SECONDARY_TARGET_CAP(0, 64),
        INTERVAL_TICKS(1, 72000), THRESHOLD(0, 64), BONUS_PER_TRIGGER(0, 10), BONUS_CAP(0, 10),
        DURATION_BONUS_TICKS(0, 72000), COOLDOWN_REFUND_CAP_TICKS(0, 72000),
        STAIN_EXTENSION_TICKS(0, 72000),
        MOVEMENT_RETENTION(0, 1), MOVEMENT_SPEED(0, 16),
        REPEAT_WINDOW_TICKS(0, 72000), REPEAT_LOCKOUT_TICKS(0, 72000),
        INCOMING_DREAD_THRESHOLD(0, 32), LOW_HEALTH_PERCENT(0, 100),
        EXECUTE_DREAD_THRESHOLD(0, 32), CLAIM_DURATION_TICKS(0, 72000),
        CLAIM_BONUS_PER_POINT(0, 10), CLAIM_BONUS_CAP(0, 10),
        ROUTED_DAMAGE_BONUS(0, 10), REPRISAL_SECONDARY_MULTIPLIER(0, 10),
        REPRISAL_PUSH_STRENGTH(0, 16), ABSORPTION_DURATION_TICKS(0, 72000),
        FOLLOW_RANGE(0, 128), FOLLOW_MIN_RANGE(0, 128), FOLLOW_SWITCH_LOCKOUT_TICKS(0, 72000),
        ACCELERATED_INTERVAL_TICKS(1, 72000),
        REPRISAL_GUARD_TICKS(0, 72000),
        PROJECTILE_FIRE_TICKS(0, 72000), REVIVE_FIRE_TICKS(0, 72000),
        PIERCE_DAMAGE_MULTIPLIER(0, 10), RETURN_DELAY_TICKS(0, 72000),
        RETURN_TRAIL_RADIUS(0, 64), RETURN_TRAIL_DAMAGE_MULTIPLIER(0, 10),
        MELEE_IMPACT_RADIUS(0, 64), MELEE_IMPACT_DAMAGE_MULTIPLIER(0, 10),
        MELEE_IMPACT_TARGET_CAP(0, 64),
        PYRE_RADIUS(0, 64), PYRE_DAMAGE_MULTIPLIER(0, 10), PYRE_TARGET_CAP(0, 64),
        TWIN_LOCKOUT_TICKS(0, 72000), TWIN_STACK_GRANT(0, 32),
        GUARD_LOCKOUT_TICKS(0, 72000), GUARD_STATUS_TICKS(0, 72000),
        ALLY_STATUS_TICKS(0, 72000), REVIVE_WINDOW_TICKS(0, 72000),
        BURN_DAMAGE_BONUS(0, 10), ARMOR_IGNORE(0, 30),
        REVIVE_COOLDOWN_FLOOR_TICKS(0, 72000),
        CHAIN_RANGE(0, 128), CHAIN_DELAY_TICKS(0, 1200), GLOAM_MOVE_RANGE(0, 128),
        FLIGHT_DAMAGE_PER_TICK(0, 10), FLIGHT_DAMAGE_CAP_TICKS(0, 1200),
        BURST_LOCKOUT_TICKS(0, 72000), DASH_TARGET_RANGE(0, 128),
        DASH_CONTACT_DAMAGE_MULTIPLIER(0, 10), DASH_CONTACT_TARGET_CAP(0, 64),
        DASH_STEERING_TICKS(0, 1200), ARRIVAL_RANGE(0, 64), ARRIVAL_DAMAGE_MULTIPLIER(0, 10),
        DASH_STATUS_DURATION_TICKS(0, 72000), DASH_STATUS_AMPLIFIER(0, 10),
        PROJECTILE_GUARD_TICKS(0, 72000), PROJECTILE_DAMAGE_REDUCTION(0, 1),
        KILL_HASTE_DURATION_TICKS(0, 72000), KILL_HASTE_AMPLIFIER(0, 10),
        KILL_COOLDOWN_REFUND_TICKS(0, 72000), ALTERNATION_WINDOW_TICKS(0, 72000),
        ALTERNATION_BONUS_PER_STACK(0, 10), ALTERNATION_BONUS_CAP(0, 10),
        RETURN_MELEE_WINDOW_TICKS(0, 72000), RETURN_MELEE_DAMAGE_MULTIPLIER(0, 10),
        LAUNCH_COUNT(0, 16), GLOAM_VULNERABILITY_BONUS(0, 10),
        HAUNT_RANGE(0, 128), HAUNT_INTERVAL_TICKS(1, 72000),
        HAUNT_DAMAGE_MULTIPLIER(0, 10), HAUNT_TARGET_CAP(0, 64),
        BURIAL_RANGE(0, 128),
        RECALL_RANGE(0, 128), RECALL_TARGET_CAP(0, 64), RECALL_PULL_STRENGTH(0, 16),
        GRAVEWALK_RANGE(0, 128), GRAVEWALK_SPEED(0, 16), GRAVEWALK_DAMAGE_MULTIPLIER(0, 10),
        BURST_RANGE(0, 128), BURST_TARGET_CAP(0, 64),
        BURST_DAMAGE_MULTIPLIER(0, 10), BURST_WINDOW_TICKS(0, 72000),
        GLOAM_GROWTH_DURATION_TICKS(0, 72000);

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

    public static final AbyssalSpectralMasteryTuning EMPTY = new AbyssalSpectralMasteryTuning(Map.of());
    private final EnumMap<Setting, Double> values;

    private AbyssalSpectralMasteryTuning(Map<Setting, Double> values) {
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

    public AbyssalSpectralMasteryTuning with(Setting setting, double value) {
        EnumMap<Setting, Double> updated = new EnumMap<>(values);
        updated.put(setting, setting.validate(value));
        return new AbyssalSpectralMasteryTuning(updated);
    }

    public AbyssalSpectralMasteryTuning add(Setting setting, double amount, double fallback) {
        return with(setting, get(setting, fallback) + amount);
    }

    public AbyssalSpectralMasteryTuning multiply(Setting setting, double factor, double fallback) {
        return with(setting, get(setting, fallback) * factor);
    }

    @Override
    public Map<String, Double> entries() {
        Map<String, Double> entries = new LinkedHashMap<>();
        values.forEach((setting, value) -> entries.put(setting.name(), value));
        return Collections.unmodifiableMap(entries);
    }
}
