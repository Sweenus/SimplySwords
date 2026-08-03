package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.ChainLightningVisualManager;

import java.util.List;

public class StormlashPower extends RunefusedGemPower {

    public StormlashPower() {
        super(false);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) {
            return;
        }

        if (attacker.getRandom().nextInt(100) >= Config.gemPowers.stormlash.chance) {
            return;
        }

        float damage = HelperMethods.gemPowerScaledDamage("lightning", attacker, stack,
                Config.gemPowers.stormlash.damageScaling,
                Config.gemPowers.stormlash.spellScaling);
        DelegatedWeaponHitContext context = SimplySwordsAPI.getDelegatedWeaponHitContext();
        if (context != null) {
            ChainLightningVisualManager.damageChain((net.minecraft.server.world.ServerWorld) attacker.getWorld(), attacker, context.actor(), target,
                    Config.gemPowers.stormlash.chainCount,
                    damage,
                    Config.gemPowers.stormlash.range,
                    ChainLightningVisualManager.STORMBRINGER_SETTINGS);
        } else {
            ChainLightningVisualManager.damageChain((net.minecraft.server.world.ServerWorld) attacker.getWorld(), attacker, target,
                    Config.gemPowers.stormlash.chainCount,
                    damage,
                    Config.gemPowers.stormlash.range,
                    ChainLightningVisualManager.STORMBRINGER_SETTINGS);
        }
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext, boolean isRunic) {
        if (isRunic) {
            tooltip.add(Text.translatable("item.simplyswords.stormlashsworditem.tooltip1").setStyle(Styles.RUNIC));
        } else {
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.stormlash").setStyle(Styles.RUNIC));
        }

        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.stormlashsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
        }
        TooltipUtils.appendSpellScaleTooltip(tooltip, "lightning");
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.STORMLASH);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;

        @ValidatedInt.Restrict(min = 1)
        public int chainCount = 6;

        @ValidatedDouble.Restrict(min = 0.0)
        public double range = 6.0;

        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.35f;

        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.8f;
    }
}
