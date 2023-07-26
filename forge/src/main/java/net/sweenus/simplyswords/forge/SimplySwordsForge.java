package net.sweenus.simplyswords.forge;

import dev.architectury.platform.forge.EventBuses;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.ConfigWrapper;
import net.sweenus.simplyswords.forge.compat.GobberCompat;

@Mod(SimplySwords.MOD_ID)
public class SimplySwordsForge {
    public SimplySwordsForge() {
        // Submit our event bus to let architectury register our content on the right time -
        EventBuses.registerModEventBus(SimplySwords.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        SimplySwords.init();
        //MinecraftForge.EVENT_BUS.register(new SimplySwordsClientEvents());

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> {
            return new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> {
                return AutoConfig.getConfigScreen(ConfigWrapper.class, screen).get();
            });
        });

        if (ModList.get().isLoaded("gobber2")) {
            GobberCompat.registerModItems();
            GobberCompat.GOBBER_ITEM.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

    }
}
