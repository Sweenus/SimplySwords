package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryTuning;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.MagibladeWardenHeadVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MagibladeAbilityManager {

    private static final String VISUAL_TAG = "simplyswords_magiblade_warden_head";
    private static final int TARGET_RETRY_TICKS = 5;
    private static final int SHOT_PULSE_TICKS = 8;
    private static final double ORBIT_BOB_HEIGHT = 0.1;
    private static final double ORBIT_BOB_SPEED = 0.16;
    private static final double SONIC_BEAM_WIDTH = 0.55;

    private static final Map<ServerWorld, Map<UUID, ChargeState>> ACTIVE_CHARGES = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, HeadState>> ACTIVE_HEADS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> LAST_PASSIVE_TICKS = new HashMap<>();
    private static final Map<UUID, RepulsionState> REPULSION_STATES = new HashMap<>();

    private MagibladeAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.MAGIBLADE.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || hasCharge(context.world(), context.actor().getUuid())
                || !isHeldInActivationHand(context.actor(), context.hand())) {
            return false;
        }

        if (context.actor() instanceof PlayerEntity) {
            return true;
        }
        return isValidEnemy(context.world(), context.actor(), context.sourcePlayer(), context.target());
    }

    public static float countertoneMultiplier(ServerWorld world, LivingEntity actor, LivingEntity target) {
        RepulsionState passive = REPULSION_STATES.get(actor.getUuid());
        if (passive == null || passive.countertoneTarget == null
                || !passive.countertoneTarget.equals(target.getUuid())
                || world.getTime() > passive.countertoneUntil) {
            return 1.0F;
        }
        passive.countertoneTarget = null;
        return (float) passive.tuning.get(ArcaneCosmicMasteryTuning.Setting.OUTGOING_MULTIPLIER, 1.15);
    }

    public static ArcaneCosmicMasteryTuning repulsionBase(int frequency, int chance, double radius) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, frequency)
                .with(ArcaneCosmicMasteryTuning.Setting.CHANCE, chance)
                .with(ArcaneCosmicMasteryTuning.Setting.RADIUS, radius)
                .with(ArcaneCosmicMasteryTuning.Setting.KNOCKBACK, 1)
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, .6);
    }

    public static ArcaneCosmicMasteryTuning wardenBase(int chargeDuration, int summonDuration,
                                                 double orbitRadius, double sonicRange) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.WINDUP_TICKS, chargeDuration)
                .with(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, summonDuration)
                .with(ArcaneCosmicMasteryTuning.Setting.RADIUS, orbitRadius)
                .with(ArcaneCosmicMasteryTuning.Setting.RANGE, sonicRange)
                .with(ArcaneCosmicMasteryTuning.Setting.SPEED, 1)
                .with(ArcaneCosmicMasteryTuning.Setting.COUNT, 1)
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    public static ArcaneCosmicMasteryTuning judgmentBase(int sonicInterval, int sonicCharge, double beamWidth) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, sonicInterval)
                .with(ArcaneCosmicMasteryTuning.Setting.WINDUP_TICKS, sonicCharge)
                .with(ArcaneCosmicMasteryTuning.Setting.SECONDARY_INTERVAL_TICKS, sonicCharge)
                .with(ArcaneCosmicMasteryTuning.Setting.WIDTH, beamWidth)
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    private static ArcaneCosmicMasteryTuning repulsionBase() {
        return repulsionBase(Config.uniqueEffects.magiblade.repelFrequency, Config.uniqueEffects.magiblade.repelChance,
                Config.uniqueEffects.magiblade.repelRadius);
    }

    private static ArcaneCosmicMasteryTuning wardenBase() {
        return wardenBase(chargeDuration(), summonDuration(), Config.uniqueEffects.magiblade.headOrbitRadius, sonicRange());
    }

    private static ArcaneCosmicMasteryTuning judgmentBase() {
        return judgmentBase(sonicInterval(), sonicChargeDuration(), SONIC_BEAM_WIDTH);
    }

    public static void clear(ServerWorld world) {
        Map<UUID, ChargeState> charges = ACTIVE_CHARGES.remove(world);
        if (charges != null) charges.values().forEach(charge -> ArcaneCosmicMasteryCombatManager.finish(charge.execution, 0));
        Map<UUID, HeadState> heads = ACTIVE_HEADS.remove(world);
        if (heads != null) heads.values().forEach(head -> ArcaneCosmicMasteryCombatManager.finish(head.execution, head.affectedTargets));
        LAST_PASSIVE_TICKS.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        REPULSION_STATES.remove(actor.getUuid());
        ACTIVE_CHARGES.values().forEach(map -> {
            ChargeState charge = map.remove(actor.getUuid());
            if (charge != null) ArcaneCosmicMasteryCombatManager.finish(charge.execution, 0);
        });
        ACTIVE_HEADS.values().forEach(map -> {
            HeadState head = map.remove(actor.getUuid());
            if (head != null) ArcaneCosmicMasteryCombatManager.finish(head.execution, head.affectedTargets);
        });
    }

    public static void clearAll() {
        ACTIVE_CHARGES.values().forEach(map -> map.values().forEach(
                charge -> ArcaneCosmicMasteryCombatManager.finish(charge.execution, 0)));
        ACTIVE_HEADS.values().forEach(map -> map.values().forEach(
                head -> ArcaneCosmicMasteryCombatManager.finish(head.execution, head.affectedTargets)));
        ACTIVE_CHARGES.clear();
        ACTIVE_HEADS.clear();
        REPULSION_STATES.clear();
        LAST_PASSIVE_TICKS.clear();
    }

    public static boolean startCharging(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        UniqueAbilityExecution execution = ArcaneCosmicMasteryCombatManager.beginActive(
                ArcaneCosmicMasteryAbilities.MAGIBLADE_WARDEN, context, Config.uniqueEffects.magiblade.cooldown,
                wardenBase());
        ArcaneCosmicMasteryTuning tuning = ArcaneCosmicMasteryAbilities.tuning(execution);
        UniqueAbilityExecution judgmentExecution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.MAGIBLADE_JUDGMENT, context.world(), context.stack(),
                context.actor(), null, judgmentBase());
        ArcaneCosmicMasteryTuning judgment = ArcaneCosmicMasteryAbilities.tuning(judgmentExecution);
        ArcaneCosmicMasteryCombatManager.finish(judgmentExecution, 0);
        LivingEntity actor = context.actor();
        Hand hand = context.hand() == null ? Hand.MAIN_HAND : context.hand();
        ChargeState charge = new ChargeState(
                actor.getUuid(),
                context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid(),
                context.target() == null ? null : context.target().getUuid(),
                context.stack().copy(),
                hand,
                context.world().getTime(),
                tuning,
                judgment,
                execution
        );
        ACTIVE_CHARGES.computeIfAbsent(context.world(), ignored -> new HashMap<>())
                .put(actor.getUuid(), charge);
        spawnChargeStartEffects(context.world(), actor);
        return true;
    }

    public static void cancelCharging(LivingEntity actor, boolean clearCooldown) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return;
        }

        Map<UUID, ChargeState> charges = ACTIVE_CHARGES.get(world);
        ChargeState removed = charges == null ? null : charges.remove(actor.getUuid());
        if (removed == null) {
            return;
        }
        if (charges.isEmpty()) {
            ACTIVE_CHARGES.remove(world);
        }
        if (clearCooldown) {
            clearCooldown(actor, removed.stack);
        }
        ArcaneCosmicMasteryCombatManager.finish(removed.execution, 0);
        spawnCancelledChargeEffects(world, actor);
    }

    public static void tickHeldPassive(LivingEntity actor, ItemStack stack) {
        if (actor == null
                || stack == null
                || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.MAGIBLADE.get())
                || !AwakeningApi.isAbilityUnlocked(stack)
                || !(actor.getWorld() instanceof ServerWorld world)
                || !actor.isAlive()
                || !HelperMethods.isHolding(stack, actor)) {
            return;
        }

        long now = world.getTime();
        RepulsionState passive = REPULSION_STATES.computeIfAbsent(actor.getUuid(), ignored -> new RepulsionState());
        if (now >= passive.refreshAt) {
            passive.refreshAt = now + 20;
            UniqueAbilityExecution execution = ArcaneCosmicMasteryCombatManager.beginPassive(
                    ArcaneCosmicMasteryAbilities.MAGIBLADE_REPULSION, world, stack, actor, null, repulsionBase());
            passive.tuning = ArcaneCosmicMasteryAbilities.tuning(execution);
            ArcaneCosmicMasteryCombatManager.finish(execution, 0);
        }
        Map<UUID, Long> lastTicks = LAST_PASSIVE_TICKS.computeIfAbsent(world, ignored -> new HashMap<>());
        if (lastTicks.getOrDefault(actor.getUuid(), Long.MIN_VALUE) == now) {
            return;
        }
        lastTicks.put(actor.getUuid(), now);
        if (now % 200L == 0L) {
            lastTicks.entrySet().removeIf(entry -> entry.getValue() < now - 40L);
            if (lastTicks.isEmpty()) {
                LAST_PASSIVE_TICKS.remove(world);
            }
        }

        int frequency = Math.max(1, passive.tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, Config.uniqueEffects.magiblade.repelFrequency));
        if ((actor.age + actor.getId()) % frequency != 0) {
            return;
        }
        int chance = Math.clamp(passive.tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.CHANCE, Config.uniqueEffects.magiblade.repelChance), 0, 100);
        if (chance <= 0 || actor.getRandom().nextInt(100) >= chance) {
            return;
        }

        double radius = Math.max(0.5, passive.tuning.get(
                ArcaneCosmicMasteryTuning.Setting.RADIUS, Config.uniqueEffects.magiblade.repelRadius));
        Box searchBox = actor.getBoundingBox().expand(radius, Math.max(1.0, radius * 0.5), radius);
        LivingEntity closest = world.getEntitiesByClass(
                        LivingEntity.class,
                        searchBox,
                        target -> isValidEnemy(world, actor, null, target) && target.distanceTo(actor) > 1.0F
                ).stream()
                .min(Comparator.comparingDouble(actor::squaredDistanceTo))
                .orElse(null);
        if (closest == null) {
            return;
        }

        double knockback = .5 * passive.tuning.get(ArcaneCosmicMasteryTuning.Setting.KNOCKBACK, 1);
        closest.setVelocity(
                (closest.getX() - actor.getX()) * knockback,
                closest.getVelocity().y,
                (closest.getZ() - actor.getZ()) * knockback
        );
        if (passive.tuning.flag(1 << 4)) closest.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SLOWNESS,
                passive.tuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 30),
                passive.tuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_AMPLIFIER, 1)), actor);
        if (passive.tuning.flag(1 << 6)) actor.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.RESISTANCE,
                passive.tuning.integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DURATION_TICKS, 20), 0), actor);
        if (passive.tuning.flag(1 << 5)) {
            passive.countertoneTarget = closest.getUuid();
            passive.countertoneUntil = now + passive.tuning.integer(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, 60);
        }
        if (passive.tuning.flag(1 << 8)) {
            float damage = HelperMethods.abilityScaledDamage("arcane", actor, stack,
                    Config.uniqueEffects.magiblade.damageScaling
                            * (float) passive.tuning.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, .6),
                    Config.uniqueEffects.magiblade.spellScaling);
            world.getEntitiesByClass(LivingEntity.class, searchBox,
                            target -> isValidEnemy(world, actor, null, target))
                    .stream().limit(passive.tuning.integer(ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, 6))
                    .forEach(target -> target.damage(
                            world.getDamageSources().indirectMagic(actor, actor), damage));
        }
        closest.velocityModified = true;
        world.playSound(null, actor.getBlockPos(), SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                actor.getSoundCategory(), 0.8F, 1.0F + actor.getRandom().nextFloat() * 0.5F);
        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.ENCHANT, closest, actor, 10);
        HelperMethods.spawnOrbitParticles(world,
                closest.getPos().add(0.0, closest.getHeight() * 0.5, 0.0),
                ParticleTypes.SCULK_CHARGE_POP, 0.5, 6);
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ChargeState> charges = ACTIVE_CHARGES.get(world);
        Map<UUID, HeadState> heads = ACTIVE_HEADS.get(world);
        return charges != null && !charges.isEmpty()
                || heads != null && !heads.isEmpty()
                || world.getTime() % 40L == 0L;
    }

    public static void tick(ServerWorld world) {
        tickCharges(world);
        tickHeads(world);
        if (world.getTime() % 40L == 0L) {
            purgeOrphanVisuals(world);
        }
    }

    private static void tickCharges(ServerWorld world) {
        Map<UUID, ChargeState> charges = ACTIVE_CHARGES.get(world);
        if (charges == null || charges.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<ChargeState> iterator = charges.values().iterator();
        while (iterator.hasNext()) {
            ChargeState charge = iterator.next();
            LivingEntity actor = resolveLiving(world, charge.actorId);
            LivingEntity sourceOwner = resolveLiving(world, charge.sourceOwnerId);
            if (actor == null
                    || charge.sourceOwnerId != null && sourceOwner == null
                    || !isChargeStillValid(actor, charge)) {
                iterator.remove();
                if (actor != null) {
                    clearCooldown(actor, charge.stack);
                    spawnCancelledChargeEffects(world, actor);
                }
                ArcaneCosmicMasteryCombatManager.finish(charge.execution, 0);
                continue;
            }

            long elapsed = now - charge.startedAt;
            if (elapsed % 4L == 0L) {
                spawnChargingEffects(world, actor, elapsed);
            }
            if (elapsed < chargeDuration(charge.tuning)) {
                continue;
            }

            iterator.remove();
            completeCharge(world, actor, sourceOwner, charge, now);
            finishPlayerChannel(actor, charge);
        }

        if (charges.isEmpty()) {
            ACTIVE_CHARGES.remove(world);
        }
    }

    private static void tickHeads(ServerWorld world) {
        Map<UUID, HeadState> heads = ACTIVE_HEADS.get(world);
        if (heads == null || heads.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<HeadState> iterator = heads.values().iterator();
        while (iterator.hasNext()) {
            HeadState head = iterator.next();
            LivingEntity actor = resolveLiving(world, head.actorId);
            LivingEntity sourceOwner = resolveLiving(world, head.sourceOwnerId);
            MagibladeWardenHeadVisualEntity visual = resolveVisual(world, head.visualId);
            if (actor == null
                    || head.sourceOwnerId != null && sourceOwner == null
                    || !isStillWieldingMagiblade(actor)
                    || now >= head.expiresAt) {
                if (visual != null) {
                    beginDismissal(world, actor, visual);
                }
                ArcaneCosmicMasteryCombatManager.finish(head.execution, head.affectedTargets);
                iterator.remove();
                continue;
            }

            if (visual == null) {
                visual = spawnVisual(world, actor, head);
                if (visual == null) {
                    ArcaneCosmicMasteryCombatManager.finish(head.execution, head.affectedTargets);
                    iterator.remove();
                    continue;
                }
            }

            updateOrbit(world, actor, visual, head, now);
            tickHeadTargeting(world, actor, sourceOwner, visual, head, now);
            spawnAmbientEffects(world, actor, visual, now);
        }

        if (heads.isEmpty()) {
            ACTIVE_HEADS.remove(world);
        }
    }

    private static void completeCharge(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                       ChargeState charge, long now) {
        float damage = HelperMethods.abilityScaledDamage("arcane", actor, charge.stack,
                Config.uniqueEffects.magiblade.damageScaling * (float) charge.tuning.get(
                        ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                        * (float) charge.judgment.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1),
                Config.uniqueEffects.magiblade.spellScaling);
        Map<UUID, HeadState> heads = ACTIVE_HEADS.computeIfAbsent(world, ignored -> new HashMap<>());
        HeadState existing = heads.get(actor.getUuid());
        if (existing != null) {
            ArcaneCosmicMasteryCombatManager.finish(existing.execution, existing.affectedTargets);
            existing.sourceOwnerId = sourceOwner == null ? null : sourceOwner.getUuid();
            existing.stack = charge.stack.copy();
            existing.damage = Math.max(0.0F, damage);
            existing.expiresAt = now + summonDuration(charge.tuning);
            existing.tuning = charge.tuning;
            existing.judgment = charge.judgment;
            existing.execution = charge.execution;
            if (existing.targetId == null && charge.preferredTargetId != null) {
                existing.targetId = charge.preferredTargetId;
            }
            MagibladeWardenHeadVisualEntity visual = resolveVisual(world, existing.visualId);
            if (visual != null) {
                visual.setScale(Math.max(0.1F, Config.uniqueEffects.magiblade.headScale));
                visual.setOwnerEntityId(actor.getId());
            }
            spawnRefreshEffects(world, actor, visual);
            return;
        }

        HeadState head = new HeadState(
                actor.getUuid(),
                sourceOwner == null ? null : sourceOwner.getUuid(),
                charge.preferredTargetId,
                charge.stack.copy(),
                now,
                now + summonDuration(charge.tuning),
                now,
                Math.max(0.0F, damage),
                Math.floorMod(actor.getUuid().hashCode(), 360) * MathHelper.RADIANS_PER_DEGREE,
                charge.tuning,
                charge.execution
        );
        head.judgment = charge.judgment;
        MagibladeWardenHeadVisualEntity visual = spawnVisual(world, actor, head);
        if (visual == null) {
            ArcaneCosmicMasteryCombatManager.finish(charge.execution, 0);
            return;
        }
        heads.put(actor.getUuid(), head);
        spawnSummonEffects(world, actor, visual);
    }

    private static MagibladeWardenHeadVisualEntity spawnVisual(ServerWorld world, LivingEntity actor, HeadState head) {
        Vec3d position = orbitPosition(actor, head, world.getTime());
        MagibladeWardenHeadVisualEntity visual = new MagibladeWardenHeadVisualEntity(
                world,
                actor,
                position.x,
                position.y,
                position.z,
                Math.max(0.1F, Config.uniqueEffects.magiblade.headScale),
                (float) head.orbitPhase
        );
        visual.addCommandTag(VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return null;
        }
        head.visualId = visual.getUuid();
        return visual;
    }

    private static void updateOrbit(ServerWorld world, LivingEntity actor,
                                    MagibladeWardenHeadVisualEntity visual, HeadState head, long now) {
        Vec3d position = orbitPosition(actor, head, now);
        visual.setPosition(position);
        visual.setOwnerEntityId(actor.getId());
        visual.setScale(Math.max(0.1F, Config.uniqueEffects.magiblade.headScale));
    }

    private static void tickBoundWarden(ServerWorld world, LivingEntity actor, HeadState head, long now) {
        if (!head.tuning.flag(1 << 15)) return;
        if (now < head.nextGuardAt) return;
        head.nextGuardAt = now + Math.max(1, head.tuning.integer(ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, 80));
        MasteryAbsorptionTracker.grant(actor, (float) head.tuning.get(ArcaneCosmicMasteryTuning.Setting.ABSORPTION, 4),
                Math.max(1, head.tuning.integer(ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, 80)),
                (float) head.tuning.get(ArcaneCosmicMasteryTuning.Setting.ABSORPTION, 4));
        actor.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.RESISTANCE, 20, 0, false, false, true), actor);
    }

    private static Vec3d orbitPosition(LivingEntity actor, HeadState head, long now) {
        double elapsed = Math.max(0L, now - head.spawnedAt);
        double angle = head.orbitPhase + elapsed * Math.max(0.001,
                Config.uniqueEffects.magiblade.headOrbitSpeed * head.tuning.get(
                        ArcaneCosmicMasteryTuning.Setting.SPEED, 1));
        double radius = Math.max(0.0, head.tuning.get(
                ArcaneCosmicMasteryTuning.Setting.RADIUS, Config.uniqueEffects.magiblade.headOrbitRadius));
        double bob = MathHelper.sin((float) (elapsed * ORBIT_BOB_SPEED + head.orbitPhase)) * ORBIT_BOB_HEIGHT;
        return new Vec3d(
                actor.getX() + Math.cos(angle) * radius,
                actor.getEyeY() + Config.uniqueEffects.magiblade.headVerticalOffset + bob,
                actor.getZ() + Math.sin(angle) * radius
        );
    }

    private static void tickHeadTargeting(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                          MagibladeWardenHeadVisualEntity visual, HeadState head, long now) {
        tickBoundWarden(world, actor, head, now);
        LivingEntity target = resolveLiving(world, head.targetId);
        if (!isValidEnemyInRange(world, actor, sourceOwner, target)) {
            target = null;
            head.targetId = null;
            if (head.fireAt >= 0L) {
                head.fireAt = -1L;
                head.nextCycleAt = Math.min(head.nextCycleAt, now + TARGET_RETRY_TICKS);
            }
        }

        if (head.fireAt < 0L && now >= head.nextCycleAt) {
            target = findTarget(world, actor, sourceOwner, target, head);
            if (target != null && !passesJudgmentFilter(head, target)) target = null;
            if (target == null) {
                head.targetId = null;
                head.nextCycleAt = now + TARGET_RETRY_TICKS;
            } else {
                head.targetId = target.getUuid();
                head.fireAt = now + sonicChargeDuration(head.judgment);
                head.nextCycleAt = now + sonicInterval(head.judgment);
                beginSonicCharge(world, actor, visual);
            }
        }

        target = resolveLiving(world, head.targetId);
        if (!isValidEnemyInRange(world, actor, sourceOwner, target)) {
            target = null;
        }
        updateHeadRotation(actor, visual, target, head, now);
        visual.setTargetEntityId(target == null ? -1 : target.getId());
        visual.setAiming(head.fireAt >= 0L && target != null);

        if (target != null && head.fireAt >= 0L && now >= head.fireAt) {
            fireSonicWave(world, actor, sourceOwner, visual, head, target);
            head.fireAt = -1L;
            visual.setAiming(false);
            visual.setShotPulseTicks(SHOT_PULSE_TICKS);
        }
    }

    private static LivingEntity findTarget(ServerWorld world, LivingEntity actor,
                                           LivingEntity sourceOwner, LivingEntity currentTarget,
                                           HeadState head) {
        if (isValidEnemyInRange(world, actor, sourceOwner, currentTarget)) {
            return currentTarget;
        }
        if (actor instanceof MobEntity mob
                && isValidEnemyInRange(world, actor, sourceOwner, mob.getTarget())) {
            return mob.getTarget();
        }

        double range = sonicRange(head.tuning);
        Box searchBox = actor.getBoundingBox().expand(range, Math.max(2.0, range * 0.5), range);
        return world.getEntitiesByClass(
                        LivingEntity.class,
                        searchBox,
                        target -> isValidEnemyInRange(world, actor, sourceOwner, target)
                ).stream()
                .min(Comparator.comparingDouble(actor::squaredDistanceTo))
                .orElse(null);
    }

    private static void updateHeadRotation(LivingEntity actor, MagibladeWardenHeadVisualEntity visual,
                                           LivingEntity target, HeadState head, long now) {
        Vec3d from = visual.getPos();
        Vec3d to;
        if (target != null) {
            to = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        } else {
            double elapsed = Math.max(0L, now - head.spawnedAt);
            double angle = head.orbitPhase
                    + elapsed * Math.max(0.001, Config.uniqueEffects.magiblade.headOrbitSpeed)
                    + MathHelper.HALF_PI;
            to = from.add(Math.cos(angle) * 3.0, 0.0, Math.sin(angle) * 3.0);
        }

        Vec3d direction = to.subtract(from);
        if (direction.lengthSquared() < 1.0E-5) {
            direction = actor.getRotationVec(1.0F);
        }
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float desiredYaw = (float) (MathHelper.atan2(direction.z, direction.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
        float desiredPitch = (float) (-(MathHelper.atan2(direction.y, horizontal) * MathHelper.DEGREES_PER_RADIAN));
        float turnSpeed = Math.max(0.1F, Config.uniqueEffects.magiblade.headTurnSpeedDegreesPerTick
                * (float) head.tuning.get(ArcaneCosmicMasteryTuning.Setting.SPEED, 1));
        visual.setTargetYaw(MathHelper.stepUnwrappedAngleTowards(visual.getTargetYaw(), desiredYaw, turnSpeed));
        visual.setTargetPitch(MathHelper.stepTowards(
                visual.getTargetPitch(),
                MathHelper.clamp(desiredPitch, -60.0F, 60.0F),
                turnSpeed
        ));
    }

    private static void harmonicCollapse(ServerWorld world, LivingEntity actor, HeadState head,
                                         LivingEntity target) {
        if (!head.judgment.flag(1 << 24)) return;
        int required = Math.max(1, head.judgment.integer(ArcaneCosmicMasteryTuning.Setting.COUNT, 3));
        if (head.sonicHits.merge(target.getUuid(), 1, Integer::sum) % required != 0) return;
        double radius = head.judgment.get(ArcaneCosmicMasteryTuning.Setting.SECONDARY_RADIUS, 2.5);
        float damage = head.damage * (float) head.judgment.get(
                ArcaneCosmicMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .5);
        world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(radius),
                        other -> isValidEnemy(world, actor, null, other))
                .stream().limit(head.judgment.integer(ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, 5))
                .forEach(other -> HelperMethods.damageThroughIframes(other,
                        world.getDamageSources().indirectMagic(actor, actor), damage));
    }

    private static boolean passesJudgmentFilter(HeadState head, LivingEntity target) {
        if (!head.judgment.has(ArcaneCosmicMasteryTuning.Setting.HEALTH_THRESHOLD)) return true;
        return target.getHealth() <= target.getMaxHealth()
                * head.judgment.get(ArcaneCosmicMasteryTuning.Setting.HEALTH_THRESHOLD, .5);
    }

    private static void fireSonicWave(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                      MagibladeWardenHeadVisualEntity visual, HeadState head,
                                      LivingEntity selectedTarget) {
        Vec3d start = getMouthPosition(visual);
        Vec3d end = selectedTarget.getPos().add(0.0, selectedTarget.getHeight() * 0.55, 0.0);
        Vec3d delta = end.subtract(start);
        double distance = delta.length();
        if (distance < 1.0E-4) {
            return;
        }

        int particles = Math.max(2, (int) Math.ceil(distance * 1.75));
        for (int i = 0; i < particles; i++) {
            double progress = particles <= 1 ? 0.0 : (double) i / (particles - 1);
            Vec3d position = start.lerp(end, progress);
            world.spawnParticles(ParticleTypes.SONIC_BOOM,
                    position.x, position.y, position.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        LivingEntity attributedOwner = sourceOwner == null ? actor : sourceOwner;
        DamageSource source = attributedOwner.getDamageSources().sonicBoom(attributedOwner);
        double beamWidth = Math.max(head.judgment.get(ArcaneCosmicMasteryTuning.Setting.WIDTH, SONIC_BEAM_WIDTH),
                head.judgment.flag(1 << 25) ? head.judgment.get(ArcaneCosmicMasteryTuning.Setting.WIDTH, 5) : 0);
        Box searchBox = new Box(start, end).expand(beamWidth);
        int hitIndex = 0;
        int beamCap = head.judgment.flag(1 << 25)
                ? head.judgment.integer(ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, 10) : Integer.MAX_VALUE;
        int struck = 0;
        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                candidate -> isValidEnemy(world, actor, sourceOwner, candidate)
        )) {
            if (struck >= beamCap) break;
            struck++;
            Box hitbox = target.getBoundingBox().expand(beamWidth);
            if (!hitbox.contains(start) && hitbox.raycast(start, end).isEmpty()) {
                continue;
            }

            float damage = HelperMethods.applyAbilityDamageEnchantments(
                    world,
                    head.stack,
                    target,
                    source,
                    head.damage * (head.judgment.flag(1 << 23) && hitIndex < 3
                            ? (float) head.judgment.get(ArcaneCosmicMasteryTuning.Setting.OUTGOING_MULTIPLIER, 1.2) : 1)
            );
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(source, damage));
            if (damaged[0]) {
                hitIndex++;
                head.affectedTargets++;
                if (head.judgment.flag(1 << 22)) target.addStatusEffect(
                        new net.minecraft.entity.effect.StatusEffectInstance(
                                net.minecraft.entity.effect.StatusEffects.WEAKNESS,
                                head.judgment.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 50),
                                head.judgment.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_AMPLIFIER, 0)), actor);
                harmonicCollapse(world, actor, head, target);
                spawnSonicImpactEffects(world, target);
            }
        }

        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                actor.getSoundCategory(), 1.0F, 1.05F);
        world.spawnParticles(ParticleTypes.SCULK_CHARGE_POP,
                start.x, start.y, start.z, 12, 0.22, 0.18, 0.22, 0.035);
    }

    private static Vec3d getMouthPosition(MagibladeWardenHeadVisualEntity visual) {
        double scale = visual.getScale();
        return visual.getPos()
                .add(0.0, 0.16 * scale, 0.0)
                .add(visual.getRotationVec(1.0F).multiply(0.52 * scale));
    }

    private static void beginSonicCharge(ServerWorld world, LivingEntity actor,
                                         MagibladeWardenHeadVisualEntity visual) {
        Vec3d position = visual.getPos();
        world.playSound(null, position.x, position.y, position.z, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE,
                actor.getSoundCategory(), 0.75F, 1.15F);
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                position.x, position.y, position.z, 8, 0.28, 0.24, 0.28, 0.025);
        world.spawnParticles(ParticleTypes.ENCHANT,
                position.x, position.y, position.z, 12, 0.42, 0.32, 0.42, 0.05);
    }

    private static void spawnAmbientEffects(ServerWorld world, LivingEntity actor,
                                            MagibladeWardenHeadVisualEntity visual, long now) {
        if ((now + actor.getId()) % 45L != 0L) {
            return;
        }
        world.spawnParticles(ParticleTypes.SCULK_CHARGE_POP,
                visual.getX(), visual.getY(), visual.getZ(), 2, 0.2, 0.16, 0.2, 0.01);
        if (actor.getRandom().nextBoolean()) {
            world.playSound(null, visual.getX(), visual.getY(), visual.getZ(),
                    SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS,
                    actor.getSoundCategory(), 0.22F, 1.25F + actor.getRandom().nextFloat() * 0.15F);
        } else {
            world.playSound(null, visual.getX(), visual.getY(), visual.getZ(),
                    SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                    actor.getSoundCategory(), 0.18F, 1.35F);
        }
    }

    private static void spawnChargeStartEffects(ServerWorld world, LivingEntity actor) {
        Vec3d position = actor.getPos().add(0.0, actor.getHeight() * 0.65, 0.0);
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                position.x, position.y, position.z, 12, 0.38, 0.45, 0.38, 0.025);
        world.playSound(null, actor.getBlockPos(), SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS,
                actor.getSoundCategory(), 0.55F, 0.9F);
    }

    private static void spawnChargingEffects(ServerWorld world, LivingEntity actor, long elapsed) {
        float progress = MathHelper.clamp(elapsed / (float) chargeDuration(), 0.0F, 1.0F);
        Vec3d position = actor.getPos().add(0.0, actor.getHeight() * (0.55 + progress * 0.3), 0.0);
        world.spawnParticles(ParticleTypes.ENCHANT,
                position.x, position.y, position.z,
                3 + (int) (progress * 5.0F), 0.34, 0.42, 0.34, 0.04);
        if (elapsed > 0L && elapsed % 12L == 0L) {
            world.playSound(null, actor.getBlockPos(), SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                    actor.getSoundCategory(), 0.28F, 0.85F + progress * 0.55F);
        }
    }

    private static void spawnCancelledChargeEffects(ServerWorld world, LivingEntity actor) {
        Vec3d position = actor.getPos().add(0.0, actor.getHeight() * 0.65, 0.0);
        world.spawnParticles(ParticleTypes.ASH,
                position.x, position.y, position.z, 6, 0.24, 0.28, 0.24, 0.015);
    }

    private static void spawnSummonEffects(ServerWorld world, LivingEntity actor,
                                           MagibladeWardenHeadVisualEntity visual) {
        Vec3d position = visual.getPos();
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                position.x, position.y, position.z, 28, 0.48, 0.4, 0.48, 0.055);
        world.spawnParticles(ParticleTypes.SCULK_CHARGE_POP,
                position.x, position.y, position.z, 18, 0.38, 0.3, 0.38, 0.045);
        world.playSound(null, position.x, position.y, position.z, SoundEvents.ENTITY_WARDEN_EMERGE,
                actor.getSoundCategory(), 0.8F, 1.25F);
    }

    private static void spawnRefreshEffects(ServerWorld world, LivingEntity actor,
                                            MagibladeWardenHeadVisualEntity visual) {
        Vec3d position = visual == null
                ? actor.getPos().add(0.0, actor.getHeight() + 0.75, 0.0)
                : visual.getPos();
        world.spawnParticles(ParticleTypes.SCULK_CHARGE_POP,
                position.x, position.y, position.z, 16, 0.34, 0.28, 0.34, 0.04);
        world.playSound(null, position.x, position.y, position.z, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
                actor.getSoundCategory(), 0.55F, 1.3F);
    }

    private static void spawnSonicImpactEffects(ServerWorld world, LivingEntity target) {
        Vec3d position = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.SCULK_CHARGE_POP,
                position.x, position.y, position.z, 8, 0.24, 0.3, 0.24, 0.035);
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT,
                target.getSoundCategory(), 0.35F, 1.35F);
    }

    private static void beginDismissal(ServerWorld world, LivingEntity actor,
                                       MagibladeWardenHeadVisualEntity visual) {
        if (visual.isDismissing()) {
            return;
        }
        visual.beginDismissal();
        Vec3d position = visual.getPos();
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                position.x, position.y, position.z, 14, 0.32, 0.28, 0.32, 0.035);
        world.playSound(null, position.x, position.y, position.z, SoundEvents.ENTITY_WARDEN_DIG,
                actor == null ? net.minecraft.sound.SoundCategory.PLAYERS : actor.getSoundCategory(),
                0.42F, 1.45F);
    }

    private static boolean isValidEnemyInRange(ServerWorld world, LivingEntity actor,
                                               LivingEntity sourceOwner, LivingEntity target) {
        double range = sonicRange();
        return isValidEnemy(world, actor, sourceOwner, target)
                && target.squaredDistanceTo(actor) <= range * range;
    }

    private static boolean isValidEnemy(ServerWorld world, LivingEntity actor,
                                        LivingEntity sourceOwner, LivingEntity target) {
        if (world == null
                || actor == null
                || !actor.isAlive()
                || actor.getWorld() != world
                || target == null
                || !target.isAlive()
                || target == actor
                || target == sourceOwner
                || target.getWorld() != world
                || !EntityPredicates.VALID_LIVING_ENTITY.test(target)
                || !HelperMethods.checkAbilityTarget(target, actor)
                || sourceOwner != null && !HelperMethods.checkAbilityTarget(target, sourceOwner)) {
            return false;
        }

        if (target instanceof PlayerEntity) {
            return true;
        }
        if (target instanceof HostileEntity || HelperMethods.isMonsterFaction(target)) {
            return true;
        }
        if (target instanceof MobEntity targetMob
                && (targetMob.getTarget() == actor
                || sourceOwner != null && targetMob.getTarget() == sourceOwner)) {
            return true;
        }
        return actor instanceof MobEntity actorMob && actorMob.getTarget() == target;
    }

    private static boolean isChargeStillValid(LivingEntity actor, ChargeState charge) {
        if (!actor.getStackInHand(charge.hand).isOf(ItemsRegistry.MAGIBLADE.get())
                || !AwakeningApi.isAbilityUnlocked(actor.getStackInHand(charge.hand))) {
            return false;
        }
        if (!(actor instanceof ServerPlayerEntity player)) {
            return true;
        }
        return player.isUsingItem() && player.getActiveHand() == charge.hand
                || PlayerWeaponAbilityChannelManager.isChanneling(
                player, charge.hand, ItemsRegistry.MAGIBLADE.get());
    }

    private static boolean isHeldInActivationHand(LivingEntity actor, Hand hand) {
        Hand resolvedHand = hand == null ? Hand.MAIN_HAND : hand;
        ItemStack held = actor.getStackInHand(resolvedHand);
        return held.isOf(ItemsRegistry.MAGIBLADE.get()) && AwakeningApi.isAbilityUnlocked(held);
    }

    private static boolean isStillWieldingMagiblade(LivingEntity actor) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = actor.getStackInHand(hand);
            if (stack.isOf(ItemsRegistry.MAGIBLADE.get()) && AwakeningApi.isAbilityUnlocked(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void finishPlayerChannel(LivingEntity actor, ChargeState charge) {
        if (!(actor instanceof ServerPlayerEntity player)) {
            return;
        }
        ItemStack currentStack = player.getStackInHand(charge.hand);
        if (!PlayerWeaponAbilityChannelManager.finishEarly(player, currentStack)
                && player.isUsingItem()
                && player.getActiveHand() == charge.hand) {
            player.stopUsingItem();
        }
    }

    private static void clearCooldown(LivingEntity actor, ItemStack stack) {
        SimplySwordsAPI.setWeaponCooldown(actor, stack, 0);
    }

    private static boolean hasCharge(ServerWorld world, UUID actorId) {
        Map<UUID, ChargeState> charges = ACTIVE_CHARGES.get(world);
        return charges != null && charges.containsKey(actorId);
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static MagibladeWardenHeadVisualEntity resolveVisual(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = world.getEntity(id);
        return entity instanceof MagibladeWardenHeadVisualEntity visual
                && visual.isAlive()
                && !visual.isDismissing()
                ? visual
                : null;
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        Set<UUID> activeVisualIds = new HashSet<>();
        Map<UUID, HeadState> heads = ACTIVE_HEADS.get(world);
        if (heads != null) {
            for (HeadState head : heads.values()) {
                if (head.visualId != null) {
                    activeVisualIds.add(head.visualId);
                }
            }
        }

        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof MagibladeWardenHeadVisualEntity visual
                    && entity.getCommandTags().contains(VISUAL_TAG)
                    && !activeVisualIds.contains(entity.getUuid())
                    && !visual.isDismissing()) {
                visual.beginDismissal();
            }
        }
    }

    private static int chargeDuration() {
        return Math.max(1, Config.uniqueEffects.magiblade.chargeDuration);
    }

    private static int chargeDuration(ArcaneCosmicMasteryTuning tuning) {
        return Math.max(1, tuning.integer(ArcaneCosmicMasteryTuning.Setting.WINDUP_TICKS, chargeDuration()));
    }

    private static int summonDuration() {
        return Math.max(1, Config.uniqueEffects.magiblade.summonDuration);
    }

    private static int summonDuration(ArcaneCosmicMasteryTuning tuning) {
        return Math.max(1, tuning.integer(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, summonDuration()));
    }

    private static int sonicInterval() {
        return Math.max(1, Config.uniqueEffects.magiblade.sonicInterval);
    }

    private static int sonicInterval(ArcaneCosmicMasteryTuning tuning) {
        return Math.max(1, tuning.integer(ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, sonicInterval()));
    }

    public static int sonicIntervalFor(ArcaneCosmicMasteryTuning judgment) {
        return sonicInterval(judgment);
    }

    private static int sonicChargeDuration() {
        return Math.max(1, Config.uniqueEffects.magiblade.sonicChargeDuration);
    }

    private static int sonicChargeDuration(ArcaneCosmicMasteryTuning tuning) {
        return Math.max(1, tuning.integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_INTERVAL_TICKS,
                tuning.integer(ArcaneCosmicMasteryTuning.Setting.WINDUP_TICKS, sonicChargeDuration())));
    }

    private static double sonicRange() {
        return Math.max(1.0, Config.uniqueEffects.magiblade.sonicDistance);
    }

    private static double sonicRange(ArcaneCosmicMasteryTuning tuning) {
        return Math.max(1, tuning.get(ArcaneCosmicMasteryTuning.Setting.RANGE, sonicRange()));
    }

    private static final class ChargeState {
        private final UUID actorId;
        private final UUID sourceOwnerId;
        private final UUID preferredTargetId;
        private final ItemStack stack;
        private final Hand hand;
        private final long startedAt;
        private final ArcaneCosmicMasteryTuning tuning;
        private final ArcaneCosmicMasteryTuning judgment;
        private final UniqueAbilityExecution execution;

        private ChargeState(UUID actorId, UUID sourceOwnerId, UUID preferredTargetId,
                            ItemStack stack, Hand hand, long startedAt,
                            ArcaneCosmicMasteryTuning tuning, ArcaneCosmicMasteryTuning judgment,
                            UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.preferredTargetId = preferredTargetId;
            this.stack = stack;
            this.hand = hand;
            this.startedAt = startedAt;
            this.tuning = tuning;
            this.judgment = judgment;
            this.execution = execution;
        }
    }

    private static final class HeadState {
        private final UUID actorId;
        private UUID sourceOwnerId;
        private UUID targetId;
        private ItemStack stack;
        private final long spawnedAt;
        private long expiresAt;
        private long nextCycleAt;
        private long fireAt = -1L;
        private float damage;
        private final double orbitPhase;
        private UUID visualId;
        private ArcaneCosmicMasteryTuning tuning;
        private ArcaneCosmicMasteryTuning judgment = ArcaneCosmicMasteryTuning.EMPTY;
        private UniqueAbilityExecution execution;
        private int affectedTargets;
        private long nextGuardAt;
        private final Map<UUID, Integer> sonicHits = new HashMap<>();
        private static final HeadState EMPTY = new HeadState(null, null, null, ItemStack.EMPTY,
                0, 0, 0, 0, 0, ArcaneCosmicMasteryTuning.EMPTY, null);

        private HeadState(UUID actorId, UUID sourceOwnerId, UUID targetId,
                          ItemStack stack, long spawnedAt, long expiresAt,
                          long nextCycleAt, float damage, double orbitPhase,
                          ArcaneCosmicMasteryTuning tuning, UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.targetId = targetId;
            this.stack = stack;
            this.spawnedAt = spawnedAt;
            this.expiresAt = expiresAt;
            this.nextCycleAt = nextCycleAt;
            this.damage = damage;
            this.orbitPhase = orbitPhase;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private static final class RepulsionState {
        private long refreshAt;
        private ArcaneCosmicMasteryTuning tuning = ArcaneCosmicMasteryTuning.EMPTY;
        private UUID countertoneTarget;
        private long countertoneUntil;
    }
}
