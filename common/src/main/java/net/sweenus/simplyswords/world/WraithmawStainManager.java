package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BloodStainVisualEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WraithmawStainManager {
    private static final int CONTACT_INTERVAL = 5;
    private static final int MAX_PATCHES_PER_OWNER = 32;
    private static final float VERTICAL_RANGE = 6.0F;
    private static final String VISUAL_TAG = "simplyswords_wraithmaw_stain_visual";
    private static final Map<ServerWorld, List<ActivePatch>> ACTIVE = new HashMap<>();

    private WraithmawStainManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActivePatch> patches = ACTIVE.get(world);
        return patches != null && !patches.isEmpty() || world.getTime() % 40L == 0L;
    }

    public static void createPatch(ServerWorld world, UUID ownerId, Vec3d center, double radius) {
        createPatch(world, ownerId, center, radius,
                Math.max(20, Config.uniqueEffects.wraithmaw.stainDuration),
                Math.max(1, Config.uniqueEffects.wraithmaw.stainFadeDuration),
                Math.clamp(Config.uniqueEffects.wraithmaw.stainSlowAmplifier, 0, 4));
    }

    public static void createPatch(ServerWorld world, UUID ownerId, Vec3d center, double radius,
                                   int durationTicks, int fadeTicks, int slowAmplifier) {
        if (world == null || ownerId == null || center == null || radius <= 0.0) {
            return;
        }
        float patchRadius = (float) Math.max(0.25, radius);
        int duration = Math.max(20, durationTicks);
        int fade = Math.clamp(fadeTicks, 1, duration);
        int amplifier = Math.clamp(slowAmplifier, 0, 4);
        long expiry = world.getTime() + duration;
        List<ActivePatch> patches = ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>());
        ActivePatch nearby = patches.stream()
                .filter(patch -> patch.shape == BloodStainVisualEntity.SHAPE_CIRCLE)
                .filter(patch -> patch.ownerId.equals(ownerId))
                .filter(patch -> patch.center.squaredDistanceTo(center)
                        <= Math.pow(Math.min(patch.radius, patchRadius) * 0.45, 2.0))
                .min(Comparator.comparingDouble(patch -> patch.center.squaredDistanceTo(center)))
                .orElse(null);
        if (nearby != null) {
            nearby.expiryTick = expiry;
            nearby.slowAmplifier = Math.max(nearby.slowAmplifier, amplifier);
            Entity entity = world.getEntity(nearby.visualId);
            if (entity instanceof BloodStainVisualEntity visual) {
                visual.setLifetime(visual.age + duration);
                visual.setFadeDuration(fade);
            }
            return;
        }
        enforceOwnerCap(world, patches, ownerId);
        BloodStainVisualEntity visual = new BloodStainVisualEntity(
                world, center.x, center.y, center.z,
                BloodStainVisualEntity.SHAPE_CIRCLE, patchRadius, 0.0F, 0.0F,
                VERTICAL_RANGE, duration, fade, world.random.nextInt(),
                BloodStainVisualEntity.STYLE_DEVOURER);
        visual.addCommandTag(VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return;
        }
        patches.add(ActivePatch.circle(ownerId, visual.getUuid(), center,
                patchRadius, expiry, duration, fade, amplifier));
    }

    public static UUID beginTrail(ServerWorld world, UUID ownerId, Vec3d origin,
                                  Vec3d direction, double width, int durationTicks,
                                  int fadeTicks, int slowAmplifier) {
        if (world == null || ownerId == null || origin == null || direction == null || width <= 0.0) {
            return null;
        }
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() < 1.0E-6) {
            return null;
        }
        horizontal = horizontal.normalize();
        float radius = (float) Math.max(0.25, width * 0.5);
        int duration = Math.max(20, durationTicks);
        int fade = Math.clamp(fadeTicks, 1, duration);
        int amplifier = Math.clamp(slowAmplifier, 0, 4);
        BloodStainVisualEntity visual = new BloodStainVisualEntity(
                world, origin.x, origin.y, origin.z,
                BloodStainVisualEntity.SHAPE_TRAIL, radius, 0.0F, yaw(horizontal),
                VERTICAL_RANGE, duration, fade, world.random.nextInt(),
                BloodStainVisualEntity.STYLE_DEVOURER);
        visual.addCommandTag(VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return null;
        }
        List<ActivePatch> patches = ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>());
        enforceOwnerCap(world, patches, ownerId);
        UUID trailId = UUID.randomUUID();
        patches.add(ActivePatch.trail(trailId, ownerId, visual.getUuid(), origin,
                horizontal, radius, world.getTime() + duration, duration, fade, amplifier));
        return trailId;
    }

    public static void extendTrail(ServerWorld world, UUID trailId, Vec3d end) {
        ActivePatch trail = find(world, trailId);
        if (trail == null || trail.shape != BloodStainVisualEntity.SHAPE_TRAIL || end == null) {
            return;
        }
        double projection = Math.max(0.0, end.subtract(trail.start).dotProduct(trail.direction));
        if (projection > trail.length) {
            trail.length = projection;
            trail.endY = end.y;
            trail.updateCenter();
        }
        trail.expiryTick = world.getTime() + trail.durationTicks;
        Entity entity = world.getEntity(trail.visualId);
        if (entity instanceof BloodStainVisualEntity visual) {
            visual.setPosition(trail.center.x, trail.center.y, trail.center.z);
            visual.setHalfLength((float) (trail.length * 0.5));
            visual.setYaw(yaw(trail.direction));
            visual.setLifetime(visual.age + trail.durationTicks);
            visual.setFadeDuration(trail.fadeTicks);
        }
    }

    public static void finishTrail(ServerWorld world, UUID trailId) {
        ActivePatch trail = find(world, trailId);
        if (trail == null || trail.shape != BloodStainVisualEntity.SHAPE_TRAIL) {
            return;
        }
        trail.expiryTick = world.getTime() + trail.durationTicks;
        Entity entity = world.getEntity(trail.visualId);
        if (entity instanceof BloodStainVisualEntity visual) {
            visual.setLifetime(visual.age + trail.durationTicks);
            visual.setFadeDuration(trail.fadeTicks);
        }
    }

    public static void tick(ServerWorld world) {
        List<ActivePatch> patches = ACTIVE.get(world);
        if (patches == null || patches.isEmpty()) {
            if (world.getTime() % 40L == 0L) purgeOrphans(world, Set.of());
            return;
        }
        long now = world.getTime();
        Iterator<ActivePatch> iterator = patches.iterator();
        while (iterator.hasNext()) {
            ActivePatch patch = iterator.next();
            if (now >= patch.expiryTick || world.getEntity(patch.visualId) == null) {
                discardVisual(world, patch.visualId);
                iterator.remove();
            }
        }
        if (patches.isEmpty()) {
            ACTIVE.remove(world);
            return;
        }
        if (now % 40L == 0L) {
            Set<UUID> activeVisualIds = patches.stream()
                    .map(patch -> patch.visualId)
                    .collect(java.util.stream.Collectors.toSet());
            purgeOrphans(world, activeVisualIds);
        }
        if (now % CONTACT_INTERVAL == 0L) {
            applySlowness(world, patches);
        }
    }

    private static void applySlowness(ServerWorld world, List<ActivePatch> patches) {
        Set<UUID> slowed = new HashSet<>();
        for (ActivePatch patch : patches) {
            LivingEntity owner = resolveLiving(world, patch.ownerId);
            if (owner == null) {
                continue;
            }
            for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, patch.bounds(),
                    entity -> entity.isAlive() && entity.isOnGround()
                            && entity != owner && HelperMethods.checkAbilityTarget(entity, owner))) {
                if (!patch.contains(target.getPos()) || !slowed.add(target.getUuid())) {
                    continue;
                }
                target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, CONTACT_INTERVAL * 2,
                        patch.slowAmplifier, false, false, true), owner);
            }
        }
    }

    private static void enforceOwnerCap(ServerWorld world, List<ActivePatch> patches, UUID ownerId) {
        long ownerCount = patches.stream().filter(patch -> patch.ownerId.equals(ownerId)).count();
        if (ownerCount < MAX_PATCHES_PER_OWNER) {
            return;
        }
        ActivePatch oldest = patches.stream()
                .filter(patch -> patch.ownerId.equals(ownerId))
                .min(Comparator.comparingLong(patch -> patch.expiryTick))
                .orElse(null);
        if (oldest != null) {
            discardVisual(world, oldest.visualId);
            patches.remove(oldest);
        }
    }

    private static ActivePatch find(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        for (ActivePatch patch : ACTIVE.getOrDefault(world, List.of())) {
            if (patch.id.equals(id)) {
                return patch;
            }
        }
        return null;
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                ? living : null;
    }

    private static float yaw(Vec3d direction) {
        return (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
    }

    private static void discardVisual(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void purgeOrphans(ServerWorld world, Set<UUID> activeVisualIds) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof BloodStainVisualEntity
                    && entity.getCommandTags().contains(VISUAL_TAG)
                    && !activeVisualIds.contains(entity.getUuid())) {
                entity.discard();
            }
        }
    }

    private static final class ActivePatch {
        private final UUID id;
        private final UUID ownerId;
        private final UUID visualId;
        private final int shape;
        private final Vec3d start;
        private final Vec3d direction;
        private final float radius;
        private final int durationTicks;
        private final int fadeTicks;
        private Vec3d center;
        private double length;
        private double endY;
        private long expiryTick;
        private int slowAmplifier;

        private ActivePatch(UUID id, UUID ownerId, UUID visualId, int shape,
                            Vec3d start, Vec3d direction, Vec3d center,
                            float radius, double length, double endY,
                            long expiryTick, int durationTicks, int fadeTicks,
                            int slowAmplifier) {
            this.id = id;
            this.ownerId = ownerId;
            this.visualId = visualId;
            this.shape = shape;
            this.start = start;
            this.direction = direction;
            this.center = center;
            this.radius = radius;
            this.length = length;
            this.endY = endY;
            this.expiryTick = expiryTick;
            this.durationTicks = durationTicks;
            this.fadeTicks = fadeTicks;
            this.slowAmplifier = slowAmplifier;
        }

        private static ActivePatch circle(UUID ownerId, UUID visualId, Vec3d center,
                                          float radius, long expiryTick, int durationTicks,
                                          int fadeTicks, int slowAmplifier) {
            return new ActivePatch(UUID.randomUUID(), ownerId, visualId,
                    BloodStainVisualEntity.SHAPE_CIRCLE, center, Vec3d.ZERO, center,
                    radius, 0.0, center.y, expiryTick, durationTicks, fadeTicks, slowAmplifier);
        }

        private static ActivePatch trail(UUID id, UUID ownerId, UUID visualId,
                                         Vec3d start, Vec3d direction, float radius,
                                         long expiryTick, int durationTicks,
                                         int fadeTicks, int slowAmplifier) {
            return new ActivePatch(id, ownerId, visualId,
                    BloodStainVisualEntity.SHAPE_TRAIL, start, direction, start,
                    radius, 0.0, start.y, expiryTick, durationTicks, fadeTicks, slowAmplifier);
        }

        private void updateCenter() {
            this.center = this.start.add(this.direction.multiply(this.length * 0.5));
            this.center = new Vec3d(this.center.x, (this.start.y + this.endY) * 0.5, this.center.z);
        }

        private Vec3d end() {
            Vec3d horizontal = this.start.add(this.direction.multiply(this.length));
            return new Vec3d(horizontal.x, this.endY, horizontal.z);
        }

        private Box bounds() {
            if (this.shape == BloodStainVisualEntity.SHAPE_CIRCLE) {
                return Box.of(this.center, this.radius * 2.0,
                        VERTICAL_RANGE * 2.0, this.radius * 2.0);
            }
            Vec3d end = end();
            return new Box(
                    Math.min(this.start.x, end.x) - this.radius,
                    Math.min(this.start.y, end.y) - VERTICAL_RANGE,
                    Math.min(this.start.z, end.z) - this.radius,
                    Math.max(this.start.x, end.x) + this.radius,
                    Math.max(this.start.y, end.y) + VERTICAL_RANGE,
                    Math.max(this.start.z, end.z) + this.radius);
        }

        private boolean contains(Vec3d position) {
            if (Math.abs(position.y - this.center.y) > VERTICAL_RANGE) {
                return false;
            }
            if (this.shape == BloodStainVisualEntity.SHAPE_CIRCLE) {
                double dx = position.x - this.center.x;
                double dz = position.z - this.center.z;
                return dx * dx + dz * dz <= this.radius * this.radius;
            }
            double projection = MathHelper.clamp(
                    new Vec3d(position.x - this.start.x, 0.0, position.z - this.start.z)
                            .dotProduct(this.direction), 0.0, this.length);
            double closestX = this.start.x + this.direction.x * projection;
            double closestZ = this.start.z + this.direction.z * projection;
            double dx = position.x - closestX;
            double dz = position.z - closestZ;
            return dx * dx + dz * dz <= this.radius * this.radius;
        }
    }
}
