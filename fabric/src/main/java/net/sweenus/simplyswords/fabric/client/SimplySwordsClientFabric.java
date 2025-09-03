package net.sweenus.simplyswords.fabric.client;

import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.sweenus.simplyswords.client.particle.CustomBubbleParticle;
import net.sweenus.simplyswords.registry.ParticlesRegistry;

public class SimplySwordsClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        // Particle Factory must be registered on both loaders, not just in Common
        ParticleProviderRegistry.register(
                ParticlesRegistry.CUSTOM_BUBBLE.get(),
                CustomBubbleParticle.Factory::new);

        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.CUSTOM_BUBBLE.get(), CustomBubbleParticle.Factory::new);
    }
}
