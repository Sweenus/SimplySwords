package net.sweenus.simplyswords.client.renderer.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.sweenus.simplyswords.entity.CaelestisDreadglareEntity;

public class CaelestisDreadglareModel extends EntityModel<CaelestisDreadglareEntity> {

    private final ModelPart core;
    private final ModelPart growths;

    public CaelestisDreadglareModel(ModelPart root) {
        this.core = root.getChild("core");
        this.growths = root.getChild("growths");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        root.addChild("core", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.0F, -8.0F, -4.0F,
                                6.0F, 6.0F, 5.0F, new Dilation(0.0F))
                        .uv(10, 11).cuboid(-2.0F, -7.0F, 1.0F,
                                4.0F, 4.0F, 1.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));
        root.addChild("growths", ModelPartBuilder.create()
                        .uv(0, 11).cuboid(1.0F, -7.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(8, 16).cuboid(-1.0F, -7.0F, 2.0F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                        .uv(0, 2).cuboid(-1.0F, -6.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(17, 0).cuboid(0.0F, -4.0F, 2.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
                        .uv(0, 11).cuboid(1.0F, -5.0F, 2.0F, 1.0F, 1.0F, 4.0F, new Dilation(0.0F))
                        .uv(13, 16).cuboid(-1.0F, -5.0F, 2.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
                        .uv(0, 16).cuboid(-2.0F, -4.0F, 2.0F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                        .uv(0, 0).cuboid(-2.0F, -7.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(5, 16).cuboid(-2.0F, -6.0F, 2.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));
        return TexturedModelData.of(modelData, 32, 32);
    }

    @Override
    public void setAngles(CaelestisDreadglareEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        float seedOffset = Math.floorMod(entity.getCorruptionSeed(), 31) * 0.17F;
        float aggression = entity.getFlightState() == CaelestisDreadglareEntity.STATE_DIVE ? 2.4F : 1.0F;
        float bob = MathHelper.sin(animationProgress * 0.16F + seedOffset) * 0.55F;
        this.core.pivotY = 24.0F + bob;
        this.growths.pivotY = 24.0F + bob;
        this.core.pitch = headPitch * 0.0025F;
        this.core.yaw = headYaw * 0.002F;
        this.core.roll = MathHelper.sin(animationProgress * 0.12F + seedOffset) * 0.06F;
        this.growths.pitch = this.core.pitch
                + MathHelper.sin(animationProgress * 0.42F + seedOffset) * 0.055F * aggression;
        this.growths.yaw = this.core.yaw
                + MathHelper.sin(animationProgress * 0.31F + seedOffset) * 0.08F * aggression;
        this.growths.roll = -this.core.roll
                + MathHelper.sin(animationProgress * 0.55F + seedOffset) * 0.075F * aggression;
        float pulse = 1.0F + MathHelper.sin(animationProgress * 0.36F + seedOffset) * 0.06F * aggression;
        this.growths.xScale = pulse;
        this.growths.yScale = pulse;
        this.growths.zScale = pulse;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer,
                       int light, int overlay, float red, float green, float blue, float alpha) {
        this.core.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        this.growths.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}
