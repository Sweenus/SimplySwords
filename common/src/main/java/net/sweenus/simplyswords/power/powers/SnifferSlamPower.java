package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.SimplySwordsMinion;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.SnifferSlamManager;

import java.util.List;

public class SnifferSlamPower extends RunefusedGemPower {

    private static final ThreadLocal<Boolean> SUPPRESSED = ThreadLocal.withInitial(() -> false);

    public SnifferSlamPower() {
        super(false);
    }

    public static void runSuppressed(Runnable runnable) {
        boolean previous = SUPPRESSED.get();
        SUPPRESSED.set(true);
        try {
            runnable.run();
        } finally {
            SUPPRESSED.set(previous);
        }
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (SUPPRESSED.get() || attacker.getWorld().isClient()) {
            return;
        }

        if (attacker instanceof SimplySwordsMinion) {
            return;
        }

        DelegatedWeaponHitContext delegatedContext = SimplySwordsAPI.getDelegatedWeaponHitContext();
        if (delegatedContext != null && delegatedContext.actor() instanceof SimplySwordsMinion) {
            return;
        }

        if (attacker.getRandom().nextInt(100) >= Config.gemPowers.snifferSlam.chance) {
            return;
        }

        SnifferSlamManager.trySummon(attacker, target, stack);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        if (isRunic)
            tooltip.add(Text.translatable("item.simplyswords.snifferslamsworditem.tooltip1").setStyle(Styles.RUNIC));
        else
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.sniffer_slam").setStyle(Styles.RUNIC));

        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.snifferslamsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
        }
        TooltipUtils.appendGemPowerSpellScaleTooltip(tooltip, "sniffer_slam");
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.SNIFFER_SLAM);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 5;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.0)
        public double damageScaling = 2.0;
        @ValidatedDouble.Restrict(min = 0.0)
        public double spellScaling = 4.0;

        @ValidatedDouble.Restrict(min = 1.0)
        public double fallHeight = 8.0;

        @ValidatedDouble.Restrict(min = 0.05)
        public double fallSpeed = 0.75;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.25)
        public double impactRadius = 2.0;

        @ValidatedInt.Restrict(min = 1)
        public int immobiliseDurationTicks = 20;

        @ValidatedInt.Restrict(min = 1)
        public int lingerTicks = 20;

        @ValidatedInt.Restrict(min = 1)
        public int maxActiveSlams = 1;

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int screamingGoatSoundChance = 5;
    }
}
