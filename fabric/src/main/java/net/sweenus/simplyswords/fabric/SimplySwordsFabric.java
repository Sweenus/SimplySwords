package net.sweenus.simplyswords.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.GobberCompat;
import net.sweenus.simplyswords.fabric.compat.MythicMetalsCompat;

public class SimplySwordsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimplySwords.init();


        if (FabricLoader.getInstance().isModLoaded("mythicmetals")) {
            MythicMetalsCompat.registerModItems();
        }

        if (FabricLoader.getInstance().isModLoaded("gobber2")) {
            GobberCompat.registerModItems();
        }

    }
}
