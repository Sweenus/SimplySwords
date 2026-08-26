package net.sweenus.simplyswords.fabric.mixin.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.loot.PityLootManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.zestyblaze.lootr.entity.LootrChestMinecartEntity", remap = false)
public abstract class LootrChestMinecartMixin {

    @Inject(method = "addLoot", at = @At("RETURN"), remap = false, require = 0)
    private void simplyswords$applyPityLoot(
            PlayerEntity player,
            Inventory inventory,
            Identifier lootTableId,
            long seed,
            CallbackInfo ci) {
        if (lootTableId == null) {
            return;
        }

        Entity cart = (Entity) (Object) this;
        PityLootManager.applyGeneratedContainerLoot(
                inventory, lootTableId, player, cart.getBlockPos(), cart.getWorld());
    }
}
