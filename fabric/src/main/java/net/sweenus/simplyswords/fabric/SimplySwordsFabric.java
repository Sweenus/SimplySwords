package net.sweenus.simplyswords.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.GobberCompat;

public class SimplySwordsFabric implements ModInitializer {
    @Override
    public void onInitialize() {

        //Only run our init if Mythic Metals is not installed
        if (!FabricLoader.getInstance().isModLoaded("mythicmetals")) {
            SimplySwords.init();
        }

        //We no longer need to init here as we instead inject our init into Mythic Metals to avoid a load order crash
        /*
        //SimplySwords.init();
        if (FabricLoader.getInstance().isModLoaded("mythicmetals")) {
            MythicMetalsCompat.registerModItems();
            System.out.println("Detected Mythic Metals. Started registering Mythic Metals compat items for Simply Swords");
            Abilities.init();
        }
        */

        if (FabricLoader.getInstance().isModLoaded("gobber2")) {
            GobberCompat.registerModItems();
        }

    }
}
