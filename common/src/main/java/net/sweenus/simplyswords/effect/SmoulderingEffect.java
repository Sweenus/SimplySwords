package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.util.HelperMethods;

public class SmoulderingEffect extends StatusEffect {
    public SmoulderingEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getEntityWorld().isClient()) {
            int frequency = 10;
            ServerWorld serverWorld = (ServerWorld) livingEntity.getEntityWorld();
            if (livingEntity.age % frequency == 0 ) {
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getEntityPos(), ParticleTypes.LAVA, 0.2, 1);
            }
            if (livingEntity.age % frequency*4 == 0 ) {
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getEntityPos(), ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, 0.2, 1);
            }
        }
        super.applyUpdateEffect(world, livingEntity, amplifier);
        return true;
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }
}
