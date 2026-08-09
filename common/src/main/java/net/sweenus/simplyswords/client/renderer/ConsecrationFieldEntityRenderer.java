package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.sweenus.simplyswords.api.render.TerrainFieldRenderData;

public class ConsecrationFieldEntityRenderer<T extends Entity & TerrainFieldRenderData> extends EntityRenderer<T> {

    private static final float VERTICAL_RANGE = 2.5F;

    private final TerrainFieldOverlayRenderer terrainOverlay = new TerrainFieldOverlayRenderer();

    public ConsecrationFieldEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(T entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        // The field extends beyond the entity's culling box.
        float reach = Math.max(0.1F, entity.getRadius()) + 3.0F;
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(reach, VERTICAL_RANGE + 2.0F, reach));
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float radius = Math.max(0.1F, entity.getRadius());

        if (ModernFieldRenderer.isEnabled()) {
            terrainOverlay.render(
                    entity.getWorld(),
                    entity.getUuid(),
                    MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                    MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                    MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()),
                    radius,
                    radius,
                    VERTICAL_RANGE,
                    TerrainFieldOverlayRenderer.WHITE_MARBLE,
                    matrices.peek(),
                    vertexConsumers
            );
        } else {
            terrainOverlay.clear(entity.getUuid());
        }

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
