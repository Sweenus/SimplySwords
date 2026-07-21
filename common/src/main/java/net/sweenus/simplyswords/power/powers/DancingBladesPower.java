package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.DancingBladeManager;

import java.util.List;

public class DancingBladesPower extends RunefusedGemPower {

    public DancingBladesPower() {
        super(false);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player) || attacker.getWorld().isClient()) {
            return;
        }

        if (attacker.getRandom().nextInt(100) >= Config.gemPowers.dancingBlades.chance) {
            return;
        }

        DancingBladeManager.trySummon(player, stack);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        if (isRunic) {
            tooltip.add(Text.translatable("item.simplyswords.dancingbladessworditem.tooltip1").setStyle(Styles.RUNIC));
        } else {
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.dancing_blades").setStyle(Styles.RUNIC));
        }

        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.dancingbladessworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
        }
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.DANCING_BLADES);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 5;

        @ValidatedInt.Restrict(min = 1)
        public int maxSwords = 3;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 1)
        public int duration = 400;

        @ValidatedDouble.Restrict(min = 0.0)
        public double orbitRadius = 2.65;

        @ValidatedFloat.Restrict(min = 0f)
        public float damageMultiplier = 1.0f;
    }
}
