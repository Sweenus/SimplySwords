package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.entity.FlameSeedVisualEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class FlamewindVisualManager {

    private static final String FLAME_SEED_VISUAL_TAG = "simplyswords_flame_seed_visual";
    private static final double MARK_HEAD_OFFSET = 0.42;
    private static final double MARK_BOB_HEIGHT = 0.09;
    private static final double FOLLOW_LERP = 0.35;
    private static final int DEFAULT_SEED_DURATION = 101;
    private static final Map<ServerWorld, Map<UUID, UUID>> ACTIVE_MARKERS = new HashMap<>();

    private FlamewindVisualManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, UUID> markers = ACTIVE_MARKERS.get(world);
        return (markers != null && !markers.isEmpty()) || (world.getTime() % 40L == 0L);
    }

    public static void refreshSeed(ServerWorld world, LivingEntity target) {
        if (!Config.general.enableModernFieldEffects || world == null || target == null || !target.isAlive()) {
            return;
        }

        Map<UUID, UUID> markers = ACTIVE_MARKERS.computeIfAbsent(world, ignored -> new HashMap<>());
        UUID targetId = target.getUuid();
        FlameSeedVisualEntity marker = resolveMarker(world, markers.get(targetId));
        if (marker == null) {
            Vec3d pos = markerPosition(target, world.getTime());
            marker = new FlameSeedVisualEntity(world, pos.x, pos.y, pos.z);
            marker.addCommandTag(FLAME_SEED_VISUAL_TAG);
            if (!world.spawnEntity(marker)) {
                return;
            }
            markers.put(targetId, marker.getUuid());
        }
        updateMarker(world, marker, target);
    }

    public static void tick(ServerWorld world) {
        Map<UUID, UUID> markers = ACTIVE_MARKERS.get(world);
        if (markers == null || markers.isEmpty()) {
            if (world.getTime() % 40L == 0L) {
                purgeOrphanMarkers(world);
            }
            return;
        }

        Iterator<Map.Entry<UUID, UUID>> iterator = markers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            Entity targetEntity = world.getEntity(entry.getKey());
            FlameSeedVisualEntity marker = resolveMarker(world, entry.getValue());
            if (!(targetEntity instanceof LivingEntity target) || marker == null || !target.isAlive() || !target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED))) {
                if (marker != null) {
                    marker.discard();
                }
                iterator.remove();
                continue;
            }

            updateMarker(world, marker, target);
        }

        if (markers.isEmpty()) {
            ACTIVE_MARKERS.remove(world);
        }
    }

    public static void spawnDetonation(ServerWorld world, LivingEntity center) {
        if (!Config.general.enableModernFieldEffects || world == null || center == null) {
            return;
        }

        spawnDetonation(world, center.getPos().add(0.0, center.getHeight() * 0.35, 0.0));
    }

    public static void spawnDetonation(ServerWorld world, Vec3d pos) {
        if (!Config.general.enableModernFieldEffects || world == null || pos == null) {
            return;
        }

        int points = 36;
        double radius = 3.0;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i;
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.FLAME, x, pos.y, z, 1, 0.02, 0.02, 0.02, 0.02);
            if (i % 2 == 0) {
                world.spawnParticles(ParticleTypes.LAVA, x, pos.y + 0.05, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 0.25, pos.z, 12, 0.45, 0.18, 0.45, 0.02);
        world.spawnParticles(ParticleTypes.POOF, pos.x, pos.y + 0.12, pos.z, 14, 0.28, 0.16, 0.28, 0.03);
        world.spawnParticles(ParticleTypes.ASH, pos.x, pos.y + 0.18, pos.z, 18, 0.65, 0.2, 0.65, 0.02);
    }

    public static void spawnSpreadArc(ServerWorld world, LivingEntity from, LivingEntity to) {
        if (!Config.general.enableModernFieldEffects || world == null || from == null || to == null) {
            return;
        }

        spawnSpreadArc(world, from.getPos().add(0.0, from.getHeight() * 0.58, 0.0), to);
    }

    public static void spawnSpreadArc(ServerWorld world, Vec3d from, LivingEntity to) {
        if (!Config.general.enableModernFieldEffects || world == null || from == null || to == null) {
            return;
        }

        Vec3d start = from.add(0.0, 0.45, 0.0);
        Vec3d end = to.getPos().add(0.0, to.getHeight() * 0.58, 0.0);
        int steps = Math.max(6, (int) Math.ceil(start.distanceTo(end) * 5.0));
        for (int i = 0; i <= steps; i++) {
            float progress = (float) i / (float) steps;
            Vec3d pos = start.lerp(end, progress).add(0.0, MathHelper.sin(progress * MathHelper.PI) * 0.55, 0.0);
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0.015, 0.015, 0.015, 0.0);
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.ASH, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    public static void removeSeed(ServerWorld world, LivingEntity target) {
        Map<UUID, UUID> markers = ACTIVE_MARKERS.get(world);
        if (markers == null || target == null) {
            return;
        }
        FlameSeedVisualEntity marker = resolveMarker(world, markers.remove(target.getUuid()));
        if (marker != null) {
            marker.discard();
        }
        if (markers.isEmpty()) {
            ACTIVE_MARKERS.remove(world);
        }
    }

    private static void updateMarker(ServerWorld world, FlameSeedVisualEntity marker, LivingEntity target) {
        Vec3d desired = markerPosition(target, world.getTime());
        Vec3d smoothed = marker.getPos().lerp(desired, FOLLOW_LERP);
        marker.setPos(smoothed.x, smoothed.y, smoothed.z);
        marker.setScale(seedScale(target));
        if (world.getTime() % 5L == 0L) {
            world.spawnParticles(ParticleTypes.SMALL_FLAME, smoothed.x, smoothed.y, smoothed.z, 1, 0.04, 0.04, 0.04, 0.002);
        }
    }

    private static Vec3d markerPosition(LivingEntity target, long worldTime) {
        double bob = Math.sin((worldTime + target.getId()) * 0.22) * MARK_BOB_HEIGHT;
        return target.getPos().add(0.0, target.getHeight() + MARK_HEAD_OFFSET + bob, 0.0);
    }

    private static float seedScale(LivingEntity target) {
        StatusEffectInstance effect = target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
        int duration = effect == null ? DEFAULT_SEED_DURATION : effect.getDuration();
        float ageFactor = 1.0F - MathHelper.clamp((float) duration / DEFAULT_SEED_DURATION, 0.0F, 1.0F);
        float pulse = 0.08F * MathHelper.sin(target.age * (0.22F + ageFactor * 0.35F));
        int spreadCount = effect instanceof SimplySwordsStatusEffectInstance simplyEffect ? simplyEffect.getAdditionalData() : 0;
        return MathHelper.clamp(0.7F + ageFactor * 0.75F + spreadCount * 0.035F + pulse, 0.55F, 1.75F);
    }

    private static FlameSeedVisualEntity resolveMarker(ServerWorld world, UUID markerId) {
        if (markerId == null) {
            return null;
        }
        Entity entity = world.getEntity(markerId);
        return entity instanceof FlameSeedVisualEntity marker && marker.isAlive() ? marker : null;
    }

    private static void purgeOrphanMarkers(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof FlameSeedVisualEntity && entity.getCommandTags().contains(FLAME_SEED_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }
}
