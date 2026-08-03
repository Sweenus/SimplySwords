package net.sweenus.simplyswords.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class ImplicitHasteEffect extends StatusEffect {
    public ImplicitHasteEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
        addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED,
                "3c5f1012-0af2-4a77-a6b2-2fe704650002",
                0.1,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
