package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.BloodwakeAbilityManager;

public class BleedEffect extends StatusEffect {
    public BleedEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
    }

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient() && livingEntity.age % 20 == 0) {
            int snapshot = 0;
            LivingEntity source = null;
            if (livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.BLEED)) instanceof SimplySwordsStatusEffectInstance instance) {
                snapshot = instance.getAdditionalData();
                source = instance.getSourceEntity();
            }
            float totalDamage = snapshot <= 0 ? (amplifier + 1) : (snapshot / 100.0F) * (amplifier + 1);
            DamageSource sourceDamage = source == null ? livingEntity.getDamageSources().generic() : livingEntity.getDamageSources().indirectMagic(source, source);
            float tickDamage = Math.max(0.25F, totalDamage / 8.0F);
            livingEntity.timeUntilRegen = 0;
            BloodwakeAbilityManager.runWithoutPlagueSpread(() ->
                    WeaponImplicitRegistry.runSuppressed(() ->
                            HelperMethods.applyDamageWithoutKnockback(livingEntity, sourceDamage, tickDamage)));
            WeaponImplicitRegistry.spawnBleedParticles(livingEntity, amplifier + 1, true);
        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }
}
