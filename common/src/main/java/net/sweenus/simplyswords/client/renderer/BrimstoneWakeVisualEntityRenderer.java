package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BrimstoneWakeVisualEntity;

public class BrimstoneWakeVisualEntityRenderer extends EntityRenderer<BrimstoneWakeVisualEntity> {
    private final TerrainFieldOverlayRenderer terrainOverlay = new TerrainFieldOverlayRenderer();

    public BrimstoneWakeVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(BrimstoneWakeVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(BrimstoneWakeVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        float reach = entity.getRadius() + 3.0F;
        float vertical = entity.getVerticalRange() + 2.0F;
        return frustum.isVisible(new Box(
                entity.getX() - reach, entity.getY() - vertical, entity.getZ() - reach,
                entity.getX() + reach, entity.getY() + vertical, entity.getZ() + reach));
    }

    @Override
    public void render(BrimstoneWakeVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (!Config.general.enableModernFieldEffects) {
            terrainOverlay.clearMoltenWakes();
            return;
        }
        terrainOverlay.renderMergedMoltenWake(entity, tickDelta, matrices.peek(), consumers);
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }
}
