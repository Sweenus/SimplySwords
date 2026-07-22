package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;
import net.sweenus.simplyswords.world.WhisperwindVisualManager;

import java.util.List;

public class WhisperwindSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {

    public WhisperwindSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient()) {
            if (attacker.getRandom().nextInt(100) <= Config.uniqueEffects.whisperwind.chance && (attacker instanceof PlayerEntity player)) {
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                        attacker.getSoundCategory(), 0.3f, 1.8f);
                player.getItemCooldownManager().set(this.getDefaultStack().getItem(), 0);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return startPlayerAbility(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_01.get(),
                user.getSoundCategory(), 0.6f, 1.0f);
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            WhisperwindVisualManager.startDash(serverWorld, user);
        }
        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FATAL_FLICKER), 12));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100));
        user.getItemCooldownManager().set(this.getDefaultStack().getItem(), Config.uniqueEffects.whisperwind.cooldown);

        return super.use(world, user, hand);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        LivingEntityAbilityMovementManager.dashTowardTarget(context.world(), context.actor(), context.target(),
                Config.uniqueEffects.whisperwind.dashVelocity, 8);
        WhisperwindVisualManager.scheduleTargetStrike(context.world(), context.actor(), context.target());
        context.actor().addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FATAL_FLICKER), 12));
        context.actor().addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100));
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.whisperwind.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip3").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.whisperwind.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WHISPERWIND::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 175;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 99;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 3;
        @ValidatedFloat.Restrict(min = 0f)
        public float dashVelocity = 3f;
        @ValidatedFloat.Restrict(min = 0f)
        public float delayedDamageScaling = 0.67f;
        @ValidatedFloat.Restrict(min = 0f)
        public float delayedDamagePerTargetScaling = 0.11f;
        @ValidatedInt.Restrict(min = 0)
        public int delayedDamageDelay = 20;

    }
}
