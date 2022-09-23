package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

import java.util.List;

public class RendSwordItem extends SwordItem {
    public RendSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int fhitchance = (int) SimplySwordsConfig.getFloatValue("soulrend_chance");
        int fduration = (int) SimplySwordsConfig.getFloatValue("soulrend_duration");


        if (attacker.getRandom().nextInt(100) <= fhitchance) {
            if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {

                int a = (target.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() + 1);

                if ((target.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() <= 0)) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, fduration, a), attacker);
                }
            } else {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, fduration, 1), attacker);
            }

            if (target.hasStatusEffect(StatusEffects.SLOWNESS)) {

                int a = (target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);

                if ((target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() <= 7)) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, fduration, a), attacker);
                }
            } else {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, fduration, 1), attacker);
            }
        }

        return super.postHit(stack, target, attacker);

    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!user.world.isClient()) {
            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld sworld = (ServerWorld) user.world;
            Box box = new Box(x + 10, y + 5, z + 10, x - 10, y - 5, z - 10);

            for(Entity ee: sworld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                if (ee != null) {
                    if (ee instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) ee;

                         if (le.hasStatusEffect(StatusEffects.SLOWNESS) && le.hasStatusEffect(StatusEffects.WEAKNESS)) {
                             user.heal(le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() / 2);
                             le.damage(DamageSource.FREEZE, le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() * 2);
                             le.removeStatusEffect(StatusEffects.WEAKNESS);
                             le.removeStatusEffect(StatusEffects.SLOWNESS);
                             world.playSoundFromEntity (null, ee, SimplySwords.EVENT_OMEN_ONE , SoundCategory.BLOCKS, 0.1f, 2f);
                         }
                    }
                }
            }
        }
        return super.use(world,user,hand);
    }


    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip3"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip6"));

        /*

        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.rendsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.rendsworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.rendsworditem.tooltip3"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(new TranslatableText("item.simplyswords.rendsworditem.tooltip4"));
        tooltip.add(new TranslatableText("item.simplyswords.rendsworditem.tooltip5"));
        tooltip.add(new TranslatableText("item.simplyswords.rendsworditem.tooltip6"));

         */
    }

}
