package net.sweenus.simplyswords.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class CustomBubbleParticle extends BillboardParticle {

    protected CustomBubbleParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider provider) {
        super(world, x, y, z, provider.getSprite(world.random));

        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;

        this.scale = 0.1f;
        this.maxAge = 60 + this.random.nextInt(20);
        this.collidesWithWorld = false;
    }

    @Override
    public void tick() {
        super.tick();

        // Bubble Slow with age
        if (this.age > this.maxAge * 0.5f) {
            float lifeFactor = (float) (this.maxAge - this.age) / (this.maxAge * 0.5f);
            this.velocityX *= lifeFactor;
            this.velocityY *= lifeFactor;
            this.velocityZ *= lifeFactor;
        }

        // Bubble Rise
        this.velocityY += 0.003;

        // Bubble Fade
        if (this.age > this.maxAge - 10) {
            this.alpha = Math.max(0.1f, this.alpha - 0.1f);
        } else {
            this.alpha = 1.0f;
        }

        // Bubble Pop
        if (this.age >= this.maxAge - 1) {
            // No public client-world overload remains here in 1.21.11; keep visual fade without explicit pop spawn.
        }
    }

    @Override
    public BillboardParticle.RenderType getRenderType() {
        return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, Random random) {
            CustomBubbleParticle particle = new CustomBubbleParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
            particle.sprite = this.spriteProvider.getSprite(world.random);
            return particle;
        }
    }
}
