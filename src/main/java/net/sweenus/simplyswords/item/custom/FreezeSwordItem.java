package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.effect.ModEffects;

import java.text.Format;
import java.util.List;

public class FreezeSwordItem extends SwordItem {
    public FreezeSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int fhitchance = SimplySwordsConfig.getIntValue("freeze_chance");
        int fduration = SimplySwordsConfig.getIntValue("freeze_duration");
        int sduration = SimplySwordsConfig.getIntValue("slowness_duration");

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 1), attacker);

        if (attacker.getRandom().nextInt(100) <= fhitchance) {
            target.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, fduration, 1), attacker);
        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2"));
    }

}
