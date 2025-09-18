//This class is intended to duplicate the behavior of the weapons from Advanced Netherite,
//for example, being fire resistant, and having material specific bonuses. Not fully implemented, I haven't figured out how to bring over the attributes from the mod.

//I think dragonloot can just use the netheriteSwordItem class because it's a smithing template upgrade and just has better stats
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

        //Netherite alloys use GRAY, GOLD, DARK_GREEN, and AQUA colors

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