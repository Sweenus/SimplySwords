package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.Frustum;
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
import net.sweenus.simplyswords.entity.DragonWingBuffetVisualEntity;

public class DragonWingBuffetVisualEntityRenderer extends EntityRenderer<DragonWingBuffetVisualEntity> {

    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/enderdragon/dragon.png");
    private static final float MODEL_BACK_OFFSET = 0.35F;
    private static final float MODEL_VERTICAL_OFFSET = -6.7F;
    private final WingModel model;

    public DragonWingBuffetVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = WingModel.create();
    }

    @Override
    public Identifier getTexture(DragonWingBuffetVisualEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(DragonWingBuffetVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(5.0));
    }

    @Override
    public void render(DragonWingBuffetVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float progress = MathHelper.clamp((entity.age + tickDelta) / Math.max(1.0F, entity.getLifetime()), 0.0F, 1.0F);
        float fade = MathHelper.sin(progress * MathHelper.PI);
        if (fade <= 0.01F) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getOwnerYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        float scale = entity.getScale() * 0.45F;
        matrices.scale(scale, scale, scale);
        matrices.translate(0.0F, MODEL_BACK_OFFSET, MODEL_VERTICAL_OFFSET);

        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(this.getTexture(entity)));
        int alpha = MathHelper.clamp((int) (180.0F + 75.0F * fade), 0, 255);
        this.renderWings(entity, tickDelta, matrices, vertices, Math.max(light, LightmapTextureManager.MAX_LIGHT_COORDINATE), alpha);
        matrices.pop();
    }

    private void renderWings(DragonWingBuffetVisualEntity entity, float tickDelta, MatrixStack matrices, VertexConsumer vertices, int light, int alpha) {
        float progress = MathHelper.clamp((entity.age + tickDelta) / Math.max(1.0F, entity.getLifetime()), 0.0F, 1.0F);
        float phase = (entity.getPhaseOffset() + progress * 4.05F) * MathHelper.TAU;
        float openAmount = MathHelper.sin(progress * MathHelper.PI);

        float wingPitch = 0.125F - MathHelper.cos(phase) * 0.2F;
        float wingYaw = -0.25F;
        float wingRoll = -(MathHelper.sin(phase) + 0.125F) * 0.8F * openAmount;
        float tipRoll = (MathHelper.sin(phase + 2.0F) + 0.5F) * 0.75F * openAmount;

        this.model.render(matrices, vertices, light, alpha, wingPitch, wingYaw, wingRoll, tipRoll);
    }

    private static class WingModel {
        private final ModelPart leftWing;
        private final ModelPart leftWingTip;
        private final ModelPart rightWing;
        private final ModelPart rightWingTip;

        private WingModel(ModelPart root) {
            this.leftWing = root.getChild("left_wing");
            this.leftWingTip = this.leftWing.getChild("left_wing_tip");
            this.rightWing = root.getChild("right_wing");
            this.rightWingTip = this.rightWing.getChild("right_wing_tip");
        }

        private static WingModel create() {
            return new WingModel(getTexturedModelData().createModel());
        }

        private static TexturedModelData getTexturedModelData() {
            ModelData modelData = new ModelData();
            ModelPartData root = modelData.getRoot();
            ModelPartData leftWing = root.addChild("left_wing", ModelPartBuilder.create().mirrored()
                    .uv(112, 88).cuboid(0.0F, -4.0F, -4.0F, 56.0F, 8.0F, 8.0F, new Dilation(0.0F))
                    .uv(-56, 88).cuboid(0.0F, 0.0F, 2.0F, 56.0F, 0.0F, 56.0F, new Dilation(0.0F)),
                    ModelTransform.pivot(12.0F, 5.0F, 2.0F));
            leftWing.addChild("left_wing_tip", ModelPartBuilder.create().mirrored()
                    .uv(112, 136).cuboid(0.0F, -2.0F, -2.0F, 56.0F, 4.0F, 4.0F, new Dilation(0.0F))
                    .uv(-56, 144).cuboid(0.0F, 0.0F, 2.0F, 56.0F, 0.0F, 56.0F, new Dilation(0.0F)),
                    ModelTransform.pivot(56.0F, 0.0F, 0.0F));
            ModelPartData rightWing = root.addChild("right_wing", ModelPartBuilder.create()
                    .uv(112, 88).cuboid(-56.0F, -4.0F, -4.0F, 56.0F, 8.0F, 8.0F, new Dilation(0.0F))
                    .uv(-56, 88).cuboid(-56.0F, 0.0F, 2.0F, 56.0F, 0.0F, 56.0F, new Dilation(0.0F)),
                    ModelTransform.pivot(-12.0F, 5.0F, 2.0F));
            rightWing.addChild("right_wing_tip", ModelPartBuilder.create()
                    .uv(112, 136).cuboid(-56.0F, -2.0F, -2.0F, 56.0F, 4.0F, 4.0F, new Dilation(0.0F))
                    .uv(-56, 144).cuboid(-56.0F, 0.0F, 2.0F, 56.0F, 0.0F, 56.0F, new Dilation(0.0F)),
                    ModelTransform.pivot(-56.0F, 0.0F, 0.0F));
            return TexturedModelData.of(modelData, 256, 256);
        }

        private void render(MatrixStack matrices, VertexConsumer vertices, int light, int alpha,
                            float wingPitch, float wingYaw, float wingRoll, float tipRoll) {
            float opacity = alpha / 255.0F;

            this.leftWing.pitch = wingPitch;
            this.leftWing.yaw = wingYaw;
            this.leftWing.roll = wingRoll;
            this.leftWingTip.pitch = 0.0F;
            this.leftWingTip.yaw = 0.0F;
            this.leftWingTip.roll = tipRoll;
            this.leftWing.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV,
                    1.0F, 1.0F, 1.0F, opacity);

            this.rightWing.pitch = wingPitch;
            this.rightWing.yaw = -wingYaw;
            this.rightWing.roll = -wingRoll;
            this.rightWingTip.pitch = 0.0F;
            this.rightWingTip.yaw = 0.0F;
            this.rightWingTip.roll = -tipRoll;
            this.rightWing.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV,
                    1.0F, 1.0F, 1.0F, opacity);
        }
    }
}
