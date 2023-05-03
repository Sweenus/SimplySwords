package net.sweenus.simplyswords.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class HelperMethods {

    /*
     * getTargetedEntity taken heavily from ZsoltMolnarrr's CombatSpells
     * https://github.com/ZsoltMolnarrr/CombatSpells/blob/main/common/src/main/java/net/combatspells/utils/TargetHelper.java#L72
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
        if ((entity instanceof PlayerEntity player)) {
            return !player.isDead() && (player.isSwimming() || player.getVelocity().horizontalLength() > 0.1);
        }
        else return false;
    }

    //Check if we should be able to hit the target
    public static boolean checkFriendlyFire (LivingEntity livingEntity, PlayerEntity player) {
        if (livingEntity == null || player == null)
            return false;
        if (!checkEntityBlacklist(livingEntity, player))
            return false;
        if (livingEntity instanceof PlayerEntity playerEntity) {
            return playerEntity.shouldDamagePlayer(player);
        } else {return true;}
    }

    //Check if the target matches blacklisted entities (expand this to be configurable if there is demand)
    public static boolean checkEntityBlacklist (LivingEntity livingEntity, PlayerEntity player) {
        if (livingEntity == null || player == null) {
            return false;
        }
        return !(livingEntity instanceof ArmorStandEntity)
                && !(livingEntity instanceof VillagerEntity);
    }


    //spawnParticle - spawns particles across both client & server
    public static void spawnParticle(World world, ParticleEffect particle,double  xpos, double ypos, double zpos,
                                     double xvelocity, double yvelocity, double zvelocity) {

        if (world.isClient) {
            world.addParticle(particle, xpos, ypos, zpos, xvelocity, yvelocity, zvelocity);
        } else {
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(particle, xpos, ypos, zpos, 1, xvelocity, yvelocity, zvelocity, 0.1);
            }
        }
    }

    // playHitSounds
    public static void playHitSounds(LivingEntity attacker, LivingEntity target) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
            boolean impactsounds_enabled = (SimplySwordsConfig.getBooleanValue("enable_weapon_impact_sounds"));
            float impactsounds_volume = (SimplySwordsConfig.getGeneralSettings("impact_sound_effect_volume"));

            if (impactsounds_enabled) {
                int choose_sound = (int) (Math.random() * 30);
                float choose_pitch = (float) Math.random() * 2;
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
                if (choose_sound <= 20 && choose_sound > 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
                if (choose_sound <= 30 && choose_sound > 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
                if (choose_sound <= 40 && choose_sound > 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
            }
        }
    }

    // choose Powers from provided list
    public static String chooseRunicPower() {
        List<String> runicList = Arrays.asList(
                "active_defence", "float", "greater_float", "freeze", "shielding", "greater_shielding", "slow",
                "greater_slow", "stoneskin", "greater_stoneskin", "swiftness", "greater_swiftness", "trailblaze",
                "greater_trailblaze", "weaken", "greater_weaken", "zephyr", "greater_zephyr", "frost_ward",
                "wildfire", "unstable", "momentum", "greater_momentum", "imbued", "greater_imbued", "pincushion",
                "greater_pincushion", "ward", "immolation");

        // Keep rolling up to 100 times to receive a runic power that isn't blacklisted
        // I'm sure there's a smarter way to do this, but I didn't choose to be born with a smol brain
        for (int i = 0; i < 100; i++) {
            Random choose = new Random();
            int randomIndex = choose.nextInt(runicList.size());
            String runicSelection = runicList.get(randomIndex);

            if (SimplySwordsConfig.getBooleanValue(runicSelection))
                return runicSelection;
        }

        return "";
    }

    public static String chooseRunefusedPower() {
        List<String> runicList = Arrays.asList(
                "float", "greater_float", "freeze", "shielding", "greater_shielding", "slow",
                "greater_slow", "stoneskin", "greater_stoneskin", "swiftness", "greater_swiftness", "trailblaze",
                "greater_trailblaze", "weaken", "greater_weaken", "zephyr", "greater_zephyr", "wildfire",
                "imbued", "greater_imbued", "pincushion", "greater_pincushion");

        for (int i = 0; i < 100; i++) {
            Random choose = new Random();
            int randomIndex = choose.nextInt(runicList.size());
            String runicSelection = runicList.get(randomIndex);

            if (SimplySwordsConfig.getBooleanValue(runicSelection))
                return runicSelection;
        }

        return "";
    }

    public static String chooseNetherfusedPower() {
        List<String> runicList = Arrays.asList(
            "echo", "berserk", "radiance", "onslaught");

        Random choose = new Random();
        int randomIndex = choose.nextInt(runicList.size());
        return runicList.get(randomIndex);
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
                stack.isOf(ItemsRegistry.WATCHER_CLAYMORE.get());
    }

    // createFootfalls - creates weapon footfall particle effects (footsteps)
    public static void createFootfalls(Entity entity,
                                       ItemStack stack,
                                       World world,
                                       int stepMod,
                                       DefaultParticleType particle,
                                       DefaultParticleType sprintParticle,
                                       DefaultParticleType passiveParticle,
                                       boolean passiveParticles) {
        if ((entity instanceof PlayerEntity player) && SimplySwordsConfig.getBooleanValue("enable_weapon_footfalls")) {
            if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack && isWalking(player) && !player.isSwimming() && player.isOnGround()) {
                if (stepMod == 6) {
                    if (player.isSprinting()) {
                        world.addParticle(sprintParticle, player.getX() + player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() + player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    }
                    else {
                        world.addParticle(particle, player.getX() + player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() + player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    }
                }
                else if (stepMod == 3) {
                    if (player.isSprinting()) {
                        world.addParticle(sprintParticle, player.getX() - player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() - player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    }
                    else {
                        world.addParticle(particle, player.getX() - player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() - player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    }
                }
            }
            if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack && passiveParticles && SimplySwordsConfig.getBooleanValue("enable_passive_particles")) {
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
                }
                else if (stepMod == 4) {
                    world.addParticle(passiveParticle, player.getX() + player.getHandPosOffset(stack.getItem()).getX(),
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.4 + randomy,
                            player.getZ() + player.getHandPosOffset(stack.getItem()).getZ(),
                            0, 0.0, 0);
                    world.addParticle(passiveParticle, player.getX() + player.getHandPosOffset(stack.getItem()).getX() - 0.1,
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + randomy,
                            player.getZ() + player.getHandPosOffset(stack.getItem()).getZ()  + 0.1,
                            0, 0.0, 0);
                }
            }
        }
    }
}