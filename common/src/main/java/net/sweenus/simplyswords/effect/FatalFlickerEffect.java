package net.sweenus.simplyswords.effect;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class FatalFlickerEffect extends StatusEffect {
    public FatalFlickerEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int pAmplifier) {

        super.applyUpdateEffect(entity, pAmplifier);


        if (!entity.getWorld().isClient() && (entity instanceof PlayerEntity player)) {

            int ability_timer = entity.getStatusEffect(EffectRegistry.FATAL_FLICKER.get()).getDuration();
            World world = entity.getWorld();
            int radius = 2;

            //Player dash forward
            if ((ability_timer >= 25 && ability_timer <= 30) ||
                    (ability_timer >= 15 && ability_timer <=20) ||
                    (ability_timer >=5 && ability_timer <=10)) {
                player.setVelocity(player.getRotationVector().multiply(+4));
                player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
                player.velocityModified = true;
                world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_03.get(),
                        SoundCategory.PLAYERS, 0.3f, 1.6f);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 3), player);
                player.timeUntilRegen = 10;
            } else {
                player.setVelocity(0, 0, 0); // Stop player at end of charges
                player.velocityModified = true;

            }

            if (player.age % 2 == 0) {
                double xpos = player.getX() - (radius + 1);
                double ypos = player.getY();
                double zpos = player.getZ() - (radius + 1);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.ELECTRIC_SPARK, xpos + i + choose,
                                ypos + 0.4,
                                zpos + j + choose,
                                0, 0.1, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.CLOUD, xpos + i + choose,
                                ypos + 0.1,
                                zpos + j + choose,
                                0, 0, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.WARPED_SPORE, xpos + i + choose,
                                ypos,
                                zpos + j + choose,
                                0, 0.1, 0);
                    }
                }
            }
        }


    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

}
