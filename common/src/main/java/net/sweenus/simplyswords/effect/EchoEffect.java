package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;

import static java.lang.Math.round;

public class EchoEffect extends StatusEffect {
    public EchoEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            if (pLivingEntity instanceof PlayerEntity) {
                if (pLivingEntity.age % 10 == 0) {
                    pLivingEntity.damage(DamageSource.MAGIC, 3f + pAmplifier);
                    pLivingEntity.removeStatusEffect(EffectRegistry.ECHO.get());
                }
            }
        }

        super.applyUpdateEffect(pLivingEntity, pAmplifier);

    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

}
