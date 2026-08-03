package net.sweenus.simplyswords.mixin;

import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.loot.PityLootContext;
import net.sweenus.simplyswords.loot.PityLootManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootableContainerBlockEntity.class)
public abstract class LootableInventoryMixin {
    @Shadow protected Identifier lootTableId;

    @Inject(method = "checkLootInteraction", at = @At("HEAD"))
    private void simplyswords$captureLootTable(PlayerEntity player, CallbackInfo ci) {
        PityLootContext.capture(lootTableId);
    }

    @Inject(method = "checkLootInteraction", at = @At("RETURN"))
    private void simplyswords$applyPityLoot(PlayerEntity player, CallbackInfo ci) {
        LootableContainerBlockEntity container = (LootableContainerBlockEntity) (Object) this;
        Identifier table = PityLootContext.take();
        if (table != null) {
            PityLootManager.applyGeneratedContainerLoot(
                    (Inventory) container, table, player, container.getPos(), container.getWorld());
        }
    }
}
