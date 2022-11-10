package net.sweenus.simplyswords.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.SimplySwordsClient;
import net.sweenus.simplyswords.forge.compat.GobberCompat;

@Mod(SimplySwords.MOD_ID)
public class SimplySwordsForge {
    public SimplySwordsForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(SimplySwords.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        SimplySwords.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);

        if (ModList.get().isLoaded("gobber2"))
            GobberCompat.GOBBER_ITEM.register(FMLJavaModLoadingContext.get().getModEventBus());

    }
    private void onClientSetup(FMLClientSetupEvent event) {
        SimplySwordsClient.init();
    }
}
