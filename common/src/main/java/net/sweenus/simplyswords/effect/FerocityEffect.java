package net.sweenus.simplyswords.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.sweenus.simplyswords.config.Config;

public class FerocityEffect extends StatusEffect {

    public FerocityEffect(StatusEffectCategory category, int color) {
        super(category, color);
        addAttributeModifier(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                "3c5f1012-0af2-4a77-a6b2-2fe704650003",
                Math.max(0.0F, Config.uniqueEffects.twisted_blade.attackSpeedPerStack),
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }
}
