package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;
import java.util.Objects;

public class FatalFlickerEffect extends StatusEffect {
    public FatalFlickerEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
    }

    public static void performDash(LivingEntity user, World world, int radius) {
        int dashDistance = (int) Config.getFloat("fatalFlickerDashVelocity", "UniqueEffects", ConfigDefaultValues.fatalFlickerDashVelocity);
        int maxAmplifier = (int) Config.getFloat("fatalFlickerMaxStacks", "UniqueEffects", ConfigDefaultValues.fatalFlickerMaxStacks);
        int amplifier = 1;

        user.setVelocity(user.getRotationVector().multiply(+dashDistance));
        user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z);
        user.velocityModified = true;
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 3), user);
        user.timeUntilRegen = 25;

        Box box = HelperMethods.createBox(user, radius);
        List<Entity> entities = world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY);
        for (Entity entity : entities) {
            if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                amplifier++;
            }
        }
        for (Entity entity : entities) {
            if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                HelperMethods.incrementStatusEffect(le, EffectRegistry.ECHO.get(), 20, amplifier, maxAmplifier);
            }
        }

        Box boxPull = HelperMethods.createBox(user, radius * 2);
        for (Entity entity : world.getOtherEntities(user, boxPull, EntityPredicates.VALID_LIVING_ENTITY)) {
            if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                le.setVelocity((user.getX() - le.getX()) / 4, (user.getY() - le.getY()) / 4, (user.getZ() - le.getZ()) / 4);
            }
        }
    }

    @Override
    public void applyUpdateEffect(LivingEntity user, int amplifier) {

        super.applyUpdateEffect(user, amplifier);

        if (!user.getWorld().isClient()) {

            int ability_timer = Objects.requireNonNull(user.getStatusEffect(EffectRegistry.FATAL_FLICKER.get())).getDuration();
            World world = user.getWorld();
            int radius = (int) Config.getFloat("fatalFlickerRadius", "UniqueEffects", ConfigDefaultValues.fatalFlickerRadius);

            //Player dash forward
            if (ability_timer >= 5) {
                performDash(user, world, radius);
            } else {
                user.setVelocity(0, 0, 0); // Stop user at end of charges
                user.velocityModified = true;
            }

            if (user.age % 2 == 0) {
                int particleRadius = (int) (radius * 0.5);
                double xpos = user.getX() - (particleRadius + 1);
                double ypos = user.getY();
                double zpos = user.getZ() - (particleRadius + 1);

                for (int i = particleRadius * 2; i > 0; i--) {
                    for (int j = particleRadius * 2; j > 0; j--) {
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

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }
}
