package net.sweenus.simplyswords.mixin.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.loot.PityLootManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "noobanidus.mods.lootr.common.api.data.DefaultLootFiller", remap = false)
public abstract class LootrDefaultLootFillerMixin {

    // Lootr fills a separate per-player inventory without calling
    // LootableInventory.generateLoot, so bridge its completed fill into the
    // same post-generation path used by vanilla containers.
    @Inject(method = "performFill", at = @At("RETURN"), remap = false, require = 0)
    private static void simplyswords$applyPityLoot(
            @Coerce Object provider,
            PlayerEntity player,
            RegistryKey<LootTable> lootTableKey,
            LootTable lootTable,
            Inventory inventory,
            LootContextParameterSet parameters,
            long seed,
            CallbackInfo ci) {
        Vec3d origin = parameters.getOptional(LootContextParameters.ORIGIN);
        if (origin == null) {
            return;
        }

        PityLootManager.applyGeneratedContainerLoot(
                inventory,
                lootTableKey,
                player,
                BlockPos.ofFloored(origin),
                parameters.getWorld());
    }
}
