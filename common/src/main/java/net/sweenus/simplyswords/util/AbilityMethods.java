package net.sweenus.simplyswords.util;

import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.List;
import java.util.Random;

public class AbilityMethods {

    //Storm's Edge - Storm Jolt
    public static void tickAbilityStormJolt(ItemStack stack, World world, LivingEntity user,
                                            int ability_timer, int skillCooldown, int radius) {
        if (!user.getWorld().isClient()) {

            //Player dash forward
            if (ability_timer == 12 || ability_timer == 13 && HelperMethods.isHolding(stack, user)) {
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
            if (ability_timer < 5 && HelperMethods.isHolding(stack, user)) {
                user.setVelocity(0, 0, 0); // Stop user at end of charge
                user.velocityModified = true;
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1), user);

            }

            if (user.age % 2 == 0 && HelperMethods.isHolding(stack, user)) {
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
            int frequency = Config.uniqueEffects.mjolnir.frequency;
            if (user.age % frequency == 0) {
                double x = user.getX();
                double y = user.getY();
                double z = user.getZ();
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, frequency+5, 5), user);
                Box box = new Box(x + radius, y + radius, z + radius, x - radius, y - radius, z - radius);
                ServerWorld sworld = (ServerWorld) user.getWorld();

                for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    float choose = (float) (Math.random() * 1);
                    if ((entity instanceof LivingEntity ee)) {
                        if (HelperMethods.checkFriendlyFire(ee, user) && choose > 0.7) {
                            var stormtarget = ee.getBlockPos();
                            ee.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FREEZE), frequency+5, 0), user);
                            LightningEntity storm = EntityType.LIGHTNING_BOLT.spawn(sworld, stormtarget, SpawnReason.TRIGGERED);
                            if (storm != null) {
                                storm.setCosmetic(true);
                            }
                            ee.damage(user.getDamageSources().indirectMagic(user, user), 5);
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
            if (user.age % 3 == 0 && HelperMethods.isHolding(stack, user)) {
                Box box = new Box(user.getX() + radius, user.getY() + radius * 2, user.getZ() + radius,
                        user.getX() - radius, user.getY() - radius, user.getZ() - radius);
                for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {

                        float choose = (float) (Math.random() * 1);

                        if (ability_timer > (ability_timer_max - 40)) {
                            le.damage(world.getDamageSources().indirectMagic(user, user), abilityDamage);
                            world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_02.get(),
                                    le.getSoundCategory(), 0.1f, choose);
                        } else if (ability_timer < (ability_timer_max - 40)) {
                            le.damage(world.getDamageSources().indirectMagic(user, user), abilityDamage * 3);
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
            if (user.age % 5 != 0 || user.getEquippedStack(EquipmentSlot.MAINHAND) != stack) return;
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
                    stack.apply(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT, StoredChargeComponent::increment);
                    le.damage(user.getDamageSources().indirectMagic(user, user), abilityDamage);
                }
            }
            world.playSound(null, lastX, lastY, lastZ, SoundRegistry.DARK_SWORD_BLOCK.get(),
                    user.getSoundCategory(), soundDistance, 0.3f, 100);

            double xPos = lastX - (radius + 1);
            double yPos = lastY;
            double zPos = lastZ - (radius + 1);
            world.playSound(xPos, yPos, zPos, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                    user.getSoundCategory(), 0.1f, 0.2f, true);

            for (int i = radius * 2; i > 0; i--) {
                for (int j = radius * 2; j > 0; j--) {
                    float choose = (float) (Math.random() * 1);
                    HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM,
                            xPos + i + choose, yPos, zPos + j + choose,
                            choose / 3, -0.3, choose / 3);
                    choose = (float) (Math.random() * 1);
                    HelperMethods.spawnParticle(world, ParticleTypes.SOUL,
                            xPos + i + choose, yPos, zPos + j + choose,
                            choose / 3, 0, choose / 3);
                }
            }

        }
    }

    //Hearthflame - Volcanic Fury
    public static void tickAbilityVolcanicFury(ItemStack stack, World world, LivingEntity user,
                                               int ability_timer, int ability_timer_max, float abilityDamage,
                                               int skillCooldown, int radius, int chargePower) {
        if (!user.getWorld().isClient()) {

            if (ability_timer < 5) user.stopUsingItem();

            //AOE Damage
            if (user.age % 20 == 0 && HelperMethods.isHolding(stack, user)) {

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
                            le.damage(world.getDamageSources().indirectMagic(user, user), abilityDamage);
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

    public static void astralShiftSounds(ServerPlayerEntity serverPlayer) {
        SoundEvent[] soundOptions = new SoundEvent[]{
                SoundRegistry.DISTORTION_ARC_01.get(),
                SoundRegistry.DISTORTION_ARC_02.get(),
                SoundRegistry.DISTORTION_ARC_03.get()
        };

        Random random = new Random();
        SoundEvent soundRandom = soundOptions[random.nextInt(soundOptions.length)];

        serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, soundRandom,
                SoundCategory.PLAYERS, 0.7f, 0.5f + (serverPlayer.getRandom().nextBetween(1, 5) * 0.1f));
    }

    public static boolean astralShiftPassive(ServerPlayerEntity serverPlayer) {
        return (serverPlayer.getRandom().nextInt(100) < Config.uniqueEffects.caelestis.chance);
    }


    public static void applyAxolotlBuff(ServerPlayerEntity player, NbtCompound axolotlDataLeft, NbtCompound axolotlDataRight) {
        boolean hasLeftAxolotl = axolotlDataLeft != null && axolotlDataLeft.contains("Variant");
        boolean hasRightAxolotl = axolotlDataRight != null && axolotlDataRight.contains("Variant");

        if (!hasLeftAxolotl && !hasRightAxolotl) {
            return;
        }

        if (player.age % 40 == 0) {
            Box area = new Box(
                    player.getX() - 5, player.getY() - 3, player.getZ() - 5,
                    player.getX() + 5, player.getY() + 3, player.getZ() + 5
            );

            int leftVariant = hasLeftAxolotl ? axolotlDataLeft.getInt("Variant") : -1;
            int rightVariant = hasRightAxolotl ? axolotlDataRight.getInt("Variant") : -1;

            boolean areVariantsMatching = hasLeftAxolotl && hasRightAxolotl && leftVariant == rightVariant;

            // Increase amplifier by 1 if both variants match
            int amplifierBoost = areVariantsMatching ? 1 : 0;

            StatusEffectInstance leftPrimaryEffect = hasLeftAxolotl ? switch (leftVariant) {
                case 0 ->
                        new StatusEffectInstance(StatusEffects.REGENERATION, 80, amplifierBoost, false, false, true);  // Lucy
                case 1 ->
                        new StatusEffectInstance(StatusEffects.NIGHT_VISION, 80, amplifierBoost, false, false, true); // Wild
                case 2 ->
                        new StatusEffectInstance(StatusEffects.RESISTANCE, 50, amplifierBoost, false, false, true);   // Gold
                case 3 ->
                        new StatusEffectInstance(StatusEffects.STRENGTH, 50, amplifierBoost, false, false, true);     // Cyan
                case 4 ->
                        new StatusEffectInstance(StatusEffects.SPEED, 50, amplifierBoost, false, false, true);            // Blue
                default -> null;
            } : null;

            StatusEffectInstance leftSecondaryEffect = (hasLeftAxolotl && leftVariant == 4)
                    ? new StatusEffectInstance(StatusEffects.LUCK, 50, amplifierBoost, false, false, true)
                    : null;

            StatusEffectInstance rightPrimaryEffect = hasRightAxolotl ? switch (rightVariant) {
                case 0 ->
                        new StatusEffectInstance(StatusEffects.REGENERATION, 80, amplifierBoost, false, false, true);  // Lucy
                case 1 ->
                        new StatusEffectInstance(StatusEffects.NIGHT_VISION, 80, amplifierBoost, false, false, true); // Wild
                case 2 ->
                        new StatusEffectInstance(StatusEffects.RESISTANCE, 50, amplifierBoost, false, false, true);   // Gold
                case 3 ->
                        new StatusEffectInstance(StatusEffects.STRENGTH, 50, amplifierBoost, false, false, true);     // Cyan
                case 4 ->
                        new StatusEffectInstance(StatusEffects.SPEED, 50, amplifierBoost, false, false, true);            // Blue
                default -> null;
            } : null;

            StatusEffectInstance rightSecondaryEffect = (hasRightAxolotl && rightVariant == 4)
                    ? new StatusEffectInstance(StatusEffects.LUCK, 50, amplifierBoost, false, false, true)
                    : null;

            List<PlayerEntity> entities = player.getWorld().getEntitiesByClass(
                    PlayerEntity.class,
                    area,
                    entity -> true
            );

            for (PlayerEntity entity : entities) {
                if (leftPrimaryEffect != null) {
                    entity.addStatusEffect(new StatusEffectInstance(leftPrimaryEffect));
                }
                if (leftSecondaryEffect != null) {
                    entity.addStatusEffect(new StatusEffectInstance(leftSecondaryEffect));
                }
                if (rightPrimaryEffect != null) {
                    entity.addStatusEffect(new StatusEffectInstance(rightPrimaryEffect));
                }
                if (rightSecondaryEffect != null) {
                    entity.addStatusEffect(new StatusEffectInstance(rightSecondaryEffect));
                }
            }
        }

        // Particles
        int frequency = player.getRandom().nextInt(10);
        World world = player.getWorld();
        if (world instanceof ServerWorld serverWorld) {
        if (player.age % 8+frequency == 0) {
                // Left shoulder particles
                if (hasLeftAxolotl) {
                    serverWorld.spawnParticles(
                            ParticleTypes.FALLING_DRIPSTONE_WATER,
                            player.getX() - 0.3, player.getY() + 1.3, player.getZ(),
                            5,
                            0.1, 0.1, 0.1,
                            0.03
                    );
                }
            }
            frequency = player.getRandom().nextInt(10);
            if (player.age % 10+frequency == 0) {
                // Right shoulder particles
                if (hasRightAxolotl) {
                    serverWorld.spawnParticles(
                            ParticleTypes.FALLING_DRIPSTONE_WATER,
                            player.getX() + 0.3, player.getY() + 1.3, player.getZ(),
                            5,
                            0.1, 0.1, 0.1,
                            0.03
                    );
                }
            }

            // Sounds
            frequency = player.getRandom().nextInt(30);
            if (player.age % 10+frequency == 0) {
                world.playSoundFromEntity(null, player, SoundEvents.ENTITY_AXOLOTL_IDLE_AIR,
                        player.getSoundCategory(), 1.0f, 1.0f);
            }



        }



    }





}
