package net.sweenus.simplyswords.util;

import dev.architectury.platform.Platform;
import net.minecraft.resource.ResourceType;
import net.sweenus.simplyswords.SimplySwords;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@SuppressWarnings({"resource", "ConstantConditions", "ResultOfMethodCallIgnored"})
public class FileCopier {
    private static final String DATA_PREFIX = ResourceType.SERVER_DATA.getDirectory() + '/';

    public static void copyFileToConfigDirectory() throws IOException {
        if (!Platform.isModLoaded(SimplySwords.MOD_ID)
                || !SimplySwords.passVersionCheck("eldritch_end", SimplySwords.minimumEldritchEndVersion)
                || Platform.isForge())
            return;

        Optional<Path> simplySwords$safeRecipePath = Platform.getMod(SimplySwords.MOD_ID).findResource(
                DATA_PREFIX + SimplySwords.MOD_ID + "/safeload_recipes/eldritch_end/dreadtide.json"
        );
        Optional<Path> simplySwords$recipePath = Platform.getMod(SimplySwords.MOD_ID).findResource(
                DATA_PREFIX + SimplySwords.MOD_ID + "/recipes/eldritch_end/dreadtide.json"
        );

        if (simplySwords$safeRecipePath.isEmpty())
            return;

        Path sourcePath = simplySwords$safeRecipePath.get();
        Path targetPath = simplySwords$recipePath.get();

        // Ensure the directories exist
        Files.createDirectories(targetPath.getParent());

        // Copy the file, replacing existing file at the destination if it exists
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Copied from " + sourcePath + " to " + targetPath);
    }
}