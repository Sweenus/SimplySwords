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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.BramblethornAbilityManager;

import java.util.List;

public class BrambleSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public BrambleSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (AwakeningApi.isAbilityUnlocked(stack) && !attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            BramblethornAbilityManager.onWeaponHit(stack, target, attacker);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack.isEmpty()
                || !AwakeningApi.isAbilityUnlocked(stack)
                || stack.getDamage() >= stack.getMaxDamage() - 1) {
            return TypedActionResult.fail(stack);
        }
        if (world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity player) {
            LivingEntity target = BramblethornAbilityManager.findPlayerTarget(player);
            WeaponAbilityContext context = WeaponAbilityContext.of(
                    serverWorld, stack, player, player, target, hand,
                    WeaponAbilityActivationSource.PLAYER);
            if (!SimplySwordsAPI.tryActivateWeaponAbility(context)) {
                return TypedActionResult.fail(stack);
            }
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return BramblethornAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return BramblethornAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.bramblethorn.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SPORE_BLOSSOM_AIR,
                ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip5").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.bramblethorn.cooldown);

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "nature");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.BRAMBLETHORN::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 240;
        @ValidatedDouble.Restrict(min = 1.0)
        public double targetRange = 16.0;
        @ValidatedDouble.Restrict(min = 0.5)
        public double captureRadius = 6.0;
        @ValidatedInt.Restrict(min = 1, max = 12)
        public int maximumTargets = 6;
        @ValidatedInt.Restrict(min = 1)
        public int rootTravelTicks = 12;
        @ValidatedInt.Restrict(min = 1)
        public int bindingDuration = 60;
        @ValidatedDouble.Restrict(min = 0.0)
        public double pullStrength = 0.22;
        @ValidatedFloat.Restrict(min = 0.0F, max = 1.0F)
        public float sharedDamageRatio = 0.35F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float slamDamageScaling = 0.85F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float slamSpellScaling = 3.9508F;
        @ValidatedFloat.Restrict(min = 0.1F)
        public float maximumLiftWidth = 1.5F;
        @ValidatedFloat.Restrict(min = 0.1F)
        public float maximumLiftHeight = 3.0F;
        @ValidatedDouble.Restrict(min = 0.0, max = 1.0)
        public double maximumLiftResistance = 0.7;
        @ValidatedDouble.Restrict(min = 0.1)
        public double liftClearance = 1.25;
        @ValidatedDouble.Restrict(min = 0.0)
        public double liftForce = 0.58;
        @ValidatedDouble.Restrict(min = 0.1)
        public double slamVelocity = 1.15;
        @ValidatedInt.Restrict(min = 1)
        public int huntMemoryDuration = 60;
        @ValidatedInt.Restrict(min = 0)
        public int huntCooldown = 8;
        @ValidatedDouble.Restrict(min = 0.5)
        public double huntMaximumRange = 10.0;
        @ValidatedInt.Restrict(min = 1)
        public int huntTravelTicks = 8;
        @ValidatedDouble.Restrict(min = 0.1)
        public double huntHitRadius = 0.7;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float huntDamageScaling = 0.30F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float huntSpellScaling = 1.3944F;
        @ValidatedInt.Restrict(min = 0)
        public int huntSlowDuration = 30;
        @ValidatedInt.Restrict(min = 0, max = 4)
        public int huntSlowAmplifier = 0;

    }
}
