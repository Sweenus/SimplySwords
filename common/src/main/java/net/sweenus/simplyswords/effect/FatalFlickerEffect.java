package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class FatalFlickerEffect extends StatusEffect {
    public FatalFlickerEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    public static void performDash(PlayerEntity player, World world, int radius, int maxAmplifier, int dashDistance, int amplifier) {

        player.setVelocity(player.getRotationVector().multiply(+dashDistance));
        player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
        player.velocityModified = true;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 3), player);
        player.timeUntilRegen = 25;
        ItemStack itemStack = player.getEquippedStack(EquipmentSlot.MAINHAND);

        Box box = HelperMethods.createBox(player, radius);
        Box boxPull = HelperMethods.createBox(player, radius * 2);
        for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

            if (entities != null) {
                if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {
                    amplifier ++;
                    HelperMethods.incrementStatusEffect(le, EffectRegistry.ECHO.get(), 20, amplifier,
                            maxAmplifier);
                }
            }
        }
        for (Entity entities : world.getOtherEntities(player, boxPull, EntityPredicates.VALID_LIVING_ENTITY)) {

            if (entities != null) {
                if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {
                    amplifier ++;
                    le.setVelocity((player.getX() - le.getX()) /4,  (player.getY() - le.getY()) /4, (player.getZ() - le.getZ()) /4);
                }
            }
        }

    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int pAmplifier) {

        super.applyUpdateEffect(entity, pAmplifier);


        if (!entity.getWorld().isClient() && (entity instanceof PlayerEntity player)) {

            int amplifier = 0;
            int ability_timer = entity.getStatusEffect(EffectRegistry.FATAL_FLICKER.get()).getDuration();
            World world = entity.getWorld();
            int dashDistance = (int) (SimplySwordsConfig.getFloatValue("fatalflicker_dash_velocity"));
            int radius = (int) (SimplySwordsConfig.getFloatValue("fatalflicker_radius"));
            int maxAmplifier = (int) (SimplySwordsConfig.getFloatValue("fatalflicker_max_echo_stacks"));

            //Player dash forward
            if (ability_timer >= 25 && ability_timer < 30) {
                performDash(player, world, radius, maxAmplifier, dashDistance, amplifier);
            } else if (ability_timer >= 15 && ability_timer <20) {
                performDash(player, world, radius, maxAmplifier, dashDistance, amplifier);
            } else if ((ability_timer >=5 && ability_timer <10)) {
                performDash(player, world, radius, maxAmplifier, dashDistance, amplifier);
            } else {
                player.setVelocity(0, 0, 0); // Stop player at end of charges
                player.velocityModified = true;
            }

            if (player.age % 2 == 0) {
                int particleRadius = (int) (radius * 0.5);
                double xpos = player.getX() - (particleRadius + 1);
                double ypos = player.getY();
                double zpos = player.getZ() - (particleRadius + 1);

                for (int i = particleRadius * 2; i > 0; i--) {
                    for (int j = particleRadius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.ELECTRIC_SPARK, xpos + i + choose,
                                ypos + 0.4,
                                zpos + j + choose,
                                0, 0.1, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.CLOUD, xpos + i + choose,
                                ypos + 0.1,
                                zpos + j + choose,
                                0, 0, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.WARPED_SPORE, xpos + i + choose,
                                ypos,
                                zpos + j + choose,
                                0, 0.1, 0);
                    }
                }
            }
        }


    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

}
