package net.sweenus.simplyswords.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

public class RunicMethods {


    // ------- POSTHIT ------- //

    // Runic Power - FREEZE
    public static void postHitRunicFreeze(ItemStack stack,  LivingEntity target, LivingEntity attacker) {

        int fhitchance = (int) SimplySwordsConfig.getFloatValue("freeze_chance");
        int fduration = (int) SimplySwordsConfig.getFloatValue("freeze_duration");
        int sduration = (int) SimplySwordsConfig.getFloatValue("slowness_duration");

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 1), attacker);

        if (attacker.getRandom().nextInt(100) <= fhitchance) {
            target.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), fduration, 1), attacker);
        }
    }

    // Runic Power - WILDFIRE
    public static void postHitRunicWildfire(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int phitchance = (int) SimplySwordsConfig.getFloatValue("wildfire_chance");
        int pduration = (int) SimplySwordsConfig.getFloatValue("wildfire_duration");

        if (attacker.getRandom().nextInt(100) <= phitchance) {
           target.addStatusEffect(new StatusEffectInstance(EffectRegistry.WILDFIRE.get(), pduration, 3), attacker);
        }
    }

    // Runic Power - WILDFIRE
    public static void postHitRunicSlow(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int shitchance = (int) SimplySwordsConfig.getFloatValue("slowness_chance");
        int sduration = (int) SimplySwordsConfig.getFloatValue("slowness_duration");

        if (attacker.getRandom().nextInt(100) <= shitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 2), attacker);
        }
    }

    // Runic Power - SPEED
    public static void postHitRunicSpeed(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int shitchance = (int) SimplySwordsConfig.getFloatValue("speed_chance");
        int sduration = (int) SimplySwordsConfig.getFloatValue("speed_duration");

        if (attacker.getRandom().nextInt(100) <= shitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, sduration, 0), attacker);
        }
    }

    // Runic Power - LEVITATION
    public static void postHitRunicLevitation(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwordsConfig.getFloatValue("levitation_chance");
        int lduration = (int) SimplySwordsConfig.getFloatValue("levitation_duration");

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, lduration, 3), attacker);
        }
    }

    // Runic Power - ZEPHYR
    public static void postHitRunicZephyr(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwordsConfig.getFloatValue("zephyr_chance");
        int lduration = (int) SimplySwordsConfig.getFloatValue("zephyr_duration");

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, lduration, 2), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration, 0), attacker);
        }
    }

    // Runic Power - SHIELDING
    public static void postHitRunicShielding(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwordsConfig.getFloatValue("shielding_chance");
        int lduration = (int) SimplySwordsConfig.getFloatValue("shielding_duration");

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, lduration, 1), attacker);
        }
    }

    // Runic Power - STONESKIN
    public static void postHitRunicStoneskin(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwordsConfig.getFloatValue("stoneskin_chance");
        int lduration = (int) SimplySwordsConfig.getFloatValue("stoneskin_duration");

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, lduration, 1), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, lduration, 0), attacker);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration, 0), attacker);
        }
    }

    // Runic Power - TRAILBLAZE
    public static void postHitRunicTrailblaze(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwordsConfig.getFloatValue("trailblaze_chance");
        int lduration = (int) SimplySwordsConfig.getFloatValue("trailblaze_duration");

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration, 2), attacker);
            attacker.setOnFireFor(lduration / 20);
        }
    }

    // Runic Power - WEAKNESS
    public static void postHitRunicWeaken(ItemStack stack,  LivingEntity target, LivingEntity attacker) {
        int lhitchance = (int) SimplySwordsConfig.getFloatValue("weaken_chance");
        int lduration = (int) SimplySwordsConfig.getFloatValue("weaken_duration");

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, lduration, 0), attacker);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration, 1), attacker);
        }
    }




    // Runic Power - EMPTY
    public static void postHitRunicEmpty(ItemStack stack,  LivingEntity target, LivingEntity attacker) {


    }



    // ------- INVENTORY TICK ------- //


    // Runic Power - UNSTABLE
    public static void inventoryTickRunicUnstable(ItemStack stack,  World world, PlayerEntity player, int slot, boolean selected) {
        int lduration = (int) SimplySwordsConfig.getFloatValue("unstable_duration");
        int lfrequency = (int) SimplySwordsConfig.getFloatValue("unstable_frequency");
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
        int lfrequency = (int) SimplySwordsConfig.getFloatValue("active_defence_frequency");
        if (player.age % lfrequency == 0) {
            int sradius = (int) SimplySwordsConfig.getFloatValue("active_defence_radius");
            int vradius = (int) (SimplySwordsConfig.getFloatValue("active_defence_radius") / 2);
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            ServerWorld sworld = (ServerWorld) player.world;
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
        int lfrequency = (int) SimplySwordsConfig.getFloatValue("frostward_frequency");
        int lduration = (int) SimplySwordsConfig.getFloatValue("frostward_slow_duration");
        if (player.age % lfrequency == 0) {
            int sradius = (int) SimplySwordsConfig.getFloatValue("frostward_radius");
            int vradius = (int) (SimplySwordsConfig.getFloatValue("frostward_radius") / 2);
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            ServerWorld sworld = (ServerWorld) player.world;
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