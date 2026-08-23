package net.sweenus.simplyswords.qa.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.sweenus.simplyswords.qa.QaClient;
import net.sweenus.simplyswords.qa.QaMod;

public final class SimplySwordsQaFabric implements ModInitializer {
    @Override public void onInitialize() { QaMod.init(); }

    public static final class Client implements ClientModInitializer {
        @Override public void onInitializeClient() { QaClient.init(); }
    }
}
