package net.sweenus.simplyswords.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.sweenus.simplyswords.SimplySwords;

public class SimplySwordsFabric implements ModInitializer {
    @Override
    public void onInitialize() {

        SimplySwords.init();

        //Quilt makes the load order wierd - gobbercompat item registry needs to be injected to prevent crash
        if (FabricLoader.getInstance().isModLoaded("quilt_loader") && FabricLoader.getInstance().isModLoaded("gobber2")) {
            System.out.println("SimplySwords: Detected Quilt Loader. Gobber compatibility fix is being applied.");
            //GobberCompat.registerModItems(); 1.21
        }
        else {
            if (FabricLoader.getInstance().isModLoaded("gobber2")) {
                //GobberCompat.registerModItems(); 1.21
            }
        }
    }

}
