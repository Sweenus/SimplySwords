package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryTuning;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.WraithfangEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;
import net.sweenus.simplyswords.world.WraithfangAbilityManager;
import net.sweenus.simplyswords.world.WraithfangTuningSnapshot;

import java.util.List;

public class WraithfangSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public WraithfangSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (attacker.getWorld().isClient()) return super.postHit(stack, target, attacker);
        HelperMethods.playHitSounds(attacker, target);

        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            itemStack = user.getStackInHand(hand);
            LivingEntity target = null;
            if (user instanceof ServerPlayerEntity serverPlayer) {
                Entity aimed = HelperMethods.getTargetedEntity(serverPlayer, 18);
                if (aimed instanceof LivingEntity living && HelperMethods.checkAbilityTarget(living, user)) {
                    target = living;
                }
            }
            UniqueAbilityExecution execution = beginThrow((ServerWorld) world,
                    itemStack, user, target, hand, 1.5, 1, null);
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(execution);
            AbyssalSpectralMasteryTuning abilityTuning = AbyssalSpectralMasteryAbilities.tuning(execution);
            WraithfangTuningSnapshot tuning = WraithfangTuningSnapshot.from(execution);
            double alternationMultiplier = WraithfangAbilityManager.recordThrow(
                    (ServerWorld) world, user, itemStack, tuning);
            double[] damage = HelperMethods.getAttackFromSlot(user, itemStack, user.getActiveHand());
            WraithfangEntity wraithfangEntity = new WraithfangEntity(world, user, itemStack.copy() );
            wraithfangEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F,
                    (float) abilityTuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED, 1.5), 1.0F);
            wraithfangEntity.setYaw(user.getYaw());
            wraithfangEntity.setPitch(user.getPitch());
            wraithfangEntity.primaryBaseDamage = HelperMethods.abilityScaledDamageFromValue(
                    SpellScalingProfile.SOUL, user, itemStack, (float) damage[0],
                    Config.uniqueEffects.wraithfang.spellScaling)
                    * (float) tuning.projectileDamageMultiplier();
            wraithfangEntity.setAbilityExecution(execution);
            wraithfangEntity.configureMastery(target, alternationMultiplier, false);
            if (hand == Hand.OFF_HAND)
                wraithfangEntity.offhandThrow = true;
            wraithfangEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(wraithfangEntity);
            SimplySwordsAPI.setWeaponCooldown(user, itemStack, tuning.cooldownTicks());

            if (!user.getAbilities().creativeMode) {
                user.setStackInHand(hand, ItemStack.EMPTY);
            }
            world.playSound(wraithfangEntity, user.getBlockPos(), SoundRegistry.DARK_SWORD_SPELL.get(),
                    user.getSoundCategory(), 0.1f, 1.0f);
            world.playSound(wraithfangEntity, user.getBlockPos(), SoundRegistry.DISTORTION_ARC_03.get(),
                    user.getSoundCategory(), 0.1f, 1.0f);
        }

        user.swingHand(hand);
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        LivingEntity actor = context.actor();
        UniqueAbilityExecution execution = beginThrow(context.world(), context.stack(), actor,
                context.target(), context.hand(), 1.65, 0, context);
        AbyssalSpectralMasteryTuning abilityTuning = AbyssalSpectralMasteryAbilities.tuning(execution);
        WraithfangTuningSnapshot tuning = WraithfangTuningSnapshot.from(execution);
        double alternationMultiplier = WraithfangAbilityManager.recordThrow(
                context.world(), actor, context.stack(), tuning);
        WraithfangEntity wraithfangEntity = new WraithfangEntity(context.world(), actor, context.stack().copy());
        Vec3d direction = LivingEntityAbilityMovementManager.getLobbedTargetDirection(actor, context.target());
        wraithfangEntity.setVelocity(direction.x, direction.y, direction.z,
                (float) abilityTuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED, 1.65), 1.0F);
        wraithfangEntity.setYaw(actor.getYaw());
        wraithfangEntity.setPitch(actor.getPitch());
        float weaponDamage = (float) Math.max(1.0, HelperMethods.getAttackFromStack(
                context.stack(), net.minecraft.component.type.AttributeModifierSlot.MAINHAND));
        wraithfangEntity.primaryBaseDamage = HelperMethods.abilityScaledDamageFromValue(
                SpellScalingProfile.SOUL, actor, context.stack(), weaponDamage,
                Config.uniqueEffects.wraithfang.spellScaling)
                * (float) tuning.projectileDamageMultiplier();
        wraithfangEntity.setAbilityExecution(execution);
        wraithfangEntity.configureMastery(context.target(), alternationMultiplier, true);
        wraithfangEntity.setPos(actor.getX(), actor.getEyeY() - 0.5, actor.getZ());
        wraithfangEntity.markNonReturning(tuning.projectileLifetimeTicks());
        context.world().spawnEntity(wraithfangEntity);
        context.world().playSound(wraithfangEntity, actor.getBlockPos(), SoundRegistry.DARK_SWORD_SPELL.get(), actor.getSoundCategory(), 0.1f, 1.0f);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return 20;
    }

    private static UniqueAbilityExecution beginThrow(ServerWorld world,
                                                      ItemStack stack, LivingEntity actor,
                                                      LivingEntity target, Hand hand,
                                                      double speed, int loyalty,
                                                      WeaponAbilityContext activeContext) {
        return UniqueAbilityApi.begin(AbyssalSpectralMasteryAbilities.WRAITHFANG_THROW,
                activeContext == null ? UniqueAbilityContext.passive(world, stack, actor, target, hand)
                        : UniqueAbilityContext.active(activeContext), builder -> builder
                        .set(AbyssalSpectralMasteryAbilities.COOLDOWN_TICKS, 20)
                        .set(AbyssalSpectralMasteryAbilities.TUNING, AbyssalSpectralMasteryTuning.EMPTY
                                .with(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_TICKS, 20)
                                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED, speed)
                                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1)
                                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_LIFETIME, 80)
                                .with(AbyssalSpectralMasteryTuning.Setting.LOYALTY, loyalty)
                                .with(AbyssalSpectralMasteryTuning.Setting.FLIGHT_DAMAGE_PER_TICK, .5)
                                .with(AbyssalSpectralMasteryTuning.Setting.FLIGHT_DAMAGE_CAP_TICKS, 1200)
                                .with(AbyssalSpectralMasteryTuning.Setting.DASH_TARGET_RANGE, 14.4)
                                .with(AbyssalSpectralMasteryTuning.Setting.DASH_SPEED, 1.35)
                                .with(AbyssalSpectralMasteryTuning.Setting.DASH_DURATION_TICKS, 10)
                                .with(AbyssalSpectralMasteryTuning.Setting.HASTE_DURATION_TICKS,
                                        Config.uniqueEffects.wraithfang.duration)
                                .with(AbyssalSpectralMasteryTuning.Setting.HASTE_AMPLIFIER,
                                        Config.uniqueEffects.wraithfang.hasteAmplifier)));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.OMINOUS_SPAWNING, ParticleTypes.OMINOUS_SPAWNING, ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wraithfangsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.wraithfangsworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.wraithfangsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wraithfangsworditem.tooltip5").setStyle(Styles.TEXT));
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "soul");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WRAITHFANG::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int hasteAmplifier = 1;
        @ValidatedInt.Restrict(min = 10)
        public int duration = 80;
        @ValidatedFloat.Restrict(min = 0)
        public float spellScaling = 3.10f;
    }
}
