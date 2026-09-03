package net.sweenus.simplyswords.world;

import net.minecraft.nbt.NbtCompound;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryTuning;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;

public record WraithmawTuningSnapshot(
        int materializeTicks, int fallIntervalTicks, double fallSpeed,
        double impactRadius, double impactDamageMultiplier, int impactTargetCap,
        int embeddedDurationTicks, double stainRadius, int stainDurationTicks,
        int stainAmplifier, int slowDurationTicks, int mode,
        int orbitCap, double recoveryRadius, int orbitDurationTicks, int recoveryLockoutTicks,
        double launchSpeed, double launchRange, int projectileLifetimeTicks,
        double homingRange, double homingTurnDegrees, double projectileDamageMultiplier,
        int launchCount, double storedBonusPerCutlass, double storedBonusCap,
        double gloamVulnerabilityBonus,
        double hauntRange, int hauntIntervalTicks, double hauntDamageMultiplier, int hauntTargetCap,
        double burialRange, int stainExtensionTicks, int extraDurationCapTicks,
        double recallRange, int recallTargetCap, double recallPullStrength,
        double gravewalkRange, double gravewalkSpeed, double gravewalkDamageMultiplier,
        double burstRange, int burstTargetCap, double burstDamageMultiplier, int burstWindowTicks
) {
    public static final int MODE_NO_EMBED = 1;
    public static final int MODE_SINGLE_TARGET = 2;
    public static final int MODE_TWIN_LAUNCH = 4;
    public static final int MODE_LONE_EXECUTIONER = 8;
    public static final int MODE_HAUNTED_STEEL = 16;
    public static final int MODE_SHARED_BURIAL = 32;
    public static final int MODE_GRAVE_RECALL = 64;
    public static final int MODE_LIVING_GRAVEYARD = 128;
    public static final int MODE_MAUSOLEUM_BURST = 256;

    private static final String NBT_KEY = "wraithmaw_tuning";

    public static WraithmawTuningSnapshot from(UniqueAbilityExecution execution) {
        return fromTuning(execution == null ? AbyssalSpectralMasteryTuning.EMPTY
                : AbyssalSpectralMasteryAbilities.tuning(execution));
    }

    static WraithmawTuningSnapshot fromTuning(AbyssalSpectralMasteryTuning t) {
        var config = Config.uniqueEffects.wraithmaw;
        return new WraithmawTuningSnapshot(
                Math.max(0, t.integer(AbyssalSpectralMasteryTuning.Setting.MATERIALIZE_TICKS, 12)),
                Math.max(1, t.integer(AbyssalSpectralMasteryTuning.Setting.INTERVAL_TICKS, 2)),
                Math.max(0.05, t.get(AbyssalSpectralMasteryTuning.Setting.FALL_SPEED, 1.25)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.IMPACT_RADIUS, 0)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.IMPACT_DAMAGE_MULTIPLIER, 0)),
                Math.clamp(t.integer(AbyssalSpectralMasteryTuning.Setting.IMPACT_TARGET_CAP, 0), 0, 64),
                Math.max(20, t.integer(AbyssalSpectralMasteryTuning.Setting.EMBEDDED_DURATION_TICKS,
                        config.embeddedDuration)),
                Math.max(0.25, t.get(AbyssalSpectralMasteryTuning.Setting.STAIN_RADIUS, config.stainRadius)),
                Math.max(20, t.integer(AbyssalSpectralMasteryTuning.Setting.STAIN_DURATION_TICKS,
                        config.stainDuration)),
                Math.clamp(t.integer(AbyssalSpectralMasteryTuning.Setting.STAIN_AMPLIFIER,
                        config.stainSlowAmplifier), 0, 4),
                Math.max(1, t.integer(AbyssalSpectralMasteryTuning.Setting.STATUS_DURATION_TICKS, 11)),
                t.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0),
                Math.clamp(t.integer(AbyssalSpectralMasteryTuning.Setting.ORBIT_CAP, config.maxRecovered), 1, 12),
                Math.max(0.1, t.get(AbyssalSpectralMasteryTuning.Setting.RECOVERY_RADIUS, 1.5)),
                Math.max(20, t.integer(AbyssalSpectralMasteryTuning.Setting.ORBIT_DURATION_TICKS,
                        config.recoveredDuration)),
                Math.max(0, t.integer(AbyssalSpectralMasteryTuning.Setting.LOCKOUT_TICKS, 0)),
                Math.max(0.1, t.get(AbyssalSpectralMasteryTuning.Setting.LAUNCH_SPEED, config.launchSpeed)),
                Math.max(1, t.get(AbyssalSpectralMasteryTuning.Setting.LAUNCH_RANGE, config.launchRange)),
                Math.max(1, t.integer(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_LIFETIME, 80)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.HOMING_RANGE, config.homingRange)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.HOMING_TURN_DEGREES, config.homingTurnRate)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1)),
                Math.clamp(t.integer(AbyssalSpectralMasteryTuning.Setting.LAUNCH_COUNT, 1), 1, 16),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.BONUS_PER_TRIGGER, 0)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.BONUS_CAP, 0)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.GLOAM_VULNERABILITY_BONUS, 0)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.HAUNT_RANGE, 0)),
                Math.max(1, t.integer(AbyssalSpectralMasteryTuning.Setting.HAUNT_INTERVAL_TICKS, 40)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.HAUNT_DAMAGE_MULTIPLIER, 0)),
                Math.clamp(t.integer(AbyssalSpectralMasteryTuning.Setting.HAUNT_TARGET_CAP, 0), 0, 64),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.BURIAL_RANGE, 0)),
                Math.max(0, t.integer(AbyssalSpectralMasteryTuning.Setting.STAIN_EXTENSION_TICKS, 0)),
                Math.max(0, t.integer(AbyssalSpectralMasteryTuning.Setting.EXTRA_DURATION_CAP, 0)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.RECALL_RANGE, 0)),
                Math.clamp(t.integer(AbyssalSpectralMasteryTuning.Setting.RECALL_TARGET_CAP, 0), 0, 64),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.RECALL_PULL_STRENGTH, 0)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.GRAVEWALK_RANGE, 0)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.GRAVEWALK_SPEED, 0)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.GRAVEWALK_DAMAGE_MULTIPLIER, 0)),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.BURST_RANGE, 0)),
                Math.clamp(t.integer(AbyssalSpectralMasteryTuning.Setting.BURST_TARGET_CAP, 0), 0, 64),
                Math.max(0, t.get(AbyssalSpectralMasteryTuning.Setting.BURST_DAMAGE_MULTIPLIER, 0)),
                Math.max(0, t.integer(AbyssalSpectralMasteryTuning.Setting.BURST_WINDOW_TICKS, 0)));
    }

    public boolean hasMode(int bit) {
        return (mode & bit) != 0;
    }

    public boolean appliesSlowness() {
        return !hasMode(MODE_MAUSOLEUM_BURST);
    }

    public boolean hauntsEnemies() {
        return hasMode(MODE_HAUNTED_STEEL) && hauntRange > 0
                && hauntDamageMultiplier > 0 && hauntTargetCap > 0;
    }

    public boolean walksToEnemies() {
        return hasMode(MODE_LIVING_GRAVEYARD) && gravewalkRange > 0 && gravewalkSpeed > 0;
    }

    public boolean extendsGloamOnKill() {
        return hasMode(MODE_SHARED_BURIAL) && burialRange > 0
                && stainExtensionTicks > 0 && extraDurationCapTicks > 0;
    }

    public boolean pullsOnRecovery() {
        return hasMode(MODE_GRAVE_RECALL) && recallRange > 0
                && recallTargetCap > 0 && recallPullStrength > 0;
    }

    public boolean detonatesEmbedded() {
        return hasMode(MODE_MAUSOLEUM_BURST) && burstRange > 0
                && burstTargetCap > 0 && burstDamageMultiplier > 0 && burstWindowTicks > 0;
    }

    public void write(NbtCompound parent) {
        NbtCompound n = new NbtCompound();
        n.putInt("materialize", materializeTicks);
        n.putInt("fall_interval", fallIntervalTicks);
        n.putDouble("fall_speed", fallSpeed);
        n.putDouble("impact_radius", impactRadius);
        n.putDouble("impact_damage", impactDamageMultiplier);
        n.putInt("impact_cap", impactTargetCap);
        n.putInt("embedded_duration", embeddedDurationTicks);
        n.putDouble("stain_radius", stainRadius);
        n.putInt("stain_duration", stainDurationTicks);
        n.putInt("stain_amplifier", stainAmplifier);
        n.putInt("slow_duration", slowDurationTicks);
        n.putInt("mode", mode);
        n.putInt("orbit_cap", orbitCap);
        n.putDouble("recovery_radius", recoveryRadius);
        n.putInt("orbit_duration", orbitDurationTicks);
        n.putInt("recovery_lockout", recoveryLockoutTicks);
        n.putDouble("launch_speed", launchSpeed);
        n.putDouble("launch_range", launchRange);
        n.putInt("projectile_lifetime", projectileLifetimeTicks);
        n.putDouble("homing_range", homingRange);
        n.putDouble("homing_turn", homingTurnDegrees);
        n.putDouble("projectile_damage", projectileDamageMultiplier);
        n.putInt("launch_count", launchCount);
        n.putDouble("stored_bonus", storedBonusPerCutlass);
        n.putDouble("stored_cap", storedBonusCap);
        n.putDouble("gloam_vulnerability", gloamVulnerabilityBonus);
        n.putDouble("haunt_range", hauntRange);
        n.putInt("haunt_interval", hauntIntervalTicks);
        n.putDouble("haunt_damage", hauntDamageMultiplier);
        n.putInt("haunt_cap", hauntTargetCap);
        n.putDouble("burial_range", burialRange);
        n.putInt("stain_extension", stainExtensionTicks);
        n.putInt("extra_duration_cap", extraDurationCapTicks);
        n.putDouble("recall_range", recallRange);
        n.putInt("recall_cap", recallTargetCap);
        n.putDouble("recall_pull", recallPullStrength);
        n.putDouble("gravewalk_range", gravewalkRange);
        n.putDouble("gravewalk_speed", gravewalkSpeed);
        n.putDouble("gravewalk_damage", gravewalkDamageMultiplier);
        n.putDouble("burst_range", burstRange);
        n.putInt("burst_cap", burstTargetCap);
        n.putDouble("burst_damage", burstDamageMultiplier);
        n.putInt("burst_window", burstWindowTicks);
        parent.put(NBT_KEY, n);
    }

    public static WraithmawTuningSnapshot read(NbtCompound parent) {
        return read(parent, null);
    }

    static WraithmawTuningSnapshot read(NbtCompound parent, WraithmawTuningSnapshot fallback) {
        if (!parent.contains(NBT_KEY)) {
            return fallback == null ? from(null) : fallback;
        }
        NbtCompound n = parent.getCompound(NBT_KEY);
        return new WraithmawTuningSnapshot(
                n.getInt("materialize"), Math.max(1, n.getInt("fall_interval")), n.getDouble("fall_speed"),
                n.getDouble("impact_radius"), n.getDouble("impact_damage"), n.getInt("impact_cap"),
                n.getInt("embedded_duration"), n.getDouble("stain_radius"), n.getInt("stain_duration"),
                n.getInt("stain_amplifier"), Math.max(1, n.getInt("slow_duration")), n.getInt("mode"),
                Math.clamp(n.getInt("orbit_cap"), 1, 12), n.getDouble("recovery_radius"),
                n.getInt("orbit_duration"), n.getInt("recovery_lockout"),
                n.getDouble("launch_speed"), n.getDouble("launch_range"),
                Math.max(1, n.getInt("projectile_lifetime")),
                n.getDouble("homing_range"), n.getDouble("homing_turn"), n.getDouble("projectile_damage"),
                Math.clamp(n.getInt("launch_count"), 1, 16),
                n.getDouble("stored_bonus"), n.getDouble("stored_cap"),
                n.getDouble("gloam_vulnerability"),
                n.getDouble("haunt_range"), Math.max(1, n.getInt("haunt_interval")),
                n.getDouble("haunt_damage"), n.getInt("haunt_cap"),
                n.getDouble("burial_range"), n.getInt("stain_extension"), n.getInt("extra_duration_cap"),
                n.getDouble("recall_range"), n.getInt("recall_cap"), n.getDouble("recall_pull"),
                n.getDouble("gravewalk_range"), n.getDouble("gravewalk_speed"),
                n.getDouble("gravewalk_damage"),
                n.getDouble("burst_range"), n.getInt("burst_cap"), n.getDouble("burst_damage"),
                n.getInt("burst_window"));
    }
}
