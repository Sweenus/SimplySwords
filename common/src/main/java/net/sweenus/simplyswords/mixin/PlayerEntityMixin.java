package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow protected abstract void dropShoulderEntities();

    @Shadow @Final private PlayerAbilities abilities;

    @Inject(at = @At("HEAD"), method = "dropShoulderEntities", cancellable = true)
    public void simplyswords$dropShoulderEntities(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        // Control how shoulder axolotl are dropped
        if (!player.isTouchingWater() && !player.isSneaking() && !player.isSleeping() && !player.inPowderSnow && !this.abilities.flying) {

            if (player.getShoulderEntityLeft() != null && "simplyswords:simplyaxolotlentity".equals(player.getShoulderEntityLeft().getString("id"))) {
                ci.cancel();
            }

            if (player.getShoulderEntityRight() != null && "simplyswords:simplyaxolotlentity".equals(player.getShoulderEntityRight().getString("id"))) {
                ci.cancel();
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "tickMovement")
    public void simplyswords$tickMovement(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getWorld().isClient() && (player.isTouchingWater() || player.isSneaking())) {
            this.dropShoulderEntities();
        }
    }
}