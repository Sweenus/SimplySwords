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
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class ThunderbrandSwordItem extends UniqueSwordItem {
    public ThunderbrandSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;
    int radius = (int) SimplySwords.uniqueEffectsConfig.thunderBlitzRadius;
    float abilityDamage = SimplySwords.uniqueEffectsConfig.thunderBlitzDamage;
    int ability_timer_max = 50;
    int skillCooldown = (int) SimplySwords.uniqueEffectsConfig.thunderBlitzCooldown;
    int chargeChance =  (int) SimplySwords.uniqueEffectsConfig.thunderBlitzChance;



    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        HelperMethods.playHitSounds(attacker, target);

        if (!attacker.getWorld().isClient()) {
            if (attacker.getRandom().nextInt(100) <= chargeChance && (attacker instanceof PlayerEntity player) && player.getItemCooldownManager().getCooldownProgress(this, 1f) > 0) {
                player.getItemCooldownManager().set(this, 0);
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_BLOCK_01.get(), SoundCategory.PLAYERS, 0.7f, 1f);
            }
        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }

        world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_BOW_CHARGE_LONG_VERSION.get(), SoundCategory.PLAYERS, 0.4f, 0.6f);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3), user);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 3), user);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient) {
            if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && user.isOnGround()) {
                AbilityMethods.tickAbilityThunderBlitz(stack, world, user, remainingUseTicks, ability_timer_max, abilityDamage, skillCooldown, radius);
            }
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient) {
            //Player dash end
            if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                user.setVelocity(0, 0, 0); // Stop player at end of charge
                user.velocityModified = true;
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 2), user);

            }
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return ability_timer_max;
    }
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (HelperMethods.commonSpellAttributeScaling(
                1.7f,
                entity,
                "lightning") > 0)
            abilityDamage = HelperMethods.commonSpellAttributeScaling(
                    1.7f,
                    entity,
                    "lightning");

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
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip7").setStyle(TEXT));
        tooltip.add(Text.literal(""));

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);

    }

}
