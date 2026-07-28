package net.sweenus.simplyswords.client.renderer.model;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SpiderEntityModel;
import net.sweenus.simplyswords.entity.CaelestisRiftlingEntity;

import java.util.List;

public class CaelestisRiftlingModel extends SpiderEntityModel<CaelestisRiftlingEntity> {

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart abdomen;
    private final List<ModelPart> rightLegs;
    private final List<ModelPart> leftLegs;
    private final List<ModelPart> allParts;

    public CaelestisRiftlingModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body0");
        this.abdomen = root.getChild("body1");
        this.rightLegs = List.of(
                root.getChild("right_hind_leg"),
                root.getChild("right_middle_hind_leg"),
                root.getChild("right_middle_front_leg"),
                root.getChild("right_front_leg")
        );
        this.leftLegs = List.of(
                root.getChild("left_hind_leg"),
                root.getChild("left_middle_hind_leg"),
                root.getChild("left_middle_front_leg"),
                root.getChild("left_front_leg")
        );
        this.allParts = List.of(
                this.head,
                this.body,
                this.abdomen,
                this.rightLegs.get(0),
                this.rightLegs.get(1),
                this.rightLegs.get(2),
                this.rightLegs.get(3),
                this.leftLegs.get(0),
                this.leftLegs.get(1),
                this.leftLegs.get(2),
                this.leftLegs.get(3)
        );
    }

    public static TexturedModelData getTexturedModelData() {
        return SpiderEntityModel.getTexturedModelData();
    }

    @Override
    public void setAngles(CaelestisRiftlingEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        resetScales();

        switch (Math.floorMod(entity.getCorruptionSeed(), 4)) {
            case 0 -> applyStiltVariant();
            case 1 -> applySwollenVariant();
            case 2 -> applyCrabVariant();
            default -> applyAsymmetricVariant();
        }
    }

    public List<ModelPart> getPatchworkParts() {
        return this.allParts;
    }

    public void setPatchworkVisible(ModelPart visiblePart) {
        for (ModelPart part : this.allParts) {
            part.visible = part == visiblePart;
        }
    }

    public void restoreVisibility() {
        for (ModelPart part : this.allParts) {
            part.visible = true;
        }
    }

    private void resetScales() {
        for (ModelPart part : this.allParts) {
            part.xScale = 1.0F;
            part.yScale = 1.0F;
            part.zScale = 1.0F;
        }
    }

    private void applyStiltVariant() {
        this.head.xScale = 0.72F;
        this.head.yScale = 0.82F;
        this.head.zScale = 0.78F;
        this.body.xScale = 0.68F;
        this.body.yScale = 1.15F;
        this.abdomen.xScale = 0.74F;
        this.abdomen.yScale = 1.22F;
        for (ModelPart leg : this.allLegs()) {
            leg.xScale = 1.32F;
            leg.yScale = 0.58F;
            leg.zScale = 0.72F;
        }
    }

    private void applySwollenVariant() {
        this.head.xScale = 0.62F;
        this.head.yScale = 0.68F;
        this.head.zScale = 0.72F;
        this.body.xScale = 1.1F;
        this.body.yScale = 1.18F;
        this.abdomen.xScale = 1.48F;
        this.abdomen.yScale = 1.32F;
        this.abdomen.zScale = 1.25F;
        for (ModelPart leg : this.allLegs()) {
            leg.xScale = 0.82F;
            leg.yScale = 1.18F;
        }
    }

    private void applyCrabVariant() {
        this.head.xScale = 1.32F;
        this.head.yScale = 0.55F;
        this.body.xScale = 1.42F;
        this.body.yScale = 0.62F;
        this.abdomen.xScale = 1.3F;
        this.abdomen.yScale = 0.65F;
        this.abdomen.zScale = 0.78F;
        for (ModelPart leg : this.allLegs()) {
            leg.xScale = 1.22F;
            leg.yScale = 0.6F;
            leg.zScale = 1.25F;
        }
    }

    private void applyAsymmetricVariant() {
        this.head.xScale = 0.8F;
        this.head.yScale = 1.2F;
        this.body.xScale = 0.9F;
        this.abdomen.xScale = 1.18F;
        this.abdomen.yScale = 0.82F;
        for (ModelPart leg : this.rightLegs) {
            leg.xScale = 1.38F;
            leg.yScale = 0.72F;
        }
        for (ModelPart leg : this.leftLegs) {
            leg.xScale = 0.72F;
            leg.yScale = 1.28F;
            leg.zScale = 0.74F;
        }
    }

    private List<ModelPart> allLegs() {
        return List.of(
                this.rightLegs.get(0),
                this.rightLegs.get(1),
                this.rightLegs.get(2),
                this.rightLegs.get(3),
                this.leftLegs.get(0),
                this.leftLegs.get(1),
                this.leftLegs.get(2),
                this.leftLegs.get(3)
        );
    }
}
