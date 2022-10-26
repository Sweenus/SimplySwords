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

import java.util.List;

public class PlagueSwordItem extends SwordItem {
    public PlagueSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        int phitchance = (int) SimplySwordsConfig.getFloatValue("plague_chance");

        if (attacker.getRandom().nextInt(100) <= phitchance) {

            //Convert Haste
            if (target.hasStatusEffect(StatusEffects.HASTE)) {
                var statdur = (target.getStatusEffect(StatusEffects.SLOWNESS).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.HASTE);
            }

            //Convert Regeneration
            if (target.hasStatusEffect(StatusEffects.REGENERATION)) {
                var statdur = (target.getStatusEffect(StatusEffects.REGENERATION).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.REGENERATION).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.REGENERATION);
            }

            //Convert Strength
            if (target.hasStatusEffect(StatusEffects.STRENGTH)) {
                var statdur = (target.getStatusEffect(StatusEffects.STRENGTH).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.STRENGTH).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.STRENGTH);
            }

            //Convert Speed
            if (target.hasStatusEffect(StatusEffects.SPEED)) {
                var statdur = (target.getStatusEffect(StatusEffects.SPEED).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.SPEED).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.SPEED);
            }

            //Convert Invisibility
            if (target.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                var statdur = (target.getStatusEffect(StatusEffects.INVISIBILITY).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.INVISIBILITY).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.INVISIBILITY);
            }

            //Convert Resistance
            if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
                var statdur = (target.getStatusEffect(StatusEffects.RESISTANCE).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.RESISTANCE);
            }

            //Convert Saturation
            if (target.hasStatusEffect(StatusEffects.SATURATION)) {
                var statdur = (target.getStatusEffect(StatusEffects.SATURATION).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.SATURATION).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.SATURATION);
            }

            //Convert Fire Resistance
            if (target.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                var statdur = (target.getStatusEffect(StatusEffects.FIRE_RESISTANCE).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.FIRE_RESISTANCE).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
            }

            //Convert Absorption
            if (target.hasStatusEffect(StatusEffects.ABSORPTION)) {
                var statdur = (target.getStatusEffect(StatusEffects.ABSORPTION).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.ABSORPTION).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, statamp), attacker);
                target.removeStatusEffect(StatusEffects.ABSORPTION);
            }


        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip3"));

        /*

        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.plaguesworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.plaguesworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.plaguesworditem.tooltip3"));

         */
    }

}
