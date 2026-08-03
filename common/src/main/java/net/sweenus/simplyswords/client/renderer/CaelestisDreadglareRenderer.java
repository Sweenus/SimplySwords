package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.renderer.feature.CaelestisCorruptionFeatureRenderer;
import net.sweenus.simplyswords.client.renderer.model.CaelestisDreadglareModel;
import net.sweenus.simplyswords.entity.CaelestisDreadglareEntity;

public class CaelestisDreadglareRenderer
        extends MobEntityRenderer<CaelestisDreadglareEntity, CaelestisDreadglareModel> {

    private static final Identifier TEXTURE =
            new Identifier(SimplySwords.MOD_ID, "textures/entity/caelestis_dreadglare.png");

    public CaelestisDreadglareRenderer(EntityRendererFactory.Context context) {
        super(context, new CaelestisDreadglareModel(
                context.getPart(SimplySwords.Client.CAELESTIS_DREADGLARE_MODEL)), 0.52F);
        this.addFeature(new CaelestisCorruptionFeatureRenderer<>(this, entity -> TEXTURE));
    }

    @Override
    public Identifier getTexture(CaelestisDreadglareEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CaelestisDreadglareEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        float scale = entity.isUnbound() ? 1.18F : 1.0F;
        matrices.scale(scale, scale, scale);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch(tickDelta)));
        float bank = entity.getFlightState() == CaelestisDreadglareEntity.STATE_ORBIT
                ? MathHelper.sin((entity.age + tickDelta) * 0.12F
                + Math.floorMod(entity.getCorruptionSeed(), 19)) * 12.0F
                : entity.getFlightState() == CaelestisDreadglareEntity.STATE_DIVE ? 18.0F : -8.0F;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(bank));
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, 0x00F000F0);
        matrices.pop();
    }
}
