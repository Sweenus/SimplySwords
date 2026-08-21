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
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.NetherGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.DragonMawManager;

import java.util.List;

public class DragonMawPower extends NetherGemPower {

    public DragonMawPower() {
        super(false);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {
        if (world.isClient()) {
            return;
        }

        int frequency = Math.max(1, Config.gemPowers.dragonMaw.scanFrequencyTicks);
        if (user.age % frequency != 0) {
            return;
        }

        DragonMawManager.tryActivate(user, stack);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.dragon_maw").setStyle(Styles.NETHERFUSED));

        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.dragon_maw.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
        }
        TooltipUtils.appendGemPowerSpellScaleTooltip(tooltip, "dragon_maw");
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.DRAGON_MAW);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;

        @ValidatedInt.Restrict(min = 1)
        public int scanFrequencyTicks = 20;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.5)
        public double range = 8.0;

        @ValidatedInt.Restrict(min = 1)
        public int visualLifetimeTicks = 300;

        @ValidatedInt.Restrict(min = 0)
        public int breathDelayTicks = 12;

        @ValidatedInt.Restrict(min = 1)
        public int breathRepeatTicks = 60;

        @ValidatedInt.Restrict(min = 1)
        public int breathCloudDurationTicks = 50;

        @ValidatedDouble.Restrict(min = 0.25)
        public double breathRadius = 2.25;

        @ValidatedInt.Restrict(min = 1)
        public int damageIntervalTicks = 10;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.45F;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.90F;

        @ValidatedFloat.Restrict(min = 0.1f)
        public float headScale = 2.35F;

        @ValidatedDouble.Restrict(min = -4.0, max = 4.0)
        public double headVerticalOffset = -0.59;

        @ValidatedFloat.Restrict(min = 0.1f)
        public float turnSpeedDegreesPerTick = 6.0F;
    }
}
