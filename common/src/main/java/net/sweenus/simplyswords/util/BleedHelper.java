package net.sweenus.simplyswords.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;

public final class BleedHelper {
    public static final int MAX_STACKS = 10;
    public static final int DEFAULT_DURATION = 160;

    private BleedHelper() {
    }

    public static int getStacks(LivingEntity target) {
        StatusEffectInstance effect = target == null ? null
                : target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.BLEED));
        return effect == null ? 0 : Math.min(MAX_STACKS, effect.getAmplifier() + 1);
    }

    public static boolean hasBleed(LivingEntity target) {
        return getStacks(target) > 0;
    }

    public static void clear(LivingEntity target) {
        if (target != null) {
            target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.BLEED));
        }
    }

    public static int apply(LivingEntity target, LivingEntity source, float triggeringDamage) {
        return apply(target, source, triggeringDamage, 1, DEFAULT_DURATION);
    }

    public static int apply(LivingEntity target, LivingEntity source, float triggeringDamage, int addedStacks, int duration) {
        if (target == null || !target.isAlive() || addedStacks <= 0) {
            return getStacks(target);
        }
        SimplySwordsStatusEffectInstance current = target.getStatusEffect(
                EffectRegistry.getReference(EffectRegistry.BLEED)) instanceof SimplySwordsStatusEffectInstance instance
                ? instance : null;
        int currentStacks = current == null ? 0 : current.getAmplifier() + 1;
        int stacks = Math.min(MAX_STACKS, currentStacks + addedStacks);
        int snapshot = Math.max(1, Math.round(Math.max(0.0F, triggeringDamage) * 0.5F * 100.0F));
        int highestSnapshot = Math.max(snapshot, current == null ? 0 : current.getAdditionalData());
        SimplySwordsStatusEffectInstance next = new SimplySwordsStatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.BLEED), Math.max(1, duration), stacks - 1,
                false, false, true);
        next.setAdditionalData(highestSnapshot);
        next.setSourceEntity(source);
        target.addStatusEffect(next, source);
        WeaponImplicitRegistry.spawnBleedParticles(target, stacks, false);
        return stacks;
    }
}
