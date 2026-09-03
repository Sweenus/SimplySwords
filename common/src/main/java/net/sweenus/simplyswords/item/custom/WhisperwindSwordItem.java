package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

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
import net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
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
import net.sweenus.simplyswords.world.WhisperwindRhythmManager;
import net.sweenus.simplyswords.world.WhisperwindVisualManager;

import java.util.List;

public class WhisperwindSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {

    public WhisperwindSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        HelperMethods.playHitSounds(attacker, target);
        if (attacker.getWorld() instanceof ServerWorld world) {
            UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.WHISPERWIND_RESET,
                    UniqueAbilityContext.passive(world, stack, attacker, target, null), builder -> builder
                            .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(execution);
            StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
            boolean stillWind = tuning.integer(StormSoulMasteryTuning.Setting.STILL_WIND_THRESHOLD, 0) > 0;
            if (stillWind) {
                WhisperwindRhythmManager.primeStillWind(world, attacker, tuning);
            }
            int chance = WhisperwindRhythmManager.resolveChance(world, attacker, tuning,
                    tuning.integer(StormSoulMasteryTuning.Setting.CHANCE, Config.uniqueEffects.whisperwind.chance));
            boolean reset = !stillWind && (tuning.has(StormSoulMasteryTuning.Setting.CHANCE)
                    ? chance >= 100 || chance > 0 && attacker.getRandom().nextInt(100) < chance
                    : attacker.getRandom().nextInt(100) <= chance);
            WhisperwindRhythmManager.recordRefreshResult(world, attacker, reset);
            if (reset && attacker instanceof PlayerEntity player) {
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                        attacker.getSoundCategory(), 0.3f, 1.8f);
                if (!WhisperwindRhythmManager.tryPartialRefresh(world, player, stack, tuning,
                        Config.uniqueEffects.whisperwind.cooldown)) {
                    SimplySwordsAPI.setWeaponCooldown(player, stack, 0);
                }
                WhisperwindRhythmManager.recordRefresh(world, player);
                UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT, target, 1, chance);
            }
            UniqueAbilityApi.finish(execution, StormSoulMasteryAbilities.FINISH, 1);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_01.get(),
                user.getSoundCategory(), 0.6f, 1.0f);
        StormSoulMasteryTuning playerTuning = StormSoulMasteryTuning.EMPTY;
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.WHISPERWIND_DASH,
                    UniqueAbilityContext.passive(serverWorld, stack, user, null, hand), builder -> builder
                            .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY)
                            .set(StormSoulMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.whisperwind.cooldown));
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(execution);
            StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
            playerTuning = tuning;
            WhisperwindVisualManager.startDash(serverWorld, user, stack, execution, tuning);
            WhisperwindRhythmManager.startWindbreak(serverWorld, user, tuning);
            SimplySwordsAPI.setWeaponCooldown(user, stack, execution.cooldownTicks(Config.uniqueEffects.whisperwind.cooldown));
        }
        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FATAL_FLICKER),
                playerTuning.integer(StormSoulMasteryTuning.Setting.DASH_TICKS, 12)));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,
                playerTuning.integer(StormSoulMasteryTuning.Setting.DASH_ABSORPTION_TICKS, 100),
                playerTuning.integer(StormSoulMasteryTuning.Setting.DASH_ABSORPTION, 2) >= 4 ? 1 : 0));

        return super.use(world, user, hand);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.WHISPERWIND_DASH,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY)
                        .set(StormSoulMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.whisperwind.cooldown));
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
        LivingEntityAbilityMovementManager.dashTowardTarget(context.world(), context.actor(), context.target(),
                tuning.get(StormSoulMasteryTuning.Setting.DASH_SPEED, Config.uniqueEffects.whisperwind.dashVelocity), 8);
        WhisperwindVisualManager.scheduleTargetStrike(context.world(), context.actor(), context.target(), context.stack(),
                execution, tuning);
        context.actor().addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FATAL_FLICKER),
                tuning.integer(StormSoulMasteryTuning.Setting.DASH_TICKS, 12)));
        context.actor().addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,
                tuning.integer(StormSoulMasteryTuning.Setting.DASH_ABSORPTION_TICKS, 100),
                tuning.integer(StormSoulMasteryTuning.Setting.DASH_ABSORPTION, 2) >= 4 ? 1 : 0));
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
        if (world instanceof ServerWorld serverWorld && entity instanceof LivingEntity holder && selected
                && world.getTime() % 20L == 0L && net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            UniqueAbilityExecution held = UniqueAbilityApi.begin(StormSoulMasteryAbilities.WHISPERWIND_RESET,
                    UniqueAbilityContext.passive(serverWorld, stack, holder, null, null), builder -> builder
                            .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(held);
            WhisperwindRhythmManager.tickHolder(serverWorld, holder, stack,
                    StormSoulMasteryAbilities.tuning(held));
            UniqueAbilityApi.finish(held, StormSoulMasteryAbilities.FINISH, 0);
        }
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
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.whisperwind.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "evocation");
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
        public float delayedDamageScaling = 0.54f;
        @ValidatedFloat.Restrict(min = 0f)
        public float delayedSpellScaling = 2.48f;
        @ValidatedFloat.Restrict(min = 0f)
        public float delayedDamagePerTargetScaling = 0.09f;
        @ValidatedFloat.Restrict(min = 0f)
        public float delayedSpellPerTargetScaling = 0.18f;
        @ValidatedInt.Restrict(min = 0)
        public int delayedDamageDelay = 20;

    }
}
