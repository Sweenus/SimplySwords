package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.effect.ModEffects;

import java.util.List;

public class WatcherSwordItem extends SwordItem {
    public WatcherSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        int thitchance = SimplySwordsConfig.getIntValue("watcher_chance");
        int phitchance = SimplySwordsConfig.getIntValue("omen_chance");

        if (attacker.getRandom().nextInt(100) <= thitchance) {
            target.addStatusEffect(new StatusEffectInstance(ModEffects.WATCHER, 1, 1), attacker);
        }

        if (attacker.getRandom().nextInt(100) <= phitchance) {
            target.addStatusEffect(new StatusEffectInstance(ModEffects.OMEN, 1, 1), attacker);
        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip3",
                (SimplySwordsConfig.getFloatValue("omen_instantkill_threshold") * 100)));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip4"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip5").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip6"));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip7"));



        //1.18.2
                /*
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.watchersworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.watchersworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.watchersworditem.tooltip3",
                (SimplySwordsConfig.getFloatValue("omen_instantkill_threshold") * 100)));
        tooltip.add(new TranslatableText("item.simplyswords.watchersworditem.tooltip4"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.watchersworditem.tooltip5").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.watchersworditem.tooltip6"));
        tooltip.add(new TranslatableText("item.simplyswords.watchersworditem.tooltip7"));

                 */
    }

}
