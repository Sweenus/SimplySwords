package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.item.custom.BrambleSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class SporeSwarmEffect extends WideOrbitingEffect {
    public SporeSwarmEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType1(ParticleTypes.SMOKE);
        setParticleType2(ParticleTypes.ASH);
        setParticleType3(ParticleTypes.WARPED_SPORE);
    }
    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getEntityWorld().isClient()) {
            double x = livingEntity.getX();
            double y = livingEntity.getY();
            double z = livingEntity.getZ();
            double radius = 1 + (amplifier * 0.5);
            int maxAmp = 49;
            if (livingEntity.age % 15 == 0) {
                Box box = new Box(x + radius, y + 1, z + radius, x - radius, y - 1, z - radius);
                for (Entity e : world.getOtherEntities(livingEntity, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if ((e instanceof LivingEntity ee) && HelperMethods.checkFriendlyFire(ee, livingEntity)) {
                        HelperMethods.incrementStatusEffect(ee, StatusEffects.SLOWNESS, 40, 1, 3);
                        HelperMethods.incrementStatusEffect(ee, StatusEffects.MINING_FATIGUE, 40, 1, 2);
                        if (!(livingEntity.getMainHandStack().getItem() instanceof BrambleSwordItem) && !(livingEntity.getOffHandStack().getItem() instanceof BrambleSwordItem))
                            maxAmp = 9;
                        if ((livingEntity.getMainHandStack().getItem() instanceof BrambleSwordItem) && (livingEntity.getOffHandStack().getItem() instanceof BrambleSwordItem))
                            maxAmp = 74;
                        SimplySwordsStatusEffectInstance effect = HelperMethods.incrementSimplySwordsStatusEffect(
                                ee, EffectRegistry.getReference(EffectRegistry.PAIN), 60, 1, maxAmp);
                        effect.setSourceEntity(livingEntity);
                        effect.setAdditionalData(0);
                        ee.addStatusEffect(effect);
                        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.SMOKE, ee, livingEntity, 10);
                        HelperMethods.incrementStatusEffect(livingEntity, EffectRegistry.getReference(EffectRegistry.SPORE_SWARM), 200, 1, 4);
                    }
                }
            }
        }

        super.applyUpdateEffect(world, livingEntity, amplifier);

        return true;
    }



    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return super.canApplyUpdateEffect(duration, amplifier);
    }

}
