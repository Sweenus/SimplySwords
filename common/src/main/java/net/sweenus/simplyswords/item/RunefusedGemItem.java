package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
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
        super( new Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof().maxCount(1));
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {

        if(stack.getOrCreateNbt().getString("runic_power").isEmpty()) {

            String runefusedPowerSelection = HelperMethods.chooseRunefusedPower();
            stack.getOrCreateNbt().putString("runic_power", runefusedPowerSelection);
        }

        return false;
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player)
    {
        if(world.isClient) return;

        String runefusedPowerSelection = HelperMethods.chooseRunefusedPower();
        stack.getOrCreateNbt().putString("runic_power", runefusedPowerSelection);

    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD);
    }


    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));

        if (itemStack.getOrCreateNbt().getString("runic_power").contains("greater"))
            tooltip.add(Text.translatable("item.simplyswords.greater_runic_power").formatted(Formatting.DARK_AQUA, Formatting.BOLD));

        if (itemStack.getOrCreateNbt().getString("runic_power").isEmpty()) {

            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("freeze")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.freeze").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("wildfire")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.wildfire").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("slow")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.slow").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("swiftness")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.swiftness").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("float")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.float").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("zephyr")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.zephyr").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("shielding")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.shielding").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("stoneskin")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.stoneskin").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("frost_ward")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.frost_ward").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("trailblaze")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.trailblaze").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("active_defence")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.active_defence").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("weaken")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.weaken").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("unstable")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.unstable").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("momentum")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.momentum").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("imbued")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.imbued").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("pincushion")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.pincushion").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("ward")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.ward").formatted(Formatting.AQUA));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip3"));
            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip4"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("immolation")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.immolation").formatted(Formatting.AQUA));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip3"));
            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip4"));

        }


        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.gem_description").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.simplyswords.gem_description2").formatted(Formatting.GRAY, Formatting.ITALIC));

    }
}
