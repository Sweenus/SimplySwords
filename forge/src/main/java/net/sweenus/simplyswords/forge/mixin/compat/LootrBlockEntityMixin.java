package net.sweenus.simplyswords.forge.mixin.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.sweenus.simplyswords.loot.PityLootManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "noobanidus.mods.lootr.block.entities.LootrChestBlockEntity",
        "noobanidus.mods.lootr.block.entities.LootrBarrelBlockEntity",
        "noobanidus.mods.lootr.block.entities.LootrShulkerBlockEntity"
}, remap = false)
public abstract class LootrBlockEntityMixin {

    @Shadow(remap = false)
    public abstract Identifier getTable();

    @Shadow(remap = false)
    public abstract BlockPos getPosition();

    // Lootr 0.7 fills a separate per-player inventory without calling
    // LootableContainerBlockEntity.checkLootInteraction. Bridge the completed
    // fill into the same post-generation path used by vanilla containers.
    @Inject(method = "unpackLootTable", at = @At("RETURN"), remap = false, require = 0)
    private void simplyswords$applyPityLoot(
            PlayerEntity player,
            Inventory inventory,
            Identifier lootTableId,
            long seed,
            CallbackInfo ci) {
        Identifier table = lootTableId != null ? lootTableId : getTable();
        if (table == null) {
            return;
        }

        PityLootManager.applyGeneratedContainerLoot(
                inventory, table, player, getPosition(), player.getWorld());
    }
}
