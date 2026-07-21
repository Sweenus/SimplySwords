package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.client.ShadowDanceFovHandler;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"), cancellable = true)
    private void simplyswords$hideShadowDanceHands(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        if (ShadowDanceFovHandler.isShadowDancing(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                    ordinal = 1))
    private void simplyswords$poseStormbringerParry(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!stack.isOf(ItemsRegistry.STORMBRINGER.get()) || !player.isUsingItem() || player.getActiveHand() != hand) {
            return;
        }

        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == Arm.RIGHT ? 1 : -1;
        float useProgress = MathHelper.clamp((stack.getMaxUseTime(player) - player.getItemUseTimeLeft() + tickDelta) / 5.0F, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - useProgress) * (1.0F - useProgress);

        matrices.translate(side * -0.42F * eased, 0.08F * eased, -0.34F * eased);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-46.0F * eased));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -72.0F * eased));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 36.0F * eased));
        matrices.scale(1.0F + 0.08F * eased, 1.0F + 0.08F * eased, 1.0F + 0.08F * eased);
    }
}
