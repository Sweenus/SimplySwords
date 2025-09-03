package net.sweenus.simplyswords.neoforge;


import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.sweenus.simplyswords.SimplySwords;

@Mod(SimplySwords.MOD_ID)
public class SimplySwordsForge {
    public SimplySwordsForge() {
        SimplySwords.init();

        if (ModList.get().isLoaded("gobber2")) {
            //GobberCompat.registerModItems();
            //GobberCompat.GOBBER_ITEM.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }
}