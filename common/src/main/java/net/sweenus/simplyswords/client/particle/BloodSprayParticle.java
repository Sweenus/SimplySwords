package net.sweenus.simplyswords.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class BloodSprayParticle extends SpriteBillboardParticle {
    private static final BillboardParticle.Rotator GROUND_ROTATOR =
            (Quaternionf rotation, net.minecraft.client.render.Camera camera, float tickDelta) ->
                    rotation.rotationX((float) (Math.PI * 0.5));

    private boolean pooled;
    private int pooledAge;
    private final float poolScale;

    protected BloodSprayParticle(ClientWorld world, double x, double y, double z,
                                 double velocityX, double velocityY, double velocityZ,
                                 SpriteProvider provider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.setSprite(provider);
        this.setColor(0.48F + this.random.nextFloat() * 0.12F,
                0.004F + this.random.nextFloat() * 0.012F,
                0.008F + this.random.nextFloat() * 0.012F);
        this.setAlpha(0.98F);
        this.scale = 0.085F + this.random.nextFloat() * 0.055F;
        this.poolScale = 0.16F + this.random.nextFloat() * 0.11F;
        this.maxAge = 200;
        this.collidesWithWorld = true;
        this.gravityStrength = 1.55F;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.dead) {
            return;
        }

        if (!this.pooled) {
            this.velocityX *= 0.965;
            this.velocityZ *= 0.965;
            if (this.onGround) {
                this.pooled = true;
                this.pooledAge = 0;
                this.collidesWithWorld = false;
                this.gravityStrength = 0.0F;
                this.velocityX = 0.0;
                this.velocityY = 0.0;
                this.velocityZ = 0.0;
                this.y += 0.012;
                this.prevPosY = this.y;
                this.maxAge = this.age + 80;
                this.scale = this.poolScale * 0.55F;
                this.angle = this.random.nextFloat() * (float) (Math.PI * 2.0);
                this.prevAngle = this.angle;
            }
            return;
        }

        this.pooledAge++;
        this.x = this.prevPosX;
        this.y = this.prevPosY;
        this.z = this.prevPosZ;
        this.scale += (this.poolScale - this.scale) * 0.18F;
        int remaining = this.maxAge - this.age;
        if (remaining < 20) {
            float fade = Math.max(0.0F, remaining / 20.0F);
            this.setAlpha(fade * 0.82F);
            this.scale *= 0.985F;
        } else {
            this.setAlpha(Math.min(0.82F, 0.46F + this.pooledAge * 0.035F));
        }
    }

    @Override
    public BillboardParticle.Rotator getRotator() {
        return this.pooled ? GROUND_ROTATOR : BillboardParticle.Rotator.ALL_AXIS;
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
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new BloodSprayParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
