package net.sweenus.simplyswords.client.renderer.feature;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class ShoulderAxolotlFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private static final Identifier WILD_AXOLOTL_TEXTURE = new Identifier("minecraft", "textures/entity/axolotl/axolotl_wild.png");
    private static final Identifier LUCY_AXOLOTL_TEXTURE = new Identifier("minecraft", "textures/entity/axolotl/axolotl_lucy.png");
    private static final Identifier GOLD_AXOLOTL_TEXTURE = new Identifier("minecraft", "textures/entity/axolotl/axolotl_gold.png");
    private static final Identifier CYAN_AXOLOTL_TEXTURE = new Identifier("minecraft", "textures/entity/axolotl/axolotl_cyan.png");
    private static final Identifier BLUE_AXOLOTL_TEXTURE = new Identifier("minecraft", "textures/entity/axolotl/axolotl_blue.png");

    private final ModelPart axolotlModel;

    public ShoulderAxolotlFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context, ModelPart axolotlModel) {
        super(context);
        this.axolotlModel = axolotlModel;
    }


    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta, float ageInTicks, float netHeadYaw, float headPitch) {
        //System.out.println("Left Shoulder NBT: " + player.getShoulderEntityLeft());
        //System.out.println("Right Shoulder NBT: " + player.getShoulderEntityRight());

        // Render axolotl on the left shoulder
        if (!player.getShoulderEntityLeft().isEmpty() && "simplyswords:simplyaxolotlentity".equals(player.getShoulderEntityLeft().getString("id"))) {
            matrixStack.push();
            attachToShoulder(matrixStack, true, player, tickDelta);
            Identifier texture = getAxolotlTexture(player.getShoulderEntityLeft());
            renderAxolotl(matrixStack, vertexConsumerProvider, light, texture);
            matrixStack.pop();
        }

        // Render axolotl on the right shoulder
        if (!player.getShoulderEntityRight().isEmpty() && "simplyswords:simplyaxolotlentity".equals(player.getShoulderEntityRight().getString("id"))) {
            matrixStack.push();
            attachToShoulder(matrixStack, false, player, tickDelta);
            Identifier texture = getAxolotlTexture(player.getShoulderEntityRight());
            renderAxolotl(matrixStack, vertexConsumerProvider, light, texture);
            matrixStack.pop();
        }
    }



    private void renderAxolotl(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, Identifier texture) {
        // Bind the correct texture for render
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutout(texture));
        this.axolotlModel.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
    }


    private void attachToShoulder(MatrixStack matrixStack, boolean leftShoulder, AbstractClientPlayerEntity player, float tickDelta) {
        int direction = leftShoulder ? -1 : 1; // -1 for left, 1 for right

        float interpolatedBodyYaw = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw) * tickDelta;
        float bodyYawRadians = (interpolatedBodyYaw % 360) * 0.017453292F;
        matrixStack.translate(direction * 0.4F, player.isInSneakingPose() ? -0.1F : -0.40F, 0.15F);
        matrixStack.translate(0.0F, -1.00F, -0.2F);

        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(bodyYawRadians));

        matrixStack.translate(direction * 0.4F, player.isInSneakingPose() ? -0.1F : 0.0F, 0.15F);
        matrixStack.translate(0.0F, -1.00F, -0.2F);

        matrixStack.pop();
    }

    private Identifier getAxolotlTexture(NbtCompound nbt) {
        // Get variant from NBT
        int variant = nbt.contains("Variant") ? nbt.getInt("Variant") : 0;

        return switch (variant) {
            case 0 -> LUCY_AXOLOTL_TEXTURE;
            case 1 -> WILD_AXOLOTL_TEXTURE;
            case 2 -> GOLD_AXOLOTL_TEXTURE;
            case 3 -> CYAN_AXOLOTL_TEXTURE;
            case 4 -> BLUE_AXOLOTL_TEXTURE;
            default -> WILD_AXOLOTL_TEXTURE;
        };
    }




}

