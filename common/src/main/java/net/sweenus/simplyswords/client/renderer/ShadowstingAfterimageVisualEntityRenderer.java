package net.sweenus.simplyswords.client.renderer;

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
import net.sweenus.simplyswords.entity.ShadowstingAfterimageVisualEntity;
import org.joml.Matrix4f;

public class ShadowstingAfterimageVisualEntityRenderer extends EntityRenderer<ShadowstingAfterimageVisualEntity> {

    public ShadowstingAfterimageVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(ShadowstingAfterimageVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(ShadowstingAfterimageVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float progress = MathHelper.clamp((entity.age + tickDelta) / Math.max(1.0F, entity.getLifetime()), 0.0F, 1.0F);
        float fade = 1.0F - progress;
        if (fade <= 0.01F) {
            return;
        }

        int alpha = MathHelper.clamp((int) (145.0F * fade), 0, 145);
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getVisualYaw()));
        matrices.translate(0.0, 0.03 * MathHelper.sin((entity.age + tickDelta) * 0.55F), 0.0);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        drawBox(vertices, matrix, -0.18F, 0.78F, -0.10F, 0.18F, 1.38F, 0.10F, alpha);
        drawBox(vertices, matrix, -0.16F, 1.42F, -0.16F, 0.16F, 1.74F, 0.16F, alpha);
        drawBox(vertices, matrix, -0.43F, 0.76F, -0.08F, -0.24F, 1.33F, 0.08F, alpha * 3 / 4);
        drawBox(vertices, matrix, 0.24F, 0.76F, -0.08F, 0.43F, 1.33F, 0.08F, alpha * 3 / 4);
        drawBox(vertices, matrix, -0.18F, 0.05F, -0.08F, -0.03F, 0.78F, 0.08F, alpha * 3 / 4);
        drawBox(vertices, matrix, 0.03F, 0.05F, -0.08F, 0.18F, 0.78F, 0.08F, alpha * 3 / 4);

        matrices.pop();
    }

    private static void drawBox(VertexConsumer vertices, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int alpha) {
        if (alpha <= 0) {
            return;
        }

        int red = 18;
        int green = 14;
        int blue = 30;
        quad(vertices, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        quad(vertices, matrix, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        quad(vertices, matrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
        quad(vertices, matrix, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        quad(vertices, matrix, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        quad(vertices, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        vertices.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
        vertices.vertex(matrix, x3, y3, z3).color(red, green, blue, alpha);
        vertices.vertex(matrix, x4, y4, z4).color(red, green, blue, alpha);
    }
}
