package net.sweenus.simplyswords.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.SimplySwordsClient;

import static net.sweenus.simplyswords.SimplySwords.LOGGER;
import static net.sweenus.simplyswords.SimplySwords.MOD_ID;

public class SimplySwordsClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SimplySwordsClient.init();
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(modContainer -> {
            ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "vanillastyle"), modContainer, ResourcePackActivationType.DEFAULT_ENABLED);
            System.out.println("Registering Vanilla Style resourcepack for Simply Swords");
        });
    }
}
