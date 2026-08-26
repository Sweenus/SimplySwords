package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase8UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import net.sweenus.simplyswords.world.Phase8CombatManager;

import java.util.List;

public class ShadowstingSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public ShadowstingSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient()
                && attacker instanceof ServerPlayerEntity serverPlayer
                && attacker.getWorld() instanceof ServerWorld serverWorld
                && target.isAlive()) {
            UniqueAbilityExecution execution = Phase8CombatManager.beginPassive(
                    Phase8UniqueAbilities.SHADOW_ECHO, serverWorld, stack, attacker, target);
            Phase8AbilityTuning tuning = Phase8UniqueAbilities.tuning(execution);
            ShadowstingShadowDanceManager.applyUmbralMarkBonus(serverWorld, serverPlayer, target, tuning);
            if (ShadowstingShadowDanceManager.canPassiveProc(serverWorld, serverPlayer, tuning)
                    && attacker.getRandom().nextInt(100) < tuning.integer(
                    Phase8AbilityTuning.Setting.CHANCE, Config.uniqueEffects.shadowsting.chance)) {
                ShadowstingShadowDanceManager.schedulePassiveCloneStrike(serverWorld, serverPlayer, target, tuning);
            }
            UniqueAbilityApi.finish(execution, Phase8UniqueAbilities.FINISH, 1);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        return UniqueWeaponActiveAbility.super.startPlayerAbility(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        if (context == null || context.actor() == null || !context.actor().isAlive()
                || context.stack() == null || context.stack().isEmpty()
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1) return false;
        if (context.actor() instanceof ServerPlayerEntity player) {
            return !ShadowstingShadowDanceManager.isActive(player);
        }
        return context.target() != null && context.target().isAlive()
                && HelperMethods.checkAbilityTarget(context.target(), context.actor());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = Phase8CombatManager.beginActive(
                Phase8UniqueAbilities.SHADOW_DANCE, context, Config.uniqueEffects.shadowsting.cooldown);
        Phase8AbilityTuning tuning = Phase8UniqueAbilities.tuning(execution);
        boolean started;
        if (context.actor() instanceof ServerPlayerEntity player) {
            started = ShadowstingShadowDanceManager.start(context.world(), player, context.stack(), tuning);
        } else {
            started = context.target() != null && HelperMethods.checkAbilityTarget(context.target(), context.actor())
                    && ShadowstingShadowDanceManager.start(context.world(), context.actor(), context.target(), context.stack(), tuning);
        }
        if (started) Phase8CombatManager.scheduleFinish(context.world(), execution,
                tuning.integer(Phase8AbilityTuning.Setting.DURATION_TICKS, 50) + 10, 1);
        return started;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.shadowsting.cooldown;
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
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.shadowsting.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "soul");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SHADOWSTING::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 40;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 200;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 100;
        @ValidatedInt.Restrict(min = 1)
        public int strikeInterval = 8;
        @ValidatedInt.Restrict(min = 1)
        public int strikeRadius = 10;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 1.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 3.0202f;

    }
}
