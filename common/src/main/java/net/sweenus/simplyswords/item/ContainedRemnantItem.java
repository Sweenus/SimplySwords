package net.sweenus.simplyswords.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.config.LootConfig;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ContainedRemnantItem extends Item {

    private static final int checkRadius = 10;
    private static final int itemCooldown = 140;
    private static final Map<UUID, Long> playerInternalCooldowns = new HashMap<>();


    public ContainedRemnantItem() {
        super( new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof().maxCount(1));
    }

    private static final Map<Block, Identifier> transformationMap = new HashMap<>();


    public static void addTransformation(Block block, Identifier identifier) {
        transformationMap.put(block, identifier);
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {

        BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
        PlayerEntity player = context.getPlayer();
        ItemStack heldStack = context.getStack();

        if (player != null && !player.getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) player.getWorld();

            if (!ItemsRegistry.ITEM.getRegistrar().contains(transformationMap.get(blockState.getBlock())))
                return ActionResult.PASS;

            Item transformedItem = ItemsRegistry.ITEM.getRegistrar().get(transformationMap.get(blockState.getBlock()));
            if (transformedItem != null) {

                if (LootConfig.INSTANCE.disabledUniqueWeaponLoot.contains(transformedItem))
                    return ActionResult.PASS;

                ItemStack newItem = AwakeningApi.initializeNaturalDrop(new ItemStack(transformedItem));
                heldStack.decrement(1);
                HelperMethods.spawnOrbitParticles(serverWorld, player.getPos(), ParticleTypes.CAMPFIRE_COSY_SMOKE, 1, 6);
                player.getWorld().playSound(null, player.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                        player.getSoundCategory(), 0.4f, 1.8f);

                player.dropItem(newItem, false);

                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }




    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).setStyle(Styles.LEGENDARY);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description").formatted(Formatting.GRAY));
        if (this.equals(ItemsRegistry.TAMPERED_REMNANT.get())) {
            tooltip.add(Text.translatable("item.simplyswords.tampered_remnant_description3").formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description3").formatted(Formatting.GRAY));
        }
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description5").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        generateDynamicTooltip(itemStack, world, tooltip, tooltipContext);
        if (this.asItem().equals(ItemsRegistry.CONTAINED_REMNANT.get())) {
            if (Screen.hasAltDown()) {
                tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description7").formatted(Formatting.GRAY));
            }
        }
    }
    protected void generateDynamicTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, world, tooltip, tooltipContext,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                null);
    }

    public static void checkNearbyBlocks(ServerPlayerEntity player) {
        World world = player.getWorld();

        if (world.isClient) {
            return;
        }

        long currentTick = world.getTime();

        UUID playerId = player.getUuid();
        long lastTick = playerInternalCooldowns.getOrDefault(playerId, 0L);
        if (currentTick - lastTick < itemCooldown) {
            return;
        }

        BlockPos playerPos = player.getBlockPos();

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        for (int x = -checkRadius; x <= checkRadius; x++) {
            for (int y = -checkRadius; y <= checkRadius; y++) {
                for (int z = -checkRadius; z <= checkRadius; z++) {
                    mutablePos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                    Block block = world.getBlockState(mutablePos).getBlock();

                    if (transformationMap.containsKey(block)) {
                        player.sendMessage(
                                Text.translatable("item.simplyswords.contained_remnant.event3"),
                                true
                        );

                        HelperMethods.spawnParticlesBetween(player, mutablePos, (ServerWorld) world, ParticleTypes.ENCHANT, 20);
                        player.getWorld().playSound(null, mutablePos, SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                                player.getSoundCategory(), 0.1f, 1.2f);

                        playerInternalCooldowns.put(playerId, currentTick);
                        return;
                    }
                }
            }
        }
    }

}
