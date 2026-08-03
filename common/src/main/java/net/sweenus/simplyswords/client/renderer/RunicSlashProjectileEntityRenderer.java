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
import net.sweenus.simplyswords.entity.RunicSlashProjectileEntity;
import org.joml.Matrix4f;

public class RunicSlashProjectileEntityRenderer extends EntityRenderer<RunicSlashProjectileEntity> {
    private static final int ARC_SEGMENTS = 18;

    public RunicSlashProjectileEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(RunicSlashProjectileEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(RunicSlashProjectileEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z) || frustum.isVisible(entity.getBoundingBox().expand(Math.max(2.5, entity.getSlashWidth() * 2.0)));
    }

    @Override
    public void render(RunicSlashProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float age = entity.age + tickDelta;
        float pulse = MathHelper.sin(age * 0.85F) * 0.06F;
        float width = Math.max(0.1F, entity.getSlashWidth() + pulse);
        int alpha = MathHelper.clamp(225 - entity.age * 8, 85, 225);

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        int packedLight = Math.max(light, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        drawCrescent(vertices, matrix, width * 1.08F, width * 1.32F, width * 0.34F, 0.0F, 226, 248, 255, alpha, packedLight);
        drawCrescent(vertices, matrix, width * 0.74F, width * 0.9F, width * 0.16F, 0.03F, 145, 215, 255, alpha / 2, packedLight);
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void drawCrescent(VertexConsumer vertices, Matrix4f matrix, float radiusX, float radiusY, float thickness, float zOffset, int red, int green, int blue, int alpha, int light) {
        for (int segment = 0; segment < ARC_SEGMENTS; segment++) {
            float start = segment / (float) ARC_SEGMENTS;
            float end = (segment + 1) / (float) ARC_SEGMENTS;
            drawCrescentSegment(vertices, matrix, start, end, radiusX, radiusY, thickness, zOffset, red, green, blue, alpha, light);
        }
    }

    private static void drawCrescentSegment(VertexConsumer vertices, Matrix4f matrix, float start, float end, float radiusX, float radiusY, float thickness, float zOffset, int red, int green, int blue, int alpha, int light) {
        CrescentPoint a = pointOnCrescent(start, radiusX, radiusY, thickness, zOffset, alpha);
        CrescentPoint b = pointOnCrescent(end, radiusX, radiusY, thickness, zOffset, alpha);

        drawQuad(vertices, matrix, a.outerX, a.outerY, a.outerZ, b.outerX, b.outerY, b.outerZ, b.innerX, b.innerY, b.innerZ, a.innerX, a.innerY, a.innerZ, red, green, blue, Math.min(a.alpha, b.alpha), light);
        drawQuad(vertices, matrix, a.innerX, a.innerY, a.innerZ, b.innerX, b.innerY, b.innerZ, b.outerX, b.outerY, b.outerZ, a.outerX, a.outerY, a.outerZ, red, green, blue, Math.min(a.alpha, b.alpha), light);
    }

    private static CrescentPoint pointOnCrescent(float progress, float radiusX, float radiusY, float thickness, float zOffset, int alpha) {
        float angle = MathHelper.lerp(progress, -145.0F, 145.0F) * MathHelper.RADIANS_PER_DEGREE;
        float taper = (float) Math.pow(Math.max(0.0F, MathHelper.sin(progress * MathHelper.PI)), 0.45);
        float localThickness = Math.max(0.02F, thickness * taper);
        float centerX = MathHelper.sin(angle) * radiusX;
        float centerY = MathHelper.cos(angle) * radiusY - radiusY * 0.12F;
        float normalX = MathHelper.sin(angle);
        float normalY = MathHelper.cos(angle);
        float z = zOffset + (0.5F - Math.abs(progress - 0.5F)) * 0.18F;
        int pointAlpha = MathHelper.clamp((int) (alpha * MathHelper.clamp(taper * 1.35F, 0.25F, 1.0F)), 0, alpha);

        return new CrescentPoint(
                centerX + normalX * localThickness,
                centerY + normalY * localThickness,
                z,
                centerX - normalX * localThickness * 0.36F,
                centerY - normalY * localThickness * 0.36F,
                z + 0.02F,
                pointAlpha
        );
    }

    private static void drawQuad(VertexConsumer vertices, Matrix4f matrix,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 int red, int green, int blue, int alpha, int light) {
        vertices.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).light(light).next();
        vertices.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).light(light).next();
        vertices.vertex(matrix, x3, y3, z3).color(red, green, blue, alpha).light(light).next();
        vertices.vertex(matrix, x4, y4, z4).color(red, green, blue, alpha).light(light).next();
    }

    private record CrescentPoint(float outerX, float outerY, float outerZ, float innerX, float innerY, float innerZ, int alpha) {
    }
}
