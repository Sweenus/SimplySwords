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
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.NetherGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.VerdantTrailManager;

import java.util.List;

public class VerdantTrailPower extends NetherGemPower {

    public VerdantTrailPower() {
        super(false);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {
        if (!world.isClient && user instanceof LivingEntity livingUser) {
            VerdantTrailManager.tryPlaceTrail(livingUser, stack);
        }
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
        tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.verdant_trail").setStyle(Styles.NETHERFUSED));

        if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
            tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.verdant_trail.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
        }
        TooltipUtils.appendSpellScaleTooltip(tooltip, "nature");
    }

    public static class Settings extends TooltipSettings {

        public Settings() {
            super(GemPowerRegistry.VERDANT_TRAIL);
        }

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedInt.Restrict(min = 1)
        public int duration = 100;

        @ValidatedDouble.Restrict(min = 0.1)
        public double placementSpacing = 1.25;

        @ValidatedInt.Restrict(min = 1)
        public int placementCooldown = 4;

        @Translation(prefix = "simplyswords.config.basic_settings")
        @ValidatedDouble.Restrict(min = 0.1)
        public double radius = 1.35;

        @ValidatedInt.Restrict(min = 1)
        public int auraInterval = 20;

        @ValidatedInt.Restrict(min = 1)
        public int regenerationDuration = 60;

        @ValidatedInt.Restrict(min = 0)
        public int regenerationAmplifier = 0;

        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.18F;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.36F;

        @ValidatedInt.Restrict(min = 1)
        public int visualPointsMin = 7;

        @ValidatedInt.Restrict(min = 1)
        public int visualPointsMax = 11;
    }
}
