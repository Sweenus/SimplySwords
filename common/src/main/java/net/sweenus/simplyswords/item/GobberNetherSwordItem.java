package net.sweenus.simplyswords.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GobberNetherSwordItem extends Item {
    String[] repairIngredient;

    public GobberNetherSwordItem(ToolMaterial toolMaterial, String... repairIngredient) {
        super(new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS));
        this.repairIngredient = repairIngredient;
    }

    public GobberNetherSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, String... repairIngredient) {
        super(new Item.Settings()
                .arch$tab(SimplySwords.SIMPLYSWORDS)
                .attributeModifiers(net.sweenus.simplyswords.util.HelperMethods.createSwordAttributeModifiers(toolMaterial, attackDamage, attackSpeed)));
        this.repairIngredient = repairIngredient;
    }

    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        List<Item> potentialIngredients = new ArrayList<>(List.of());
        Arrays.stream(repairIngredient).toList().forEach(repIngredient ->
                potentialIngredients.add(
                        Registries.ITEM.get(Identifier.of(repIngredient))));


        return potentialIngredients.contains(ingredient.getItem());
    }

    //Nether ignite on hit
    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setOnFireFor(10);


        super.postHit(stack, target, attacker);

    }
}
