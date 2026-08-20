package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.DevourerAbilityManager;

import java.util.List;

public final class DevourerClaymoreItem extends UniqueSwordItem implements UniqueWeaponActiveAbility, TwoHandedWeapon {
    public DevourerClaymoreItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return DevourerAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return DevourerAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.devourer.cooldown);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SCULK_SOUL,
                ParticleTypes.REVERSE_PORTAL, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.devourerclaymoreitem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.devourerclaymoreitem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.devourerclaymoreitem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.devourerclaymoreitem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.devourerclaymoreitem.tooltip3",
                Config.uniqueEffects.devourer.maxTargets).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.devourerclaymoreitem.tooltip4",
                Config.uniqueEffects.devourer.duration / 20.0F).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.gloam.tooltip").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.devourer.cooldown);
        appendAbilityManaCostTooltip(tooltip, stack);
        super.appendTooltip(stack, context, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, SpellScalingProfile.SOUL);
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.devourer");
    }

    public static final class EffectSettings extends TooltipSettings {
        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.THE_DEVOURER::get));
        }

        @ValidatedInt.Restrict(min = 1) public int cooldown = 1200;
        @ValidatedInt.Restrict(min = 1) public int duration = 800;
        @ValidatedDouble.Restrict(min = 1.0) public double castRange = 14.0;
        @ValidatedDouble.Restrict(min = 1.0) public double targetingRadius = 7.0;
        @ValidatedDouble.Restrict(min = 1.0) public double verticalRange = 5.0;
        @ValidatedInt.Restrict(min = 1, max = 16) public int maxTargets = 6;
        @ValidatedDouble.Restrict(min = 0.0) public double pullStrength = 0.28;
        @ValidatedInt.Restrict(min = 1, max = 128) public int looseTargetCap = 64;
        @ValidatedInt.Restrict(min = 1, max = 3) public int looseTendrilCap = 3;
        @ValidatedDouble.Restrict(min = 0.0) public double loosePullStrength = 0.36;
        @ValidatedInt.Restrict(min = 0, max = 4) public int stainSlowAmplifier = 0;
        @ValidatedInt.Restrict(min = 1) public int stainSpreadDuration = 40;
        @ValidatedInt.Restrict(min = 1, max = 32) public int stainCarrierCap = 12;
        @ValidatedDouble.Restrict(min = 0.5, max = 3.0) public double stainTrailWidth = 1.35;
        @ValidatedFloat.Restrict(min = 0.2F) public float startingMassRadius = 0.2F;
        @ValidatedFloat.Restrict(min = 0.2F) public float maximumMassRadius = 3.9F;
        @ValidatedInt.Restrict(min = 1) public int damageInterval = 20;
        @ValidatedFloat.Restrict(min = 0.0F) public float damageScaling = 0.12F;
        @ValidatedFloat.Restrict(min = 0.0F) public float spellScaling = 0.7436F;
        @ValidatedDouble.Restrict(min = 1.0) public double reprisalReach = 24.0;
        @ValidatedDouble.Restrict(min = 0.5) public double reprisalRadius = 3.5;
        @ValidatedInt.Restrict(min = 1, max = 16) public int reprisalTargetCap = 6;
        @ValidatedInt.Restrict(min = 1, max = 40) public int reprisalDragDuration = 10;
        @ValidatedDouble.Restrict(min = 0.0) public double reprisalPullStrength = 0.32;
        @ValidatedFloat.Restrict(min = 0.0F) public float reprisalDamageScaling = 0.50F;
        @ValidatedFloat.Restrict(min = 0.0F) public float reprisalSpellScaling = 3.0983F;
    }
}
