package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
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
import net.sweenus.simplyswords.util.WeaponManaCost;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.Phase5CombatManager;

import java.util.List;

public class EmberIreSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    private static final double TARGET_RANGE = 18.0;

    public EmberIreSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    private static SimpleParticleType particleWalk = ParticleTypes.FALLING_LAVA;
    private static SimpleParticleType particleSprint = ParticleTypes.FALLING_LAVA;
    private static SimpleParticleType particlePassive = ParticleTypes.SMOKE;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);

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
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient && remainingUseTicks %10 == 0 && remainingUseTicks < getMaxUseTime(stack, user) - 5) {
            world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_RECHARGE.get(),
                    user.getSoundCategory(), 0.2f, 1.1f - (remainingUseTicks * 0.001f));

            if (remainingUseTicks < 20) {
                onStoppedUsing(stack, world, user, remainingUseTicks);
            }

        }
    }

    @Override
    public boolean chargesManaOnRelease() {
        return true;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            WeaponManaCost.spend(user, stack);
            LivingEntity targetEntity = user instanceof PlayerEntity player ? findPlayerTarget(player) : null;
            float chargeRatio = 1.0f - ((float) remainingUseTicks / getMaxUseTime(stack, user));
            Phase5CombatManager.releaseEmberbladeCharge((ServerWorld) world, stack, user, targetEntity, chargeRatio);
        }
    }

    public static LivingEntity findPlayerTarget(PlayerEntity player) {
        return StealSwordItem.findLenientTarget(player, TARGET_RANGE);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return Phase5CombatManager.releaseEmberblade(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.emberblade.cooldown);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 80;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if ((entity instanceof PlayerEntity player)) {
            if (!player.hasStatusEffect(StatusEffects.STRENGTH) && !player.isOnFire()) {
                particlePassive = ParticleTypes.SMOKE;
                particleWalk = ParticleTypes.FALLING_LAVA;
                particleSprint = ParticleTypes.FALLING_LAVA;
            }
        }
        int stepMod = 7 - (int)(world.getTime() % 7);
        HelperMethods.createFootfalls(entity, stack, world, particleWalk, particleSprint, particlePassive, true);
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
