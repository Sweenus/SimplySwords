package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplyswords.SimplySwords;


public class ParticlesRegistry {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.PARTICLE_TYPE);

    public static final RegistrySupplier<DefaultParticleType> CUSTOM_BUBBLE = PARTICLES.register(
            "custom_bubble",
            () -> new DefaultParticleType(true) {}
    );

    public static final RegistrySupplier<DefaultParticleType> DRIPPING_BLOOD = PARTICLES.register(
            "dripping_blood",
            () -> new DefaultParticleType(true) {}
    );

    public static void registerParticles() {
        PARTICLES.register();
    }
}
