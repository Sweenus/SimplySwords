package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.HearthflameAbilityManager;

import java.util.List;

public class HearthflameSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {

    public HearthflameSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (attacker.getWorld() instanceof ServerWorld world) {
            HelperMethods.playHitSounds(attacker, target);
            HearthflameAbilityManager.onMeleeHit(world, stack, attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return HearthflameAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return HearthflameAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.hearthflame.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(
                entity,
                stack,
                world,
                ParticleTypes.FALLING_LAVA,
                ParticleTypes.LAVA,
                ParticleTypes.LARGE_SMOKE,
                true
        );
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world,
                              List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip8").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.hearthflame.cooldown);
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.HEARTHFLAME::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 50;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 450;
        @ValidatedInt.Restrict(min = 1)
        public int brandDuration = 350;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 220;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 10;
        @ValidatedDouble.Restrict(min = 0.0)
        public double brandedRangeBonus = 4.0;
        @ValidatedInt.Restrict(min = 1)
        public int maxChains = 6;
        @ValidatedInt.Restrict(min = 1)
        public int maximumPressure = 300;
        @ValidatedDouble.Restrict(min = 0.5)
        public double minimumChainLength = 2.5;
        @ValidatedDouble.Restrict(min = 0.0)
        public double pullStrength = 0.16;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float echoDamageScaling = 0.20F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float echoSpellScaling = 0.40F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float snapDamageScaling = 0.55F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float snapSpellScaling = 0.85F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float finalDamageScaling = 0.90F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float finalSpellScaling = 1.25F;
        @ValidatedDouble.Restrict(min = 0.1)
        public double snapRadius = 2.5;
        @ValidatedInt.Restrict(min = 0)
        public int igniteSeconds = 4;
    }
}
