package net.sweenus.simplyswords.item.custom;


import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

import java.util.List;

public class StealSwordItem extends SwordItem {
    public StealSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld sworld = (ServerWorld) attacker.world;
            int fhitchance = SimplySwordsConfig.getIntValue("steal_chance");
            int fduration = SimplySwordsConfig.getIntValue("steal_duration");
            attacker.setVelocity(attacker.getRotationVector().multiply(+1));
            attacker.velocityModified = true;


            if (attacker.getRandom().nextInt(100) <= fhitchance) {

                sworld.playSoundFromEntity (null, target, SimplySwords.EVENT_OMEN_ONE , SoundCategory.BLOCKS, 0.4f, 3f);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, 2), attacker);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, fduration, 1), attacker);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, fduration, 1), attacker);

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
            Box box = new Box(x + 30, y + 5, z + 30, x - 30, y - 5, z - 30);
            for(Entity ee: sworld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                if (ee != null) {
                    if (ee instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) ee;

                        if (le.hasStatusEffect(StatusEffects.SLOWNESS) && le.hasStatusEffect(StatusEffects.GLOWING) && le.distanceTo(user) > 5){ //can we check target here?
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 1), user);
                            user.teleport(le.getX(), le.getY(), le.getZ());
                            sworld.playSoundFromEntity (null, le, SimplySwords.EVENT_OMEN_TWO , SoundCategory.BLOCKS, 0.3f, 1.5f);
                            le.damage(DamageSource.FREEZE, 5f);
                            le.removeStatusEffect(StatusEffects.SLOWNESS);
                            le.removeStatusEffect(StatusEffects.GLOWING);
                        }
                        if (le.hasStatusEffect(StatusEffects.SLOWNESS) && le.hasStatusEffect(StatusEffects.GLOWING) && le.distanceTo(user) <= 5){
                            user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 120, 1), user);
                            user.setVelocity(user.getRotationVector().multiply(+2));
                            user.velocityModified = true;
                            sworld.playSoundFromEntity (null, ee, SimplySwords.EVENT_OMEN_TWO , SoundCategory.BLOCKS, 0.3f, 1.5f);
                            le.removeStatusEffect(StatusEffects.SLOWNESS);
                            le.removeStatusEffect(StatusEffects.GLOWING);
                        }
                    }
                }
            }
        }

        return super.use(world,user,hand);
    }

    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        return false;
    }


    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip3"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip4"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip6"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip7"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip8"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip9"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip10"));


/*
        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip3"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip4"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip5"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip6"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip7"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip8"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip9"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip10"));

 */
    }

}
