package net.sweenus.simplyswords.client.util;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class OracleIndexUtils {

    private static Identifier pendingEntry = null;
    private static String pendingModId = null;

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (pendingEntry == null) return;

            // Open only once Ctrl is released
            if (!Screen.hasControlDown()) {
                if (!isOracleScreen(client.currentScreen)) {
                    openScreen(client, pendingEntry, pendingModId);
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
        if (!isOracleScreen(MinecraftClient.getInstance().currentScreen)) {
            if (identifier.getPath().contains("lichblade")) // Lichblade variants are contained within one wiki entry
                identifier = new Identifier("oracle_index", "books/simplyswords/unique-weapons/lichblade.mdx");
            if (identifier.getPath().contains("ionbound_stormscale"))
                identifier = new Identifier("oracle_index", "books/simplyswords/unique-weapons/stormscale.mdx");

            pendingEntry = identifier;
            pendingModId = modId;
        }
    }

    private static boolean isOracleScreen(Screen screen) {
        return screen != null && "rearth.oracle.ui.OracleScreen".equals(screen.getClass().getName());
    }

    private static void openScreen(MinecraftClient client, Identifier entry, String modId) {
        try {
            Class<?> screenClass = Class.forName("rearth.oracle.ui.OracleScreen");
            Field activeWiki = screenClass.getField("activeWiki");
            Field activeEntry = screenClass.getField("activeEntry");
            Constructor<?> constructor = screenClass.getConstructor();
            activeWiki.set(null, modId);
            activeEntry.set(null, entry);
            client.setScreen((Screen) constructor.newInstance());
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Oracle Index is optional and has no stable 1.20.1 API artifact.
        }
    }

}
