package net.sweenus.simplyswords.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

public class AbilityMethods {

    //Storm's Edge - Storm Jolt
    public static void tickAbilityStormJolt(ItemStack stack, World world, Entity entity,
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

    //Thunder Brand - Thunder Blitz
    public static void tickAbilityThunderBlitz(ItemStack stack, World world, Entity entity,
                                               int ability_timer, int ability_timer_max, int abilityDamage, int skillCooldown, int radius) {
        if (!entity.world.isClient() && (entity instanceof PlayerEntity player)) {

            //Player dash control
            if (ability_timer > (ability_timer_max - 42) && ability_timer < (ability_timer_max - 40)) {
                player.setVelocity(player.getRotationVector().multiply(+6));
                player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z); // Prevent player flying to the heavens
                player.velocityModified = true;
                player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
                world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.3f, 1.6f);
            }
            //Player dash end
            if (ability_timer < 5) {
                player.setVelocity(0, 0, 0); // Stop player at end of charge
                player.velocityModified = true;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 2), player);

            }

            //AOE Damage & charge control
            if (player.age % 3 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                Box box = new Box(player.getX() + radius, player.getY() + radius * 2, player.getZ() + radius, player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if (entities != null) {
                        if (entities instanceof LivingEntity le) {

                            float choose = (float) (Math.random() * 1);

                            if (ability_timer > (ability_timer_max - 40)) {
                                le.damage(DamageSource.MAGIC, abilityDamage);
                                world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_02.get(), SoundCategory.PLAYERS, 0.1f, choose);

                            }

                            if (ability_timer < (ability_timer_max - 40)) {
                                le.damage(DamageSource.MAGIC, abilityDamage * 10);
                                world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(), SoundCategory.PLAYERS, 0.1f, choose);
                            }

                        }
                    }
                }

                //world.playSoundFromEntity(null, player, SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(), SoundCategory.PLAYERS, 0.1f, 0.6f);
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

    //Lichblade - Soul Anguish
    public static void tickAbilitySoulAnguish(ItemStack stack, World world, Entity entity,
                                              int ability_timer, int ability_timer_max, int abilityDamage,
                                              int skillCooldown, int radius, int damageTracker, int chanceReduce,
                                              double lastX, double lastY, double lastZ,
                                              double targetX, double targetY, double targetZ,
                                              int range, float healAmount, LivingEntity abilityTarget) {

    if (!entity.world.isClient() && (entity instanceof PlayerEntity player) && abilityTarget != null) {

            //3D sound control
            float soundDistance = 0.2f - (float) player.squaredDistanceTo(lastX, lastY, lastZ) / 800;

            //Target tracking cloud
            if (player.age % 5 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                Box box = new Box(lastX + radius, lastY + radius, lastZ + radius, lastX - radius, lastY - radius, lastZ - radius);
                for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if (entities != null) {
                        if (entities instanceof LivingEntity le) {

                            //Heal effect
                            float choose = (float) (Math.random() * 1);
                            if (player.getRandom().nextInt(100) <= (8 - chanceReduce)) {
                                world.playSoundFromEntity(null, le, SoundRegistry.DARK_SWORD_BREAKS.get(), SoundCategory.PLAYERS, soundDistance, choose);
                                player.heal(healAmount);
                                chanceReduce++;
                            }
                            le.damage(DamageSource.MAGIC, abilityDamage);
                            damageTracker += abilityDamage * 3;

                        }
                    }
                }

                world.playSound(null, lastX, lastY, lastZ, SoundRegistry.DARK_SWORD_BLOCK.get(), SoundCategory.PLAYERS, soundDistance, 0.3f, 100);

                double xpos = lastX - (radius + 1);
                double ypos = lastY;
                double zpos = lastZ - (radius + 1);
                world.playSound(xpos, ypos, zpos, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.1f, 0.2f, true);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM, xpos + i + choose,
                                ypos,
                                zpos + j + choose,
                                choose / 3, -0.3, choose / 3);
                        choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.SOUL, xpos + i + choose,
                                ypos,
                                zpos + j + choose,
                                choose / 3, 0, choose / 3);
                    }
                }
            }
        }
    }
}