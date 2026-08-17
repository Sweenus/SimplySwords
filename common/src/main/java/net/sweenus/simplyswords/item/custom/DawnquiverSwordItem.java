package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.DawnquiverAbilityManager;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityChannelManager;

import java.util.List;

public final class DawnquiverSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public DawnquiverSwordItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack.isEmpty() || !AwakeningApi.isAbilityUnlocked(stack)
                || stack.getDamage() >= stack.getMaxDamage() - 1
                || user.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            return TypedActionResult.fail(stack);
        }
        if (world instanceof ServerWorld serverWorld
                && !DawnquiverAbilityManager.startDraw(serverWorld, user, stack, hand)) {
            return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        int maxUseTime = getMaxUseTime(stack, user);
        float chargeRatio = chargeRatio(stack, user, remainingUseTicks);
        DawnquiverAbilityManager.tickDraw(serverWorld, user, chargeRatio);

        if (remainingUseTicks % 8 == 0 && remainingUseTicks < maxUseTime - 4) {
            world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_BOW_PULL_BACK_LONG_VERSION_01.get(),
                    user.getSoundCategory(), 0.18F, 0.85F + chargeRatio * 0.55F);
        }

        if (remainingUseTicks <= 1) {
            if (!(user instanceof ServerPlayerEntity serverPlayer)
                    || !PlayerWeaponAbilityChannelManager.finishEarly(serverPlayer, stack)) {
                user.stopUsingItem();
            }
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        if (!HelperMethods.isHolding(stack, user)) {
            DawnquiverAbilityManager.cancel(serverWorld, user.getUuid());
            return;
        }
        float chargeRatio = chargeRatio(stack, user, remainingUseTicks);
        DawnquiverAbilityManager.release(serverWorld, user, stack, chargeRatio);

        int cooldown = chargeRatio < Config.uniqueEffects.dawnquiver.minimumDraw
                ? Math.max(1, Config.uniqueEffects.dawnquiver.cooldown / 4)
                : Math.max(1, Config.uniqueEffects.dawnquiver.cooldown);
        if (user instanceof PlayerEntity player) {
            player.getItemCooldownManager().set(stack.getItem(), cooldown);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && entity instanceof LivingEntity livingEntity) {
            DawnquiverAbilityManager.tickHeldPassive(livingEntity, stack);
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return Math.max(4, Config.uniqueEffects.dawnquiver.drawDuration);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return false;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return false;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.dawnquiver.cooldown);
    }

    private float chargeRatio(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        int maxUseTime = getMaxUseTime(stack, user);
        if (maxUseTime <= 0) {
            return 0.0F;
        }
        return net.minecraft.util.math.MathHelper.clamp(
                1.0F - (float) remainingUseTicks / maxUseTime, 0.0F, 1.0F);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.dawnquiversworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.dawnquiversworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.dawnquiversworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.dawnquiver.cooldown);
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.dawnquiver");
    }

    public static final class EffectSettings extends TooltipSettings {
        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.DAWNQUIVER::get));
        }

        @ValidatedInt.Restrict(min = 1) public int cooldown = 300;
        @ValidatedInt.Restrict(min = 4, max = 200) public int drawDuration = 40;
        @ValidatedDouble.Restrict(min = 0.0, max = 1.0) public double minimumDraw = 0.35;
        @ValidatedDouble.Restrict(min = 0.0, max = 8.0) public double bowDistance = 1.0;
        @ValidatedDouble.Restrict(min = 0.1, max = 3.0) public double bowScale = 0.75;
        @ValidatedDouble.Restrict(min = 1.0, max = 64.0) public double activeRange = 32.0;
        @ValidatedDouble.Restrict(min = 0.1, max = 4.0) public double arrowSpeed = 1.35;
        @ValidatedDouble.Restrict(min = 0.0, max = 45.0) public double homingStrength = 9.0;
        @ValidatedDouble.Restrict(min = 0.0, max = 8.0) public double impactRadius = 2.0;
        @ValidatedDouble.Restrict(min = 0.0) public double initialDamageScaling = 0.4;
        @ValidatedDouble.Restrict(min = 0.0) public double initialSpellScaling = 0.8;
        @ValidatedDouble.Restrict(min = 0.0) public double maxChargeDamageScaling = 2.6;
        @ValidatedDouble.Restrict(min = 0.0) public double maxChargeSpellScaling = 5.0;
        @ValidatedInt.Restrict(min = 1) public int passiveInterval = 100;
        @ValidatedInt.Restrict(min = 1) public int passiveLockout = 80;
        @ValidatedDouble.Restrict(min = 1.0, max = 64.0) public double passiveRange = 24.0;
        @ValidatedDouble.Restrict(min = 0.0) public double passiveDamageScaling = 0.5;
        @ValidatedDouble.Restrict(min = 0.0) public double passiveSpellScaling = 1.0;
        @ValidatedDouble.Restrict(min = 0.1, max = 2.0) public double passiveArrowScale = 0.45;
        @ValidatedDouble.Restrict(min = 0.1, max = 2.0) public double passiveBowScale = 0.45;
    }
}
