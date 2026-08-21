package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.DeathKnellAbilityManager;

import java.util.List;

public class PlagueSwordItem extends UniqueSwordItem {
    public PlagueSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (attacker.getWorld() instanceof ServerWorld serverWorld) {
            HelperMethods.playHitSounds(attacker, target);
            DeathKnellAbilityManager.onMeleeHit(serverWorld, stack, attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SPORE_BLOSSOM_AIR,
                ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.FALLING_SPORE_BLOSSOM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip2",
                Config.uniqueEffects.toxic_longsword.feverThreshold).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip3",
                Config.uniqueEffects.toxic_longsword.maxCascadeTolls).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip4").setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "soul");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.TOXIC_LONGSWORD::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 55;
        @ValidatedInt.Restrict(min = 0)
        public int feverPerHit = 1;
        @ValidatedInt.Restrict(min = 0)
        public int conversionFeverBonus = 2;
        @ValidatedInt.Restrict(min = 1)
        public int feverThreshold = 5;
        @ValidatedInt.Restrict(min = 1)
        public int feverDuration = 120;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float tollDamageScaling = 0.60F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float tollSpellScaling = 2.75F;
        @ValidatedFloat.Restrict(min = 0.1F)
        public float tollRadius = 5.0F;
        @ValidatedInt.Restrict(min = 0)
        public int tollFeverSpread = 2;
        @ValidatedInt.Restrict(min = 1)
        public int maxCascadeTolls = 6;
        @ValidatedFloat.Restrict(min = 0.0F, max = 1.0F)
        public float copiedAilmentDurationMultiplier = 0.50F;

    }
}
