package net.sweenus.simplyswords.util;

import dev.architectury.platform.Platform;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.scoreboard.AbstractTeam;
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
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
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

        // Check if the player and the living entity are on the same team
        AbstractTeam playerTeam = attacker.getScoreboardTeam();
        AbstractTeam entityTeam = target.getScoreboardTeam();
        if (playerTeam != null && entityTeam != null && target.isTeammate(attacker)) {
            // They are on the same team, so friendly fire should not be allowed
            return false;
        }

        if (target instanceof PlayerEntity playerTarget) {
            if (playerTarget == attacker) {
                return false;
            }
            return !(attacker instanceof PlayerEntity playerAttacker) || playerAttacker.shouldDamagePlayer(playerTarget);
        }
        if (target instanceof Tameable tameable && attacker instanceof PlayerEntity player) {
            if (tameable.getOwner() != null) {
                if (tameable.getOwner() != player
                        && (tameable.getOwner() instanceof PlayerEntity ownerPlayer))
                    return player.shouldDamagePlayer(ownerPlayer);
                return tameable.getOwner() != player;
            }
            return true;
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
            boolean impactsounds_enabled = Config.getBoolean("enableWeaponImpactSounds", "General",ConfigDefaultValues.enableWeaponImpactSounds);
            float impactsounds_volume = Config.getFloat("weaponImpactSoundsVolume", "General",ConfigDefaultValues.weaponImpactSoundsVolume);

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
            case "active_defence" ->        Config.getBoolean("enableActiveDefence", "RunicEffects",ConfigDefaultValues.enableActiveDefence);
            case "float" ->                 Config.getBoolean("enableFloat", "RunicEffects",ConfigDefaultValues.enableFloat);
            case "greater_float" ->         Config.getBoolean("enableGreaterFloat", "RunicEffects",ConfigDefaultValues.enableGreaterFloat);
            case "freeze" ->                Config.getBoolean("enableFreeze", "RunicEffects",ConfigDefaultValues.enableFreeze);
            case "shielding" ->             Config.getBoolean("enableShielding", "RunicEffects",ConfigDefaultValues.enableShielding);
            case "greater_shielding" ->     Config.getBoolean("enableGreaterShielding", "RunicEffects",ConfigDefaultValues.enableGreaterShielding);
            case "slow" ->                  Config.getBoolean("enableSlow", "RunicEffects",ConfigDefaultValues.enableSlow);
            case "greater_slow" ->          Config.getBoolean("enableGreaterSlow", "RunicEffects",ConfigDefaultValues.enableGreaterSlow);
            case "stoneskin" ->             Config.getBoolean("enableStoneskin", "RunicEffects",ConfigDefaultValues.enableStoneskin);
            case "greater_stoneskin" ->     Config.getBoolean("enableGreaterStoneskin", "RunicEffects",ConfigDefaultValues.enableGreaterStoneskin);
            case "swiftness" ->             Config.getBoolean("enableSwiftness", "RunicEffects",ConfigDefaultValues.enableSwiftness);
            case "greater_swiftness" ->     Config.getBoolean("enableGreaterSwiftness", "RunicEffects",ConfigDefaultValues.enableGreaterSwiftness);
            case "trailblaze" ->            Config.getBoolean("enableTrailblaze", "RunicEffects",ConfigDefaultValues.enableTrailblaze);
            case "greater_trailblaze" ->    Config.getBoolean("enableGreaterTrailblaze", "RunicEffects",ConfigDefaultValues.enableGreaterTrailblaze);
            case "weaken" ->                Config.getBoolean("enableWeaken", "RunicEffects",ConfigDefaultValues.enableWeaken);
            case "greater_weaken" ->        Config.getBoolean("enableGreaterWeaken", "RunicEffects",ConfigDefaultValues.enableGreaterWeaken);
            case "zephyr" ->                Config.getBoolean("enableZephyr", "RunicEffects",ConfigDefaultValues.enableZephyr);
            case "greater_zephyr" ->        Config.getBoolean("enableGreaterZephyr", "RunicEffects",ConfigDefaultValues.enableGreaterZephyr);
            case "frost_ward" ->            Config.getBoolean("enableFrostWard", "RunicEffects",ConfigDefaultValues.enableFrostWard);
            case "wildfire" ->              Config.getBoolean("enableWildfire", "RunicEffects",ConfigDefaultValues.enableWildfire);
            case "unstable" ->              Config.getBoolean("enableUnstable", "RunicEffects",ConfigDefaultValues.enableUnstable);
            case "momentum" ->              Config.getBoolean("enableMomentum", "RunicEffects",ConfigDefaultValues.enableMomentum);
            case "greater_momentum" ->      Config.getBoolean("enableGreaterMomentum", "RunicEffects",ConfigDefaultValues.enableGreaterMomentum);
            case "imbued" ->                Config.getBoolean("enableImbued", "RunicEffects",ConfigDefaultValues.enableImbued);
            case "greater_imbued" ->        Config.getBoolean("enableGreaterImbued", "RunicEffects",ConfigDefaultValues.enableGreaterImbued);
            case "pincushion" ->            Config.getBoolean("enablePincushion", "RunicEffects",ConfigDefaultValues.enablePincushion);
            case "greater_pincushion" ->    Config.getBoolean("enableGreaterPincushion", "RunicEffects",ConfigDefaultValues.enableGreaterPincushion);
            case "ward" ->                  Config.getBoolean("enableWard", "RunicEffects",ConfigDefaultValues.enableWard);
            case "immolation" ->            Config.getBoolean("enableImmolate", "RunicEffects",ConfigDefaultValues.enableImmolate);
            default -> false;
        };
    }

    public static boolean checkNetherBlacklist(String netherPower) {
        return switch (netherPower) {
            case "echo" ->                  Config.getBoolean("enableEcho", "GemEffects",ConfigDefaultValues.enableEcho);
            case "berserk" ->               Config.getBoolean("enableBerserk", "GemEffects",ConfigDefaultValues.enableBerserk);
            case "radiance" ->              Config.getBoolean("enableRadiance", "GemEffects",ConfigDefaultValues.enableRadiance);
            case "onslaught" ->             Config.getBoolean("enableOnslaught", "GemEffects",ConfigDefaultValues.enableOnslaught);
            case "nullification" ->         Config.getBoolean("enableNullification", "GemEffects",ConfigDefaultValues.enableNullification);
            case "precise" ->               Config.getBoolean("enablePrecise", "GemEffects",ConfigDefaultValues.enablePrecise);
            case "mighty" ->                Config.getBoolean("enableMighty", "GemEffects",ConfigDefaultValues.enableMighty);
            case "stealthy" ->              Config.getBoolean("enableStealthy", "GemEffects",ConfigDefaultValues.enableStealthy);
            case "renewed" ->               Config.getBoolean("enableRenewed", "GemEffects",ConfigDefaultValues.enableRenewed);
            case "accelerant" ->            Config.getBoolean("enableAccelerant", "GemEffects",ConfigDefaultValues.enableAccelerant);
            case "leaping" ->               Config.getBoolean("enableLeaping", "GemEffects",ConfigDefaultValues.enableLeaping);
            case "spellshield" ->           Config.getBoolean("enableSpellshield", "GemEffects",ConfigDefaultValues.enableSpellshield);
            case "spellforged" ->           Config.getBoolean("enableSpellforged", "GemEffects",ConfigDefaultValues.enableSpellforged);
            case "soulshock" ->             Config.getBoolean("enableSoulshock", "GemEffects",ConfigDefaultValues.enableSoulshock);
            case "spell_standard" ->         Config.getBoolean("enableSpellStandard", "GemEffects",ConfigDefaultValues.enableSpellStandard);
            case "war_standard" ->           Config.getBoolean("enableWarStandard", "GemEffects",ConfigDefaultValues.enableWarStandard);
            case "deception" ->             Config.getBoolean("enableDeception", "GemEffects",ConfigDefaultValues.enableDeception);
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
        List<String> netherList;
        if (Platform.isModLoaded("simplyskills"))
            netherList = Arrays.asList("echo", "berserk", "radiance", "onslaught", "nullification",
                    "precise", "mighty", "stealthy", "renewed", "accelerant", "leaping", "spellshield", "spellforged",
                    "soulshock", "spell_standard", "war_standard", "deception");
        else
            netherList = Arrays.asList("echo", "berserk", "radiance", "onslaught", "nullification");

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
        if ((entity instanceof PlayerEntity player) && Config.getBoolean("enableWeaponFootfalls", "General",ConfigDefaultValues.enableWeaponFootfalls) && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
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
            if (passiveParticles && Config.getBoolean("enablePassiveParticles", "General",ConfigDefaultValues.enablePassiveParticles)) {
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

    public static void spawnOrbitParticles(ServerWorld world, Vec3d center, ParticleEffect particleType, double radius, int particleCount) {
        for (int i = 0; i < particleCount; i++) {
            // Calculate the angle for this particle
            double angle = 2 * Math.PI * i / particleCount;

            // Calculate the x and z coordinates on the orbit
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y;

            // Spawn the particle at the calculated position
            world.spawnParticles(particleType, x, y, z, 1, 0, 0, 0, 0);
            //spawnParticle(world, particleType, x, y, z, 0, 0, 0);
        }
    }


    public static float commonSpellAttributeScaling(float damageModifier, Entity entity, String magicSchool) {
        if (Platform.isModLoaded("spell_power") && Platform.isFabric())
            if ((entity instanceof PlayerEntity player) && Config.getBoolean("compatEnableSpellPowerScaling", "General",ConfigDefaultValues.compatEnableSpellPowerScaling))
                return SimplySwordsExpectPlatform.getSpellPowerDamage(damageModifier, player, magicSchool);

        return 0f;
    }

}