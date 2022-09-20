package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.List;

public class HasteSwordItem extends SwordItem {
    public HasteSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int fhitchance = SimplySwordsConfig.getIntValue("ferocity_chance");
        int fduration = SimplySwordsConfig.getIntValue("ferocity_duration");


        if (attacker.getRandom().nextInt(100) <= fhitchance) {
            if (attacker.hasStatusEffect(StatusEffects.HASTE)) {

                int a = (attacker.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1);

                if ((attacker.getStatusEffect(StatusEffects.HASTE).getAmplifier() <= 14)) {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, a), attacker);
                } else {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, a - 1), attacker);
                }
            } else {
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, 1), attacker);
            }
        }

        return super.postHit(stack, target, attacker);

    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (user.hasStatusEffect(StatusEffects.HASTE)) {

            int a = (user.getStatusEffect(StatusEffects.HASTE).getAmplifier() * 20);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, a, 1), user);
            user.swingHand(hand);
            user.removeStatusEffect(StatusEffects.HASTE);
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_MEDIUM_AMETHYST_BUD_BREAK, SoundCategory.BLOCKS, 1f, 1.5f);
        }
        return super.use(world,user,hand);
    }


    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip3"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip6"));
    }

}
