package net.sweenus.simplyswords.client.renderer.feature;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.client.renderer.model.CaelestisHollowModel;
import net.sweenus.simplyswords.entity.CaelestisHollowEntity;

public class CaelestisHollowPatchworkFeatureRenderer
        extends FeatureRenderer<CaelestisHollowEntity, CaelestisHollowModel> {

    private static final Identifier[] TEXTURES = {
            Identifier.of("minecraft", "textures/entity/zombie/zombie.png"),
            Identifier.of("minecraft", "textures/entity/zombie/husk.png"),
            Identifier.of("minecraft", "textures/entity/zombie/drowned.png"),
            Identifier.of("minecraft", "textures/entity/piglin/zombified_piglin.png"),
            Identifier.of("minecraft", "textures/entity/illager/illusioner.png")
    };

    public CaelestisHollowPatchworkFeatureRenderer(
            FeatureRendererContext<CaelestisHollowEntity, CaelestisHollowModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       CaelestisHollowEntity entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        CaelestisHollowModel model = this.getContextModel();
        int color = CaelestisCorruptionFeatureRenderer.colorFor(entity);
        model.setVisible(false);
        try {
            model.head.visible = true;
            model.hat.visible = true;
            renderVisible(model, texture(entity, 0), matrices, vertexConsumers, color);
            model.head.visible = false;
            model.hat.visible = false;

            model.body.visible = true;
            renderVisible(model, texture(entity, 1), matrices, vertexConsumers, color);
            model.body.visible = false;

            model.rightArm.visible = true;
            renderVisible(model, texture(entity, 2), matrices, vertexConsumers, color);
            model.rightArm.visible = false;

            model.leftArm.visible = true;
            renderVisible(model, texture(entity, 3), matrices, vertexConsumers, color);
            model.leftArm.visible = false;

            model.rightLeg.visible = true;
            renderVisible(model, texture(entity, 4), matrices, vertexConsumers, color);
            model.rightLeg.visible = false;

            model.leftLeg.visible = true;
            renderVisible(model, texture(entity, 5), matrices, vertexConsumers, color);
        } finally {
            model.setVisible(true);
        }
    }

    private static Identifier texture(CaelestisHollowEntity entity, int part) {
        int mixed = entity.getCorruptionSeed() * 31 + part * 0x45d9f3b;
        return TEXTURES[Math.floorMod(mixed, TEXTURES.length)];
    }

    private static void renderVisible(CaelestisHollowModel model, Identifier texture,
                                      MatrixStack matrices, VertexConsumerProvider vertexConsumers, int color) {
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        model.render(matrices, vertices, 0x00F000F0, OverlayTexture.DEFAULT_UV, color);
    }
}
