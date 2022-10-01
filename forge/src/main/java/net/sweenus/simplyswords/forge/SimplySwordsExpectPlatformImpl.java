package net.sweenus.simplyswords.forge;

import net.minecraftforge.fml.loading.FMLPaths;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;

import java.nio.file.Path;

public class SimplySwordsExpectPlatformImpl {
    /**
     * This is our actual method to {@link SimplySwordsExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
