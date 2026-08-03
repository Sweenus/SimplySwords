package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.EvocationFangManager;

import java.util.List;

public class EvocationPower extends RunefusedGemPower {

    public EvocationPower() {
        super(false);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {
        if (world.isClient()) {
            return;
        }

        int frequency = Math.max(1, Config.gemPowers.evocation.scanFrequencyTicks);
        if (user.age % frequency != 0) {
            return;
        }

        EvocationFangManager.tryStrikeNearby(user, stack);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext, boolean isRunic) {
        if (isRunic)
            tooltip.add(Text.translatable("item.simplyswords.evocationsworditem.tooltip1").setStyle(Styles.RUNIC));
        else
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.evocation").setStyle(Styles.RUNIC));

        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.evocationsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
        }
        TooltipUtils.appendSpellScaleTooltip(tooltip, "evocation");
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.EVOCATION);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.5)
        public double range = 8.0;

        @ValidatedInt.Restrict(min = 1)
        public int scanFrequencyTicks = 10;

        @ValidatedInt.Restrict(min = 1)
        public int maxTargetsPerScan = 3;

        @ValidatedInt.Restrict(min = 1)
        public int targetCooldownTicks = 60;

        @ValidatedInt.Restrict(min = 0)
        public int fangWarmupTicks = 0;

        @ValidatedInt.Restrict(min = 0)
        public int damageDelayTicks = 8;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.2F;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.4F;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 1)
        public int slownessDurationTicks = 60;

        @ValidatedInt.Restrict(min = 0, max = 1)
        public int slownessAmplifier = 1;
    }
}
