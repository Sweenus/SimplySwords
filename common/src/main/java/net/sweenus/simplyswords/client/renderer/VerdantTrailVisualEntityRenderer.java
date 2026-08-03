package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.BlockState;
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
import net.sweenus.simplyswords.entity.VerdantTrailVisualEntity;

public class VerdantTrailVisualEntityRenderer extends EntityRenderer<VerdantTrailVisualEntity> {

    public VerdantTrailVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(VerdantTrailVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(VerdantTrailVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float heightScale = Math.max(0.0F, entity.getHeightScale());
        if (heightScale <= 0.01F) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw()));
        matrices.translate(-0.5, 0.0, -0.5);

        float xzScale = entity.getPlantType() <= 1 ? 0.9F : 0.78F;
        matrices.scale(xzScale, Math.max(0.05F, heightScale), xzScale);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                stateForType(entity.getPlantType()),
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }

    private static BlockState stateForType(int type) {
        return switch (type) {
            case 1 -> Blocks.FERN.getDefaultState();
            case 2 -> Blocks.DANDELION.getDefaultState();
            case 3 -> Blocks.POPPY.getDefaultState();
            case 4 -> Blocks.AZURE_BLUET.getDefaultState();
            case 5 -> Blocks.OXEYE_DAISY.getDefaultState();
            case 6 -> Blocks.CORNFLOWER.getDefaultState();
            default -> Blocks.GRASS.getDefaultState();
        };
    }
}
