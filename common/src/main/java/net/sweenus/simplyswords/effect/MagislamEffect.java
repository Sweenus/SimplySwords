package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Objects;


public class MagislamEffect extends OrbitingEffect {
    public MagislamEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
        setParticleType(ParticleTypes.ENCHANT);
    }


    @Override
    public boolean applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {

            if (livingEntity instanceof PlayerEntity player) {
                int ability_timer = Objects.requireNonNull(player.getStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISLAM))).getDuration();
                double radius = Config.uniqueEffects.magislam.radius;
                double leapVelocity = 1.5;
                double height = 0.9;
                double descentVelocity = 1;
                double damage_multiplier = Config.uniqueEffects.magislam.damageModifier;
                double damage = (HelperMethods.getEntityAttackDamage(livingEntity) * damage_multiplier);

                if (ability_timer >= 60) {
                    player.setVelocity(livingEntity.getRotationVector().multiply(+leapVelocity));
                player.setVelocity(livingEntity.getVelocity().x, height, livingEntity.getVelocity().z);
                player.velocityModified = true;
                }
                else if (ability_timer <= 50) {
                    player.setVelocity(livingEntity.getVelocity().x, -descentVelocity, livingEntity.getVelocity().z);
                    player.velocityModified = true;
                    if (player.isOnGround()) {
                        Box box = HelperMethods.createBox(player, radius);
                        for (Entity entities : livingEntity.getWorld().getOtherEntities(livingEntity, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                            if (entities != null) {
                                if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {
                                    le.setVelocity((le.getX() - player.getX()) / 4, (le.getY() - player.getY()) / 4, (le.getZ() - player.getZ()) / 4);
                                    le.damage(player.getDamageSources().playerAttack(player), (float) damage);
                                }
                            }
                        }
                        HelperMethods.spawnOrbitParticles((ServerWorld) player.getWorld(), player.getPos(), ParticleTypes.CAMPFIRE_COSY_SMOKE, 2, 8);
                        HelperMethods.spawnOrbitParticles((ServerWorld) player.getWorld(), player.getPos(), ParticleTypes.EXPLOSION, 1, 3);
                        player.getWorld().playSoundFromEntity(null, player, SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                                SoundCategory.PLAYERS, 0.9f, 1.1f);
                        player.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISLAM));
                        player.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.RESILIENCE));
                    }
                }
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
        return true;
    }

    @Override
    public void onRemoved(AttributeContainer attributes) {
        super.onRemoved(attributes);
    }


    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

}