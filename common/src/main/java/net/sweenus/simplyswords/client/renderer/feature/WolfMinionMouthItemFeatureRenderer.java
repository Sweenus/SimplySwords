package net.sweenus.simplyswords.client.renderer.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.WolfEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

public class WolfMinionMouthItemFeatureRenderer extends FeatureRenderer<WolfEntity, WolfEntityModel<WolfEntity>> {

    private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;

    private final FeatureRendererContext<WolfEntity, WolfEntityModel<WolfEntity>> context;
    private final ModelPart root;

    public WolfMinionMouthItemFeatureRenderer(FeatureRendererContext<WolfEntity, WolfEntityModel<WolfEntity>> context, ModelPart root) {
        super(context);
        this.context = context;
        this.root = root;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       WolfEntity wolf, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        ItemStack stack = wolf.getMainHandStack();
        if (stack == null || stack.isEmpty() || this.root == null) {
            return;
        }

        ModelPart head = this.root.getChild("head");
        if (head == null) {
            return;
        }

        matrices.push();
        head.rotate(matrices);
        matrices.multiply(new Quaternionf().rotationZYX(0.0F, headYaw * DEG_TO_RAD, headPitch * DEG_TO_RAD));
        matrices.translate(0.18, 0.10, -0.30);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        matrices.scale(0.6F, 0.6F, 0.6F);
        MinecraftClient.getInstance().getItemRenderer().renderItem(
                stack, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false,
                matrices, vertexConsumers, light, net.minecraft.client.render.OverlayTexture.DEFAULT_UV,
                MinecraftClient.getInstance().getItemRenderer().getModel(stack, wolf.getWorld(), wolf, 0));
        matrices.pop();
    }
}
