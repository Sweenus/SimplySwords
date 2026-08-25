package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.ability.Phase2AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase2UniqueAbilities;
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
            UniqueAbilityExecution execution = beginThrow((net.minecraft.server.world.ServerWorld) world,
                    itemStack, user, null, hand, 1.5, 1);
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(execution);
            Phase2AbilityTuning tuning = Phase2UniqueAbilities.tuning(execution);
            double[] damage = HelperMethods.getAttackFromSlot(user, itemStack, user.getActiveHand());
            WraithfangEntity wraithfangEntity = new WraithfangEntity(world, user, itemStack.copy() );
            wraithfangEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F,
                    (float) tuning.get(Phase2AbilityTuning.Setting.PROJECTILE_SPEED, 1.5), 1.0F);
            wraithfangEntity.setYaw(user.getYaw());
            wraithfangEntity.setPitch(user.getPitch());
            wraithfangEntity.primaryBaseDamage = HelperMethods.abilityScaledDamageFromValue(
                    SpellScalingProfile.SOUL, user, itemStack, (float) damage[0],
                    Config.uniqueEffects.wraithfang.spellScaling)
                    * (float) tuning.get(Phase2AbilityTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1);
            wraithfangEntity.hasLoyalty = tuning.integer(Phase2AbilityTuning.Setting.LOYALTY, 1);
            wraithfangEntity.setAbilityExecution(execution);
            if (hand == Hand.OFF_HAND)
                wraithfangEntity.offhandThrow = true;
            wraithfangEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(wraithfangEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
            world.playSound(wraithfangEntity, user.getBlockPos(), SoundRegistry.DARK_SWORD_SPELL.get(),
                    user.getSoundCategory(), 0.1f, 1.0f);
            world.playSound(wraithfangEntity, user.getBlockPos(), SoundRegistry.DISTORTION_ARC_03.get(),
                    user.getSoundCategory(), 0.1f, 1.0f);
        }

        user.swingHand(hand);

        SimplySwordsAPI.setWeaponCooldown(user, itemStack, 1);
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        LivingEntity actor = context.actor();
        UniqueAbilityExecution execution = beginThrow(context.world(), context.stack(), actor,
                context.target(), context.hand(), 1.65, 0);
        Phase2AbilityTuning tuning = Phase2UniqueAbilities.tuning(execution);
        WraithfangEntity wraithfangEntity = new WraithfangEntity(context.world(), actor, context.stack().copy());
        Vec3d direction = LivingEntityAbilityMovementManager.getLobbedTargetDirection(actor, context.target());
        wraithfangEntity.setVelocity(direction.x, direction.y, direction.z,
                (float) tuning.get(Phase2AbilityTuning.Setting.PROJECTILE_SPEED, 1.65), 1.0F);
        wraithfangEntity.setYaw(actor.getYaw());
        wraithfangEntity.setPitch(actor.getPitch());
        float weaponDamage = (float) Math.max(1.0, HelperMethods.getAttackFromStack(
                context.stack(), net.minecraft.component.type.AttributeModifierSlot.MAINHAND));
        wraithfangEntity.primaryBaseDamage = HelperMethods.abilityScaledDamageFromValue(
                SpellScalingProfile.SOUL, actor, context.stack(), weaponDamage,
                Config.uniqueEffects.wraithfang.spellScaling)
                * (float) tuning.get(Phase2AbilityTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1)
                * (float) tuning.get(Phase2AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
        wraithfangEntity.hasLoyalty = tuning.integer(Phase2AbilityTuning.Setting.LOYALTY, 0);
        wraithfangEntity.setAbilityExecution(execution);
        wraithfangEntity.setPos(actor.getX(), actor.getEyeY() - 0.5, actor.getZ());
        wraithfangEntity.markNonReturning(tuning.integer(Phase2AbilityTuning.Setting.PROJECTILE_LIFETIME, 80));
        context.world().spawnEntity(wraithfangEntity);
        LivingEntityAbilityMovementManager.dashTowardTarget(context.world(), actor, context.target(),
                tuning.get(Phase2AbilityTuning.Setting.DASH_SPEED, 1.35),
                tuning.integer(Phase2AbilityTuning.Setting.DASH_DURATION_TICKS, 10));
        int hasteDuration = tuning.integer(Phase2AbilityTuning.Setting.HASTE_DURATION_TICKS, 80);
        if (hasteDuration > 0) actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,
                hasteDuration, tuning.integer(Phase2AbilityTuning.Setting.HASTE_AMPLIFIER, 1)), actor);
        context.world().playSound(wraithfangEntity, actor.getBlockPos(), SoundRegistry.DARK_SWORD_SPELL.get(), actor.getSoundCategory(), 0.1f, 1.0f);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return 20;
    }

    private static UniqueAbilityExecution beginThrow(net.minecraft.server.world.ServerWorld world,
                                                      ItemStack stack, LivingEntity actor,
                                                      LivingEntity target, Hand hand,
                                                      double speed, int loyalty) {
        return UniqueAbilityApi.begin(Phase2UniqueAbilities.WRAITHFANG_THROW,
                UniqueAbilityContext.passive(world, stack, actor, target, hand), builder -> builder
                        .set(Phase2UniqueAbilities.COOLDOWN_TICKS, 20)
                        .set(Phase2UniqueAbilities.TUNING, Phase2AbilityTuning.EMPTY
                                .with(Phase2AbilityTuning.Setting.COOLDOWN_TICKS, 20)
                                .with(Phase2AbilityTuning.Setting.PROJECTILE_SPEED, speed)
                                .with(Phase2AbilityTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1)
                                .with(Phase2AbilityTuning.Setting.PROJECTILE_LIFETIME, 80)
                                .with(Phase2AbilityTuning.Setting.LOYALTY, loyalty)
                                .with(Phase2AbilityTuning.Setting.DASH_SPEED, 1.35)
                                .with(Phase2AbilityTuning.Setting.DASH_DURATION_TICKS, 10)
                                .with(Phase2AbilityTuning.Setting.HASTE_DURATION_TICKS, 80)
                                .with(Phase2AbilityTuning.Setting.HASTE_AMPLIFIER, 1)));
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
