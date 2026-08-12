package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplyswords.SimplySwords;


public class ParticlesRegistry {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.PARTICLE_TYPE);

    public static final RegistrySupplier<SimpleParticleType> CUSTOM_BUBBLE = PARTICLES.register(
            "custom_bubble",
            () -> new SimpleParticleType(true) {}
    );

    public static final RegistrySupplier<SimpleParticleType> DRIPPING_BLOOD = PARTICLES.register(
            "dripping_blood",
            () -> new SimpleParticleType(true) {}
    );

    public static final RegistrySupplier<SimpleParticleType> BLOOD_SPRAY = PARTICLES.register(
            "blood_spray",
            () -> new SimpleParticleType(true) {}
    );

    public static void registerParticles() {
        PARTICLES.register();
    }
}
