package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;

public class VoidAssaultEffect extends OrbitingEffect {
    public LivingEntity sourceEntity; // The player who applied the effect
    public int additionalData; // Additional integer data
    public VoidAssaultEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType(ParticleTypes.SMOKE);
    }
    public void setSourcePlayer(LivingEntity livingEntity) {
        sourceEntity = livingEntity;
    }
    public void setAdditionalData(int data) {
        additionalData = data;
    }

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {

        if (livingEntity.getStatusEffect(EffectRegistry.VOIDASSAULT.get()) instanceof SimplySwordsStatusEffectInstance statusEffect) {
            sourceEntity = statusEffect.getSourceEntity();
            additionalData = statusEffect.getAdditionalData();
        }

        if (livingEntity.age % Math.max(1, (12-(amplifier * 2))) == 0 && additionalData != 0) {
            DamageSource damageSource = livingEntity.getDamageSources().magic();
            livingEntity.timeUntilRegen = 0;
            if (sourceEntity != null)
                damageSource = livingEntity.getDamageSources().indirectMagic(livingEntity, sourceEntity);
            livingEntity.damage(damageSource, additionalData + amplifier);
            //System.out.println("sourceEntity: " + sourceEntity);
            //System.out.println("additionalData: " + additionalData);
        }


        super.applyUpdateEffect(livingEntity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}
