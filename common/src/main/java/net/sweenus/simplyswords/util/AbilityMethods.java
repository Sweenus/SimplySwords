package net.sweenus.simplyswords.util;

import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

public class AbilityMethods {

    //Storm's Edge - Storm Jolt
    public static void tickAbilityStormJolt(ItemStack stack, World world, Entity user,
                                            int ability_timer, int skillCooldown, int radius) {
        if (!user.getWorld().isClient() && (user instanceof PlayerEntity player)) {

            //Player dash forward
            if (ability_timer == 12 || ability_timer == 13 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                player.setVelocity(player.getRotationVector().multiply(+4));
                player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z); // Prevent player flying to the heavens
                player.velocityModified = true;
                world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_03.get(),
                        player.getSoundCategory(), 0.3f, 1.6f);
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
                        HelperMethods.spawnParticle(world, ParticleTypes.ELECTRIC_SPARK,
                                xpos + i + choose, ypos + 0.4, zpos + j + choose,
                                0, 0.1, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.CLOUD,
                                xpos + i + choose, ypos + 0.1, zpos + j + choose,
                                0, 0, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.WARPED_SPORE,
                                xpos + i + choose, ypos, zpos + j + choose,
                                0, 0.1, 0);
                    }
                }
            }
        }
    }

    //Mjolnir - Storm
    public static void tickAbilityStorm(ItemStack stack, World world, Entity user,
                                        int ability_timer, int skillCooldown, int radius) {
        if (!user.getWorld().isClient() && (user instanceof PlayerEntity player)) {

            if (player.age % 10 == 0) {
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 15, 5), player);
                Box box = new Box(x + radius, y + radius, z + radius, x - radius, y - radius, z - radius);
                ServerWorld sworld = (ServerWorld) player.getWorld();

                for (Entity entity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    float choose = (float) (Math.random() * 1);
                    if ((entity instanceof LivingEntity ee)) {
                        if (HelperMethods.checkFriendlyFire(ee, player) && choose > 0.7) {
                            var stormtarget = ee.getBlockPos();
                            ee.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), 10, 0), player);
                            LightningEntity storm = EntityType.LIGHTNING_BOLT.spawn(sworld, stormtarget, SpawnReason.TRIGGERED);
                            if (storm != null) {
                                storm.setCosmetic(true);
                            }
                            ee.damage(player.getDamageSources().magic(), 5);
                        }
                    }
                }
            }
            if (player.age % 5 == 0) {
                double xpos = player.getX() - (radius + 1);
                double ypos = player.getY();
                double zpos = player.getZ() - (radius + 1);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.CLOUD,
                                xpos + i + choose, ypos + 10, zpos + j + choose,
                                0, 0, 0);
                    }
                }
            }
        }
    }

    //Thunder Brand - Thunder Blitz
    public static void tickAbilityThunderBlitz(ItemStack stack, World world, Entity user,
                                               int ability_timer, int ability_timer_max, float abilityDamage, int skillCooldown, int radius) {
        if (!user.getWorld().isClient() && (user instanceof PlayerEntity player)) {

            //Player dash control
            if (ability_timer > (ability_timer_max - 42) && ability_timer < (ability_timer_max - 40)) {
                player.setVelocity(player.getRotationVector().multiply(+6));
                player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z); // Prevent player flying to the heavens
                player.velocityModified = true;
                player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
                world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(),
                        player.getSoundCategory(), 0.3f, 1.6f);
            }
            //Player dash end
            if (ability_timer < 5) {
                player.setVelocity(0, 0, 0); // Stop player at end of charge
                player.velocityModified = true;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 2), player);

            }

            //AOE Damage & charge control
            if (player.age % 3 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                Box box = new Box(player.getX() + radius, player.getY() + radius * 2, player.getZ() + radius,
                        player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                for (Entity entity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {

                        float choose = (float) (Math.random() * 1);

                        if (ability_timer > (ability_timer_max - 40)) {
                            le.damage(world.getDamageSources().magic(), abilityDamage);
                            world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_02.get(),
                                    le.getSoundCategory(), 0.1f, choose);
                        } else if (ability_timer < (ability_timer_max - 40)) {
                            le.damage(world.getDamageSources().magic(), abilityDamage * 3);
                            world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(),
                                    le.getSoundCategory(), 0.1f, choose);
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
                        HelperMethods.spawnParticle(world, ParticleTypes.ELECTRIC_SPARK,
                                xpos + i + choose, ypos + 0.4, zpos + j + choose,
                                0, 0.1, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.CLOUD,
                                xpos + i + choose, ypos + 0.1, zpos + j + choose,
                                0, 0, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.WARPED_SPORE,
                                xpos + i + choose, ypos, zpos + j + choose,
                                0, 0.1, 0);
                    }
                }
            }
        }
    }

    //Lichblade - Soul Anguish
    public static void tickAbilitySoulAnguish(ItemStack stack, World world, Entity user, float abilityDamage, int radius,
                                              double lastX, double lastY, double lastZ, float healAmount, LivingEntity abilityTarget) {

        if (!user.getWorld().isClient() && (user instanceof PlayerEntity player) && abilityTarget != null) {

            //3D sound control
            float soundDistance = 0.2f - (float) player.squaredDistanceTo(lastX, lastY, lastZ) / 800;

            //Target tracking cloud
            if (player.age % 5 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                Box box = new Box(lastX + radius, lastY + radius, lastZ + radius,
                        lastX - radius, lastY - radius, lastZ - radius);
                for (Entity entity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {

                        //Heal effect
                        float choose = (float) (Math.random() * 1);
                        if (player.getRandom().nextInt(100) <= 8) {
                            world.playSoundFromEntity(null, le, SoundRegistry.DARK_SWORD_BREAKS.get(), le.getSoundCategory(), soundDistance, choose);
                            player.heal(healAmount);
                        }
                        le.damage(world.getDamageSources().magic(), abilityDamage);
                    }
                }
                world.playSound(null, lastX, lastY, lastZ, SoundRegistry.DARK_SWORD_BLOCK.get(),
                        player.getSoundCategory(), soundDistance, 0.3f, 100);

                double xpos = lastX - (radius + 1);
                double ypos = lastY;
                double zpos = lastZ - (radius + 1);
                world.playSound(xpos, ypos, zpos, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                        player.getSoundCategory(), 0.1f, 0.2f, true);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM,
                                xpos + i + choose, ypos, zpos + j + choose,
                                choose / 3, -0.3, choose / 3);
                        choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.SOUL,
                                xpos + i + choose, ypos, zpos + j + choose,
                                choose / 3, 0, choose / 3);
                    }
                }
            }
        }
    }

    //Icewhisper - Permafrost
    public static void tickAbilityPermafrost(ItemStack stack, World world, Entity user,
                                             int ability_timer, int ability_timer_max, float abilityDamage,
                                             int skillCooldown, int radius,
                                             double lastX, double lastY, double lastZ) {

        if (!user.getWorld().isClient() && (user instanceof PlayerEntity player)) {
            int rradius = radius * 2;
            if (ability_timer < 5) player.stopUsingItem();

            //AOE Blizzard
            if (player.age % 10 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                player.getHungerManager().addExhaustion(0.8f);
                Box box = new Box(player.getX() + rradius, player.getY() + radius, player.getZ() + rradius,
                        player.getX() - rradius, player.getY() - radius, player.getZ() - rradius);
                for (Entity entity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {
                        if (le.hasStatusEffect(StatusEffects.SLOWNESS)) {

                            int a = (le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);

                            if (a < 4) {
                                le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, a), player);
                            } else {
                                le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, a - 1), player);
                            }
                        } else {
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 0), player);
                        }
                        float choose = (float) (Math.random() * 1);
                        world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                                player.getSoundCategory(), 0.1f, choose);
                        le.damage(world.getDamageSources().magic(), abilityDamage * 3);
                    }
                }

                double xpos = lastX - (rradius + 1);
                double ypos = lastY;
                double zpos = lastZ - (rradius + 1);
                world.playSound(xpos, ypos, zpos, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                        player.getSoundCategory(), 0.1f, 0.2f, true);

                for (int i = rradius * 2; i > 0; i--) {
                    for (int j = rradius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.SNOWFLAKE,
                                xpos + i + choose, ypos + 6, zpos + j + choose,
                                choose / 3, -0.3, choose / 3);
                        choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.WHITE_ASH,
                                xpos + i + choose, ypos + 6, zpos + j + choose,
                                choose / 3, 0, choose / 3);
                    }
                }
            }
        }
    }

    //Arcanethyst - Arcane Assault
    public static void tickAbilityArcaneAssault(ItemStack stack, World world, Entity user,
                                                int ability_timer, int ability_timer_max, float abilityDamage,
                                                int skillCooldown, int radius) {

        if (!user.getWorld().isClient() && (user instanceof PlayerEntity player)) {

            if (ability_timer < 5) player.stopUsingItem();

            //AOE Lift - 1 charge
            if (player.age % 10 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                Box box = new Box(player.getX() + radius, player.getY() + radius * 2, player.getZ() + radius,
                        player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                for (Entity entity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {

                        float choose = (float) (Math.random() * 1);

                        if (!le.hasStatusEffect(StatusEffects.LEVITATION) && ability_timer > 30) {
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 20, 1), player);
                            world.playSoundFromEntity(null, le, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_03.get(),
                                    le.getSoundCategory(), 0.1f, choose);
                        }
                        le.damage(world.getDamageSources().magic(), abilityDamage);
                        if (ability_timer < 10) { //Ground Slam - 3 Charges
                            le.removeStatusEffect(StatusEffects.LEVITATION);
                            le.damage(world.getDamageSources().magic(), abilityDamage * 3);
                            le.setVelocity(0, -10, 0);
                            player.stopUsingItem();
                            world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                                    le.getSoundCategory(), 0.3f, choose);
                        }
                    }
                }
                world.playSoundFromEntity(null, player, SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(),
                        player.getSoundCategory(), 0.1f, 0.6f);
                double xpos = player.getX() - (radius + 1);
                double ypos = player.getY();
                double zpos = player.getZ() - (radius + 1);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.DRAGON_BREATH,
                                xpos + i + choose, ypos + 0.4, zpos + j + choose,
                                0, 0.1, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.PORTAL,
                                xpos + i + choose, ypos + 0.1, zpos + j + choose,
                                0, 0, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.REVERSE_PORTAL,
                                xpos + i + choose, ypos + 1, zpos + j + choose,
                                0, 0.1, 0);
                    }
                }
            }
        }
    }

    //Thunder Brand - Thunder Blitz
    public static void tickAbilityVolcanicFury(ItemStack stack, World world, Entity user,
                                               int ability_timer, int ability_timer_max, float abilityDamage,
                                               int skillCooldown, int radius, int chargePower) {
        if (!user.getWorld().isClient() && (user instanceof PlayerEntity player)) {

            if (ability_timer < 5) player.stopUsingItem();

            //AOE Damage
            if (player.age % 20 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {


                if (ability_timer > 10) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 5), player);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20, 5), player);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 5), player);
                    world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_EARTH_SHOOT_IMPACT_02.get(),
                            player.getSoundCategory(), 0.8f, 0.1f * chargePower);
                    if (player.getHealth() > 2 && !player.isCreative())
                        player.setHealth(player.getHealth() - 1);
                }

                Box box = new Box(player.getX() + radius * 8, player.getY() + radius, player.getZ() + radius * 8,
                        player.getX() - radius * 8, player.getY() - radius, player.getZ() - radius * 8);
                for (Entity entity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {

                        if (ability_timer > 12) {
                            le.damage(world.getDamageSources().magic(), abilityDamage);
                            le.setVelocity((player.getX() - le.getX()) / 10, (player.getY() - le.getY()) / 10, (player.getZ() - le.getZ()) / 10);
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3), player);
                        }
                    }
                }
                double xpos = player.getX() - (radius + 1);
                double ypos = player.getY();
                double zpos = player.getZ() - (radius + 1);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.WARPED_SPORE,
                                xpos + i + choose, ypos + 0.4, zpos + j + choose,
                                0, 0.1, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                xpos + i + choose, ypos + 0.1, zpos + j + choose,
                                0, 0, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.LAVA,
                                xpos + i + choose, ypos, zpos + j + choose,
                                0, 0.1, 0);
                    }
                }
            }
        }
    }
    //Next ability
}