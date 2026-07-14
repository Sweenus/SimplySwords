package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void simplyswords$ignoreShadowDancingTargets(LivingEntity target, CallbackInfo ci) {
        if (target instanceof ServerPlayerEntity player && ShadowstingShadowDanceManager.isActive(player)) {
            ci.cancel();
        }
    }
}
