package net.sweenus.simplyswords.api.ability;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArcaneCosmicMasteryTuning implements MasteryTuningSnapshot {
    public enum Setting {
        COOLDOWN_TICKS(0, 72000), DURATION_TICKS(0, 72000), SECONDARY_DURATION_TICKS(0, 72000),
        INTERVAL_TICKS(1, 72000), SECONDARY_INTERVAL_TICKS(1, 72000), LOCKOUT_TICKS(0, 72000),
        REFUND_TICKS(0, 72000), WINDUP_TICKS(0, 72000), DELAY_TICKS(0, 72000),
        CHANCE(0, 100), PITY_CHANCE(0, 100), FLAT_DAMAGE(0, 1000), DAMAGE_MULTIPLIER(0, 10),
        SECONDARY_DAMAGE_MULTIPLIER(0, 10), FINAL_DAMAGE_MULTIPLIER(0, 10), SPELL_MULTIPLIER(0, 10),
        INCOMING_MULTIPLIER(0, 10), OUTGOING_MULTIPLIER(0, 10), HEAL_MULTIPLIER(0, 10),
        PER_STACK_MULTIPLIER(0, 10), RADIUS(0, 64), SECONDARY_RADIUS(0, 64), RANGE(0, 128),
        WIDTH(0, 64), HEIGHT(0, 64), ANGLE(0, 360), SPEED(0, 16), PULL_STRENGTH(0, 16),
        KNOCKBACK(0, 16), TARGET_CAP(0, 64), SEARCH_CAP(0, 64), COUNT(0, 64), STACK_CAP(0, 64),
        STATUS_DURATION_TICKS(0, 72000), STATUS_AMPLIFIER(0, 10), ABSORPTION(0, 40),
        HEALTH_THRESHOLD(0, 1), ARMOR_IGNORE(0, 1), REPAIR_AMOUNT(0, 1000),
        SECONDARY_STATUS_DURATION_TICKS(0, 72000), TERTIARY_DURATION_TICKS(0, 72000),
        TERTIARY_INTERVAL_TICKS(1, 72000), TERTIARY_RADIUS(0, 64), TERTIARY_TARGET_CAP(0, 64),
        TERTIARY_DAMAGE_MULTIPLIER(0, 10), TERTIARY_STATUS_DURATION_TICKS(0, 72000),
        SECONDARY_COUNT(0, 64), SECONDARY_TARGET_CAP(0, 64),
        SECONDARY_REFUND_TICKS(0, 72000), TERTIARY_REFUND_TICKS(0, 72000),
        ARCANETHYST_LEVITATION_MELEE_DAMAGE_MULTIPLIER(0, 10),
        CAELESTIS_EAGER_RIFT_TICKS(0, 72000), CAELESTIS_RIM_DAMAGE_TARGET_CAP(0, 64),
        MODE(0, Integer.MAX_VALUE);

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

    public static final ArcaneCosmicMasteryTuning EMPTY = new ArcaneCosmicMasteryTuning(Map.of());
    private final EnumMap<Setting, Double> values;

    private ArcaneCosmicMasteryTuning(Map<Setting, Double> values) {
        this.values = new EnumMap<>(Setting.class);
        this.values.putAll(Map.copyOf(values));
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

    public boolean flag(int bit) {
        return (integer(Setting.MODE, 0) & bit) != 0;
    }

    public ArcaneCosmicMasteryTuning with(Setting setting, double value) {
        EnumMap<Setting, Double> updated = new EnumMap<>(values);
        updated.put(setting, setting.validate(value));
        return new ArcaneCosmicMasteryTuning(updated);
    }

    public ArcaneCosmicMasteryTuning add(Setting setting, double amount, double fallback) {
        return with(setting, get(setting, fallback) + amount);
    }

    public ArcaneCosmicMasteryTuning multiply(Setting setting, double factor, double fallback) {
        return with(setting, get(setting, fallback) * factor);
    }

    @Override
    public Map<String, Double> entries() {
        Map<String, Double> entries = new LinkedHashMap<>();
        values.forEach((setting, value) -> entries.put(setting.name(), value));
        return Collections.unmodifiableMap(entries);
    }
}
