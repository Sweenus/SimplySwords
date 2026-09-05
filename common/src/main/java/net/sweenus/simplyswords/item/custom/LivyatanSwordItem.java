package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.LivyatanEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;
import net.sweenus.simplyswords.world.LivyatanWaveManager;
import net.sweenus.simplyswords.world.LivyatanAbilityManager;
import net.sweenus.simplyswords.world.StormFrostWaterMasteryCombatManager;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityManager;
import net.sweenus.simplyswords.util.WeaponManaCost;

import java.util.List;

public class LivyatanSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public LivyatanSwordItem(ToolMaterial toolMaterial, Settings settings) {
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

    public void onSwing(ItemStack stack, ServerWorld world, LivingEntity user, Hand hand) {
        LivyatanWaveManager.tryFire(world, user, stack);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        boolean reboundInput = PlayerWeaponAbilityManager.shouldSkipDefaultAbilityUse(world, user, hand, stack);
        if (reboundInput && !WeaponManaCost.canAfford(user, stack)) return TypedActionResult.fail(stack);
        TypedActionResult<ItemStack> result = UniqueWeaponActiveAbility.super.startPlayerAbility(world, user, hand);
        if (reboundInput && result.getResult().isAccepted()) WeaponManaCost.spend(user, stack);
        return result;
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        if (context == null || context.world() == null || context.actor() == null || !context.actor().isAlive()
                || context.stack() == null || context.stack().isEmpty()
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1) return false;
        if (context.target() == null) return context.actor() instanceof PlayerEntity;
        return HelperMethods.checkAbilityTarget(context.target(), context.actor())
                && (context.sourcePlayer() == null || context.target() != context.sourcePlayer()
                && HelperMethods.checkFriendlyFire(context.target(), context.sourcePlayer()));
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        UniqueAbilityExecution execution = StormFrostWaterMasteryCombatManager.beginActive(
                StormFrostWaterMasteryAbilities.LIVYATAN_THROW, context, Config.uniqueEffects.livyatan.cooldown);
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryAbilities.tuning(execution);
        UniqueAbilityExecution returnExecution = StormFrostWaterMasteryCombatManager.preparePassive(
                StormFrostWaterMasteryAbilities.LIVYATAN_RETURN, context.world(), context.stack(), context.actor(), context.target());
        StormFrostWaterMasteryTuning returnTuning = StormFrostWaterMasteryAbilities.tuning(returnExecution);
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.publishStartedExecution(execution);
        LivyatanEntity livyatanEntity = createEntity(context.world(), context.actor(),
                context.stack().copy(), tuning, execution, returnTuning, returnExecution,
                execution.cooldownTicks(Config.uniqueEffects.livyatan.cooldown));
        boolean playerThrow = context.actor() instanceof PlayerEntity && !context.isDelegated();
        if (!playerThrow && context.target() != null) {
            Vec3d direction = LivingEntityAbilityMovementManager.getLobbedTargetDirection(context.actor(), context.target());
            livyatanEntity.setVelocity(direction.x, direction.y, direction.z, 1.65F, 1.0F);
            livyatanEntity.markNonReturning(Config.uniqueEffects.livyatan.duration + 80);
        } else {
            livyatanEntity.setVelocity(context.actor(), context.actor().getPitch(), context.actor().getYaw(),
                    0.0F, 1.5F, 1.0F);
        }
        livyatanEntity.setYaw(context.actor().getYaw());
        livyatanEntity.setPitch(context.actor().getPitch());
        if (context.hand() == Hand.OFF_HAND) livyatanEntity.offhandThrow = true;
        livyatanEntity.setPos(context.actor().getX(), context.actor().getEyeY() - 0.5, context.actor().getZ());
        context.world().spawnEntity(livyatanEntity);
        if (playerThrow && context.actor() instanceof PlayerEntity player
                && !player.getAbilities().creativeMode && context.hand() != null) {
            player.setStackInHand(context.hand(), ItemStack.EMPTY);
        }
        context.actor().swingHand(context.hand() == null ? Hand.MAIN_HAND : context.hand());
        return true;
    }

    private static LivyatanEntity createEntity(World world, LivingEntity actor, ItemStack stack,
                                               StormFrostWaterMasteryTuning tuning, UniqueAbilityExecution execution,
                                               StormFrostWaterMasteryTuning returnTuning,
                                               UniqueAbilityExecution returnExecution, int cooldown) {
        LivyatanEntity entity = new LivyatanEntity(world, actor, stack);
        entity.primaryBaseDamage = HelperMethods.abilityScaledDamage("frost", actor, stack,
                Config.uniqueEffects.livyatan.damageScaling, Config.uniqueEffects.livyatan.spellScaling)
                * (float) tuning.get(s("LIVYATAN_THROW_DAMAGE_MULTIPLIER"), 1);
        entity.slownessDuration = Config.uniqueEffects.livyatan.duration;
        entity.primaryReturnDamage = HelperMethods.abilityScaledDamage("frost", actor, stack,
                Config.uniqueEffects.livyatan.returnDamageScaling, Config.uniqueEffects.livyatan.returnSpellScaling);
        entity.primaryReturnDamageRadius = LivyatanAbilityManager.returnRadius(
                Config.uniqueEffects.livyatan.radius, returnTuning);
        entity.setMastery(tuning, execution, returnTuning, returnExecution, cooldown);
        return entity;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.livyatan.cooldown);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "frost_lightning");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.LIVYATAN::get));
        }

        @ValidatedFloat.Restrict(min = 0)
        public int cooldown = 65;
        public float returnDamageScaling = 0.64f;
        @ValidatedFloat.Restrict(min = 0)
        public float returnSpellScaling = 3.32f;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.64f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 100;
        @ValidatedDouble.Restrict(min = 0.5)
        public double radius = 6.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 3.32f;

        @ValidatedFloat.Restrict(min = 0f)
        public float waveDamageScaling = 0.64f;
        @ValidatedInt.Restrict(min = 1)
        public int swingWaveMinimumCooldownTicks = 2;
        @ValidatedDouble.Restrict(min = 0.5)
        public double waveWidthBlocks = 5.0;
        @ValidatedDouble.Restrict(min = 0.25)
        public double waveSegmentThickness = 1.25;
        @ValidatedDouble.Restrict(min = 0.1)
        public double waveStepDistance = 0.8;
        @ValidatedInt.Restrict(min = 1)
        public int waveStepIntervalTicks = 1;
        @ValidatedDouble.Restrict(min = 0.1)
        public double waveForwardStartOffset = 1.2;
        @ValidatedInt.Restrict(min = 1)
        public int waveLengthSteps = 7;
        @ValidatedInt.Restrict(min = 1, max = 64)
        public int waveTargetCap = 8;
        @ValidatedDouble.Restrict(min = 0.0)
        public double waveKnockback = 0.52;
        @ValidatedDouble.Restrict(min = 0.0)
        public double waveKnockUp = 0.14;

        @ValidatedDouble.Restrict(min = 0.0)
        public double returnWavePullStrength = 0.42;
        @ValidatedInt.Restrict(min = 1, max = 64)
        public int returnWaveTargetCap = 16;
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int returnLightningChance = 20;
        @ValidatedFloat.Restrict(min = 0f)
        public float returnLightningDamageScaling = 0.35f;
        @ValidatedFloat.Restrict(min = 0f)
        public float returnLightningSpellScaling = 1.82f;
        @ValidatedDouble.Restrict(min = 1.0)
        public double returnLightningSkyHeight = 12.0;
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }
}
