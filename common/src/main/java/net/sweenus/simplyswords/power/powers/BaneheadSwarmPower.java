package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.BaneheadSwarmManager;

import java.util.List;

public class BaneheadSwarmPower extends RunefusedGemPower {

    public BaneheadSwarmPower() {
        super(false);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {
        if (world.isClient()) {
            return;
        }

        int frequency = Math.max(1, Config.gemPowers.baneheadSwarm.spawnFrequency);
        if (user.age % frequency != 0) {
            return;
        }

        BaneheadSwarmManager.trySpawnHead(user, stack);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        if (isRunic)
            tooltip.add(Text.translatable("item.simplyswords.baneheadswarmsworditem.tooltip1").setStyle(Styles.RUNIC));
        else
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.banehead_swarm").setStyle(Styles.RUNIC));

        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.baneheadswarmsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
        }
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.BANEHEAD_SWARM);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 1)
        public int maxActiveHeads = 5;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 1)
        public int spawnFrequency = 60;

        @ValidatedDouble.Restrict(min = 0.0)
        public double orbitRadius = 1.6;

        @ValidatedDouble.Restrict(min = 0.0)
        public double orbitHeight = 1.4;

        @ValidatedDouble.Restrict(min = 0.0)
        public double orbitAngularSpeed = 0.05;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.0)
        public double homingRange = 12.0;

        @ValidatedDouble.Restrict(min = 0.0)
        public double homingSpeed = 0.4;

        @ValidatedDouble.Restrict(min = 0.0, max = 1.0)
        public double homingInitialSpeedFraction = 0.3;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 1)
        public int homingAccelerationTicks = 20;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0)
        public int homingCooldownTicks = 30;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.1)
        public double homingTurnRateDegrees = 9.0;

        @ValidatedDouble.Restrict(min = 0.0)
        public double explosionProximity = 1.1;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.0)
        public double explosionRadius = 2.5;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.0)
        public double damageScaling = 0.6;

        @ValidatedInt.Restrict(min = 1)
        public int maxHomingTicks = 100;
    }
}
