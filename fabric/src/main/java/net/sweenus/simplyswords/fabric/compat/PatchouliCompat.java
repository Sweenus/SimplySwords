package net.sweenus.simplyswords.fabric.compat;

import dev.architectury.platform.Platform;
import net.minecraft.util.Identifier;
import vazkii.patchouli.api.PatchouliAPI;

public class PatchouliCompat {

    public static void openPatchouli(Identifier entry) {
        if (Platform.isFabric()) {
            if (PatchouliAPI.get().getOpenBookGui() != null && !PatchouliAPI.get().getOpenBookGui().toString().isEmpty())
                return; // Prevent spam opening book
            final Identifier book = (Identifier.of("simplyswords:runic_grimoire"));
            PatchouliAPI.get().openBookEntry(book, entry, 0);
        }
    }



}
