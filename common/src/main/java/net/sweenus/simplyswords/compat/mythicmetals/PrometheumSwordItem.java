package net.sweenus.simplyswords.compat.mythicmetals;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.world.World;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;
import nourl.mythicmetals.item.tools.PrometheumToolSet;

public class PrometheumSwordItem extends SimplySwordsSwordItem {
    public PrometheumSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        PrometheumToolSet.tickAutoRepair(stack, world);
        super.inventoryTick(stack, world, entity, slot, selected);
    }
}
