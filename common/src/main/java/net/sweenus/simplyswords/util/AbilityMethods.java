package net.sweenus.simplyswords.util;

import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.List;

public class AbilityMethods {

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
                    ComponentTypeRegistry.STORED_CHARGE.apply(stack, StoredChargeComponent.DEFAULT, StoredChargeComponent::increment);
                    DamageSource damageSource = user.getDamageSources().indirectMagic(user, user);
                    float damage = world instanceof ServerWorld serverWorld
                            ? HelperMethods.applyAbilityDamageEnchantments(serverWorld, stack, le, damageSource, abilityDamage)
                            : abilityDamage;
                    le.damage(damageSource, damage);
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
