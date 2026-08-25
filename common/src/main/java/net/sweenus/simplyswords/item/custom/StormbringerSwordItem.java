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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.ParryComponent;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.WeaponManaCost;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.ChainLightningVisualManager;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityChannelManager;
import net.sweenus.simplyswords.world.Phase6CombatManager;
import net.sweenus.simplyswords.world.StormbringerParryManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StormbringerSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    private static final ThreadLocal<Boolean> SUPPRESS_STORMBRINGER_CHAIN = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Long> NEXT_CHAIN_TICK = new HashMap<>();
    private static final Map<UUID, Long> NEXT_FREE_CHAIN_TICK = new HashMap<>();

    public StormbringerSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient() && attacker instanceof ServerPlayerEntity player && !SUPPRESS_STORMBRINGER_CHAIN.get()) {
            tryTriggerChainLightning(stack, target, player);
        }
        if (!SUPPRESS_STORMBRINGER_CHAIN.get()) {
            HelperMethods.playHitSounds(attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    public static void tryTriggerChainLightningOnMeleeDamage(ItemStack stack, LivingEntity target, ServerPlayerEntity player) {
        if (!SUPPRESS_STORMBRINGER_CHAIN.get() && stack.isOf(ItemsRegistry.STORMBRINGER.get())) {
            tryTriggerChainLightning(stack, target, player);
        }
    }

    private static void tryTriggerChainLightning(ItemStack stack, LivingEntity target, ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();
        long nextTriggerTick = NEXT_CHAIN_TICK.getOrDefault(player.getUuid(), Long.MIN_VALUE);
        if (now < nextTriggerTick) {
            return;
        }

        UniqueAbilityExecution execution = Phase6CombatManager.beginPassive(
                Phase6UniqueAbilities.STORMBRINGER_CHAIN, player.getServerWorld(), stack, player, target);
        Phase6AbilityTuning tuning = Phase6UniqueAbilities.tuning(execution);
        ParryComponent component = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT);
        int chargeCap = tuning.integer(s("CHARGE_CAP"), Math.max(0, Config.uniqueEffects.stormbringer.maxStormCharges));
        int stormCharges = Math.clamp(component.stormCharges(), 0, chargeCap);
        if (stormCharges <= 0) {
            UniqueAbilityApi.finish(execution, Phase6UniqueAbilities.FINISH, 0);
            return;
        }

        float damage = HelperMethods.abilityScaledDamage("lightning", player, stack,
                Config.uniqueEffects.stormbringer.chainLightningDamageScaling,
                Config.uniqueEffects.stormbringer.chainLightningSpellScaling)
                * (float) tuning.get(s("DAMAGE_MULTIPLIER"), 1);
        if (tuning.flag(1 << 15) && stormCharges >= chargeCap) damage *= 1.25F;
        int targets = tuning.integer(s("TARGET_CAP"), stormCharges);
        double range = tuning.get(s("RANGE"), Config.uniqueEffects.stormbringer.chainLightningRange);
        SUPPRESS_STORMBRINGER_CHAIN.set(true);
        try {
            int damaged = ChainLightningVisualManager.damageStormbringerChain(player.getServerWorld(), player,
                    target, Math.min(stormCharges, targets), damage, range);
            if (damaged > 0) {
                boolean free = tuning.flag(1 << 10)
                        && now >= NEXT_FREE_CHAIN_TICK.getOrDefault(player.getUuid(), Long.MIN_VALUE);
                int consumed = tuning.flag(1 << 15) && stormCharges >= chargeCap ? 2 : 1;
                ParryComponent updated = component;
                if (free) {
                    NEXT_FREE_CHAIN_TICK.put(player.getUuid(), now + tuning.integer(s("LOCKOUT_TICKS"), 80));
                } else {
                    for (int i = 0; i < consumed; i++) updated = updated.consumeStormCharge();
                }
                stack.set(ComponentTypeRegistry.PARRY.get(), updated);
                int cooldown = tuning.integer(s("COOLDOWN_TICKS"), Config.uniqueEffects.stormbringer.chainLightningCooldown);
                NEXT_CHAIN_TICK.put(player.getUuid(), now + SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                        stack, player, cooldown));
                UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, Phase6UniqueAbilities.HIT,
                        target, damaged, damage);
            }
        } finally {
            SUPPRESS_STORMBRINGER_CHAIN.set(false);
            UniqueAbilityApi.finish(execution, Phase6UniqueAbilities.FINISH, 0);
        }
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
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            StormbringerParryManager.activate(serverPlayer, hand);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient && remainingUseTicks <= 1) {
            if (!(user instanceof ServerPlayerEntity serverPlayer) || !PlayerWeaponAbilityChannelManager.finishEarly(serverPlayer, stack)) {
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
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            StormbringerParryManager.finishUse(serverPlayer, stack);
            WeaponManaCost.spend(serverPlayer, stack);
        }
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        ItemStack stack = context.stack();
        UniqueAbilityExecution execution = Phase6CombatManager.beginActive(
                Phase6UniqueAbilities.STORMBRINGER_GUARD, context, Config.uniqueEffects.stormbringer.cooldown);
        Phase6AbilityTuning tuning = Phase6UniqueAbilities.tuning(execution);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                tuning.integer(s("DURATION_TICKS"), Math.max(1, Config.uniqueEffects.stormbringer.blockDuration)), 5), actor);
        ParryComponent parryComponent = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT)
                .gainBlockedStormCharges(tuning.integer(s("COUNT"), Config.uniqueEffects.stormbringer.stormChargesPerBlock),
                        tuning.integer(s("CHARGE_CAP"), Config.uniqueEffects.stormbringer.maxStormCharges));
        stack.set(ComponentTypeRegistry.PARRY.get(), parryComponent);
        context.world().spawnParticles(ParticleTypes.ELECTRIC_SPARK, actor.getX(), actor.getBodyY(0.5), actor.getZ(), 18, 0.35, 0.38, 0.35, 0.06);
        UniqueAbilityApi.finish(execution, Phase6UniqueAbilities.FINISH, 0);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.stormbringer.cooldown;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return Math.max(1, Config.uniqueEffects.stormbringer.blockDuration);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
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
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.stormbringer.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "lightning");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.STORMBRINGER::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 240;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 1.06f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 5.34f;

        @ValidatedInt.Restrict(min = 0)
        public int blockDuration = 50;
        @ValidatedInt.Restrict(min = 0)
        public int parryDuration = 20;
        @ValidatedFloat.Restrict(min = 0f)
        public float radius = 3f;
        @ValidatedInt.Restrict(min = 1)
        public int maxStormCharges = 10;
        @ValidatedInt.Restrict(min = 0)
        public int stormChargesPerParry = 5;
        @ValidatedInt.Restrict(min = 0)
        public int stormChargesPerBlock = 1;
        @ValidatedInt.Restrict(min = 1)
        public int chainLightningCooldown = 20;
        @ValidatedFloat.Restrict(min = 0f)
        public float chainLightningRange = 6f;
        @ValidatedFloat.Restrict(min = 0f)
        public float chainLightningDamageScaling = 0.35f;
        @ValidatedFloat.Restrict(min = 0f)
        public float chainLightningSpellScaling = 1.65f;

    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }
}
