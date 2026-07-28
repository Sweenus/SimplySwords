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
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.entity.MoltenRuptureVisualEntity;

public class MoltenRuptureVisualEntityRenderer extends EntityRenderer<MoltenRuptureVisualEntity> {

    public MoltenRuptureVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(MoltenRuptureVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(MoltenRuptureVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float heightScale = Math.max(0.0F, entity.getHeightScale());
        if (heightScale <= 0.01F) {
            return;
        }

        float height = Math.max(0.08F, entity.getTargetHeight() * heightScale);
        float width = 0.48F + Math.min(0.24F, entity.getTargetHeight() * 0.12F);
        float rotation = (entity.getId() * 47) % 360;

        matrices.push();
        matrices.translate(0.0, height * 0.46F, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(((entity.getId() % 5) - 2) * 5.0F));
        matrices.scale(width, height, width);
        matrices.translate(-0.5, -0.5, -0.5);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.MAGMA_BLOCK.getDefaultState(),
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }
}
