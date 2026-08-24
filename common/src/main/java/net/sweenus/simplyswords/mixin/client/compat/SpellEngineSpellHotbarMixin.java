package net.sweenus.simplyswords.mixin.client.compat;

import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.sweenus.simplyswords.client.compat.SpellEngineCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.spell_engine.client.input.SpellHotbar")
public class SpellEngineSpellHotbarMixin {

    @Redirect(method = "expectedUseStack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;"),
            require = 0)
    private static UseAction simplyswords$adjustExpectedUseAction(ItemStack stack) {
        return SpellEngineCompat.getExpectedUseAction(stack);
    }
}
