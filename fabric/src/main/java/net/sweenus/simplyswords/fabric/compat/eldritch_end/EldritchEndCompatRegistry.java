package net.sweenus.simplyswords.fabric.compat.eldritch_end;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import elocindev.eldritch_end.registry.AttributeRegistry;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplyswords.SimplySwords;

public class EldritchEndCompatRegistry {

    public static final DeferredRegister<StatusEffect> EFFECT = DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.STATUS_EFFECT);

    public static final RegistrySupplier<StatusEffect> VOIDHUNGER = EFFECT.register("voidhunger", () ->
            new VoidhungerEffect(StatusEffectCategory.HARMFUL, 1124687)
                    .addAttributeModifier(AttributeRegistry.CORRUPTION,
                            "03360b9d-c99b-4525-826a-ff5da528ebc2",
                            1.0,
                            EntityAttributeModifier.Operation.ADDITION));


}
