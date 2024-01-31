package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class FrostVortexEffect extends OrbitingEffect {
    public LivingEntity sourceEntity; // The player who applied the effect
    public int additionalData; // Additional integer data
    public FrostVortexEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType(ParticleTypes.SNOWFLAKE);
    }
    public void setSourcePlayer(LivingEntity livingEntity) {
        sourceEntity = livingEntity;
    }
    public void setAdditionalData(int data) {
        additionalData = data;
    }

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
            if (livingEntity.getStatusEffect(EffectRegistry.FROST_VORTEX.get()) instanceof SimplySwordsStatusEffectInstance statusEffect) {
                sourceEntity = statusEffect.getSourceEntity();
                additionalData = statusEffect.getAdditionalData();
            }

            if (livingEntity.age % Math.max(1, (15 - (amplifier))) == 0 && additionalData != 0) {
                DamageSource damageSource = livingEntity.getDamageSources().magic();
                livingEntity.timeUntilRegen = 0;
                if (sourceEntity != null)
                    damageSource = livingEntity.getDamageSources().indirectMagic(livingEntity, sourceEntity);
                livingEntity.damage(damageSource, additionalData + ((float) amplifier / 4));
            }

            if (livingEntity.age % 40 == 0) {
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 2), 0), ParticleTypes.CLOUD, 1, 6);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 2), 0), ParticleTypes.FALLING_WATER, 1, 6);
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}
