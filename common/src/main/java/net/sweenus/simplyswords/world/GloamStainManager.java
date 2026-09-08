package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.entity.BloodStainVisualEntity;
import net.sweenus.simplyswords.entity.DevourerMassVisualEntity;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;

public final class GloamStainManager {
    private static final int CONTACT_INTERVAL = 5;
    private static final int MAX_PATCHES_PER_OWNER = 32;
    private static final float VERTICAL_RANGE = 6.0F;
    private static final double CLIENT_VISUAL_SEARCH_RANGE = 32.0;
    private static final String VISUAL_TAG = "simplyswords_gloam_stain_visual";
    private static final Map<ServerWorld, List<ActivePatch>> ACTIVE = new HashMap<>();
    private static final DustParticleEffect GLOAM_DUST =
            new DustParticleEffect(new Vector3f(0.34F, 0.07F, 0.48F), 1.0F);

    private GloamStainManager() {
    }

    public record PatchBehavior(UUID sourceId, ItemStack weaponStack, float spearDamage,
                                int slowAmplifier, int slowDurationTicks, boolean appliesSlowness,
                                double moveRange, double moveSpeed, double expiryDamageMultiplier,
                                double expiryRadius, int expiryTargetCap, int growthDurationTicks,
                                IntConsumer onResolved) {
        public static final PatchBehavior NONE = new PatchBehavior(ItemStack.EMPTY, 0, 11, true,
                0, 0, 0, 0, 0);

        public PatchBehavior {
            weaponStack = weaponStack == null ? ItemStack.EMPTY : weaponStack.copy();
            spearDamage = Math.max(0, spearDamage);
            slowAmplifier = Math.clamp(slowAmplifier, 0, 4);
            slowDurationTicks = Math.max(1, slowDurationTicks);
            moveRange = Math.max(0, moveRange);
            moveSpeed = Math.max(0, moveSpeed);
            expiryDamageMultiplier = Math.max(0, expiryDamageMultiplier);
            expiryRadius = Math.max(0, expiryRadius);
            expiryTargetCap = Math.clamp(expiryTargetCap, 0, 64);
            growthDurationTicks = Math.max(0, growthDurationTicks);
        }

        public PatchBehavior(ItemStack weaponStack, float spearDamage, int slowDurationTicks,
                             boolean appliesSlowness, double moveRange, double moveSpeed,
                             double expiryDamageMultiplier, double expiryRadius, int expiryTargetCap) {
            this(null, weaponStack, spearDamage, 0, slowDurationTicks, appliesSlowness,
                    moveRange, moveSpeed, expiryDamageMultiplier, expiryRadius, expiryTargetCap,
                    0, null);
        }

        public PatchBehavior withAmplifier(int amplifier) {
            return new PatchBehavior(sourceId, weaponStack, spearDamage, amplifier, slowDurationTicks,
                    appliesSlowness, moveRange, moveSpeed, expiryDamageMultiplier, expiryRadius,
                    expiryTargetCap, growthDurationTicks, null);
        }

        public boolean sameSource(PatchBehavior other) {
            return other != null
                    && sourceId != null && sourceId.equals(other.sourceId)
                    && appliesSlowness == other.appliesSlowness
                    && slowDurationTicks == other.slowDurationTicks
                    && growthDurationTicks == other.growthDurationTicks
                    && moveRange == other.moveRange
                    && moveSpeed == other.moveSpeed
                    && expiryDamageMultiplier == other.expiryDamageMultiplier
                    && expiryRadius == other.expiryRadius
                    && expiryTargetCap == other.expiryTargetCap;
        }

        public void resolve(int hits) {
            if (onResolved != null) {
                onResolved.accept(Math.max(0, hits));
            }
        }
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActivePatch> patches = ACTIVE.get(world);
        return patches != null && !patches.isEmpty() || world.getTime() % 40L == 0L;
    }

    public static void clear(ServerWorld world) {
        List<ActivePatch> patches = ACTIVE.remove(world);
        if (patches != null) patches.forEach(patch -> patch.resolve(0));
    }

    public static void clearAll() {
        ACTIVE.values().forEach(patches -> patches.forEach(patch -> patch.resolve(0)));
        ACTIVE.clear();
    }

    public static boolean isOnGloam(Entity entity) {
        if (entity == null) {
            return false;
        }
        Vec3d position = entity.getPos();
        World world = entity.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            for (ActivePatch patch : ACTIVE.getOrDefault(serverWorld, List.of())) {
                if (patch.contains(position)) {
                    return true;
                }
            }
            return DevourerStainManager.contains(serverWorld, position);
        }
        if (!world.isClient()) {
            return false;
        }
        Box search = Box.of(position,
                CLIENT_VISUAL_SEARCH_RANGE * 2.0, VERTICAL_RANGE * 2.0,
                CLIENT_VISUAL_SEARCH_RANGE * 2.0);
        for (BloodStainVisualEntity visual : world.getEntitiesByClass(
                BloodStainVisualEntity.class, search,
                stain -> stain.isAlive()
                        && stain.getStyle() == BloodStainVisualEntity.STYLE_DEVOURER)) {
            if (containsClientStain(visual, position)) {
                return true;
            }
        }
        for (DevourerMassVisualEntity mass : world.getEntitiesByClass(
                DevourerMassVisualEntity.class, search,
                candidate -> candidate.isAlive() && candidate.spreadsGloam())) {
            if (containsClientMass(mass, position)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOnAnyGloam(ServerWorld world, Entity entity) {
        if (world == null || entity == null) return false;
        Vec3d position = entity.getPos();
        return ACTIVE.getOrDefault(world, List.of()).stream().anyMatch(patch -> patch.contains(position))
                || DevourerStainManager.contains(world, position);
    }

    public static boolean isOnOwnerGloam(ServerWorld world, UUID ownerId, Entity entity) {
        if (world == null || ownerId == null || entity == null) return false;
        Vec3d position = entity.getPos();
        return ACTIVE.getOrDefault(world, List.of()).stream()
                .anyMatch(patch -> patch.ownerId.equals(ownerId) && patch.contains(position))
                || DevourerStainManager.containsForOwner(world, ownerId, position);
    }

    private static boolean containsClientStain(
            BloodStainVisualEntity visual, Vec3d position) {
        if (Math.abs(position.y - visual.getY()) > visual.getVerticalRange()) {
            return false;
        }
        double dx = position.x - visual.getX();
        double dz = position.z - visual.getZ();
        double radius = visual.getRadius();
        if (visual.getShape() == BloodStainVisualEntity.SHAPE_CIRCLE) {
            return dx * dx + dz * dz <= radius * radius;
        }
        double yaw = visual.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double directionX = Math.cos(yaw);
        double directionZ = Math.sin(yaw);
        double halfLength = visual.getHalfLength();
        double projection = MathHelper.clamp(
                dx * directionX + dz * directionZ, -halfLength, halfLength);
        double closestX = visual.getX() + directionX * projection;
        double closestZ = visual.getZ() + directionZ * projection;
        double closestDx = position.x - closestX;
        double closestDz = position.z - closestZ;
        return closestDx * closestDx + closestDz * closestDz <= radius * radius;
    }

    private static boolean containsClientMass(
            DevourerMassVisualEntity mass, Vec3d position) {
        if (mass.age < mass.getTravelTicks()
                || mass.age >= mass.getTotalLifetime()
                || Math.abs(position.y - mass.getY()) > VERTICAL_RANGE) {
            return false;
        }
        double radius = Math.max(0.4, mass.getStableRadius(0.0F) * 2.1);
        double dx = position.x - mass.getX();
        double dz = position.z - mass.getZ();
        return dx * dx + dz * dz <= radius * radius;
    }

    public static void createPatch(ServerWorld world, UUID ownerId, Vec3d center, double radius) {
        createPatch(world, ownerId, center, radius,
                Math.max(20, Config.uniqueEffects.wraithmaw.stainDuration),
                Math.max(1, Config.uniqueEffects.wraithmaw.stainFadeDuration),
                Math.clamp(Config.uniqueEffects.wraithmaw.stainSlowAmplifier, 0, 4));
    }

    public static void createPatch(ServerWorld world, UUID ownerId, Vec3d center, double radius,
                                   int durationTicks, int fadeTicks, int slowAmplifier) {
        createPatch(world, ownerId, center, radius, durationTicks, fadeTicks, slowAmplifier,
                PatchBehavior.NONE);
    }

    public static void createPatch(ServerWorld world, UUID ownerId, Vec3d center, double radius,
                                   int durationTicks, int fadeTicks, int slowAmplifier,
                                   PatchBehavior behavior) {
        createCircle(world, ownerId, center, radius, durationTicks, fadeTicks,
                slowAmplifier, false, 0, behavior);
    }

    public static void createGrowthPatch(ServerWorld world, UUID ownerId,
                                         Vec3d position, int slowAmplifier) {
        createGrowthPatch(world, ownerId, position, slowAmplifier, true);
    }

    public static void createGrowthPatch(ServerWorld world, UUID ownerId,
                                         Vec3d position, int slowAmplifier, boolean appliesSlowness) {
        createGrowthPatch(world, ownerId, position,
                new PatchBehavior(ItemStack.EMPTY, 0, 11, appliesSlowness, 0, 0, 0, 0, 0)
                        .withAmplifier(slowAmplifier));
    }

    public static void createGrowthPatch(ServerWorld world, UUID ownerId,
                                         Vec3d position, PatchBehavior source) {
        if (world == null || ownerId == null || position == null) {
            return;
        }
        PatchBehavior inherited = source == null ? PatchBehavior.NONE : source;
        Vec3d center = new Vec3d(position.x,
                LivyatanWaveManager.findGroundTopY(world, position.x, position.z, position.y),
                position.z);
        int duration = inherited.growthDurationTicks() > 0
                ? inherited.growthDurationTicks()
                : Math.max(20, Config.uniqueEffects.gloam.growthDuration);
        int fade = Math.clamp(Config.uniqueEffects.gloam.growthFadeDuration, 1, duration);
        createCircle(world, ownerId, center,
                Math.max(0.25, Config.uniqueEffects.gloam.growthRadius),
                duration, fade, inherited.slowAmplifier(), true, 8, inherited);
        world.spawnParticles(GLOAM_DUST, center.x, center.y + 0.08, center.z,
                16, 0.55, 0.05, 0.55, 0.035);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.12, center.z,
                8, 0.42, 0.08, 0.42, 0.018);
    }

    private static void createCircle(ServerWorld world, UUID ownerId, Vec3d center, double radius,
                                     int durationTicks, int fadeTicks, int slowAmplifier,
                                     boolean growth, int fadeInTicks, PatchBehavior behavior) {
        PatchBehavior incoming = behavior == null ? PatchBehavior.NONE : behavior;
        if (world == null || ownerId == null || center == null || radius <= 0.0) {
            incoming.resolve(0);
            return;
        }
        float patchRadius = (float) Math.max(0.25, radius);
        int duration = Math.max(20, durationTicks);
        int fade = Math.clamp(fadeTicks, 1, duration);
        int amplifier = Math.clamp(slowAmplifier, 0, 4);
        long expiry = world.getTime() + duration;
        List<ActivePatch> patches = ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>());
        if (!growth) {
            ActivePatch nearby = patches.stream()
                    .filter(patch -> patch.shape == BloodStainVisualEntity.SHAPE_CIRCLE)
                    .filter(patch -> patch.ownerId.equals(ownerId))
                    .filter(patch -> patch.compatibleWith(incoming, amplifier))
                    .filter(patch -> patch.center.squaredDistanceTo(center)
                            <= Math.pow(Math.min(patch.radius, patchRadius) * 0.45, 2.0))
                    .min(Comparator.comparingDouble(patch -> patch.center.squaredDistanceTo(center)))
                    .orElse(null);
            if (nearby != null) {
                nearby.expiryTick = expiry;
                nearby.radius = Math.max(nearby.radius, patchRadius);
                nearby.slowAmplifier = Math.max(nearby.slowAmplifier, amplifier);
                nearby.adopt(incoming);
                Entity entity = world.getEntity(nearby.visualId);
                if (entity instanceof BloodStainVisualEntity visual) {
                    visual.setRadius(nearby.radius);
                    visual.setLifetime(visual.age + duration);
                    visual.setFadeDuration(fade);
                }
                return;
            }
        }
        if (growth) {
            enforceGrowthCap(world, patches, ownerId);
        }
        enforceOwnerCap(world, patches, ownerId);
        BloodStainVisualEntity visual = new BloodStainVisualEntity(
                world, center.x, center.y, center.z,
                BloodStainVisualEntity.SHAPE_CIRCLE, patchRadius, 0.0F, 0.0F,
                VERTICAL_RANGE, duration, fade, world.random.nextInt(),
                BloodStainVisualEntity.STYLE_DEVOURER);
        visual.setFadeInDuration(fadeInTicks);
        visual.addCommandTag(VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            incoming.resolve(0);
            return;
        }
        patches.add(ActivePatch.circle(ownerId, visual.getUuid(), center,
                patchRadius, expiry, duration, fade, amplifier, growth, incoming));
    }

    public static UUID beginTrail(ServerWorld world, UUID ownerId, Vec3d origin,
                                  Vec3d direction, double width, int durationTicks,
                                  int fadeTicks, int slowAmplifier) {
        return beginTrail(world, ownerId, origin, direction, width, durationTicks, fadeTicks,
                slowAmplifier, PatchBehavior.NONE);
    }

    public static UUID beginTrail(ServerWorld world, UUID ownerId, Vec3d origin,
                                  Vec3d direction, double width, int durationTicks,
                                  int fadeTicks, int slowAmplifier, PatchBehavior behavior) {
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
        ActivePatch trail = ActivePatch.trail(trailId, ownerId, visual.getUuid(), origin,
                horizontal, radius, world.getTime() + duration, duration, fade, amplifier);
        trail.adopt(behavior);
        patches.add(trail);
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

    public static int extendPatches(ServerWorld world, UUID ownerId, Vec3d center, double range,
                                    int extensionTicks, int extraDurationCapTicks) {
        if (world == null || ownerId == null || center == null || range <= 0.0
                || extensionTicks <= 0 || extraDurationCapTicks <= 0) {
            return 0;
        }
        List<ActivePatch> patches = ACTIVE.get(world);
        if (patches == null || patches.isEmpty()) {
            return 0;
        }
        long now = world.getTime();
        double squared = range * range;
        int extended = 0;
        for (ActivePatch patch : patches) {
            if (patch.shape != BloodStainVisualEntity.SHAPE_CIRCLE
                    || !patch.ownerId.equals(ownerId)
                    || patch.expiryTick <= now
                    || patch.center.squaredDistanceTo(center) > squared) {
                continue;
            }
            int granted = Math.min(extensionTicks, extraDurationCapTicks - patch.grantedExtensionTicks);
            if (granted <= 0) {
                continue;
            }
            patch.grantedExtensionTicks += granted;
            patch.expiryTick += granted;
            Entity entity = world.getEntity(patch.visualId);
            if (entity instanceof BloodStainVisualEntity visual) {
                visual.setLifetime(visual.age + (int) Math.max(1, patch.expiryTick - now));
                visual.setFadeDuration(patch.fadeTicks);
            }
            extended++;
        }
        return extended;
    }

    public static void tick(ServerWorld world) {
        List<ActivePatch> patches = ACTIVE.get(world);
        if (patches == null || patches.isEmpty()) {
            if (world.getTime() % 40L == 0L) purgeOrphans(world, Set.of());
            return;
        }
        long now = world.getTime();
        List<ActivePatch> expiring = new ArrayList<>();
        Iterator<ActivePatch> iterator = patches.iterator();
        while (iterator.hasNext()) {
            ActivePatch patch = iterator.next();
            if (now >= patch.expiryTick || world.getEntity(patch.visualId) == null) {
                if (now >= patch.expiryTick) expiring.add(patch);
                else patch.resolve(0);
                discardVisual(world, patch.visualId);
                iterator.remove();
            } else {
                movePatch(world, patch);
            }
        }
        expiring.forEach(patch -> expirePatch(world, patch));
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
            recordContacts(world, patches);
        }
    }

    private static void recordContacts(ServerWorld world, List<ActivePatch> patches) {
        for (ActivePatch patch : patches) {
            LivingEntity owner = resolveLiving(world, patch.ownerId);
            if (owner == null) {
                continue;
            }
            for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, patch.bounds(),
                    entity -> entity.isAlive() && entity.isOnGround()
                            && entity != owner && HelperMethods.checkAbilityTarget(entity, owner))) {
                if (!patch.contains(target.getPos())) {
                    continue;
                }
                GloamMechanicsManager.recordContact(world, owner, target, patch.source());
            }
        }
    }

    private static void movePatch(ServerWorld world, ActivePatch patch) {
        double moveRange = patch.behavior.moveRange();
        double moveSpeed = patch.behavior.moveSpeed();
        if (patch.shape != BloodStainVisualEntity.SHAPE_CIRCLE || moveRange <= 0 || moveSpeed <= 0) return;
        LivingEntity owner = resolveLiving(world, patch.ownerId);
        Entity visual = world.getEntity(patch.visualId);
        if (owner == null || visual == null) return;
        LivingEntity target = world.getEntitiesByClass(LivingEntity.class,
                        Box.of(patch.center, moveRange * 2, VERTICAL_RANGE * 2, moveRange * 2),
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && HelperMethods.checkAbilityTarget(entity, owner)
                                && entity.squaredDistanceTo(patch.center) <= moveRange * moveRange
                                && isVisibleFrom(world, visual, patch.center, entity))
                .stream().min(Comparator.comparingDouble(entity ->
                        entity.squaredDistanceTo(patch.center))).orElse(null);
        if (target == null) return;
        Vec3d direction = new Vec3d(target.getX() - patch.center.x, 0, target.getZ() - patch.center.z);
        if (direction.lengthSquared() < 1.0E-6) return;
        double distance = Math.min(moveSpeed, direction.horizontalLength());
        Vec3d moved = patch.center.add(direction.normalize().multiply(distance));
        Vec3d stepped = steppedGround(world, visual, patch.center, moved);
        if (stepped == null) return;
        patch.center = stepped;
        visual.setPosition(patch.center);
    }

    private static boolean isVisibleFrom(ServerWorld world, Entity source, Vec3d center, Entity target) {
        return world.raycast(new RaycastContext(center.add(0.0, 0.4, 0.0),
                target.getBoundingBox().getCenter(), RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, source)).getType() == HitResult.Type.MISS;
    }

    private static Vec3d steppedGround(ServerWorld world, Entity source, Vec3d from, Vec3d to) {
        if (world.raycast(new RaycastContext(from.add(0.0, 0.6, 0.0), to.add(0.0, 0.6, 0.0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source))
                .getType() != HitResult.Type.MISS) {
            return null;
        }
        BlockHitResult ground = world.raycast(new RaycastContext(
                new Vec3d(to.x, from.y + 1.0, to.z), new Vec3d(to.x, from.y - 1.0, to.z),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
        if (ground.getType() == HitResult.Type.MISS) {
            return null;
        }
        double groundY = ground.getPos().y;
        if (Math.abs(groundY - from.y) > 1.0) {
            return null;
        }
        return new Vec3d(to.x, groundY, to.z);
    }

    private static void expirePatch(ServerWorld world, ActivePatch patch) {
        PatchBehavior behavior = patch.behavior;
        LivingEntity owner = resolveLiving(world, patch.ownerId);
        if (behavior.expiryDamageMultiplier() <= 0 || behavior.expiryRadius() <= 0
                || behavior.expiryTargetCap() <= 0 || behavior.weaponStack().isEmpty()
                || owner == null) {
            patch.resolve(0);
            return;
        }
        double radius = behavior.expiryRadius();
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                        Box.of(patch.center, radius * 2, radius * 2, radius * 2),
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && HelperMethods.checkAbilityTarget(entity, owner)
                                && entity.squaredDistanceTo(patch.center) <= radius * radius)
                .stream().sorted(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(patch.center)))
                .limit(behavior.expiryTargetCap()).toList();
        ItemStack stack = behavior.weaponStack().copy();
        float damage = behavior.spearDamage() * (float) behavior.expiryDamageMultiplier();
        int hits = 0;
        for (LivingEntity target : targets) {
            if (SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, damage)) hits++;
        }
        patch.resolve(hits);
    }

    private static void enforceGrowthCap(ServerWorld world, List<ActivePatch> patches, UUID ownerId) {
        int cap = Math.clamp(Config.uniqueEffects.gloam.growthPatchCap, 1, 32);
        long count = patches.stream()
                .filter(patch -> patch.growth && patch.ownerId.equals(ownerId))
                .count();
        if (count < cap) {
            return;
        }
        ActivePatch oldest = patches.stream()
                .filter(patch -> patch.growth && patch.ownerId.equals(ownerId))
                .min(Comparator.comparingLong(patch -> patch.expiryTick))
                .orElse(null);
        if (oldest != null) {
            discardVisual(world, oldest.visualId);
            patches.remove(oldest);
            oldest.resolve(0);
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
            oldest.resolve(0);
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
        private float radius;
        private final int durationTicks;
        private final int fadeTicks;
        private final boolean growth;
        private Vec3d center;
        private double length;
        private double endY;
        private long expiryTick;
        private int grantedExtensionTicks;
        private int slowAmplifier;
        private PatchBehavior behavior = PatchBehavior.NONE;
        private final List<IntConsumer> pending = new ArrayList<>();
        private boolean resolved;

        private ActivePatch(UUID id, UUID ownerId, UUID visualId, int shape,
                            Vec3d start, Vec3d direction, Vec3d center,
                            float radius, double length, double endY,
                            long expiryTick, int durationTicks, int fadeTicks,
                            int slowAmplifier, boolean growth, PatchBehavior behavior) {
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
            this.growth = growth;
            adopt(behavior);
        }

        private static ActivePatch circle(UUID ownerId, UUID visualId, Vec3d center,
                                          float radius, long expiryTick, int durationTicks,
                                          int fadeTicks, int slowAmplifier, boolean growth,
                                          PatchBehavior behavior) {
            return new ActivePatch(UUID.randomUUID(), ownerId, visualId,
                    BloodStainVisualEntity.SHAPE_CIRCLE, center, Vec3d.ZERO, center,
                    radius, 0.0, center.y, expiryTick, durationTicks, fadeTicks,
                    slowAmplifier, growth, behavior);
        }

        private static ActivePatch trail(UUID id, UUID ownerId, UUID visualId,
                                         Vec3d start, Vec3d direction, float radius,
                                         long expiryTick, int durationTicks,
                                         int fadeTicks, int slowAmplifier) {
            return new ActivePatch(id, ownerId, visualId,
                    BloodStainVisualEntity.SHAPE_TRAIL, start, direction, start,
                    radius, 0.0, start.y, expiryTick, durationTicks, fadeTicks,
                    slowAmplifier, false, PatchBehavior.NONE);
        }

        private boolean compatibleWith(PatchBehavior incoming, int amplifier) {
            if (behavior.sourceId() == null || incoming.sourceId() == null) {
                return behavior.sourceId() == null && incoming.sourceId() == null
                        && behavior.appliesSlowness() == incoming.appliesSlowness();
            }
            return slowAmplifier == amplifier && behavior.sameSource(incoming);
        }

        private void adopt(PatchBehavior incoming) {
            PatchBehavior value = incoming == null ? PatchBehavior.NONE : incoming;
            if (value.onResolved() != null) {
                pending.add(value.onResolved());
            }
            if (behavior.sourceId() != null || value.sourceId() != null) {
                behavior = value;
                return;
            }
            double currentDamage = behavior.spearDamage() * behavior.expiryDamageMultiplier();
            double incomingDamage = value.spearDamage() * value.expiryDamageMultiplier();
            PatchBehavior damage = incomingDamage >= currentDamage ? value : behavior;
            behavior = new PatchBehavior(null, damage.weaponStack(), damage.spearDamage(),
                    Math.max(behavior.slowAmplifier(), value.slowAmplifier()),
                    Math.max(behavior.slowDurationTicks(), value.slowDurationTicks()),
                    behavior.appliesSlowness() && value.appliesSlowness(),
                    Math.max(behavior.moveRange(), value.moveRange()),
                    Math.max(behavior.moveSpeed(), value.moveSpeed()),
                    damage.expiryDamageMultiplier(), damage.expiryRadius(), damage.expiryTargetCap(),
                    Math.max(behavior.growthDurationTicks(), value.growthDurationTicks()), null);
        }

        private PatchBehavior source() {
            return behavior.withAmplifier(slowAmplifier);
        }

        private void resolve(int hits) {
            if (resolved) {
                return;
            }
            resolved = true;
            int reported = Math.max(0, hits);
            for (IntConsumer callback : pending) {
                callback.accept(reported);
                reported = 0;
            }
            pending.clear();
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
