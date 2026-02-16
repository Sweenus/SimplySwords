package net.sweenus.simplyswords.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.util.Styles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EmpoweredRemnantItem extends Item {

    public EmpoweredRemnantItem() {
        super( new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof());
    }


    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey()).setStyle(Styles.UNIQUE);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        List<Text> tooltip = new ArrayList<>();

        tooltip.add(Text.literal(""));
        generateDynamicTooltip(itemStack, tooltipContext, tooltip, type);
        if (TooltipUtils.isAltDown()) {
            tooltip.add(Text.translatable("item.simplyswords.remnant_description").formatted(Formatting.GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("item.simplyswords.remnant_description2").formatted(Formatting.GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("item.simplyswords.remnant_description3").formatted(Formatting.GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("item.simplyswords.remnant_description4").formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        tooltip.forEach(textConsumer);
    }

    protected void generateDynamicTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, tooltipContext, tooltip, type,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                null);
    }
}
