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
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.GloampiercerAbilityManager;

import java.util.List;

public final class GloampiercerSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public GloampiercerSwordItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return GloampiercerAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return GloampiercerAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.gloampiercer.cooldown);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.gloampiercersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.gloampiercersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.gloampiercersworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.gloam.tooltip").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.gloampiercer.cooldown);
        super.appendTooltip(stack, context, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, SpellScalingProfile.SOUL);
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.gloampiercer");
    }

    public static final class EffectSettings extends TooltipSettings {
        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.GLOAMPIERCER::get));
        }

        @ValidatedInt.Restrict(min = 1) public int cooldown = 450;
        @ValidatedDouble.Restrict(min = 1.0) public double castRange = 18.0;
        @ValidatedDouble.Restrict(min = 1.0) public double barrageRadius = 7.0;
        @ValidatedInt.Restrict(min = 3, max = 36) public int spearCount = 18;
        @ValidatedFloat.Restrict(min = 0.0F) public float strikeDamageScaling = 1.0F;
        @ValidatedFloat.Restrict(min = 0.0F) public float strikeSpellScaling = 5.1F;
        @ValidatedInt.Restrict(min = 1, max = 8) public int cloneCount = 5;
        @ValidatedInt.Restrict(min = 20, max = 120) public int channelDuration = 60;
        @ValidatedDouble.Restrict(min = 0.0, max = 8.0) public double liftHeight = 3.0;
        @ValidatedDouble.Restrict(min = 0.0, max = 1.0) public double movementRetention = 0.1;
        @ValidatedDouble.Restrict(min = 0.1) public double projectileSpeed = 1.55;
        @ValidatedDouble.Restrict(min = 0.0, max = 2.0) public double collisionGrace = 0.45;
        @ValidatedInt.Restrict(min = 20) public int embeddedDuration = 100;
        @ValidatedDouble.Restrict(min = 0.1) public double triggerRadius = 1.0;
        @ValidatedDouble.Restrict(min = 0.25) public double explosionRadius = 2.5;
        @ValidatedDouble.Restrict(min = 0.0) public double passiveMinRange = 3.0;
        @ValidatedDouble.Restrict(min = 1.0) public double passiveMaxRange = 12.0;
        @ValidatedDouble.Restrict(min = 1.0, max = 180.0) public double passiveConeDegrees = 110.0;
        @ValidatedInt.Restrict(min = 1) public int passiveCooldown = 12;
        @ValidatedDouble.Restrict(min = 0.25) public double stainRadius = 1.5;
        @ValidatedInt.Restrict(min = 20) public int stainDuration = 240;
        @ValidatedInt.Restrict(min = 1) public int stainFadeDuration = 40;
        @ValidatedInt.Restrict(min = 0, max = 4) public int stainSlowAmplifier = 0;
    }
}
