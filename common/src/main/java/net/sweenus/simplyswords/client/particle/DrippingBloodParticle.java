package net.sweenus.simplyswords.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class DrippingBloodParticle extends SpriteBillboardParticle {

    protected DrippingBloodParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider provider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.setSprite(provider);
        this.setColor(0.42F + this.random.nextFloat() * 0.08F, 0.01F, 0.01F);
        this.setAlpha(0.95F);
        this.scale = 0.075F + this.random.nextFloat() * 0.035F;
        this.maxAge = 18 + this.random.nextInt(12);
        this.collidesWithWorld = true;
        this.gravityStrength = 0.085F;
        this.velocityX = velocityX * 0.45;
        this.velocityY = -0.035 - Math.abs(velocityY) * 0.45 - this.random.nextDouble() * 0.025;
        this.velocityZ = velocityZ * 0.45;
    }

    @Override
    public void tick() {
        super.tick();
        this.velocityX *= 0.86;
        this.velocityZ *= 0.86;

        if (this.onGround) {
            this.velocityX *= 0.3;
            this.velocityZ *= 0.3;
            if (this.age < this.maxAge - 4) {
                this.maxAge = this.age + 4;
            }
        }

        if (this.age > this.maxAge - 6) {
            this.setAlpha(Math.max(0.0F, (this.maxAge - this.age) / 6.0F));
            this.scale *= 0.9F;
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new DrippingBloodParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
