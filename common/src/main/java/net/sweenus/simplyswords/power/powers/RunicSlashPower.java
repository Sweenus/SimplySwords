package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.RunicSlashManager;

import java.util.List;

public class RunicSlashPower extends RunefusedGemPower {

    public RunicSlashPower() {
        super(false);
    }

    @Override
    public void onSwing(ItemStack stack, ServerWorld world, LivingEntity user, Hand hand) {
        RunicSlashManager.tryFire(world, user, stack);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        tooltip.add(Text.translatable("item.simplyswords.runicslashsworditem.tooltip1").setStyle(Styles.RUNIC));

        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.translatable("item.simplyswords.onattack").setStyle(Styles.RIGHT_CLICK));
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.runicslashsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
        }
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.RUNIC_SLASH);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.25)
        public double distance = 16.0;

        @ValidatedDouble.Restrict(min = 0.05)
        public double speed = 1.2;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.1)
        public double width = 1.15;

        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 1.0F;

        @ValidatedInt.Restrict(min = 1)
        public int minimumSwingCooldownTicks = 2;
    }
}
