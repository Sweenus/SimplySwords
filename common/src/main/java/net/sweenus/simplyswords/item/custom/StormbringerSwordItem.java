package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class StormbringerSwordItem extends UniqueSwordItem {

    private static int stepMod = 0;
    int radius = 3;
    int ability_timer_max = (int) (SimplySwordsConfig.getFloatValue("shockdeflect_block_duration"));
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("shockdeflect_cooldown"));
    int perfectParryWindow = (int) (SimplySwordsConfig.getFloatValue("shockdeflect_parry_duration"));
    int abilityDamage = (int) (SimplySwordsConfig.getFloatValue("shockdeflect_damage"));
    boolean parrySuccess;
    int parrySuccession;

    public StormbringerSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }


    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        HelperMethods.playHitSounds(attacker, target);
            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_PARRY_02.get(), SoundCategory.PLAYERS, 0.8f, (float) (0.8f * ( parrySuccession * 0.1)));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, ability_timer_max, 2), user);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }
    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient) {

            if (remainingUseTicks <= 2)
                user.stopUsingItem();

            Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                    user.getX() - radius, user.getY() - radius, user.getZ() - radius);
            for (Entity entities : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                //Parry attack
                if (entities != null) {
                    if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, (PlayerEntity) user)) {
                        if (le.handSwinging && remainingUseTicks > getMaxUseTime(stack) - perfectParryWindow) {
                            parrySuccess = true;
                            if (parrySuccession < 20)
                                parrySuccession +=1;
                            user.stopUsingItem();
                            le.handSwinging = false;
                            world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_PARRY_01.get(), SoundCategory.PLAYERS, 1f, (float) (0.8f * ( parrySuccession * 0.1)));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient) {

            if (parrySuccess) {
                //Damage
                Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                        user.getX() - radius, user.getY() - radius, user.getZ() - radius);
                for (Entity entities : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    //damage & knockback
                    if (entities != null) {
                        if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, (PlayerEntity) user)) {

                            float choose = (float) (Math.random() * 1);
                            le.damage(user.getDamageSources().magic(), abilityDamage + parrySuccession);
                            world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(), SoundCategory.PLAYERS, 0.3f, choose);
                            le.setVelocity(le.getX() - user.getX(), 0.1, le.getZ() - user.getZ());

                            //player dodge backwards
                            user.setVelocity(le.getRotationVector().multiply(+1.5));
                            user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent player flying to the heavens
                            user.velocityModified = true;

                        }
                    }
                }
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_01.get(), SoundCategory.PLAYERS, (float) (0.2f * ( parrySuccession * 0.04)), 0.8f);
                ((PlayerEntity) user).getItemCooldownManager().set(stack.getItem(), (skillCooldown / 2) + (parrySuccession * 2));
            }
            if (!parrySuccess) {
                ((PlayerEntity) user).getItemCooldownManager().set(stack.getItem(), skillCooldown);
                parrySuccession = 0;
            }
            parrySuccess = false;

        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
            return ability_timer_max;
    }
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }



    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip7").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip8").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip9").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip10").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip11").setStyle(TEXT));

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
