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
import net.sweenus.simplyswords.entity.BloodPlagueSpreadVisualEntity;
import org.joml.Matrix4f;

public class BloodPlagueSpreadVisualEntityRenderer extends EntityRenderer<BloodPlagueSpreadVisualEntity> {
    private static final Identifier WHITE_TEXTURE = Identifier.ofVanilla("textures/misc/white.png");
    private static final int RING_SEGMENTS = 64;
    private static final float RING_DURATION = 8.0F;

    public BloodPlagueSpreadVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(BloodPlagueSpreadVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(BloodPlagueSpreadVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(entity.getRadius() + 1.0F));
    }

    @Override
    public void render(BloodPlagueSpreadVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        float age = entity.age + tickDelta;
        var target = entity.getWorld().getEntityById(entity.getTargetId());
        float startRadius = target == null
                ? 0.55F
                : Math.min(entity.getRadius(), Math.max(0.5F, target.getWidth() * 0.82F));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        VertexConsumer dark = consumers.getBuffer(RenderLayer.getDebugQuads());
        renderRing(dark, matrix, entity, age, startRadius, false);

        VertexConsumer glow = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        renderRing(glow, matrix, entity, age, startRadius, true);
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static void renderRing(VertexConsumer vertices, Matrix4f matrix,
                                   BloodPlagueSpreadVisualEntity entity, float age,
                                   float startRadius, boolean emissive) {
        if (age < 0.0F || age > RING_DURATION) {
            return;
        }
        float progress = MathHelper.clamp(age / RING_DURATION, 0.0F, 1.0F);
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0);
        float fade = 1.0F - progress;
        float radius = MathHelper.lerp(eased, startRadius, entity.getRadius());
        float width = Math.max(0.025F, 0.12F * (1.0F - progress * 0.66F));
        float y = 0.06F + progress * 0.10F;
        int alpha = Math.round((emissive ? 218.0F : 168.0F) * fade);
        drawRing(vertices, matrix, radius, emissive ? width * 0.58F : width, y,
                emissive ? 0xF02B3D : 0x39030B, alpha, emissive);
    }

    private static void drawRing(VertexConsumer vertices, Matrix4f matrix,
                                 float radius, float width, float y,
                                 int color, int alpha, boolean emissive) {
        float inner = Math.max(0.01F, radius - width);
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        for (int segment = 0; segment < RING_SEGMENTS; segment++) {
            float start = MathHelper.TAU * segment / RING_SEGMENTS;
            float end = MathHelper.TAU * (segment + 1) / RING_SEGMENTS;
            float x1 = MathHelper.cos(start) * radius;
            float z1 = MathHelper.sin(start) * radius;
            float x2 = MathHelper.cos(end) * radius;
            float z2 = MathHelper.sin(end) * radius;
            float x3 = MathHelper.cos(end) * inner;
            float z3 = MathHelper.sin(end) * inner;
            float x4 = MathHelper.cos(start) * inner;
            float z4 = MathHelper.sin(start) * inner;
            vertex(vertices, matrix, x1, y, z1, red, green, blue, alpha, emissive);
            vertex(vertices, matrix, x2, y, z2, red, green, blue, alpha, emissive);
            vertex(vertices, matrix, x3, y, z3, red, green, blue, alpha, emissive);
            vertex(vertices, matrix, x4, y, z4, red, green, blue, alpha, emissive);
            vertex(vertices, matrix, x4, y, z4, red, green, blue, alpha, emissive);
            vertex(vertices, matrix, x3, y, z3, red, green, blue, alpha, emissive);
            vertex(vertices, matrix, x2, y, z2, red, green, blue, alpha, emissive);
            vertex(vertices, matrix, x1, y, z1, red, green, blue, alpha, emissive);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix,
                               float x, float y, float z,
                               int red, int green, int blue, int alpha, boolean emissive) {
        var vertex = vertices.vertex(matrix, x, y, z).color(red, green, blue, alpha);
        if (emissive) {
            vertex.texture(0.5F, 0.5F)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                    .normal(0.0F, 1.0F, 0.0F);
        } else {
            vertex.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
    }
}
