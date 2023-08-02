package net.sweenus.simplyswords.util;

import dev.architectury.platform.Platform;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class HelperMethods {

    /*
     * getTargetedEntity taken heavily from ZsoltMolnarrr's CombatSpells
     * https://github.com/ZsoltMolnarrr/SpellEngine/blob/1.19.2/common/src/main/java/net/spell_engine/utils/TargetHelper.java#L136
     */
    public static Entity getTargetedEntity(Entity user, int range) {
        Vec3d rayCastOrigin = user.getEyePos();
        Vec3d userView = user.getRotationVec(1.0F).normalize().multiply(range);
        Vec3d rayCastEnd = rayCastOrigin.add(userView);
        Box searchBox = user.getBoundingBox().expand(range, range, range);
        EntityHitResult hitResult = ProjectileUtil.raycast(user, rayCastOrigin, rayCastEnd, searchBox,
                (target) -> !target.isSpectator() && target.canHit() && target instanceof LivingEntity, range * range);
        if (hitResult != null) {
            return hitResult.getEntity();
        }
        return null;
    }

    public static boolean isWalking(Entity entity) {
        return entity instanceof PlayerEntity player && (!player.isDead() && (player.isSwimming() || player.getVelocity().horizontalLength() > 0.1));
    }

    public static Style getStyle(String styleType) {
        int rgbCommon = 0xFFFFFF;
        int rgbRunic = 0x9D62CA;
        int rgbUnique = 0xE2A834;
        int rgbLegendary = 0xE26234;
        int rgbAbility = 0xE2A834;
        int rgbRightClick = 0x20BD69;
        int rgbText = 0xE0E0E0;
        Style COMMON = Style.EMPTY.withColor(TextColor.fromRgb(rgbCommon));
        Style UNIQUE = Style.EMPTY.withColor(TextColor.fromRgb(rgbUnique));
        Style LEGENDARY = Style.EMPTY.withColor(TextColor.fromRgb(rgbLegendary));
        Style ABILITY = Style.EMPTY.withColor(TextColor.fromRgb(rgbAbility));
        Style RIGHTCLICK = Style.EMPTY.withColor(TextColor.fromRgb(rgbRightClick));
        Style RUNIC = Style.EMPTY.withColor(TextColor.fromRgb(rgbRunic));
        Style TEXT = Style.EMPTY.withColor(TextColor.fromRgb(rgbText));

        return switch (styleType) {
            case "unique" -> UNIQUE;
            case "legendary" -> LEGENDARY;
            case "ability" -> ABILITY;
            case "rightclick" -> RIGHTCLICK;
            case "runic" -> RUNIC;
            case "text" -> TEXT;
            default -> COMMON;
        };
    }

    //Check if we should be able to hit the target
    public static boolean checkFriendlyFire(LivingEntity target, LivingEntity attacker) {
        if (!checkEntityBlacklist(target, attacker)) {
            return false;
        }
        if (target instanceof PlayerEntity playerTarget) {
            if (playerTarget == attacker) {
                return false;
            }
            return !(attacker instanceof PlayerEntity playerAttacker) || playerAttacker.shouldDamagePlayer(playerTarget);
        }
        if (target instanceof TameableEntity tameableEntity && tameableEntity.getOwner() != null) {
            return tameableEntity.getOwner() != attacker;
        }
        return true;
    }

    //Check if the target matches blacklisted entities (expand this to be configurable if there is demand)
    public static boolean checkEntityBlacklist(LivingEntity target, LivingEntity player) {
        if (target == null || player == null) {
            return false;
        }
        return !(target instanceof ArmorStandEntity)
                && !(target instanceof VillagerEntity)
                && !(target instanceof BattleStandardEntity)
                && !(target instanceof BattleStandardDarkEntity);
    }

    //spawnParticle - spawns particles across both client & server
    public static void spawnParticle(World world, ParticleEffect particle, double xpos, double ypos, double zpos,
                                     double xvelocity, double yvelocity, double zvelocity) {
        if (world.isClient) {
            world.addParticle(particle, xpos, ypos, zpos, xvelocity, yvelocity, zvelocity);
        } else if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(particle, xpos, ypos, zpos, 1, xvelocity, yvelocity, zvelocity, 0.1);
        }
    }

    // playHitSounds
    public static void playHitSounds(LivingEntity attacker, LivingEntity target) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            boolean impactsounds_enabled = (SimplySwords.generalConfig.enableWeaponImpactSounds);
            float impactsounds_volume = (SimplySwords.generalConfig.weaponImpactSoundsVolume);

            if (impactsounds_enabled) {
                int choose_sound = (int) (Math.random() * 30);
                float choose_pitch = (float) Math.random() * 2;
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
                else if (choose_sound <= 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
                else if (choose_sound <= 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
                else if (choose_sound <= 40)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
            }
        }
    }

    public static boolean checkRunicBlacklist(String runicPower) {
        return switch (runicPower) {
            case "active_defence" -> SimplySwords.runicEffectsConfig.enableActiveDefence;
            case "float" -> SimplySwords.runicEffectsConfig.enableFloat;
            case "greater_float" -> SimplySwords.runicEffectsConfig.enableGreaterFloat;
            case "freeze" -> SimplySwords.runicEffectsConfig.enableFreeze;
            case "shielding" -> SimplySwords.runicEffectsConfig.enableShielding;
            case "greater_shielding" -> SimplySwords.runicEffectsConfig.enableGreaterShielding;
            case "slow" -> SimplySwords.runicEffectsConfig.enableSlow;
            case "greater_slow" -> SimplySwords.runicEffectsConfig.enableGreaterSlow;
            case "stoneskin" -> SimplySwords.runicEffectsConfig.enableStoneskin;
            case "greater_stoneskin" -> SimplySwords.runicEffectsConfig.enableGreaterStoneskin;
            case "swiftness" -> SimplySwords.runicEffectsConfig.enableSwiftness;
            case "greater_swiftness" -> SimplySwords.runicEffectsConfig.enableGreaterSwiftness;
            case "trailblaze" -> SimplySwords.runicEffectsConfig.enableTrailblaze;
            case "greater_trailblaze" -> SimplySwords.runicEffectsConfig.enableGreaterTrailblaze;
            case "weaken" -> SimplySwords.runicEffectsConfig.enableWeaken;
            case "greater_weaken" -> SimplySwords.runicEffectsConfig.enableGreaterWeaken;
            case "zephyr" -> SimplySwords.runicEffectsConfig.enableZephyr;
            case "greater_zephyr" -> SimplySwords.runicEffectsConfig.enableGreaterZephyr;
            case "frost_ward" -> SimplySwords.runicEffectsConfig.enableFrostWard;
            case "wildfire" -> SimplySwords.runicEffectsConfig.enableWildfire;
            case "unstable" -> SimplySwords.runicEffectsConfig.enableUnstable;
            case "momentum" -> SimplySwords.runicEffectsConfig.enableMomentum;
            case "greater_momentum" -> SimplySwords.runicEffectsConfig.enableGreaterMomentum;
            case "imbued" -> SimplySwords.runicEffectsConfig.enableImbued;
            case "greater_imbued" -> SimplySwords.runicEffectsConfig.enableGreaterImbued;
            case "pincushion" -> SimplySwords.runicEffectsConfig.enablePincushion;
            case "greater_pincushion" -> SimplySwords.runicEffectsConfig.enableGreaterPincushion;
            case "ward" -> SimplySwords.runicEffectsConfig.enableWard;
            case "immolation" -> SimplySwords.runicEffectsConfig.enableImmolate;
            default -> false;
        };
    }

    public static boolean checkNetherBlacklist(String netherPower) {
        return switch (netherPower) {
            case "echo" -> SimplySwords.gemEffectsConfig.enableEcho;
            case "berserk" -> SimplySwords.gemEffectsConfig.enableBerserk;
            case "radiance" -> SimplySwords.gemEffectsConfig.enableRadiance;
            case "onslaught" -> SimplySwords.gemEffectsConfig.enableOnslaught;
            case "nullification" -> SimplySwords.gemEffectsConfig.enableNullification;
            default -> false;
        };
    }

    // choose Powers from provided list
    public static String chooseRunicPower() {
        List<String> runicList = Arrays.asList(
                "active_defence", "float", "greater_float", "freeze", "shielding", "greater_shielding", "slow",
                "greater_slow", "stoneskin", "greater_stoneskin", "swiftness", "greater_swiftness", "trailblaze",
                "greater_trailblaze", "weaken", "greater_weaken", "zephyr", "greater_zephyr", "frost_ward",
                "wildfire", "unstable", "momentum", "greater_momentum", "imbued", "greater_imbued", "pincushion",
                "greater_pincushion", "ward", "immolation");

        String runicSelection;
        do {
            Random choose = new Random();
            int randomIndex = choose.nextInt(runicList.size());
            runicSelection = runicList.get(randomIndex);
        } while (!checkRunicBlacklist(runicSelection));
        return runicSelection;
    }

    public static String chooseRunefusedPower() {
        List<String> runicList = Arrays.asList(
                "active_defence", "float", "greater_float", "freeze", "shielding", "greater_shielding", "slow",
                "greater_slow", "stoneskin", "greater_stoneskin", "swiftness", "greater_swiftness", "trailblaze",
                "greater_trailblaze", "weaken", "greater_weaken", "zephyr", "greater_zephyr", "frost_ward", "wildfire",
                "unstable", "imbued", "greater_imbued", "pincushion", "greater_pincushion");

        String runicSelection;
        do {
            Random choose = new Random();
            int randomIndex = choose.nextInt(runicList.size());
            runicSelection = runicList.get(randomIndex);
        } while (!checkRunicBlacklist(runicSelection));
        return runicSelection;
    }

    public static String chooseNetherfusedPower() {
        List<String> netherList = Arrays.asList("echo", "berserk", "radiance", "onslaught", "nullification");

        String netherSelection;
        do {
            Random choose = new Random();
            int randomIndex = choose.nextInt(netherList.size());
            netherSelection = netherList.get(randomIndex);
        } while (!checkNetherBlacklist(netherSelection));
        return netherSelection;
    }

    //Check if item is a unique 2H weapon
    public static boolean isUniqueTwohanded(ItemStack stack) {
        return stack.isOf(ItemsRegistry.SOULPYRE.get()) ||
                stack.isOf(ItemsRegistry.SOULKEEPER.get()) ||
                stack.isOf(ItemsRegistry.TWISTED_BLADE.get()) ||
                stack.isOf(ItemsRegistry.HEARTHFLAME.get()) ||
                stack.isOf(ItemsRegistry.SOULRENDER.get()) ||
                stack.isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get()) ||
                stack.isOf(ItemsRegistry.WAKING_LICHBLADE.get()) ||
                stack.isOf(ItemsRegistry.AWAKENED_LICHBLADE.get()) ||
                stack.isOf(ItemsRegistry.BRIMSTONE_CLAYMORE.get()) ||
                stack.isOf(ItemsRegistry.ICEWHISPER.get()) ||
                stack.isOf(ItemsRegistry.ARCANETHYST.get()) ||
                stack.isOf(ItemsRegistry.THUNDERBRAND.get()) ||
                stack.isOf(ItemsRegistry.WHISPERWIND.get()) ||
                stack.isOf(ItemsRegistry.WATCHER_CLAYMORE.get());
    }

    //Create Box
    public static Box createBox(Entity entity, int radius) {
        return new Box(entity.getX() + radius, entity.getY() + (float) radius / 3, entity.getZ() + radius,
                entity.getX() - radius, entity.getY() - (float) radius / 3, entity.getZ() - radius);
    }

    //Gets the blockpos we are looking at
    public static Vec3d getPositionLookingAt(PlayerEntity player, int range) {
        HitResult result = player.raycast(range, 0, false);
        //System.out.println(result.getType());
        if (!(result.getType() == HitResult.Type.BLOCK)) return null;

        BlockHitResult blockResult = (BlockHitResult) result;
        //System.out.println(blockResult.getBlockPos());
        return blockResult.getPos();
    }

    public static void incrementStatusEffect(
            LivingEntity livingEntity,
            StatusEffect statusEffect,
            int duration,
            int amplifier,
            int amplifierMax) {

        if (livingEntity.hasStatusEffect(statusEffect)) {
            int currentDuration = livingEntity.getStatusEffect(statusEffect).getDuration();
            int currentAmplifier = livingEntity.getStatusEffect(statusEffect).getAmplifier();

            if (currentAmplifier >= amplifierMax) {
                livingEntity.addStatusEffect(new StatusEffectInstance(
                        statusEffect, Math.max(currentDuration, duration), currentAmplifier));
                return;
            }

            livingEntity.addStatusEffect(new StatusEffectInstance(
                    statusEffect, Math.max(currentDuration, duration), Math.min(amplifierMax, currentAmplifier + amplifier)));
        }
        livingEntity.addStatusEffect(new StatusEffectInstance(statusEffect, duration));
    }

    public static void decrementStatusEffect(LivingEntity livingEntity, StatusEffect statusEffect) {

        if (livingEntity.hasStatusEffect(statusEffect)) {
            int currentAmplifier = livingEntity.getStatusEffect(statusEffect).getAmplifier();
            int currentDuration = livingEntity.getStatusEffect(statusEffect).getDuration();

            if (currentAmplifier < 1) {
                livingEntity.removeStatusEffect(statusEffect);
                return;
            }

            livingEntity.removeStatusEffect(statusEffect);
            livingEntity.addStatusEffect(new StatusEffectInstance(
                    statusEffect, currentDuration, currentAmplifier - 1));
        }
    }

    // createFootfalls - creates weapon footfall particle effects (footsteps)
    public static void createFootfalls(Entity entity, ItemStack stack, World world, int stepMod, DefaultParticleType particle,
                                       DefaultParticleType sprintParticle, DefaultParticleType passiveParticle, boolean passiveParticles) {
        if ((entity instanceof PlayerEntity player) && SimplySwords.generalConfig.enableWeaponFootfalls && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            if (isWalking(player) && !player.isSwimming() && player.isOnGround()) {
                if (stepMod == 6) {
                    if (player.isSprinting()) {
                        world.addParticle(sprintParticle, player.getX() + player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() + player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    } else {
                        world.addParticle(particle, player.getX() + player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() + player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    }
                } else if (stepMod == 3) {
                    if (player.isSprinting()) {
                        world.addParticle(sprintParticle, player.getX() - player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() - player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    } else {
                        world.addParticle(particle, player.getX() - player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() - player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    }
                }
            }
            if (passiveParticles && SimplySwords.generalConfig.enablePassiveParticles) {
                float randomy = (float) (Math.random());
                if (stepMod == 1) {
                    world.addParticle(passiveParticle, player.getX() - player.getHandPosOffset(stack.getItem()).getX(),
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.4 + randomy,
                            player.getZ() - player.getHandPosOffset(stack.getItem()).getZ(),
                            0, 0.0, 0);
                    world.addParticle(passiveParticle, player.getX() - player.getHandPosOffset(stack.getItem()).getX() + 0.1,
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + randomy,
                            player.getZ() - player.getHandPosOffset(stack.getItem()).getZ() - 0.1,
                            0, 0.0, 0);
                } else if (stepMod == 4) {
                    world.addParticle(passiveParticle, player.getX() + player.getHandPosOffset(stack.getItem()).getX(),
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.4 + randomy,
                            player.getZ() + player.getHandPosOffset(stack.getItem()).getZ(),
                            0, 0.0, 0);
                    world.addParticle(passiveParticle, player.getX() + player.getHandPosOffset(stack.getItem()).getX() - 0.1,
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + randomy,
                            player.getZ() + player.getHandPosOffset(stack.getItem()).getZ() + 0.1,
                            0, 0.0, 0);
                }
            }
        }
    }

    public static float commonSpellAttributeScaling(float damageModifier, Entity entity, String magicSchool) {
        if (Platform.isModLoaded("spell_power"))
            if ((entity instanceof PlayerEntity player) && SimplySwords.generalConfig.compatEnableSpellPowerScaling)
                return SimplySwordsExpectPlatform.getSpellPowerDamage(damageModifier, player, magicSchool);

        return 0f;
    }

}