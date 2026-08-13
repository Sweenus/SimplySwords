package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.IonboundStormscaleAbilityManager;

import java.util.List;

public final class IonboundStormscaleSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public IonboundStormscaleSwordItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return IonboundStormscaleAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return IonboundStormscaleAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return IonboundStormscaleAbilityManager.hasPendingFollowup(context.world(), context.actor())
                ? 1 : Math.max(1, Config.uniqueEffects.ionbound_stormscale.cooldown);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        IonboundStormscaleAbilityManager.tickStack(stack, world, entity);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.ionbound_stormscalesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.ionbound_stormscalesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.ionbound_stormscalesworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.ionbound_stormscalesworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.ionbound_stormscalesworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.ionbound_stormscalesworditem.tooltip6").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.ionbound_stormscale.cooldown);
        super.appendTooltip(stack, context, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "lightning");
    }

    public static final class EffectSettings extends TooltipSettings {
        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.IONBOUND_STORMSCALE::get));
        }

        @ValidatedInt.Restrict(min = 1) public int rechargeInterval = 160;
        @ValidatedDouble.Restrict(min = 0.01, max = 1.0) public double shieldTriggerHealthFraction = 0.20;
        @ValidatedInt.Restrict(min = 1) public int shieldDuration = 40;
        @ValidatedInt.Restrict(min = 1) public int cooldown = 60;
        @ValidatedDouble.Restrict(min = 1.0) public double corridorLength = 12.0;
        @ValidatedDouble.Restrict(min = 1.0) public double corridorWidth = 6.0;
        @ValidatedDouble.Restrict(min = 1.0) public double corridorHeight = 4.0;
        @ValidatedInt.Restrict(min = 1) public int corridorMaterializeTicks = 6;
        @ValidatedInt.Restrict(min = 1) public int corridorHoldTicks = 8;
        @ValidatedInt.Restrict(min = 1) public int corridorCloseTicks = 8;
        @ValidatedDouble.Restrict(min = 0.0) public double corridorPullStrength = 0.34;
        @ValidatedFloat.Restrict(min = 0.0F) public float slamDamageScaling = 1.0F;
        @ValidatedFloat.Restrict(min = 0.0F) public float slamSpellScaling = 2.0F;
        @ValidatedInt.Restrict(min = 1) public int slowDuration = 80;
        @ValidatedInt.Restrict(min = 0, max = 4) public int slowAmplifier = 1;
        @ValidatedInt.Restrict(min = 1) public int followupWindow = 20;
        @ValidatedDouble.Restrict(min = 0.1) public double beamWidth = 1.1;
        @ValidatedInt.Restrict(min = 1) public int beamDuration = 60;
        @ValidatedInt.Restrict(min = 1) public int beamDamageInterval = 5;
        @ValidatedFloat.Restrict(min = 0.0F) public float beamTotalDamageMultiplier = 4.0F;
        @ValidatedDouble.Restrict(min = 0.0, max = 0.99) public double beamMovementSpeedReduction = 0.90;
        @ValidatedFloat.Restrict(min = 0.0F) public float beamDamageScaling = 2.75F;
        @ValidatedFloat.Restrict(min = 0.0F) public float beamSpellScaling = 2.5F;
        @ValidatedInt.Restrict(min = 1) public int paralysisDuration = 100;
    }
}
