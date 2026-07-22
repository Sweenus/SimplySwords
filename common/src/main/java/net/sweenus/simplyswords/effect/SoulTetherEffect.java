package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class SoulTetherEffect extends StatusEffect {
    public LivingEntity sourceEntity; // The player who applied the effect
    public int additionalData; // Additional integer data
    public SoulTetherEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }
    public void setSourcePlayer(LivingEntity livingEntity) {
        sourceEntity = livingEntity;
    }
    public void setAdditionalData(int data) {
        additionalData = data;
    }
    private int remainingDetonations = Config.uniqueEffects.soulpyre.pulseCount;
    public double detonateRadius = Config.uniqueEffects.soulpyre.radius;
    public float detonateDamage = Config.uniqueEffects.soulpyre.damage;
    public float heal = Config.uniqueEffects.soulpyre.heal;

    @Override
    public boolean applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {
            int detonateDelay = 15;
            ServerWorld world = (ServerWorld) livingEntity.getWorld();
            DamageSource damageSource = livingEntity.getDamageSources().playerAttack(livingEntity instanceof PlayerEntity player ? player : null);

            if (remainingDetonations <= 0) {
                remainingDetonations = 10; // For Cycling effect
            }

            if ((livingEntity.age % detonateDelay == 0) && remainingDetonations > 0) {
                int detonateCount = remainingDetonations;
                livingEntity.heal(heal);

                Box box = HelperMethods.createBox(livingEntity, detonateRadius - detonateCount);

                for (Entity otherEntity : world.getOtherEntities(livingEntity, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if ((otherEntity instanceof LivingEntity le) &&
                            HelperMethods.checkAbilityTarget(le, livingEntity)) {
                        HelperMethods.damageThroughIframes(le, damageSource, Math.min(30, detonateDamage - detonateCount));
                        if (le.distanceTo(livingEntity) > 1)
                            le.setVelocity((livingEntity.getX() - le.getX()) / 8, (livingEntity.getY() - le.getY()) / 8, (livingEntity.getZ() - le.getZ()) / 8);

                        if (detonateDamage > le.getHealth()) {
                            detonateDamage = detonateDamage + 1;
                            HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.OMINOUS_SPAWNING, livingEntity, le,20 - detonateCount);
                        }

                    }
                }

                world.playSoundFromEntity(null, livingEntity,
                        SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                        livingEntity.getSoundCategory(),
                        0.05f,
                        0.8f + ((float) detonateCount / 10)
                );

                HelperMethods.spawnOrbitParticles(world, livingEntity.getPos(), ParticleTypes.POOF, detonateRadius - detonateCount, 35 - detonateCount);
                HelperMethods.spawnOrbitParticles(world, livingEntity.getPos().add(0, 0.1, 0), ParticleTypes.SOUL, detonateRadius - detonateCount, 35 - detonateCount);
                HelperMethods.spawnOrbitParticles(world, livingEntity.getPos(), ParticleTypes.VAULT_CONNECTION, detonateRadius - detonateCount, 20 - detonateCount);
                HelperMethods.spawnOrbitParticles(world, livingEntity.getPos().add(0, 1, 0), ParticleTypes.OMINOUS_SPAWNING, detonateRadius - detonateCount, 40 - detonateCount);

                remainingDetonations--;
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
        return true;
    }

    public void onRemoved(AttributeContainer attributeContainer) {
        remainingDetonations = 10;
        detonateDamage = 11;
        super.onRemoved(attributeContainer);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }
}
