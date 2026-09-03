package net.sweenus.simplyswords.item.custom;

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
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.util.WeaponManaCost;
import net.sweenus.simplyswords.world.StormFrostWaterMasteryCombatManager;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityManager;
import net.sweenus.simplyswords.world.TempestAbilityManager;

import java.util.List;

public class TempestSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public TempestSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {

            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            UniqueAbilityExecution execution = StormFrostWaterMasteryCombatManager.beginPassive(
                    StormFrostWaterMasteryAbilities.TEMPEST_MARK, serverWorld, stack, attacker, target);
            StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryAbilities.tuning(execution);
            HelperMethods.playHitSounds(attacker, target);
            float fireDamage = Math.max(1, HelperMethods.abilityScaledDamage("fire", attacker, stack,
                    Config.uniqueEffects.tempest.damageScaling, Config.uniqueEffects.tempest.spellScaling));
            float frostDamage = Math.max(1, HelperMethods.abilityScaledDamage("frost", attacker, stack,
                    Config.uniqueEffects.tempest.damageScaling, Config.uniqueEffects.tempest.spellScaling));
            TempestAbilityManager.MarkApplication application = TempestAbilityManager.applyMark(
                    serverWorld, attacker, target, tuning, fireDamage, frostDamage, 500,
                    Config.uniqueEffects.tempest.maxStacks);
            boolean fire = application.primary() == TempestAbilityManager.Element.FIRE;
            HelperMethods.spawnWaistHeightParticles(serverWorld,
                    fire ? ParticleTypes.SMOKE : ParticleTypes.CLOUD, attacker, target, 10);
            serverWorld.playSound(null, attacker.getBlockPos(),
                    fire ? SoundRegistry.SPELL_FIRE.get() : SoundRegistry.ELEMENTAL_SWORD_WATER_ATTACK_03.get(),
                    attacker.getSoundCategory(), 0.2f, 1.3f);
            UniqueAbilityApi.finish(execution, StormFrostWaterMasteryAbilities.FINISH, 1);

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
        boolean reboundInput = PlayerWeaponAbilityManager.shouldSkipDefaultAbilityUse(world, user, hand, stack);
        if (reboundInput && !WeaponManaCost.canAfford(user, stack)) return TypedActionResult.fail(stack);
        TypedActionResult<ItemStack> result = UniqueWeaponActiveAbility.super.startPlayerAbility(world, user, hand);
        if (reboundInput && result.getResult().isAccepted()) WeaponManaCost.spend(user, stack);
        return result;
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && TempestAbilityManager.hasConsumableMarks(context.world(), context.actor(), 15);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!TempestAbilityManager.hasConsumableMarks(context.world(), context.actor(), 15)) return false;
        UniqueAbilityExecution execution = StormFrostWaterMasteryCombatManager.beginActive(
                StormFrostWaterMasteryAbilities.TEMPEST_VORTEX, context, 200);
        return TempestAbilityManager.startVortex(context, StormFrostWaterMasteryAbilities.tuning(execution), execution,
                Config.uniqueEffects.tempest.duration, Config.uniqueEffects.tempest.maxSize);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return 200;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.DUST_PLUME,
                ParticleTypes.DUST_PLUME, ParticleTypes.DUST_PLUME, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip7").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, 200);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "frost_fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.TEMPEST::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int duration = 1200;
        @ValidatedInt.Restrict(min = 1)
        public int maxSize = 30;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 10;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.33f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.2266f;

    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }
}
