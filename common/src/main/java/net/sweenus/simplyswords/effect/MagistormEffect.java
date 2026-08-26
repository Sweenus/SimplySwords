package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.item.custom.MagiscytheSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.MagiscytheMasteryManager;

import java.util.List;
import java.util.Random;

public class MagistormEffect extends HighOrbitingEffect {
    public MagistormEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType1(ParticleTypes.ENCHANT);
        setParticleType2(ParticleTypes.POOF);
        setParticleType3(ParticleTypes.CRIT);
        yOffset = 4f;
        width = 4;
    }
    @Override
    public boolean applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {
            MagiscytheMasteryManager.tick(livingEntity);
        }
        super.applyUpdateEffect(livingEntity, amplifier);
        return true;
    }



    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return super.canApplyUpdateEffect(duration, amplifier);
    }

}
