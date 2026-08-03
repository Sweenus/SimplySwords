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
import net.sweenus.simplyswords.power.NetherGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.GoatStampedeManager;

import java.util.List;

public class GoatStampedePower extends NetherGemPower {

    public GoatStampedePower() {
        super(false);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) {
            return;
        }

        if (attacker instanceof SimplySwordsMinion) {
            return;
        }

        DelegatedWeaponHitContext delegatedContext = SimplySwordsAPI.getDelegatedWeaponHitContext();
        if (delegatedContext != null && delegatedContext.actor() instanceof SimplySwordsMinion) {
            return;
        }

        if (attacker.getRandom().nextInt(100) >= Config.gemPowers.goatStampede.chance) {
            return;
        }

        GoatStampedeManager.trySummon(attacker, stack);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.goat_stampede").setStyle(Styles.NETHERFUSED));
        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.goat_stampede.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
        }
        TooltipUtils.appendSpellScaleTooltip(tooltip, "nature");
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.GOAT_STAMPEDE);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;

        @ValidatedInt.Restrict(min = 1)
        public int goatCount = 4;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 1)
        public int duration = 50;

        @ValidatedDouble.Restrict(min = 0.0)
        public double damageScaling = 0.30;
        @ValidatedDouble.Restrict(min = 0.0)
        public double spellScaling = 0.60;

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int screamingChance = 15;

        @ValidatedDouble.Restrict(min = 0.0)
        public double screamingDamageScaling = 0.50;
        @ValidatedDouble.Restrict(min = 0.0)
        public double screamingSpellScaling = 1.0;

        @ValidatedDouble.Restrict(min = 0.0)
        public double knockbackStrength = 0.9;

        @ValidatedDouble.Restrict(min = 0.0)
        public double chargeSpeed = 0.55;

        @ValidatedDouble.Restrict(min = 0.0)
        public double summonerKnockback = 0.5;

        @ValidatedInt.Restrict(min = 1)
        public int maxActiveStampedes = 1;
    }
}
