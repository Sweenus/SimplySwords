package net.sweenus.simplyswords.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

public class RunicMethods {

    // ------- POSTHIT ------- //

    // Runic Power - FREEZE
    public static void postHitRunicFreeze(LivingEntity target, LivingEntity attacker) {

        int hitChance = (int) Config.getFloat("freezeChance", "RunicEffects", ConfigDefaultValues.freezeChance);
        int freezeDuration = (int) Config.getFloat("freezeDuration", "RunicEffects", ConfigDefaultValues.freezeDuration);
        int duration = freezeDuration * 3;

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1), attacker);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), freezeDuration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - WILDFIRE
    public static void postHitRunicWildfire(LivingEntity target, LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("wildfireChance", "RunicEffects", ConfigDefaultValues.wildfireChance);
        int duration = (int) Config.getFloat("wildfireDuration", "RunicEffects", ConfigDefaultValues.wildfireDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.addStatusEffect(new StatusEffectInstance(EffectRegistry.WILDFIRE.get(), duration, 3), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - SLOW
    public static void postHitRunicSlow(LivingEntity target, LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("slowChance", "RunicEffects", ConfigDefaultValues.slowChance);
        int duration = (int) Config.getFloat("slowDuration", "RunicEffects", ConfigDefaultValues.slowDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1), attacker);
        }
    }

    // Runic Power - GREATER SLOW
    public static void postHitRunicGreaterSlow(LivingEntity target, LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("slowChance", "RunicEffects", ConfigDefaultValues.slowChance);
        int duration = (int) Config.getFloat("slowDuration", "RunicEffects", ConfigDefaultValues.slowDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 2), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - SWIFTNESS
    public static void postHitRunicSwiftness(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("swiftnessChance", "RunicEffects", ConfigDefaultValues.swiftnessChance);
        int duration = (int) Config.getFloat("swiftnessDuration", "RunicEffects", ConfigDefaultValues.swiftnessDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 0), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER SWIFTNESS
    public static void postHitRunicGreaterSwiftness(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("swiftnessChance", "RunicEffects", ConfigDefaultValues.swiftnessChance);
        int duration = (int) Config.getFloat("swiftnessDuration", "RunicEffects", ConfigDefaultValues.swiftnessDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - FLOAT
    public static void postHitRunicFloat(LivingEntity target, LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("floatChance", "RunicEffects", ConfigDefaultValues.floatChance);
        int duration = (int) Config.getFloat("floatDuration", "RunicEffects", ConfigDefaultValues.floatDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, duration, 2), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER FLOAT
    public static void postHitRunicGreaterFloat(LivingEntity target, LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("floatChance", "RunicEffects", ConfigDefaultValues.floatChance);
        int duration = (int) Config.getFloat("floatDuration", "RunicEffects", ConfigDefaultValues.floatDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, duration, 3), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - ZEPHYR
    public static void postHitRunicZephyr(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("zephyrChance", "RunicEffects", ConfigDefaultValues.zephyrChance);
        int duration = (int) Config.getFloat("zephyrDuration", "RunicEffects", ConfigDefaultValues.zephyrDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 0), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 0), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER ZEPHYR
    public static void postHitRunicGreaterZephyr(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("zephyrChance", "RunicEffects", ConfigDefaultValues.zephyrChance);
        int duration = (int) Config.getFloat("zephyrDuration", "RunicEffects", ConfigDefaultValues.zephyrDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 1), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - SHIELDING
    public static void postHitRunicShielding(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("shieldingChance", "RunicEffects", ConfigDefaultValues.shieldingChance);
        int duration = (int) Config.getFloat("shieldingDuration", "RunicEffects", ConfigDefaultValues.shieldingDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, 0), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER SHIELDING
    public static void postHitRunicGreaterShielding(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("shieldingChance", "RunicEffects", ConfigDefaultValues.shieldingChance);
        int duration = (int) Config.getFloat("shieldingDuration", "RunicEffects", ConfigDefaultValues.shieldingDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - STONESKIN
    public static void postHitRunicStoneskin(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("stoneskinChance", "RunicEffects", ConfigDefaultValues.stoneskinChance);
        int duration = (int) Config.getFloat("stoneskinDuration", "RunicEffects", ConfigDefaultValues.stoneskinDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 0), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_02.get(),
                    attacker.getSoundCategory(), 0.3f, 1.3f);
        }
    }

    // Runic Power - GREATER STONESKIN
    public static void postHitRunicGreaterStoneskin(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("stoneskinChance", "RunicEffects", ConfigDefaultValues.stoneskinChance);
        int duration = (int) Config.getFloat("stoneskinDuration", "RunicEffects", ConfigDefaultValues.stoneskinDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 2), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_02.get(),
                    attacker.getSoundCategory(), 0.3f, 1.1f);
        }
    }

    // Runic Power - TRAILBLAZE
    public static void postHitRunicTrailblaze(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("trailblazeChance", "RunicEffects", ConfigDefaultValues.trailblazeChance);
        int duration = (int) Config.getFloat("trailblazeDuration", "RunicEffects", ConfigDefaultValues.trailblazeDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1), attacker);
            attacker.setOnFireFor(duration / 20);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER TRAILBLAZE
    public static void postHitRunicGreaterTrailblaze(LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("trailblazeChance", "RunicEffects", ConfigDefaultValues.trailblazeChance);
        int duration = (int) Config.getFloat("trailblazeDuration", "RunicEffects", ConfigDefaultValues.trailblazeDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 2), attacker);
            attacker.setOnFireFor(duration / 20);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - WEAKNESS
    public static void postHitRunicWeaken(LivingEntity target, LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("weakenChance", "RunicEffects", ConfigDefaultValues.weakenChance);
        int duration = (int) Config.getFloat("weakenDuration", "RunicEffects", ConfigDefaultValues.weakenDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0), attacker);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER WEAKNESS
    public static void postHitRunicGreaterWeaken(LivingEntity target, LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("weakenChance", "RunicEffects", ConfigDefaultValues.weakenChance);
        int duration = (int) Config.getFloat("weakenDuration", "RunicEffects", ConfigDefaultValues.weakenDuration);

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 1), attacker);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 2), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.1f, 1.8f);
        }
    }

    // Runic Power - IMBUED
    public static void postHitRunicImbued(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("imbuedChance", "RunicEffects", ConfigDefaultValues.imbuedChance);
        int damage = 6 - ((stack.getDamage() / stack.getMaxDamage()) * 100) / 20;

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.timeUntilRegen = 0;
            target.damage(attacker.getDamageSources().magic(), damage);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.2f, 1.8f);
        }
    }

    // Runic Power - GREATER IMBUED
    public static void postHitRunicGreaterImbued(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int hitChance = (int) Config.getFloat("imbuedChance", "RunicEffects", ConfigDefaultValues.imbuedChance);
        int damage = 10 - ((stack.getDamage() / stack.getMaxDamage()) * 100) / 10;

        if (attacker.getRandom().nextInt(100) <= hitChance) {
            target.timeUntilRegen = 0;
            target.damage(attacker.getDamageSources().magic(), damage);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.2f, 1.8f);
        }
    }

    // Runic Power - PinCushion
    public static void postHitRunicPinCushion(LivingEntity target, LivingEntity attacker) {
        int stuckArrows = attacker.getStuckArrowCount();
        target.damage(attacker.getDamageSources().generic(), stuckArrows);
        attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                attacker.getSoundCategory(), 0.1f, 1.8f);
    }

    // Runic Power - Greater PinCushion
    public static void postHitRunicGreaterPinCushion(LivingEntity target, LivingEntity attacker) {
        int stuckArrows = attacker.getStuckArrowCount();
        target.damage(attacker.getDamageSources().generic(), stuckArrows * 2);
        attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                attacker.getSoundCategory(), 0.1f, 1.8f);
    }

    // Nether Power - ECHO
    public static void postHitNetherEcho(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int amp = 0;
        //increase damage if 2H wep
        if (HelperMethods.isUniqueTwohanded(stack))
            amp = 2;
        target.addStatusEffect(new StatusEffectInstance(EffectRegistry.ECHO.get(), 20, amp), attacker);
    }

    // Nether Power - BERSERK
    public static void postHitNetherBerserk(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int amp = 2;
        //increase damage if 2H wep
        if (HelperMethods.isUniqueTwohanded(stack))
            amp = 4;
        if (attacker.getArmor() < 10) {
            target.setHealth(target.getHealth() - amp);
            attacker.heal((float) amp / 2);
        }
    }

    // Nether Power - RADIANCE
    public static void postHitNetherRadiance(LivingEntity target, LivingEntity attacker) {
        if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {
            attacker.addStatusEffect(new StatusEffectInstance(EffectRegistry.IMMOLATION.get(), 100, 0), attacker);
        }
    }

    // Nether Power - ONSLAUGHT
    public static void postHitNetherOnslaught(LivingEntity target, LivingEntity attacker) {
        if (target.hasStatusEffect(StatusEffects.SLOWNESS) && !attacker.hasStatusEffect(StatusEffects.WEAKNESS)) {
            attacker.addStatusEffect(new StatusEffectInstance(EffectRegistry.ONSLAUGHT.get(), 80, 0), attacker);
        }
    }

    // Nether Power - NULLIFICATION
    public static void postHitNetherNullification(LivingEntity user) {
        if (!user.hasStatusEffect(EffectRegistry.BATTLE_FATIGUE.get())) {
            if (!user.getWorld().isClient()) {
                ServerWorld serverWorld = (ServerWorld) user.getWorld();
                BlockState currentState = serverWorld.getBlockState(user.getBlockPos().up(4).offset(user.getMovementDirection(), 3));
                BlockState state = Blocks.AIR.getDefaultState();
                if (currentState == state) {
                    serverWorld.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
                            user.getSoundCategory(), 0.4f, 0.8f);
                    BattleStandardEntity banner = EntityRegistry.BATTLESTANDARD.get().spawn(
                            serverWorld,
                            user.getBlockPos().up(4).offset(user.getMovementDirection(), 3),
                            SpawnReason.MOB_SUMMONED);
                    if (banner != null) {
                        banner.setVelocity(0, -1, 0);
                        banner.ownerEntity = user;
                        banner.decayRate = 3;
                        banner.standardType = "nullification";
                        banner.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name", user.getName()));
                    }
                    user.addStatusEffect(new StatusEffectInstance(EffectRegistry.BATTLE_FATIGUE.get(), 800, 0), user);
                }
            }
        }
    }


    // Runic Power - EMPTY
    public static void postHitRunicEmpty(ItemStack stack, LivingEntity target, LivingEntity attacker) {


    }

    // ------- ON STOPPED USING ------- //

    public static void stoppedUsingRunicMomentum(ItemStack stack, LivingEntity user) {
        //Player dash end
        if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            user.setVelocity(0, 0, 0); // Stop player at end of charge
            user.velocityModified = true;
        }
    }


    // ------- USAGE TICK ------- //


    //Runic Power - MOMENTUM
    public static void usageTickRunicMomentum(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        int skillCooldown = (int) Config.getFloat("momentumCooldown", "RunicEffects", ConfigDefaultValues.momentumCooldown);
        if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && user.isOnGround()) {
            //Player dash forward
            if (remainingUseTicks == 12 || remainingUseTicks == 13 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                user.setVelocity(user.getRotationVector().multiply(+3));
                user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent player flying to the heavens
                user.velocityModified = true;
                if (user instanceof PlayerEntity player) {
                    player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
                }
            }
        }
    }

    //Runic Power - GREATER MOMENTUM
    public static void usageTickRunicGreaterMomentum(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        int skillCooldown = (int) Config.getFloat("momentumCooldown", "RunicEffects", ConfigDefaultValues.momentumCooldown);
        if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && user.isOnGround()) {
            //Player dash forward
            if (remainingUseTicks == 10 || remainingUseTicks == 13 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                user.setVelocity(user.getRotationVector().multiply(+3));
                user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent player flying to the heavens
                user.velocityModified = true;
                if (user instanceof PlayerEntity player) {
                    player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
                }
            }
        }
    }

    // ------- INVENTORY TICK ------- //


    // Runic Power - UNSTABLE
    public static void inventoryTickRunicUnstable(LivingEntity user) {
        int duration = (int) Config.getFloat("unstableDuration", "RunicEffects", ConfigDefaultValues.unstableDuration);
        int frequency = (int) Config.getFloat("unstableFrequency", "RunicEffects", ConfigDefaultValues.unstableFrequency);
        if (user.age % frequency == 0) {
            int random = (int) (Math.random() * 100);
            if (random < 10)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration));
            else if (random < 20)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration));
            else if (random < 30)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration));
            else if (random < 40)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, duration));
            else if (random < 50)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration));
            else if (random < 60)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration));
            else if (random < 70)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration));
            else if (random < 80)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration));
            else if (random < 90)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration));
            else if (random < 95)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, duration));
            else if (random < 100)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, duration));
        }
    }

    // Runic Power - ACTIVE DEFENCE
    public static void inventoryTickRunicActiveDefence(World world, LivingEntity user) {
        if (!world.isClient() && user instanceof PlayerEntity player && player.getInventory().contains(Items.ARROW.getDefaultStack())) {
            int frequency = (int) Config.getFloat("activeDefenceFrequency", "RunicEffects", ConfigDefaultValues.activeDefenceFrequency);
            if (player.age % frequency == 0) {
                int sradius = (int) Config.getFloat("activeDefenceRadius", "RunicEffects", ConfigDefaultValues.activeDefenceRadius);
                int vradius = (int) (Config.getFloat("activeDefenceRadius", "RunicEffects", ConfigDefaultValues.activeDefenceRadius) / 2);
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                Box box = new Box(x + sradius, y + vradius, z + sradius, x - sradius, y - vradius, z - sradius);
                for (Entity entity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if (entity instanceof LivingEntity le && HelperMethods.checkFriendlyFire(le, player)) {

                        int arrowSlot = player.getInventory().getSlotWithStack(Items.ARROW.getDefaultStack());
                        ItemStack arrowStack = player.getInventory().getStack(arrowSlot);
                        int randomc = (int) (Math.random() * 100);
                        if (randomc < 15) {
                            arrowStack.decrement(1);
                        }
                        ArrowEntity arrow = new ArrowEntity(EntityType.ARROW, world);
                        arrow.updatePosition(player.getX(), (player.getY() + 1.5), player.getZ());
                        arrow.setOwner(player);
                        arrow.setVelocity(le.getX() - player.getX(), (le.getY() - player.getY()) - 1, le.getZ() - player.getZ());
                        world.spawnEntity(arrow);
                        break;
                    }
                }
            }
        }
    }

    // Runic Power - FROST WARD
    public static void inventoryTickRunicFrostWard(World world, LivingEntity user) {
        int frequency = (int) Config.getFloat("frostWardFrequency", "RunicEffects", ConfigDefaultValues.frostWardFrequency);
        int duration = (int) Config.getFloat("frostWardDuration", "RunicEffects", ConfigDefaultValues.frostWardDuration);
        if (user.age % frequency == 0) {
            int sradius = (int) Config.getFloat("frostWardRadius", "RunicEffects", ConfigDefaultValues.frostWardRadius);
            int vradius = (int) (Config.getFloat("frostWardRadius", "RunicEffects", ConfigDefaultValues.frostWardRadius) / 2);
            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld serverWorld = (ServerWorld) world;
            Box box = new Box(x + sradius, y + vradius, z + sradius, x - sradius, y - vradius, z - sradius);
            for (Entity entity : serverWorld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                if (entity instanceof LivingEntity le && HelperMethods.checkFriendlyFire(le, user)) {

                    if (le.distanceTo(user) < sradius) {
                        SnowballEntity snowball = new SnowballEntity(EntityType.SNOWBALL, serverWorld);
                        snowball.updatePosition(user.getX(), (user.getY() + 1.5), user.getZ());
                        snowball.setOwner(user);
                        le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration));
                        snowball.setVelocity(le.getX() - user.getX(), (le.getY() - user.getY()) - 1, le.getZ() - user.getZ());
                        serverWorld.spawnEntity(snowball);
                    }
                }
            }
        }
    }
}