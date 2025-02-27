package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class FrostVortexEffect extends OrbitingEffect {
    public LivingEntity sourceEntity;
    public int additionalData;
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
    public boolean applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
            float abilityDamage = 0;
            if (livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FROST_VORTEX)) instanceof SimplySwordsStatusEffectInstance statusEffect) {
                sourceEntity = statusEffect.getSourceEntity();
                additionalData = statusEffect.getAdditionalData();
            }

            if (livingEntity.age % Math.max(1, (15 - (amplifier))) == 0 && additionalData != 0) {
                DamageSource damageSource = livingEntity.getDamageSources().magic();
                livingEntity.timeUntilRegen = 0;
                if (sourceEntity != null) {
                    damageSource = livingEntity.getDamageSources().indirectMagic(livingEntity, sourceEntity);
                    float spellScalingModifier = Config.uniqueEffects.vortex.spellScaling;
                    abilityDamage = HelperMethods.commonSpellAttributeScaling(spellScalingModifier, sourceEntity, "frost");
                    if (livingEntity instanceof PlayerEntity && sourceEntity instanceof PlayerEntity sourcePlayer)
                        damageSource = livingEntity.getDamageSources().playerAttack(sourcePlayer);
                }
                livingEntity.damage(damageSource, (additionalData + ((float) amplifier / 4) + abilityDamage));
            }

            if (livingEntity.age % 40 == 0) {
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 2), 0), ParticleTypes.CLOUD, 1, 6);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 2), 0), ParticleTypes.FALLING_WATER, 1, 6);
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
        return true;
    }

    @Override
    public void onRemoved(AttributeContainer attributes) {

    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}