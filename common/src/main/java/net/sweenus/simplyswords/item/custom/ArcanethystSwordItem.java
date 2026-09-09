package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryTuning;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.ArcanethystAssaultManager;
import net.sweenus.simplyswords.world.ArcaneCosmicMasteryCombatManager;

import java.util.List;

public class ArcanethystSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public ArcanethystSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            UniqueAbilityExecution execution = ArcaneCosmicMasteryCombatManager.beginPassive(
                    ArcaneCosmicMasteryAbilities.ARCANETHYST_SPARK, serverWorld, stack, attacker, target,
                    ArcanethystAssaultManager.sparkBase());
            ArcaneCosmicMasteryTuning tuning = ArcaneCosmicMasteryAbilities.tuning(execution);
            ArcanethystAssaultManager.markTarget(serverWorld, attacker, target, tuning);
            int chance = tuning.integer(ArcaneCosmicMasteryTuning.Setting.CHANCE,
                    Config.uniqueEffects.arcanethyst.chance);
            int sparkRoll = attacker.getRandom().nextInt(100);
            boolean sparkProc = sparkRoll < chance;
            UniqueAbilityApi.reportRoll(attacker, ArcaneCosmicMasteryAbilities.ARCANETHYST_SPARK.id(),
                    "CHANCE", chance, sparkRoll, sparkProc);
            if (sparkProc) {
                int duration = tuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 60);
                int amplifier = tuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_AMPLIFIER, 1);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, duration, amplifier), attacker);
                ArcanethystAssaultManager.onPassiveProc(serverWorld, attacker, target, stack, tuning);
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_01.get(),
                        attacker.getSoundCategory(), 0.5f, 1.2f);
            }
            ArcaneCosmicMasteryCombatManager.finish(execution, 1);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        UniqueAbilityExecution impactExecution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.ARCANETHYST_IMPACT, context.world(), context.stack(),
                context.actor(), context.target(), ArcanethystAssaultManager.impactBase());
        ArcaneCosmicMasteryTuning impact = ArcaneCosmicMasteryAbilities.tuning(impactExecution);
        ArcaneCosmicMasteryCombatManager.finish(impactExecution, 0);
        UniqueAbilityExecution execution = ArcaneCosmicMasteryCombatManager.beginActive(
                ArcaneCosmicMasteryAbilities.ARCANETHYST_SUSPENSION, context,
                impact.integer(ArcaneCosmicMasteryTuning.Setting.COOLDOWN_TICKS, Config.uniqueEffects.arcanethyst.cooldown),
                ArcanethystAssaultManager.suspensionBase());
        UniqueAbilityExecution sparkExecution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.ARCANETHYST_SPARK, context.world(), context.stack(),
                context.actor(), null, ArcanethystAssaultManager.sparkBase());
        ArcaneCosmicMasteryTuning spark = ArcaneCosmicMasteryAbilities.tuning(sparkExecution);
        ArcaneCosmicMasteryCombatManager.finish(sparkExecution, 0);
        activateArcanethyst(context.world(), context.actor(), context.stack(),
                ArcaneCosmicMasteryAbilities.tuning(execution), spark, impact, execution);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.arcanethyst.cooldown;
    }

    private static void activateArcanethyst(ServerWorld serverWorld, LivingEntity actor, ItemStack stack,
                                            ArcaneCosmicMasteryTuning suspension, ArcaneCosmicMasteryTuning spark,
                                            ArcaneCosmicMasteryTuning impact, UniqueAbilityExecution execution) {
        double radius = suspension.get(ArcaneCosmicMasteryTuning.Setting.RADIUS,
                Config.uniqueEffects.arcanethyst.radius);
        float abilityDamage = HelperMethods.abilityScaledDamage("arcane", actor, stack,
                Config.uniqueEffects.arcanethyst.damageScaling
                        * (float) spark.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1),
                Config.uniqueEffects.arcanethyst.spellScaling
                        * (float) spark.get(ArcaneCosmicMasteryTuning.Setting.SPELL_MULTIPLIER, 1));
        ArcanethystAssaultManager.start(serverWorld, actor, stack, radius, abilityDamage, suspension, impact, execution);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.DRAGON_BREATH,
                ParticleTypes.DRAGON_BREATH, ParticleTypes.REVERSE_PORTAL, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip3").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.arcanethyst.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "arcane");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.ARCANETHYST::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 25;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 220;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.06f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 100;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 6;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.76f;
        @ValidatedFloat.Restrict(min = 0f)
        public float liftHeight = 4.0f;
        @ValidatedInt.Restrict(min = 1)
        public int liftTicks = 18;
        @ValidatedInt.Restrict(min = 0)
        public int suspendTicks = 14;
        @ValidatedInt.Restrict(min = 1)
        public int slamTicks = 10;
        @ValidatedFloat.Restrict(min = 0f)
        public float slamDamageMultiplier = 4.0f;
    }
}
