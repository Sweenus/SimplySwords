package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.ability.Phase10AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase10UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DawnquiverArrowEntity;
import net.sweenus.simplyswords.entity.DawnquiverBowVisualEntity;
import net.sweenus.simplyswords.entity.DawnquiverImpactVisualEntity;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.item.custom.DawnquiverSwordItem;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class DawnquiverAbilityManager {

    private static final DustColorTransitionParticleEffect DAWN_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(1.0F, 0.88F, 0.42F),
                    new Vector3f(1.0F, 1.0F, 0.92F), 1.15F);

    private static final int PRUNE_INTERVAL = 200;
    private static final int BOW_TRAILING_TICKS = 8;
    private static final int BOW_HOLD_EXTENSION = 80;

    private static final Map<ServerWorld, Map<UUID, ActiveDraw>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> LAST_PASSIVE_TICK = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> PASSIVE_LOCKOUT = new HashMap<>();
    private static final Map<ServerWorld, List<ScheduledShot>> SCHEDULED_SHOTS = new HashMap<>();
    private static final Map<ServerWorld, List<ScheduledBloom>> SCHEDULED_BLOOMS = new HashMap<>();
    private static final Map<ItemStack, Integer> CHORUS_CAP = new WeakHashMap<>();
    private static final Map<ItemStack, Phase10AbilityTuning> CHORUS_TUNING = new WeakHashMap<>();
    private static final Map<ItemStack, Phase10AbilityTuning> LESSER_TUNING = new WeakHashMap<>();
    private static final Map<UUID, Phase10AbilityTuning> DRAW_TUNING = new HashMap<>();

    private DawnquiverAbilityManager() {
    }

    public static int getChorus(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(ItemsRegistry.DAWNQUIVER.get())) {
            return 0;
        }
        return Math.min(CHORUS_CAP.getOrDefault(stack, Math.max(1, Config.uniqueEffects.dawnquiver.maxChorus)),
                Math.max(0, stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(),
                        StoredChargeComponent.DEFAULT).charge()));
    }

    public static float maximumDrawProgress(ItemStack stack) {
        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        float minimum = MathHelper.clamp((float) settings.minimumDraw, 0.0F, 1.0F);
        float piercing = MathHelper.clamp((float) settings.piercingThreshold, minimum, 1.0F);
        float full = MathHelper.clamp((float) settings.fullDrawThreshold, piercing, 1.0F);
        int chorus = getChorus(stack);
        if (chorus <= 0) {
            return minimum;
        }
        if (chorus == 1) {
            return piercing;
        }
        return chorus == 2 ? full : 1.0F;
    }

    public static float capDrawProgress(ItemStack stack, float progress) {
        return MathHelper.clamp(progress, 0.0F, maximumDrawProgress(stack));
    }

    public static int drawDuration(ServerWorld world, LivingEntity owner) {
        ActiveDraw draw = activeDraw(world, owner.getUuid());
        return Math.max(4, draw == null ? Config.uniqueEffects.dawnquiver.drawDuration
                : draw.tuning.integer(Phase10AbilityTuning.Setting.WINDUP_TICKS,
                Config.uniqueEffects.dawnquiver.drawDuration));
    }

    public static int affordableDrawTier(ItemStack stack, float progress) {
        int maximumTier = Math.min(2, getChorus(stack) - 1);
        return Math.min(drawTier(progress, Config.uniqueEffects.dawnquiver), maximumTier);
    }

    public static boolean startDraw(ServerWorld world, LivingEntity owner, ItemStack stack, Hand hand) {
        if (world == null || owner == null || stack == null || stack.isEmpty()
                || hand == null || !stack.isOf(ItemsRegistry.DAWNQUIVER.get())) {
            return false;
        }
        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        cancel(world, owner.getUuid());
        UniqueAbilityExecution execution = Phase10CombatManager.beginDirectActive(
                Phase10UniqueAbilities.DAWN_DRAW, world, owner, stack, hand, settings.cooldown);
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        DRAW_TUNING.put(owner.getUuid(), tuning);

        int seed = owner.getRandom().nextInt(4096);
        int lifetime = Math.max(20, tuning.integer(Phase10AbilityTuning.Setting.WINDUP_TICKS,
                settings.drawDuration)) + BOW_TRAILING_TICKS;
        LivingEntity target = findActiveTarget(world, owner, settings);
        Vec3d aimDirection = activeAimDirection(owner, target);
        Vec3d anchor = bowAnchor(owner, aimDirection, settings.bowDistance);
        DawnquiverBowVisualEntity bow = new DawnquiverBowVisualEntity(world, owner, hand,
                anchor.x, anchor.y, anchor.z, directionYaw(aimDirection), directionPitch(aimDirection),
                lifetime, 1.0F, seed, DawnquiverBowVisualEntity.MODE_ACTIVE);
        bow.setTarget(target);
        if (!world.spawnEntity(bow)) {
            bow.discard();
            UniqueAbilityApi.cancel(execution);
            return false;
        }

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>())
                .put(owner.getUuid(), new ActiveDraw(bow.getUuid(), hand, tuning, execution));

        world.spawnParticles(DAWN_DUST, anchor.x, anchor.y, anchor.z, 30, 0.5, 0.5, 0.5, 0.05);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_VOICE_15.get(),
                SoundCategory.PLAYERS, 0.45F, 1.1F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_CHARGE_LONG_VERSION.get(),
                SoundCategory.PLAYERS, 0.5F, 1.0F);
        return true;
    }

    public static void tickDraw(ServerWorld world, LivingEntity owner, float drawProgress) {
        ActiveDraw drawState = activeDraw(world, owner.getUuid());
        Phase10AbilityTuning tuning = drawState == null ? Phase10AbilityTuning.EMPTY : drawState.tuning;
        DawnquiverBowVisualEntity bow = resolveBow(world, owner.getUuid());
        if (bow == null || drawState == null) {
            return;
        }
        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        LivingEntity target = findActiveTarget(world, owner, settings);
        Vec3d aimDirection = activeAimDirection(owner, target);
        Vec3d anchor = bowAnchor(owner, aimDirection, settings.bowDistance);
        bow.refreshPositionAndAngles(anchor.x, anchor.y, anchor.z,
                directionYaw(aimDirection), directionPitch(aimDirection));
        bow.setTarget(target);
        bow.setDrawProgress(drawProgress);
        if (bow.getLifetime() - bow.age < BOW_HOLD_EXTENSION / 2) {
            bow.setLifetime(bow.age + BOW_HOLD_EXTENSION);
        }

        ItemStack stack = owner.getStackInHand(drawState.hand);
        int tier = affordableDrawTier(stack, drawProgress);
        if (tier > drawState.lastTier) {
            playTierCue(world, owner, tier);
            drawState.lastTier = tier;
        }
        boolean atCap = drawProgress >= maximumDrawProgress(stack) - 1.0E-4F;
        int particleInterval = atCap ? 12 : 6;
        if (bow.age % particleInterval == 0) {
            world.spawnParticles(DAWN_DUST, anchor.x, anchor.y, anchor.z,
                    Math.round(2 + 6 * drawProgress), 0.3, 0.3, 0.3, 0.02);
        }
    }

    public static int release(ServerWorld world, LivingEntity owner, ItemStack stack, float chargeRatio) {
        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        chargeRatio = capDrawProgress(stack, chargeRatio);
        ActiveDraw drawState = activeDraw(world, owner.getUuid());
        Phase10AbilityTuning tuning = drawState == null ? Phase10AbilityTuning.EMPTY : drawState.tuning;
        Hand hand = drawState == null ? heldHand(owner, stack) : drawState.hand;
        DawnquiverBowVisualEntity bow = resolveBow(world, owner.getUuid());
        LivingEntity target = findActiveTarget(world, owner, settings);
        Vec3d aimDirection = activeAimDirection(owner, target);
        Vec3d origin = bowAnchor(owner, aimDirection, settings.bowDistance);
        if (bow != null) {
            bow.refreshPositionAndAngles(origin.x, origin.y, origin.z,
                    directionYaw(aimDirection), directionPitch(aimDirection));
            bow.setTarget(target);
        }
        clear(world, owner.getUuid());

        float minimumDraw = MathHelper.clamp((float) tuning.get(
                Phase10AbilityTuning.Setting.HEALTH_THRESHOLD, settings.minimumDraw), 0.0F, 1.0F);
        if (chargeRatio < minimumDraw) {
            if (bow != null) {
                bow.discard();
            }
            world.spawnParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z,
                    8, 0.25, 0.25, 0.25, 0.01);
            world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_SHOOT_MISS_02.get(),
                    SoundCategory.PLAYERS, 0.35F, 1.15F);
            if (drawState != null) Phase10CombatManager.finish(drawState.execution, 0);
            return Math.max(1, settings.cooldown / 4);
        }

        int tier = affordableDrawTier(stack, chargeRatio);
        int chorus = getChorus(stack);
        Phase10AbilityTuning chorusTuning = CHORUS_TUNING.getOrDefault(stack, Phase10AbilityTuning.EMPTY);
        boolean heaven = tuning.flag(1 << 25);
        int stackCost = heaven ? chorus : Math.max(0, tier + 1);
        boolean empowered = heaven ? chorus >= 2 : tier >= 0 && chorus >= stackCost;
        if (heaven && !empowered) {
            if (bow != null) bow.discard();
            if (drawState != null) Phase10CombatManager.finish(drawState.execution, 0);
            return Math.max(1, settings.cooldown / 4);
        }
        Vec3d direction = target != null ? aimPoint(target).subtract(origin).normalize() : aimDirection;
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.HEALING, owner, stack,
                (float) MathHelper.lerp(chargeRatio, (float) settings.initialDamageScaling,
                        (float) settings.maxChargeDamageScaling),
                (float) MathHelper.lerp(chargeRatio, (float) settings.initialSpellScaling,
                        (float) settings.maxChargeSpellScaling));
        float lesserDamage = HelperMethods.abilityScaledDamage(SpellScalingProfile.HEALING, owner, stack,
                (float) settings.passiveDamageScaling, (float) settings.passiveSpellScaling);
        damage *= (float) tuning.get(tier >= 2 ? Phase10AbilityTuning.Setting.FINAL_DAMAGE_MULTIPLIER
                : Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
        damage *= (float) chorusTuning.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
        lesserDamage *= (float) tuning.get(Phase10AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1);

        int arrowMode = DawnquiverArrowEntity.MODE_NORMAL;
        int maximumPiercingTargets = 1;
        double retention = 1.0;
        if (heaven || empowered && tier == 1) {
            arrowMode = DawnquiverArrowEntity.MODE_PIERCING;
            maximumPiercingTargets = Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.TARGET_CAP,
                    settings.piercingMaxTargets));
            retention = MathHelper.clamp(tuning.get(Phase10AbilityTuning.Setting.OUTGOING_MULTIPLIER,
                    settings.piercingDamageRetention), 0.0, 1.0);
        } else if (empowered && tier == 2) {
            arrowMode = DawnquiverArrowEntity.MODE_FULL_PRIMARY;
        }
        int arrowCount = Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.COUNT, 1));
        if (arrowCount > 1) arrowMode = DawnquiverArrowEntity.MODE_NORMAL;
        if (arrowMode != DawnquiverArrowEntity.MODE_FULL_PRIMARY) DRAW_TUNING.remove(owner.getUuid());

        DawnquiverArrowEntity arrow = new DawnquiverArrowEntity(world, owner, stack, hand,
                origin, direction, target, damage, lesserDamage,
                settings.arrowSpeed, settings.homingStrength,
                0.7 + chargeRatio * 0.8, tuning.get(Phase10AbilityTuning.Setting.RADIUS,
                AwakeningApi.scaleEffect(stack, settings.impactRadius)),
                72.0, arrowMode, maximumPiercingTargets, retention);
        if (!world.spawnEntity(arrow)) {
            arrow.discard();
            if (bow != null) {
                bow.discard();
            }
            if (drawState != null) Phase10CombatManager.finish(drawState.execution, 0);
            return cooldownForTier(tier, settings);
        }
        if (arrowCount > 1) {
            double spread = Math.toRadians(tuning.get(Phase10AbilityTuning.Setting.ANGLE, 12));
            for (int index = 0; index < arrowCount; index++) {
                float angle = (float) (-spread * .5 + spread * index / (arrowCount - 1));
                if (Math.abs(angle) < 1.0E-4) continue;
                DawnquiverArrowEntity extra = new DawnquiverArrowEntity(world, owner, stack, hand,
                        origin, direction.rotateY(angle), target, damage, lesserDamage,
                        settings.arrowSpeed, settings.homingStrength, 0.7 + chargeRatio * 0.8,
                        tuning.get(Phase10AbilityTuning.Setting.RADIUS,
                                AwakeningApi.scaleEffect(stack, settings.impactRadius)),
                        72.0, DawnquiverArrowEntity.MODE_NORMAL, 1, 1);
                world.spawnEntity(extra);
            }
        }

        if (empowered) {
            if (!chorusTuning.flag(1 << 16)) setChorus(stack, chorus - stackCost);
            if (tier == 0) {
                scheduleQuickVolley(world, owner, stack, hand, target, lesserDamage);
            }
        }

        if (bow != null) {
            bow.markReleased();
            bow.setLifetime(bow.age + BOW_TRAILING_TICKS);
        }

        world.spawnParticles(DAWN_DUST, origin.x, origin.y, origin.z, 36, 0.4, 0.4, 0.4, 0.14);
        world.spawnParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_FLYBY_01.get(),
                SoundCategory.PLAYERS, 0.75F, 0.95F + world.random.nextFloat() * 0.1F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_SHOOT_FLYBY_02.get(),
                SoundCategory.PLAYERS, empowered ? 0.72F : 0.55F, empowered ? 1.12F : 1.0F);
        if (drawState != null) Phase10CombatManager.finish(drawState.execution, 1);
        return tuning.integer(Phase10AbilityTuning.Setting.COOLDOWN_TICKS, cooldownForTier(tier, settings));
    }

    public static void onPassiveArrowHit(ServerWorld world, LivingEntity owner, Hand hand) {
        onChorusHit(world, owner, hand, false);
    }

    private static void onChorusHit(ServerWorld world, LivingEntity owner, Hand hand, boolean fullImpact) {
        ItemStack stack = owner.getStackInHand(hand);
        if (!stack.isOf(ItemsRegistry.DAWNQUIVER.get())
                || !AwakeningApi.isAbilityUnlocked(stack)) {
            return;
        }
        UniqueAbilityExecution execution = Phase10CombatManager.beginPassive(
                Phase10UniqueAbilities.DAWN_CHORUS, world, stack, owner, null);
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        CHORUS_TUNING.put(stack, tuning);
        int maximum = Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.STACK_CAP,
                Config.uniqueEffects.dawnquiver.maxChorus));
        CHORUS_CAP.put(stack, maximum);
        int current = getChorus(stack);
        if (current >= maximum) {
            Phase10CombatManager.finish(execution, 0);
            return;
        }
        Phase10AbilityTuning lesser = LESSER_TUNING.getOrDefault(stack, Phase10AbilityTuning.EMPTY);
        double chance = MathHelper.clamp((tuning.get(Phase10AbilityTuning.Setting.CHANCE,
                Config.uniqueEffects.dawnquiver.passiveChorusChance * 100)
                + Math.max(0, lesser.get(Phase10AbilityTuning.Setting.CHANCE,
                Config.uniqueEffects.dawnquiver.passiveChorusChance * 100)
                - Config.uniqueEffects.dawnquiver.passiveChorusChance * 100)
                + (fullImpact ? tuning.get(Phase10AbilityTuning.Setting.PITY_CHANCE, 0) : 0)) / 100, 0.0, 1.0);
        if (world.random.nextDouble() >= chance) {
            Phase10CombatManager.finish(execution, 0);
            return;
        }
        setChorus(stack, current + 1, maximum);
        if (tuning.has(Phase10AbilityTuning.Setting.ABSORPTION))
            owner.setAbsorptionAmount(Math.min(owner.getAbsorptionAmount()
                    + (float) tuning.get(Phase10AbilityTuning.Setting.ABSORPTION, 4), 4));
        Vec3d position = owner.getPos().add(0.0, owner.getHeight() * 0.78, 0.0);
        world.spawnParticles(DAWN_DUST, position.x, position.y, position.z,
                18, 0.42, 0.36, 0.42, 0.045);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_PULL_BACK_SHORT_VERSION_03.get(),
                owner.getSoundCategory(), 0.42F, 1.05F + current * 0.12F);
        Phase10CombatManager.finish(execution, 1);
    }

    public static void onFullArrowImpact(ServerWorld world, LivingEntity owner, ItemStack stack,
                                         Hand hand, Vec3d center, @Nullable LivingEntity preferredTarget,
                                         float lesserDamage) {
        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        Phase10AbilityTuning tuning = DRAW_TUNING.remove(owner.getUuid());
        if (tuning == null) tuning = Phase10AbilityTuning.EMPTY;
        int formationDelay = Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.DELAY_TICKS,
                settings.convergenceFormationDelay));
        int stagger = Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.INTERVAL_TICKS,
                settings.convergenceFiringStagger));
        onChorusHit(world, owner, hand, true);
        double radius = Math.max(1.0, settings.convergenceRadius);
        Vec3d reference = horizontalDirection(center.subtract(owner.getPos()), owner);
        double baseAngle = Math.atan2(reference.z, reference.x);
        UUID preferredTargetId = preferredTarget == null ? null : preferredTarget.getUuid();

        for (int slot = 0; slot < 3; slot++) {
            double arc = Math.toRadians(-70.0 + slot * 70.0);
            double angle = baseAngle + arc;
            Vec3d origin = center.add(Math.cos(angle) * radius,
                    2.6 + slot * 0.55, Math.sin(angle) * radius);
            Vec3d direction = center.subtract(origin).normalize();
            int lifetime = formationDelay + stagger * slot + BOW_TRAILING_TICKS + 3;
            DawnquiverBowVisualEntity bow = new DawnquiverBowVisualEntity(world, owner, hand,
                    origin.x, origin.y, origin.z, directionYaw(direction), directionPitch(direction),
                    lifetime, (float) Math.max(0.35, settings.passiveBowScale * 1.35),
                    owner.getRandom().nextInt(4096), DawnquiverBowVisualEntity.MODE_FIXED);
            bow.setTarget(preferredTarget);
            bow.setDrawProgress(1.0F);
            if (!world.spawnEntity(bow)) {
                bow.discard();
                continue;
            }
            SCHEDULED_SHOTS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                    new ScheduledShot(owner.getUuid(), preferredTargetId, bow.getUuid(), stack.copy(), hand,
                            origin, center, lesserDamage,
                            world.getTime() + formationDelay + (long) stagger * slot,
                            DawnquiverArrowEntity.MODE_CONVERGENCE, slot == 2));
        }

        world.spawnParticles(DAWN_DUST, center.x, center.y, center.z,
                42, radius * 0.32, 0.65, radius * 0.32, 0.04);
        world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(center),
                SoundRegistry.MAGIC_SHAMANIC_VOICE_20.get(), SoundCategory.PLAYERS, 0.7F, 1.08F);
    }

    public static void cancel(ServerWorld world, UUID ownerUuid) {
        ActiveDraw draw = activeDraw(world, ownerUuid);
        DawnquiverBowVisualEntity bow = resolveBow(world, ownerUuid);
        if (bow != null) {
            bow.discard();
        }
        if (draw != null) UniqueAbilityApi.cancel(draw.execution);
        clear(world, ownerUuid);
    }

    public static void tickHeldPassive(LivingEntity owner, ItemStack stack) {
        if (owner == null || stack == null || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.DAWNQUIVER.get())
                || !AwakeningApi.isAbilityUnlocked(stack)
                || !(owner.getWorld() instanceof ServerWorld world)
                || !owner.isAlive()
                || !HelperMethods.isHolding(stack, owner)) {
            return;
        }

        long now = world.getTime();
        Map<UUID, Long> lastTicks = LAST_PASSIVE_TICK.computeIfAbsent(world, ignored -> new HashMap<>());
        if (lastTicks.getOrDefault(owner.getUuid(), Long.MIN_VALUE) == now) {
            return;
        }
        lastTicks.put(owner.getUuid(), now);

        DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
        UniqueAbilityExecution execution = Phase10CombatManager.beginPassive(
                Phase10UniqueAbilities.DAWN_LESSER, world, stack, owner, null);
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        LESSER_TUNING.put(stack, tuning);
        int interval = Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.INTERVAL_TICKS,
                settings.passiveInterval));
        if (Math.floorMod(owner.age + owner.getId(), interval) != 0) {
            Phase10CombatManager.finish(execution, 0);
            return;
        }
        Map<UUID, Long> lockouts = PASSIVE_LOCKOUT.computeIfAbsent(world, ignored -> new HashMap<>());
        if (now < lockouts.getOrDefault(owner.getUuid(), Long.MIN_VALUE)) {
            Phase10CombatManager.finish(execution, 0);
            return;
        }

        LivingEntity target = findFurthestTarget(world, owner,
                tuning.get(Phase10AbilityTuning.Setting.RANGE, settings.passiveRange));
        if (target == null) {
            Phase10CombatManager.finish(execution, 0);
            return;
        }

        Vec3d anchor = shoulderAnchor(owner);
        int seed = owner.getRandom().nextInt(4096);
        Hand hand = heldHand(owner, stack);
        DawnquiverBowVisualEntity bow = new DawnquiverBowVisualEntity(world, owner, hand,
                anchor.x, anchor.y, anchor.z,
                directionYaw(directionTo(owner, target)), directionPitch(directionTo(owner, target)),
                14, (float) settings.passiveBowScale, seed, DawnquiverBowVisualEntity.MODE_PASSIVE);
        bow.setTarget(target);
        bow.setDrawProgress(1.0F);
        bow.markReleased();
        world.spawnEntity(bow);

        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.HEALING, owner, stack,
                (float) settings.passiveDamageScaling, (float) settings.passiveSpellScaling);
        damage *= (float) tuning.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1)
                * (1 + getChorus(stack) * (float) tuning.get(
                Phase10AbilityTuning.Setting.PER_STACK_MULTIPLIER, 0));
        Vec3d direction = aimPoint(target).subtract(anchor).normalize();
        DawnquiverArrowEntity arrow = new DawnquiverArrowEntity(world, owner, stack, hand,
                anchor, direction, target, damage, 0.0F,
                settings.arrowSpeed * 0.85, settings.homingStrength,
                settings.passiveArrowScale, 0.0, settings.passiveRange * 2.0,
                DawnquiverArrowEntity.MODE_PASSIVE, 1, 1.0);
        world.spawnEntity(arrow);
        if (tuning.has(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS))
            target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.GLOWING,
                    tuning.integer(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS, 60), 0), owner);

        lockouts.put(owner.getUuid(), now + SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                stack, owner, tuning.integer(Phase10AbilityTuning.Setting.LOCKOUT_TICKS,
                        settings.passiveLockout)));
        world.spawnParticles(DAWN_DUST, anchor.x, anchor.y, anchor.z, 10, 0.2, 0.2, 0.2, 0.03);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_PULL_BACK_SHORT_VERSION_01.get(),
                SoundCategory.PLAYERS, 0.3F, 1.25F);
        Phase10CombatManager.finish(execution, 1);
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveDraw> draws = ACTIVE.get(world);
        Map<UUID, Long> lastTicks = LAST_PASSIVE_TICK.get(world);
        Map<UUID, Long> lockouts = PASSIVE_LOCKOUT.get(world);
        List<ScheduledShot> shots = SCHEDULED_SHOTS.get(world);
        List<ScheduledBloom> blooms = SCHEDULED_BLOOMS.get(world);
        return draws != null && !draws.isEmpty()
                || lastTicks != null && !lastTicks.isEmpty()
                || lockouts != null && !lockouts.isEmpty()
                || shots != null && !shots.isEmpty()
                || blooms != null && !blooms.isEmpty();
    }

    public static void tick(ServerWorld world) {
        tickActiveDraws(world);
        tickScheduledShots(world);
        tickScheduledBlooms(world);

        if (world.getTime() % PRUNE_INTERVAL != 0L) {
            return;
        }
        long now = world.getTime();
        prune(LAST_PASSIVE_TICK, world, now, 40L);
        prune(PASSIVE_LOCKOUT, world, now, 1200L);
    }

    private static void tickActiveDraws(ServerWorld world) {
        Map<UUID, ActiveDraw> draws = ACTIVE.get(world);
        if (draws == null || draws.isEmpty()) {
            return;
        }
        draws.entrySet().removeIf(entry -> {
            Entity owner = world.getEntity(entry.getKey());
            ActiveDraw draw = entry.getValue();
            boolean valid = owner instanceof LivingEntity living && living.isAlive()
                    && living.isUsingItem()
                    && living.getActiveHand() == draw.hand
                    && living.getStackInHand(draw.hand).isOf(ItemsRegistry.DAWNQUIVER.get());
            if (!valid) {
                Entity entity = world.getEntity(draw.bowUuid);
                if (entity instanceof DawnquiverBowVisualEntity bow) {
                    bow.discard();
                }
            }
            return !valid;
        });
        if (draws.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static void tickScheduledShots(ServerWorld world) {
        List<ScheduledShot> shots = SCHEDULED_SHOTS.get(world);
        if (shots == null || shots.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<ScheduledShot> iterator = shots.iterator();
        while (iterator.hasNext()) {
            ScheduledShot shot = iterator.next();
            if (now < shot.fireAt) {
                continue;
            }
            LivingEntity owner = resolveLiving(world, shot.ownerId);
            Entity bowEntity = world.getEntity(shot.bowId);
            if (owner == null) {
                if (bowEntity != null) {
                    bowEntity.discard();
                }
                iterator.remove();
                continue;
            }

            DawnquiverSwordItem.EffectSettings settings = Config.uniqueEffects.dawnquiver;
            LivingEntity target = resolveValidTarget(world, owner, shot.targetId);
            if (target == null && shot.mode == DawnquiverArrowEntity.MODE_CONVERGENCE) {
                target = findNearestTarget(world, owner, shot.aimCenter,
                        Math.max(1.0, settings.convergenceRetargetRange));
            }
            Vec3d origin = shot.mode == DawnquiverArrowEntity.MODE_QUICK_CHORUS
                    ? shoulderAnchor(owner) : shot.origin;
            Vec3d direction;
            if (target != null) {
                direction = aimPoint(target).subtract(origin).normalize();
            } else {
                direction = shot.aimCenter.subtract(origin).normalize();
            }

            if (bowEntity instanceof DawnquiverBowVisualEntity bow) {
                bow.setTarget(target);
                bow.markReleased();
                bow.setLifetime(bow.age + BOW_TRAILING_TICKS);
            }
            DawnquiverArrowEntity arrow = new DawnquiverArrowEntity(world, owner, shot.stack, shot.hand,
                    origin, direction, target, shot.damage, 0.0F,
                    settings.arrowSpeed * 0.92, settings.homingStrength,
                    settings.passiveArrowScale, 0.0, settings.passiveRange * 2.0,
                    shot.mode, 1, 1.0);
            world.spawnEntity(arrow);
            world.spawnParticles(DAWN_DUST, origin.x, origin.y, origin.z,
                    18, 0.25, 0.25, 0.25, 0.08);
            world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(origin),
                    SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_FLYBY_03.get(),
                    SoundCategory.PLAYERS, 0.52F, 1.08F + world.random.nextFloat() * 0.16F);

            if (shot.finalBloom) {
                SCHEDULED_BLOOMS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                        new ScheduledBloom(owner.getUuid(), shot.aimCenter, now + 4L));
            }
            iterator.remove();
        }
        if (shots.isEmpty()) {
            SCHEDULED_SHOTS.remove(world);
        }
    }

    private static void tickScheduledBlooms(ServerWorld world) {
        List<ScheduledBloom> blooms = SCHEDULED_BLOOMS.get(world);
        if (blooms == null || blooms.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<ScheduledBloom> iterator = blooms.iterator();
        while (iterator.hasNext()) {
            ScheduledBloom bloom = iterator.next();
            if (now < bloom.fireAt) {
                continue;
            }
            LivingEntity owner = resolveLiving(world, bloom.ownerId);
            if (owner != null) {
                world.spawnParticles(ParticleTypes.FLASH, bloom.center.x, bloom.center.y,
                        bloom.center.z, 1, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticles(DAWN_DUST, bloom.center.x, bloom.center.y,
                        bloom.center.z, 50, 0.9, 0.65, 0.9, 0.11);
                world.spawnEntity(new DawnquiverImpactVisualEntity(world,
                        bloom.center.x, bloom.center.y, bloom.center.z, 1.15F,
                        owner.getRandom().nextInt(4096)));
                world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(bloom.center),
                        SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_03.get(),
                        SoundCategory.PLAYERS, 0.8F, 1.08F);
            }
            iterator.remove();
        }
        if (blooms.isEmpty()) {
            SCHEDULED_BLOOMS.remove(world);
        }
    }

    private static void scheduleQuickVolley(ServerWorld world, LivingEntity owner, ItemStack stack,
                                            Hand hand, @Nullable LivingEntity target, float damage) {
        Vec3d anchor = shoulderAnchor(owner);
        Vec3d direction = target == null ? horizontalOrLook(owner) : directionTo(owner, target);
        DawnquiverBowVisualEntity bow = new DawnquiverBowVisualEntity(world, owner, hand,
                anchor.x, anchor.y, anchor.z, directionYaw(direction), directionPitch(direction),
                14, (float) Config.uniqueEffects.dawnquiver.passiveBowScale,
                owner.getRandom().nextInt(4096), DawnquiverBowVisualEntity.MODE_PASSIVE);
        bow.setTarget(target);
        bow.setDrawProgress(1.0F);
        if (!world.spawnEntity(bow)) {
            bow.discard();
            return;
        }
        Vec3d aimCenter = target == null ? owner.getEyePos().add(direction.multiply(12.0)) : aimPoint(target);
        SCHEDULED_SHOTS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                new ScheduledShot(owner.getUuid(), target == null ? null : target.getUuid(),
                        bow.getUuid(), stack.copy(), hand, anchor, aimCenter, damage,
                        world.getTime() + 3L, DawnquiverArrowEntity.MODE_QUICK_CHORUS, false));
    }

    private static void setChorus(ItemStack stack, int chorus) {
        setChorus(stack, chorus, CHORUS_CAP.getOrDefault(stack,
                Math.max(1, Config.uniqueEffects.dawnquiver.maxChorus)));
    }

    private static void setChorus(ItemStack stack, int chorus, int maximum) {
        stack.set(ComponentTypeRegistry.STORED_CHARGE.get(),
                new StoredChargeComponent(MathHelper.clamp(chorus, 0, maximum)));
    }

    private static int drawTier(float progress, DawnquiverSwordItem.EffectSettings settings) {
        float minimum = MathHelper.clamp((float) settings.minimumDraw, 0.0F, 1.0F);
        if (progress < minimum) {
            return -1;
        }
        float piercing = MathHelper.clamp((float) settings.piercingThreshold, minimum, 1.0F);
        float full = MathHelper.clamp((float) settings.fullDrawThreshold, piercing, 1.0F);
        if (progress >= full) {
            return 2;
        }
        return progress >= piercing ? 1 : 0;
    }

    private static int cooldownForTier(int tier, DawnquiverSwordItem.EffectSettings settings) {
        if (tier >= 2) {
            return Math.max(1, settings.cooldown);
        }
        return tier == 1 ? Math.max(1, settings.piercingCooldown) : Math.max(1, settings.quickCooldown);
    }

    private static void playTierCue(ServerWorld world, LivingEntity owner, int tier) {
        if (tier < 0) {
            return;
        }
        if (tier == 0) {
            world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_PULL_BACK_SHORT_VERSION_01.get(),
                    owner.getSoundCategory(), 0.25F, 1.25F);
        } else if (tier == 1) {
            world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_PULL_BACK_LONG_VERSION_02.get(),
                    owner.getSoundCategory(), 0.42F, 1.18F);
        } else {
            world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_VOICE_04.get(),
                    owner.getSoundCategory(), 0.58F, 1.32F);
            world.playSound(null, owner.getBlockPos(), SoundRegistry.MAGIC_BOW_PULL_BACK_LONG_VERSION_03.get(),
                    owner.getSoundCategory(), 0.55F, 1.05F);
        }
    }

    private static void prune(Map<ServerWorld, Map<UUID, Long>> source, ServerWorld world,
                              long now, long maximumAge) {
        Map<UUID, Long> entries = source.get(world);
        if (entries == null) {
            return;
        }
        entries.values().removeIf(tick -> now - tick > maximumAge);
        if (entries.isEmpty()) {
            source.remove(world);
        }
    }

    @Nullable
    private static LivingEntity findActiveTarget(ServerWorld world, LivingEntity owner,
                                                 DawnquiverSwordItem.EffectSettings settings) {
        Entity aimed = HelperMethods.getTargetedEntity(owner, settings.activeRange);
        if (aimed instanceof LivingEntity living && isValidTarget(living, owner)) {
            return living;
        }
        double range = Math.max(1.0, settings.activeRange);
        double threshold = Math.cos(Math.toRadians(45.0));
        Vec3d look = owner.getRotationVec(1.0F).normalize();
        return world.getEntitiesByClass(LivingEntity.class, owner.getBoundingBox().expand(range),
                        entity -> isValidTarget(entity, owner)
                                && entity.squaredDistanceTo(owner) <= range * range
                                && owner.canSee(entity))
                .stream()
                .filter(entity -> directionTo(owner, entity).dotProduct(look) >= threshold)
                .min(Comparator.comparingDouble(owner::squaredDistanceTo))
                .orElse(null);
    }

    @Nullable
    private static LivingEntity findFurthestTarget(ServerWorld world, LivingEntity owner, double range) {
        double maximum = Math.max(1.0, range);
        return world.getEntitiesByClass(LivingEntity.class, owner.getBoundingBox().expand(maximum),
                        entity -> isValidTarget(entity, owner)
                                && entity.squaredDistanceTo(owner) <= maximum * maximum
                                && owner.canSee(entity))
                .stream()
                .max(Comparator.comparingDouble(owner::squaredDistanceTo))
                .orElse(null);
    }

    @Nullable
    private static LivingEntity findNearestTarget(ServerWorld world, LivingEntity owner,
                                                  Vec3d center, double range) {
        Box box = new Box(center, center).expand(range);
        return world.getEntitiesByClass(LivingEntity.class, box,
                        entity -> isValidTarget(entity, owner)
                                && entity.squaredDistanceTo(center) <= range * range)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(center)))
                .orElse(null);
    }

    @Nullable
    private static LivingEntity resolveValidTarget(ServerWorld world, LivingEntity owner,
                                                   @Nullable UUID targetId) {
        if (targetId == null) {
            return null;
        }
        Entity entity = world.getEntity(targetId);
        return entity instanceof LivingEntity living && isValidTarget(living, owner) ? living : null;
    }

    @Nullable
    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    private static boolean isValidTarget(LivingEntity entity, LivingEntity owner) {
        return entity != owner && entity.isAlive() && !entity.isRemoved()
                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                && HelperMethods.checkAbilityTarget(entity, owner);
    }

    private static Vec3d directionTo(LivingEntity owner, LivingEntity target) {
        Vec3d offset = aimPoint(target).subtract(owner.getEyePos());
        return offset.lengthSquared() < 1.0E-6 ? owner.getRotationVec(1.0F) : offset.normalize();
    }

    private static Vec3d aimPoint(LivingEntity target) {
        return target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
    }

    private static Vec3d horizontalOrLook(LivingEntity owner) {
        Vec3d look = owner.getRotationVec(1.0F);
        return look.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 0.0, 1.0) : look.normalize();
    }

    private static Vec3d horizontalDirection(Vec3d direction, LivingEntity owner) {
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() < 1.0E-6) {
            Vec3d look = owner.getRotationVec(1.0F);
            horizontal = new Vec3d(look.x, 0.0, look.z);
        }
        return horizontal.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 0.0, 1.0) : horizontal.normalize();
    }

    private static Vec3d activeAimDirection(LivingEntity owner, @Nullable LivingEntity target) {
        return target == null ? horizontalOrLook(owner) : directionTo(owner, target);
    }

    private static Vec3d bowAnchor(LivingEntity owner, Vec3d direction, double distance) {
        return owner.getPos()
                .add(0.0, owner.getHeight() * 0.62, 0.0)
                .add(direction.multiply(Math.max(0.0, distance)));
    }

    private static float directionYaw(Vec3d direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    private static float directionPitch(Vec3d direction) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
    }

    private static Vec3d shoulderAnchor(LivingEntity owner) {
        Vec3d look = horizontalDirection(owner.getRotationVec(1.0F), owner);
        Vec3d side = new Vec3d(-look.z, 0.0, look.x).normalize();
        return owner.getEyePos().add(side.multiply(0.55)).add(0.0, 0.25, 0.0);
    }

    private static Hand heldHand(LivingEntity owner, ItemStack stack) {
        return owner.getOffHandStack() == stack ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    @Nullable
    private static ActiveDraw activeDraw(ServerWorld world, UUID ownerUuid) {
        Map<UUID, ActiveDraw> draws = ACTIVE.get(world);
        return draws == null ? null : draws.get(ownerUuid);
    }

    @Nullable
    private static DawnquiverBowVisualEntity resolveBow(ServerWorld world, UUID ownerUuid) {
        ActiveDraw draw = activeDraw(world, ownerUuid);
        if (draw == null) {
            return null;
        }
        Entity entity = world.getEntity(draw.bowUuid);
        return entity instanceof DawnquiverBowVisualEntity bow && !bow.isRemoved() ? bow : null;
    }

    private static void clear(ServerWorld world, UUID ownerUuid) {
        Map<UUID, ActiveDraw> draws = ACTIVE.get(world);
        if (draws == null) {
            return;
        }
        draws.remove(ownerUuid);
        if (draws.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static final class ActiveDraw {
        private final UUID bowUuid;
        private final Hand hand;
        private final Phase10AbilityTuning tuning;
        private final UniqueAbilityExecution execution;
        private int lastTier = -1;

        private ActiveDraw(UUID bowUuid, Hand hand, Phase10AbilityTuning tuning,
                           UniqueAbilityExecution execution) {
            this.bowUuid = bowUuid;
            this.hand = hand;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private record ScheduledShot(UUID ownerId, @Nullable UUID targetId, UUID bowId,
                                 ItemStack stack, Hand hand, Vec3d origin, Vec3d aimCenter,
                                 float damage, long fireAt, int mode, boolean finalBloom) {
    }

    private record ScheduledBloom(UUID ownerId, Vec3d center, long fireAt) {
    }
}
