package net.sweenus.simplyswords.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.SimplySwordsClient;

public class SimplySwordsClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SimplySwordsClient.init();
    }
}
