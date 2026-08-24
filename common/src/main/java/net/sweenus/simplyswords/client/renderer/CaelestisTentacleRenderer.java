package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.renderer.model.CaelestisTentacleModel;
import net.sweenus.simplyswords.entity.CaelestisTentacleEntity;

public class CaelestisTentacleRenderer extends EntityRenderer<CaelestisTentacleEntity> {

    private static final Identifier TEXTURE =
            Identifier.of(SimplySwords.MOD_ID, "textures/entity/caelestis_tentacle.png");
    private static final Identifier EMISSIVE_TEXTURE =
            Identifier.of(SimplySwords.MOD_ID, "textures/entity/caelestis_tentacle_emissive.png");

    private final CaelestisTentacleModel model;

    public CaelestisTentacleRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new CaelestisTentacleModel(
                context.getPart(SimplySwords.Client.CAELESTIS_TENTACLE_MODEL));
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(CaelestisTentacleEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CaelestisTentacleEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float emergence = MathHelper.clamp(
                (entity.getEmergenceTicksElapsed() + tickDelta)
                        / CaelestisTentacleEntity.EMERGE_TICKS,
                0.0F, 1.0F);
        emergence = emergence * emergence * (3.0F - 2.0F * emergence);
        float retraction = 1.0F;
        if (entity.isRetracting()) {
            retraction = 1.0F - MathHelper.clamp(
                    (entity.getRetractTicksElapsed() + tickDelta)
                            / CaelestisTentacleEntity.RETRACT_TICKS,
                    0.0F, 1.0F);
            retraction = retraction * retraction;
        }
        float verticalScale = emergence * retraction;
        if (verticalScale <= 0.01F) {
            return;
        }

        float scale = entity.getRenderScale();
        float time = entity.age + tickDelta;
        this.model.setAngles(entity, 0.0F, 0.0F, time, 0.0F, 0.0F);

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));
        matrices.scale(-scale, -scale * verticalScale, scale);

        VertexConsumer baseVertices = vertexConsumers.getBuffer(
                RenderLayer.getEntityCutoutNoCull(TEXTURE));
        this.model.render(matrices, baseVertices, light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);

        float pulse = 0.78F + MathHelper.sin(
                time * 0.19F + Math.floorMod(entity.getCorruptionSeed(), 37)) * 0.18F;
        int alpha = MathHelper.clamp((int) (255.0F * pulse), 0, 255);
        int emissiveColor = alpha << 24 | 0xFFFFFF;
        VertexConsumer emissiveVertices = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(EMISSIVE_TEXTURE));
        this.model.render(
                matrices,
                emissiveVertices,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                emissiveColor
        );
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
