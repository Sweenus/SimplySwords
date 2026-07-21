package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.sweenus.simplyswords.client.ShadowDanceFovHandler;
import net.sweenus.simplyswords.client.renderer.ModernFieldRenderer;
import net.sweenus.simplyswords.client.renderer.feature.ShoulderAxolotlFeatureRenderer;
import net.sweenus.simplyswords.registry.EffectRegistry;
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

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    private void simplyswords$renderImmolationField(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        StatusEffectInstance immolation = player.getStatusEffect(EffectRegistry.getReference(EffectRegistry.IMMOLATION));
        if (immolation == null) {
            return;
        }

        ModernFieldRenderer.renderImmolation(matrices, vertexConsumers, player.age, Math.max(0.75F, immolation.getAmplifier()));
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


