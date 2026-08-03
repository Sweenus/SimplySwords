package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class HighOrbitingEffect extends StatusEffect {
    protected ParticleEffect particleType1 = null; // Default particle type
    protected ParticleEffect particleType2 = null;
    protected ParticleEffect particleType3 = null;
    protected ParticleEffect particleType4 = null;
    protected ParticleEffect particleType5 = null;
    protected float yOffset = 2;
    protected float width = 2;
    public HighOrbitingEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    private double currentAngle = 0.0;

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
            Vec3d center = livingEntity.getPos().add(0, (livingEntity.getHeight() + yOffset), 0); // Center around the entity's waist
            double speed = Math.PI / 8; // Control the speed of the orbit

            // Calculate the radius based on the amplifier
            double radius = width; // Increase the radius based on the amplifier
            // Increase the number of particles based on the amplifier to fill in the gaps
            int particleCount = (int) (5 + width); // Example: Increase particle count with the amplifier

            for (int i = 0; i < particleCount; i++) {
                // Calculate the angle for each particle around the circle
                double angle = 2 * Math.PI * i / particleCount;
                double x = center.x + radius * Math.cos(angle + currentAngle);
                double z = center.z + radius * Math.sin(angle + currentAngle);
                double y = center.y;

                // Spawn the particle at the calculated position
                spawnParticles(serverWorld, x, y, z);
            }

            // Increment the angle for the next application
            currentAngle += speed;
            if (currentAngle >= 2 * Math.PI) {
                currentAngle -= 2 * Math.PI;
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
    }

    private void spawnParticles(ServerWorld serverWorld, double x, double y, double z) {
        if (particleType1 != null)
            serverWorld.spawnParticles(particleType1, x, y, z, 1, 0, 0, 0, 0);
        if (particleType2 != null)
            serverWorld.spawnParticles(particleType2, x, y, z, 1, 0, 0, 0, 0);
        if (particleType3 != null)
            serverWorld.spawnParticles(particleType3, x, y, z, 1, 0, 0, 0, 0);
        if (particleType4 != null)
            serverWorld.spawnParticles(particleType4, x, y, z, 1, 0, 0, 0, 0);
        if (particleType5 != null)
            serverWorld.spawnParticles(particleType5, x, y, z, 1, 0, 0, 0, 0);
    }

    // Protected method to set the particle type
    protected void setParticleType1(ParticleEffect particleType) {
        this.particleType1 = particleType;
    }
    protected void setParticleType2(ParticleEffect particleType) {
        this.particleType2 = particleType;
    }
    protected void setParticleType3(ParticleEffect particleType) {
        this.particleType3 = particleType;
    }
    protected void setParticleType4(ParticleEffect particleType) {
        this.particleType4 = particleType;
    }
    protected void setParticleType5(ParticleEffect particleType) {
        this.particleType5 = particleType;
    }
    protected void setyOffset(float yOffset) {
        this.yOffset = yOffset;
    }
    protected void setWidth(float width) {
        this.width = width;
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }
}
