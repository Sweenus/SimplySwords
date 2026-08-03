package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public record WeaponImplicitComponent(Identifier implicitId, Identifier weaponType, int value) {

    public static final Codec<WeaponImplicitComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("implicit_id").forGetter(WeaponImplicitComponent::implicitId),
                    Identifier.CODEC.fieldOf("weapon_type").forGetter(WeaponImplicitComponent::weaponType),
                    Codec.INT.fieldOf("value").forGetter(WeaponImplicitComponent::value)
            ).apply(instance, WeaponImplicitComponent::new));

}
