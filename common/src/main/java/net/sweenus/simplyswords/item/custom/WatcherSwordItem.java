package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.WatcherAbilityManager;
import net.sweenus.simplyswords.world.WatcherWeaponType;

import java.util.List;

public abstract class WatcherSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public WatcherSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    protected abstract WatcherWeaponType getWatcherWeaponType();

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient() && attacker.getWorld() instanceof ServerWorld world) {
            DelegatedWeaponHitContext delegated = SimplySwordsAPI.getDelegatedWeaponHitContext();
            LivingEntity effectiveActor = delegated == null ? attacker : delegated.actor();
            if (delegated == null || delegated.owner() == null
                    || target != delegated.owner() && HelperMethods.checkAbilityTarget(target, delegated.owner())) {
                WatcherAbilityManager.addDread(world, effectiveActor, target, getWatcherWeaponType());
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return WatcherAbilityManager.canActivate(context, getWatcherWeaponType());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return WatcherAbilityManager.activate(context, getWatcherWeaponType());
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return getWatcherWeaponType() == WatcherWeaponType.WARGLAIVE
                ? Config.uniqueEffects.watcher.warglaiveCooldown
                : Config.uniqueEffects.watcher.claymoreCooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT, ParticleTypes.ENCHANT,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext,
                              List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip2",
                Config.uniqueEffects.watcher.maxDread).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        if (getWatcherWeaponType() == WatcherWeaponType.WARGLAIVE) {
            tooltip.add(Text.translatable("item.simplyswords.watchersworditem.warglaive.tooltip2").setStyle(Styles.TEXT));
            appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.watcher.warglaiveCooldown);
            appendAbilityManaCostTooltip(tooltip, itemStack);
        } else {
            tooltip.add(Text.translatable("item.simplyswords.watchersworditem.claymore.tooltip2").setStyle(Styles.TEXT));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.watchersworditem.claymore.tooltip3",
                    Math.round(Config.uniqueEffects.watcher.omenInstantKillThreshold * 100.0F)).setStyle(Styles.TEXT));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.watchersworditem.claymore.tooltip4").setStyle(Styles.TEXT));
            appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.watcher.claymoreCooldown);
            appendAbilityManaCostTooltip(tooltip, itemStack);
        }

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "soul");
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.watcher");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WATCHER_CLAYMORE::get,
                    ItemsRegistry.WATCHING_WARGLAIVE::get));
        }

        @ValidatedInt.Restrict(min = 1)
        public int dreadDuration = 200;
        @ValidatedInt.Restrict(min = 1, max = 10)
        public int maxDread = 5;
        @ValidatedInt.Restrict(min = 1, max = 10)
        public int maxMarkedTargets = 5;
        @ValidatedDouble.Restrict(min = 1.0)
        public double activationRange = 12.0;

        @ValidatedInt.Restrict(min = 1)
        public int warglaiveCooldown = 100;
        @ValidatedDouble.Restrict(min = 1.0)
        public double warglaiveHuntRadius = 10.0;
        @ValidatedInt.Restrict(min = 1, max = 10)
        public int warglaiveMaxTargets = 5;
        @ValidatedDouble.Restrict(min = 0.05)
        public double warglaiveBatSpeed = 1.35;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float warglaiveDamageScaling = 0.22F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float warglaiveSpellScaling = 0.32F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float warglaiveLifeSteal = 0.50F;
        @ValidatedFloat.Restrict(min = 0.0F, max = 1.0F)
        public float warglaiveHealCap = 0.40F;

        @ValidatedInt.Restrict(min = 1)
        public int claymoreCooldown = 180;
        @ValidatedInt.Restrict(min = 20)
        public int claymoreSwoopDuration = 60;
        @ValidatedInt.Restrict(min = 1, max = 100)
        public int claymoreMinimumSwoops = 4;
        @ValidatedInt.Restrict(min = 1, max = 100)
        public int claymoreMaximumSwoops = 12;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float claymoreSwoopDamageScaling = 0.12F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float claymoreSwoopSpellScaling = 0.18F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float claymoreDamageScaling = 0.85F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float claymoreSpellScaling = 1.25F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float claymoreDreadBonusPerStack = 0.15F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float claymoreMissingHealthBonus = 0.75F;
        @ValidatedFloat.Restrict(min = 0.0F, max = 100.0F)
        public float omenAbsorptionCap = 20.0F;
        @ValidatedFloat.Restrict(min = 0.0F, max = 1.0F)
        public float omenInstantKillThreshold = 0.25F;
    }
}
