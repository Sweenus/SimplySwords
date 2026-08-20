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
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.SoulstalkerAbilityManager;

import java.util.List;

public final class SoulstalkerSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public SoulstalkerSwordItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return SoulstalkerAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return SoulstalkerAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.soulstalker.cooldown);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && entity instanceof LivingEntity livingEntity) {
            SoulstalkerAbilityManager.tickHeldPassive(livingEntity, stack);
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulstalkersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.soulstalkersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.soulstalkersworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulstalkersworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.gloam.tooltip").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.soulstalker.cooldown);
        appendAbilityManaCostTooltip(tooltip, stack);
        super.appendTooltip(stack, context, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, SpellScalingProfile.SOUL);
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.soulstalker");
    }

    public static final class EffectSettings extends TooltipSettings {
        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULSTALKER::get));
        }

        @ValidatedInt.Restrict(min = 1) public int cooldown = 1200;
        @ValidatedInt.Restrict(min = 20) public int duration = 800;
        @ValidatedFloat.Restrict(min = 0.0F) public float strikeDamageScaling = 1.0F;
        @ValidatedFloat.Restrict(min = 0.0F) public float strikeSpellScaling = 4.59F;
        @ValidatedDouble.Restrict(min = 0.05, max = 1.0) public double movementSpeed = 0.215;
        @ValidatedDouble.Restrict(min = 0.05, max = 1.0) public double climbSpeed = 0.30;
        @ValidatedDouble.Restrict(min = 0.5, max = 4.0) public double stepHeight = 2.0;
        @ValidatedDouble.Restrict(min = 0.5, max = 4.0) public double riderHeight = 2.2;
        @ValidatedDouble.Restrict(min = 0.0, max = 3.0) public double leapVerticalStrength = 1.0;
        @ValidatedDouble.Restrict(min = 0.1, max = 3.0) public double leapHorizontalStrength = 2.0;
        @ValidatedDouble.Restrict(min = 0.0, max = 1.0) public double leapSurfaceReleaseStrength = 0.6;
        @ValidatedDouble.Restrict(min = 0.0, max = 3.0) public double leapImpactDamageScaling = 0.75;
        @ValidatedDouble.Restrict(min = 0.25, max = 8.0) public double leapImpactRadius = 2.5;
        @ValidatedDouble.Restrict(min = 0.0, max = 3.0) public double leapImpactKnockback = 0.7;
        @ValidatedDouble.Restrict(min = 0.0, max = 1.0) public double leapImpactLift = 0.18;
        @ValidatedDouble.Restrict(min = 0.25, max = 4.0) public double leapImpactStainRadius = 0.9;
        @ValidatedDouble.Restrict(min = 0.0, max = 1.0) public double footfallDamageScaling = 0.15;
        @ValidatedDouble.Restrict(min = 0.1, max = 3.0) public double footfallRadius = 0.8;
        @ValidatedInt.Restrict(min = 1) public int footfallTargetImmunity = 10;
        @ValidatedDouble.Restrict(min = 1.0) public double cleaveRange = 16.0;
        @ValidatedDouble.Restrict(min = 0.05) public double cleaveSpeed = 0.8;
        @ValidatedDouble.Restrict(min = 0.25) public double cleaveInitialWidth = 0.5;
        @ValidatedDouble.Restrict(min = 0.25) public double cleaveFinalWidth = 3.2;
        @ValidatedInt.Restrict(min = 1) public int cleaveMinimumSwingCooldownTicks = 2;
        @ValidatedInt.Restrict(min = 0, max = 100) public int passiveChance = 25;
        @ValidatedInt.Restrict(min = 1) public int passiveCheckInterval = 20;
        @ValidatedInt.Restrict(min = 1) public int passiveLockout = 60;
        @ValidatedDouble.Restrict(min = 1.0) public double passiveRange = 8.0;
        @ValidatedDouble.Restrict(min = 0.25) public double stainTrailWidth = 1.25;
        @ValidatedDouble.Restrict(min = 0.25) public double stainRadius = 1.25;
        @ValidatedInt.Restrict(min = 20) public int stainDuration = 240;
        @ValidatedInt.Restrict(min = 1) public int stainFadeDuration = 40;
        @ValidatedInt.Restrict(min = 0, max = 4) public int stainSlowAmplifier = 0;
    }
}
