package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class StormsEdgeSwordItem extends UniqueSwordItem {

    private static int stepMod = 0;
    int radius = 1;
    int ability_timer_max = 13;
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("stormjolt_cooldown"));
    int chargeChance =  (int) (SimplySwordsConfig.getFloatValue("stormjolt_chance"));

    public StormsEdgeSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }


    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.world.isClient()) {
            if (attacker.getRandom().nextInt(100) <= chargeChance && (attacker instanceof PlayerEntity player) && player.getItemCooldownManager().getCooldownProgress(this, 1f) > 0) {
                player.getItemCooldownManager().set(this, 0);
                attacker.world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_BLOCK_01.get(), SoundCategory.PLAYERS, 0.7f, 1f);
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
        world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(), SoundCategory.PLAYERS, 0.4f, 1.2f);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 5), user);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }
    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient) {
            if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                AbilityMethods.tickAbilityStormJolt(stack, world, user, remainingUseTicks, skillCooldown, radius);
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
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1), user);

            }
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
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip2"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip3"));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip5"));

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
