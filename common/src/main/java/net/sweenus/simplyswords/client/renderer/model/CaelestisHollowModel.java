package net.sweenus.simplyswords.client.renderer.model;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.sweenus.simplyswords.entity.CaelestisHollowEntity;

import java.util.List;

public class CaelestisHollowModel extends ZombieEntityModel<CaelestisHollowEntity> {

    private final List<ModelPart> parts;

    public CaelestisHollowModel(ModelPart root) {
        super(root);
        this.parts = List.of(
                this.head, this.hat, this.body,
                this.rightArm, this.leftArm,
                this.rightLeg, this.leftLeg
        );
    }

    @Override
    public void setAngles(CaelestisHollowEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        resetScales();

        switch (Math.floorMod(entity.getCorruptionSeed(), 4)) {
            case 0 -> applyLankyVariant();
            case 1 -> applyBruteVariant();
            case 2 -> applyBrokenVariant();
            default -> applyLongLimbedVariant();
        }
    }

    private void resetScales() {
        for (ModelPart part : this.parts) {
            part.xScale = 1.0F;
            part.yScale = 1.0F;
            part.zScale = 1.0F;
        }
    }

    private void applyLankyVariant() {
        scale(this.head, 0.78F, 0.82F, 0.78F);
        scale(this.hat, 0.78F, 0.82F, 0.78F);
        scale(this.body, 0.7F, 1.18F, 0.72F);
        scale(this.rightArm, 0.62F, 1.38F, 0.62F);
        scale(this.leftArm, 0.62F, 1.38F, 0.62F);
        scale(this.rightLeg, 0.68F, 1.3F, 0.68F);
        scale(this.leftLeg, 0.68F, 1.3F, 0.68F);
    }

    private void applyBruteVariant() {
        scale(this.head, 0.68F, 0.68F, 0.68F);
        scale(this.hat, 0.68F, 0.68F, 0.68F);
        scale(this.body, 1.38F, 1.08F, 1.28F);
        scale(this.rightArm, 1.48F, 1.12F, 1.42F);
        scale(this.leftArm, 1.48F, 1.12F, 1.42F);
        scale(this.rightLeg, 1.16F, 0.86F, 1.18F);
        scale(this.leftLeg, 1.16F, 0.86F, 1.18F);
    }

    private void applyBrokenVariant() {
        scale(this.head, 1.04F, 0.76F, 0.86F);
        scale(this.hat, 1.04F, 0.76F, 0.86F);
        scale(this.body, 0.92F, 1.08F, 0.78F);
        scale(this.rightArm, 0.74F, 1.5F, 0.72F);
        scale(this.leftArm, 1.32F, 0.68F, 1.2F);
        scale(this.rightLeg, 0.76F, 1.2F, 0.76F);
        scale(this.leftLeg, 1.18F, 0.82F, 1.1F);
        this.head.roll += 0.3F;
        this.hat.roll += 0.3F;
        this.body.roll -= 0.08F;
    }

    private void applyLongLimbedVariant() {
        scale(this.head, 1.18F, 0.62F, 0.82F);
        scale(this.hat, 1.18F, 0.62F, 0.82F);
        scale(this.body, 0.82F, 1.22F, 0.7F);
        scale(this.rightArm, 0.72F, 1.58F, 0.68F);
        scale(this.leftArm, 0.88F, 1.35F, 0.72F);
        scale(this.rightLeg, 0.7F, 1.42F, 0.68F);
        scale(this.leftLeg, 0.78F, 1.24F, 0.72F);
    }

    private static void scale(ModelPart part, float x, float y, float z) {
        part.xScale = x;
        part.yScale = y;
        part.zScale = z;
    }
}
