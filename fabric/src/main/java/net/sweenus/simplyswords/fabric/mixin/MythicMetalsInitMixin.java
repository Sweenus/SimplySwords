package net.sweenus.simplyswords.fabric.mixin;

import net.minecraft.text.Style;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.fabric.SimplySwordsFabric;
import net.sweenus.simplyswords.fabric.compat.MythicMetalsCompat;
import nourl.mythicmetals.MythicMetals;
import nourl.mythicmetals.abilities.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mixin to Mythic Metals Abilities.class to add our weapons and gain the special effects
// https://github.com/Noaaan/MythicMetals/blob/cbbb15b9f3dae18b8575e77f52b0fb5cff2f9e5f/src/main/java/nourl/mythicmetals/abilities/Abilities.java


@Mixin(MythicMetals.class)
public class MythicMetalsInitMixin {

    @Inject(at = @At("HEAD"), method = "onInitialize", remap = false)
    public void init(CallbackInfo info) {

        System.out.println("SimplySwords: Detected Mythic Metals - injecting compatibility init");
        SimplySwords.init();
        MythicMetalsCompat.registerModItems();
    }
}
