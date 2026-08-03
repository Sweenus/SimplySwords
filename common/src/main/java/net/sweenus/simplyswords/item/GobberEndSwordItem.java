package net.sweenus.simplyswords.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GobberEndSwordItem extends SwordItem {
    String[] repairIngredient;

    public GobberEndSwordItem(ToolMaterial toolMaterial, String... repairIngredient) {
        super(toolMaterial, 3, -2.4F, new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS));
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

    //Unbreakable weapon support for Gobber
    static boolean unbreakable = Config.general.compatGobberEndWeaponsUnbreakable.get();

    /* 1.21
    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player)
    {
        if(world.isClient) return;

        if(Config.general.compatGobberEndWeaponsUnbreakable.get();)
        {
            stack.getOrCreateNbt().putBoolean("Unbreakable", true);
        }
    }
     */

}
