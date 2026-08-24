package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.entity.TwistedBladeCrescendoVisualEntity;
import org.joml.Matrix4f;

public class TwistedBladeCrescendoVisualEntityRenderer
        extends EntityRenderer<TwistedBladeCrescendoVisualEntity> {

    private static final int ARC_SEGMENTS = 28;
    private static final int RING_SEGMENTS = 32;

    public TwistedBladeCrescendoVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(TwistedBladeCrescendoVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(TwistedBladeCrescendoVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(Math.max(3.0F, entity.getScale() * 1.8F)));
    }

    @Override
    public void render(TwistedBladeCrescendoVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float progress = MathHelper.clamp(
                (entity.age + tickDelta) / Math.max(1.0F, entity.getLifetime()),
                0.0F,
                1.0F
        );
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0);
        float fade = MathHelper.clamp((1.0F - progress) * 1.35F, 0.0F, 1.0F);
        float scale = entity.getScale() * MathHelper.lerp(eased, 0.62F, 1.12F);
        float mirror = entity.isMirrored() ? -1.0F : 1.0F;
        int packedLight = Math.max(light, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
        renderSlash(
                matrices,
                vertexConsumers,
                progress,
                scale,
                mirror,
                MathHelper.lerp(eased, -18.0F, 11.0F) * mirror,
                fade,
                packedLight
        );
        if (entity.isEmpowered()) {
            float secondProgress = MathHelper.clamp((progress - 0.10F) / 0.90F, 0.0F, 1.0F);
            float secondFade = MathHelper.clamp((1.0F - secondProgress) * 1.4F, 0.0F, 1.0F);
            renderSlash(
                    matrices,
                    vertexConsumers,
                    secondProgress,
                    scale * 1.08F,
                    -mirror,
                    MathHelper.lerp(secondProgress, 28.0F, -8.0F) * mirror,
                    secondFade,
                    packedLight
            );
            renderExpandingRing(
                    matrices,
                    vertexConsumers,
                    eased,
                    entity.getScale(),
                    entity.getGroundOffset(),
                    fade,
                    packedLight
            );
        }
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void renderSlash(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                    float progress, float scale, float mirror, float roll,
                                    float fade, int light) {
        if (progress <= 0.0F || fade <= 0.0F) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());

        drawSweepingCrescent(vertices, matrix, progress, scale * mirror, scale,
                0.28F * scale, 71, 24, 104, Math.round(185.0F * fade), light);
        drawSweepingCrescent(vertices, matrix, progress, scale * 0.965F * mirror, scale * 0.965F,
                0.16F * scale, 181, 99, 226, Math.round(225.0F * fade), light);
        drawSweepingCrescent(vertices, matrix, progress, scale * 0.92F * mirror, scale * 0.92F,
                0.065F * scale, 248, 238, 255, Math.round(255.0F * fade), light);
        matrices.pop();
    }

    private static void drawSweepingCrescent(VertexConsumer vertices, Matrix4f matrix,
                                              float progress, float radiusX, float radiusY,
                                              float thickness, int red, int green, int blue,
                                              int alpha, int light) {
        float head = MathHelper.clamp(progress * 1.32F, 0.0F, 1.0F);
        float tail = Math.max(0.0F, head - 0.62F);
        float forwardEdge = Math.min(1.0F, head + 0.10F);
        float visibleLength = Math.max(0.05F, forwardEdge - tail);

        for (int segment = 0; segment < ARC_SEGMENTS; segment++) {
            float start = segment / (float) ARC_SEGMENTS;
            float end = (segment + 1) / (float) ARC_SEGMENTS;
            if (end < tail || start > forwardEdge) {
                continue;
            }

            float localProgress = MathHelper.clamp(
                    ((start + end) * 0.5F - tail) / visibleLength,
                    0.0F,
                    1.0F
            );
            float trailFade = MathHelper.clamp(localProgress * 2.2F, 0.08F, 1.0F);
            float headFade = MathHelper.clamp((1.0F - localProgress) * 6.0F, 0.25F, 1.0F);
            int segmentAlpha = Math.max(1, Math.round(alpha * trailFade * headFade));
            drawCrescentSegment(vertices, matrix, start, end, radiusX, radiusY, thickness,
                    red, green, blue, segmentAlpha, light);
        }
    }

    private static void drawCrescentSegment(VertexConsumer vertices, Matrix4f matrix,
                                            float start, float end, float radiusX, float radiusY,
                                            float thickness, int red, int green, int blue,
                                            int alpha, int light) {
        CrescentPoint a = pointOnCrescent(start, radiusX, radiusY, thickness);
        CrescentPoint b = pointOnCrescent(end, radiusX, radiusY, thickness);
        drawDoubleSidedQuad(vertices, matrix,
                a.outerX, a.outerY, a.z,
                b.outerX, b.outerY, b.z,
                b.innerX, b.innerY, b.z + 0.018F,
                a.innerX, a.innerY, a.z + 0.018F,
                red, green, blue, alpha, light);
    }

    private static CrescentPoint pointOnCrescent(float progress, float radiusX,
                                                 float radiusY, float thickness) {
        float angle = MathHelper.lerp(progress, -138.0F, 138.0F) * MathHelper.RADIANS_PER_DEGREE;
        float taper = (float) Math.pow(
                Math.max(0.0F, MathHelper.sin(progress * MathHelper.PI)),
                0.42
        );
        float localThickness = Math.max(0.012F, thickness * taper);
        float centerX = MathHelper.sin(angle) * radiusX;
        float centerY = MathHelper.cos(angle) * radiusY - radiusY * 0.10F;
        float normalX = MathHelper.sin(angle) * Math.signum(radiusX);
        float normalY = MathHelper.cos(angle);
        float z = (0.5F - Math.abs(progress - 0.5F)) * 0.12F;

        return new CrescentPoint(
                centerX + normalX * localThickness,
                centerY + normalY * localThickness,
                centerX - normalX * localThickness * 0.35F,
                centerY - normalY * localThickness * 0.35F,
                z
        );
    }

    private static void renderExpandingRing(MatrixStack matrices,
                                            VertexConsumerProvider vertexConsumers,
                                            float progress, float scale, float groundOffset,
                                            float fade, int light) {
        if (fade <= 0.0F) {
            return;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        float radius = scale * MathHelper.lerp(progress, 0.28F, 1.08F);
        float width = Math.max(0.025F, scale * 0.085F * (1.0F - progress * 0.55F));
        float y = -groundOffset + 0.08F + progress * 0.15F;
        int alpha = Math.round(205.0F * fade);

        for (int segment = 0; segment < RING_SEGMENTS; segment++) {
            float start = MathHelper.TAU * segment / RING_SEGMENTS;
            float end = MathHelper.TAU * (segment + 1) / RING_SEGMENTS;
            drawRingSegment(vertices, matrix, start, end, radius, width, y,
                    199, 116, 238, alpha, light);
        }
    }

    private static void drawRingSegment(VertexConsumer vertices, Matrix4f matrix,
                                        float start, float end, float radius, float width, float y,
                                        int red, int green, int blue, int alpha, int light) {
        float inner = Math.max(0.01F, radius - width);
        float x1 = MathHelper.cos(start) * radius;
        float z1 = MathHelper.sin(start) * radius;
        float x2 = MathHelper.cos(end) * radius;
        float z2 = MathHelper.sin(end) * radius;
        float x3 = MathHelper.cos(end) * inner;
        float z3 = MathHelper.sin(end) * inner;
        float x4 = MathHelper.cos(start) * inner;
        float z4 = MathHelper.sin(start) * inner;
        drawDoubleSidedQuad(vertices, matrix,
                x1, y, z1,
                x2, y, z2,
                x3, y, z3,
                x4, y, z4,
                red, green, blue, alpha, light);
    }

    private static void drawDoubleSidedQuad(VertexConsumer vertices, Matrix4f matrix,
                                            float x1, float y1, float z1,
                                            float x2, float y2, float z2,
                                            float x3, float y3, float z3,
                                            float x4, float y4, float z4,
                                            int red, int green, int blue, int alpha, int light) {
        vertex(vertices, matrix, x1, y1, z1, red, green, blue, alpha, light);
        vertex(vertices, matrix, x2, y2, z2, red, green, blue, alpha, light);
        vertex(vertices, matrix, x3, y3, z3, red, green, blue, alpha, light);
        vertex(vertices, matrix, x4, y4, z4, red, green, blue, alpha, light);
        vertex(vertices, matrix, x4, y4, z4, red, green, blue, alpha, light);
        vertex(vertices, matrix, x3, y3, z3, red, green, blue, alpha, light);
        vertex(vertices, matrix, x2, y2, z2, red, green, blue, alpha, light);
        vertex(vertices, matrix, x1, y1, z1, red, green, blue, alpha, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix,
                               float x, float y, float z,
                               int red, int green, int blue, int alpha, int light) {
        vertices.vertex(matrix, x, y, z).color(red, green, blue, alpha).light(light);
    }

    private record CrescentPoint(float outerX, float outerY,
                                 float innerX, float innerY, float z) {
    }
}
