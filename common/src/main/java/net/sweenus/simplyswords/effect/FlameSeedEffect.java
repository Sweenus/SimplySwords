package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class FlameSeedEffect extends OrbitingEffect {
    public LivingEntity sourceEntity; // The player who applied the effect
    public int additionalData; // Additional integer data
    public FlameSeedEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType(ParticleTypes.ASH);
    }
    public void setSourcePlayer(LivingEntity livingEntity) {
        sourceEntity = livingEntity;
    }
    public void setAdditionalData(int data) {
        additionalData = data;
    }

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        int duration = 0;
        if (!livingEntity.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
            float abilityDamage = Config.getFloat("emberstormDamage", "UniqueEffects", ConfigDefaultValues.emberstormDamage);
            float volume = 0.3f;
            float pitch = 1.3f;
            int frequency = 20;
            SoundEvent soundEvent = SoundEvents.ENTITY_GENERIC_BURN;
            if (livingEntity.getStatusEffect(EffectRegistry.FLAMESEED.get()) instanceof SimplySwordsStatusEffectInstance statusEffect) {
                sourceEntity = statusEffect.getSourceEntity();
                additionalData = statusEffect.getAdditionalData();
                duration = statusEffect.getDuration();
            }

            if (livingEntity.age % frequency == 0 && additionalData != 0) {
                DamageSource damageSource = livingEntity.getDamageSources().magic();
                livingEntity.timeUntilRegen = 0;

                if (duration < 20  && sourceEntity != null) {
                    HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.LAVA, 1, 8);
                    HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, 2, 6);
                    HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.POOF, 1, 10);
                    HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.EXPLOSION, 0.5, 2);
                    HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.WARPED_SPORE, 1, 10);
                    abilityDamage = Config.getFloat("emberstormDetonationDamage", "UniqueEffects", ConfigDefaultValues.emberstormDetonationDamage);
                    volume = 0.6f;
                    pitch = 1.0f;
                    soundEvent = SoundRegistry.SPELL_FIRE.get();
                    if (livingEntity.distanceTo(sourceEntity) < 30) {
                        int maxHaste = (int) Config.getFloat("emberstormMaxHaste", "UniqueEffects", ConfigDefaultValues.emberstormMaxHaste);
                        HelperMethods.incrementStatusEffect(sourceEntity, StatusEffects.HASTE, 120, 1, maxHaste);
                        //HelperMethods.spawnWaistHeightParticles(serverWorld, ParticleTypes.EFFECT, sourceEntity, livingEntity, 20);
                    }

                    Box box = HelperMethods.createBox(livingEntity, 3);
                    for (Entity entity : serverWorld.getOtherEntities(livingEntity, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, sourceEntity)) {
                            le.damage(damageSource, (abilityDamage));
                            if (!le.hasStatusEffect(EffectRegistry.FLAMESEED.get()) && additionalData > 0) {
                                additionalData -= 1;
                                SimplySwordsStatusEffectInstance flamSeedEffect = new SimplySwordsStatusEffectInstance(
                                        EffectRegistry.FLAMESEED.get(), 101, 0, false,
                                        false, true);
                                flamSeedEffect.setSourceEntity(sourceEntity);
                                flamSeedEffect.setAdditionalData(additionalData);
                                le.addStatusEffect(flamSeedEffect);
                                le.setVelocity(le.getX() - livingEntity.getX(), 0.5, le.getZ() - livingEntity.getZ());
                            }
                        }
                    }

                }

                if (sourceEntity != null) {
                    damageSource = livingEntity.getDamageSources().indirectMagic(livingEntity, sourceEntity);
                    float spellScalingModifier = Config.getFloat("emberstormSpellScaling", "UniqueEffects", ConfigDefaultValues.emberstormSpellScaling);
                    if (HelperMethods.commonSpellAttributeScaling(spellScalingModifier, sourceEntity, "fire") > abilityDamage)
                        abilityDamage = HelperMethods.commonSpellAttributeScaling(spellScalingModifier, sourceEntity, "fire");
                }
                livingEntity.damage(damageSource, (additionalData + ((float) amplifier / 4) + abilityDamage));
                serverWorld.playSound(null, livingEntity.getBlockPos(), soundEvent,
                        livingEntity.getSoundCategory(), volume, pitch);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.LAVA, 1, 4);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.ASH, 1, 6);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.SMOKE, 1, 6);
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}
