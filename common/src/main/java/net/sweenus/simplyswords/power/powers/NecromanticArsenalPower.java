package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
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
import net.sweenus.simplyswords.power.NetherGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.NecromanticArsenalManager;

import java.util.List;

public class NecromanticArsenalPower extends NetherGemPower {

    private static final ThreadLocal<Boolean> SUPPRESS_SUMMON = ThreadLocal.withInitial(() -> false);

    public NecromanticArsenalPower() {
        super(false);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (SUPPRESS_SUMMON.get() || !(attacker instanceof ServerPlayerEntity player) || attacker.getWorld().isClient()) {
            return;
        }

        if (attacker.getRandom().nextInt(100) >= Config.gemPowers.necromanticArsenal.chance) {
            return;
        }

        NecromanticArsenalManager.trySummon(player, stack);
    }

    public static void runSuppressed(Runnable runnable) {
        SUPPRESS_SUMMON.set(true);
        try {
            runnable.run();
        } finally {
            SUPPRESS_SUMMON.set(false);
        }
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.necromantic_arsenal").setStyle(Styles.NETHERFUSED));
        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.necromantic_arsenal.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
        }
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
        public double damageMultiplier = 1.0;
    }
}
