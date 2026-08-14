package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
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
        if (world == null || ownerId == null || center == null || radius <= 0.0) {
            return;
        }
        float patchRadius = (float) Math.max(0.25, radius);
        int duration = Math.max(20, Config.uniqueEffects.wraithmaw.stainDuration);
        int fade = Math.clamp(Config.uniqueEffects.wraithmaw.stainFadeDuration, 1, duration);
        long expiry = world.getTime() + duration;
        List<ActivePatch> patches = ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>());
        ActivePatch nearby = patches.stream()
                .filter(patch -> patch.ownerId.equals(ownerId))
                .filter(patch -> patch.center.squaredDistanceTo(center)
                        <= Math.pow(Math.min(patch.radius, patchRadius) * 0.45, 2.0))
                .min(Comparator.comparingDouble(patch -> patch.center.squaredDistanceTo(center)))
                .orElse(null);
        if (nearby != null) {
            nearby.expiryTick = expiry;
            Entity entity = world.getEntity(nearby.visualId);
            if (entity instanceof BloodStainVisualEntity visual) {
                visual.setLifetime(visual.age + duration);
                visual.setFadeDuration(fade);
            }
            return;
        }
        long ownerCount = patches.stream().filter(patch -> patch.ownerId.equals(ownerId)).count();
        if (ownerCount >= MAX_PATCHES_PER_OWNER) {
            ActivePatch oldest = patches.stream()
                    .filter(patch -> patch.ownerId.equals(ownerId))
                    .min(Comparator.comparingLong(patch -> patch.expiryTick))
                    .orElse(null);
            if (oldest != null) {
                discardVisual(world, oldest.visualId);
                patches.remove(oldest);
            }
        }
        BloodStainVisualEntity visual = new BloodStainVisualEntity(
                world, center.x, center.y, center.z,
                BloodStainVisualEntity.SHAPE_CIRCLE, patchRadius, 0.0F, 0.0F,
                VERTICAL_RANGE, duration, fade, world.random.nextInt(),
                BloodStainVisualEntity.STYLE_DEVOURER);
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);
        patches.add(new ActivePatch(ownerId, visual.getUuid(), center, patchRadius, expiry));
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
        int amplifier = Math.clamp(Config.uniqueEffects.wraithmaw.stainSlowAmplifier, 0, 4);
        for (ActivePatch patch : patches) {
            LivingEntity owner = resolveLiving(world, patch.ownerId);
            if (owner == null) {
                continue;
            }
            Box bounds = Box.of(patch.center, patch.radius * 2.0,
                    VERTICAL_RANGE * 2.0, patch.radius * 2.0);
            for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, bounds,
                    entity -> entity.isAlive() && entity.isOnGround()
                            && entity != owner && HelperMethods.checkAbilityTarget(entity, owner))) {
                double dx = target.getX() - patch.center.x;
                double dz = target.getZ() - patch.center.z;
                if (dx * dx + dz * dz > patch.radius * patch.radius
                        || Math.abs(target.getY() - patch.center.y) > VERTICAL_RANGE
                        || !slowed.add(target.getUuid())) {
                    continue;
                }
                target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, CONTACT_INTERVAL * 2,
                        amplifier, false, false, true), owner);
            }
        }
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                ? living : null;
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
        private final UUID ownerId;
        private final UUID visualId;
        private final Vec3d center;
        private final float radius;
        private long expiryTick;

        private ActivePatch(UUID ownerId, UUID visualId, Vec3d center,
                            float radius, long expiryTick) {
            this.ownerId = ownerId;
            this.visualId = visualId;
            this.center = center;
            this.radius = radius;
            this.expiryTick = expiryTick;
        }
    }
}
