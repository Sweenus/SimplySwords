package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.registry.EffectRegistry;

public class EchoEffect extends StatusEffect {
    public EchoEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {
            int damage = Config.getInt("echoDamage", "StatusEffects", ConfigDefaultValues.echoDamage);
            if (livingEntity.age % 15 == 0) {
                livingEntity.timeUntilRegen = 0;
                livingEntity.damage(livingEntity.getDamageSources().magic(), damage+amplifier);
                livingEntity.removeStatusEffect(EffectRegistry.ECHO.get());
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }
}
