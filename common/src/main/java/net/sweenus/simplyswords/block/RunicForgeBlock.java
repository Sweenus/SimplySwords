package net.sweenus.simplyswords.block;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sweenus.simplyswords.screen.RunicForgeScreenHandler;

public class RunicForgeBlock extends Block {
    public RunicForgeBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            SimpleNamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, owner) ->
                            new RunicForgeScreenHandler(syncId, inventory, pos),
                    Text.translatable("container.simplyswords.runic_forge")
            );
            MenuRegistry.openExtendedMenu(serverPlayer, factory, buf -> buf.writeBlockPos(pos));
        }
        return ActionResult.success(world.isClient);
    }
}
