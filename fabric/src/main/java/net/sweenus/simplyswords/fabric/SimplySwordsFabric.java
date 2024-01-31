package net.sweenus.simplyswords.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.GobberCompat;
import net.sweenus.simplyswords.fabric.compat.EldritchEndCompat;
import net.sweenus.simplyswords.fabric.compat.MythicMetalsCompat;
import net.sweenus.simplyswords.fabric.compat.eldritch_end.EldritchEndCompatRegistry;

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
                if (passVersionCheck("eldritch_end", "0.2.31"))
                    EldritchEndCompat.registerModItems();
                EldritchEndCompatRegistry.EFFECT.register();
            }
            if (FabricLoader.getInstance().isModLoaded("gobber2")) {
                GobberCompat.registerModItems();
            }
            if (FabricLoader.getInstance().isModLoaded("mythicmetals")) {
                MythicMetalsCompat.registerModItems();
            }
        }
    }


    public static boolean passVersionCheck(String modId, String requiredVersion) {
        if (FabricLoader.getInstance().isModLoaded(modId)) {
            ModMetadata modMetadata = FabricLoader.getInstance().getModContainer(modId).get().getMetadata();
            try {
                SemanticVersion currentVersion = (SemanticVersion) modMetadata.getVersion();
                SemanticVersion targetVersion = SemanticVersion.parse(requiredVersion);

                // Check if the current version is greater than or equal to the target version
                return currentVersion.compareTo(targetVersion) >= 0;
            } catch (VersionParsingException e) {
                System.err.println("Error parsing version: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

}
