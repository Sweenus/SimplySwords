package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class WatcherSwordItem extends UniqueSwordItem {
    public WatcherSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            int thitchance = (int) SimplySwords.uniqueEffectsConfig.watcherChance;
            int phitchance = (int) SimplySwords.uniqueEffectsConfig.omenChance;

            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= thitchance) {
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.WATCHER.get(), 1, 1), attacker);
            }

            if (attacker.getRandom().nextInt(100) <= phitchance) {
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.OMEN.get(), 1, 1), attacker);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.ENCHANT, ParticleTypes.ENCHANT,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip3",
                (SimplySwords.uniqueEffectsConfig.omenInstantKillThreshold * 100)).setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip5").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip7").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
