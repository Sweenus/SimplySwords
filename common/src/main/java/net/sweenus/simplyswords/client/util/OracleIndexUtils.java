package net.sweenus.simplyswords.client.util;

import dev.architectury.platform.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import rearth.oracle.ui.OracleScreen;

public class OracleIndexUtils {

    public static void openOracleIndex(Identifier identifier, String modId) {
        if (!Platform.isModLoaded("oracle_index")) {
            return;
        }
        if (!(MinecraftClient.getInstance().currentScreen instanceof OracleScreen)) {
            if (identifier.getPath().contains("lichblade")) // Lichblade variants are contained within one wiki entry
                identifier = Identifier.of("oracle_index:books/simplyswords/unique-weapons/lichblade.mdx");

            OracleScreen.activeWiki = modId;
            OracleScreen.activeEntry = identifier;
            MinecraftClient.getInstance().setScreen(new OracleScreen());
        }
    }

}
