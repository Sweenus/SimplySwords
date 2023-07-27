package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class RunefusedGemItem extends Item {

    public RunefusedGemItem() {
        super(new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof().maxCount(1));
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {

        if (stack.getOrCreateNbt().getString("runic_power").isEmpty()) {

            String runefusedPowerSelection = HelperMethods.chooseRunefusedPower();
            stack.getOrCreateNbt().putString("runic_power", runefusedPowerSelection);
        }

        return false;
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        if (world.isClient) return;

        String runefusedPowerSelection = HelperMethods.chooseRunefusedPower();
        stack.getOrCreateNbt().putString("runic_power", runefusedPowerSelection);

    }

    @Override
    public Text getName(ItemStack stack) {
        Style RUNIC = HelperMethods.getStyle("runic");
        return Text.translatable(this.getTranslationKey(stack)).setStyle(RUNIC);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RUNIC = HelperMethods.getStyle("runic");
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));

        if (itemStack.getOrCreateNbt().getString("runic_power").isEmpty()) {
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").setStyle(RUNIC));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2").setStyle(TEXT));
        } else {
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("greater")) {
                tooltip.add(Text.translatable("item.simplyswords.greater_runic_power").setStyle(RUNIC));
            }
            switch (itemStack.getOrCreateNbt().getString("runic_power")) {
                case "freeze" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.freeze").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2").setStyle(TEXT));
                }
                case "wildfire" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.wildfire").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip3").setStyle(TEXT));
                }
                case "slow", "greater_slow" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.slow").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip3").setStyle(TEXT));
                }
                case "swiftness", "greater_swiftness" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.swiftness").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip3").setStyle(TEXT));
                }
                case "float", "greater_float" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.float").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip3").setStyle(TEXT));
                }
                case "zephyr", "greater_zephyr" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.zephyr").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip3").setStyle(TEXT));
                }
                case "shielding", "greater_shielding" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.shielding").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip3").setStyle(TEXT));
                }
                case "stoneskin", "greater_stoneskin" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.stoneskin").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip3").setStyle(TEXT));
                }
                case "frost_ward" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.frost_ward").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip3").setStyle(TEXT));
                }
                case "trailblaze", "greater_trailblaze" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.trailblaze").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip3").setStyle(TEXT));
                }
                case "active_defence" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.active_defence").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip3").setStyle(TEXT));
                }
                case "weaken", "greater_weaken" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.weaken").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip3").setStyle(TEXT));
                }
                case "unstable" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.unstable").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip3").setStyle(TEXT));
                }
                case "momentum", "greater_momentum" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.momentum").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip3").setStyle(TEXT));
                }
                case "imbued", "greater_imbued" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.imbued").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip3").setStyle(TEXT));
                }
                case "pincushion", "greater_pincushion" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.pincushion").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip3").setStyle(TEXT));
                }
                case "ward" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.ward").setStyle(RUNIC));
                    tooltip.add(Text.literal(""));
                    tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
                    tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip3").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip4").setStyle(TEXT));
                }
                case "immolation" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.immolation").setStyle(RUNIC));
                    tooltip.add(Text.literal(""));
                    tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
                    tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip3").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip4").setStyle(TEXT));
                }
            }
        }
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.gem_description").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.simplyswords.gem_description2").formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
