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
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.NetherGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.WingBuffetManager;

import java.util.List;

public class WingBuffetPower extends NetherGemPower {

    public WingBuffetPower() {
        super(false);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) {
            return;
        }

        if (attacker.getRandom().nextInt(100) >= Config.gemPowers.wingBuffet.chance) {
            return;
        }

        WingBuffetManager.tryActivate(attacker, stack);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.wing_buffet").setStyle(Styles.NETHERFUSED));
        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.wing_buffet.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
        }
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.WING_BUFFET);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;

        @ValidatedDouble.Restrict(min = 0.5)
        public double range = 5.5;

        @ValidatedDouble.Restrict(min = 1.0, max = 180.0)
        public double coneAngleDegrees = 70.0;

        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 1.75F;

        @ValidatedDouble.Restrict(min = 0.0)
        public double knockbackStrength = 1.4;

        @ValidatedDouble.Restrict(min = 0.0)
        public double upwardKnockback = 0.25;

        @ValidatedInt.Restrict(min = 1)
        public int visualLifetime = 18;

        @ValidatedFloat.Restrict(min = 0.1f)
        public float visualScale = 0.65F;
    }
}
