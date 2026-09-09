package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.sweenus.simplyswords.world.MasteryAbsorptionTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAbsorptionMixin {
    @ModifyArg(method = "setAbsorptionAmount", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;setAbsorptionAmountUnclamped(F)V"), index = 0)
    private float simplyswords$consumeMasteryAbsorption(float amount) {
        MasteryAbsorptionTracker.consume((LivingEntity) (Object) this, amount);
        return amount;
    }

    @Inject(method = "setAbsorptionAmount", at = @At("TAIL"))
    private void simplyswords$updateAbsorptionCapacity(float amount, CallbackInfo ci) {
        MasteryAbsorptionTracker.updateCapacity((LivingEntity) (Object) this);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void simplyswords$expireAbsorptionBeforeSave(NbtCompound nbt, CallbackInfo ci) {
        MasteryAbsorptionTracker.tick((LivingEntity) (Object) this);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void simplyswords$saveAbsorption(NbtCompound nbt, CallbackInfo ci) {
        MasteryAbsorptionTracker.writeNbt((LivingEntity) (Object) this, nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void simplyswords$restoreAbsorption(NbtCompound nbt, CallbackInfo ci) {
        MasteryAbsorptionTracker.readNbt((LivingEntity) (Object) this, nbt);
    }
}
