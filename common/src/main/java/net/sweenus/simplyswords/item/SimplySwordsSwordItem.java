package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimplySwordsSwordItem extends SwordItem {
    String[] repairIngredient;

    public SimplySwordsSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, String... repairIngredient) {
        super(toolMaterial, attackDamage, attackSpeed,
                new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS));
        this.repairIngredient = repairIngredient;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        List<Item> potentialIngredients = new ArrayList<>(List.of());
        Arrays.stream(repairIngredient).toList().forEach(repIngredient ->
            potentialIngredients.add(
                    Registries.ITEM.get(new Identifier(repIngredient))));


        return potentialIngredients.contains(ingredient.getItem());
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {


        int rgbPrometheum = 0x3A6A56;
        int rgbCarmot = 0xE63E73;
        Style PROMETHEUM = Style.EMPTY.withColor(TextColor.fromRgb(rgbPrometheum));
        Style CARMOT = Style.EMPTY.withColor(TextColor.fromRgb(rgbCarmot));

        if (this.getName(itemStack).getString().contains("Prometheum"))
            tooltip.add(Text.translatable("item.simplyswords.compat.mythicmetals.regrowth").setStyle(PROMETHEUM));
        else if (this.getName(itemStack).getString().contains("Carmot"))
            tooltip.add(Text.translatable("item.simplyswords.compat.mythicmetals.looting").setStyle(CARMOT));

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
