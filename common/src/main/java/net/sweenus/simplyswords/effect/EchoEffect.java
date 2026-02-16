package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;

public class EchoEffect extends StatusEffect {
    public EchoEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getEntityWorld().isClient()) {
            if (livingEntity.age % 15 == 0) {
                int damage = Config.statusEffects.echoDamage;
                livingEntity.timeUntilRegen = 0;
                livingEntity.damage(world, livingEntity.getDamageSources().magic(), damage + amplifier);
                livingEntity.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.ECHO));
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
