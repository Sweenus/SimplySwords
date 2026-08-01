package net.sweenus.simplyswords.fabric.client;

import dev.architectury.platform.Platform;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.particle.CustomBubbleParticle;
import net.sweenus.simplyswords.client.particle.DrippingBloodParticle;
import net.sweenus.simplyswords.client.screen.RunicForgeScreen;
import net.sweenus.simplyswords.client.tooltip.UniqueTooltipExportController;
import net.sweenus.simplyswords.client.util.OracleIndexUtils;
import net.sweenus.simplyswords.registry.ParticlesRegistry;
import net.sweenus.simplyswords.registry.ScreenHandlerRegistry;

public class SimplySwordsClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SimplySwords.Client.initializeRegistryDependentClient();
        MenuRegistry.registerScreenFactory(
                ScreenHandlerRegistry.RUNIC_FORGE.get(),
                RunicForgeScreen::new
        );

        if (Platform.isModLoaded("oracle_index")) {
            OracleIndexUtils.init();
        }

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("simplyswords_client")
                        .then(ClientCommandManager.literal("export_unique_tooltips")
                                .executes(context -> UniqueTooltipExportController.start())
                                .then(ClientCommandManager.literal("cancel")
                                        .executes(context -> UniqueTooltipExportController.cancel())))));

        // Particle Factory must be registered on both loaders, not just in Common
        ParticleProviderRegistry.register(
                ParticlesRegistry.CUSTOM_BUBBLE.get(),
                CustomBubbleParticle.Factory::new);
        ParticleProviderRegistry.register(
                ParticlesRegistry.DRIPPING_BLOOD.get(),
                DrippingBloodParticle.Factory::new);

        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.CUSTOM_BUBBLE.get(), CustomBubbleParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.DRIPPING_BLOOD.get(), DrippingBloodParticle.Factory::new);
    }
}
