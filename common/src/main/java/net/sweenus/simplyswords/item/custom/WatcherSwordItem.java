package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
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
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;

            int thitchance = (int) SimplySwordsConfig.getFloatValue("watcher_chance");
            int phitchance = (int) SimplySwordsConfig.getFloatValue("omen_chance");

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

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.ENCHANT, ParticleTypes.ENCHANT, ParticleTypes.MYCELIUM, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);

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
        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
