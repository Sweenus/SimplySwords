package net.sweenus.simplyswords.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.config.LootConfig;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;
import java.util.Map;

public class ContainedRemnantItem extends Item {

    public ContainedRemnantItem() {
        super( new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof().maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {

        BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
        PlayerEntity player = context.getPlayer();
        ItemStack heldStack = context.getStack();
        BlockPos blockPos = context.getBlockPos();

        if (player != null && !player.getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) player.getWorld();
            Map<Block, Item> transformationMap = Map.ofEntries(
                    Map.entry(Blocks.SWEET_BERRY_BUSH, ItemsRegistry.BRAMBLETHORN.get()),
                    Map.entry(Blocks.FURNACE, ItemsRegistry.HEARTHFLAME.get()),
                    Map.entry(Blocks.CANDLE, ItemsRegistry.WICKPIERCER.get()),
                    Map.entry(Blocks.BOOKSHELF, ItemsRegistry.WAXWEAVER.get()),
                    Map.entry(Blocks.AMETHYST_BLOCK, ItemsRegistry.ARCANETHYST.get()),
                    Map.entry(Blocks.RED_WOOL, ItemsRegistry.RIBBONCLEAVER.get()),
                    Map.entry(Blocks.ICE, ItemsRegistry.FROSTFALL.get()),
                    Map.entry(Blocks.LAVA_CAULDRON, ItemsRegistry.MOLTEN_EDGE.get()),
                    Map.entry(Blocks.MAGMA_BLOCK, ItemsRegistry.BRIMSTONE_CLAYMORE.get()),
                    Map.entry(Blocks.FIRE, ItemsRegistry.EMBERLASH.get()),
                    Map.entry(Blocks.SNOW_BLOCK, ItemsRegistry.ICEWHISPER.get()),
                    Map.entry(Blocks.DEEPSLATE, ItemsRegistry.SHADOWSTING.get()),
                    Map.entry(Blocks.SPORE_BLOSSOM, ItemsRegistry.TOXIC_LONGSWORD.get()),
                    Map.entry(Blocks.IRON_BLOCK, ItemsRegistry.MJOLNIR.get()),
                    Map.entry(Blocks.LIGHTNING_ROD, ItemsRegistry.STORMBRINGER.get()),
                    Map.entry(Blocks.BEE_NEST, ItemsRegistry.HIVEHEART.get()),
                    Map.entry(Blocks.IRON_ORE, ItemsRegistry.TWISTED_BLADE.get()),
                    Map.entry(Blocks.CAMPFIRE, ItemsRegistry.EMBERBLADE.get()),
                    Map.entry(Blocks.SOUL_LANTERN, ItemsRegistry.SOULKEEPER.get()),
                    Map.entry(Blocks.SOUL_SOIL, ItemsRegistry.SOULSTEALER.get()),
                    Map.entry(Blocks.SOUL_CAMPFIRE, ItemsRegistry.SOULPYRE.get()),
                    Map.entry(Blocks.SOUL_SAND, ItemsRegistry.SOULRENDER.get()),
                    Map.entry(Blocks.SOUL_FIRE, ItemsRegistry.SLUMBERING_LICHBLADE.get()),
                    Map.entry(Blocks.BLAST_FURNACE, ItemsRegistry.FLAMEWIND.get()),
                    Map.entry(Blocks.OBSIDIAN, ItemsRegistry.WATCHER_CLAYMORE.get()),
                    Map.entry(Blocks.CRYING_OBSIDIAN, ItemsRegistry.WATCHING_WARGLAIVE.get()),
                    Map.entry(Blocks.POWDER_SNOW, ItemsRegistry.LIVYATAN.get()),
                    Map.entry(Blocks.END_PORTAL_FRAME, ItemsRegistry.CAELESTIS.get()),
                    Map.entry(Blocks.TUBE_CORAL_BLOCK, ItemsRegistry.CHOMPOLOTL.get()),
                    Map.entry(Blocks.FIRE_CORAL_BLOCK, ItemsRegistry.TEMPEST.get()),
                    Map.entry(Blocks.SUSPICIOUS_SAND, ItemsRegistry.DORMANT_RELIC.get()),
                    Map.entry(Blocks.CYAN_BANNER, ItemsRegistry.WHISPERWIND.get()),
                    Map.entry(Blocks.SKELETON_SKULL, ItemsRegistry.WRAITHFANG.get()),
                    Map.entry(Blocks.CAULDRON, ItemsRegistry.THUNDERBRAND.get()),
                    Map.entry(Blocks.SAND, ItemsRegistry.STARS_EDGE.get())
            );

            Item transformedItem = transformationMap.get(blockState.getBlock());
            if (transformedItem != null) {

                if (LootConfig.INSTANCE.disabledUniqueWeaponLoot.contains(transformedItem))
                    return ActionResult.PASS;

                if (transformedItem.equals(ItemsRegistry.STARS_EDGE.get())) {
                    boolean isNight = serverWorld.isNight();
                    boolean canSeeSky = serverWorld.isSkyVisible(context.getBlockPos().up(1));
                    if (!isNight || !canSeeSky) {
                        return ActionResult.PASS;
                    }
                }

                ItemStack newItem = new ItemStack(transformedItem);
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
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description2").formatted(Formatting.GRAY));
        if (this.equals(ItemsRegistry.TAMPERED_REMNANT.get())) {
            tooltip.add(Text.translatable("item.simplyswords.tampered_remnant_description3").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("item.simplyswords.tampered_remnant_description4").formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description3").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description4").formatted(Formatting.GRAY));
        }
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description5").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description6").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        generateDynamicTooltip(itemStack, tooltipContext, tooltip, type);
        if (this.asItem().equals(ItemsRegistry.CONTAINED_REMNANT.get())) {
            if (Screen.hasAltDown()) {
                tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description7").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description8").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description9").formatted(Formatting.GRAY));
            }
        }
    }
    protected void generateDynamicTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, tooltipContext, tooltip, type,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                null);
    }
}