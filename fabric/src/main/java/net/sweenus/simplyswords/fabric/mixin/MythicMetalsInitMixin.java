package net.sweenus.simplyswords.fabric.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.GobberCompat;
import net.sweenus.simplyswords.fabric.compat.MythicMetalsCompat;
import nourl.mythicmetals.MythicMetals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Inject our init into MythicMetals onInitialize to prevent load order issues
// https://github.com/Noaaan/MythicMetals/blob/1.19.2/src/main/java/nourl/mythicmetals/MythicMetals.java


@Mixin(MythicMetals.class)
public class MythicMetalsInitMixin {

    @Inject(at = @At("HEAD"), method = "onInitialize", remap = false)
    public void init(CallbackInfo info) {

        System.out.println("SimplySwords: Detected Mythic Metals - injecting compatibility init");
        SimplySwords.init();
        MythicMetalsCompat.registerModItems();

        if (FabricLoader.getInstance().isModLoaded("quilt_loader") && FabricLoader.getInstance().isModLoaded("gobber2") && FabricLoader.getInstance().isModLoaded("mythicmetals")) {
            GobberCompat.registerModItems();
        }
    }
}
