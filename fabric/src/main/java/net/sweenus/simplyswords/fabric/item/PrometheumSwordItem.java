package net.sweenus.simplyswords.fabric.item;

import net.minecraft.item.AxeItem;
import net.minecraft.item.ToolMaterial;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;
import nourl.mythicmetals.armor.BanglumArmor;
import nourl.mythicmetals.item.tools.AutoRepairable;

public class PrometheumSwordItem extends SimplySwordsSwordItem implements AutoRepairable {
    public PrometheumSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, String... repairIngredient) {
        super(toolMaterial, attackDamage, attackSpeed, repairIngredient);
    }
}
