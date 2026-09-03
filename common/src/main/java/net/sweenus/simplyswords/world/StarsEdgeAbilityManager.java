package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryTuning;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.StarsEdgeConstellationVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StarsEdgeAbilityManager {

    private static final String VISUAL_TAG = "simplyswords_stars_edge_constellation";
    private static final int PHASE_INITIAL_DASH = 0;
    private static final int PHASE_RECORDING = 1;
    private static final int PHASE_DETONATING = 2;

    private static final Map<ServerWorld, Map<UUID, ActiveReprise>> ACTIVE = new HashMap<>();
    private static final Map<UUID, RepriseState> REPRISE_STATES = new HashMap<>();

    public static ArcaneCosmicMasteryTuning solarBase() {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    public static ArcaneCosmicMasteryTuning lunarBase() {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                .with(ArcaneCosmicMasteryTuning.Setting.HEAL_MULTIPLIER, 1);
    }

    public static ArcaneCosmicMasteryTuning constellationBase(int recordingDuration, int constellationDuration,
                                                        int segmentInterval, int contactInterval,
                                                        double segmentRadius, double contactWidth, int maxNodes) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, recordingDuration)
                .with(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DURATION_TICKS, constellationDuration)
                .with(ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, segmentInterval)
                .with(ArcaneCosmicMasteryTuning.Setting.SECONDARY_INTERVAL_TICKS, contactInterval)
                .with(ArcaneCosmicMasteryTuning.Setting.RADIUS, segmentRadius)
                .with(ArcaneCosmicMasteryTuning.Setting.SECONDARY_RADIUS, contactWidth)
                .with(ArcaneCosmicMasteryTuning.Setting.COUNT, maxNodes)
                .with(ArcaneCosmicMasteryTuning.Setting.WIDTH, 1)
                .with(ArcaneCosmicMasteryTuning.Setting.RANGE, 1)
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    private static ArcaneCosmicMasteryTuning constellationBase() {
        return constellationBase(Config.uniqueEffects.stars_edge.recordingDuration, Config.uniqueEffects.stars_edge.constellationDuration,
                Config.uniqueEffects.stars_edge.segmentExplosionInterval, Config.uniqueEffects.stars_edge.constellationDamageInterval,
                Config.uniqueEffects.stars_edge.segmentExplosionRadius, Config.uniqueEffects.stars_edge.constellationDamageWidth,
                Config.uniqueEffects.stars_edge.maxNodes);
    }

    public static void clear(ServerWorld world) {
        Map<UUID, ActiveReprise> active = ACTIVE.remove(world);
        if (active != null) active.values().forEach(reprise ->
                UniqueAbilityApi.cancel(reprise.execution));
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        REPRISE_STATES.remove(actor.getUuid());
        ACTIVE.values().forEach(map -> {
            ActiveReprise reprise = map.remove(actor.getUuid());
            if (reprise != null) UniqueAbilityApi.cancel(reprise.execution);
        });
    }

    public static void clearAll() {
        ACTIVE.values().forEach(map -> map.values().forEach(reprise ->
                UniqueAbilityApi.cancel(reprise.execution)));
        ACTIVE.clear();
        REPRISE_STATES.clear();
    }

    private StarsEdgeAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.STARS_EDGE.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1) {
            return false;
        }

        ActiveReprise active = getActive(context.world(), context.actor().getUuid());
        if (active != null) {
            return active.phase == PHASE_RECORDING && isWieldingStarsEdge(context.actor());
        }

        if (!isWieldingStarsEdge(context.actor())) {
            return false;
        }
        if (context.actor() instanceof PlayerEntity) {
            return true;
        }
        return context.target() != null
                && context.target().isAlive()
                && HelperMethods.checkAbilityTarget(context.target(), context.actor());
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        ActiveReprise existing = getActive(context.world(), context.actor().getUuid());
        if (existing != null) {
            sealCurrentEndpoint(context.world(), context.actor(), existing);
            beginDetonation(context.world(), context.actor(), existing);
            return true;
        }
        UniqueAbilityExecution execution = ArcaneCosmicMasteryCombatManager.beginActive(
                ArcaneCosmicMasteryAbilities.STARS_CONSTELLATION, context, Config.uniqueEffects.stars_edge.cooldown,
                constellationBase());
        return start(context, ArcaneCosmicMasteryAbilities.tuning(execution), execution);
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        HelperMethods.playHitSounds(attacker, target);
        boolean day = world.isDay();
        UniqueAbilityExecution execution = ArcaneCosmicMasteryCombatManager.beginPassive(day
                ? ArcaneCosmicMasteryAbilities.STARS_SOLAR : ArcaneCosmicMasteryAbilities.STARS_LUNAR,
                world, stack, attacker, target, day ? solarBase() : lunarBase());
        ArcaneCosmicMasteryTuning tuning = ArcaneCosmicMasteryAbilities.tuning(execution);
        ArcaneCosmicMasteryTuning carryOver = ArcaneCosmicMasteryTuning.EMPTY;
        if (!day) {
            UniqueAbilityExecution solarExecution = ArcaneCosmicMasteryCombatManager.beginPassive(
                    ArcaneCosmicMasteryAbilities.STARS_SOLAR, world, stack, attacker, target, solarBase());
            carryOver = ArcaneCosmicMasteryAbilities.tuning(solarExecution);
            ArcaneCosmicMasteryCombatManager.finish(solarExecution, 0);
        }
        RepriseState state = REPRISE_STATES.computeIfAbsent(attacker.getUuid(), ignored -> new RepriseState());
        float abilityDamage = HelperMethods.abilityScaledDamage("arcane", attacker, stack,
                Config.uniqueEffects.stars_edge.damageScaling * (float) tuning.get(
                        ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1),
                Config.uniqueEffects.stars_edge.spellScaling);
        abilityDamage = HelperMethods.applyNonPlayerWeaponHitDamageModifier(attacker, abilityDamage);
        DamageSource source = attacker instanceof PlayerEntity player
                ? attacker.getDamageSources().playerAttack(player) : world.getDamageSources().generic();
        if (day) {
            if (tuning.flag(1 << 2)) {
                if (world.getTime() - state.lastSolarHit > tuning.integer(
                        ArcaneCosmicMasteryTuning.Setting.LOCKOUT_TICKS, 40)) state.solarChain = 0;
                state.solarChain = Math.min(tuning.integer(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, 4),
                        state.solarChain + 1);
                abilityDamage *= 1 + state.solarChain * tuning.get(
                        ArcaneCosmicMasteryTuning.Setting.PER_STACK_MULTIPLIER, .03);
                state.lastSolarHit = world.getTime();
            }
            if (tuning.flag(1 << 3)) {
                long time = world.getTimeOfDay() % 24000;
                if (time >= 5000 && time <= 7000) abilityDamage *= tuning.get(
                        ArcaneCosmicMasteryTuning.Setting.OUTGOING_MULTIPLIER, 1.15);
            }
            if (tuning.flag(1 << 7)) {
                state.solarCharge += tuning.integer(ArcaneCosmicMasteryTuning.Setting.COUNT, 5);
                if (state.solarCharge >= tuning.integer(ArcaneCosmicMasteryTuning.Setting.FLAT_DAMAGE, 25)) {
                    state.solarCharge = 0;
                    pulse(world, attacker, stack, target, abilityDamage * (float) tuning.get(
                            ArcaneCosmicMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1.5),
                            tuning.get(ArcaneCosmicMasteryTuning.Setting.RADIUS, 5), 64);
                }
            }
            target.timeUntilRegen = 0;
            boolean damaged = target.damage(source, HelperMethods.applyAbilityDamageEnchantments(
                    world, stack, target, source, abilityDamage));
            if (damaged && tuning.flag(1 << 1)) target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.GLOWING, tuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 60), 0), attacker);
            if (damaged && tuning.flag(1 << 4) && ++state.solarHits % 5 == 0) pulse(world, attacker, stack,
                    target, abilityDamage * .3F, 2, 5);
            if (damaged && tuning.flag(1 << 6)) target.setOnFireFor(2);
            if (!target.isAlive() && tuning.flag(1 << 5)) attacker.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, 60, 0), attacker);
        } else {
            boolean endlessDay = carryOver.flag(1 << 8);
            if (state.markedTarget != null && state.markedTarget.equals(target.getUuid())
                    && world.getTime() < state.markExpiresAt) {
                abilityDamage *= (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.PER_STACK_MULTIPLIER, 1.08);
                state.markedTarget = null;
            }
            if (state.ambushReady && tuning.flag(1 << 16)) {
                abilityDamage *= (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.OUTGOING_MULTIPLIER, 1.35);
                state.ambushReady = false;
            }
            if (endlessDay) {
                float nightDamage = abilityDamage
                        * (float) carryOver.get(ArcaneCosmicMasteryTuning.Setting.OUTGOING_MULTIPLIER, .6);
                target.timeUntilRegen = 0;
                target.damage(source, HelperMethods.applyAbilityDamageEnchantments(
                        world, stack, target, source, nightDamage));
            }
            float heal = endlessDay ? 0.0F : abilityDamage
                    * Config.uniqueEffects.stars_edge.lifestealModifier
                    * (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.HEAL_MULTIPLIER, 1);
            if (tuning.flag(1 << 12) && attacker.getHealth() / attacker.getMaxHealth()
                    < tuning.get(ArcaneCosmicMasteryTuning.Setting.HEALTH_THRESHOLD, .4))
                heal *= (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.INCOMING_MULTIPLIER, 1.25);
            if (heal > 0 && tuning.flag(1 << 13)) {
                state.markedTarget = target.getUuid();
                state.markExpiresAt = world.getTime()
                        + tuning.integer(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, 60);
            }
            float missing = attacker.getMaxHealth() - attacker.getHealth();
            attacker.heal(heal);
            if (tuning.flag(1 << 15) && heal > missing) attacker.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.ABSORPTION, 80,
                    Math.min(1, Math.max(0, (int) ((heal - missing) / 4)))), attacker);
            if (tuning.flag(1 << 10) && world.getTime() >= state.guardAt) {
                state.guardAt = world.getTime() + 40;
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 40, 0), attacker);
            }
            if (tuning.flag(1 << 9) && target.hasStatusEffect(StatusEffects.GLOWING)) attacker.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.SPEED, 30, 0), attacker);
            if (!target.isAlive() && tuning.flag(1 << 16)) {
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,
                        tuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 40), 0), attacker);
                state.ambushReady = true;
            }
            if (!target.isAlive() && tuning.flag(1 << 14)) {
                int refund = tuning.integer(ArcaneCosmicMasteryTuning.Setting.REFUND_TICKS, 10);
                int cap = tuning.integer(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, 60);
                int granted = Math.min(refund, Math.max(0, cap - state.nightRefunded));
                if (granted > 0) {
                    state.nightRefunded += granted;
                    SimplySwordsAPI.reduceWeaponCooldown(attacker, stack,
                            Config.uniqueEffects.stars_edge.cooldown, granted);
                }
            }
            float sharedHeal = heal * .35F;
            if (tuning.flag(1 << 17)) world.getEntitiesByClass(LivingEntity.class,
                            attacker.getBoundingBox().expand(5), ally -> ally != attacker
                                    && !HelperMethods.checkAbilityTarget(ally, attacker))
                    .stream().limit(2).forEach(ally -> ally.heal(sharedHeal));
        }
        ArcaneCosmicMasteryCombatManager.finish(execution, 1);
    }

    private static void pulse(ServerWorld world, LivingEntity actor, ItemStack stack, LivingEntity center,
                              float damage, double radius, int cap) {
        DamageSource source = world.getDamageSources().indirectMagic(actor, actor);
        world.getEntitiesByClass(LivingEntity.class, center.getBoundingBox().expand(radius),
                        target -> HelperMethods.checkAbilityTarget(target, actor))
                .stream().limit(cap).forEach(target -> HelperMethods.damageThroughIframes(target, source,
                        HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, damage)));
    }

    public static boolean isActive(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        return getActive(world, actor.getUuid()) != null;
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveReprise> active = ACTIVE.get(world);
        return active != null && !active.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ActiveReprise> activeByOwner = ACTIVE.get(world);
        if (activeByOwner == null || activeByOwner.isEmpty()) {
            return;
        }

        Iterator<ActiveReprise> iterator = activeByOwner.values().iterator();
        while (iterator.hasNext()) {
            ActiveReprise active = iterator.next();
            Entity ownerEntity = world.getEntity(active.actorId);
            if (!(ownerEntity instanceof LivingEntity actor) || !actor.isAlive() || actor.isRemoved()) {
                applyMissingOwnerCooldown(world, active);
                fadeVisuals(world, active);
                ArcaneCosmicMasteryCombatManager.finish(active.execution, active.affectedTargets);
                iterator.remove();
                continue;
            }

            if (active.phase != PHASE_DETONATING && !isWieldingStarsEdge(actor)) {
                stopForcedMovement(actor);
                applyCooldown(world, actor, active);
                fadeVisuals(world, active);
                spawnCancellationEffects(world, actor);
                ArcaneCosmicMasteryCombatManager.finish(active.execution, active.affectedTargets);
                iterator.remove();
                continue;
            }

            if (tickActive(world, actor, active)) {
                ArcaneCosmicMasteryCombatManager.finish(active.execution, active.affectedTargets);
                iterator.remove();
            }
        }

        if (activeByOwner.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static boolean start(WeaponAbilityContext context, ArcaneCosmicMasteryTuning tuning,
                                 UniqueAbilityExecution execution) {
        LivingEntity actor = context.actor();
        Vec3d direction = resolveDirection(context);
        if (direction.lengthSquared() < 0.0001) {
            return false;
        }

        long now = context.world().getTime();
        ItemStack abilityStack = context.stack().copy();
        float constellationDamage = HelperMethods.abilityScaledDamage(
                "arcane", actor, abilityStack,
                Math.max(0.0F, Config.uniqueEffects.stars_edge.constellationDamageScaling),
                Math.max(0.0F, Config.uniqueEffects.stars_edge.constellationSpellScaling));
        ActiveReprise active = new ActiveReprise(
                actor.getUuid(),
                abilityStack,
                direction,
                actor.getPos(),
                now,
                constellationDamage * (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1),
                tuning,
                execution
        );
        ACTIVE.computeIfAbsent(context.world(), ignored -> new HashMap<>())
                .put(actor.getUuid(), active);
        appendNode(context.world(), active, actor.getPos());
        applyForcedVelocity(actor, direction, Math.max(0.1, Config.uniqueEffects.stars_edge.initialDashSpeed)
                * tuning.get(ArcaneCosmicMasteryTuning.Setting.RANGE, 1));
        spawnActivationEffects(context.world(), actor);
        return true;
    }

    private static boolean tickActive(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        return switch (active.phase) {
            case PHASE_INITIAL_DASH -> tickInitialDash(world, actor, active);
            case PHASE_RECORDING -> tickRecording(world, actor, active);
            case PHASE_DETONATING -> tickDetonation(world, actor, active);
            default -> true;
        };
    }

    private static boolean tickInitialDash(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        Vec3d current = actor.getPos();
        double moved = horizontalDistance(active.previousPosition, current);
        active.dashDistanceTravelled += moved;
        active.previousPosition = current;
        if (recordMovement(world, active, current)) {
            beginDetonation(world, actor, active);
            return false;
        }

        long elapsed = world.getTime() - active.phaseStartedAt;
        double distance = Math.max(0.1, Config.uniqueEffects.stars_edge.initialDashDistance);
        double speed = Math.max(0.1, Config.uniqueEffects.stars_edge.initialDashSpeed);
        int maximumTicks = Math.max(2, (int) Math.ceil(distance / speed) + 4);
        if ((elapsed > 1L && actor.horizontalCollision)
                || active.dashDistanceTravelled >= distance
                || elapsed >= maximumTicks) {
            stopForcedMovement(actor);
            active.phase = PHASE_RECORDING;
            active.phaseStartedAt = world.getTime();
            active.previousPosition = actor.getPos();
            spawnRecordingReadyEffects(world, actor);
            return false;
        }

        applyForcedVelocity(actor, active.initialDirection,
                Math.min(speed, Math.max(0.0, distance - active.dashDistanceTravelled)));
        spawnCometTrail(world, actor);
        return false;
    }

    private static boolean tickRecording(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        Vec3d current = actor.getPos();
        active.previousPosition = current;
        if (recordMovement(world, active, current)) {
            beginDetonation(world, actor, active);
            return false;
        }

        int recordingDuration = Math.max(1, active.tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, Config.uniqueEffects.stars_edge.recordingDuration));
        if (world.getTime() - active.phaseStartedAt >= recordingDuration) {
            sealCurrentEndpoint(world, actor, active);
            beginDetonation(world, actor, active);
        } else if ((actor.age + actor.getId()) % 3 == 0) {
            spawnRecordingMotes(world, actor);
        }
        return false;
    }

    private static boolean tickDetonation(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        long phaseAge = world.getTime() - active.phaseStartedAt;
        if (phaseAge < 8L) {
            spawnCompletionRing(world, active, (int) phaseAge);
        }
        if (active.nextSegmentIndex >= active.nodes.size()) {
            fadeVisuals(world, active);
            return true;
        }
        if (world.getTime() < active.nextExplosionTick) {
            damageConstellationOnContact(world, actor, active, phaseAge);
            return false;
        }

        if (active.tuning.flag(1 << 26)) {
            fadeVisuals(world, active);
            return true;
        }
        if (active.tuning.flag(1 << 25)) {
            Set<UUID> struck = new HashSet<>();
            for (int index = 1; index < active.nodes.size(); index++) {
                RouteNode from = active.nodes.get(index - 1);
                RouteNode to = active.nodes.get(index);
                detonateSegment(world, actor, active, from.position, to.position, struck);
                setVisualPhase(world, from.nodeVisualId, StarsEdgeConstellationVisualEntity.PHASE_DETONATE);
                setVisualPhase(world, to.linkVisualId, StarsEdgeConstellationVisualEntity.PHASE_DETONATE);
                setVisualPhase(world, to.nodeVisualId, StarsEdgeConstellationVisualEntity.PHASE_DETONATE);
            }
            active.nextSegmentIndex = active.nodes.size();
            return true;
        }
        int segmentIndex = active.nextSegmentIndex;
        RouteNode startNode = active.nodes.get(segmentIndex - 1);
        RouteNode endNode = active.nodes.get(segmentIndex);
        detonateSegment(world, actor, active, startNode.position, endNode.position);
        setVisualPhase(world, startNode.nodeVisualId, StarsEdgeConstellationVisualEntity.PHASE_DETONATE);
        setVisualPhase(world, endNode.linkVisualId, StarsEdgeConstellationVisualEntity.PHASE_DETONATE);

        active.nextSegmentIndex++;
        if (active.nextSegmentIndex >= active.nodes.size()) {
            setVisualPhase(world, endNode.nodeVisualId, StarsEdgeConstellationVisualEntity.PHASE_DETONATE);
            return true;
        }

        damageConstellationOnContact(world, actor, active, phaseAge);
        active.nextExplosionTick = world.getTime() + Math.max(3, active.tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS,
                Config.uniqueEffects.stars_edge.segmentExplosionInterval));
        return false;
    }

    private static boolean recordMovement(ServerWorld world, ActiveReprise active, Vec3d current) {
        double spacing = Math.max(0.25, Config.uniqueEffects.stars_edge.nodeSpacing
                * active.tuning.get(ArcaneCosmicMasteryTuning.Setting.WIDTH, 1));
        int maxNodes = Math.clamp(active.tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.COUNT, Config.uniqueEffects.stars_edge.maxNodes), 2, 16);
        RouteNode lastNode = active.nodes.getLast();
        Vec3d remaining = current.subtract(lastNode.position);
        while (remaining.length() >= spacing && active.nodes.size() < maxNodes) {
            Vec3d next = lastNode.position.add(remaining.normalize().multiply(spacing));
            appendNode(world, active, next);
            lastNode = active.nodes.getLast();
            remaining = current.subtract(lastNode.position);
        }
        return active.nodes.size() >= maxNodes;
    }

    private static void sealCurrentEndpoint(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        Vec3d current = actor.getPos();
        recordMovement(world, active, current);
        int maxNodes = Math.clamp(active.tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.COUNT, Config.uniqueEffects.stars_edge.maxNodes), 2, 16);
        if (active.nodes.size() < maxNodes && active.nodes.getLast().position.distanceTo(current) > 0.25) {
            appendNode(world, active, current);
        }
    }

    private static void beginDetonation(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        if (active.phase == PHASE_DETONATING) {
            return;
        }
        stopForcedMovement(actor);
        active.phase = PHASE_DETONATING;
        active.phaseStartedAt = world.getTime();
        active.nextSegmentIndex = 1;
        active.nextExplosionTick = world.getTime()
                + Math.max(1, active.tuning.integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DURATION_TICKS,
                Config.uniqueEffects.stars_edge.constellationDuration));
        applyCooldown(world, actor, active);
        setAllVisualPhases(world, active, StarsEdgeConstellationVisualEntity.PHASE_ARMED);
        spawnCompletionEffects(world, actor, actor.getPos());
    }

    private static void appendNode(ServerWorld world, ActiveReprise active, Vec3d position) {
        StarsEdgeConstellationVisualEntity nodeVisual = StarsEdgeConstellationVisualEntity.node(
                world, position, visualLifetime());
        nodeVisual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(nodeVisual);

        UUID linkVisualId = null;
        if (!active.nodes.isEmpty()) {
            Vec3d previous = active.nodes.getLast().position;
            StarsEdgeConstellationVisualEntity linkVisual = StarsEdgeConstellationVisualEntity.link(
                    world, previous, position, visualLifetime());
            linkVisual.addCommandTag(VISUAL_TAG);
            world.spawnEntity(linkVisual);
            linkVisualId = linkVisual.getUuid();
        }
        active.nodes.add(new RouteNode(position, nodeVisual.getUuid(), linkVisualId));
        spawnNodeEffects(world, position, active.nodes.size());
    }

    private static int visualLifetime() {
        long requested = (long) Math.max(1, Config.uniqueEffects.stars_edge.recordingDuration)
                + Math.max(1, Config.uniqueEffects.stars_edge.constellationDuration)
                + (long) Math.max(2, Config.uniqueEffects.stars_edge.maxNodes)
                * Math.max(1, Config.uniqueEffects.stars_edge.segmentExplosionInterval)
                + 60L;
        return (int) Math.min(Integer.MAX_VALUE - 1L, Math.max(120L, requested));
    }

    private static void setAllVisualPhases(ServerWorld world, ActiveReprise active, int phase) {
        for (RouteNode node : active.nodes) {
            setVisualPhase(world, node.nodeVisualId, phase);
            setVisualPhase(world, node.linkVisualId, phase);
        }
    }

    private static void setVisualPhase(ServerWorld world, UUID visualId, int phase) {
        if (visualId == null) {
            return;
        }
        if (world.getEntity(visualId) instanceof StarsEdgeConstellationVisualEntity visual) {
            visual.setPhase(phase);
            if (phase == StarsEdgeConstellationVisualEntity.PHASE_DETONATE) {
                visual.setLifetime(visual.age + 5);
            } else if (phase == StarsEdgeConstellationVisualEntity.PHASE_FADE) {
                visual.setLifetime(visual.age + 10);
            }
        }
    }

    private static void fadeVisuals(ServerWorld world, ActiveReprise active) {
        setAllVisualPhases(world, active, StarsEdgeConstellationVisualEntity.PHASE_FADE);
    }

    private static void detonateSegment(ServerWorld world, LivingEntity actor, ActiveReprise active,
                                        Vec3d start, Vec3d end) {
        detonateSegment(world, actor, active, start, end, null);
    }

    private static void detonateSegment(ServerWorld world, LivingEntity actor, ActiveReprise active,
                                        Vec3d start, Vec3d end, Set<UUID> struck) {
        damageSegmentExplosion(world, actor, active, start, end,
                Math.max(0.1, active.tuning.get(ArcaneCosmicMasteryTuning.Setting.RADIUS,
                        Config.uniqueEffects.stars_edge.segmentExplosionRadius)),
                active.constellationDamage, struck);
        spawnSegmentExplosionEffects(world, actor, start, end);
    }

    private static void damageConstellationOnContact(ServerWorld world, LivingEntity actor,
                                                      ActiveReprise active, long phaseAge) {
        int interval = Math.max(1, active.tuning.integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_INTERVAL_TICKS,
                Config.uniqueEffects.stars_edge.constellationDamageInterval));
        if (phaseAge % interval != 0L || active.constellationDamage <= 0.0F) {
            return;
        }

        double width = Math.max(0.1, active.tuning.get(ArcaneCosmicMasteryTuning.Setting.SECONDARY_RADIUS,
                Config.uniqueEffects.stars_edge.constellationDamageWidth));
        Set<UUID> pulseHitTargets = new HashSet<>();
        for (int segmentIndex = active.nextSegmentIndex;
             segmentIndex < active.nodes.size();
             segmentIndex++) {
            damageContactSegment(world, actor, active,
                    active.nodes.get(segmentIndex - 1).position,
                    active.nodes.get(segmentIndex).position,
                    width,
                    pulseHitTargets);
        }
    }

    private static void damageContactSegment(ServerWorld world, LivingEntity actor, ActiveReprise active,
                                             Vec3d start, Vec3d end, double width,
                                             Set<UUID> pulseHitTargets) {
        if (start.squaredDistanceTo(end) < 0.0001) {
            return;
        }

        Vec3d bodyStart = start.add(0.0, 0.85, 0.0);
        Vec3d bodyEnd = end.add(0.0, 0.85, 0.0);
        double halfWidth = width * 0.5;
        Box search = new Box(bodyStart, bodyEnd).expand(halfWidth + 1.0);

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, search,
                target -> target != actor
                        && target.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && !pulseHitTargets.contains(target.getUuid())
                        && HelperMethods.checkAbilityTarget(target, actor))) {
            Box targetBox = target.getBoundingBox();
            double lineMidY = (bodyStart.y + bodyEnd.y) * 0.5;
            Vec3d targetCenter = new Vec3d(
                    target.getX(),
                    Math.clamp(lineMidY, targetBox.minY, targetBox.maxY),
                    target.getZ());
            double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
            double allowed = halfWidth + targetRadius;
            if (distanceSquaredToSegment(targetCenter, bodyStart, bodyEnd) > allowed * allowed) {
                continue;
            }
            if (!pulseHitTargets.add(target.getUuid())) {
                continue;
            }
            damageTarget(world, actor, active.stack, target, active.constellationDamage, true);
            active.affectedTargets++;
            if (active.tuning.flag(1 << 24)) target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 30, 1), actor);
        }
    }

    private static void damageSegmentExplosion(ServerWorld world, LivingEntity actor, ActiveReprise active,
                                               Vec3d start, Vec3d end, double radius,
                                               float baseDamage, Set<UUID> shared) {
        if (baseDamage <= 0.0F || start.squaredDistanceTo(end) < 0.0001) {
            return;
        }

        Vec3d bodyStart = start.add(0.0, 0.85, 0.0);
        Vec3d bodyEnd = end.add(0.0, 0.85, 0.0);
        Box search = new Box(bodyStart, bodyEnd).expand(radius + 1.0);
        Set<UUID> segmentHitTargets = shared == null ? new HashSet<>() : shared;

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, search,
                target -> target != actor
                        && target.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && HelperMethods.checkAbilityTarget(target, actor))) {
            Box targetBox = target.getBoundingBox();
            double lineMidY = (bodyStart.y + bodyEnd.y) * 0.5;
            Vec3d targetCenter = new Vec3d(
                    target.getX(),
                    Math.clamp(lineMidY, targetBox.minY, targetBox.maxY),
                    target.getZ());
            double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
            double allowed = radius + targetRadius;
            if (distanceSquaredToSegment(targetCenter, bodyStart, bodyEnd) > allowed * allowed) {
                continue;
            }
            if (!segmentHitTargets.add(target.getUuid())) {
                continue;
            }
            damageTarget(world, actor, active.stack, target, baseDamage, false);
            active.affectedTargets++;
        }
    }

    private static void spawnSegmentExplosionEffects(ServerWorld world, LivingEntity actor,
                                                      Vec3d start, Vec3d end) {
        Vec3d raisedStart = start.add(0.0, 0.85, 0.0);
        Vec3d raisedEnd = end.add(0.0, 0.85, 0.0);
        double length = raisedStart.distanceTo(raisedEnd);
        int samples = Math.max(3, (int) Math.ceil(length * 3.0));
        for (int i = 0; i <= samples; i++) {
            Vec3d point = raisedStart.lerp(raisedEnd, i / (double) samples);
            world.spawnParticles(ParticleTypes.FIREWORK, point.x, point.y, point.z,
                    7, 0.2, 0.24, 0.2, 0.08);
            world.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
                    4, 0.16, 0.2, 0.16, 0.045);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y, point.z,
                    5, 0.22, 0.24, 0.22, 0.06);
        }

        Vec3d midpoint = raisedStart.lerp(raisedEnd, 0.5);
        double radius = Math.max(0.1, Config.uniqueEffects.stars_edge.segmentExplosionRadius);
        int ringPoints = Math.max(24, (int) Math.ceil(radius * 14.0));
        for (int i = 0; i < ringPoints; i++) {
            double angle = Math.PI * 2.0 * i / ringPoints;
            world.spawnParticles(ParticleTypes.FIREWORK,
                    midpoint.x + Math.cos(angle) * radius,
                    midpoint.y,
                    midpoint.z + Math.sin(angle) * radius,
                    1, 0.015, 0.025, 0.015, 0.02);
        }
        world.playSound(null, midpoint.x, midpoint.y, midpoint.z,
                SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                actor.getSoundCategory(), 0.75F, 1.05F + world.random.nextFloat() * 0.2F);
        world.playSound(null, midpoint.x, midpoint.y, midpoint.z,
                SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                actor.getSoundCategory(), 0.55F, 1.35F + world.random.nextFloat() * 0.2F);
    }

    private static void damageTarget(ServerWorld world, LivingEntity actor, ItemStack stack,
                                     LivingEntity target, float baseDamage, boolean preserveVelocity) {
        DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(
                world, stack, target, source, baseDamage);
        Vec3d previousVelocity = preserveVelocity ? target.getVelocity() : null;
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage));
        if (damaged[0] && preserveVelocity) {
            target.setVelocity(previousVelocity);
            target.velocityModified = true;
            target.velocityDirty = true;
        }
        Vec3d center = target.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z,
                10, 0.28, 0.32, 0.28, 0.05);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                8, 0.24, 0.28, 0.24, 0.04);
    }

    private static double distanceSquaredToSegment(Vec3d point, Vec3d start, Vec3d end) {
        Vec3d segment = end.subtract(start);
        double lengthSquared = segment.lengthSquared();
        if (lengthSquared < 0.0001) {
            return point.squaredDistanceTo(start);
        }
        double projection = point.subtract(start).dotProduct(segment) / lengthSquared;
        double t = Math.clamp(projection, 0.0, 1.0);
        return point.squaredDistanceTo(start.add(segment.multiply(t)));
    }

    private static Vec3d resolveDirection(WeaponAbilityContext context) {
        Vec3d direction;
        if (!(context.actor() instanceof PlayerEntity)
                && context.target() != null
                && context.target().isAlive()) {
            direction = context.target().getPos().subtract(context.actor().getPos());
        } else {
            direction = context.facing();
        }
        Vec3d horizontal = direction == null ? Vec3d.ZERO : new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() < 0.0001) {
            horizontal = Vec3d.fromPolar(0.0F, context.actor().getYaw());
        }
        return horizontal.normalize();
    }

    private static void applyForcedVelocity(LivingEntity actor, Vec3d direction, double speed) {
        actor.setVelocity(direction.multiply(speed));
        actor.fallDistance = 0.0F;
        actor.velocityModified = true;
    }

    private static void stopForcedMovement(LivingEntity actor) {
        if (actor == null) {
            return;
        }
        actor.setVelocity(Vec3d.ZERO);
        actor.velocityModified = true;
    }

    private static boolean isWieldingStarsEdge(LivingEntity actor) {
        return actor.getMainHandStack().isOf(ItemsRegistry.STARS_EDGE.get())
                || actor.getOffHandStack().isOf(ItemsRegistry.STARS_EDGE.get());
    }

    private static void applyCooldown(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        if (active.cooldownApplied) {
            return;
        }
        active.cooldownApplied = true;
        int cooldown = Math.max(1, Config.uniqueEffects.stars_edge.cooldown);
        SimplySwordsAPI.setWeaponCooldown(actor, active.stack, cooldown);
    }

    private static void applyMissingOwnerCooldown(ServerWorld world, ActiveReprise active) {
        if (active.cooldownApplied) {
            return;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(active.actorId);
        if (player != null) {
            applyCooldown(player.getServerWorld(), player, active);
        }
    }

    private static ActiveReprise getActive(ServerWorld world, UUID actorId) {
        Map<UUID, ActiveReprise> active = ACTIVE.get(world);
        return active == null ? null : active.get(actorId);
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double dx = second.x - first.x;
        double dz = second.z - first.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z,
                28, 0.42, 0.5, 0.42, 0.12);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                22, 0.38, 0.45, 0.38, 0.08);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_FLYBY_01.get(),
                actor.getSoundCategory(), 0.65F, 1.2F);
    }

    private static void spawnRecordingReadyEffects(ServerWorld world, LivingEntity actor) {
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                actor.getSoundCategory(), 0.35F, 1.65F);
    }

    private static void spawnNodeEffects(ServerWorld world, Vec3d position, int index) {
        world.spawnParticles(ParticleTypes.END_ROD, position.x, position.y + 0.85, position.z,
                7, 0.12, 0.18, 0.12, 0.025);
        world.playSound(null, position.x, position.y, position.z,
                SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                net.minecraft.sound.SoundCategory.PLAYERS, 0.18F,
                Math.min(1.9F, 1.05F + index * 0.08F));
    }

    private static void spawnRecordingMotes(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                2, 0.22, 0.28, 0.22, 0.015);
    }

    private static void spawnCometTrail(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z,
                5, 0.16, 0.22, 0.16, 0.025);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                3, 0.18, 0.2, 0.18, 0.035);
    }

    private static void spawnCompletionRing(ServerWorld world, ActiveReprise active, int age) {
        Vec3d center = active.nodes.isEmpty() ? active.previousPosition : active.nodes.getFirst().position;
        double maximumRadius = 2.5;
        for (RouteNode node : active.nodes) {
            double dx = node.position.x - center.x;
            double dz = node.position.z - center.z;
            maximumRadius = Math.max(maximumRadius, Math.sqrt(dx * dx + dz * dz));
        }
        maximumRadius = Math.min(9.0, maximumRadius + 1.25);
        double radius = maximumRadius * (age + 1.0) / 8.0;
        int points = Math.max(24, (int) Math.ceil(radius * 10.0));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points + age * 0.14;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.END_ROD, x, center.y + 0.16, z,
                    1, 0.01, 0.02, 0.01, 0.01);
            if ((i + age) % 3 == 0) {
                world.spawnParticles(ParticleTypes.FIREWORK, x, center.y + 0.22, z,
                        1, 0.015, 0.025, 0.015, 0.015);
            }
        }
    }

    private static void spawnCompletionEffects(ServerWorld world, LivingEntity actor, Vec3d origin) {
        world.spawnParticles(ParticleTypes.FIREWORK, origin.x, origin.y + 0.7, origin.z,
                64, 1.0, 0.7, 1.0, 0.16);
        world.spawnParticles(ParticleTypes.END_ROD, origin.x, origin.y + 0.7, origin.z,
                42, 0.85, 0.55, 0.85, 0.11);
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                actor.getSoundCategory(), 0.95F, 0.8F);
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                actor.getSoundCategory(), 0.75F, 1.15F);
    }

    private static void spawnCancellationEffects(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                12, 0.3, 0.35, 0.3, 0.02);
    }

    private static final class ActiveReprise {
        private final UUID actorId;
        private final ItemStack stack;
        private final Vec3d initialDirection;
        private final float constellationDamage;
        private final List<RouteNode> nodes = new ArrayList<>();
        private int phase = PHASE_INITIAL_DASH;
        private long phaseStartedAt;
        private Vec3d previousPosition;
        private double dashDistanceTravelled;
        private int nextSegmentIndex;
        private long nextExplosionTick;
        private boolean cooldownApplied;
        private int affectedTargets;
        private final ArcaneCosmicMasteryTuning tuning;
        private final UniqueAbilityExecution execution;

        private ActiveReprise(UUID actorId, ItemStack stack, Vec3d initialDirection,
                              Vec3d previousPosition, long startedAt,
                              float constellationDamage, ArcaneCosmicMasteryTuning tuning,
                              UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.stack = stack;
            this.initialDirection = initialDirection;
            this.previousPosition = previousPosition;
            this.phaseStartedAt = startedAt;
            this.constellationDamage = constellationDamage;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private static final class RepriseState {
        private int solarHits;
        private int solarChain;
        private int solarCharge;
        private long lastSolarHit;
        private long guardAt;
        private UUID markedTarget;
        private long markExpiresAt;
        private boolean ambushReady;
        private int nightRefunded;
    }

    private record RouteNode(Vec3d position, UUID nodeVisualId, UUID linkVisualId) {
    }
}
