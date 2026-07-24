package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
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
import net.sweenus.simplyswords.world.WolfPackManager;

import java.util.List;

public class WolfPackPower extends NetherGemPower {

    public WolfPackPower() {
        super(false);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) {
            return;
        }

        if (attacker.getRandom().nextInt(100) >= Config.gemPowers.wolfPack.chance) {
            return;
        }

        WolfPackManager.trySummon(attacker, stack);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.wolf_pack").setStyle(Styles.NETHERFUSED));
        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.wolf_pack.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
        }
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.WOLF_PACK);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 40;

        @ValidatedInt.Restrict(min = 1)
        public int maxMinions = 2;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 1)
        public int duration = 1400;

        @ValidatedDouble.Restrict(min = 0.0)
        public double damageScaling = 1.0;

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int activeAbilityChance = 35;

        @ValidatedInt.Restrict(min = 1)
        public int activeAbilityCheckInterval = 40;

        @ValidatedInt.Restrict(min = 0)
        public int activeAbilityInitialDelay = 40;
    }
}
