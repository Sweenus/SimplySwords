package net.sweenus.simplyswords.effect.instance;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

public class SimplySwordsStatusEffectInstance extends StatusEffectInstance {

    public LivingEntity sourceEntity;
    public int additionalData;

    public SimplySwordsStatusEffectInstance(StatusEffect type, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
        super(type, duration, amplifier, ambient, showParticles, showIcon);
    }

    public LivingEntity getSourceEntity() {
        if (sourceEntity != null)
            return sourceEntity;

        return null;
    }

    public void setSourceEntity(LivingEntity entity) {
        if (entity != null)
            sourceEntity = entity;
    }

    public int getAdditionalData() {
        if (additionalData != 0)
            return additionalData;

        return 0;
    }

    public void setAdditionalData(int data) {
        if (data != 0)
            additionalData = data;
    }


}