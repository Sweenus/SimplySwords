package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.ImplicitStatusVisualEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ImplicitStatusVisualManager {

    private static final String STATUS_VISUAL_TAG = "simplyswords_implicit_status_visual";
    private static final double BOB_HEIGHT = 0.06;
    private static final double FOLLOW_LERP = 0.34;
    private static final Map<ServerWorld, Map<UUID, UUID>> ACTIVE_VISUALS = new HashMap<>();

    private ImplicitStatusVisualManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, UUID> visuals = ACTIVE_VISUALS.get(world);
        return (visuals != null && !visuals.isEmpty()) || (world.getTime() % 40L == 0L);
    }

    public static void refresh(ServerWorld world, LivingEntity target) {
        if (!Config.general.enableModernFieldEffects || world == null || target == null || !target.isAlive() || !hasImplicitStatus(target)) {
            return;
        }

        Map<UUID, UUID> visuals = ACTIVE_VISUALS.computeIfAbsent(world, ignored -> new HashMap<>());
        UUID targetId = target.getUuid();
        ImplicitStatusVisualEntity visual = resolveVisual(world, visuals.get(targetId));
        if (visual == null) {
            Vec3d pos = visualPosition(target, world.getTime());
            visual = new ImplicitStatusVisualEntity(world, pos.x, pos.y, pos.z);
            visual.addCommandTag(STATUS_VISUAL_TAG);
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
            ImplicitStatusVisualEntity visual = resolveVisual(world, entry.getValue());
            if (!(targetEntity instanceof LivingEntity target) || visual == null || !target.isAlive() || !hasImplicitStatus(target)) {
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

    private static void updateVisual(ServerWorld world, ImplicitStatusVisualEntity visual, LivingEntity target) {
        Vec3d desired = visualPosition(target, world.getTime());
        Vec3d smoothed = visual.getPos().lerp(desired, FOLLOW_LERP);
        visual.setPos(smoothed.x, smoothed.y, smoothed.z);
        visual.setYaw(target.getYaw());
        visual.setBleedStacks(0);
        visual.setSunderAmount(getSunderAmount(target));
    }

    private static Vec3d visualPosition(LivingEntity target, long worldTime) {
        double bob = MathHelper.sin((worldTime + target.getId()) * 0.18F) * BOB_HEIGHT;
        return target.getPos().add(0.0, target.getHeight() * 0.68 + bob, 0.0);
    }

    private static boolean hasImplicitStatus(LivingEntity target) {
        return target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.SUNDERED_ARMOR));
    }

    private static int getSunderAmount(LivingEntity target) {
        StatusEffectInstance effect = target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SUNDERED_ARMOR));
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    private static ImplicitStatusVisualEntity resolveVisual(ServerWorld world, UUID visualId) {
        if (visualId == null) {
            return null;
        }
        Entity entity = world.getEntity(visualId);
        return entity instanceof ImplicitStatusVisualEntity visual && visual.isAlive() ? visual : null;
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof ImplicitStatusVisualEntity && entity.getCommandTags().contains(STATUS_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }
}
