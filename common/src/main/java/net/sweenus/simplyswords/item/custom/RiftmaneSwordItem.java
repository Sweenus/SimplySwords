package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
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
import net.sweenus.simplyswords.world.RiftmaneAbilityManager;

import java.util.List;

public final class RiftmaneSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public RiftmaneSwordItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return RiftmaneAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return RiftmaneAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.riftmane.cooldown);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.riftmanesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.riftmanesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.riftmanesworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.riftmanesworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.riftmane.cooldown);
        appendAbilityManaCostTooltip(tooltip, stack);
        super.appendTooltip(stack, context, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, SpellScalingProfile.ARCANE);
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.riftmane");
    }

    public static final class EffectSettings extends TooltipSettings {
        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.RIFTMANE::get));
        }

        @ValidatedInt.Restrict(min = 1) public int cooldown = 200;
        @ValidatedInt.Restrict(min = 1, max = 12) public int chargerCount = 5;
        @ValidatedDouble.Restrict(min = 0.5, max = 24.0) public double rankWidth = 7.0;
        @ValidatedDouble.Restrict(min = 1.0, max = 64.0) public double chargeDistance = 20.0;
        @ValidatedDouble.Restrict(min = 0.05, max = 3.0) public double chargeSpeed = 0.9;
        @ValidatedInt.Restrict(min = 0, max = 60) public int rearDuration = 16;
        @ValidatedDouble.Restrict(min = 0.0, max = 8.0) public double spawnOffset = 3.0;
        @ValidatedDouble.Restrict(min = 1.0, max = 5.0) public double mountedDistanceMultiplier = 2.0;
        public boolean waterWalking = true;
        @ValidatedDouble.Restrict(min = 0.0, max = 2.0) public double stepHeight = 1.0;
        @ValidatedDouble.Restrict(min = 0.0) public double damageScaling = 1.0;
        @ValidatedDouble.Restrict(min = 0.0) public double spellScaling = 6.23;
        @ValidatedDouble.Restrict(min = 0.0, max = 5.0) public double knockbackStrength = 1.2;
        @ValidatedDouble.Restrict(min = 0.25, max = 4.0) public double hitRadius = 1.1;
        @ValidatedInt.Restrict(min = 0, max = 100) public int passiveChance = 70;
        @ValidatedInt.Restrict(min = 1) public int passiveLockout = 60;
        @ValidatedDouble.Restrict(min = 0.0) public double passiveMinRange = 4.0;
        @ValidatedDouble.Restrict(min = 1.0) public double passiveMaxRange = 16.0;
        @ValidatedDouble.Restrict(min = 1.0, max = 180.0) public double passiveConeDegrees = 110.0;
    }
}
