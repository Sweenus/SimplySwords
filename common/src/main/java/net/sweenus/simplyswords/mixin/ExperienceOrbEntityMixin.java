package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.world.DevourerAbilityManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void simplyswords$preventCapturedPickup(PlayerEntity player, CallbackInfo ci) {
        if (DevourerAbilityManager.isCapturedLoot((ExperienceOrbEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
