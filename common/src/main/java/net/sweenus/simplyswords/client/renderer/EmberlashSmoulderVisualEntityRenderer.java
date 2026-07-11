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
import net.sweenus.simplyswords.entity.EmberlashSmoulderVisualEntity;

public class EmberlashSmoulderVisualEntityRenderer extends EntityRenderer<EmberlashSmoulderVisualEntity> {

    private static final float BLOCK_SCALE = 0.13F;
    private static final float ARC_WIDTH_PER_BLOCK = 0.23F;
    private static final float ARC_MIN_WIDTH = 0.44F;
    private static final float ARC_HEIGHT = 0.34F;
    private static final int MAX_RENDERED_STACKS = 12;

    public EmberlashSmoulderVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(EmberlashSmoulderVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(EmberlashSmoulderVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        int stacks = MathHelper.clamp(entity.getStacks(), 1, MAX_RENDERED_STACKS);
        float arcWidth = Math.max(ARC_MIN_WIDTH, (stacks - 1) * ARC_WIDTH_PER_BLOCK);
        float pulse = 0.94F + 0.06F * MathHelper.sin((entity.age + tickDelta) * 0.24F);
        float durationScale = MathHelper.clamp(entity.getScale(), 0.0F, 1.0F);
        if (durationScale <= 0.01F) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.sin((entity.age + tickDelta) * 0.05F) * 4.0F));
        for (int i = 0; i < stacks; i++) {
            float progress = stacks == 1 ? 0.5F : (float) i / (float) (stacks - 1);
            float x = MathHelper.lerp(progress, -arcWidth * 0.5F, arcWidth * 0.5F);
            float y = MathHelper.sin(progress * MathHelper.PI) * ARC_HEIGHT;

            matrices.push();
            matrices.translate(x, y, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((entity.age + tickDelta) * 2.0F + i * 23.0F));
            matrices.scale(BLOCK_SCALE * pulse * durationScale, BLOCK_SCALE * pulse * durationScale, BLOCK_SCALE * pulse * durationScale);
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
        matrices.pop();
    }
}
