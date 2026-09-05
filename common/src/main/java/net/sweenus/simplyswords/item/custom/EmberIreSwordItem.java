package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.AwakeningApi;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ChanceDurationSettings;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.EmberbladeAbilityManager;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityChannelManager;

import java.util.List;

public class EmberIreSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    private static final double TARGET_RANGE = 18.0;

    public EmberIreSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            if (attacker.getWorld() instanceof ServerWorld serverWorld) {
                EmberbladeAbilityManager.onMeleeHit(serverWorld, stack, attacker, target);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        if (!AwakeningApi.isAbilityUnlocked(itemStack)) {
            return TypedActionResult.fail(itemStack);
        }
        if (user.getItemCooldownManager().isCoolingDown(itemStack.getItem())) {
            return TypedActionResult.fail(itemStack);
        }
        if (!world.isClient && (!(world instanceof ServerWorld serverWorld)
                || !(user instanceof ServerPlayerEntity serverPlayer)
                || !EmberbladeAbilityManager.startChannel(serverWorld, serverPlayer, itemStack, hand))) {
            return TypedActionResult.fail(itemStack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient && remainingUseTicks % 10 == 0 && remainingUseTicks < getMaxUseTime(stack, user) - 5) {
            world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_RECHARGE.get(),
                    user.getSoundCategory(), 0.2f, 1.1f - (remainingUseTicks * 0.001f));
        }
        if (!world.isClient && world instanceof ServerWorld serverWorld
                && EmberbladeAbilityManager.isFullyCharged(serverWorld, user)) {
            if (!(user instanceof ServerPlayerEntity serverPlayer)
                    || !PlayerWeaponAbilityChannelManager.finishEarly(serverPlayer, stack)) {
                user.stopUsingItem();
            }
        }
    }

    @Override
    public boolean chargesManaOnRelease() {
        return true;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            LivingEntity targetEntity = user instanceof PlayerEntity player ? findPlayerTarget(player) : null;
            EmberbladeAbilityManager.releaseChannel(serverWorld, stack, user, targetEntity);
        }
    }

    public static LivingEntity findPlayerTarget(PlayerEntity player) {
        return StealSwordItem.findLenientTarget(player, TARGET_RANGE);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return EmberbladeAbilityManager.releaseDelegated(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.emberblade.cooldown);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return user.getWorld() instanceof ServerWorld serverWorld
                ? EmberbladeAbilityManager.channelTicks(serverWorld, user) : 100;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        boolean empowered = entity instanceof PlayerEntity player
                && player.hasStatusEffect(StatusEffects.STRENGTH)
                && player.hasStatusEffect(StatusEffects.HASTE)
                && player.hasStatusEffect(StatusEffects.SPEED);
        HelperMethods.createFootfalls(entity, stack, world,
                empowered ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.FALLING_LAVA,
                empowered ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.FALLING_LAVA,
                empowered ? ParticleTypes.LAVA : ParticleTypes.SMOKE, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip9").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, 10);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "fire");
    }

    @Translation(prefix = "", negate = true)
    public static class EffectSettings extends ChanceDurationSettings {

        public EffectSettings() {
            super(30, 150, new ItemStackTooltipAppender(ItemsRegistry.EMBERBLADE::get));
        }

        public int cooldown = 60;
        public float initialDamageScaling = 0.24f;
        public float initialSpellScaling = 0.9593f;
        public float maxChargeDamageScaling = 2.4f;
        public float maxChargeSpellScaling = 9.5935f;
    }
}
