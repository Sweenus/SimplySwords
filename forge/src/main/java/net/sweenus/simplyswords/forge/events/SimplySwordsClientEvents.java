package net.sweenus.simplyswords.forge.events;

import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.resource.PathPackResources;
import net.sweenus.simplyswords.SimplySwords;

@Mod.EventBusSubscriber(modid = SimplySwords.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SimplySwordsClientEvents {
    @SubscribeEvent
    public static void simplySwords$addPackFinder(AddPackFindersEvent event) {
        if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
            simplySwords$registerResourcePack(event, new Identifier("simplyswords", "classic"), false);
        }
    }

    private static void simplySwords$registerResourcePack(AddPackFindersEvent event, Identifier identifier, boolean alwaysEnabled) {
        event.addRepositorySource((profileAdder -> {
            IModFile file = ModList.get().getModFileById(identifier.getNamespace()).getFile();
            try (PathPackResources packResources = new PathPackResources(
                    identifier.toString(),
                    true,
                    file.findResource("resourcepacks/" + identifier.getPath()))) {
                profileAdder.accept(ResourcePackProfile.create(
                        identifier.toString(),
                        Text.of(identifier.getNamespace()+"/"+identifier.getPath()),
                        alwaysEnabled,
                        a -> packResources,
                        ResourceType.CLIENT_RESOURCES,
                        ResourcePackProfile.InsertionPosition.TOP,
                        ResourcePackSource.BUILTIN));
            } catch (NullPointerException e) {e.printStackTrace();}
        }));
    }
}