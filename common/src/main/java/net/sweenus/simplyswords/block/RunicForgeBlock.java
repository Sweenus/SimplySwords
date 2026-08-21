package net.sweenus.simplyswords.block;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.BlockView;
import net.sweenus.simplyswords.screen.RunicForgeScreenHandler;

import java.util.List;

public class RunicForgeBlock extends HorizontalFacingBlock {
    public RunicForgeBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("block.simplyswords.runic_forge_description")
                .formatted(Formatting.GRAY));
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
