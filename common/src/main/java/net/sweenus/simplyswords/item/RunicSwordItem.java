package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunicSwordItem extends SwordItem {
    public RunicSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        //FREEZE
        if(stack.getOrCreateNbt().getInt("runic_power") <= 25 && stack.getOrCreateNbt().getInt("runic_power") >= 1) {

            int fhitchance = (int) SimplySwordsConfig.getFloatValue("freeze_chance");
            int fduration = (int) SimplySwordsConfig.getFloatValue("freeze_duration");
            int sduration = (int) SimplySwordsConfig.getFloatValue("slowness_duration");

            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 1), attacker);

            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), fduration, 1), attacker);
            }
        }
        //WILDFIRE
        if(stack.getOrCreateNbt().getInt("runic_power") > 25 && stack.getOrCreateNbt().getInt("runic_power") <= 40) {
            int phitchance = (int) SimplySwordsConfig.getFloatValue("wildfire_chance");
            int pduration = (int) SimplySwordsConfig.getFloatValue("wildfire_duration");

            if (attacker.getRandom().nextInt(100) <= phitchance) {
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.WILDFIRE.get(), pduration, 3), attacker);
            }
        }
        //SLOW
        if(stack.getOrCreateNbt().getInt("runic_power") > 40 && stack.getOrCreateNbt().getInt("runic_power") <= 60) {
            int shitchance = (int) SimplySwordsConfig.getFloatValue("slowness_chance");
            int sduration = (int) SimplySwordsConfig.getFloatValue("slowness_duration");

            if (attacker.getRandom().nextInt(100) <= shitchance) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 3), attacker);
            }
        }
        //SPEED
        if(stack.getOrCreateNbt().getInt("runic_power") > 60 && stack.getOrCreateNbt().getInt("runic_power") <= 80) {
            int shitchance = (int) SimplySwordsConfig.getFloatValue("speed_chance");
            int sduration = (int) SimplySwordsConfig.getFloatValue("speed_duration");

            if (attacker.getRandom().nextInt(100) <= shitchance) {
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, sduration, 1), attacker);
            }
        }
        //LEVITATION
        if(stack.getOrCreateNbt().getInt("runic_power") > 80 && stack.getOrCreateNbt().getInt("runic_power") <= 100) {
            int lhitchance = (int) SimplySwordsConfig.getFloatValue("levitation_chance");
            int lduration = (int) SimplySwordsConfig.getFloatValue("levitation_duration");

            if (attacker.getRandom().nextInt(100) <= lhitchance) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, lduration, 3), attacker);
            }
        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player)
    {
        if(world.isClient) return;

        if(stack.getOrCreateNbt().getInt("runic_power") < 1)
        {
            int choose = (int) (Math.random() * 100);
            stack.getOrCreateNbt().putInt("runic_power", choose);
        }
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        if(itemStack.getOrCreateNbt().getInt("runic_power") <= 25 && itemStack.getOrCreateNbt().getInt("runic_power") >= 0) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 25 && itemStack.getOrCreateNbt().getInt("runic_power") <= 40) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 40 && itemStack.getOrCreateNbt().getInt("runic_power") <= 60) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 60 && itemStack.getOrCreateNbt().getInt("runic_power") <= 80) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 80 && itemStack.getOrCreateNbt().getInt("runic_power") <= 100) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip3"));

        }

    }
}
