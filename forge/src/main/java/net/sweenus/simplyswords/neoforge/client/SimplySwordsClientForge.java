package net.sweenus.simplyswords.neoforge.client;

import dev.architectury.platform.Platform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.particle.CustomBubbleParticle;
import net.sweenus.simplyswords.client.particle.DrippingBloodParticle;
import net.sweenus.simplyswords.client.screen.RunicForgeScreen;
import net.sweenus.simplyswords.client.util.OracleIndexUtils;
import net.sweenus.simplyswords.registry.ParticlesRegistry;
import net.sweenus.simplyswords.registry.ScreenHandlerRegistry;


@EventBusSubscriber(modid = SimplySwords.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SimplySwordsClientForge {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SimplySwords.Client.initializeRegistryDependentClient();
            if (Platform.isModLoaded("oracle_index")) {
                OracleIndexUtils.init();
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ScreenHandlerRegistry.RUNIC_FORGE.get(), RunicForgeScreen::new);
    }

    // Particle Factory must be registered on both loaders, not just in Common
    @SubscribeEvent
    public static void onRegisterParticleFactories(RegisterParticleProvidersEvent event) {
        ParticlesRegistry.CUSTOM_BUBBLE.ifPresent(bubble -> {
            event.registerSpriteSet(
                    bubble,
                    CustomBubbleParticle.Factory::new
            );
        });
        ParticlesRegistry.DRIPPING_BLOOD.ifPresent(drippingBlood -> {
            event.registerSpriteSet(
                    drippingBlood,
                    DrippingBloodParticle.Factory::new
            );
        });
    }

}
