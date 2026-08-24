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
import net.sweenus.simplyswords.entity.IcewhisperCometVisualEntity;

public class IcewhisperCometVisualEntityRenderer extends EntityRenderer<IcewhisperCometVisualEntity> {

    private static final float COMET_SCALE = 0.42F;

    public IcewhisperCometVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(IcewhisperCometVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(IcewhisperCometVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float scale = MathHelper.clamp(entity.getScale(), 0.0F, 2.0F);
        if (scale <= 0.01F) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((entity.age + tickDelta) * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(35.0F + (entity.age + tickDelta) * 6.0F));
        matrices.scale(COMET_SCALE * scale, COMET_SCALE * scale, COMET_SCALE * scale);
        matrices.translate(-0.5, -0.5, -0.5);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.PACKED_ICE.getDefaultState(),
                matrices,
                vertexConsumers,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }
}
