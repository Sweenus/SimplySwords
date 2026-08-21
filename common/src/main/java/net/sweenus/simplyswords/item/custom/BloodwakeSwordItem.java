package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.BloodwakeAbilityManager;

import java.util.List;

public class BloodwakeSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public BloodwakeSwordItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (AwakeningApi.isAbilityUnlocked(stack) && attacker.getWorld() instanceof ServerWorld world) {
            BloodwakeAbilityManager.triggerPassiveHit(world, stack, attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        if (context == null || context.world() == null || context.actor() == null || !context.actor().isAlive()
                || context.stack() == null || context.stack().isEmpty()
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1) {
            return false;
        }
        int tier = BloodwakeAbilityManager.getFrenzy(context.stack());
        return tier > 0;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        int tier = BloodwakeAbilityManager.getFrenzy(context.stack());
        if (!BloodwakeAbilityManager.activate(context.world(), context.actor(), context.stack(),
                context.hand(), context.facing(), tier)) {
            return false;
        }
        BloodwakeAbilityManager.setFrenzy(context.stack(), 0);
        context.actor().swingHand(context.hand() == null ? Hand.MAIN_HAND : context.hand(), true);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.bloodwake.cooldown);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.bloodwakesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.bloodwakesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.bloodwakesworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.bloodwakesworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.bloodwakesworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.bloodwakesworditem.tooltip5").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, stack, Config.uniqueEffects.bloodwake.cooldown);
        appendAbilityManaCostTooltip(tooltip, stack);
        super.appendTooltip(stack, context, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "soul");
    }

    public static class EffectSettings extends TooltipSettings {
        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.BLOODWAKE::get));
        }

        @ValidatedInt.Restrict(min = 1)
        public int cooldown = 10;
        @ValidatedDouble.Restrict(min = 0.5)
        public double burstRadius = 4.0;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float burstDamageScaling = 0.65F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float burstSpellScaling = 2.3491F;
        @ValidatedInt.Restrict(min = 1)
        public int plagueDuration = 160;
        @ValidatedDouble.Restrict(min = 0.5)
        public double plagueRadius = 4.0;
        @ValidatedDouble.Restrict(min = 1.0)
        public double targetingRadius = 10.0;
        @ValidatedInt.Restrict(min = 1, max = 64)
        public int maximumScreamTargets = 6;
        @ValidatedInt.Restrict(min = 1)
        public int screamDuration = 200;
        @ValidatedInt.Restrict(min = 1, max = 128)
        public int bladeTargetCap = 24;
        @ValidatedInt.Restrict(min = 1)
        public int bladeHoverTicks = 30;
        @ValidatedInt.Restrict(min = 1)
        public int bladePlungeTicks = 10;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float bladeDamagePerBleedStack = 0.20F;
        @ValidatedInt.Restrict(min = 1, max = 32)
        public int bloodFlyCount = 8;
        @ValidatedInt.Restrict(min = 20)
        public int bloodFlyLifetime = 200;
        @ValidatedInt.Restrict(min = 1)
        public int bloodFlyContacts = 3;
        @ValidatedInt.Restrict(min = 1)
        public int bloodFlyContactInterval = 20;
        @ValidatedDouble.Restrict(min = 0.1)
        public double waveThickness = 1.25;
        @ValidatedDouble.Restrict(min = 0.1)
        public double waveStepDistance = 0.8;
        @ValidatedInt.Restrict(min = 1)
        public int waveStepInterval = 1;
        @ValidatedInt.Restrict(min = 1)
        public int waveLengthSteps = 10;
        @ValidatedDouble.Restrict(min = 0.0)
        public double wavePush = 0.65;
        @ValidatedDouble.Restrict(min = 0.0)
        public double waveLift = 0.15;
        @ValidatedInt.Restrict(min = 20)
        public int delugeDuration = 100;
        @ValidatedInt.Restrict(min = 1)
        public int delugeExtensionDuration = 100;
        @ValidatedInt.Restrict(min = 1)
        public int delugeVolleyInterval = 15;
        @ValidatedInt.Restrict(min = 1)
        public int stainDuration = 600;
        @ValidatedInt.Restrict(min = 1)
        public int stainFadeDuration = 100;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float stainHealAmount = 1.0F;
        @ValidatedInt.Restrict(min = 1)
        public int stainHealInterval = 40;
        @ValidatedInt.Restrict(min = 0, max = 4)
        public int stainSlowAmplifier = 0;
    }
}
