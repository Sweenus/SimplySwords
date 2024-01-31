package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.SoundHelper;

public class ElementalVortexEffect extends OrbitingEffect {
    public LivingEntity sourceEntity; // The player who applied the effect
    public int additionalData; // Additional integer data
    public ElementalVortexEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType(ParticleTypes.CLOUD);
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
            float abilityDamageFire = 0;
            float abilityDamageFrost = 0;
            SoundHelper.loopSound(livingEntity, SoundRegistry.AMBIENCE_WIND_LOOP.getId(), 20);

            if (livingEntity.getStatusEffect(EffectRegistry.ELEMENTAL_VORTEX.get()) instanceof SimplySwordsStatusEffectInstance statusEffect) {
                sourceEntity = statusEffect.getSourceEntity();
                additionalData = statusEffect.getAdditionalData();
            }

            if (livingEntity.age % 10 == 0) {
                Box box = HelperMethods.createBox(livingEntity, 1 + (amplifier / 6));
                for (Entity entity : serverWorld.getOtherEntities(livingEntity, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, livingEntity)) {

                        if (additionalData != 0) {
                            DamageSource damageSource = livingEntity.getDamageSources().indirectMagic(le, livingEntity);
                            damageSource = livingEntity.getDamageSources().indirectMagic(livingEntity, sourceEntity);
                            float spellScalingModifier = Config.getFloat("vortexSpellScaling", "UniqueEffects", ConfigDefaultValues.vortexSpellScaling);
                            if (HelperMethods.commonSpellAttributeScaling(spellScalingModifier, sourceEntity, "frost") > 1)
                                abilityDamageFrost = HelperMethods.commonSpellAttributeScaling(spellScalingModifier, sourceEntity, "frost");
                            if (HelperMethods.commonSpellAttributeScaling(spellScalingModifier, sourceEntity, "fire") > 1)
                                abilityDamageFire = HelperMethods.commonSpellAttributeScaling(spellScalingModifier, sourceEntity, "fire");
                            le.timeUntilRegen = 0;
                            le.damage(damageSource, (3 + ((float) amplifier / 2)) + (abilityDamageFire + abilityDamageFrost));
                        }

                    }
                }
            }

            if (livingEntity.age % 40 == 0) {
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 3), 0), ParticleTypes.LAVA, 0.5, 4);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 2), 0), ParticleTypes.SNOWFLAKE, 1, 6);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 2), 0), ParticleTypes.MYCELIUM, 1.5, 8);
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        SoundHelper.stopLoopingSound(entity, SoundRegistry.AMBIENCE_WIND_LOOP.getId());

        super.onRemoved(entity, attributes, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}
