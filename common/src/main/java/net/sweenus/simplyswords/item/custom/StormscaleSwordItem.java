package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.StormscaleLightningRodManager;

import java.util.List;

public class StormscaleSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public StormscaleSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return StormscaleLightningRodManager.canStart(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return StormscaleLightningRodManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.stormscale.cooldown;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (AwakeningApi.isAbilityUnlocked(stack) && attacker.getWorld() instanceof ServerWorld world) {
            HelperMethods.playHitSounds(attacker, target);
            StormscaleLightningRodManager.onMeleeHit(world, stack, attacker);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ELECTRIC_SPARK,
                ParticleTypes.ELECTRIC_SPARK, ParticleTypes.ENCHANTED_HIT, false);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext,
                              List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormscalesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stormscalesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stormscalesworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormscalesworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.stormscale.cooldown);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "lightning");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.STORMSCALE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 1000;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 800;
        @ValidatedDouble.Restrict(min = 1.0)
        public double targetingRange = 18.0;
        @ValidatedDouble.Restrict(min = 1.0)
        public double maxTetherDistance = 32.0;
        @ValidatedInt.Restrict(min = 1)
        public int energyTravelTicks = 16;
        @ValidatedDouble.Restrict(min = 0.1)
        public double pulseRadius = 3.5;
        @ValidatedFloat.Restrict(min = 0.0F, max = 0.8F)
        public float pulseGrowthPerHit = 0.01F;
        @ValidatedFloat.Restrict(min = 0.0F, max = 0.8F)
        public float maximumPulseGrowth = 0.8F;
        @ValidatedDouble.Restrict(min = 0.0, max = 1.0)
        public double pulsePullStrength = 0.32;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float pulseDamageScaling = 0.4F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float pulseSpellScaling = 0.8F;
    }
}
