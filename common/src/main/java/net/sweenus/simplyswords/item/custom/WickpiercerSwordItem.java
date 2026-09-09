package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryTuning;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.WickpiercerEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.item.interfaces.RevivalWeapon;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;
import net.sweenus.simplyswords.world.RevivalCandleVisualManager;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;
import net.sweenus.simplyswords.world.WickpiercerMasteryStateManager;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WickpiercerSwordItem extends UniqueSwordItem implements RevivalWeapon, UniqueWeaponActiveAbility {
    private static final int MAX_PENDING_REVIVALS = 128;
    private static final Map<UUID, UniqueAbilityExecution> PENDING_REVIVALS = new LinkedHashMap<>();

    public WickpiercerSwordItem(ToolMaterial toolMaterial, Settings settings) {
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
        ItemStack mainhand = user.getMainHandStack();
        ItemStack offhand  = user.getOffHandStack();
        boolean dualWielding = mainhand.isOf(this) && offhand.isOf(this);

        world.playSound(null, user.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),
                user.getSoundCategory(), 0.1f, 1.0f);

        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            itemStack = user.getStackInHand(hand);
            UniqueAbilityExecution execution = beginThrow((ServerWorld) world, itemStack, user, null, hand,
                    1.5, Config.uniqueEffects.wickpiercer.cooldown, Config.uniqueEffects.wickpiercer.duration);
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(execution);
            AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(execution);
            int effectDuration = tuning.integer(AbyssalSpectralMasteryTuning.Setting.STACK_DURATION_TICKS,
                    Config.uniqueEffects.wickpiercer.duration) * (dualWielding ? 2 : 1);
            WickpiercerMasteryStateManager.recordWick((ServerWorld) world, user, tuning, effectDuration);
            HelperMethods.incrementStatusEffect(user, EffectRegistry.getReference(EffectRegistry.FRENZY),
                    effectDuration,
                    WickpiercerMasteryStateManager.frenzyGrant((ServerWorld) world, user, tuning, dualWielding),
                    tuning.integer(AbyssalSpectralMasteryTuning.Setting.STACK_CAP, 4));
            WickpiercerEntity wickpiercerEntity = new WickpiercerEntity(world, user, itemStack.copy() );
            wickpiercerEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F,
                    (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED, 1.5), 1.0F);
            wickpiercerEntity.setYaw(user.getYaw());
            wickpiercerEntity.setPitch(user.getPitch()-90);
            wickpiercerEntity.primaryBaseDamage = HelperMethods.abilityScaledDamage("fire", user, itemStack,
                    Config.uniqueEffects.wickpiercer.throwDamageScaling,
                    Config.uniqueEffects.wickpiercer.throwSpellScaling)
                    * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1);
            wickpiercerEntity.hasLoyalty = tuning.integer(AbyssalSpectralMasteryTuning.Setting.LOYALTY, 3);
            wickpiercerEntity.setAbilityExecution(execution);
            if (hand == Hand.OFF_HAND)
                wickpiercerEntity.offhandThrow = true;
            wickpiercerEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(wickpiercerEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }

        user.swingHand(hand);

        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        LivingEntity actor = context.actor();
        UniqueAbilityExecution execution = beginThrow(context.world(), context.stack(), actor, context.target(),
                context.hand(), 1.65, Config.uniqueEffects.wickpiercer.cooldown,
                Config.uniqueEffects.wickpiercer.duration);
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(execution);
        int frenzyDuration = tuning.integer(AbyssalSpectralMasteryTuning.Setting.STACK_DURATION_TICKS,
                Config.uniqueEffects.wickpiercer.duration);
        WickpiercerMasteryStateManager.recordWick(context.world(), actor, tuning, frenzyDuration);
        HelperMethods.incrementStatusEffect(actor, EffectRegistry.getReference(EffectRegistry.FRENZY),
                frenzyDuration, 1,
                tuning.integer(AbyssalSpectralMasteryTuning.Setting.STACK_CAP, 4));
        WickpiercerEntity wickpiercerEntity = new WickpiercerEntity(context.world(), actor, context.stack().copy());
        Vec3d direction = LivingEntityAbilityMovementManager.getLobbedTargetDirection(actor, context.target());
        wickpiercerEntity.setVelocity(direction.x, direction.y, direction.z,
                (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED, 1.65), 1.0F);
        wickpiercerEntity.setYaw(actor.getYaw());
        wickpiercerEntity.setPitch(actor.getPitch() - 90);
        wickpiercerEntity.primaryBaseDamage = HelperMethods.abilityScaledDamage("fire", actor, context.stack(),
                Config.uniqueEffects.wickpiercer.throwDamageScaling,
                Config.uniqueEffects.wickpiercer.throwSpellScaling)
                * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1);
        wickpiercerEntity.hasLoyalty = tuning.integer(AbyssalSpectralMasteryTuning.Setting.LOYALTY, 0);
        wickpiercerEntity.setAbilityExecution(execution);
        wickpiercerEntity.setPos(actor.getX(), actor.getEyeY() - 0.5, actor.getZ());
        wickpiercerEntity.markNonReturning(tuning.integer(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_LIFETIME, 80));
        context.world().spawnEntity(wickpiercerEntity);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.wickpiercer.cooldown);
    }

    private static UniqueAbilityExecution beginThrow(ServerWorld world, ItemStack stack, LivingEntity actor,
                                                      LivingEntity target, Hand hand, double speed,
                                                      int cooldown, int frenzyDuration) {
        return UniqueAbilityApi.begin(AbyssalSpectralMasteryAbilities.WICKPIERCER_THROW,
                UniqueAbilityContext.passive(world, stack, actor, target, hand), builder -> builder
                        .set(AbyssalSpectralMasteryAbilities.COOLDOWN_TICKS, cooldown)
                        .set(AbyssalSpectralMasteryAbilities.TUNING, AbyssalSpectralMasteryTuning.EMPTY
                                .with(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_TICKS, cooldown)
                                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED, speed)
                                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1)
                                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_LIFETIME, 80)
                                .with(AbyssalSpectralMasteryTuning.Setting.LOYALTY, speed <= 1.5 ? 3 : 0)
                                .with(AbyssalSpectralMasteryTuning.Setting.STACK_DURATION_TICKS, frenzyDuration)
                                .with(AbyssalSpectralMasteryTuning.Setting.STACK_CAP, 4)));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.WHITE_ASH,
                ParticleTypes.WHITE_ASH, ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.wickpiercer.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip6", Config.uniqueEffects.wickpiercer.duration / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip7").setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "fire");
    }

    @Override
    public boolean canRevive(LivingEntity entity, ItemStack stack, DamageSource source) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (entity instanceof PlayerEntity player) {
            return !player.getItemCooldownManager().isCoolingDown(this);
        }
        return entity.getWorld() instanceof ServerWorld serverWorld
                && !WeaponAbilityCooldownManager.isCoolingDown(serverWorld, entity, stack);
    }

    @Override
    public void postRevive(LivingEntity entity, ItemStack stack, DamageSource source) {
        int skillCooldown = Config.uniqueEffects.waxweaver.cooldown;
        UniqueAbilityExecution execution = PENDING_REVIVALS.remove(entity.getUuid());
        if (execution == null && entity.getWorld() instanceof ServerWorld serverWorld) {
            execution = beginRevive(serverWorld, entity, stack, skillCooldown);
        }
        AbyssalSpectralMasteryTuning tuning = execution == null
                ? AbyssalSpectralMasteryTuning.EMPTY : AbyssalSpectralMasteryAbilities.tuning(execution);
        int mode = tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0);
        if (entity instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            RevivalCandleVisualManager.activate(serverPlayer, stack);
        }
        double cooldownMultiplier = tuning.get(AbyssalSpectralMasteryTuning.Setting.REVIVE_COOLDOWN_MULTIPLIER, 1);
        int cooldownFloor = cooldownMultiplier < 1
                ? tuning.integer(AbyssalSpectralMasteryTuning.Setting.REVIVE_COOLDOWN_FLOOR_TICKS, 600) : 1;
        SimplySwordsAPI.setWeaponCooldown(entity, stack,
                Math.max(cooldownFloor, (int) Math.round(skillCooldown * cooldownMultiplier)));
        if ((mode & 4096) == 0) {
            HelperMethods.incrementStatusEffect(entity, StatusEffects.RESISTANCE,
                    tuning.integer(AbyssalSpectralMasteryTuning.Setting.STATUS_DURATION_TICKS, 100),
                    tuning.integer(AbyssalSpectralMasteryTuning.Setting.STATUS_AMPLIFIER, 2), 3);
        }
        float absorption = (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.REVIVE_ABSORPTION, 0);
        if (absorption > 0) entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), absorption));
        if (entity.getWorld() instanceof ServerWorld world) {
            if ((mode & 512) != 0) regenerateAllies(world, entity, tuning);
            if ((mode & 4096) != 0) funeralPyre(world, entity, stack, tuning);
            WickpiercerMasteryStateManager.recordRevive(world, entity, tuning);
        }
        if (execution != null) UniqueAbilityApi.finish(execution, execution.definition().id(), 1);

        World world = entity.getWorld();
        world.playSound(null, entity.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                entity.getSoundCategory(), 0.7f, 1.0f);
        world.playSound(null, entity.getBlockPos(), SoundRegistry.SPELL_MISC_02.get(),
                entity.getSoundCategory(), 0.8f, 1.0f);
    }

    @Override
    public float getReviveHealth(LivingEntity entity, ItemStack stack, DamageSource source) {
        if (!(entity.getWorld() instanceof ServerWorld world)) return entity.getMaxHealth();
        UniqueAbilityExecution execution = PENDING_REVIVALS.computeIfAbsent(entity.getUuid(), ignored -> {
            if (PENDING_REVIVALS.size() >= MAX_PENDING_REVIVALS) {
                UUID oldest = PENDING_REVIVALS.keySet().iterator().next();
                UniqueAbilityApi.cancel(PENDING_REVIVALS.remove(oldest));
            }
            return beginRevive(world, entity, stack, Config.uniqueEffects.waxweaver.cooldown);
        });
        double multiplier = AbyssalSpectralMasteryAbilities.tuning(execution).get(
                AbyssalSpectralMasteryTuning.Setting.REVIVE_HEALTH_MULTIPLIER, 1);
        return Math.max(1, entity.getMaxHealth() * (float) multiplier);
    }

    private static UniqueAbilityExecution beginRevive(ServerWorld world, LivingEntity entity,
                                                       ItemStack stack, int cooldown) {
        UniqueAbilityExecution execution = UniqueAbilityApi.preparePassive(AbyssalSpectralMasteryAbilities.WICKPIERCER_REVIVE,
                UniqueAbilityContext.passive(world, stack, entity, null, null), builder -> builder
                        .set(AbyssalSpectralMasteryAbilities.TUNING, AbyssalSpectralMasteryTuning.EMPTY
                                .with(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_TICKS, cooldown)
                                .with(AbyssalSpectralMasteryTuning.Setting.STATUS_DURATION_TICKS, 100)
                                .with(AbyssalSpectralMasteryTuning.Setting.STATUS_AMPLIFIER, 2)
                                .with(AbyssalSpectralMasteryTuning.Setting.REVIVE_HEALTH_MULTIPLIER, 1)
                                .with(AbyssalSpectralMasteryTuning.Setting.REVIVE_COOLDOWN_MULTIPLIER, 1)));
        UniqueAbilityApi.start(execution);
        return execution;
    }

    private static void regenerateAllies(ServerWorld world, LivingEntity owner, AbyssalSpectralMasteryTuning tuning) {
        double radius = tuning.get(AbyssalSpectralMasteryTuning.Setting.RADIUS, 6);
        int cap = tuning.integer(AbyssalSpectralMasteryTuning.Setting.TARGET_CAP, 8);
        var allies = world.getEntitiesByClass(LivingEntity.class,
                new Box(owner.getPos(), owner.getPos()).expand(radius), candidate -> candidate != owner
                        && candidate.isAlive() && !HelperMethods.checkAbilityTarget(candidate, owner));
        allies.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(owner)));
        for (int index = 0; index < Math.min(cap, allies.size()); index++) {
            allies.get(index).addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    StatusEffects.REGENERATION,
                    tuning.integer(AbyssalSpectralMasteryTuning.Setting.ALLY_STATUS_TICKS, 80), 1), owner);
        }
    }

    private static void funeralPyre(ServerWorld world, LivingEntity owner, ItemStack stack,
                                    AbyssalSpectralMasteryTuning tuning) {
        double radius = tuning.get(AbyssalSpectralMasteryTuning.Setting.PYRE_RADIUS, 5);
        int cap = tuning.integer(AbyssalSpectralMasteryTuning.Setting.PYRE_TARGET_CAP, 12);
        float damage = HelperMethods.abilityScaledDamage("fire", owner, stack,
                Config.uniqueEffects.wickpiercer.throwDamageScaling,
                Config.uniqueEffects.wickpiercer.throwSpellScaling)
                * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.PYRE_DAMAGE_MULTIPLIER, 2);
        var targets = world.getEntitiesByClass(LivingEntity.class,
                new Box(owner.getPos(), owner.getPos()).expand(radius), candidate -> candidate != owner
                        && candidate.isAlive() && HelperMethods.checkAbilityTarget(candidate, owner));
        targets.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(owner)));
        for (int index = 0; index < Math.min(cap, targets.size()); index++) {
            SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack,
                    targets.get(index), damage, net.sweenus.simplyswords.api.SpellScalingProfile.FIRE);
        }
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WICKPIERCER::get));
        }

        @ValidatedInt.Restrict(min = 1)
        public int cooldown = 33;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 4.1312f;
        @ValidatedFloat.Restrict(min = 0f)
        public float throwDamageScaling = 0.4f;
        @ValidatedFloat.Restrict(min = 0f)
        public float throwSpellScaling = 2.0656f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 80;

    }
}
