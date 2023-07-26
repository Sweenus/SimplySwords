package net.sweenus.simplyswords.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import static net.sweenus.simplyswords.SimplySwords.MOD_ID;

public class SimplySwordsClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(modContainer -> {
            //ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(MOD_ID, "classic"), modContainer, ResourcePackActivationType.NORMAL);
            //System.out.println("Registering Classic style resourcepack for Simply Swords");
        });
    }
}
