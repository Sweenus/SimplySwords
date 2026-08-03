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
import net.sweenus.simplyswords.entity.CaelestisTentacleEntity;

public class CaelestisTentacleModel extends EntityModel<CaelestisTentacleEntity> {

    private static final int SEGMENT_COUNT = 6;
    private final ModelPart base;
    private final ModelPart[] segments = new ModelPart[SEGMENT_COUNT];

    public CaelestisTentacleModel(ModelPart root) {
        this.base = root.getChild("base");
        ModelPart current = this.base;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            current = current.getChild("segment_" + i);
            this.segments[i] = current;
        }
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData base = root.addChild("base",
                ModelPartBuilder.create().uv(0, 0)
                        .cuboid(-4.0F, -4.0F, -4.0F,
                                8.0F, 4.0F, 8.0F, new Dilation(0.0F)),
                ModelTransform.NONE);

        ModelPartData current = base;
        int[] widths = {7, 6, 6, 5, 4, 3};
        int[] lengths = {7, 7, 7, 6, 5, 4};
        float parentPivot = -4.0F;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            float halfWidth = widths[i] * 0.5F;
            current = current.addChild(
                    "segment_" + i,
                    ModelPartBuilder.create().uv(0, 14)
                            .cuboid(-halfWidth, -lengths[i], -halfWidth,
                                    widths[i], lengths[i], widths[i], new Dilation(0.0F)),
                    ModelTransform.pivot(0.0F, parentPivot, 0.0F)
            );
            parentPivot = -lengths[i];
        }
        return TexturedModelData.of(data, 32, 32);
    }

    @Override
    public void setAngles(CaelestisTentacleEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        this.base.resetTransform();
        for (ModelPart segment : this.segments) {
            segment.resetTransform();
        }

        int seed = entity.getCorruptionSeed();
        float seedPhase = Math.floorMod(seed, 4096) * 0.0137F;
        float speed = 0.84F + Math.floorMod(seed >>> 5, 29) * 0.012F;
        float contact = entity.getContactIntensity();
        float baseLean = (Math.floorMod(seed >>> 11, 17) - 8) * 0.007F;
        this.base.roll = baseLean * 0.45F;

        for (int i = 0; i < this.segments.length; i++) {
            float progress = (i + 1.0F) / this.segments.length;
            float delayedPhase = animationProgress * 0.075F * speed
                    + seedPhase - i * (0.42F + contact * 0.08F);
            float curl = contact * progress * progress * 0.16F;
            this.segments[i].pitch = baseLean * 0.75F
                    + MathHelper.sin(delayedPhase) * (0.045F + progress * 0.105F)
                    + curl;
            this.segments[i].roll = MathHelper.cos(
                    delayedPhase * 0.83F + seedPhase * 0.37F)
                    * (0.04F + progress * 0.12F)
                    + baseLean;
            this.segments[i].yaw = MathHelper.sin(
                    delayedPhase * 0.61F - seedPhase * 0.24F)
                    * progress * (0.05F + contact * 0.055F);
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer,
                       int light, int overlay, float red, float green, float blue, float alpha) {
        this.base.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}
