package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BloodStainVisualEntity;
import net.sweenus.simplyswords.entity.DevourerMassVisualEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DevourerStainManager {
    private static final int CONTACT_INTERVAL = 5;
    private static final int COLLAPSE_TICKS = 8;
    private static final int MAX_SEGMENTS_PER_CARRIER = 4;
    private static final double VERTICAL_RANGE = 6.0;
    private static final double MAX_CARRIER_STEP = 3.0;
    private static final double MIN_TRAIL_STEP = 0.16;
    private static final double TURN_COSINE = 0.7071067811865476;
    private static final String VISUAL_TAG = "simplyswords_devourer_stain_visual";
    private static final Map<ServerWorld, Map<UUID, ActiveField>> ACTIVE = new HashMap<>();

    private DevourerStainManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveField> fields = ACTIVE.get(world);
        return fields != null && !fields.isEmpty() || world.getTime() % 40L == 0L;
    }

    public static boolean contains(ServerWorld world, Vec3d position) {
        if (world == null || position == null) {
            return false;
        }
        for (ActiveField field : ACTIVE.getOrDefault(world, Map.of()).values()) {
            DevourerMassVisualEntity mass = resolveMass(world, field.massVisualId);
            if (mass != null && Math.abs(position.y - field.center.y) <= VERTICAL_RANGE) {
                double mainRadius = Math.max(0.4, mass.getStableRadius(0.0F) * 2.1);
                if (horizontalDistanceSquared(position, field.center) <= mainRadius * mainRadius) {
                    return true;
                }
            }
            if (containsTrail(field.segments, position)) {
                return true;
            }
        }
        return false;
    }

    public static void begin(ServerWorld world, UUID ownerId, UUID sourcePlayerId,
                             UUID massVisualId, Vec3d center,
                             long activeStartTick, long activeEndTick, long collapseEndTick) {
        ActiveField field = new ActiveField(ownerId, sourcePlayerId, massVisualId,
                center, activeStartTick, activeEndTick, collapseEndTick);
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(massVisualId, field);
    }

    public static void cancel(ServerWorld world, UUID massVisualId) {
        Map<UUID, ActiveField> fields = ACTIVE.get(world);
        ActiveField field = fields == null ? null : fields.remove(massVisualId);
        if (field != null) {
            discardVisuals(world, field);
        }
        if (fields != null && fields.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ActiveField> fields = ACTIVE.get(world);
        if (fields == null || fields.isEmpty()) {
            if (world.getTime() % 40L == 0L) purgeOrphans(world, Set.of());
            return;
        }
        long now = world.getTime();
        Iterator<ActiveField> iterator = fields.values().iterator();
        while (iterator.hasNext()) {
            ActiveField field = iterator.next();
            if (now >= field.collapseEndTick) {
                discardVisuals(world, field);
                iterator.remove();
                continue;
            }
            DevourerMassVisualEntity mass = resolveMass(world, field.massVisualId);
            LivingEntity owner = resolveLiving(world, field.ownerId);
            if (mass == null || owner == null) {
                discardVisuals(world, field);
                iterator.remove();
                continue;
            }
            field.segments.removeIf(segment ->
                    !(world.getEntity(segment.visualId) instanceof BloodStainVisualEntity));
            for (Carrier carrier : field.carriers.values()) {
                if (carrier.currentSegment != null && !field.segments.contains(carrier.currentSegment)) {
                    carrier.currentSegment = null;
                }
            }
            if (now >= field.activeEndTick) {
                field.insideMain.clear();
                field.carriers.clear();
                continue;
            }
            if (now < field.activeStartTick) {
                continue;
            }
            tickCarriers(world, field, mass, now);
            if (now % CONTACT_INTERVAL == 0L) {
                tickSurfaceContacts(world, owner, field, mass, now);
            }
        }
        if (fields.isEmpty()) {
            ACTIVE.remove(world);
        } else if (now % 40L == 0L) {
            Set<UUID> activeVisualIds = fields.values().stream()
                    .flatMap(field -> field.segments.stream())
                    .map(segment -> segment.visualId)
                    .collect(java.util.stream.Collectors.toSet());
            purgeOrphans(world, activeVisualIds);
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

    private static void tickSurfaceContacts(ServerWorld world, LivingEntity owner,
                                            ActiveField field, DevourerMassVisualEntity mass,
                                            long now) {
        double mainRadius = Math.max(0.4, mass.getStableRadius(0.0F) * 2.1);
        Box mainBounds = Box.of(field.center, mainRadius * 2.0,
                VERTICAL_RANGE * 2.0, mainRadius * 2.0);
        Map<UUID, Vec3d> insideNow = new HashMap<>();
        Set<UUID> slowed = new HashSet<>();
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, mainBounds,
                entity -> entity.isAlive() && entity.isOnGround()
                        && isValidTarget(world, owner, field.sourcePlayerId, entity))) {
            if (horizontalDistanceSquared(target.getPos(), field.center) > mainRadius * mainRadius) {
                continue;
            }
            insideNow.put(target.getUuid(), target.getPos());
            applySlow(world, owner, target);
            slowed.add(target.getUuid());
        }

        int carrierCap = Math.max(1, Config.uniqueEffects.devourer.stainCarrierCap);
        int segmentCap = carrierCap * MAX_SEGMENTS_PER_CARRIER;
        for (Map.Entry<UUID, Vec3d> previous : field.insideMain.entrySet()) {
            if (insideNow.containsKey(previous.getKey())
                    || field.carriers.containsKey(previous.getKey())
                    || field.carriers.size() >= carrierCap
                    || field.segments.size() >= segmentCap) {
                continue;
            }
            LivingEntity target = resolveLiving(world, previous.getKey());
            if (target == null || !target.isOnGround()
                    || !isValidTarget(world, owner, field.sourcePlayerId, target)
                    || Math.abs(target.getY() - field.center.y) > VERTICAL_RANGE
                    || horizontalDistanceSquared(target.getPos(), field.center) <= mainRadius * mainRadius
                    || target.getPos().squaredDistanceTo(previous.getValue())
                    > MAX_CARRIER_STEP * MAX_CARRIER_STEP) {
                continue;
            }
            int duration = Math.max(1, Config.uniqueEffects.devourer.stainSpreadDuration);
            Vec3d origin = groundPosition(world, mainBoundaryCrossing(
                    field.center, previous.getValue(), target.getPos(), mainRadius));
            field.carriers.put(target.getUuid(), new Carrier(
                    target.getUuid(), now + duration, origin));
        }
        field.insideMain.clear();
        field.insideMain.putAll(insideNow);
        applyTrailSlow(world, owner, field, slowed);
    }

    private static void tickCarriers(ServerWorld world, ActiveField field,
                                     DevourerMassVisualEntity mass, long now) {
        Iterator<Carrier> iterator = field.carriers.values().iterator();
        while (iterator.hasNext()) {
            Carrier carrier = iterator.next();
            LivingEntity target = resolveLiving(world, carrier.targetId);
            if (now >= carrier.endTick || target == null || !target.isOnGround()) {
                iterator.remove();
                continue;
            }
            Vec3d point = groundPosition(world, target.getPos());
            Vec3d movement = new Vec3d(
                    point.x - carrier.lastPoint.x, 0.0, point.z - carrier.lastPoint.z);
            double distance = movement.length();
            if (distance < MIN_TRAIL_STEP) {
                continue;
            }
            if (distance > MAX_CARRIER_STEP) {
                iterator.remove();
                continue;
            }
            Vec3d direction = movement.normalize();
            StainSegment segment = carrier.currentSegment;
            if (segment == null || segment.direction.dotProduct(direction) < TURN_COSINE
                    && carrier.segmentCount < MAX_SEGMENTS_PER_CARRIER) {
                int segmentCap = Math.max(1, Config.uniqueEffects.devourer.stainCarrierCap)
                        * MAX_SEGMENTS_PER_CARRIER;
                if (field.segments.size() >= segmentCap) {
                    if (segment == null) {
                        iterator.remove();
                    } else {
                        updateSegment(world, segment, point);
                        carrier.lastPoint = point;
                    }
                    continue;
                }
                segment = createSegment(world, field, mass.getId(), carrier.lastPoint, point);
                if (segment == null) {
                    iterator.remove();
                    continue;
                }
                field.segments.add(segment);
                carrier.currentSegment = segment;
                carrier.segmentCount++;
            } else {
                updateSegment(world, segment, point);
            }
            carrier.lastPoint = point;
        }
    }

    private static StainSegment createSegment(ServerWorld world, ActiveField field,
                                              int sourceEntityId, Vec3d start, Vec3d end) {
        Vec3d horizontal = new Vec3d(end.x - start.x, 0.0, end.z - start.z);
        double length = horizontal.length();
        if (length < 1.0E-5) {
            return null;
        }
        float radius = (float) Math.max(0.25,
                Config.uniqueEffects.devourer.stainTrailWidth * 0.5);
        Vec3d center = new Vec3d((start.x + end.x) * 0.5,
                (start.y + end.y) * 0.5, (start.z + end.z) * 0.5);
        int lifetime = Math.max(1, (int) (field.collapseEndTick - world.getTime()));
        BloodStainVisualEntity visual = new BloodStainVisualEntity(
                world, center.x, center.y, center.z,
                BloodStainVisualEntity.SHAPE_TRAIL, radius, (float) (length * 0.5),
                yaw(horizontal), (float) VERTICAL_RANGE, lifetime,
                Math.min(COLLAPSE_TICKS, lifetime), world.random.nextInt(),
                BloodStainVisualEntity.STYLE_DEVOURER);
        visual.setSourceEntityId(sourceEntityId);
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);
        return new StainSegment(start, end, horizontal.normalize(), radius, visual.getUuid());
    }

    private static void updateSegment(ServerWorld world, StainSegment segment, Vec3d end) {
        Vec3d horizontal = new Vec3d(end.x - segment.start.x, 0.0, end.z - segment.start.z);
        double length = horizontal.length();
        if (length < 1.0E-5) {
            return;
        }
        segment.end = end;
        segment.direction = horizontal.normalize();
        Entity entity = world.getEntity(segment.visualId);
        if (entity instanceof BloodStainVisualEntity visual) {
            visual.setPosition((segment.start.x + end.x) * 0.5,
                    (segment.start.y + end.y) * 0.5,
                    (segment.start.z + end.z) * 0.5);
            visual.setHalfLength((float) (length * 0.5));
            visual.setYaw(yaw(horizontal));
        }
    }

    private static void applyTrailSlow(ServerWorld world, LivingEntity owner,
                                       ActiveField field, Set<UUID> alreadySlowed) {
        if (field.segments.isEmpty()) {
            return;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (StainSegment segment : field.segments) {
            minX = Math.min(minX, Math.min(segment.start.x, segment.end.x) - segment.radius);
            minY = Math.min(minY, Math.min(segment.start.y, segment.end.y) - VERTICAL_RANGE);
            minZ = Math.min(minZ, Math.min(segment.start.z, segment.end.z) - segment.radius);
            maxX = Math.max(maxX, Math.max(segment.start.x, segment.end.x) + segment.radius);
            maxY = Math.max(maxY, Math.max(segment.start.y, segment.end.y) + VERTICAL_RANGE);
            maxZ = Math.max(maxZ, Math.max(segment.start.z, segment.end.z) + segment.radius);
        }
        Box bounds = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, bounds,
                entity -> entity.isAlive() && entity.isOnGround()
                        && isValidTarget(world, owner, field.sourcePlayerId, entity))) {
            if (alreadySlowed.contains(target.getUuid()) || !containsTrail(field.segments, target.getPos())) {
                continue;
            }
            applySlow(world, owner, target);
            alreadySlowed.add(target.getUuid());
        }
    }

    private static boolean containsTrail(List<StainSegment> segments, Vec3d position) {
        for (StainSegment segment : segments) {
            double centerY = (segment.start.y + segment.end.y) * 0.5;
            if (Math.abs(position.y - centerY) > VERTICAL_RANGE) {
                continue;
            }
            Vec3d line = new Vec3d(
                    segment.end.x - segment.start.x, 0.0,
                    segment.end.z - segment.start.z);
            double lengthSquared = line.lengthSquared();
            double progress = lengthSquared < 1.0E-8 ? 0.0 : MathHelper.clamp(
                    new Vec3d(position.x - segment.start.x, 0.0,
                            position.z - segment.start.z).dotProduct(line) / lengthSquared,
                    0.0, 1.0);
            double closestX = segment.start.x + line.x * progress;
            double closestZ = segment.start.z + line.z * progress;
            double dx = position.x - closestX;
            double dz = position.z - closestZ;
            if (dx * dx + dz * dz <= segment.radius * segment.radius) {
                return true;
            }
        }
        return false;
    }

    private static void applySlow(ServerWorld world, LivingEntity owner, LivingEntity target) {
        int amplifier = Math.clamp(Config.uniqueEffects.devourer.stainSlowAmplifier, 0, 4);
        GloamMechanicsManager.recordContact(world, owner, target, amplifier);
    }

    private static boolean isValidTarget(ServerWorld world, LivingEntity owner,
                                         UUID sourcePlayerId, LivingEntity target) {
        if (target == owner || target.getWorld() != world || target.isRemoved()
                || !EntityPredicates.VALID_LIVING_ENTITY.test(target)
                || !HelperMethods.checkAbilityTarget(target, owner)) {
            return false;
        }
        LivingEntity sourcePlayer = resolveLiving(world, sourcePlayerId);
        return sourcePlayer == null || target != sourcePlayer
                && HelperMethods.checkAbilityTarget(target, sourcePlayer);
    }

    private static Vec3d groundPosition(ServerWorld world, Vec3d position) {
        return new Vec3d(position.x,
                LivyatanWaveManager.findGroundTopY(world, position.x, position.z, position.y),
                position.z);
    }

    private static Vec3d mainBoundaryCrossing(Vec3d center, Vec3d inside,
                                              Vec3d outside, double radius) {
        double low = 0.0;
        double high = 1.0;
        double radiusSquared = radius * radius;
        for (int iteration = 0; iteration < 10; iteration++) {
            double middle = (low + high) * 0.5;
            Vec3d point = inside.lerp(outside, middle);
            if (horizontalDistanceSquared(point, center) <= radiusSquared) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return inside.lerp(outside, high);
    }

    private static float yaw(Vec3d direction) {
        return (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
    }

    private static double horizontalDistanceSquared(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                ? living : null;
    }

    private static DevourerMassVisualEntity resolveMass(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof DevourerMassVisualEntity mass ? mass : null;
    }

    private static void discardVisuals(ServerWorld world, ActiveField field) {
        for (StainSegment segment : field.segments) {
            Entity visual = world.getEntity(segment.visualId);
            if (visual != null) {
                visual.discard();
            }
        }
        field.segments.clear();
        field.carriers.clear();
        field.insideMain.clear();
    }

    private static final class ActiveField {
        private final UUID ownerId;
        private final UUID sourcePlayerId;
        private final UUID massVisualId;
        private final Vec3d center;
        private final long activeStartTick;
        private final long activeEndTick;
        private final long collapseEndTick;
        private final Map<UUID, Vec3d> insideMain = new HashMap<>();
        private final Map<UUID, Carrier> carriers = new HashMap<>();
        private final List<StainSegment> segments = new ArrayList<>();

        private ActiveField(UUID ownerId, UUID sourcePlayerId, UUID massVisualId,
                            Vec3d center, long activeStartTick,
                            long activeEndTick, long collapseEndTick) {
            this.ownerId = ownerId;
            this.sourcePlayerId = sourcePlayerId;
            this.massVisualId = massVisualId;
            this.center = center;
            this.activeStartTick = activeStartTick;
            this.activeEndTick = activeEndTick;
            this.collapseEndTick = collapseEndTick;
        }
    }

    private static final class Carrier {
        private final UUID targetId;
        private final long endTick;
        private Vec3d lastPoint;
        private StainSegment currentSegment;
        private int segmentCount;

        private Carrier(UUID targetId, long endTick, Vec3d lastPoint) {
            this.targetId = targetId;
            this.endTick = endTick;
            this.lastPoint = lastPoint;
        }
    }

    private static final class StainSegment {
        private final Vec3d start;
        private final float radius;
        private final UUID visualId;
        private Vec3d end;
        private Vec3d direction;

        private StainSegment(Vec3d start, Vec3d end, Vec3d direction,
                             float radius, UUID visualId) {
            this.start = start;
            this.end = end;
            this.direction = direction;
            this.radius = radius;
            this.visualId = visualId;
        }
    }
}
