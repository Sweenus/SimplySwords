package net.sweenus.simplyswords.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.GobberCompat;
import net.sweenus.simplyswords.fabric.compat.EldritchEndCompat;
import net.sweenus.simplyswords.fabric.compat.MythicMetalsCompat;

public class SimplySwordsFabric implements ModInitializer {
    @Override
    public void onInitialize() {


        SimplySwords.init();

        //Quilt makes the load order wierd - gobbercompat item registry needs to be injected to prevent crash
        if (FabricLoader.getInstance().isModLoaded("quilt_loader") && FabricLoader.getInstance().isModLoaded("gobber2")
                && FabricLoader.getInstance().isModLoaded("mythicmetals")
                && FabricLoader.getInstance().isModLoaded("eldritch_end")) {
            System.out.println("SimplySwords: Detected Quilt Loader. Mythic Metals, Gobber, and Eldritch End compatibility fix is being applied.");
            EldritchEndCompat.registerModItems();
            GobberCompat.registerModItems();
            MythicMetalsCompat.registerModItems();
        }
        else {
            if (FabricLoader.getInstance().isModLoaded("eldritch_end")) {
                EldritchEndCompat.registerModItems();
            }
            if (FabricLoader.getInstance().isModLoaded("gobber2")) {
                GobberCompat.registerModItems();
            }
            if (FabricLoader.getInstance().isModLoaded("mythicmetals")) {
                MythicMetalsCompat.registerModItems();
            }
        }
    }
}
