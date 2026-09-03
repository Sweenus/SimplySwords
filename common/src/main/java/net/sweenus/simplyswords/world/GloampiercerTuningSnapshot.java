package net.sweenus.simplyswords.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryTuning;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;

public record GloampiercerTuningSnapshot(
        double projectileSpeed,
        double homingTurnDegrees,
        int projectileLifetimeTicks,
        double projectileRange,
        int embeddedDurationTicks,
        double triggerRadius,
        double explosionRadius,
        int explosionTargetCap,
        double stainRadius,
        int stainDurationTicks,
        int stainAmplifier,
        int slowDurationTicks,
        int mode,
        double vulnerabilityBonus,
        int pullTargetCap,
        double pullStrength,
        double explosionDamageMultiplier,
        double chainRange,
        int chainDelayTicks,
        double moveRange,
        double moveSpeed,
        double expiryDamageMultiplier,
        double expiryRadius,
        int expiryTargetCap
) {
    private static final String NBT_KEY = "gloampiercer_tuning";

    public static GloampiercerTuningSnapshot from(UniqueAbilityExecution execution) {
        AbyssalSpectralMasteryTuning tuning = execution == null
                ? AbyssalSpectralMasteryTuning.EMPTY : AbyssalSpectralMasteryAbilities.tuning(execution);
        double lifetime = tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_LIFETIME, 0);
        boolean tunedLifetime = lifetime > 0;
        return new GloampiercerTuningSnapshot(
                Math.max(.1, tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED,
                        Config.uniqueEffects.gloampiercer.projectileSpeed)),
                Math.max(0, tuning.get(AbyssalSpectralMasteryTuning.Setting.HOMING_TURN_DEGREES, 6)),
                tunedLifetime ? Math.max(1, (int) Math.round(lifetime)) : 120,
                tunedLifetime ? Double.MAX_VALUE : 48,
                Math.max(20, tuning.integer(AbyssalSpectralMasteryTuning.Setting.EMBEDDED_DURATION_TICKS,
                        Config.uniqueEffects.gloampiercer.embeddedDuration)),
                Math.max(.1, tuning.get(AbyssalSpectralMasteryTuning.Setting.TRIGGER_RADIUS,
                        Config.uniqueEffects.gloampiercer.triggerRadius)),
                Math.max(.25, tuning.get(AbyssalSpectralMasteryTuning.Setting.EXPLOSION_RADIUS,
                        Config.uniqueEffects.gloampiercer.explosionRadius)),
                Math.clamp(tuning.integer(AbyssalSpectralMasteryTuning.Setting.SECONDARY_TARGET_CAP, 64), 0, 64),
                Math.max(.25, tuning.get(AbyssalSpectralMasteryTuning.Setting.STAIN_RADIUS,
                        Config.uniqueEffects.gloampiercer.stainRadius)),
                Math.max(20, tuning.integer(AbyssalSpectralMasteryTuning.Setting.STAIN_DURATION_TICKS,
                        Config.uniqueEffects.gloampiercer.stainDuration)),
                Math.clamp(tuning.integer(AbyssalSpectralMasteryTuning.Setting.STAIN_AMPLIFIER,
                        Config.uniqueEffects.gloampiercer.stainSlowAmplifier), 0, 4),
                Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.STATUS_DURATION_TICKS, 11)),
                tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0),
                Math.max(0, tuning.get(AbyssalSpectralMasteryTuning.Setting.BONUS_PER_TRIGGER, 0)),
                Math.clamp(tuning.integer(AbyssalSpectralMasteryTuning.Setting.TARGET_CAP, 0), 0, 64),
                Math.max(0, tuning.get(AbyssalSpectralMasteryTuning.Setting.PULL_STRENGTH, 0)),
                Math.max(0, tuning.get(AbyssalSpectralMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)),
                Math.max(0, tuning.get(AbyssalSpectralMasteryTuning.Setting.CHAIN_RANGE, 0)),
                Math.max(0, tuning.integer(AbyssalSpectralMasteryTuning.Setting.CHAIN_DELAY_TICKS, 0)),
                Math.max(0, tuning.get(AbyssalSpectralMasteryTuning.Setting.GLOAM_MOVE_RANGE, 0)),
                Math.max(0, tuning.get(AbyssalSpectralMasteryTuning.Setting.MOVEMENT_SPEED, 0)),
                Math.max(0, tuning.get(AbyssalSpectralMasteryTuning.Setting.IMPACT_DAMAGE_MULTIPLIER, 0)),
                Math.max(0, tuning.get(AbyssalSpectralMasteryTuning.Setting.IMPACT_RADIUS, 0)),
                Math.clamp(tuning.integer(AbyssalSpectralMasteryTuning.Setting.IMPACT_TARGET_CAP, 0), 0, 64));
    }

    public void write(NbtCompound parent) {
        NbtCompound nbt = new NbtCompound();
        nbt.putDouble("projectile_speed", projectileSpeed);
        nbt.putDouble("homing_turn", homingTurnDegrees);
        nbt.putInt("projectile_lifetime", projectileLifetimeTicks);
        nbt.putDouble("projectile_range", projectileRange);
        nbt.putInt("embedded_duration", embeddedDurationTicks);
        nbt.putDouble("trigger_radius", triggerRadius);
        nbt.putDouble("explosion_radius", explosionRadius);
        nbt.putInt("explosion_cap", explosionTargetCap);
        nbt.putDouble("stain_radius", stainRadius);
        nbt.putInt("stain_duration", stainDurationTicks);
        nbt.putInt("stain_amplifier", stainAmplifier);
        nbt.putInt("slow_duration", slowDurationTicks);
        nbt.putInt("mode", mode);
        nbt.putDouble("vulnerability_bonus", vulnerabilityBonus);
        nbt.putInt("pull_cap", pullTargetCap);
        nbt.putDouble("pull_strength", pullStrength);
        nbt.putDouble("explosion_damage", explosionDamageMultiplier);
        nbt.putDouble("chain_range", chainRange);
        nbt.putInt("chain_delay", chainDelayTicks);
        nbt.putDouble("move_range", moveRange);
        nbt.putDouble("move_speed", moveSpeed);
        nbt.putDouble("expiry_damage", expiryDamageMultiplier);
        nbt.putDouble("expiry_radius", expiryRadius);
        nbt.putInt("expiry_cap", expiryTargetCap);
        parent.put(NBT_KEY, nbt);
    }

    public static GloampiercerTuningSnapshot read(NbtCompound parent) {
        if (!parent.contains(NBT_KEY)) return from(null);
        NbtCompound nbt = parent.getCompound(NBT_KEY);
        return new GloampiercerTuningSnapshot(
                nbt.getDouble("projectile_speed"), nbt.getDouble("homing_turn"),
                nbt.getInt("projectile_lifetime"), nbt.getDouble("projectile_range"),
                nbt.getInt("embedded_duration"), nbt.getDouble("trigger_radius"),
                nbt.getDouble("explosion_radius"), nbt.getInt("explosion_cap"),
                nbt.getDouble("stain_radius"), nbt.getInt("stain_duration"),
                nbt.getInt("stain_amplifier"), nbt.getInt("slow_duration"), nbt.getInt("mode"),
                nbt.getDouble("vulnerability_bonus"), nbt.getInt("pull_cap"),
                nbt.getDouble("pull_strength"), nbt.getDouble("explosion_damage"),
                nbt.getDouble("chain_range"), nbt.getInt("chain_delay"),
                nbt.getDouble("move_range"), nbt.getDouble("move_speed"),
                nbt.getDouble("expiry_damage"), nbt.getDouble("expiry_radius"),
                nbt.getInt("expiry_cap"));
    }

    public boolean hasMode(int bit) {
        return (mode & bit) != 0;
    }

    public boolean flightExpired(int age, double traveled) {
        return age > projectileLifetimeTicks || traveled > projectileRange;
    }

    public static Vec3d turnToward(Vec3d current, Vec3d desired, double maximumAngle) {
        double dot = MathHelper.clamp(current.dotProduct(desired), -1.0, 1.0);
        double angle = Math.acos(dot);
        if (angle <= maximumAngle || angle < 1.0E-5) return desired;
        double progress = maximumAngle / angle;
        double sin = Math.sin(angle);
        if (Math.abs(sin) < 1.0E-6) {
            return current.multiply(1.0 - progress).add(desired.multiply(progress)).normalize();
        }
        return current.multiply(Math.sin((1.0 - progress) * angle) / sin)
                .add(desired.multiply(Math.sin(progress * angle) / sin)).normalize();
    }
}
