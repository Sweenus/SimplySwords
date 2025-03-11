package net.sweenus.simplyswords.item;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ContainedRemnantItem extends Item {

    public ContainedRemnantItem() {
        super( new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof().maxCount(1));
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
        if (this.asItem().equals(ItemsRegistry.CONTAINED_REMNANT.get())) {
            if (Screen.hasAltDown()) {
                tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description7").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description8").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.simplyswords.contained_remnant_description9").formatted(Formatting.GRAY));
            } else {
                tooltip.add(Text.translatable("item.simplyswords.common.showtooltip.info").formatted(Formatting.GRAY));
            }
        }

    }
}