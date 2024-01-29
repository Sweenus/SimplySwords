package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;

public class VoidcloakEffect extends OrbitingEffect {
    public VoidcloakEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType(ParticleTypes.SMOKE);
    }

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        super.applyUpdateEffect(livingEntity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}
