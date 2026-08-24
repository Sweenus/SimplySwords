package net.sweenus.simplyswords.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class ImplicitHasteEffect extends StatusEffect {
    public ImplicitHasteEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
        addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED,
                Identifier.of("simplyswords", "implicit_attack_speed"),
                0.1,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
