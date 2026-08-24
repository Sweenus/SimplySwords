package net.sweenus.simplyswords.api;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

//
// Exact before/after result of a completed Runic Forge transaction.
//
public record AwakeningFormCommitContext(
        ServerWorld world,
        ServerPlayerEntity player,
        BlockPos forgePos,
        ItemStack sourceStack,
        ItemStack resultStack,
        int originalLevel,
        int targetLevel
) {
    public AwakeningFormCommitContext {
        sourceStack = sourceStack.copy();
        resultStack = resultStack.copy();
        forgePos = forgePos.toImmutable();
    }
}
