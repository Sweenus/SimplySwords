package net.sweenus.simplyswords.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;

import java.nio.file.Path;

public class SimplySwordsExpectPlatformImpl {
    /**
     * This is our actual method to {@link SimplySwordsExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
