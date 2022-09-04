package net.sweenus.simplyswords.item.custom;


import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

public class PlagueSwordItem extends SwordItem {
    public PlagueSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        int phitchance = SimplySwordsConfig.getIntValue("plague_chance");
        int pduration = SimplySwordsConfig.getIntValue("plague_duration");

        if (attacker.getRandom().nextInt(100) <= phitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, pduration, 1), attacker);
        }

        return super.postHit(stack, target, attacker);

    }

}
