package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LivingEntityAbilityMovementManager {

    private static final double PROJECTILE_AIM_BASE_HEIGHT = 0.65;
    private static final double PROJECTILE_AIM_DISTANCE_FACTOR = 0.08;
    private static final double PROJECTILE_AIM_MAX_EXTRA_HEIGHT = 1.8;
    private static final Map<ServerWorld, Map<UUID, Long>> STOP_TICKS = new HashMap<>();
    private static final Map<ServerWorld, List<ImpactDash>> IMPACT_DASHES = new HashMap<>();

    private LivingEntityAbilityMovementManager() {
    }

    public static Vec3d getLobbedTargetDirection(LivingEntity actor, LivingEntity target) {
        Vec3d start = actor.getEyePos();
        double horizontalDistance = Math.sqrt(actor.squaredDistanceTo(target.getX(), actor.getY(), target.getZ()));
        double extraHeight = Math.min(PROJECTILE_AIM_MAX_EXTRA_HEIGHT, horizontalDistance * PROJECTILE_AIM_DISTANCE_FACTOR);
        Vec3d end = new Vec3d(target.getX(), target.getEyeY() + PROJECTILE_AIM_BASE_HEIGHT + extraHeight, target.getZ());
        Vec3d direction = end.subtract(start);
        return direction.lengthSquared() < 0.0001 ? actor.getRotationVec(1.0F) : direction.normalize();
    }

    public static void dashTowardTarget(ServerWorld world, LivingEntity actor, LivingEntity target, double speed, int stopTicks) {
        Vec3d direction = target.getPos().subtract(actor.getPos());
        dash(world, actor, direction, speed, stopTicks);
    }

    public static void dashTowardTargetWithImpact(ServerWorld world, LivingEntity actor, LivingEntity target, double speed,
                                                  int stopTicks, double impactRadius, Runnable onImpact) {
        if (world == null || actor == null || target == null || onImpact == null) {
            return;
        }
        dashTowardTarget(world, actor, target, speed, stopTicks);
        IMPACT_DASHES.computeIfAbsent(world, ignored -> new ArrayList<>())
                .add(new ImpactDash(actor.getUuid(), target.getUuid(), world.getTime() + Math.max(1, stopTicks),
                        Math.max(0.1, impactRadius), onImpact));
    }

    public static void dashAwayFromTarget(ServerWorld world, LivingEntity actor, LivingEntity target, double speed, int stopTicks) {
        Vec3d direction = actor.getPos().subtract(target.getPos());
        dash(world, actor, direction, speed, stopTicks);
    }

    public static void leapInDirection(ServerWorld world, LivingEntity actor, Vec3d direction, double speed,
                                      double verticalVelocity, int stopTicks) {
        if (world == null || actor == null || direction == null
                || direction.horizontalLengthSquared() < 0.0001 || speed <= 0.0) {
            return;
        }
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z).normalize().multiply(speed);
        actor.setVelocity(horizontal.x, verticalVelocity, horizontal.z);
        actor.velocityModified = true;
        scheduleStop(world, actor, stopTicks);
    }

    private static void dash(ServerWorld world, LivingEntity actor, Vec3d direction, double speed, int stopTicks) {
        if (world == null || actor == null || direction.horizontalLengthSquared() < 0.0001 || speed <= 0.0) {
            return;
        }
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z).normalize().multiply(speed);
        actor.setVelocity(horizontal.x, actor.getVelocity().y, horizontal.z);
        actor.velocityModified = true;
        scheduleStop(world, actor, stopTicks);
    }

    private static void scheduleStop(ServerWorld world, LivingEntity actor, int stopTicks) {
        if (stopTicks <= 0) {
            return;
        }
        STOP_TICKS.computeIfAbsent(world, ignored -> new HashMap<>())
                .put(actor.getUuid(), world.getTime() + stopTicks);
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, Long> stops = STOP_TICKS.get(world);
        List<ImpactDash> impacts = IMPACT_DASHES.get(world);
        return (stops != null && !stops.isEmpty()) || (impacts != null && !impacts.isEmpty());
    }

    public static void tick(ServerWorld world) {
        tickImpactDashes(world);
        tickStops(world);
    }

    private static void tickImpactDashes(ServerWorld world) {
        List<ImpactDash> impacts = IMPACT_DASHES.get(world);
        if (impacts == null || impacts.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<ImpactDash> iterator = impacts.iterator();
        while (iterator.hasNext()) {
            ImpactDash impact = iterator.next();
            if (!(world.getEntity(impact.actorId()) instanceof LivingEntity actor)
                    || !(world.getEntity(impact.targetId()) instanceof LivingEntity target)
                    || !actor.isAlive()
                    || !target.isAlive()
                    || !HelperMethods.checkAbilityTarget(target, actor)
                    || now > impact.expiresAt()) {
                iterator.remove();
                continue;
            }

            if (actor.getBoundingBox().intersects(target.getBoundingBox().expand(impact.radius()))
                    || actor.squaredDistanceTo(target) <= impact.radius() * impact.radius()) {
                impact.onImpact().run();
                actor.setVelocity(0.0, actor.getVelocity().y, 0.0);
                actor.velocityModified = true;
                iterator.remove();
            }
        }
        if (impacts.isEmpty()) {
            IMPACT_DASHES.remove(world);
        }
    }

    private static void tickStops(ServerWorld world) {
        Map<UUID, Long> stops = STOP_TICKS.get(world);
        if (stops == null || stops.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<UUID, Long>> iterator = stops.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now < entry.getValue()) {
                continue;
            }
            if (world.getEntity(entry.getKey()) instanceof LivingEntity actor && actor.isAlive()) {
                actor.setVelocity(0.0, actor.getVelocity().y, 0.0);
                actor.velocityModified = true;
            }
            iterator.remove();
        }
        if (stops.isEmpty()) {
            STOP_TICKS.remove(world);
        }
    }

    private record ImpactDash(UUID actorId, UUID targetId, long expiresAt, double radius, Runnable onImpact) {
    }
}
