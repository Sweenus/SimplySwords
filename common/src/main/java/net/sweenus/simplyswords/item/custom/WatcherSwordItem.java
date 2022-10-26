package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.List;

public class WatcherSwordItem extends SwordItem {
    public WatcherSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;

            int thitchance = (int) SimplySwordsConfig.getFloatValue("watcher_chance");
            int phitchance = (int) SimplySwordsConfig.getFloatValue("omen_chance");

            boolean impactsounds_enabled = (SimplySwordsConfig.getBooleanValue("enable_weapon_impact_sounds"));

            if (impactsounds_enabled) {
                int choose_sound = (int) (Math.random() * 30);
                float choose_pitch = (float) Math.random() * 2;
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 20 && choose_sound > 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 30 && choose_sound > 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 40 && choose_sound > 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
            }

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
    }

}
