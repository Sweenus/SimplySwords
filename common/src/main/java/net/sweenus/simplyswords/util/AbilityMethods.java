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
import net.sweenus.simplyswords.item.custom.LichbladeSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

public class AbilityMethods {

    //Storm's Edge - Storm Jolt
    public static void tickAbilityStormJolt(ItemStack stack, World world, LivingEntity user,
                                            int ability_timer, int skillCooldown, int radius) {
        if (!user.getWorld().isClient()) {

            //Player dash forward
            if (ability_timer == 12 || ability_timer == 13 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                user.setVelocity(user.getRotationVector().multiply(+4));
                user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent user flying to the heavens
                user.velocityModified = true;
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_03.get(),
                        user.getSoundCategory(), 0.3f, 1.6f);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 1), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 10, 5), user);
                if (user instanceof PlayerEntity player) player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
            }

            //Player dash end
            if (ability_timer < 5 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                user.setVelocity(0, 0, 0); // Stop user at end of charge
                user.velocityModified = true;
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1), user);

            }

            if (user.age % 2 == 0 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                double xpos = user.getX() - (radius + 1);
                double ypos = user.getY();
                double zpos = user.getZ() - (radius + 1);

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
    public static void tickAbilityStorm(ItemStack stack, World world, LivingEntity user,
                                        int ability_timer, int skillCooldown, int radius) {
        if (!user.getWorld().isClient()) {
            if (user.age % 10 == 0) {
                double x = user.getX();
                double y = user.getY();
                double z = user.getZ();
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 15, 5), user);
                Box box = new Box(x + radius, y + radius, z + radius, x - radius, y - radius, z - radius);
                ServerWorld sworld = (ServerWorld) user.getWorld();

                for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    float choose = (float) (Math.random() * 1);
                    if ((entity instanceof LivingEntity ee)) {
                        if (HelperMethods.checkFriendlyFire(ee, user) && choose > 0.7) {
                            var stormtarget = ee.getBlockPos();
                            ee.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), 10, 0), user);
                            LightningEntity storm = EntityType.LIGHTNING_BOLT.spawn(sworld, stormtarget, SpawnReason.TRIGGERED);
                            if (storm != null) {
                                storm.setCosmetic(true);
                            }
                            ee.damage(user.getDamageSources().magic(), 5);
                        }
                    }
                }
            }
            if (user.age % 5 == 0) {
                double xpos = user.getX() - (radius + 1);
                double ypos = user.getY();
                double zpos = user.getZ() - (radius + 1);

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
    public static void tickAbilityThunderBlitz(ItemStack stack, World world, LivingEntity user, int ability_timer,
                                               int ability_timer_max, float abilityDamage, int skillCooldown, int radius) {
        if (!user.getWorld().isClient()) {

            //Player dash control
            if (ability_timer > (ability_timer_max - 42) && ability_timer < (ability_timer_max - 40)) {
                user.setVelocity(user.getRotationVector().multiply(+6));
                user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent user flying to the heavens
                user.velocityModified = true;
                if (user instanceof PlayerEntity player) {
                    player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
                }
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(),
                        user.getSoundCategory(), 0.3f, 1.6f);
            }
            //Player dash end
            if (ability_timer < 5) {
                user.setVelocity(0, 0, 0); // Stop user at end of charge
                user.velocityModified = true;
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 2), user);

            }

            //AOE Damage & charge control
            if (user.age % 3 == 0 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                Box box = new Box(user.getX() + radius, user.getY() + radius * 2, user.getZ() + radius,
                        user.getX() - radius, user.getY() - radius, user.getZ() - radius);
                for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {

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
                double xpos = user.getX() - (radius + 1);
                double ypos = user.getY();
                double zpos = user.getZ() - (radius + 1);

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
    public static void tickAbilitySoulAnguish(ItemStack stack, World world, LivingEntity user, float abilityDamage, int radius,
                                              double lastX, double lastY, double lastZ, float healAmount, LivingEntity abilityTarget) {
        if (!user.getWorld().isClient() && abilityTarget != null) {

            //3D sound control
            float soundDistance = 0.2f - (float) user.squaredDistanceTo(lastX, lastY, lastZ) / 800;

            //Target tracking cloud
            if (user.age % 5 == 0 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                Box box = new Box(lastX + radius, lastY + radius, lastZ + radius,
                        lastX - radius, lastY - radius, lastZ - radius);
                for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire((LivingEntity) entity, user)) {

                        //Heal effect
                        float choose = (float) (Math.random() * 1);
                        if (user.getRandom().nextInt(100) <= 8) {
                            world.playSoundFromEntity(null, le, SoundRegistry.DARK_SWORD_BREAKS.get(),
                                    le.getSoundCategory(), soundDistance, choose);
                            user.heal(healAmount);
                        }
                        ((LichbladeSwordItem)stack.getItem()).damageTracker++;
                        le.damage(user.getDamageSources().magic(), abilityDamage);
                    }
                }
                world.playSound(null, lastX, lastY, lastZ, SoundRegistry.DARK_SWORD_BLOCK.get(),
                        user.getSoundCategory(), soundDistance, 0.3f, 100);

                double xpos = lastX - (radius + 1);
                double ypos = lastY;
                double zpos = lastZ - (radius + 1);
                world.playSound(xpos, ypos, zpos, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                        user.getSoundCategory(), 0.1f, 0.2f, true);

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
    public static void tickAbilityPermafrost(ItemStack stack, World world, LivingEntity user,
                                             int ability_timer, int ability_timer_max, float abilityDamage,
                                             int skillCooldown, int radius,
                                             double lastX, double lastY, double lastZ) {
        if (!user.getWorld().isClient()) {
            int rradius = radius * 2;
            if (ability_timer < 5) user.stopUsingItem();

            //AOE Blizzard
            if (user.age % 10 == 0 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                if (user instanceof PlayerEntity player) {
                    player.getHungerManager().addExhaustion(0.8f);
                }
                Box box = new Box(user.getX() + rradius, user.getY() + radius, user.getZ() + rradius,
                        user.getX() - rradius, user.getY() - radius, user.getZ() - rradius);
                for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                        if (le.hasStatusEffect(StatusEffects.SLOWNESS)) {

                            int a = (le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);

                            if (a < 4) {
                                le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, a), user);
                            } else {
                                le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, a - 1), user);
                            }
                        } else {
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 0), user);
                        }
                        float choose = (float) (Math.random() * 1);
                        world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                                user.getSoundCategory(), 0.1f, choose);
                        le.damage(world.getDamageSources().magic(), abilityDamage * 3);
                    }
                }

                double xpos = lastX - (rradius + 1);
                double ypos = lastY;
                double zpos = lastZ - (rradius + 1);
                world.playSound(xpos, ypos, zpos, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                        user.getSoundCategory(), 0.1f, 0.2f, true);

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
    public static void tickAbilityArcaneAssault(ItemStack stack, World world, LivingEntity user,
                                                int ability_timer, int ability_timer_max, float abilityDamage,
                                                int skillCooldown, int radius) {
        if (!user.getWorld().isClient()) {

            if (ability_timer < 5) user.stopUsingItem();

            //AOE Lift - 1 charge
            if (user.age % 10 == 0 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                Box box = new Box(user.getX() + radius, user.getY() + radius * 2, user.getZ() + radius,
                        user.getX() - radius, user.getY() - radius, user.getZ() - radius);
                for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {

                        float choose = (float) (Math.random() * 1);

                        if (!le.hasStatusEffect(StatusEffects.LEVITATION) && ability_timer > 30) {
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 20, 1), user);
                            world.playSoundFromEntity(null, le, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_03.get(),
                                    le.getSoundCategory(), 0.1f, choose);
                        }
                        le.damage(world.getDamageSources().magic(), abilityDamage);
                        if (ability_timer < 10) { //Ground Slam - 3 Charges
                            le.removeStatusEffect(StatusEffects.LEVITATION);
                            le.damage(world.getDamageSources().magic(), abilityDamage * 3);
                            le.setVelocity(0, -10, 0);
                            user.stopUsingItem();
                            world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                                    le.getSoundCategory(), 0.3f, choose);
                        }
                    }
                }
                world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(),
                        user.getSoundCategory(), 0.1f, 0.6f);
                double xpos = user.getX() - (radius + 1);
                double ypos = user.getY();
                double zpos = user.getZ() - (radius + 1);

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
    public static void tickAbilityVolcanicFury(ItemStack stack, World world, LivingEntity user,
                                               int ability_timer, int ability_timer_max, float abilityDamage,
                                               int skillCooldown, int radius, int chargePower) {
        if (!user.getWorld().isClient()) {

            if (ability_timer < 5) user.stopUsingItem();

            //AOE Damage
            if (user.age % 20 == 0 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {

                if (ability_timer > 10) {
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 5), user);
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20, 5), user);
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 5), user);
                    world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_EARTH_SHOOT_IMPACT_02.get(),
                            user.getSoundCategory(), 0.8f, 0.1f * chargePower);
                    if (user.getHealth() > 2 && (!(user instanceof PlayerEntity player) || !player.isCreative()))
                        user.setHealth(user.getHealth() - 1);
                }

                Box box = new Box(user.getX() + radius * 8, user.getY() + radius, user.getZ() + radius * 8,
                        user.getX() - radius * 8, user.getY() - radius, user.getZ() - radius * 8);
                for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {

                        if (ability_timer > 12) {
                            le.damage(world.getDamageSources().magic(), abilityDamage);
                            le.setVelocity((user.getX() - le.getX()) / 10, (user.getY() - le.getY()) / 10, (user.getZ() - le.getZ()) / 10);
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3), user);
                        }
                    }
                }
                double xpos = user.getX() - (radius + 1);
                double ypos = user.getY();
                double zpos = user.getZ() - (radius + 1);

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