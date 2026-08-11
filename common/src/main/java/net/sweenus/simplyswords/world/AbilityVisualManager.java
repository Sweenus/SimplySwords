package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.render.*;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.*;

import java.util.UUID;

public final class AbilityVisualManager {

    private AbilityVisualManager() {
    }

    public static UUID spawnLightningPhenomenon(ServerWorld world, Vec3d start, Vec3d end,
                                                LightningPhenomenonStyle style) {
        if (!valid(world, start, style) || end == null || start.squaredDistanceTo(end) < 0.0001) return null;
        LightningPhenomenonVisualEntity visual = new LightningPhenomenonVisualEntity(
                world, start, end, style, world.random.nextInt());
        world.spawnEntity(visual);
        return visual.getUuid();
    }

    public static UUID spawnLightningPhenomenon(ServerWorld world, Entity start, float startYOffset,
                                                Entity end, float endYOffset,
                                                LightningPhenomenonStyle style) {
        if (world == null || start == null || end == null || style == null
                || !Config.general.enableModernFieldEffects || start.getWorld() != world || end.getWorld() != world) return null;
        LightningPhenomenonVisualEntity visual = new LightningPhenomenonVisualEntity(world,
                start.getPos().add(0.0, startYOffset, 0.0), end.getPos().add(0.0, endYOffset, 0.0),
                style, world.random.nextInt());
        visual.anchor(start, startYOffset, end, endYOffset);
        world.spawnEntity(visual);
        return visual.getUuid();
    }

    public static UUID spawnLightningPhenomenon(ServerWorld world, Vec3d start,
                                                Entity end, float endYOffset,
                                                LightningPhenomenonStyle style) {
        if (world == null || start == null || end == null || style == null
                || !Config.general.enableModernFieldEffects || end.getWorld() != world) return null;
        LightningPhenomenonVisualEntity visual = new LightningPhenomenonVisualEntity(world, start,
                end.getPos().add(0.0, endYOffset, 0.0), style, world.random.nextInt());
        visual.anchorEnd(end, endYOffset);
        world.spawnEntity(visual);
        return visual.getUuid();
    }

    public static UUID spawnStormVolume(ServerWorld world, Vec3d position, StormVolumeStyle style) {
        if (!valid(world, position, style)) return null;
        AtmosphericVisualEntity visual = new AtmosphericVisualEntity(world, position, style, world.random.nextInt());
        world.spawnEntity(visual);
        return visual.getUuid();
    }

    public static UUID spawnStormVolume(ServerWorld world, Entity anchor, StormVolumeStyle style) {
        if (world == null || anchor == null || style == null
                || !Config.general.enableModernFieldEffects || anchor.getWorld() != world) return null;
        AtmosphericVisualEntity visual = new AtmosphericVisualEntity(world, anchor.getPos(), style, world.random.nextInt());
        visual.anchor(anchor);
        world.spawnEntity(visual);
        return visual.getUuid();
    }

    public static UUID spawnSurfaceDischarge(ServerWorld world, Vec3d origin, Vec3d direction,
                                             double length, SurfaceDischargeStyle style) {
        if (!valid(world, origin, style) || direction == null || direction.horizontalLengthSquared() < 0.0001) return null;
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z).normalize();
        AtmosphericVisualEntity visual = new AtmosphericVisualEntity(world, origin,
                origin.add(horizontal.multiply(Math.max(0.1, length))), style, world.random.nextInt());
        world.spawnEntity(visual);
        return visual.getUuid();
    }

    public static UUID spawnShockFront(ServerWorld world, Vec3d origin, Vec3d direction,
                                       ShockFrontStyle style) {
        if (!valid(world, origin, style)) return null;
        AtmosphericVisualEntity visual = new AtmosphericVisualEntity(world, origin,
                direction == null ? Vec3d.ZERO : direction, style, world.random.nextInt());
        world.spawnEntity(visual);
        return visual.getUuid();
    }

    public static void discard(ServerWorld world, UUID visualId) {
        if (world == null || visualId == null) {
            return;
        }
        Entity visual = world.getEntity(visualId);
        if (visual instanceof LightningPhenomenonVisualEntity
                || visual instanceof AtmosphericVisualEntity) {
            visual.discard();
        }
    }

    private static boolean valid(ServerWorld world, Vec3d position, Object style) {
        return world != null && position != null && style != null
                && Config.general.enableModernFieldEffects;
    }
}
