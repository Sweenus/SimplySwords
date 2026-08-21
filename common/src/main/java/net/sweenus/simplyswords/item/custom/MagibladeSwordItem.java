package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.MagibladeAbilityManager;

import java.util.List;

public class MagibladeSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public MagibladeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }


    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        TypedActionResult<ItemStack> result = UniqueWeaponActiveAbility.super.startPlayerAbility(world, user, hand);
        if (result.getResult().isAccepted()) {
            user.setCurrentHand(hand);
        }
        return result;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        // Charging is advanced by MagibladeAbilityManager for both players and non-player wielders.
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return Math.max(1, Config.uniqueEffects.magiblade.chargeDuration) + 5;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient()) {
            MagibladeAbilityManager.cancelCharging(user, true);
        }
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return MagibladeAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return MagibladeAbilityManager.startCharging(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.magiblade.chargeDuration)
                + Math.max(1, Config.uniqueEffects.magiblade.cooldown);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && entity instanceof LivingEntity livingEntity) {
            MagibladeAbilityManager.tickHeldPassive(livingEntity, stack);
        }
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT,
                ParticleTypes.ENCHANT, ParticleTypes.ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable(
                "item.simplyswords.magibladesworditem.tooltip5",
                Math.max(1, Config.uniqueEffects.magiblade.summonDuration) / 20.0F
        ).setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.magiblade.cooldown);

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "arcane");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MAGIBLADE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 35;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.85f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 4.20f;
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int repelChance = 55;
        @ValidatedFloat.Restrict(min = 1f)
        public float repelRadius = 4f;
        @ValidatedFloat.Restrict(min = 1f)
        public float sonicDistance = 16f;
        @ValidatedInt.Restrict(min = 1)
        public int chargeDuration = 10;
        @ValidatedInt.Restrict(min = 1)
        public int summonDuration = 300;
        @ValidatedInt.Restrict(min = 1)
        public int sonicInterval = 100;
        @ValidatedInt.Restrict(min = 1)
        public int sonicChargeDuration = 12;
        @ValidatedInt.Restrict(min = 1)
        public int repelFrequency = 8;
        @ValidatedFloat.Restrict(min = 0f)
        public float headOrbitRadius = 0.9f;
        @ValidatedFloat.Restrict(min = -4f, max = 4f)
        public float headVerticalOffset = 0.9f;
        @ValidatedFloat.Restrict(min = 0.001f)
        public float headOrbitSpeed = 0.075f;
        @ValidatedFloat.Restrict(min = 0.1f)
        public float headScale = 0.65f;
        @ValidatedFloat.Restrict(min = 0.1f)
        public float headTurnSpeedDegreesPerTick = 12f;

    }
}
