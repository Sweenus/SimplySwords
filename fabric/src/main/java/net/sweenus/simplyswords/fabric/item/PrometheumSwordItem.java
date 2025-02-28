package net.sweenus.simplyswords.fabric.item;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.world.World;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;
import nourl.mythicmetals.component.PrometheumComponent;

public class PrometheumSwordItem extends SimplySwordsSwordItem {
    String[] repairIngredient;
    public PrometheumSwordItem(ToolMaterial toolMaterial, Settings settings, String... repairIngredient) {
        super(toolMaterial, settings);
        this.repairIngredient = repairIngredient;
    }
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        PrometheumComponent.tickAutoRepair(stack, world);
        super.inventoryTick(stack, world, entity, slot, selected);
    }
}