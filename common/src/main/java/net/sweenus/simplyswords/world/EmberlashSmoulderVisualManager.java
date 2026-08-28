package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.EmberlashSmoulderVisualEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class EmberlashSmoulderVisualManager {

    private static final String SMOULDER_VISUAL_TAG = "simplyswords_emberlash_smoulder_visual";
    private static final double HEAD_OFFSET = 0.54;
    private static final double BOB_HEIGHT = 0.07;
    private static final double FOLLOW_LERP = 0.34;
    private static final float FULL_DURATION_TICKS = 100.0F;
    private static final float MIN_DURATION_SCALE = 0.25F;
    private static final Map<ServerWorld, Map<UUID, UUID>> ACTIVE_VISUALS = new HashMap<>();

    private EmberlashSmoulderVisualManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, UUID> visuals = ACTIVE_VISUALS.get(world);
        return (visuals != null && !visuals.isEmpty()) || (world.getTime() % 40L == 0L);
    }

    public static void refresh(ServerWorld world, LivingEntity target) {
        if (!Config.general.enableModernFieldEffects || world == null || target == null || !target.isAlive() || !isSmouldering(target)) {
            return;
        }

        Map<UUID, UUID> visuals = ACTIVE_VISUALS.computeIfAbsent(world, ignored -> new HashMap<>());
        UUID targetId = target.getUuid();
        EmberlashSmoulderVisualEntity visual = resolveVisual(world, visuals.get(targetId));
        if (visual == null) {
            Vec3d pos = visualPosition(target, world.getTime());
            visual = new EmberlashSmoulderVisualEntity(world, pos.x, pos.y, pos.z, getStacks(target));
            visual.addCommandTag(SMOULDER_VISUAL_TAG);
            if (!world.spawnEntity(visual)) {
                return;
            }
            visuals.put(targetId, visual.getUuid());
        }
        updateVisual(world, visual, target);
    }

    public static void tick(ServerWorld world) {
        Map<UUID, UUID> visuals = ACTIVE_VISUALS.get(world);
        if (visuals == null || visuals.isEmpty()) {
            if (world.getTime() % 40L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        Iterator<Map.Entry<UUID, UUID>> iterator = visuals.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            Entity targetEntity = world.getEntity(entry.getKey());
            EmberlashSmoulderVisualEntity visual = resolveVisual(world, entry.getValue());
            if (!(targetEntity instanceof LivingEntity target) || visual == null || !target.isAlive() || !isSmouldering(target)) {
                if (visual != null) {
                    visual.discard();
                }
                iterator.remove();
                continue;
            }

            updateVisual(world, visual, target);
        }

        if (visuals.isEmpty()) {
            ACTIVE_VISUALS.remove(world);
        }
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        Map<UUID, UUID> visuals = ACTIVE_VISUALS.remove(world);
        if (visuals != null) {
            for (UUID visualId : visuals.values()) {
                EmberlashSmoulderVisualEntity visual = resolveVisual(world, visualId);
                if (visual != null) visual.discard();
            }
        }
        purgeOrphanVisuals(world);
    }

    public static void clearAll() {
        for (ServerWorld world : java.util.List.copyOf(ACTIVE_VISUALS.keySet())) clear(world);
        ACTIVE_VISUALS.clear();
    }

    private static void updateVisual(ServerWorld world, EmberlashSmoulderVisualEntity visual, LivingEntity target) {
        Vec3d desired = visualPosition(target, world.getTime());
        Vec3d smoothed = visual.getPos().lerp(desired, FOLLOW_LERP);
        visual.setPos(smoothed.x, smoothed.y, smoothed.z);
        visual.setYaw(target.getYaw());
        visual.setStacks(getStacks(target));
        visual.setScale(getDurationScale(target));
        if (world.getTime() % 8L == 0L) {
            world.spawnParticles(ParticleTypes.SMALL_FLAME, smoothed.x, smoothed.y + 0.04, smoothed.z, 1, 0.12, 0.08, 0.12, 0.004);
        }
    }

    private static Vec3d visualPosition(LivingEntity target, long worldTime) {
        double bob = MathHelper.sin((worldTime + target.getId()) * 0.18F) * BOB_HEIGHT;
        return target.getPos().add(0.0, target.getHeight() + HEAD_OFFSET + bob, 0.0);
    }

    private static boolean isSmouldering(LivingEntity target) {
        return target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
    }

    private static int getStacks(LivingEntity target) {
        StatusEffectInstance effect = target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
        return effect == null ? 1 : Math.max(1, effect.getAmplifier() + 1);
    }

    private static float getDurationScale(LivingEntity target) {
        StatusEffectInstance effect = target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
        if (effect == null) {
            return MIN_DURATION_SCALE;
        }
        float durationProgress = MathHelper.clamp(effect.getDuration() / FULL_DURATION_TICKS, 0.0F, 1.0F);
        return MathHelper.lerp(durationProgress, MIN_DURATION_SCALE, 1.0F);
    }

    private static EmberlashSmoulderVisualEntity resolveVisual(ServerWorld world, UUID visualId) {
        if (visualId == null) {
            return null;
        }
        Entity entity = world.getEntity(visualId);
        return entity instanceof EmberlashSmoulderVisualEntity visual && visual.isAlive() ? visual : null;
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof EmberlashSmoulderVisualEntity && entity.getCommandTags().contains(SMOULDER_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }
}
