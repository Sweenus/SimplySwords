package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow protected abstract void dropShoulderEntities();

    @Shadow @Final private PlayerAbilities abilities;

    @Unique
    public void simplySwords$invokeDropShoulderEntities() {
        dropShoulderEntities();
    }

    @Inject(at = @At("HEAD"), method = "dropShoulderEntities", cancellable = true)
    public void simplyswords$dropShoulderEntities(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (!HelperMethods.hasItemInInventory(player, ItemsRegistry.CHOMPOLOTL.get())) return;

        // Control how shoulder axolotl are dropped
        if (!player.isTouchingWater() && !player.isSneaking() && !player.isSleeping() && !player.inPowderSnow && !this.abilities.flying && player.isAlive()) {

            if (player.getShoulderEntityLeft() != null && "simplyswords:simplyaxolotlentity".equals(player.getShoulderEntityLeft().getString("id"))) {
                ci.cancel();
            }

            if (player.getShoulderEntityRight() != null && "simplyswords:simplyaxolotlentity".equals(player.getShoulderEntityRight().getString("id"))) {
                ci.cancel();
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void simplyswords$tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.age % 40 == 0) {
            // Drop axolotls if Chompolotl item not present
            if (!HelperMethods.hasItemInInventory(player, ItemsRegistry.CHOMPOLOTL.get())) {
                dropShoulderEntities();
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