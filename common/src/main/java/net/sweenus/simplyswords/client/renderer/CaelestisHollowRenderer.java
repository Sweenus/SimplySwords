package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieBaseEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.client.renderer.feature.CaelestisGrowthFeatureRenderer;
import net.sweenus.simplyswords.client.renderer.feature.CaelestisHollowPatchworkFeatureRenderer;
import net.sweenus.simplyswords.client.renderer.model.CaelestisHollowModel;
import net.sweenus.simplyswords.entity.CaelestisHollowEntity;

public class CaelestisHollowRenderer
        extends ZombieBaseEntityRenderer<CaelestisHollowEntity, CaelestisHollowModel> {

    private static final Identifier FALLBACK_TEXTURE =
            new Identifier("minecraft", "textures/entity/zombie/zombie.png");

    public CaelestisHollowRenderer(EntityRendererFactory.Context context) {
        super(
                context,
                new CaelestisHollowModel(context.getPart(EntityModelLayers.ZOMBIE)),
                new CaelestisHollowModel(context.getPart(EntityModelLayers.ZOMBIE_INNER_ARMOR)),
                new CaelestisHollowModel(context.getPart(EntityModelLayers.ZOMBIE_OUTER_ARMOR))
        );
        this.addFeature(new CaelestisHollowPatchworkFeatureRenderer(this));
        this.addFeature(new CaelestisGrowthFeatureRenderer<>(
                this, CaelestisGrowthFeatureRenderer.Shape.HOLLOW));
    }

    @Override
    protected RenderLayer getRenderLayer(CaelestisHollowEntity entity, boolean showBody,
                                         boolean translucent, boolean showOutline) {
        return showOutline ? RenderLayer.getOutline(FALLBACK_TEXTURE) : null;
    }

    @Override
    public void render(CaelestisHollowEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        float scale = entity.isUnbound() ? 1.16F : 1.0F;
        float width = 0.9F + Math.floorMod(entity.getCorruptionSeed(), 7) * 0.028F;
        matrices.scale(scale * width, scale, scale / width);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                (Math.floorMod(entity.getCorruptionSeed() >>> 4, 9) - 4) * 1.15F));
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }
}
