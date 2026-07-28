package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.sweenus.simplyswords.item.component.MoltenHeatComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.MoltenEdgeAbilityManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow protected abstract void dropShoulderEntities();

    @Shadow @Final private PlayerAbilities abilities;

    @Shadow protected abstract void setShoulderEntityLeft(NbtCompound nbt);

    @Shadow protected abstract void setShoulderEntityRight(NbtCompound nbt);

    @Invoker("dropShoulderEntity")
    protected abstract void simplyswords$dropShoulderEntity(NbtCompound nbt);

    @Unique
    public void simplySwords$invokeDropShoulderEntities() {
        dropShoulderEntities();
    }

    @Unique
    private boolean simplyswords$isSimplyAxolotl(NbtCompound nbt) {
        return nbt != null && !nbt.isEmpty() && "simplyswords:simplyaxolotlentity".equals(nbt.getString("id"));
    }

    @Unique
    private void simplyswords$dropSimplyAxolotls() {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) {
            return;
        }

        NbtCompound left = player.getShoulderEntityLeft();
        if (simplyswords$isSimplyAxolotl(left)) {
            simplyswords$dropShoulderEntity(left);
            setShoulderEntityLeft(new NbtCompound());
        }

        NbtCompound right = player.getShoulderEntityRight();
        if (simplyswords$isSimplyAxolotl(right)) {
            simplyswords$dropShoulderEntity(right);
            setShoulderEntityRight(new NbtCompound());
        }
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
                simplyswords$dropSimplyAxolotls();
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "tickMovement")
    public void simplyswords$tickMovement(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getWorld().isClient() && (player.isTouchingWater() || player.isSneaking())) {
            simplyswords$dropSimplyAxolotls();
        }
    }

    @Inject(
            method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
            at = @At("HEAD")
    )
    private void simplyswords$resetDroppedMoltenEdge(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getWorld().isClient() && stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())) {
            stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
            MoltenEdgeAbilityManager.cancelVent(player, stack);
        }
    }
}
