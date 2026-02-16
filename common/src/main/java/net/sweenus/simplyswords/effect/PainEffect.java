package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class PainEffect extends StatusEffect {
    public LivingEntity sourceEntity; // The player who applied the effect
    public int additionalData; // Additional integer data
    public PainEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }
    public void setSourcePlayer(LivingEntity livingEntity) {
        sourceEntity = livingEntity;
    }
    public void setAdditionalData(int data) {
        additionalData = data;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getEntityWorld().isClient()) {
            int startingTickFrequency = 15;

            if (livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.PAIN)) instanceof SimplySwordsStatusEffectInstance statusEffect) {
                sourceEntity = statusEffect.getSourceEntity();
                additionalData = statusEffect.getAdditionalData();
            }

            if (livingEntity.age % Math.max(1, (startingTickFrequency - (amplifier / 8))) == 0) {
                DamageSource damageSource = livingEntity.getDamageSources().generic();
                float amount = (1 + amplifier) * 0.5f;
                livingEntity.timeUntilRegen = 0;
                if (sourceEntity != null && sourceEntity instanceof PlayerEntity player)
                    damageSource = livingEntity.getDamageSources().playerAttack(player);
                //livingEntity.damage(damageSource, (1 + amplifier) * 0.5f);
                HelperMethods.applyDamageWithoutKnockback(livingEntity, damageSource, amount);
                livingEntity.timeUntilRegen = 0;
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