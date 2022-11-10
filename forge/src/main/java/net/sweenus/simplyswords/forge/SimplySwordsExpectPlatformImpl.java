package net.sweenus.simplyswords.forge;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;

import java.nio.file.Path;

import static net.sweenus.simplyswords.SimplySwords.MOD_ID;

public class SimplySwordsExpectPlatformImpl {
    /**
     * This is our actual method to {@link SimplySwordsExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static String getVersion() {return ModList.get().getModContainerById(MOD_ID).map(it -> it.getModInfo().getVersion().toString()).orElseThrow();}

}