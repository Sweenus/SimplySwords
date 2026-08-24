package net.sweenus.simplyswords.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.config.Config;

public class FerocityEffect extends StatusEffect {

    public FerocityEffect(StatusEffectCategory category, int color) {
        super(category, color);
        addAttributeModifier(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                Identifier.of("simplyswords", "twisted_blade_ferocity"),
                Math.max(0.0F, Config.uniqueEffects.twisted_blade.attackSpeedPerStack),
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
