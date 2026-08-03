package net.sweenus.simplyswords.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class SunderedArmorEffect extends StatusEffect {
    public SunderedArmorEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
        addAttributeModifier(EntityAttributes.GENERIC_ARMOR,
                "3c5f1012-0af2-4a77-a6b2-2fe704650001",
                -0.01,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
