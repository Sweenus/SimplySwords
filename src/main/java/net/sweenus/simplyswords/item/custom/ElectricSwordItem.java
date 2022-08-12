package net.sweenus.simplyswords.item.custom;


import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.effect.ModEffects;

public class ElectricSwordItem extends SwordItem {
    public ElectricSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int fhitchance = SimplySwordsConfig.getIntValue("freeze_chance");
        int fduration = SimplySwordsConfig.getIntValue("freeze_duration");
        int sduration = SimplySwordsConfig.getIntValue("slowness_duration");


        if (attacker.getRandom().nextInt(100) <= fhitchance) {
            target.addStatusEffect(new StatusEffectInstance(ModEffects.BURN, 5, 1), attacker);
        }

        return super.postHit(stack, target, attacker);

    }

}
