package net.sweenus.simplyswords.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class CustomBubbleParticle extends SpriteBillboardParticle {

    protected CustomBubbleParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider provider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.setSprite(provider);

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
            this.world.addParticle(ParticleTypes.BUBBLE_POP, this.x, this.y, this.z, 0, 0, 0); // No velocity for the pop
        }
    }




    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            CustomBubbleParticle particle = new CustomBubbleParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
            particle.setSprite(this.spriteProvider);
            return particle;
        }
    }
}

