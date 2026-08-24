package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.WhisperwindSlashVisualEntity;
import org.joml.Matrix4f;

public class WhisperwindSlashVisualEntityRenderer extends EntityRenderer<WhisperwindSlashVisualEntity> {

    public WhisperwindSlashVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(WhisperwindSlashVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(WhisperwindSlashVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(Math.max(4.0, entity.getSlashLength())));
    }

    @Override
    public void render(WhisperwindSlashVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        Vec3d end = new Vec3d(entity.getEndOffsetX(), entity.getEndOffsetY(), entity.getEndOffsetZ());
        double length = end.length();
        if (length < 0.05) {
            return;
        }

        float progress = MathHelper.clamp((entity.age + tickDelta) / Math.max(1.0F, entity.getLifetime()), 0.0F, 1.0F);
        float fade = 1.0F - progress;
        Vec3d direction = end.normalize();
        Vec3d head = end.multiply(progress);
        Vec3d tail = head.subtract(direction.multiply(Math.min(length, 2.4)));
        Vec3d side = new Vec3d(-direction.z, 0.0, direction.x).normalize().multiply(0.28 + 0.08 * fade);
        double baseY = 1.05 + 0.2 * MathHelper.sin(progress * MathHelper.PI);
        double height = 0.46 + 0.28 * fade;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        int alpha = MathHelper.clamp((int) (210.0F * fade), 0, 210);
        drawRibbon(vertices, matrix, tail, head, side, baseY, height, 246, 232, 255, alpha);
        drawRibbon(vertices, matrix, tail.subtract(direction.multiply(0.65)), head.subtract(direction.multiply(0.45)), side.multiply(0.55), baseY - 0.08, height * 0.62, 255, 173, 224, alpha / 2);
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix, Vec3d tail, Vec3d head, Vec3d side, double baseY, double height, int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }

        Vec3d tailLeft = tail.add(side);
        Vec3d tailRight = tail.subtract(side);
        Vec3d headLeft = head.add(side);
        Vec3d headRight = head.subtract(side);
        vertices.vertex(matrix, (float) tailLeft.x, (float) baseY, (float) tailLeft.z).color(red, green, blue, alpha);
        vertices.vertex(matrix, (float) tailRight.x, (float) (baseY + height * 0.35), (float) tailRight.z).color(red, green, blue, alpha);
        vertices.vertex(matrix, (float) headRight.x, (float) (baseY + height), (float) headRight.z).color(red, green, blue, alpha);
        vertices.vertex(matrix, (float) headLeft.x, (float) (baseY + height * 0.62), (float) headLeft.z).color(red, green, blue, alpha);
    }
}
