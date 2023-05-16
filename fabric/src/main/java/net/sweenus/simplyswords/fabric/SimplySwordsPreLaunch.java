package net.sweenus.simplyswords.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

import javax.naming.spi.DirectoryManager;
import java.io.File;

public class SimplySwordsPreLaunch implements PreLaunchEntrypoint {

    //Checks for config before mods initialise. When missing, create the config to prevent dependent mod crashes.
    public static void configDetect() {
        File file = new File("config/simplyswords/booleans.json5");
        if (!file.exists()) {
            SimplySwordsConfig.init();
            System.out.println("Simply Swords config is missing. Creating fresh config files now.");
        }
    }

    @Override
    public void onPreLaunch() {
        configDetect();
    }
//
}
