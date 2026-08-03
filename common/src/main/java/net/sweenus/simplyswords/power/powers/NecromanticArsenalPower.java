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
import net.sweenus.simplyswords.world.MinionHitSuppression;
import net.sweenus.simplyswords.world.NecromanticArsenalManager;

import java.util.List;

public class NecromanticArsenalPower extends NetherGemPower {

    public NecromanticArsenalPower() {
        super(false);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) {
            return;
        }

        if (attacker.getRandom().nextInt(100) >= Config.gemPowers.necromanticArsenal.chance) {
            return;
        }

        NecromanticArsenalManager.trySummon(attacker, stack);
    }

    public static void runSuppressed(Runnable runnable) {
        MinionHitSuppression.runSuppressed(runnable);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.necromantic_arsenal").setStyle(Styles.NETHERFUSED));
        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.necromantic_arsenal.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
        }
        TooltipUtils.appendSpellScaleTooltip(tooltip, "soul");
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.NECROMANTIC_ARSENAL);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 10;

        @ValidatedInt.Restrict(min = 1)
        public int maxMinions = 3;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 1)
        public int duration = 600;

        @ValidatedDouble.Restrict(min = 0.0)
        public double damageScaling = 0.6;
        @ValidatedDouble.Restrict(min = 0.0)
        public double spellScaling = 1.2;

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int activeAbilityChance = 10;

        @ValidatedInt.Restrict(min = 1)
        public int activeAbilityCheckInterval = 110;

        @ValidatedInt.Restrict(min = 0)
        public int activeAbilityInitialDelay = 40;
    }
}
