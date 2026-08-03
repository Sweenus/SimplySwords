package net.sweenus.simplyswords.compat.mythicmetals;

import nourl.mythicmetals.item.tools.PalladiumToolSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;

public class PalladiumSwordItem extends SimplySwordsSwordItem {
    public PalladiumSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        PalladiumToolSet.applyHeatToTarget(target, attacker);
        return super.postHit(stack, target, attacker);
    }
}
