package net.sweenus.simplyswords.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;

public final class ShadowDanceFovHandler {

    private static final int FINISH_TICKS = 8;
    private static final double MAX_FOV_ADDITION = 14.0;

    private ShadowDanceFovHandler() {
    }

    public static double modifyFov(Entity focusedEntity, double baseFov) {
        if (!(focusedEntity instanceof LivingEntity livingEntity)) {
            return baseFov;
        }

        StatusEffectInstance instance = getShadowDance(livingEntity);
        if (instance == null) {
            return baseFov;
        }

        int activeTicks = instance.getAmplifier() + 1;
        int totalTicks = activeTicks + FINISH_TICKS;
        int remainingTicks = Math.max(0, instance.getDuration());
        double intensity;
        if (remainingTicks <= FINISH_TICKS) {
            intensity = (double) remainingTicks / FINISH_TICKS;
        } else {
            int elapsedActiveTicks = Math.max(0, totalTicks - remainingTicks);
            intensity = (double) elapsedActiveTicks / Math.max(1, activeTicks);
        }

        intensity = Math.max(0.0, Math.min(1.0, intensity));
        return baseFov + MAX_FOV_ADDITION * intensity;
    }

    public static boolean isShadowDancing(Entity entity) {
        return entity instanceof LivingEntity livingEntity && getShadowDance(livingEntity) != null;
    }

    private static StatusEffectInstance getShadowDance(LivingEntity livingEntity) {
        return livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SHADOW_DANCE));
    }
}
