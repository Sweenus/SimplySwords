package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.renderer.feature.CaelestisGrowthFeatureRenderer;
import net.sweenus.simplyswords.client.renderer.feature.CaelestisRiftlingPatchworkFeatureRenderer;
import net.sweenus.simplyswords.client.renderer.model.CaelestisRiftlingModel;
import net.sweenus.simplyswords.entity.CaelestisRiftlingEntity;

public class CaelestisRiftlingRenderer
        extends MobEntityRenderer<CaelestisRiftlingEntity, CaelestisRiftlingModel> {

    private static final Identifier FALLBACK_TEXTURE =
            Identifier.of("minecraft", "textures/entity/spider/spider.png");

    public CaelestisRiftlingRenderer(EntityRendererFactory.Context context) {
        super(context, new CaelestisRiftlingModel(
                context.getPart(SimplySwords.Client.CAELESTIS_RIFTLING_MODEL)), 0.58F);
        this.addFeature(new CaelestisRiftlingPatchworkFeatureRenderer(this));
        this.addFeature(new CaelestisGrowthFeatureRenderer<>(
                this, CaelestisGrowthFeatureRenderer.Shape.RIFTLING));
    }

    @Override
    public Identifier getTexture(CaelestisRiftlingEntity entity) {
        return FALLBACK_TEXTURE;
    }

    @Override
    protected RenderLayer getRenderLayer(CaelestisRiftlingEntity entity, boolean showBody,
                                         boolean translucent, boolean showOutline) {
        return showOutline ? RenderLayer.getOutline(FALLBACK_TEXTURE) : null;
    }

    @Override
    public void render(CaelestisRiftlingEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        float scale = entity.isUnbound() ? 1.12F : 0.88F;
        float skew = 1.0F + (Math.floorMod(entity.getCorruptionSeed(), 9) - 4) * 0.026F;
        matrices.scale(scale * skew, scale, scale / skew);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }
}
