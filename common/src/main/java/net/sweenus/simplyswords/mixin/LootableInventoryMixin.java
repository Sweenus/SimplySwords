package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.sweenus.simplyswords.loot.PityLootContext;
import net.sweenus.simplyswords.loot.PityLootManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootableInventory.class)
public interface LootableInventoryMixin {
    @Inject(method = "generateLoot", at = @At("HEAD"))
    private void simplyswords$captureLootTable(PlayerEntity player, CallbackInfo ci) {
        LootableInventory inventory = (LootableInventory) this;
        PityLootContext.capture(inventory.getLootTable());
    }

    @Inject(method = "generateLoot", at = @At("RETURN"))
    private void simplyswords$applyPityLoot(PlayerEntity player, CallbackInfo ci) {
        LootableInventory inventory = (LootableInventory) this;
        RegistryKey<LootTable> table = PityLootContext.take();
        if (table != null) {
            PityLootManager.applyGeneratedContainerLoot(
                    inventory, table, player, inventory.getPos(), inventory.getWorld());
        }
    }
}
