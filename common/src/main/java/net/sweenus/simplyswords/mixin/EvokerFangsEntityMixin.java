package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.sweenus.simplyswords.world.EvocationFangManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EvokerFangsEntity.class)
public abstract class EvokerFangsEntityMixin {

    @Inject(method = "damage(Lnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private void simplyswords$cancelVisualOnlyFangDamage(LivingEntity target, CallbackInfo ci) {
        if (((Entity) (Object) this).getCommandTags().contains(EvocationFangManager.VISUAL_ONLY_TAG)) {
            ci.cancel();
        }
    }
}
