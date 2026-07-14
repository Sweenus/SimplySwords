package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetPredicate.class)
public abstract class TargetPredicateMixin {

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void simplyswords$rejectShadowDancingTargets(LivingEntity baseEntity, LivingEntity targetEntity, CallbackInfoReturnable<Boolean> cir) {
        if (targetEntity instanceof ServerPlayerEntity player && ShadowstingShadowDanceManager.isActive(player)) {
            cir.setReturnValue(false);
        }
    }
}
