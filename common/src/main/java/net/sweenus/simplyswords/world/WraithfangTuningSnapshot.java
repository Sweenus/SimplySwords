package net.sweenus.simplyswords.world;

import net.minecraft.nbt.NbtCompound;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryTuning;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;

public record WraithfangTuningSnapshot(
        double projectileDamageMultiplier, double flightDamagePerTick, int flightDamageCapTicks,
        int loyalty, int pierceCount, double pierceDamageMultiplier,
        int weaknessDurationTicks, int weaknessAmplifier,
        double burstDamageMultiplier, double burstRadius, int burstTargetCap, int burstLockoutTicks,
        int returnDelayTicks, int mode, int cooldownTicks, int projectileLifetimeTicks,
        double dashTargetRange, double dashSpeed, int dashDurationTicks,
        double dashContactDamageMultiplier, int dashContactTargetCap,
        double steeringDegrees, int steeringTicks, double arrivalRange, double arrivalDamageMultiplier,
        int dashStatusDurationTicks, int dashStatusAmplifier,
        int hasteDurationTicks, int hasteAmplifier, double meleeHasteBonus,
        int projectileGuardTicks, double projectileDamageReduction,
        int killHasteDurationTicks, int killHasteAmplifier, int killCooldownRefundTicks,
        int alternationWindowTicks, double alternationBonusPerStack, double alternationBonusCap,
        int hasteKillRefundTicks, int hasteKillRefundCapTicks,
        double hasteDamageMultiplier, int returnMeleeWindowTicks, double returnMeleeDamageMultiplier
) {
    private static final String NBT_KEY = "wraithfang_tuning";

    public static WraithfangTuningSnapshot from(UniqueAbilityExecution execution) {
        AbyssalSpectralMasteryTuning t = execution == null ? AbyssalSpectralMasteryTuning.EMPTY
                : AbyssalSpectralMasteryAbilities.tuning(execution);
        return fromTuning(t, execution == null ? 20 : execution.cooldownTicks(20));
    }

    static WraithfangTuningSnapshot fromTuning(AbyssalSpectralMasteryTuning t, int cooldownTicks) {
        return new WraithfangTuningSnapshot(
                t.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1),
                t.get(AbyssalSpectralMasteryTuning.Setting.FLIGHT_DAMAGE_PER_TICK, .5),
                t.integer(AbyssalSpectralMasteryTuning.Setting.FLIGHT_DAMAGE_CAP_TICKS, 1200),
                t.integer(AbyssalSpectralMasteryTuning.Setting.LOYALTY, 1),
                t.integer(AbyssalSpectralMasteryTuning.Setting.PIERCE_COUNT, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.PIERCE_DAMAGE_MULTIPLIER, 1),
                t.integer(AbyssalSpectralMasteryTuning.Setting.STATUS_DURATION_TICKS, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.STATUS_AMPLIFIER, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.IMPACT_DAMAGE_MULTIPLIER, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.IMPACT_RADIUS, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.IMPACT_TARGET_CAP, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.BURST_LOCKOUT_TICKS, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.RETURN_DELAY_TICKS, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0),
                cooldownTicks,
                t.integer(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_LIFETIME, 80),
                t.get(AbyssalSpectralMasteryTuning.Setting.DASH_TARGET_RANGE, 14.4),
                t.get(AbyssalSpectralMasteryTuning.Setting.DASH_SPEED, 1.35),
                t.integer(AbyssalSpectralMasteryTuning.Setting.DASH_DURATION_TICKS, 10),
                t.get(AbyssalSpectralMasteryTuning.Setting.DASH_CONTACT_DAMAGE_MULTIPLIER, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.DASH_CONTACT_TARGET_CAP, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.HOMING_TURN_DEGREES, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.DASH_STEERING_TICKS, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.ARRIVAL_RANGE, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.ARRIVAL_DAMAGE_MULTIPLIER, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.DASH_STATUS_DURATION_TICKS, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.DASH_STATUS_AMPLIFIER, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.HASTE_DURATION_TICKS, 80),
                t.integer(AbyssalSpectralMasteryTuning.Setting.HASTE_AMPLIFIER, 1),
                t.get(AbyssalSpectralMasteryTuning.Setting.MELEE_BONUS_PER_STACK, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_GUARD_TICKS, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_REDUCTION, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.KILL_HASTE_DURATION_TICKS, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.KILL_HASTE_AMPLIFIER, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.KILL_COOLDOWN_REFUND_TICKS, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.ALTERNATION_WINDOW_TICKS, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.ALTERNATION_BONUS_PER_STACK, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.ALTERNATION_BONUS_CAP, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_REFUND_TICKS, 0),
                t.integer(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_REFUND_CAP_TICKS, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1),
                t.integer(AbyssalSpectralMasteryTuning.Setting.RETURN_MELEE_WINDOW_TICKS, 0),
                t.get(AbyssalSpectralMasteryTuning.Setting.RETURN_MELEE_DAMAGE_MULTIPLIER, 0));
    }

    public boolean hasMode(int bit) {
        return (mode & bit) != 0;
    }

    public void write(NbtCompound parent) {
        NbtCompound n = new NbtCompound();
        n.putDouble("projectile_damage", projectileDamageMultiplier);
        n.putDouble("flight_damage", flightDamagePerTick);
        n.putInt("flight_cap", flightDamageCapTicks);
        n.putInt("loyalty", loyalty);
        n.putInt("pierce_count", pierceCount);
        n.putDouble("pierce_damage", pierceDamageMultiplier);
        n.putInt("weakness_duration", weaknessDurationTicks);
        n.putInt("weakness_amplifier", weaknessAmplifier);
        n.putDouble("burst_damage", burstDamageMultiplier);
        n.putDouble("burst_radius", burstRadius);
        n.putInt("burst_cap", burstTargetCap);
        n.putInt("burst_lockout", burstLockoutTicks);
        n.putInt("return_delay", returnDelayTicks);
        n.putInt("mode", mode);
        n.putInt("cooldown", cooldownTicks);
        n.putInt("projectile_lifetime", projectileLifetimeTicks);
        n.putDouble("dash_range", dashTargetRange);
        n.putDouble("dash_speed", dashSpeed);
        n.putInt("dash_duration", dashDurationTicks);
        n.putDouble("contact_damage", dashContactDamageMultiplier);
        n.putInt("contact_cap", dashContactTargetCap);
        n.putDouble("steering_degrees", steeringDegrees);
        n.putInt("steering_ticks", steeringTicks);
        n.putDouble("arrival_range", arrivalRange);
        n.putDouble("arrival_damage", arrivalDamageMultiplier);
        n.putInt("dash_status_duration", dashStatusDurationTicks);
        n.putInt("dash_status_amplifier", dashStatusAmplifier);
        n.putInt("haste_duration", hasteDurationTicks);
        n.putInt("haste_amplifier", hasteAmplifier);
        n.putDouble("melee_haste_bonus", meleeHasteBonus);
        n.putInt("guard_ticks", projectileGuardTicks);
        n.putDouble("guard_reduction", projectileDamageReduction);
        n.putInt("kill_haste_duration", killHasteDurationTicks);
        n.putInt("kill_haste_amplifier", killHasteAmplifier);
        n.putInt("kill_refund", killCooldownRefundTicks);
        n.putInt("alternation_window", alternationWindowTicks);
        n.putDouble("alternation_bonus", alternationBonusPerStack);
        n.putDouble("alternation_cap", alternationBonusCap);
        n.putInt("haste_kill_refund", hasteKillRefundTicks);
        n.putInt("haste_kill_cap", hasteKillRefundCapTicks);
        n.putDouble("haste_damage", hasteDamageMultiplier);
        n.putInt("return_melee_window", returnMeleeWindowTicks);
        n.putDouble("return_melee_damage", returnMeleeDamageMultiplier);
        parent.put(NBT_KEY, n);
    }

    public static WraithfangTuningSnapshot read(NbtCompound parent) {
        if (!parent.contains(NBT_KEY)) return from(null);
        NbtCompound n = parent.getCompound(NBT_KEY);
        return new WraithfangTuningSnapshot(
                n.getDouble("projectile_damage"), n.getDouble("flight_damage"), n.getInt("flight_cap"),
                n.getInt("loyalty"), n.getInt("pierce_count"), n.getDouble("pierce_damage"),
                n.getInt("weakness_duration"), n.getInt("weakness_amplifier"),
                n.getDouble("burst_damage"), n.getDouble("burst_radius"), n.getInt("burst_cap"),
                n.getInt("burst_lockout"), n.getInt("return_delay"), n.getInt("mode"),
                n.getInt("cooldown"), n.getInt("projectile_lifetime"), n.getDouble("dash_range"),
                n.getDouble("dash_speed"), n.getInt("dash_duration"), n.getDouble("contact_damage"),
                n.getInt("contact_cap"), n.getDouble("steering_degrees"), n.getInt("steering_ticks"),
                n.getDouble("arrival_range"), n.getDouble("arrival_damage"),
                n.getInt("dash_status_duration"), n.getInt("dash_status_amplifier"),
                n.getInt("haste_duration"), n.getInt("haste_amplifier"), n.getDouble("melee_haste_bonus"),
                n.getInt("guard_ticks"), n.getDouble("guard_reduction"), n.getInt("kill_haste_duration"),
                n.getInt("kill_haste_amplifier"), n.getInt("kill_refund"),
                n.getInt("alternation_window"), n.getDouble("alternation_bonus"),
                n.getDouble("alternation_cap"), n.getInt("haste_kill_refund"),
                n.getInt("haste_kill_cap"), n.getDouble("haste_damage"),
                n.getInt("return_melee_window"), n.getDouble("return_melee_damage"));
    }
}
