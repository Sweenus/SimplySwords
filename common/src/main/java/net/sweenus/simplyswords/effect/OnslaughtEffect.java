package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;

public class OnslaughtEffect extends StatusEffect {
    public OnslaughtEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            if (pLivingEntity instanceof PlayerEntity) {

                //Grant pulsing haste
                if (pLivingEntity.age % 40 == 0) {

                    PlayerEntity player = (PlayerEntity) pLivingEntity;
                    pLivingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 10, 15),
                            pLivingEntity);
                }
                //Periodically remove haste so that players cannot abuse the mechanic
                if (pLivingEntity.age % 65 == 0) {
                    if (pLivingEntity.hasStatusEffect(StatusEffects.HASTE))
                        pLivingEntity.removeStatusEffect(StatusEffects.HASTE);
                }

                //If Onslaught is expiring, remove all haste and grant weakness. Also expire Onslaught early.
                if (pLivingEntity.hasStatusEffect(EffectRegistry.ONSLAUGHT.get())) {
                    StatusEffectInstance statusEffect = pLivingEntity.getStatusEffect(EffectRegistry.ONSLAUGHT.get());
                    assert statusEffect != null;
                    if (statusEffect.getDuration() < 10 && pLivingEntity.hasStatusEffect(StatusEffects.HASTE)) {
                        pLivingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80,
                                0), pLivingEntity);
                        pLivingEntity.removeStatusEffect(StatusEffects.HASTE);
                        pLivingEntity.removeStatusEffect(EffectRegistry.ONSLAUGHT.get());
                    }
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
