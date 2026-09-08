package net.sweenus.simplyswords.world;

import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning;
import net.sweenus.simplyswords.config.Config;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class SoulstalkerGloam {
    private SoulstalkerGloam() {
    }

    public static UUID sourceId(UUID ownerId) {
        return ownerId == null ? null : UUID.nameUUIDFromBytes(
                ("simplyswords/soulstalker/" + ownerId).getBytes(StandardCharsets.UTF_8));
    }

    public static double widthScale(StormSoulMasteryTuning tuning) {
        double configured = Math.max(0.25, Config.uniqueEffects.soulstalker.stainTrailWidth);
        double tuned = tuning == null ? configured
                : tuning.get(StormSoulMasteryTuning.Setting.TRAIL_STAIN_WIDTH, configured);
        return MathHelper.clamp(tuned / configured, 0.25, 8.0);
    }

    public static int stainDuration(StormSoulMasteryTuning tuning) {
        return Math.max(20, tuning == null
                ? Config.uniqueEffects.soulstalker.stainDuration
                : tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_STAIN_DURATION_TICKS,
                        Config.uniqueEffects.soulstalker.stainDuration));
    }

    public static int slowAmplifier(StormSoulMasteryTuning tuning) {
        return Math.clamp(tuning == null
                ? Config.uniqueEffects.soulstalker.stainSlowAmplifier
                : tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_SLOW_AMPLIFIER,
                        Config.uniqueEffects.soulstalker.stainSlowAmplifier), 0, 4);
    }

    public static GloamStainManager.PatchBehavior behavior(UUID ownerId, StormSoulMasteryTuning tuning,
                                                           int durationTicks) {
        int slowTicks = tuning == null ? 0
                : tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_SLOW_DURATION_TICKS, 0);
        return new GloamStainManager.PatchBehavior(sourceId(ownerId), ItemStack.EMPTY, 0,
                slowAmplifier(tuning), slowTicks > 0 ? slowTicks : 11, true,
                0, 0, 0, 0, 0, Math.max(20, durationTicks), null);
    }

    public static void createPatch(ServerWorld world, UUID ownerId, StormSoulMasteryTuning tuning,
                                   Vec3d position, double radius, int durationOverrideTicks) {
        if (world == null || ownerId == null || position == null || radius <= 0.0) {
            return;
        }
        int duration = durationOverrideTicks > 0
                ? Math.max(20, durationOverrideTicks) : stainDuration(tuning);
        double y = LivyatanWaveManager.findGroundTopY(world, position.x, position.z, position.y + 1.5);
        GloamStainManager.createPatch(world, ownerId, new Vec3d(position.x, y, position.z),
                Math.max(0.25, radius), duration,
                Math.max(1, Config.uniqueEffects.soulstalker.stainFadeDuration),
                slowAmplifier(tuning), behavior(ownerId, tuning, duration));
    }
}
