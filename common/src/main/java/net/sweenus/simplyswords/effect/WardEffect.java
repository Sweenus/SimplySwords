package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import static java.lang.Math.round;

public class WardEffect extends StatusEffect {
    public WardEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getEntityWorld().isClient()) {
            if (livingEntity instanceof PlayerEntity) {
                if (livingEntity.age % 20 == 0) {
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20, round(livingEntity.getHealth() / 4 )), livingEntity);
                }
            }
        }
        super.applyUpdateEffect(world, livingEntity, amplifier);
        return true;
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }
}
