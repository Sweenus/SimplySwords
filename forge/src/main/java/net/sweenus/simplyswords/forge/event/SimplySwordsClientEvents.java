package net.sweenus.simplyswords.forge.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.*;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.resource.PathPackResources;

import java.io.IOException;

// Code mostly sourced from Cull Leaves by Motschen
// https://www.curseforge.com/minecraft/mc-mods/cull-leaves

@Mod.EventBusSubscriber(modid = "simplyswords", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SimplySwordsClientEvents {
    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
            registerResourcePack(event, new Identifier("simplyswords", "vanillastyle"), false);
        }
    }

    private static void registerResourcePack(AddPackFindersEvent event, Identifier id, boolean alwaysEnabled) {
        event.addRepositorySource(((profileAdder, factory) -> {
            IModFile file = ModList.get().getModFileById(id.getNamespace()).getFile();
            try (PathPackResources pack = new PathPackResources(id.toString(), file.findResource("resourcepacks/"+id.getPath()))) {
                profileAdder.accept(new ResourcePackProfile(id.toString(), alwaysEnabled, () -> pack, Text.of(id.getNamespace()+"/"+id.getPath()), pack.parseMetadata(PackResourceMetadata.READER).getDescription().copy().append(" §7(built-in)"), ResourcePackCompatibility.COMPATIBLE, ResourcePackProfile.InsertionPosition.TOP, false, ResourcePackSource.PACK_SOURCE_BUILTIN, false));
            } catch (IOException | NullPointerException e) {e.printStackTrace();}
        }));
    }
}
