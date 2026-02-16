package net.sweenus.simplyswords.compat.mythicmetals;

import com.mythicmetals.item.tools.PalladiumToolSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;

public class PalladiumSwordItem extends SimplySwordsSwordItem {
    public PalladiumSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        PalladiumToolSet.applyHeatToTarget(target, attacker);
        super.postHit(stack, target, attacker);
    }
}
