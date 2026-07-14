package net.sweenus.simplyswords.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class SunderedArmorEffect extends StatusEffect {
    public SunderedArmorEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
        addAttributeModifier(EntityAttributes.GENERIC_ARMOR,
                Identifier.of("simplyswords", "implicit_sundered_armor"),
                -0.01,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
