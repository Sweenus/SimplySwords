package net.sweenus.simplyswords.client.renderer;

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
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.entity.ImplicitStatusVisualEntity;
import org.joml.Matrix4f;

public class ImplicitStatusVisualEntityRenderer extends EntityRenderer<ImplicitStatusVisualEntity> {

    private static final Identifier SUNDERED_ARMOR_TEXTURE = Identifier.of("simplyswords", "textures/mob_effect/sundered_armor.png");

    public ImplicitStatusVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(ImplicitStatusVisualEntity entity) {
        return SUNDERED_ARMOR_TEXTURE;
    }

    @Override
    public void render(ImplicitStatusVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        int sunderAmount = entity.getSunderAmount();
        if (sunderAmount <= 0) {
            return;
        }

        float age = entity.age + tickDelta;
        int shardCount = MathHelper.clamp(3 + sunderAmount / 12, 3, 6);
        float severity = MathHelper.clamp(sunderAmount / 50.0F, 0.0F, 1.0F);
        float radius = 0.34F + severity * 0.14F;
        float baseScale = 0.18F + severity * 0.04F;

        for (int i = 0; i < shardCount; i++) {
            float phase = i * (MathHelper.TAU / shardCount);
            float angle = phase + age * (0.045F + i * 0.002F);
            float wobble = MathHelper.sin(age * 0.16F + i * 1.7F) * 0.05F;
            double x = MathHelper.cos(angle) * (radius + wobble);
            double z = MathHelper.sin(angle) * (radius * 0.72F + wobble * 0.5F);
            double y = MathHelper.sin(age * 0.13F + phase) * 0.12F;
            float pulse = 0.94F + MathHelper.sin(age * 0.22F + phase) * 0.06F;

            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(this.dispatcher.getRotation());
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-32.0F + i * 19.0F + MathHelper.sin(age * 0.1F + i) * 8.0F));
            renderSunderedArmorSprite(matrices, vertexConsumers, baseScale * pulse);
            matrices.pop();
        }
    }

    private void renderSunderedArmorSprite(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float scale) {
        float halfSize = scale * 0.5F;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SUNDERED_ARMOR_TEXTURE));
        vertex(vertices, matrix, -halfSize, -halfSize, 0.0F, 0.0F, 1.0F);
        vertex(vertices, matrix, halfSize, -halfSize, 0.0F, 1.0F, 1.0F);
        vertex(vertices, matrix, halfSize, halfSize, 0.0F, 1.0F, 0.0F);
        vertex(vertices, matrix, -halfSize, halfSize, 0.0F, 0.0F, 0.0F);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float z, float u, float v) {
        vertices.vertex(matrix, x, y, z)
                .color(255, 255, 255, 230)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0.0F, 1.0F, 0.0F);
    }
}
