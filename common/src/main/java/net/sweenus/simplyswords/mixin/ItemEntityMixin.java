package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.world.DevourerAbilityManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void simplyswords$preventCapturedPickup(PlayerEntity player, CallbackInfo ci) {
        if (DevourerAbilityManager.isCapturedLoot((ItemEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
