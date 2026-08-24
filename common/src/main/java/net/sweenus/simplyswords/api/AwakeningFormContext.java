package net.sweenus.simplyswords.api;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

//
// Authoritative server context used when a Runic Forge first selects a route.
// Route selection may run repeatedly while generating previews and must be
// deterministic and side-effect free.
//
public record AwakeningFormContext(
        ServerWorld world,
        ServerPlayerEntity player,
        BlockPos forgePos,
        ItemStack sourceStack,
        int originalLevel,
        int targetLevel
) {
    public AwakeningFormContext {
        sourceStack = sourceStack.copy();
        forgePos = forgePos.toImmutable();
    }
}
