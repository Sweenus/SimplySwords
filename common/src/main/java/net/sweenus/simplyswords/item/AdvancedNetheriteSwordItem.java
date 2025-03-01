package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdvancedNetheriteSwordItem extends SwordItem {
    String[] repairIngredient;

    public AdvancedNetheriteSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, String... repairIngredient) {
        super(toolMaterial, attackDamage, attackSpeed,
                new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).fireproof());
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

        //TODO: Set RGB values to proper ones as soon as I can find them via digging in AN source
        int rgbNetheriteIron = 0x000000;
        int rgbNetheriteGold = 0x000000;
        int rgbNetheriteEmerald = 0x000000;
        int rgbNetheriteDiamond = 0x000000;

        //Leftover stuff from SimplySwordsSwordItem.java which I don't know what is used for

        //Style PROMETHEUM = Style.EMPTY.withColor(TextColor.fromRgb(rgbPrometheum));
        //Style CARMOT = Style.EMPTY.withColor(TextColor.fromRgb(rgbCarmot));

        if (this.getName(itemStack).getString().contains("Netherite-Iron"));
            //Add Netherite-Iron tooltip
        else if (this.getName(itemStack).getString().contains("Netherite-Gold"));
            //Add Netherite-Gold tooltip
        else if (this.getName(itemStack).getString().contains("Netherite-Emerald"));
            //Add Netherite-Emerald tooltip
        else if (this.getName(itemStack).getString().contains("Netherite-Diamond"));
            //Add Netherite-Iron, -Gold, AND -Emerald tooltips

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}

