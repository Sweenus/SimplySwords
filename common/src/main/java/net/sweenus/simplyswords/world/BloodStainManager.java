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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BloodStainManager {
    private static final int CONTACT_INTERVAL = 5;
    private static final float VERTICAL_RANGE = 6.0F;
    private static final String VISUAL_TAG = "simplyswords_blood_stain_visual";
    private static final Map<ServerWorld, List<ActiveStain>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Integer>> HEAL_PROGRESS = new HashMap<>();

    private BloodStainManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveStain> stains = ACTIVE.get(world);
        return stains != null && !stains.isEmpty() || world.getTime() % 40L == 0L;
    }

    public static void createCircle(ServerWorld world, LivingEntity owner, Vec3d center, double radius) {
        if (world == null || owner == null || center == null || radius <= 0.0) {
            return;
        }
        Vec3d grounded = groundPosition(world, center);
        float stainRadius = (float) Math.max(0.5, radius);
        long now = world.getTime();
        int duration = duration();
        List<ActiveStain> stains = ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>());
        for (ActiveStain stain : stains) {
            if (stain.shape == BloodStainVisualEntity.SHAPE_CIRCLE
                    && stain.ownerId.equals(owner.getUuid())
                    && stain.center.squaredDistanceTo(grounded)
                    <= MathHelper.square(Math.max(0.75F, Math.min(stain.radius, stainRadius) * 0.5F))) {
                stain.expiryTick = now + duration;
                stain.radius = Math.max(stain.radius, stainRadius);
                updateCircleVisual(world, stain);
                return;
            }
        }

        BloodStainVisualEntity visual = new BloodStainVisualEntity(
                world, grounded.x, grounded.y, grounded.z,
                BloodStainVisualEntity.SHAPE_CIRCLE, stainRadius, 0.0F, 0.0F,
                VERTICAL_RANGE, duration, fadeDuration(), world.random.nextInt());
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);
        stains.add(ActiveStain.circle(owner.getUuid(), visual.getUuid(), grounded,
                stainRadius, now + duration));
    }

    public static UUID beginTrail(ServerWorld world, LivingEntity owner, Vec3d origin,
                                  Vec3d direction, double width) {
        if (world == null || owner == null || origin == null || direction == null) {
            return null;
        }
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() < 1.0E-6) {
            return null;
        }
        horizontal = horizontal.normalize();
        Vec3d start = groundPosition(world, origin);
        float radius = (float) Math.max(0.25, width * 0.5);
        int provisionalLifetime = duration() + Math.max(40,
                Config.uniqueEffects.bloodwake.waveLengthSteps
                        * Math.max(1, Config.uniqueEffects.bloodwake.waveStepInterval) + 20);
        BloodStainVisualEntity visual = new BloodStainVisualEntity(
                world, start.x, start.y, start.z,
                BloodStainVisualEntity.SHAPE_TRAIL, radius, 0.0F,
                yaw(horizontal), VERTICAL_RANGE, provisionalLifetime,
                fadeDuration(), world.random.nextInt());
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);

        ActiveStain stain = ActiveStain.trail(UUID.randomUUID(), owner.getUuid(),
                visual.getUuid(), start, horizontal, radius);
        ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>()).add(stain);
        return stain.id;
    }

    public static void extendTrail(ServerWorld world, UUID stainId, Vec3d end) {
        ActiveStain stain = find(world, stainId);
        if (stain == null || stain.shape != BloodStainVisualEntity.SHAPE_TRAIL || end == null) {
            return;
        }
        Vec3d grounded = groundPosition(world, end);
        double projection = Math.max(0.0, grounded.subtract(stain.start).dotProduct(stain.direction));
        if (projection <= stain.length) {
            return;
        }
        stain.length = projection;
        stain.endY = grounded.y;
        updateTrailVisual(world, stain);
    }

    public static void finishTrail(ServerWorld world, UUID stainId) {
        ActiveStain stain = find(world, stainId);
        if (stain == null || stain.shape != BloodStainVisualEntity.SHAPE_TRAIL || stain.finished) {
            return;
        }
        stain.finished = true;
        stain.expiryTick = world.getTime() + duration();
        Entity entity = world.getEntity(stain.visualId);
        if (entity instanceof BloodStainVisualEntity visual) {
            visual.setLifetime(visual.age + duration());
            visual.setFadeDuration(fadeDuration());
        }
    }

    public static void tick(ServerWorld world) {
        List<ActiveStain> stains = ACTIVE.get(world);
        if (stains == null || stains.isEmpty()) {
            HEAL_PROGRESS.remove(world);
            if (world.getTime() % 40L == 0L) {
                purgeOrphans(world, Set.of());
            }
            return;
        }

        long now = world.getTime();
        Iterator<ActiveStain> iterator = stains.iterator();
        while (iterator.hasNext()) {
            ActiveStain stain = iterator.next();
            Entity visual = world.getEntity(stain.visualId);
            if (!(visual instanceof BloodStainVisualEntity)
                    || stain.finished && now >= stain.expiryTick) {
                discardVisual(world, stain.visualId);
                iterator.remove();
            }
        }
        if (stains.isEmpty()) {
            ACTIVE.remove(world);
            HEAL_PROGRESS.remove(world);
            return;
        }
        if (now % 40L == 0L) {
            Set<UUID> activeVisualIds = stains.stream()
                    .map(stain -> stain.visualId)
                    .collect(java.util.stream.Collectors.toSet());
            purgeOrphans(world, activeVisualIds);
        }
        if (now % CONTACT_INTERVAL == 0L) {
            applySurfaceEffects(world, stains);
        }
    }

    private static void applySurfaceEffects(ServerWorld world, List<ActiveStain> stains) {
        Set<UUID> slowed = new HashSet<>();
        Set<UUID> ownersStanding = new HashSet<>();
        int slowAmplifier = Math.clamp(Config.uniqueEffects.bloodwake.stainSlowAmplifier, 0, 4);
        for (ActiveStain stain : stains) {
            LivingEntity owner = resolveLiving(world, stain.ownerId);
            if (owner == null) {
                continue;
            }
            Box bounds = bounds(stain);
            for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, bounds,
                    entity -> entity.isAlive() && entity.isOnGround())) {
                if (!contains(stain, target.getPos())) {
                    continue;
                }
                if (target == owner) {
                    ownersStanding.add(owner.getUuid());
                } else if (HelperMethods.checkAbilityTarget(target, owner)
                        && slowed.add(target.getUuid())) {
                    target.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS, CONTACT_INTERVAL * 2,
                            slowAmplifier, false, false, true), owner);
                }
            }
        }

        Map<UUID, Integer> progress = HEAL_PROGRESS.computeIfAbsent(world, ignored -> new HashMap<>());
        progress.keySet().removeIf(ownerId -> !ownersStanding.contains(ownerId));
        int interval = Math.max(CONTACT_INTERVAL, Config.uniqueEffects.bloodwake.stainHealInterval);
        float amount = Math.max(0.0F, Config.uniqueEffects.bloodwake.stainHealAmount);
        for (UUID ownerId : ownersStanding) {
            int accumulated = progress.getOrDefault(ownerId, 0) + CONTACT_INTERVAL;
            if (accumulated >= interval) {
                LivingEntity owner = resolveLiving(world, ownerId);
                if (owner != null && amount > 0.0F) {
                    owner.heal(amount);
                }
                accumulated %= interval;
            }
            progress.put(ownerId, accumulated);
        }
    }

    private static boolean contains(ActiveStain stain, Vec3d position) {
        if (Math.abs(position.y - stain.center.y) > VERTICAL_RANGE) {
            return false;
        }
        Vec3d offset = position.subtract(stain.start);
        if (stain.shape == BloodStainVisualEntity.SHAPE_CIRCLE) {
            double dx = position.x - stain.center.x;
            double dz = position.z - stain.center.z;
            return dx * dx + dz * dz <= stain.radius * stain.radius;
        }
        double along = MathHelper.clamp(offset.dotProduct(stain.direction), 0.0, stain.length);
        double closestX = stain.start.x + stain.direction.x * along;
        double closestZ = stain.start.z + stain.direction.z * along;
        double dx = position.x - closestX;
        double dz = position.z - closestZ;
        return dx * dx + dz * dz <= stain.radius * stain.radius;
    }

    private static Box bounds(ActiveStain stain) {
        if (stain.shape == BloodStainVisualEntity.SHAPE_CIRCLE) {
            return Box.of(stain.center, stain.radius * 2.0, VERTICAL_RANGE * 2.0, stain.radius * 2.0);
        }
        Vec3d end = stain.start.add(stain.direction.multiply(stain.length));
        return new Box(
                Math.min(stain.start.x, end.x) - stain.radius,
                stain.center.y - VERTICAL_RANGE,
                Math.min(stain.start.z, end.z) - stain.radius,
                Math.max(stain.start.x, end.x) + stain.radius,
                stain.center.y + VERTICAL_RANGE,
                Math.max(stain.start.z, end.z) + stain.radius);
    }

    private static void updateCircleVisual(ServerWorld world, ActiveStain stain) {
        Entity entity = world.getEntity(stain.visualId);
        if (entity instanceof BloodStainVisualEntity visual) {
            visual.setRadius(stain.radius);
            visual.setLifetime(visual.age + duration());
            visual.setFadeDuration(fadeDuration());
        }
    }

    private static void updateTrailVisual(ServerWorld world, ActiveStain stain) {
        Entity entity = world.getEntity(stain.visualId);
        if (!(entity instanceof BloodStainVisualEntity visual)) {
            return;
        }
        Vec3d end = stain.start.add(stain.direction.multiply(stain.length));
        Vec3d center = stain.start.lerp(end, 0.5);
        stain.center = new Vec3d(center.x, (stain.start.y + stain.endY) * 0.5, center.z);
        visual.setPosition(stain.center);
        visual.setHalfLength((float) (stain.length * 0.5));
        visual.setYaw(yaw(stain.direction));
    }

    private static ActiveStain find(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        for (ActiveStain stain : ACTIVE.getOrDefault(world, List.of())) {
            if (stain.id.equals(id)) {
                return stain;
            }
        }
        return null;
    }

    private static Vec3d groundPosition(ServerWorld world, Vec3d position) {
        return new Vec3d(position.x,
                LivyatanWaveManager.findGroundTopY(world, position.x, position.z, position.y),
                position.z);
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static float yaw(Vec3d direction) {
        return (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
    }

    private static int duration() {
        return Math.max(1, Config.uniqueEffects.bloodwake.stainDuration);
    }

    private static int fadeDuration() {
        return Math.clamp(Config.uniqueEffects.bloodwake.stainFadeDuration, 1, duration());
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

    private static final class ActiveStain {
        private final UUID id;
        private final UUID ownerId;
        private final UUID visualId;
        private final int shape;
        private final Vec3d start;
        private final Vec3d direction;
        private Vec3d center;
        private float radius;
        private double length;
        private double endY;
        private long expiryTick;
        private boolean finished;

        private ActiveStain(UUID id, UUID ownerId, UUID visualId, int shape,
                            Vec3d start, Vec3d direction, Vec3d center,
                            float radius, double endY, long expiryTick, boolean finished) {
            this.id = id;
            this.ownerId = ownerId;
            this.visualId = visualId;
            this.shape = shape;
            this.start = start;
            this.direction = direction;
            this.center = center;
            this.radius = radius;
            this.endY = endY;
            this.expiryTick = expiryTick;
            this.finished = finished;
        }

        private static ActiveStain circle(UUID ownerId, UUID visualId, Vec3d center,
                                          float radius, long expiryTick) {
            return new ActiveStain(UUID.randomUUID(), ownerId, visualId,
                    BloodStainVisualEntity.SHAPE_CIRCLE, center, Vec3d.ZERO,
                    center, radius, center.y, expiryTick, true);
        }

        private static ActiveStain trail(UUID id, UUID ownerId, UUID visualId,
                                         Vec3d start, Vec3d direction, float radius) {
            return new ActiveStain(id, ownerId, visualId,
                    BloodStainVisualEntity.SHAPE_TRAIL, start, direction,
                    start, radius, start.y, Long.MAX_VALUE, false);
        }
    }
}
