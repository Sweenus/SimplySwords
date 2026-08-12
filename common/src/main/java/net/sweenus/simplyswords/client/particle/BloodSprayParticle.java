package net.sweenus.simplyswords.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class BloodSprayParticle extends SpriteBillboardParticle {
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
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        Vec3d cameraPos = camera.getPos();
        float x = (float) (MathHelper.lerp((double) tickDelta, this.prevPosX, this.x) - cameraPos.getX());
        float y = (float) (MathHelper.lerp((double) tickDelta, this.prevPosY, this.y) - cameraPos.getY());
        float z = (float) (MathHelper.lerp((double) tickDelta, this.prevPosZ, this.z) - cameraPos.getZ());

        Quaternionf rotation;
        if (this.pooled) {
            rotation = new Quaternionf().rotationX((float) (Math.PI * 0.5));
        } else if (this.angle == 0.0F) {
            rotation = camera.getRotation();
        } else {
            rotation = new Quaternionf(camera.getRotation());
            rotation.rotateZ(MathHelper.lerp(tickDelta, this.prevAngle, this.angle));
        }

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };
        float size = this.getSize(tickDelta);
        for (Vector3f corner : corners) {
            corner.rotate(rotation);
            corner.mul(size);
            corner.add(x, y, z);
        }

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();
        int light = this.getBrightness(tickDelta);
        vertexConsumer.vertex(corners[0].x(), corners[0].y(), corners[0].z())
                .texture(maxU, maxV).color(this.red, this.green, this.blue, this.alpha).light(light).next();
        vertexConsumer.vertex(corners[1].x(), corners[1].y(), corners[1].z())
                .texture(maxU, minV).color(this.red, this.green, this.blue, this.alpha).light(light).next();
        vertexConsumer.vertex(corners[2].x(), corners[2].y(), corners[2].z())
                .texture(minU, minV).color(this.red, this.green, this.blue, this.alpha).light(light).next();
        vertexConsumer.vertex(corners[3].x(), corners[3].y(), corners[3].z())
                .texture(minU, maxV).color(this.red, this.green, this.blue, this.alpha).light(light).next();
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
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new BloodSprayParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
