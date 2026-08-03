package net.sweenus.simplyswords.client.renderer.feature;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.client.renderer.model.CaelestisRiftlingModel;
import net.sweenus.simplyswords.entity.CaelestisRiftlingEntity;
import net.sweenus.simplyswords.client.renderer.LegacyRenderColor;

import java.util.List;

public class CaelestisRiftlingPatchworkFeatureRenderer
        extends FeatureRenderer<CaelestisRiftlingEntity, CaelestisRiftlingModel> {

    private static final Identifier[] TEXTURES = {
            new Identifier("minecraft", "textures/entity/spider/spider.png"),
            new Identifier("minecraft", "textures/entity/spider/cave_spider.png"),
            new Identifier("minecraft", "textures/entity/endermite.png")
    };

    public CaelestisRiftlingPatchworkFeatureRenderer(
            FeatureRendererContext<CaelestisRiftlingEntity, CaelestisRiftlingModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       CaelestisRiftlingEntity entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        CaelestisRiftlingModel model = this.getContextModel();
        List<ModelPart> parts = model.getPatchworkParts();
        try {
            for (int index = 0; index < parts.size(); index++) {
                model.setPatchworkVisible(parts.get(index));
                renderVisible(model, texture(entity, index), matrices, vertexConsumers,
                        CaelestisCorruptionFeatureRenderer.colorFor(entity));
            }
        } finally {
            model.restoreVisibility();
        }
    }

    private static Identifier texture(CaelestisRiftlingEntity entity, int part) {
        int mixed = entity.getCorruptionSeed() * 31 + part * 0x45d9f3b;
        return TEXTURES[Math.floorMod(mixed, TEXTURES.length)];
    }

    private static void renderVisible(CaelestisRiftlingModel model, Identifier texture,
                                      MatrixStack matrices, VertexConsumerProvider vertexConsumers, int color) {
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        model.render(matrices, vertices, 0x00F000F0, OverlayTexture.DEFAULT_UV,
                LegacyRenderColor.red(color), LegacyRenderColor.green(color),
                LegacyRenderColor.blue(color), LegacyRenderColor.alpha(color));
    }
}
