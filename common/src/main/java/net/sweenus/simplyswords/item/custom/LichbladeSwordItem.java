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
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class LichbladeSwordItem extends UniqueSwordItem {
    public LichbladeSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;
    int radius = (int) (SimplySwordsConfig.getFloatValue("soulanguish_radius"));
    int abilityDamage = (int) (SimplySwordsConfig.getFloatValue("soulanguish_damage"));
    int ability_timer_max = (int) (SimplySwordsConfig.getFloatValue("soulanguish_duration"));
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("soulanguish_cooldown"));
    float healAmount = (SimplySwordsConfig.getFloatValue("soulanguish_heal"));
    int range = (int) (SimplySwordsConfig.getFloatValue("soulanguish_range"));
    int damageTracker;
    int chanceReduce;
    double lastX;
    double lastY;
    double lastZ;
    double targetX;
    double targetY;
    double targetZ;

    LivingEntity abilityTarget;


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

        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get()) || this.getDefaultStack().isOf(ItemsRegistry.WAKING_LICHBLADE.get())) {

            abilityTarget = (LivingEntity) HelperMethods.getTargetedEntity(user, range);
            if (abilityTarget != null) {
                abilityTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 10, 0), user);
                world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ENCHANT.get(), SoundCategory.PLAYERS, 0.5f, 0.5f);
                lastX = user.getX();
                lastY = user.getY();
                lastZ = user.getZ();
                chanceReduce = 0;
            }

        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {

            if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && (user instanceof PlayerEntity player)) {

                //Return to player after a duration & buff player
                if (remainingUseTicks < 200) {
                    if (stack.isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
                        if (abilityTarget != player)
                            abilityTarget = player;
                        if (player.squaredDistanceTo(lastX, lastY, lastZ) < radius) {
                            if (!player.hasStatusEffect(StatusEffects.ABSORPTION))
                                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 120, 2), player);
                            damageTracker = 0;
                            remainingUseTicks = 0;
                            player.stopUsingItem();
                            world.playSoundFromEntity(null, player, SoundRegistry.DARK_SWORD_SPELL.get(), SoundCategory.PLAYERS, 0.04f, 0.5f);
                        }
                    } else {
                        remainingUseTicks = 0;
                        player.stopUsingItem();
                        damageTracker = 0;
                    }
                }

                //Move aura to target
                if (abilityTarget != null && player.age % 5 == 0) {
                    targetX = abilityTarget.getX();
                    targetY = abilityTarget.getY();
                    targetZ = abilityTarget.getZ();

                    if (targetX > lastX)
                        lastX += 1;
                    if (targetX < lastX)
                        lastX -= 1;
                    if (targetZ > lastZ)
                        lastZ += 1;
                    if (targetZ < lastZ)
                        lastZ -= 1;
                    if (targetY > lastY)
                        lastY += 1;
                    if (targetY < lastY)
                        lastY -= 1;
                }

                AbilityMethods.tickAbilitySoulAnguish(stack, world, user, remainingUseTicks, ability_timer_max,
                        abilityDamage, skillCooldown, radius, damageTracker, chanceReduce,
                        lastX, lastY, lastZ, targetX, targetY, targetZ, range, healAmount,
                        abilityTarget);

        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return ability_timer_max;
    }
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && (user instanceof PlayerEntity player) && abilityTarget != null) {
            player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        if (!entity.getWorld().isClient() && (entity instanceof PlayerEntity player)) {
            //AOE Aura
            if (player.age % 35 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack && !player.isUsingItem()) {
                Box box = new Box(player.getX() + radius, player.getY() + radius, player.getZ() + radius, player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    if (entities != null) {
                        if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {
                            le.damage(player.getDamageSources().magic(), abilityDamage);
                        }
                    }
                }

                world.playSoundFromEntity(null, player, SoundRegistry.DARK_SWORD_BLOCK.get(), SoundCategory.PLAYERS, 0.1f, 0.2f);
                double xpos = player.getX() - (radius + 1);
                double ypos = player.getY();
                double zpos = player.getZ() - (radius + 1);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.SCULK_SOUL, xpos + i + choose,
                                ypos,
                                zpos + j + choose,
                                0, 0.1, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.SOUL, xpos + i + choose,
                                ypos + 0.1,
                                zpos + j + choose,
                                0, 0, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM, xpos + i + choose,
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
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.SOUL, ParticleTypes.SOUL, ParticleTypes.MYCELIUM, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        if (this.getDefaultStack().isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1").setStyle(ABILITY));
        if (this.getDefaultStack().isOf(ItemsRegistry.WAKING_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.2").setStyle(ABILITY));
        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.3").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip3", radius).setStyle(TEXT));
        tooltip.add(Text.literal(""));
        if (this.getDefaultStack().isOf(ItemsRegistry.WAKING_LICHBLADE.get()) || this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
            tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip4").setStyle(TEXT));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip5").setStyle(TEXT));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip6").setStyle(TEXT));
            tooltip.add(Text.literal(""));
        }
        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip7").setStyle(TEXT));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip8").setStyle(TEXT));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip9").setStyle(TEXT));
            tooltip.add(Text.literal(""));
        }
        super.appendTooltip(itemStack,world, tooltip, tooltipContext);

    }

}
