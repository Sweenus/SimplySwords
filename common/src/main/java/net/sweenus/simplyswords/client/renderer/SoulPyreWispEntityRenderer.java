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
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulPyreWispEntity;
import org.joml.Matrix4f;

public class SoulPyreWispEntityRenderer extends EntityRenderer<SoulPyreWispEntity> {

    private static final Identifier WHITE_TEXTURE =
            new Identifier("minecraft", "textures/misc/white.png");

    public SoulPyreWispEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(SoulPyreWispEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public void render(SoulPyreWispEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light) {
        if (!Config.general.enableModernFieldEffects) {
            return;
        }
        float progress = MathHelper.clamp(
                (entity.age + tickDelta) / Math.max(1.0F, entity.getLifetime()),
                0.0F,
                1.0F
        );
        float fade = Math.min(1.0F, (1.0F - progress) * 3.0F);
        float pulse = 0.9F + MathHelper.sin(
                (entity.age + tickDelta) * 0.48F + entity.getSeed()) * 0.1F;
        Vec3d velocity = entity.getVelocity();
        float stretch = (float) MathHelper.clamp(velocity.length() * 1.1, 0.3, 1.0);

        matrices.push();
        matrices.multiply(this.dispatcher.getRotation());
        VertexConsumer vertices = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        drawDiamond(vertices, matrices.peek().getPositionMatrix(),
                0.28F * pulse, 0.46F + stretch, 32, 202, 215,
                (int) (150 * fade));
        drawDiamond(vertices, matrices.peek().getPositionMatrix(),
                0.15F * pulse, 0.25F + stretch * 0.35F, 190, 255, 245,
                (int) (235 * fade));
        matrices.pop();
    }

    private static void drawDiamond(
            VertexConsumer vertices, Matrix4f matrix,
            float halfWidth, float halfHeight,
            int red, int green, int blue, int alpha) {
        vertex(vertices, matrix, 0.0F, halfHeight, 0.0F, 0.5F, 0.0F,
                red, green, blue, alpha);
        vertex(vertices, matrix, halfWidth, 0.0F, 0.0F, 1.0F, 0.5F,
                red, green, blue, alpha);
        vertex(vertices, matrix, 0.0F, -halfHeight, 0.0F, 0.5F, 1.0F,
                red, green, blue, alpha);
        vertex(vertices, matrix, -halfWidth, 0.0F, 0.0F, 0.0F, 0.5F,
                red, green, blue, alpha);
    }

    private static void vertex(
            VertexConsumer vertices, Matrix4f matrix,
            float x, float y, float z, float u, float v,
            int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0.0F, 1.0F, 0.0F).next();
    }
}
