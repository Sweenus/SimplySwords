package net.sweenus.simplyswords.item.custom;


import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

public class LevitationSwordItem extends SwordItem {
    public LevitationSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        int lhitchance = SimplySwordsConfig.getIntValue("levitation_chance");
        int lduration = SimplySwordsConfig.getIntValue("levitation_duration");

        if (attacker.getRandom().nextInt(100) <= lhitchance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, lduration, 3), attacker);
        }

        return super.postHit(stack, target, attacker);
    }

}
