package net.sweenus.simplyswords.forge;


import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sweenus.simplyswords.SimplySwords;

@Mod(SimplySwords.MOD_ID)
public class SimplySwordsForge {
    public SimplySwordsForge() {
        EventBuses.registerModEventBus(
                SimplySwords.MOD_ID,
                FMLJavaModLoadingContext.get().getModEventBus()
        );

        SimplySwords.init();

        if (ModList.get().isLoaded("gobber2")) {
            //GobberCompat.registerModItems();
            //GobberCompat.GOBBER_ITEM.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }
}
