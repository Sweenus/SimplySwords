package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import net.sweenus.simplyswords.client.renderer.ObserverStatusVisualRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class WaxEncasedLivingEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void simplyswords$hideSealedWaxPrisoner(LivingEntity entity, float yaw, float tickDelta,
                                                     MatrixStack matrices,
                                                     VertexConsumerProvider vertexConsumers,
                                                     int light, CallbackInfo ci) {
        if (WaxweaverEncasementManager.isVisuallySealed(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    private void simplyswords$renderObserverStatusVisuals(LivingEntity entity, float yaw, float tickDelta,
                                                           MatrixStack matrices,
                                                           VertexConsumerProvider vertexConsumers,
                                                           int light, CallbackInfo ci) {
        ObserverStatusVisualRenderer.render(entity, tickDelta, matrices, vertexConsumers, light);
    }
}
