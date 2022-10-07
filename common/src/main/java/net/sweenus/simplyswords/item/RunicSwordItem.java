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
        if(stack.getOrCreateNbt().getInt("runic_power") <= 50 && stack.getOrCreateNbt().getInt("runic_power") >= 1) {

            int fhitchance = (int) SimplySwordsConfig.getFloatValue("freeze_chance");
            int fduration = (int) SimplySwordsConfig.getFloatValue("freeze_duration");
            int sduration = (int) SimplySwordsConfig.getFloatValue("slowness_duration");

            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 1), attacker);

            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), fduration, 1), attacker);
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

        if(itemStack.getOrCreateNbt().getInt("runic_power") <= 50 && itemStack.getOrCreateNbt().getInt("runic_power") >= 1) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2"));

        }

    }
}
