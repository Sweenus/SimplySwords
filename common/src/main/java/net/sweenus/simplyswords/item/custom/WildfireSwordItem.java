package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;

import java.util.List;

public class WildfireSwordItem extends SwordItem {
    public WildfireSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        int phitchance = (int) SimplySwordsConfig.getFloatValue("wildfire_chance");
        int pduration = (int) SimplySwordsConfig.getFloatValue("wildfire_duration");

        if (attacker.getRandom().nextInt(100) <= phitchance) {
            target.addStatusEffect(new StatusEffectInstance(EffectRegistry.WILDFIRE.get(), pduration, 3), attacker);
        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        /*
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.wildfiresworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.wildfiresworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.wildfiresworditem.tooltip3"));


        */
        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.wildfiresworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.wildfiresworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.wildfiresworditem.tooltip3"));
         
    }

}
