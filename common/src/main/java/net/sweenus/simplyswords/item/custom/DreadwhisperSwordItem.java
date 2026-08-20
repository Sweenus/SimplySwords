package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
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
import net.sweenus.simplyswords.world.DreadwhisperAbilityManager;

import java.util.List;

public final class DreadwhisperSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public DreadwhisperSwordItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return DreadwhisperAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return DreadwhisperAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.dreadwhisper.cooldown);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.dreadwhispersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.dreadwhispersworditem.tooltip2",
                Config.uniqueEffects.dreadwhisper.woundDuration / 20.0F).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.dreadwhispersworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.dreadwhispersworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.dreadwhispersworditem.tooltip5",
                Math.round(Config.uniqueEffects.dreadwhisper.healRatio * 100.0F)).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.gloam.tooltip").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.dreadwhisper.cooldown);
        appendAbilityManaCostTooltip(tooltip, stack);
        super.appendTooltip(stack, context, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, SpellScalingProfile.SOUL);
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.dreadwhisper");
    }

    public static final class EffectSettings extends TooltipSettings {
        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.DREADWHISPER::get));
        }

        @ValidatedInt.Restrict(min = 1) public int cooldown = 250;
        @ValidatedDouble.Restrict(min = 1.0) public double dashDistance = 20.0;
        @ValidatedDouble.Restrict(min = 0.1) public double dashSpeed = 1.6;
        @ValidatedDouble.Restrict(min = 1.0) public double frontWidth = 4.5;
        @ValidatedDouble.Restrict(min = 0.5) public double frontHeight = 2.8;
        @ValidatedFloat.Restrict(min = 0.0F) public float weaponHitScaling = 1.0F;
        @ValidatedFloat.Restrict(min = 0.0F) public float weaponHitSpellScaling = 4.59F;
        @ValidatedFloat.Restrict(min = 0.0F, max = 1.0F) public float healRatio = 0.35F;
        @ValidatedInt.Restrict(min = 20) public int stainDuration = 240;
        @ValidatedInt.Restrict(min = 1) public int stainFadeDuration = 40;
        @ValidatedInt.Restrict(min = 0, max = 4) public int stainSlowAmplifier = 0;
        @ValidatedInt.Restrict(min = 1) public int woundDuration = 200;
        @ValidatedFloat.Restrict(min = 1.0F) public float criticalMultiplier = 1.5F;
    }
}
