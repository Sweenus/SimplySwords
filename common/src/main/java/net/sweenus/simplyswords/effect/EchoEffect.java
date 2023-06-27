package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.sweenus.simplyswords.registry.EffectRegistry;

public class EchoEffect extends StatusEffect {
    public EchoEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.getWorld().isClient()) {
            if (pLivingEntity.age % 15 == 0) {
                pLivingEntity.timeUntilRegen = 0;
                pLivingEntity.damage(pLivingEntity.getDamageSources().magic(), 2+pAmplifier);
                pLivingEntity.removeStatusEffect(EffectRegistry.ECHO.get());
            }
        }

        super.applyUpdateEffect(pLivingEntity, pAmplifier);

    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

}
