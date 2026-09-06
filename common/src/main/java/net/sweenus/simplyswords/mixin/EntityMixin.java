package net.sweenus.simplyswords.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.sweenus.simplyswords.entity.RiftmaneChargerEntity;
import net.sweenus.simplyswords.world.GloamStainManager;
import net.sweenus.simplyswords.world.SoulPyreAbilityManager;
import net.sweenus.simplyswords.world.ObserverStatusEffectSyncManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void simplyswords$protectPhasingRiftmaneRider(CallbackInfoReturnable<Boolean> cir) {
        if (((Entity) (Object) this).getVehicle() instanceof RiftmaneChargerEntity charger && charger.isPhasing()) {
            cir.setReturnValue(false);
        }
    }

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

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void simplyswords$playGloamStepSound(
            BlockPos pos, BlockState state, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!GloamStainManager.isOnGloam(entity)) {
            return;
        }
        entity.playSound(SoundEvents.BLOCK_WET_SPONGE_STEP, 0.15F,
                0.72F + entity.getRandom().nextFloat() * 0.16F);
        entity.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 0.065F,
                0.62F + entity.getRandom().nextFloat() * 0.16F);
        ci.cancel();
    }
}
