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
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.TwistedBladeAbilityManager;

import java.util.List;

public class TwistedBladeItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public TwistedBladeItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (attacker.getWorld() instanceof ServerWorld world) {
            HelperMethods.playHitSounds(attacker, target);
            TwistedBladeAbilityManager.onMeleeHit(world, stack, attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return TwistedBladeAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return TwistedBladeAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return 1;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ASH,
                ParticleTypes.ASH, ParticleTypes.ASH, false);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip2",
                Config.uniqueEffects.twisted_blade.maxStacks).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip4").setStyle(Styles.TEXT));

        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.TWISTED_BLADE::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 75;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 100;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 15;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float attackSpeedPerStack = 0.10F;
        @ValidatedInt.Restrict(min = 1)
        public int crescendoBaseInterval = 5;
        @ValidatedInt.Restrict(min = 1)
        public int crescendoMinimumInterval = 2;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float crescendoDamageScaling = 0.35F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float crescendoSpellScaling = 1.8331F;
        @ValidatedDouble.Restrict(min = 0.1)
        public double crescendoRadius = 2.5;
        @ValidatedDouble.Restrict(min = 0.0)
        public double crescendoKnockback = 0.25;
        @ValidatedInt.Restrict(min = 1)
        public int empoweredWindow = 100;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float empoweredMinimumDamageScaling = 0.5F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float empoweredMinimumSpellScaling = 2.6188F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float empoweredMaximumDamageScaling = 2.5F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float empoweredMaximumSpellScaling = 13.0938F;
        @ValidatedDouble.Restrict(min = 0.1)
        public double empoweredMinimumRadius = 2.5;
        @ValidatedDouble.Restrict(min = 0.1)
        public double empoweredMaximumRadius = 4.0;
        @ValidatedDouble.Restrict(min = 0.0)
        public double empoweredMinimumKnockback = 0.35;
        @ValidatedDouble.Restrict(min = 0.0)
        public double empoweredMaximumKnockback = 0.9;
    }
}
