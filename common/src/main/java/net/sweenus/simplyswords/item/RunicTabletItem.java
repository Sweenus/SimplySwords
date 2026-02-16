package net.sweenus.simplyswords.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.util.Styles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RunicTabletItem extends Item {

    public RunicTabletItem() {
        super( new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof());
    }


    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey()).setStyle(Styles.RUNIC);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        List<Text> tooltip = new ArrayList<>();

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.runic_tablet.tooltip").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.simplyswords.runic_tablet.tooltip2").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.runic_tablet.tooltip3").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.simplyswords.runic_tablet.tooltip4").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.forEach(textConsumer);
    }
}
