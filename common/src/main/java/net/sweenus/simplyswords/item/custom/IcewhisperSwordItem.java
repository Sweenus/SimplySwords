package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
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
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class IcewhisperSwordItem extends UniqueSwordItem {
    public IcewhisperSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;
    int radius = (int) (SimplySwordsConfig.getFloatValue("permafrost_radius"));
    int frostDamage = (int) (SimplySwordsConfig.getFloatValue("permafrost_damage"));
    int blizzard_timer_max = (int) (SimplySwordsConfig.getFloatValue("permafrost_duration"));
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("permafrost_cooldown"));
    int blizzard_timer;
    double lastX;
    double lastY;
    double lastZ;



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

        lastX = user.getX();
        lastY = user.getY();
        lastZ = user.getZ();

        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {

        if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && (user instanceof PlayerEntity player)) {

            AbilityMethods.tickAbilityPermafrost(stack, world, user, remainingUseTicks, blizzard_timer_max, frostDamage,
                    skillCooldown, radius, lastX, lastY, lastZ);

        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return blizzard_timer_max;
    }
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && (user instanceof PlayerEntity player)) {
            player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

            if (!world.isClient && (entity instanceof PlayerEntity player)) {

                //AOE Aura
                if (player.age % 35 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                    Box box = new Box(player.getX() + radius, player.getY() + radius, player.getZ() + radius, player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                    for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {
                                if (le.hasStatusEffect(StatusEffects.SLOWNESS)) {

                                    int a = (le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);

                                    if (a < 4) {
                                        le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, a), player);
                                    } else {
                                        le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, a - 1), player);
                                    }
                                } else {
                                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 0), player);
                                }
                                float choose = (float) (Math.random() * 1);
                                world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.1f, choose);
                                le.damage(player.getDamageSources().freeze(), frostDamage);
                            }
                        }
                    }

                    world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_SWORD_ICE_ATTACK_02.get(), SoundCategory.PLAYERS, 0.1f, 0.6f);
                    double xpos = player.getX() - (radius + 1);
                    double ypos = player.getY();
                    double zpos = player.getZ() - (radius + 1);

                    for (int i = radius * 2; i > 0; i--) {
                        for (int j = radius * 2; j > 0; j--) {
                            float choose = (float) (Math.random() * 1);
                            HelperMethods.spawnParticle(world, ParticleTypes.SNOWFLAKE, xpos + i + choose,
                                    ypos + 0.4,
                                    zpos + j + choose,
                                    0, 0.1, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.CLOUD, xpos + i + choose,
                                    ypos + 0.1,
                                    zpos + j + choose,
                                    0, 0, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.WHITE_ASH, xpos + i + choose,
                                    ypos + 2,
                                    zpos + j + choose,
                                    0, 0, 0);
                        }
                    }
                }
            }

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, ParticleTypes.WHITE_ASH, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }



    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip3", radius).setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip6", radius * 2).setStyle(TEXT));
        tooltip.add(Text.literal(""));

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);

    }

}
