package net.sweenus.simplyswords.client.util;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import rearth.oracle.ui.OracleScreen;

public class OracleIndexUtils {

    private static Identifier pendingEntry = null;
    private static String pendingModId = null;

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (pendingEntry == null) return;

            // Open only once Ctrl is released
            if (!Screen.hasControlDown()) {
                if (!(client.currentScreen instanceof OracleScreen)) {
                    OracleScreen.activeWiki = pendingModId;
                    OracleScreen.activeEntry = pendingEntry;
                    client.setScreen(new OracleScreen());
                }
                pendingEntry = null;
                pendingModId = null;
            }
        });
    }

    public static void openOracleIndex(Identifier identifier, String modId) {
        if (!Platform.isModLoaded("oracle_index")) {
            return;
        }
        if (!(MinecraftClient.getInstance().currentScreen instanceof OracleScreen)) {
            if (identifier.getPath().contains("lichblade")) // Lichblade variants are contained within one wiki entry
                identifier = Identifier.of("oracle_index:books/simplyswords/unique-weapons/lichblade.mdx");
            if (identifier.getPath().contains("ionbound_stormscale"))
                identifier = Identifier.of("oracle_index:books/simplyswords/unique-weapons/stormscale.mdx");

            pendingEntry = identifier;
            pendingModId = modId;
        }
    }

}
