package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.sweenus.simplyswords.entity.SoulstalkerStrideEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void simplyswords$standWhileStriding(LivingEntity entity, float limbAngle, float limbDistance,
                                                 float animationProgress, float headYaw, float headPitch,
                                                 CallbackInfo ci) {
        if (entity != null && entity.getVehicle() instanceof SoulstalkerStrideEntity) {
            ((EntityModel<?>) (Object) this).riding = false;
        }
    }
}
