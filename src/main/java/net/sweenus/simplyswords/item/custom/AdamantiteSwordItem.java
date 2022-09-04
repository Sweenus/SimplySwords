package net.sweenus.simplyswords.item.custom;

import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class AdamantiteSwordItem implements ToolMaterial {
    @Override
    public int getDurability() {
        return 600;
    }
    @Override
    public float getMiningSpeedMultiplier() {
        return 5.0F;
    }
    @Override
    public int getMiningLevel() {
        return 3;
    }
    @Override
    public int getEnchantability() {
        return 15;
    }
    @Override
    public Ingredient getRepairIngredient(){
        return Ingredient.ofItems();
    }
    @Override
    public float getAttackDamage() {
        return 3.0F;
    }
}
