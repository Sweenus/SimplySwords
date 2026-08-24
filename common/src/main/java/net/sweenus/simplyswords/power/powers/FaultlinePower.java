package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.FaultlineSunderManager;

import java.util.List;

public class FaultlinePower extends RunefusedGemPower {

    public FaultlinePower() {
        super(false);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) {
            return;
        }
        if (attacker.getRandom().nextInt(100) >= Config.gemPowers.faultline.chance) {
            return;
        }

        float damage = HelperMethods.gemPowerScaledDamage(
                SpellScalingComponents.power("faultline"),
                attacker,
                stack,
                Config.gemPowers.faultline.damageScaling,
                Config.gemPowers.faultline.spellScaling
        );
        DelegatedWeaponHitContext context = SimplySwordsAPI.getDelegatedWeaponHitContext();
        if (context != null) {
            FaultlineSunderManager.createSunder((net.minecraft.server.world.ServerWorld) attacker.getWorld(), attacker, context.origin(), context.facing(), damage);
        } else {
            FaultlineSunderManager.createSunder((net.minecraft.server.world.ServerWorld) attacker.getWorld(), attacker, damage);
        }
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        if (isRunic) {
            tooltip.add(Text.translatable("item.simplyswords.faultlinesworditem.tooltip1").setStyle(Styles.RUNIC));
        } else {
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.faultline").setStyle(Styles.RUNIC));
        }

        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.faultlinesworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
        }
        TooltipUtils.appendGemPowerSpellScaleTooltip(tooltip, "faultline");
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.FAULTLINE);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 10;

        @ValidatedDouble.Restrict(min = 1.0)
        public double length = 8.0;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.5)
        public double width = 2.4;

        @ValidatedDouble.Restrict(min = 0.25)
        public double stepDistance = 1.0;

        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.35F;

        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.8F;

        @ValidatedFloat.Restrict(min = 0f)
        public float centerSpikeHeight = 1.05F;

        @ValidatedFloat.Restrict(min = 0f)
        public float edgeSpikeHeight = 2.6F;

        @ValidatedInt.Restrict(min = 1)
        public int riseTicks = 6;

        @ValidatedInt.Restrict(min = 0)
        public int holdTicks = 4;

        @ValidatedInt.Restrict(min = 1)
        public int sinkTicks = 8;

        @ValidatedInt.Restrict(min = 0)
        public int waveStepDelayTicks = 1;
    }
}
