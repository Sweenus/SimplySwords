package net.sweenus.simplyswords.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.particle.CustomBubbleParticle;
import net.sweenus.simplyswords.registry.ParticlesRegistry;


@EventBusSubscriber(modid = SimplySwords.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SimplySwordsClientForge {

    // Particle Factory must be registered on both loaders, not just in Common
    @SubscribeEvent
    public static void onRegisterParticleFactories(RegisterParticleProvidersEvent event) {
        ParticlesRegistry.CUSTOM_BUBBLE.ifPresent(bubble -> {
            event.registerSpriteSet(
                    bubble,
                    CustomBubbleParticle.Factory::new
            );
        });
    }

}
