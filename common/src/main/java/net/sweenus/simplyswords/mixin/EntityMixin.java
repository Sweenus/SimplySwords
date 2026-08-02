package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.sweenus.simplyswords.world.SoulPyreAbilityManager;
import net.sweenus.simplyswords.world.ObserverStatusEffectSyncManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void simplyswords$removeObserverStatusEffects(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity livingEntity) {
            ObserverStatusEffectSyncManager.removeEntity(livingEntity);
        }
    }

    @Inject(method = "isInLava", at = @At("RETURN"), cancellable = true)
    private void simplyswords$reportSoulPyreLava(
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                && SoulPyreAbilityManager.isInTransmutedLava(
                (Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setOnFireFromLava", at = @At("HEAD"), cancellable = true)
    private void simplyswords$applySoulPyreLava(CallbackInfo ci) {
        if (SoulPyreAbilityManager.handleTransmutedLavaContact(
                (Entity) (Object) this)) {
            ci.cancel();
        }
    }
}
