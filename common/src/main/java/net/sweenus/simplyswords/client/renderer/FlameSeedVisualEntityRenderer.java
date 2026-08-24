package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.entity.FlameSeedVisualEntity;

public class FlameSeedVisualEntityRenderer extends EntityRenderer<FlameSeedVisualEntity> {

    private static final float MAGMA_BLOCK_SCALE = 0.28F;

    public FlameSeedVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(FlameSeedVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(FlameSeedVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float scale = MathHelper.clamp(entity.getScale(), 0.0F, 2.0F);
        if (scale <= 0.01F) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((entity.age + tickDelta) * 2.5F));
        matrices.scale(MAGMA_BLOCK_SCALE * scale, MAGMA_BLOCK_SCALE * scale, MAGMA_BLOCK_SCALE * scale);
        matrices.translate(-0.5, -0.5, -0.5);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.MAGMA_BLOCK.getDefaultState(),
                matrices,
                vertexConsumers,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }
}
