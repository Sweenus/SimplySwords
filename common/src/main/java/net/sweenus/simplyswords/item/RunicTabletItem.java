package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class RunicTabletItem extends Item {

    public RunicTabletItem() {
        super( new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof());
    }


    @Override
    public Text getName(ItemStack stack) {
        Style RUNIC = HelperMethods.getStyle("runic");
        return Text.translatable(this.getTranslationKey(stack)).setStyle(RUNIC);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.runic_tablet.tooltip").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.simplyswords.runic_tablet.tooltip2").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.runic_tablet.tooltip3").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.simplyswords.runic_tablet.tooltip4").formatted(Formatting.GRAY, Formatting.ITALIC));

    }
}
