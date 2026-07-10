package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.entity.FrostfallIceSpikeVisualEntity;

public class FrostfallIceSpikeVisualEntityRenderer extends EntityRenderer<FrostfallIceSpikeVisualEntity> {

    public FrostfallIceSpikeVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(FrostfallIceSpikeVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(FrostfallIceSpikeVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float heightScale = Math.max(0.0F, entity.getHeightScale());
        if (heightScale <= 0.01F) {
            return;
        }

        float height = Math.max(0.05F, entity.getTargetHeight() * heightScale);
        matrices.push();
        matrices.translate(-0.5, 0.0, -0.5);
        matrices.scale(0.42F, height, 0.42F);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.BLUE_ICE.getDefaultState(),
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }
}
