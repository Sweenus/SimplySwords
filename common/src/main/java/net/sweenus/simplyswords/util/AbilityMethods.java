package net.sweenus.simplyswords.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.SoundRegistry;

public class AbilityMethods {

    //Storm's Edge - Jolt
    public static void tickAbilityStormJolt(ItemStack stack, World world, Entity entity, int slot, boolean selected,
                                            int ability_timer, int skillCooldown, int radius) {
        if (!entity.world.isClient() && (entity instanceof PlayerEntity player)) {

            //Player dash forward
            if (ability_timer == 12 || ability_timer == 13 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                player.setVelocity(player.getRotationVector().multiply(+4));
                player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z); // Prevent player flying to the heavens
                player.velocityModified = true;
                world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_03.get(),
                        SoundCategory.PLAYERS, 0.3f, 1.6f);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 1), player);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 10, 5), player);
                player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
            }

            //Player dash end
            if (ability_timer < 5 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                player.setVelocity(0, 0, 0); // Stop player at end of charge
                player.velocityModified = true;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1), player);

            }

            if (player.age % 2 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
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
}