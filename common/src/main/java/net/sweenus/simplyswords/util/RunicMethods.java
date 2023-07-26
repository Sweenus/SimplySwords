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
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

public class RunicMethods {


    // ------- POSTHIT ------- //

    // Runic Power - FREEZE
    public static void postHitRunicFreeze(ItemStack stack,  LivingEntity target, LivingEntity attacker) {

        int fhitchance = (int) SimplySwords.runicEffectsConfig.freezeChance;
        int fduration = (int) SimplySwords.runicEffectsConfig.freezeDuration;
        int sduration = fduration * 3;

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 1), attacker);

        if (attacker.getRandom().nextInt(100) <= fhitchance) {
            target.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), fduration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - WILDFIRE
    public static void postHitRunicWildfire(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int phitchance = (int) SimplySwords.runicEffectsConfig.wildfireChance;
        int pduration = (int) SimplySwords.runicEffectsConfig.wildfireDuration;

        if (attacker.getRandom().nextInt(100) <= phitchance) {
            target.addStatusEffect(new StatusEffectInstance(EffectRegistry.WILDFIRE.get(), pduration, 3), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - SLOW
    public static void postHitRunicSlow(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int shitchance = (int) SimplySwords.runicEffectsConfig.slowChance;
        int sduration = (int) SimplySwords.runicEffectsConfig.slowDuration;

        if (attacker.getRandom().nextInt(100) <= shitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 1), attacker);
        }
    }
    // Runic Power - GREATER SLOW
    public static void postHitRunicGreaterSlow(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int shitchance = (int) SimplySwords.runicEffectsConfig.slowChance;
        int sduration = (int) SimplySwords.runicEffectsConfig.slowDuration;

        if (attacker.getRandom().nextInt(100) <= shitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 2), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - SWIFTNESS
    public static void postHitRunicSwiftness(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int shitchance = (int) SimplySwords.runicEffectsConfig.swiftnessChance;
        int sduration = (int) SimplySwords.runicEffectsConfig.swiftnessDuration;

        if (attacker.getRandom().nextInt(100) <= shitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, sduration, 0), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER SWIFTNESS
    public static void postHitRunicGreaterSwiftness(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int shitchance = (int) SimplySwords.runicEffectsConfig.swiftnessChance;
        int sduration = (int) SimplySwords.runicEffectsConfig.swiftnessDuration;

        if (attacker.getRandom().nextInt(100) <= shitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, sduration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - FLOAT
    public static void postHitRunicFloat(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.floatChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.floatDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, lduration, 2), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER FLOAT
    public static void postHitRunicGreaterFloat(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.floatChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.floatDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, lduration, 3), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - ZEPHYR
    public static void postHitRunicZephyr(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.zephyrChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.zephyrDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, lduration, 0), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration, 0), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER ZEPHYR
    public static void postHitRunicGreaterZephyr(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.zephyrChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.zephyrDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, lduration, 1), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - SHIELDING
    public static void postHitRunicShielding(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.shieldingChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.shieldingDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, lduration, 0), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER SHIELDING
    public static void postHitRunicGreaterShielding(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.shieldingChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.shieldingDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, lduration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - STONESKIN
    public static void postHitRunicStoneskin(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.stoneskinChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.stoneskinDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, lduration, 1), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, lduration, 0), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration, 0), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_02.get(),
                    SoundCategory.PLAYERS, 0.3f, 1.3f);
        }
    }

    // Runic Power - GREATER STONESKIN
    public static void postHitRunicGreaterStoneskin(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.stoneskinChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.stoneskinDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, lduration, 2), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, lduration, 0), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_02.get(),
                    SoundCategory.PLAYERS, 0.3f, 1.1f);
        }
    }

    // Runic Power - TRAILBLAZE
    public static void postHitRunicTrailblaze(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.trailblazeChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.trailblazeDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration, 1), attacker);
            attacker.setOnFireFor(lduration / 20);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER TRAILBLAZE
    public static void postHitRunicGreaterTrailblaze(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.trailblazeChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.trailblazeDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration, 2), attacker);
            attacker.setOnFireFor(lduration / 20);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - WEAKNESS
    public static void postHitRunicWeaken(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.weakenChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.weakenDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, lduration, 0), attacker);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration, 1), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - GREATER WEAKNESS
    public static void postHitRunicGreaterWeaken(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwords.runicEffectsConfig.weakenChance;
        int lduration = (int) SimplySwords.runicEffectsConfig.weakenDuration;

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, lduration, 1), attacker);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration, 2), attacker);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.1f, 1.8f);
        }
    }

    // Runic Power - IMBUED
    public static void postHitRunicImbued(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int hitchance = (int) SimplySwords.runicEffectsConfig.imbuedChance;
        int damage = 6 - ((stack.getDamage() / stack.getMaxDamage()) * 100) / 20;

        if (attacker.getRandom().nextInt(100) <= hitchance) {
            target.timeUntilRegen = 0;
            target.damage(attacker.getDamageSources().magic(), damage);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.2f, 1.8f);
        }
    }

    // Runic Power - GREATER IMBUED
    public static void postHitRunicGreaterImbued(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int hitchance = (int) SimplySwords.runicEffectsConfig.imbuedChance;
        int damage = 10 - ((stack.getDamage() / stack.getMaxDamage()) * 100) / 10;

        if (attacker.getRandom().nextInt(100) <= hitchance) {
            target.timeUntilRegen = 0;
            target.damage(attacker.getDamageSources().magic(), damage);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.2f, 1.8f);
        }
    }


    // Runic Power - PinCushion
    public static void postHitRunicPinCushion(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int stuckArrows = attacker.getStuckArrowCount();
        target.damage(attacker.getDamageSources().generic(), stuckArrows);
        attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                SoundCategory.PLAYERS, 0.1f, 1.8f);
    }
    // Runic Power - Greater PinCushion
    public static void postHitRunicGreaterPinCushion(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int stuckArrows = attacker.getStuckArrowCount();
        target.damage(attacker.getDamageSources().generic(), stuckArrows * 2);
        attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                SoundCategory.PLAYERS, 0.1f, 1.8f);
    }

    // Nether Power - ECHO
    public static void postHitNetherEcho(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int amp = 0;
        //increase damage if 2H wep
        if (HelperMethods.isUniqueTwohanded(stack))
            amp = 2;
        target.addStatusEffect(new StatusEffectInstance(EffectRegistry.ECHO.get(), 20, amp), attacker);
    }

    // Nether Power - BERSERK
    public static void postHitNetherBerserk(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
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
    public static void postHitNetherRadiance(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {
            attacker.addStatusEffect(new StatusEffectInstance(EffectRegistry.IMMOLATION.get(), 100, 0), attacker);
        }
    }

    // Nether Power - ONSLAUGHT
    public static void postHitNetherOnslaught(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        if (target.hasStatusEffect(StatusEffects.SLOWNESS) && !attacker.hasStatusEffect(StatusEffects.WEAKNESS)) {
            attacker.addStatusEffect(new StatusEffectInstance(EffectRegistry.ONSLAUGHT.get(), 80, 0), attacker);
        }
    }

    // Nether Power - NULLIFICATION
    public static void postHitNetherNullification(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        if (!attacker.hasStatusEffect(EffectRegistry.BATTLE_FATIGUE.get()) && (attacker instanceof PlayerEntity user)) {
            if (!user.getWorld().isClient()) {
                ServerWorld serverWorld = (ServerWorld) user.getWorld();
                BlockState currentState = serverWorld.getBlockState(user.getBlockPos().up(4).offset(user.getMovementDirection(), 3));
                BlockState state = Blocks.AIR.getDefaultState();
                if (currentState == state ) {
                    serverWorld.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
                            SoundCategory.PLAYERS, 0.4f, 0.8f);
                    BattleStandardEntity banner = EntityRegistry.BATTLESTANDARD.get().spawn(
                            serverWorld,
                            user.getBlockPos().up(4).offset(user.getMovementDirection(), 3),
                            SpawnReason.MOB_SUMMONED );
                    if (banner != null) {
                        banner.setVelocity(0, -1, 0);
                        banner.ownerEntity = user;
                        banner.decayRate = 3;
                        banner.standardType = "nullification";
                        banner.setCustomName(Text.translatable( "entity.simplyswords.battlestandard.name",user.getName()));
                    }
                    attacker.addStatusEffect(new StatusEffectInstance(EffectRegistry.BATTLE_FATIGUE.get(), 800, 0), attacker);
                }
            }
        }
    }



    // Runic Power - EMPTY
    public static void postHitRunicEmpty(ItemStack stack,  LivingEntity target, LivingEntity attacker) {


    }

    // ------- ON STOPPED USING ------- //

    public static void stoppedUsingRunicMomentum(ItemStack stack, World world,  LivingEntity user, int remainingUseTicks) {
        //Player dash end
        if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            user.setVelocity(0, 0, 0); // Stop player at end of charge
            user.velocityModified = true;
        }
    }


    // ------- USAGE TICK ------- //


    //Runic Power - MOMENTUM
    public static void usageTickRunicMomentum(ItemStack stack, World world,  LivingEntity user, int remainingUseTicks) {
        int skillCooldown = (int) SimplySwords.runicEffectsConfig.momentumCooldown;
        if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && user.isOnGround() && (user instanceof PlayerEntity player)) {
            //Player dash forward
            if (remainingUseTicks == 12 || remainingUseTicks == 13 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                player.setVelocity(player.getRotationVector().multiply(+3));
                player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z); // Prevent player flying to the heavens
                player.velocityModified = true;
                player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
            }
        }
    }

    //Runic Power - GREATER MOMENTUM
    public static void usageTickRunicGreaterMomentum(ItemStack stack, World world,  LivingEntity user, int remainingUseTicks) {
        int skillCooldown = (int) SimplySwords.runicEffectsConfig.momentumCooldown;
        if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && user.isOnGround() && (user instanceof PlayerEntity player)) {
            //Player dash forward
            if (remainingUseTicks == 10 || remainingUseTicks == 13 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                player.setVelocity(player.getRotationVector().multiply(+3));
                player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z); // Prevent player flying to the heavens
                player.velocityModified = true;
                player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
            }
        }
    }

    // ------- INVENTORY TICK ------- //


    // Runic Power - UNSTABLE
    public static void inventoryTickRunicUnstable(ItemStack stack,  World world, PlayerEntity player, int slot, boolean selected) {
        int lduration = (int) SimplySwords.runicEffectsConfig.unstableDuration;
        int lfrequency = (int) SimplySwords.runicEffectsConfig.unstableFrequency;
        if (player.age % lfrequency == 0) {
            int random = (int) (Math.random() * 100);
            if (random >= 0 && random < 10)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration));
            if (random >= 10 && random < 20)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, lduration));
            if (random >= 20 && random < 30)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, lduration));
            if (random >= 30 && random < 40)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, lduration));
            if (random >= 40 && random < 50)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, lduration));
            if (random >= 50 && random < 60)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, lduration));
            if (random >= 60 && random < 70)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, lduration));
            if (random >= 70 && random < 80)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, lduration));
            if (random >= 80 && random < 90)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, lduration));
            if (random >= 90 && random < 95)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, lduration));
            if (random >= 95 && random < 100)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, lduration));
        }
    }

    // Runic Power - ACTIVE DEFENCE
    public static void inventoryTickRunicActiveDefence(ItemStack stack,  World world, PlayerEntity player, int slot, boolean selected) {
        int lfrequency = (int) SimplySwords.runicEffectsConfig.activeDefenceFrequency;
        if (player.age % lfrequency == 0) {
            int sradius = (int) SimplySwords.runicEffectsConfig.activeDefenceRadius;
            int vradius = (int) (SimplySwords.runicEffectsConfig.activeDefenceRadius / 2);
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            ServerWorld sworld = (ServerWorld) player.getWorld();
            Box box = new Box(x + sradius, y + vradius, z + sradius, x - sradius, y - vradius, z - sradius);
            for (Entity entities : sworld.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                if (entities != null) {
                    if (entities instanceof LivingEntity le && player.getInventory().contains(Items.ARROW.getDefaultStack()) && HelperMethods.checkFriendlyFire(le, player)) {

                        var arrowstack = player.getInventory().getSlotWithStack(Items.ARROW.getDefaultStack());
                        var astack = player.getInventory().getStack(arrowstack);
                        int randomc = (int) (Math.random() * 100);
                        if (randomc < 15)
                            astack.setCount(astack.getCount()-1);

                        if (le.distanceTo(player) < sradius) {
                            double ex = le.getX();
                            double ey = le.getY();
                            double ez = le.getZ();
                            BlockPos position = (player.getBlockPos());
                            Vec3d rotation = le.getRotationVec(1f);
                            Vec3d newPos = player.getPos().add(rotation);

                            ArrowEntity arrow = new ArrowEntity(EntityType.ARROW, (ServerWorld) world);
                            arrow.updatePosition(player.getX(), (player.getY() + 1.5), player.getZ());
                            arrow.setOwner(player);
                            arrow.setVelocity( le.getX() - player.getX(), (le.getY() - player.getY()) - 1, le.getZ() - player.getZ());
                            sworld.spawnEntity(arrow);
                            break;
                        }
                    }
                }
            }
        }
    }

    // Runic Power - FROST WARD
    public static void inventoryTickRunicFrostWard(ItemStack stack,  World world, PlayerEntity player, int slot, boolean selected) {
        int lfrequency = (int) SimplySwords.runicEffectsConfig.frostWardFrequency;;
        int lduration = (int) SimplySwords.runicEffectsConfig.frostWardDuration;
        if (player.age % lfrequency == 0) {
            int sradius = (int) SimplySwords.runicEffectsConfig.frostWardRadius;
            int vradius = (int) (SimplySwords.runicEffectsConfig.frostWardRadius / 2);
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            ServerWorld sworld = (ServerWorld) player.getWorld();
            Box box = new Box(x + sradius, y + vradius, z + sradius, x - sradius, y - vradius, z - sradius);
            for (Entity entities : sworld.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                if (entities != null) {
                    if (entities instanceof LivingEntity le && HelperMethods.checkFriendlyFire(le, player)) {

                        if (le.distanceTo(player) < sradius) {
                            double ex = le.getX();
                            double ey = le.getY();
                            double ez = le.getZ();
                            BlockPos position = (player.getBlockPos());
                            Vec3d rotation = le.getRotationVec(1f);
                            Vec3d newPos = player.getPos().add(rotation);

                            SnowballEntity snowball = new SnowballEntity(EntityType.SNOWBALL, (ServerWorld) world);
                            snowball.updatePosition(player.getX(), (player.getY() + 1.5), player.getZ());
                            snowball.setOwner(player);
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration));
                            snowball.setVelocity( le.getX() - player.getX(), (le.getY() - player.getY()) - 1, le.getZ() - player.getZ());
                            sworld.spawnEntity(snowball);
                        }
                    }
                }
            }
        }
    }


}