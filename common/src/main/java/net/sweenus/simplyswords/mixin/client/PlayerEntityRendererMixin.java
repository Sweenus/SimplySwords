package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.sweenus.simplyswords.client.ShadowDanceFovHandler;
import net.sweenus.simplyswords.client.renderer.feature.ShoulderAxolotlFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void simplyswords$hideShadowDancingPlayer(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (ShadowDanceFovHandler.isShadowDancing(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplyswords$addCustomShoulderFeature(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        // Call the protected addFeature method via the accessor mixin
        System.out.println("Simply Swords: Adding axolotl shoulder feature to client");
        ((LivingEntityRendererAccessor) this).invokeAddFeature(new ShoulderAxolotlFeatureRenderer(
                (FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>) this,
                context.getPart(EntityModelLayers.AXOLOTL)
        ));
    }
}



