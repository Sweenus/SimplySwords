package net.sweenus.simplyswords.fabric.compat.eldritch_end;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class VoidhungerEffect extends StatusEffect {
    public VoidhungerEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        super.applyUpdateEffect(livingEntity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}
