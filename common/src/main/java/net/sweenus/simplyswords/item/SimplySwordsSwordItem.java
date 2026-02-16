package net.sweenus.simplyswords.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SimplySwordsSwordItem extends Item {
    String[] repairIngredient;

    public SimplySwordsSwordItem(ToolMaterial toolMaterial, Settings settings, String... repairIngredient) {
        super(settings);
        this.repairIngredient = repairIngredient;
    }

    public SimplySwordsSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(settings);
        this.repairIngredient = new String[]{};
    }

    public SimplySwordsSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, String... repairIngredient) {
        super(new Settings()
                .arch$tab(SimplySwords.SIMPLYSWORDS)
                .attributeModifiers(HelperMethods.createSwordAttributeModifiers(toolMaterial, attackDamage, attackSpeed)));
        this.repairIngredient = repairIngredient;
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        List<Text> tooltip = new ArrayList<>();
        generateDynamicTooltip(itemStack, tooltipContext, tooltip, type);
        tooltip.forEach(textConsumer);
    }

    // Override this with your own id & paths
    protected void generateDynamicTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, tooltipContext, tooltip, type,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                null);
    }


}
