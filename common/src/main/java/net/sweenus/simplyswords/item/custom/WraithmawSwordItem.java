package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.WraithmawAbilityManager;

import java.util.List;

public final class WraithmawSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public WraithmawSwordItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return WraithmawAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return WraithmawAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.wraithmaw.cooldown);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wraithmawsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.wraithmawsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wraithmawsworditem.tooltip3",
                Config.uniqueEffects.wraithmaw.maxRecovered,
                Config.uniqueEffects.wraithmaw.recoveredDuration / 20.0F).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.wraithmawsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.wraithmaw.cooldown);
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.wraithmaw");
    }

    public static final class EffectSettings extends TooltipSettings {
        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WRAITHMAW::get));
        }

        @ValidatedInt.Restrict(min = 1) public int cooldown = 600;
        @ValidatedDouble.Restrict(min = 1.0) public double castRange = 18.0;
        @ValidatedDouble.Restrict(min = 0.5) public double stormRadius = 6.0;
        @ValidatedInt.Restrict(min = 1, max = 32) public int cutlassCount = 16;
        @ValidatedDouble.Restrict(min = 0.0, max = 2.0) public double collisionGrace = 0.45;
        @ValidatedInt.Restrict(min = 20) public int embeddedDuration = 500;
        @ValidatedInt.Restrict(min = 1, max = 8) public int maxRecovered = 6;
        @ValidatedInt.Restrict(min = 20) public int recoveredDuration = 900;
        @ValidatedDouble.Restrict(min = 0.1) public double launchSpeed = 1.35;
        @ValidatedDouble.Restrict(min = 1.0) public double launchRange = 24.0;
        @ValidatedDouble.Restrict(min = 0.0) public double homingRange = 4.0;
        @ValidatedDouble.Restrict(min = 0.0, max = 45.0) public double homingTurnRate = 7.0;
        @ValidatedDouble.Restrict(min = 0.25) public double stainRadius = 1.25;
        @ValidatedInt.Restrict(min = 20) public int stainDuration = 240;
        @ValidatedInt.Restrict(min = 1) public int stainFadeDuration = 40;
        @ValidatedInt.Restrict(min = 0, max = 4) public int stainSlowAmplifier = 0;
    }
}
