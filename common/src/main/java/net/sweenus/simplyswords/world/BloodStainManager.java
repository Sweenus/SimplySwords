package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase8UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
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
    private static final Identifier BLOODBOUND = Identifier.of("simplyswords", "bloodbound");
    private static final Map<ServerWorld, List<ActiveStain>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Integer>> HEAL_PROGRESS = new HashMap<>();
    private static final Map<ServerWorld, Set<UUID>> BLOODBOUND_OWNERS = new HashMap<>();
    private static final Set<ServerWorld> PENDING_PURGE = new HashSet<>();

    private BloodStainManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveStain> stains = ACTIVE.get(world);
        return stains != null && !stains.isEmpty()
                || PENDING_PURGE.contains(world)
                || BLOODBOUND_OWNERS.containsKey(world);
    }

    public static int stainDuration(int configured, Phase8AbilityTuning tuning) {
        return Math.max(1, configured + tuning.integer(Phase8AbilityTuning.Setting.BLOOD_STAIN_DURATION_BONUS_TICKS, 0));
    }

    public static double stainRadius(double configured, Phase8AbilityTuning tuning) {
        double radius = configured;
        if (tuning.flag(1 << 25)) radius *= tuning.get(Phase8AbilityTuning.Setting.BLOOD_SEA_RADIUS_MULTIPLIER, 1.6);
        if (tuning.flag(1 << 26)) radius *= tuning.get(Phase8AbilityTuning.Setting.BLOOD_HEARTPOOL_RADIUS_MULTIPLIER, .6);
        return radius;
    }

    public static int slowAmplifier(int configured, Phase8AbilityTuning tuning) {
        return Math.clamp(configured + tuning.integer(Phase8AbilityTuning.Setting.BLOOD_STAIN_SLOW_BONUS, 0),
                0, tuning.integer(Phase8AbilityTuning.Setting.BLOOD_STAIN_SLOW_CAP, 4));
    }

    public static int healInterval(int configured, Phase8AbilityTuning tuning) {
        return Math.max(tuning.integer(Phase8AbilityTuning.Setting.BLOOD_STAIN_HEAL_INTERVAL_FLOOR, CONTACT_INTERVAL),
                configured + tuning.integer(Phase8AbilityTuning.Setting.BLOOD_STAIN_HEAL_INTERVAL_BONUS, 0));
    }

    public static float healAmount(float configured, Phase8AbilityTuning tuning) {
        float amount = configured + (float) tuning.get(Phase8AbilityTuning.Setting.BLOOD_STAIN_HEAL_BONUS, 0);
        if (tuning.flag(1 << 25)) return 0;
        if (tuning.flag(1 << 26)) amount *= (float) tuning.get(Phase8AbilityTuning.Setting.BLOOD_HEARTPOOL_HEAL_MULTIPLIER, 2);
        return Math.max(0, amount);
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        clearBloodbound(world);
        List<ActiveStain> stains = ACTIVE.remove(world);
        if (stains != null) stains.forEach(stain -> discardVisual(world, stain.visualId));
        HEAL_PROGRESS.remove(world);
        PENDING_PURGE.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        UUID actorId = actor.getUuid();
        HEAL_PROGRESS.values().forEach(progress -> progress.remove(actorId));
        BLOODBOUND_OWNERS.forEach((world, owners) -> {
            if (!owners.remove(actorId)) return;
            EntityAttributeInstance attribute = actor.getAttributeInstance(
                    EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
            if (attribute != null) attribute.removeModifier(BLOODBOUND);
        });
        BLOODBOUND_OWNERS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        ACTIVE.forEach((world, stains) -> stains.removeIf(stain -> {
            if (!stain.ownerId.equals(actorId)) return false;
            discardVisual(world, stain.visualId);
            return true;
        }));
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void clearAll() {
        new java.util.ArrayList<>(BLOODBOUND_OWNERS.keySet()).forEach(BloodStainManager::clearBloodbound);
        ACTIVE.forEach((world, stains) -> stains.forEach(stain -> discardVisual(world, stain.visualId)));
        ACTIVE.clear();
        HEAL_PROGRESS.clear();
        BLOODBOUND_OWNERS.clear();
        PENDING_PURGE.clear();
    }

    public static void createCircle(ServerWorld world, LivingEntity owner, Vec3d center, double radius) {
        if (world == null || owner == null || center == null || radius <= 0.0) {
            return;
        }
        Vec3d grounded = groundPosition(world, center);
        Phase8AbilityTuning tuning = tuning(world, owner);
        float stainRadius = (float) Math.max(0.5, stainRadius(radius, tuning));
        long now = world.getTime();
        int duration = duration(tuning);
        List<ActiveStain> stains = ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>());
        for (ActiveStain stain : stains) {
            if (stain.shape == BloodStainVisualEntity.SHAPE_CIRCLE
                    && stain.ownerId.equals(owner.getUuid())
                    && stain.center.squaredDistanceTo(grounded)
                    <= MathHelper.square(Math.max(0.75F, Math.min(stain.radius, stainRadius) * 0.5F))) {
                stain.expiryTick = tuning.flag(1 << 24)
                        ? Math.min(now + tuning.integer(Phase8AbilityTuning.Setting.BLOOD_CONFLUENCE_CAP_TICKS, 800),
                        Math.max(stain.expiryTick, now) + Math.max(0, duration)) : now + duration;
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
        PENDING_PURGE.add(world);
        stains.add(ActiveStain.circle(owner.getUuid(), visual.getUuid(), grounded,
                stainRadius, now + duration, tuning));
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
        Phase8AbilityTuning tuning = tuning(world, owner);
        width = stainRadius(width, tuning);
        Vec3d start = groundPosition(world, origin);
        float radius = (float) Math.max(0.25, width * 0.5);
        int provisionalLifetime = duration(tuning) + Math.max(40,
                Config.uniqueEffects.bloodwake.waveLengthSteps
                        * Math.max(1, Config.uniqueEffects.bloodwake.waveStepInterval) + 20);
        BloodStainVisualEntity visual = new BloodStainVisualEntity(
                world, start.x, start.y, start.z,
                BloodStainVisualEntity.SHAPE_TRAIL, radius, 0.0F,
                yaw(horizontal), VERTICAL_RANGE, provisionalLifetime,
                fadeDuration(), world.random.nextInt());
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);
        PENDING_PURGE.add(world);

        ActiveStain stain = ActiveStain.trail(UUID.randomUUID(), owner.getUuid(),
                visual.getUuid(), start, horizontal, radius, tuning);
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
        stain.expiryTick = world.getTime() + duration(stain.tuning);
        Entity entity = world.getEntity(stain.visualId);
        if (entity instanceof BloodStainVisualEntity visual) {
            visual.setLifetime(visual.age + duration(stain.tuning));
            visual.setFadeDuration(fadeDuration());
        }
    }

    public static void tick(ServerWorld world) {
        List<ActiveStain> stains = ACTIVE.get(world);
        if (stains == null || stains.isEmpty()) {
            HEAL_PROGRESS.remove(world);
            clearBloodbound(world);
            if (world.getTime() % 40L == 0L && PENDING_PURGE.remove(world)) {
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
            clearBloodbound(world);
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
        Set<UUID> bloodboundOwners = new HashSet<>();
        Map<UUID, Phase8AbilityTuning> ownerTunings = new HashMap<>();
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
                    ownerTunings.put(owner.getUuid(), stain.tuning);
                    if (stain.tuning.flag(1 << 22)) bloodboundOwners.add(owner.getUuid());
                } else if (HelperMethods.checkAbilityTarget(target, owner)
                        && slowed.add(target.getUuid())) {
                    if (stain.tuning.flag(1 << 26)) continue;
                    int slowAmplifier = slowAmplifier(
                            Config.uniqueEffects.bloodwake.stainSlowAmplifier, stain.tuning);
                    target.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS, CONTACT_INTERVAL * 2,
                            slowAmplifier, false, false, true), owner);
                }
            }
        }

        refreshBloodbound(world, bloodboundOwners, ownerTunings);

        Map<UUID, Integer> progress = HEAL_PROGRESS.computeIfAbsent(world, ignored -> new HashMap<>());
        progress.keySet().removeIf(ownerId -> !ownersStanding.contains(ownerId));
        for (UUID ownerId : ownersStanding) {
            Phase8AbilityTuning tuning = ownerTunings.getOrDefault(ownerId, Phase8AbilityTuning.EMPTY);
            int interval = healInterval(Config.uniqueEffects.bloodwake.stainHealInterval, tuning);
            float amount = healAmount(Config.uniqueEffects.bloodwake.stainHealAmount, tuning);
            int accumulated = progress.getOrDefault(ownerId, 0) + CONTACT_INTERVAL;
            if (accumulated >= interval) {
                LivingEntity owner = resolveLiving(world, ownerId);
                if (owner != null && amount > 0.0F) {
                    owner.heal(amount);
                }
                accumulated %= interval;
            }
            if (tuning.flag(1 << 26)) {
                int absorptionInterval = Math.max(CONTACT_INTERVAL, tuning.integer(
                        Phase8AbilityTuning.Setting.BLOOD_HEARTPOOL_ABSORPTION_INTERVAL_TICKS, 80));
                int absorption = tuning.integer(Phase8AbilityTuning.Setting.BLOOD_HEARTPOOL_ABSORPTION, 4);
                LivingEntity owner = resolveLiving(world, ownerId);
                if (owner != null && absorption > 0 && world.getTime() % absorptionInterval == 0) {
                    owner.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 80,
                            Math.max(0, absorption / 4 - 1)), owner);
                }
            }
            progress.put(ownerId, accumulated);
        }
    }

    private static void refreshBloodbound(ServerWorld world, Set<UUID> current,
                                          Map<UUID, Phase8AbilityTuning> tunings) {
        Set<UUID> previous = BLOODBOUND_OWNERS.computeIfAbsent(world, ignored -> new HashSet<>());
        for (UUID ownerId : new HashSet<>(previous)) {
            if (current.contains(ownerId)) continue;
            LivingEntity owner = resolveLiving(world, ownerId);
            if (owner != null) {
                EntityAttributeInstance attribute = owner.getAttributeInstance(
                        EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
                if (attribute != null) attribute.removeModifier(BLOODBOUND);
            }
            previous.remove(ownerId);
        }
        for (UUID ownerId : current) {
            LivingEntity owner = resolveLiving(world, ownerId);
            if (owner == null) continue;
            EntityAttributeInstance attribute = owner.getAttributeInstance(
                    EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
            if (attribute == null) continue;
            attribute.removeModifier(BLOODBOUND);
            attribute.addTemporaryModifier(new EntityAttributeModifier(BLOODBOUND,
                    tunings.getOrDefault(ownerId, Phase8AbilityTuning.EMPTY).get(
                            Phase8AbilityTuning.Setting.BLOOD_BLOODBOUND_RESISTANCE, 1),
                    EntityAttributeModifier.Operation.ADD_VALUE));
            previous.add(ownerId);
        }
        if (previous.isEmpty()) BLOODBOUND_OWNERS.remove(world);
    }

    private static void clearBloodbound(ServerWorld world) {
        Set<UUID> owners = BLOODBOUND_OWNERS.remove(world);
        if (owners == null) return;
        for (UUID ownerId : owners) {
            LivingEntity owner = resolveLiving(world, ownerId);
            if (owner == null) continue;
            EntityAttributeInstance attribute = owner.getAttributeInstance(
                    EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
            if (attribute != null) attribute.removeModifier(BLOODBOUND);
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

    public static boolean containsOwnerStain(ServerWorld world, LivingEntity owner, LivingEntity target) {
        if (world == null || owner == null || target == null) return false;
        return ACTIVE.getOrDefault(world, List.of()).stream().anyMatch(stain ->
                stain.ownerId.equals(owner.getUuid()) && contains(stain, target.getPos()));
    }

    public static float ownerStainDamageMultiplier(ServerWorld world, LivingEntity owner, LivingEntity target) {
        if (world == null || owner == null || target == null) return 1;
        return (float) ACTIVE.getOrDefault(world, List.of()).stream()
                .filter(stain -> stain.ownerId.equals(owner.getUuid()) && stain.tuning.flag(1 << 23)
                        && contains(stain, target.getPos()))
                .mapToDouble(stain -> stain.tuning.get(
                        Phase8AbilityTuning.Setting.BLOOD_FOOTING_MULTIPLIER, 1.08))
                .max().orElse(1);
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
            visual.setLifetime(visual.age + duration(stain.tuning));
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

    private static int duration(Phase8AbilityTuning tuning) {
        return stainDuration(duration(), tuning);
    }

    private static Phase8AbilityTuning tuning(ServerWorld world, LivingEntity owner) {
        if (owner == null || owner.getMainHandStack().isEmpty()) return Phase8AbilityTuning.EMPTY;
        UniqueAbilityExecution execution = Phase8CombatManager.beginPassive(
                Phase8UniqueAbilities.BLOOD_GROUND, world, owner.getMainHandStack(), owner, null);
        Phase8AbilityTuning tuning = Phase8UniqueAbilities.tuning(execution);
        UniqueAbilityApi.finish(execution, Phase8UniqueAbilities.FINISH, 0);
        return tuning;
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
        private final Phase8AbilityTuning tuning;

        private ActiveStain(UUID id, UUID ownerId, UUID visualId, int shape,
                            Vec3d start, Vec3d direction, Vec3d center,
                            float radius, double endY, long expiryTick, boolean finished,
                            Phase8AbilityTuning tuning) {
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
            this.tuning = tuning;
        }

        private static ActiveStain circle(UUID ownerId, UUID visualId, Vec3d center,
                                          float radius, long expiryTick, Phase8AbilityTuning tuning) {
            return new ActiveStain(UUID.randomUUID(), ownerId, visualId,
                    BloodStainVisualEntity.SHAPE_CIRCLE, center, Vec3d.ZERO,
                    center, radius, center.y, expiryTick, true, tuning);
        }

        private static ActiveStain trail(UUID id, UUID ownerId, UUID visualId,
                                         Vec3d start, Vec3d direction, float radius,
                                         Phase8AbilityTuning tuning) {
            return new ActiveStain(id, ownerId, visualId,
                    BloodStainVisualEntity.SHAPE_TRAIL, start, direction,
                    start, radius, start.y, Long.MAX_VALUE, false, tuning);
        }
    }
}
