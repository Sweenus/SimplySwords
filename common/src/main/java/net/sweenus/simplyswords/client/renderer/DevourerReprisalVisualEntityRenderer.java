package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.entity.DevourerReprisalVisualEntity;
import org.joml.Matrix4f;

public final class DevourerReprisalVisualEntityRenderer extends EntityRenderer<DevourerReprisalVisualEntity> {
    private static final Identifier WHITE = Identifier.ofVanilla("textures/misc/white.png");
    private static final float CELL = 0.5F;
    private static final int EYE_COUNT = 12;
    private static final int TENDRIL_COUNT = 8;
    private static final int TENDRIL_SEGMENTS = 5;

    public DevourerReprisalVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(DevourerReprisalVisualEntity entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(DevourerReprisalVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getVisibilityBoundingBox());
    }

    @Override
    public void render(DevourerReprisalVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        float age = entity.age + tickDelta;
        float open = ease(MathHelper.clamp(age / 3.0F, 0.0F, 1.0F));
        float close = ease(MathHelper.clamp((age - (entity.getLifetime() - 5.0F)) / 5.0F, 0.0F, 1.0F));
        if (open <= 0.001F || close >= 0.999F) {
            return;
        }
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer body = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
        renderPlates(entity, matrix, body, light, open, close, false);
        renderTendrils(entity, matrix, body, light, open, close, false);
        renderEyes(entity, matrix, body, light, age, open, close, false);

        VertexConsumer glow = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE));
        renderTendrils(entity, matrix, glow, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                open, close, true);
        renderEyes(entity, matrix, glow, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                age, open, close, true);
        renderClosingRing(entity, matrix, glow, close);
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static void renderPlates(DevourerReprisalVisualEntity entity, Matrix4f matrix,
                                     VertexConsumer vertices, int light,
                                     float open, float close, boolean emissive) {
        float radius = entity.getRadius() * 0.84F;
        float reach = radius * (0.24F + open * 0.76F);
        float tileScale = Math.max(0.06F, 1.0F - close * close);
        int extent = MathHelper.ceil(radius / CELL) + 1;
        for (int x = -extent; x <= extent; x++) {
            for (int z = -extent; z <= extent; z++) {
                float centerX = x * CELL;
                float centerZ = z * CELL;
                float distance = MathHelper.sqrt(centerX * centerX + centerZ * centerZ);
                long hash = mix(entity.getSeed(), x, z);
                float edge = 0.78F + noise(hash) * 0.24F;
                if (distance > reach * edge || distance < 0.22F + noise(hash + 17L) * 0.3F) {
                    continue;
                }
                float reveal = ease(MathHelper.clamp((reach * edge - distance) / CELL, 0.0F, 1.0F));
                float half = CELL * 0.49F * reveal * tileScale;
                if (half <= 0.01F) {
                    continue;
                }
                float y = 0.012F + noise(hash + 41L) * 0.018F;
                int shade = Math.round(noise(hash + 83L) * 18.0F);
                int red = 20 + shade;
                int green = 4 + shade / 5;
                int blue = 38 + shade * 2;
                drawHorizontalRect(vertices, matrix, centerX, y, centerZ,
                        half, half, red, green, blue, 255, light, emissive);
                if ((hash & 3L) == 0L) {
                    float patchHalf = half * (0.22F + noise(hash + 131L) * 0.24F);
                    float patchX = centerX + (noise(hash + 149L) - 0.5F) * half;
                    float patchZ = centerZ + (noise(hash + 173L) - 0.5F) * half;
                    drawHorizontalRect(vertices, matrix, patchX, y + 0.004F, patchZ,
                            patchHalf, patchHalf * 0.58F,
                            Math.max(9, red - 10), Math.max(2, green - 2), Math.max(20, blue - 12),
                            255, light, emissive);
                }
            }
        }
    }

    private static void renderTendrils(DevourerReprisalVisualEntity entity, Matrix4f matrix,
                                       VertexConsumer vertices, int light,
                                       float open, float close, boolean glow) {
        float radius = entity.getRadius() * 0.92F;
        float feedBoost = entity.isFed() ? 1.12F : 1.0F;
        for (int tendril = 0; tendril < TENDRIL_COUNT; tendril++) {
            float angle = tendril * MathHelper.TAU / TENDRIL_COUNT
                    + (noise(entity.getSeed() + tendril * 47L) - 0.5F) * 0.18F;
            Vec3d forward = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
            Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
            for (int segment = 0; segment < TENDRIL_SEGMENTS; segment++) {
                float order = segment / (float) TENDRIL_SEGMENTS;
                float reveal = ease(MathHelper.clamp((open - order * 0.58F) / 0.42F, 0.0F, 1.0F));
                float retractOrder = (TENDRIL_SEGMENTS - 1 - segment) / (float) TENDRIL_SEGMENTS;
                float retract = 1.0F - ease(MathHelper.clamp((close - retractOrder * 0.42F) / 0.58F, 0.0F, 1.0F));
                float visibility = reveal * retract;
                if (visibility <= 0.01F) {
                    continue;
                }
                float distance = radius * (0.2F + segment * 0.155F);
                float stagger = (noise(entity.getSeed() + tendril * 91L + segment * 13L) - 0.5F) * 0.26F;
                Vec3d center = forward.multiply(distance).add(right.multiply(stagger));
                float y = 0.055F + MathHelper.sin(open * MathHelper.PI + segment * 0.7F) * 0.11F * visibility;
                float halfLength = radius * 0.095F * visibility;
                float halfWidth = (glow ? 0.035F : 0.115F) * feedBoost * visibility;
                int red = glow ? (entity.isFed() ? 186 : 129) : 25;
                int green = glow ? (entity.isFed() ? 103 : 61) : 6;
                int blue = glow ? 255 : 52;
                int alpha = glow ? Math.round(215.0F * visibility) : 255;
                drawOrientedRect(vertices, matrix, center.x, y, center.z, forward, right,
                        halfLength, halfWidth, red, green, blue, alpha, light, glow);
            }
        }
    }

    private static void renderEyes(DevourerReprisalVisualEntity entity, Matrix4f matrix,
                                   VertexConsumer vertices, int light, float age,
                                   float open, float close, boolean glow) {
        float ring = Math.min(entity.getRadius() * 0.52F, 1.85F) * (0.72F + open * 0.28F);
        float fade = 1.0F - close;
        for (int i = 0; i < EYE_COUNT; i++) {
            float angle = i * MathHelper.TAU / EYE_COUNT
                    + (noise(entity.getSeed() + i * 101L) - 0.5F) * 0.2F;
            float variation = 0.86F + noise(entity.getSeed() + i * 131L) * 0.28F;
            Vec3d radial = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
            Vec3d tangent = new Vec3d(-radial.z, 0.0, radial.x);
            Vec3d center = radial.multiply(ring * variation).add(0.0, 0.065, 0.0);
            float blink = eyeOpening(age, entity.getSeed(), i) * open * fade;
            float halfWidth = (0.18F + noise(entity.getSeed() + i * 173L) * 0.07F) * open;
            float halfHeight = Math.max(0.012F, halfWidth * 0.42F * blink);
            if (glow) {
                int red = entity.isFed() ? 255 : 225;
                int green = entity.isFed() ? 188 : 134;
                int alpha = Math.round((entity.isFed() ? 250.0F : 225.0F) * fade);
                drawHorizontalDiamond(vertices, matrix, center.add(0.0, 0.008, 0.0), tangent, radial,
                        halfWidth * 0.77F, halfHeight * 0.72F,
                        red, green, 32, alpha, light, true);
            } else {
                drawHorizontalDiamond(vertices, matrix, center, tangent, radial,
                        halfWidth, Math.max(0.03F, halfWidth * 0.58F),
                        7, 2, 14, 255, light, false);
                drawHorizontalDiamond(vertices, matrix, center.add(0.0, 0.004, 0.0), tangent, radial,
                        halfWidth * 0.82F, halfHeight,
                        176, 104, 19, 255, light, false);
                drawOrientedRect(vertices, matrix, center.x, center.y + 0.009, center.z,
                        radial, tangent, halfHeight * 0.72F,
                        Math.max(0.009F, halfWidth * 0.075F),
                        6, 2, 12, 255, light, false);
            }
        }
    }

    private static void renderClosingRing(DevourerReprisalVisualEntity entity,
                                          Matrix4f matrix, VertexConsumer vertices, float close) {
        if (close <= 0.001F) {
            return;
        }
        float radius = MathHelper.lerp(ease(close), 0.55F, entity.getRadius() * 1.08F);
        float width = Math.max(0.018F, 0.13F * (1.0F - close));
        int alpha = Math.round(235.0F * (1.0F - close));
        int red = entity.isFed() ? 204 : 152;
        int green = entity.isFed() ? 118 : 67;
        drawHorizontalRect(vertices, matrix, 0.0F, 0.14F, -radius,
                radius, width, red, green, 255, alpha,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, true);
        drawHorizontalRect(vertices, matrix, 0.0F, 0.14F, radius,
                radius, width, red, green, 255, alpha,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, true);
        drawHorizontalRect(vertices, matrix, -radius, 0.14F, 0.0F,
                width, radius, red, green, 255, alpha,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, true);
        drawHorizontalRect(vertices, matrix, radius, 0.14F, 0.0F,
                width, radius, red, green, 255, alpha,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, true);
    }

    private static float eyeOpening(float age, int seed, int index) {
        float phase = noise(seed + index * 211L) * 9.0F;
        float blink = MathHelper.sin(age * 0.32F + phase);
        return blink > 0.78F ? Math.max(0.12F, 1.0F - (blink - 0.78F) * 4.5F) : 1.0F;
    }

    private static void drawHorizontalRect(VertexConsumer vertices, Matrix4f matrix,
                                           double x, double y, double z,
                                           float halfX, float halfZ,
                                           int red, int green, int blue, int alpha,
                                           int light, boolean emissive) {
        putVertex(vertices, matrix, x - halfX, y, z - halfZ,
                red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, x - halfX, y, z + halfZ,
                red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, x + halfX, y, z + halfZ,
                red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, x + halfX, y, z - halfZ,
                red, green, blue, alpha, light, emissive);
    }

    private static void drawOrientedRect(VertexConsumer vertices, Matrix4f matrix,
                                         double x, double y, double z,
                                         Vec3d forward, Vec3d right,
                                         float halfLength, float halfWidth,
                                         int red, int green, int blue, int alpha,
                                         int light, boolean emissive) {
        Vec3d center = new Vec3d(x, y, z);
        Vec3d length = forward.multiply(halfLength);
        Vec3d width = right.multiply(halfWidth);
        putVertex(vertices, matrix, center.subtract(length).subtract(width),
                red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, center.subtract(length).add(width),
                red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, center.add(length).add(width),
                red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, center.add(length).subtract(width),
                red, green, blue, alpha, light, emissive);
    }

    private static void drawHorizontalDiamond(VertexConsumer vertices, Matrix4f matrix,
                                              Vec3d center, Vec3d right, Vec3d forward,
                                              float halfWidth, float halfHeight,
                                              int red, int green, int blue, int alpha,
                                              int light, boolean emissive) {
        putVertex(vertices, matrix, center.subtract(right.multiply(halfWidth)),
                red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, center.add(forward.multiply(halfHeight)),
                red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, center.add(right.multiply(halfWidth)),
                red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, center.subtract(forward.multiply(halfHeight)),
                red, green, blue, alpha, light, emissive);
    }

    private static void putVertex(VertexConsumer vertices, Matrix4f matrix, Vec3d position,
                                  int red, int green, int blue, int alpha,
                                  int light, boolean emissive) {
        putVertex(vertices, matrix, position.x, position.y, position.z,
                red, green, blue, alpha, light, emissive);
    }

    private static void putVertex(VertexConsumer vertices, Matrix4f matrix,
                                  double x, double y, double z,
                                  int red, int green, int blue, int alpha,
                                  int light, boolean emissive) {
        vertices.vertex(matrix, (float) x, (float) y, (float) z)
                .color(red, green, blue, alpha)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(emissive ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light)
                .normal(0.0F, 1.0F, 0.0F);
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static long mix(int seed, int x, int z) {
        long value = seed;
        value ^= (long) x * 0x9E3779B97F4A7C15L;
        value ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        value ^= value >>> 29;
        value *= 0x165667B19E3779F9L;
        return value ^ value >>> 32;
    }

    private static float noise(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return (mixed & 0xFFFFFFL) / (float) 0x1000000;
    }
}
