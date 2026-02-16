package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.item.custom.RibboncleaverSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;

public class RibboncleaveEffect extends OrbitingEffect {
    public RibboncleaveEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType(ParticleTypes.CLOUD);
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getEntityWorld().isClient()) {
            if (!(livingEntity.getMainHandStack().getItem() instanceof RibboncleaverSwordItem))
                livingEntity.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.RIBBONCLEAVE));
        }
        super.applyUpdateEffect(world, livingEntity, amplifier);
        return true;
    }

    @Override
    public void onRemoved(AttributeContainer attributes) {
        super.onRemoved(attributes);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}