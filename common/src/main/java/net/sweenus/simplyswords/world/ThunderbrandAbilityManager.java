package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ThunderbrandAbilityManager {
    private static final int MAX_STORED_DAMAGE_INSTANCES = 15;
    private static final double BASE_TURN_RADIANS = Math.toRadians(12);
    private static final Map<ServerWorld, Map<UUID, ActiveThunderBlitz>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Memory>> MEMORIES = new HashMap<>();
    private static final Map<ServerWorld, Map<MarkKey, Long>> MARKS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, AbsorptionGrant>> ABSORPTION = new HashMap<>();

    private ThunderbrandAbilityManager() {
    }

    public static int refreshChance(ServerWorld world, ItemStack stack, LivingEntity actor, LivingEntity target) {
        int configured = Math.clamp(Config.uniqueEffects.thunderbrand.chance, 0, 100);
        if (world == null || stack == null || stack.isEmpty() || actor == null) return configured;
        UniqueAbilityExecution execution = StormFrostWaterMasteryCombatManager.beginPassive(
                StormFrostWaterMasteryAbilities.THUNDERBRAND_REFRESH, world, stack, actor, target);
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryAbilities.tuning(execution);
        int result = Math.clamp(tuning.integer(s("CHANCE"), configured)
                + tuning.integer(s("THUNDERBRAND_REFRESH_CHANCE_BONUS"), 0), 0, 100);
        UniqueAbilityApi.finish(execution, StormFrostWaterMasteryAbilities.FINISH, 0);
        return result;
    }

    public static boolean start(WeaponAbilityContext context) {
        if (context == null || context.world() == null || context.actor() == null
                || !context.actor().isAlive() || context.stack() == null || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.THUNDERBRAND.get())) return false;
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        Map<UUID, ActiveThunderBlitz> active = ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>());
        if (active.containsKey(actor.getUuid())) return false;

        UniqueAbilityExecution execution = StormFrostWaterMasteryCombatManager.beginActive(
                StormFrostWaterMasteryAbilities.THUNDERBRAND_BLITZ, context, Config.uniqueEffects.thunderbrand.cooldown);
        UniqueAbilityApi.start(execution);
        ActiveThunderBlitz ability = new ActiveThunderBlitz(actor.getUuid(),
                isValidTarget(context.target(), actor) ? context.target().getUuid() : null,
                context.stack().copy(), context.hand() == null ? Hand.MAIN_HAND : context.hand(),
                horizontalDirection(context.facing(), actor), world.getTime(),
                StormFrostWaterMasteryAbilities.tuning(execution), execution);
        Memory memory = removeMemory(world, actor.getUuid());
        if (memory != null && memory.expiresAt >= world.getTime()) {
            ability.storedDamageInstances = Math.min(storedHitCap(ability),
                    memory.ability.storedDamageInstances);
            cancel(memory.ability.execution);
        } else if (memory != null) {
            cancel(memory.ability.execution);
        }
        active.put(actor.getUuid(), ability);
        applyChargeEffects(actor, ability);
        spawnChargeStartEffects(world, actor);
        return true;
    }

    public static boolean resume(ServerWorld world, ServerPlayerEntity actor, Hand hand, ItemStack stack) {
        if (world == null || actor == null || hand == null || stack == null || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.THUNDERBRAND.get()) || isActive(actor)) return false;
        Memory memory = removeMemory(world, actor.getUuid());
        if (memory == null || memory.expiresAt < world.getTime() || memory.ability.execution.isTerminal()) {
            if (memory != null) cancel(memory.ability.execution);
            return false;
        }
        ActiveThunderBlitz ability = memory.ability;
        ability.phase = Phase.CHARGING;
        ability.startedAt = world.getTime();
        ability.hand = hand;
        ability.fallbackDirection = horizontalDirection(actor.getRotationVec(1), actor);
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), ability);
        applyChargeEffects(actor, ability);
        spawnChargeStartEffects(world, actor);
        return true;
    }

    public static boolean isActive(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return false;
        Map<UUID, ActiveThunderBlitz> active = ACTIVE.get(world);
        return active != null && active.containsKey(actor.getUuid());
    }

    public static boolean handleIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (actor == null || source == null || amount <= 0 || !(actor.getWorld() instanceof ServerWorld world)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || actor instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) return false;
        ActiveThunderBlitz ability = state(world, actor.getUuid());
        if (ability == null || !isStillHolding(actor, ability)) return false;
        boolean grounded = ability.tuning.flag(1 << 16);
        if (ability.phase == Phase.DASHING) {
            return world.getTime() < ability.defenseUntil && !grounded;
        }
        if (ability.phase != Phase.CHARGING) return false;
        if (ability.tuning.flag(1 << 17)) return true;

        int cap = storedHitCap(ability);
        if (ability.storedDamageInstances < cap) ability.storedDamageInstances++;
        if (!grounded) retaliate(source, ability);
        spawnAbsorbEffects(world, actor, ability.storedDamageInstances);
        return !grounded;
    }

    public static void cancelCharging(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        Map<UUID, ActiveThunderBlitz> active = ACTIVE.get(world);
        ActiveThunderBlitz ability = active == null ? null : active.get(actor.getUuid());
        if (ability == null || ability.phase != Phase.CHARGING) return;
        active.remove(actor.getUuid());
        if (active.isEmpty()) ACTIVE.remove(world);
        removeChargeEffects(actor, ability, world.getTime());
        int memoryTicks = ability.tuning.integer(s("THUNDERBRAND_MEMORY_TICKS"), 0);
        if (memoryTicks > 0) {
            MEMORIES.computeIfAbsent(world, ignored -> new HashMap<>())
                    .put(actor.getUuid(), new Memory(ability, world.getTime() + memoryTicks));
        } else {
            cancel(ability.execution);
        }
        spawnCancelledEffects(world, actor);
    }

    public static boolean hasActive(ServerWorld world) {
        return has(ACTIVE.get(world)) || has(MEMORIES.get(world)) || has(MARKS.get(world))
                || has(ABSORPTION.get(world));
    }

    public static void tick(ServerWorld world) {
        long now = world.getTime();
        sweepMemory(world, now);
        sweepMarks(world, now);
        sweepAbsorption(world, now);
        Map<UUID, ActiveThunderBlitz> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) return;
        Iterator<ActiveThunderBlitz> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveThunderBlitz ability = iterator.next();
            LivingEntity actor = world.getEntity(ability.actorId) instanceof LivingEntity living ? living : null;
            if (actor == null || !actor.isAlive() || !isStillHolding(actor, ability)
                    || ability.phase == Phase.CHARGING && !isPlayerStillCharging(actor, ability)) {
                cleanupAbility(world, actor, ability, true);
                clearOwnedState(world, ability.actorId, actor);
                iterator.remove();
                continue;
            }
            if (ability.phase == Phase.CHARGING) tickCharging(world, actor, ability, now);
            if (ability.phase == Phase.DASHING && !tickDashing(world, actor, ability, now)) iterator.remove();
        }
        if (active.isEmpty()) ACTIVE.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        Map<UUID, ActiveThunderBlitz> active = ACTIVE.get(world);
        ActiveThunderBlitz ability = active == null ? null : active.remove(actor.getUuid());
        if (ability != null) cleanupAbility(world, actor, ability, true);
        Memory memory = removeMemory(world, actor.getUuid());
        if (memory != null) cancel(memory.ability.execution);
        clearOwnedState(world, actor.getUuid(), actor);
        if (active != null && active.isEmpty()) ACTIVE.remove(world);
    }

    public static void clear(ServerWorld world) {
        Map<UUID, ActiveThunderBlitz> active = ACTIVE.remove(world);
        if (active != null) active.values().forEach(ability -> {
            LivingEntity actor = world.getEntity(ability.actorId) instanceof LivingEntity living ? living : null;
            cleanupAbility(world, actor, ability, true);
        });
        Map<UUID, Memory> memories = MEMORIES.remove(world);
        if (memories != null) memories.values().forEach(memory -> cancel(memory.ability.execution));
        Map<UUID, AbsorptionGrant> grants = ABSORPTION.remove(world);
        if (grants != null) grants.forEach((id, grant) -> {
            if (world.getEntity(id) instanceof LivingEntity actor) removeGrant(actor, grant);
        });
        MARKS.remove(world);
    }

    public static void clearAll() {
        Set<ServerWorld> worlds = new HashSet<>();
        worlds.addAll(ACTIVE.keySet());
        worlds.addAll(MEMORIES.keySet());
        worlds.addAll(MARKS.keySet());
        worlds.addAll(ABSORPTION.keySet());
        worlds.forEach(ThunderbrandAbilityManager::clear);
    }

    private static void tickCharging(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability, long now) {
        if ((now - ability.startedAt) % 3 == 0) spawnChargingEffects(world, actor, ability.storedDamageInstances);
        if (now - ability.startedAt >= chargeDuration(ability)) beginDash(world, actor, ability, now);
    }

    private static void beginDash(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability, long now) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(ability == null ? null : CombatProvenanceApi.from(ability.stack, null))) {
        ability.phase = Phase.DASHING;
        ability.dashStartedAt = now;
        ability.previousPosition = actor.getPos();
        ability.dashDirection = resolveDashDirection(world, actor, ability);
        ability.defenseUntil = now + ability.tuning.integer(s("THUNDERBRAND_DEFENSE_PADDING_TICKS"), 0);
        removeChargeEffects(actor, ability, now);
        grantReactiveAbsorption(world, actor, ability);
        applyDashVelocity(actor, ability);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 2), actor);
        if (actor instanceof ServerPlayerEntity player) {
            ItemStack currentStack = player.getStackInHand(ability.hand);
            if (!PlayerWeaponAbilityChannelManager.finishEarly(player, currentStack) && player.isUsingItem()) {
                player.stopUsingItem();
            }
        }
        spawnDashStartEffects(world, actor, ability.storedDamageInstances);
        }
    }

    private static boolean tickDashing(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability, long now) {
        int duration = dashDuration(ability);
        int dashTick = (int) Math.max(0, now - ability.dashStartedAt);
        if (dashTick >= duration) {
            damageDashTargets(world, actor, ability);
            releaseBlitzBurst(world, actor, ability);
            stopDashMovement(actor);
            spawnDashEndEffects(world, actor);
            refundAtCap(actor, ability);
            UniqueAbilityApi.finish(ability.execution, StormFrostWaterMasteryAbilities.FINISH, ability.dashHitTargets.size());
            return false;
        }
        steer(world, actor, ability);
        damageDashTargets(world, actor, ability);
        releaseScheduledChains(world, actor, ability, dashTick, duration);
        ability.previousPosition = actor.getPos();
        return true;
    }

    private static void steer(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        double steering = ability.tuning.get(s("THUNDERBRAND_STEERING_MULTIPLIER"), 1);
        if (steering <= 0) return;
        Vec3d desired = resolveDashDirection(world, actor, ability);
        ability.dashDirection = turnToward(ability.dashDirection, desired, BASE_TURN_RADIANS * steering);
        applyDashVelocity(actor, ability);
    }

    private static void applyDashVelocity(LivingEntity actor, ActiveThunderBlitz ability) {
        double speed = ability.tuning.get(s("SPEED"), Config.uniqueEffects.thunderbrand.dashSpeed)
                * ability.tuning.get(s("THUNDERBRAND_DASH_SPEED_MULTIPLIER"), 1);
        actor.setVelocity(ability.dashDirection.x * Math.max(.1, speed), 0, ability.dashDirection.z * Math.max(.1, speed));
        actor.velocityModified = true;
    }

    private static void damageDashTargets(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        Vec3d current = actor.getPos();
        Vec3d previous = ability.previousPosition == null ? current : ability.previousPosition;
        Box currentBox = actor.getBoundingBox();
        Box previousBox = currentBox.offset(previous.subtract(current));
        double radius = collisionRadius(ability);
        Box swept = new Box(Math.min(currentBox.minX, previousBox.minX) - radius,
                Math.min(currentBox.minY, previousBox.minY) - Math.max(.5, radius * .5),
                Math.min(currentBox.minZ, previousBox.minZ) - radius,
                Math.max(currentBox.maxX, previousBox.maxX) + radius,
                Math.max(currentBox.maxY, previousBox.maxY) + Math.max(.5, radius * .5),
                Math.max(currentBox.maxZ, previousBox.maxZ) + radius);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, swept,
                target -> validTarget(actor, target) && !ability.dashHitTargets.contains(target.getUuid()))) {
            ability.dashHitTargets.add(target.getUuid());
            float damage = dashDamage(world, actor, target, ability);
            DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
            if (damageSuppressed(ability.stack, actor, target, source, damage)) {
                if (ability.tuning.flag(1 << 21)) mark(world, actor, target, ability);
                spawnDashHitEffects(world, target);
                UniqueAbilityApi.emit(ability.execution, UniqueAbilityPhase.HIT, StormFrostWaterMasteryAbilities.HIT,
                        target, 1, damage);
            }
        }
    }

    private static float dashDamage(ServerWorld world, LivingEntity actor, LivingEntity target,
                                    ActiveThunderBlitz ability) {
        float multiplier = 3 * (float) ability.tuning.get(s("DAMAGE_MULTIPLIER"), 1)
                * (float) ability.tuning.get(s("THUNDERBRAND_COLLISION_DAMAGE_MULTIPLIER"), 1);
        int threshold = ability.tuning.integer(s("THUNDERBRAND_OVERLOAD_THRESHOLD"), 0);
        if (threshold > 0 && ability.storedDamageInstances >= threshold) {
            multiplier *= (float) ability.tuning.get(s("THUNDERBRAND_OVERLOAD_DAMAGE_MULTIPLIER"), 1);
        }
        if (ability.tuning.flag(1 << 16)) {
            multiplier *= 1 + ability.storedDamageInstances
                    * (float) ability.tuning.get(s("THUNDERBRAND_GROUNDED_DAMAGE_PER_HIT"), .12);
        }
        if (ability.tuning.flag(1 << 17)) {
            multiplier *= (float) ability.tuning.get(s("THUNDERBRAND_INSULATOR_DAMAGE_MULTIPLIER"), 1.4);
        }
        DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
        return HelperMethods.applyAbilityDamageEnchantments(world, ability.stack, target, source,
                baseDamage(actor, ability) * multiplier);
    }

    private static void releaseScheduledChains(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability,
                                               int dashTick, int dashDuration) {
        int stored = Math.min(ability.storedDamageInstances, storedHitCap(ability));
        if (stored <= 0 || ability.releasesFinished) return;
        if (ability.tuning.flag(1 << 25)) {
            if (dashTick >= scheduledDashTick(0, 1, dashDuration)) {
                releaseRailbolt(world, actor, ability, stored);
                finishReleases(world, actor, ability);
            }
            return;
        }
        int chainsPerHit = Math.max(1, ability.tuning.integer(s("THUNDERBRAND_WEB_CHAIN_COUNT"), 1));
        while (ability.storedHitsReleased < stored
                && scheduledDashTick(ability.storedHitsReleased, stored, dashDuration) <= dashTick) {
            for (int i = 0; i < chainsPerHit; i++) releaseChain(world, actor, ability);
            ability.storedHitsReleased++;
        }
        if (ability.storedHitsReleased >= stored) finishReleases(world, actor, ability);
    }

    private static void releaseChain(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        boolean web = ability.tuning.flag(1 << 26);
        Set<UUID> excluded = web ? ability.chainHitTargets : Set.of();
        LivingEntity first = findNext(world, actor, actor.getPos(), chainRange(ability), excluded);
        if (first == null) {
            spawnDissipatedChainEffects(world, actor);
            return;
        }
        List<LivingEntity> targets = buildChain(world, actor, first, chainTargetCap(ability),
                chainRange(ability), excluded);
        List<Vec3d> points = new ArrayList<>();
        points.add(midpoint(actor));
        float damage = chainDamage(actor, ability);
        if (web) damage *= (float) ability.tuning.get(s("THUNDERBRAND_WEB_DAMAGE_MULTIPLIER"), .55);
        for (LivingEntity target : targets) {
            damageChainTarget(world, actor, target, ability, damage);
            if (web) ability.chainHitTargets.add(target.getUuid());
            points.add(midpoint(target));
        }
        LivingEntity finalTarget = targets.getLast();
        LivingEntity cross = crosscurrentTarget(world, actor, finalTarget, ability, new HashSet<>(targets));
        if (cross != null) {
            float crossDamage = damage * (float) ability.tuning.get(
                    s("THUNDERBRAND_CROSSCURRENT_DAMAGE_MULTIPLIER"), .4);
            damageChainTarget(world, actor, cross, ability, crossDamage);
            if (web) ability.chainHitTargets.add(cross.getUuid());
            points.add(midpoint(cross));
            finalTarget = cross;
        }
        ChainLightningVisualManager.spawnChain(world, points, ChainLightningVisualManager.STORMBRINGER_SETTINGS);
        releaseArcWake(world, actor, finalTarget, ability);
    }

    private static void releaseRailbolt(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability, int stored) {
        Vec3d start = midpoint(actor);
        Vec3d end = start.add(ability.dashDirection.multiply(chainRange(ability)));
        int storedCap = ability.tuning.integer(s("THUNDERBRAND_RAIL_HIT_CAP"), 6);
        int targetCap = chainTargetCap(ability);
        List<LivingEntity> targets = lineTargets(world, actor, start, end, targetCap);
        float damage = chainDamage(actor, ability) * (float) ability.tuning.get(
                s("THUNDERBRAND_RAIL_DAMAGE_PER_HIT"), .3) * Math.min(stored, storedCap);
        for (LivingEntity target : targets) {
            damageChainTarget(world, actor, target, ability, damage);
            if (ability.tuning.flag(1 << 26)) ability.chainHitTargets.add(target.getUuid());
        }
        ChainLightningVisualManager.spawnBolt(world, start, end, ChainLightningVisualManager.STORMBRINGER_SETTINGS);
        if (ability.tuning.flag(1 << 22)) {
            float returnDamage = damage * (float) ability.tuning.get(
                    s("THUNDERBRAND_CROSSCURRENT_DAMAGE_MULTIPLIER"), .4);
            for (int i = targets.size() - 1; i >= 0; i--) {
                damageChainTarget(world, actor, targets.get(i), ability, returnDamage);
            }
            ChainLightningVisualManager.spawnBolt(world, end, start, ChainLightningVisualManager.STORMBRINGER_SETTINGS);
        }
        if (!targets.isEmpty()) releaseArcWake(world, actor, targets.getLast(), ability);
    }

    private static boolean damageChainTarget(ServerWorld world, LivingEntity actor, LivingEntity target,
                                             ActiveThunderBlitz ability, float damage) {
        if (ability.tuning.flag(1 << 26) && ability.chainHitTargets.contains(target.getUuid())) return false;
        float resolved = damage;
        if (isMarked(world, actor, target)) {
            resolved *= (float) ability.tuning.get(s("THUNDERBRAND_CONDUCTOR_DAMAGE_MULTIPLIER"), 1.2);
        }
        boolean damaged = ChainLightningVisualManager.damageBoltTargetWithoutKnockback(
                world, actor, ability.stack, target, resolved);
        if (damaged) UniqueAbilityApi.emit(ability.execution, UniqueAbilityPhase.HIT,
                StormFrostWaterMasteryAbilities.HIT, target, 1, resolved);
        return damaged;
    }

    private static void releaseArcWake(ServerWorld world, LivingEntity actor, LivingEntity anchor,
                                       ActiveThunderBlitz ability) {
        double radius = ability.tuning.get(s("THUNDERBRAND_ARC_WAKE_RADIUS"), 0);
        int cap = ability.tuning.integer(s("THUNDERBRAND_ARC_WAKE_TARGET_CAP"), 0);
        if (radius <= 0 || cap <= 0 || anchor == null) return;
        float damage = baseDamage(actor, ability)
                * (float) ability.tuning.get(s("THUNDERBRAND_ARC_WAKE_DAMAGE_MULTIPLIER"), .2)
                * (float) ability.tuning.get(s("THUNDERBRAND_BURST_DAMAGE_MULTIPLIER"), 1);
        List<LivingEntity> targets = areaTargets(world, actor, anchor.getPos(), radius, cap,
                Set.of(anchor.getUuid()), ability.tuning.flag(1 << 26) ? ability.chainHitTargets : Set.of());
        for (LivingEntity target : targets) {
            damageChainTarget(world, actor, target, ability, damage);
            if (ability.tuning.flag(1 << 26)) ability.chainHitTargets.add(target.getUuid());
            ChainLightningVisualManager.spawnBolt(world, midpoint(anchor), midpoint(target),
                    ChainLightningVisualManager.STORMBRINGER_SETTINGS);
        }
    }

    private static void finishReleases(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        ability.releasesFinished = true;
        double radius = ability.tuning.get(s("THUNDERBRAND_FINAL_RADIUS"), 0);
        int cap = ability.tuning.integer(s("THUNDERBRAND_FINAL_TARGET_CAP"), 0);
        if (radius <= 0 || cap <= 0) return;
        float damage = baseDamage(actor, ability)
                * (float) ability.tuning.get(s("THUNDERBRAND_FINAL_DAMAGE_MULTIPLIER"), .6)
                * (float) ability.tuning.get(s("THUNDERBRAND_BURST_DAMAGE_MULTIPLIER"), 1);
        Set<UUID> webExcluded = ability.tuning.flag(1 << 26) ? ability.chainHitTargets : Set.of();
        List<LivingEntity> targets = areaTargets(world, actor, actor.getPos(), radius, cap, Set.of(), webExcluded);
        for (LivingEntity target : targets) {
            if (damageChainTarget(world, actor, target, ability, damage)) ability.chainHitTargets.add(target.getUuid());
            ChainLightningVisualManager.spawnBolt(world, midpoint(actor), midpoint(target),
                    ChainLightningVisualManager.STORMBRINGER_SETTINGS);
        }
        UniqueAbilityApi.emit(ability.execution, UniqueAbilityPhase.HIT, StormFrostWaterMasteryAbilities.PULSE,
                null, targets.size(), damage);
    }

    private static void releaseBlitzBurst(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        float multiplier = ability.tuning.has(s("THUNDERBRAND_BLITZ_BURST_DAMAGE_MULTIPLIER"))
                ? (float) ability.tuning.get(s("THUNDERBRAND_BLITZ_BURST_DAMAGE_MULTIPLIER"), 0)
                : (float) ability.tuning.get(s("FINAL_DAMAGE_MULTIPLIER"), 0);
        if (ability.tuning.flag(1 << 7)) multiplier = 1;
        multiplier *= (float) ability.tuning.get(s("THUNDERBRAND_BURST_DAMAGE_MULTIPLIER"), 1);
        if (multiplier <= 0) return;
        double radius = ability.tuning.get(s("THUNDERBRAND_BLITZ_BURST_RADIUS"), 3);
        int cap = ability.tuning.integer(s("THUNDERBRAND_BLITZ_BURST_TARGET_CAP"), 10);
        float damage = baseDamage(actor, ability) * multiplier;
        List<LivingEntity> targets = areaTargets(world, actor, actor.getPos(), radius, cap, Set.of(), Set.of());
        DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
        int affected = 0;
        for (LivingEntity target : targets) {
            float enchanted = HelperMethods.applyAbilityDamageEnchantments(world, ability.stack, target, source, damage);
            if (damageSuppressed(ability.stack, actor, target, source, enchanted)) affected++;
        }
        UniqueAbilityApi.emit(ability.execution, UniqueAbilityPhase.HIT, StormFrostWaterMasteryAbilities.PULSE,
                null, affected, damage);
    }

    private static void refundAtCap(LivingEntity actor, ActiveThunderBlitz ability) {
        int refund = ability.tuning.integer(s("THUNDERBRAND_CAP_REFUND_TICKS"), 0);
        int cap = storedHitCap(ability);
        if (refund <= 0 || cap <= 0 || ability.storedDamageInstances < cap) return;
        SimplySwordsAPI.reduceWeaponCooldown(actor, actor.getStackInHand(ability.hand),
                ability.execution.cooldownTicks(Config.uniqueEffects.thunderbrand.cooldown), refund);
    }

    private static void retaliate(DamageSource source, ActiveThunderBlitz ability) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(ability == null ? null : CombatProvenanceApi.from(ability.stack, null))) {
        int ticks = ability.tuning.integer(s("THUNDERBRAND_RETALIATORY_SLOW_TICKS"), 0);
        Entity attacker = source.getAttacker();
        if (ticks <= 0 || attacker == null || attacker != source.getSource()
                || !(attacker instanceof LivingEntity living)) return;
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 0), living);
        }
    }

    private static void grantReactiveAbsorption(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        float perHit = (float) ability.tuning.get(s("THUNDERBRAND_ABSORPTION_PER_HIT"), 0);
        float cap = (float) ability.tuning.get(s("THUNDERBRAND_ABSORPTION_CAP"), 0);
        int duration = ability.tuning.integer(s("THUNDERBRAND_ABSORPTION_DURATION_TICKS"), 0);
        float amount = Math.min(cap, perHit * ability.storedDamageInstances);
        if (amount <= 0 || duration <= 0) return;
        float before = actor.getAbsorptionAmount();
        actor.setAbsorptionAmount(before + amount);
        float applied = actor.getAbsorptionAmount() - before;
        if (applied > 0) ABSORPTION.computeIfAbsent(world, ignored -> new HashMap<>())
                .put(actor.getUuid(), new AbsorptionGrant(before, applied, world.getTime() + duration));
    }

    private static void applyChargeEffects(LivingEntity actor, ActiveThunderBlitz ability) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(ability == null ? null : CombatProvenanceApi.from(ability.stack, null))) {
        int duration = chargeDuration(ability) + 2;
        ability.chargeEffectsRemoved = false;
        ability.previousSlowness = copy(actor.getStatusEffect(StatusEffects.SLOWNESS));
        ability.previousFatigue = copy(actor.getStatusEffect(StatusEffects.MINING_FATIGUE));
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 3), actor);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, 3), actor);
        if (ability.tuning.flag(1 << 16)) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    duration + ability.tuning.integer(s("THUNDERBRAND_DEFENSE_PADDING_TICKS"), 0), 2), actor);
        }
        }
    }

    private static void removeChargeEffects(LivingEntity actor, ActiveThunderBlitz ability, long now) {
        if (actor == null || ability.chargeEffectsRemoved) return;
        ability.chargeEffectsRemoved = true;
        int elapsed = (int) Math.max(0, now - ability.startedAt);
        restore(actor, StatusEffects.SLOWNESS, ability.previousSlowness, 3,
                Math.max(0, chargeDuration(ability) + 2 - elapsed), elapsed);
        restore(actor, StatusEffects.MINING_FATIGUE, ability.previousFatigue, 3,
                Math.max(0, chargeDuration(ability) + 2 - elapsed), elapsed);
    }

    private static void cleanupAbility(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability,
                                       boolean cancelExecution) {
        if (actor != null) {
            removeChargeEffects(actor, ability, world.getTime());
            stopDashMovement(actor);
        }
        if (cancelExecution) cancel(ability.execution);
    }

    private static void mark(ServerWorld world, LivingEntity actor, LivingEntity target,
                             ActiveThunderBlitz ability) {
        int duration = ability.tuning.integer(s("THUNDERBRAND_CONDUCTOR_DURATION_TICKS"), 60);
        if (duration > 0) MARKS.computeIfAbsent(world, ignored -> new HashMap<>())
                .put(new MarkKey(actor.getUuid(), target.getUuid()), world.getTime() + duration);
    }

    private static boolean isMarked(ServerWorld world, LivingEntity actor, LivingEntity target) {
        Map<MarkKey, Long> marks = MARKS.get(world);
        return marks != null && marks.getOrDefault(new MarkKey(actor.getUuid(), target.getUuid()), -1L) >= world.getTime();
    }

    private static LivingEntity crosscurrentTarget(ServerWorld world, LivingEntity actor, LivingEntity origin,
                                                   ActiveThunderBlitz ability, Set<LivingEntity> localTargets) {
        if (!ability.tuning.flag(1 << 22)) return null;
        Set<UUID> excluded = new HashSet<>();
        localTargets.forEach(target -> excluded.add(target.getUuid()));
        if (ability.tuning.flag(1 << 26)) excluded.addAll(ability.chainHitTargets);
        return ability.dashHitTargets.stream().filter(id -> !excluded.contains(id))
                .map(world::getEntity).filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(target -> validTarget(actor, target)
                        && target.squaredDistanceTo(origin) <= chainRange(ability) * chainRange(ability))
                .min(Comparator.comparingDouble((LivingEntity target) -> origin.squaredDistanceTo(target))
                        .thenComparing(LivingEntity::getUuid))
                .orElse(null);
    }

    private static List<LivingEntity> buildChain(ServerWorld world, LivingEntity actor, LivingEntity first,
                                                  int cap, double range, Set<UUID> excluded) {
        List<LivingEntity> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>(excluded);
        LivingEntity current = first;
        while (current != null && result.size() < cap) {
            result.add(current);
            visited.add(current.getUuid());
            current = findNext(world, actor, current.getPos(), range, visited);
        }
        return result;
    }

    private static LivingEntity findNext(ServerWorld world, LivingEntity actor, Vec3d origin,
                                         double range, Set<UUID> excluded) {
        Box box = new Box(origin, origin).expand(range, range * .5, range);
        return world.getEntitiesByClass(LivingEntity.class, box,
                        target -> validTarget(actor, target) && !excluded.contains(target.getUuid()))
                .stream().min(Comparator.comparingDouble((LivingEntity target) -> target.squaredDistanceTo(origin))
                        .thenComparing(LivingEntity::getUuid)).orElse(null);
    }

    private static List<LivingEntity> areaTargets(ServerWorld world, LivingEntity actor, Vec3d origin,
                                                   double radius, int cap, Set<UUID> excluded,
                                                   Set<UUID> castExcluded) {
        Box box = new Box(origin, origin).expand(radius);
        return world.getEntitiesByClass(LivingEntity.class, box, target -> validTarget(actor, target)
                        && !excluded.contains(target.getUuid()) && !castExcluded.contains(target.getUuid())
                        && target.getPos().squaredDistanceTo(origin) <= radius * radius)
                .stream().sorted(Comparator.comparingDouble((LivingEntity target) -> target.squaredDistanceTo(origin))
                        .thenComparing(LivingEntity::getUuid)).limit(Math.min(64, cap)).toList();
    }

    private static List<LivingEntity> lineTargets(ServerWorld world, LivingEntity actor, Vec3d start,
                                                   Vec3d end, int cap) {
        Box box = new Box(start, end).expand(1);
        Vec3d segment = end.subtract(start);
        double lengthSquared = segment.lengthSquared();
        return world.getEntitiesByClass(LivingEntity.class, box, target -> validTarget(actor, target)
                        && distanceToSegmentSquared(target.getPos(), start, segment, lengthSquared) <= 1)
                .stream().sorted(Comparator.comparingDouble((LivingEntity target) -> projection(
                                target.getPos(), start, segment, lengthSquared))
                        .thenComparing(LivingEntity::getUuid)).limit(Math.min(64, cap)).toList();
    }

    static double distanceToSegmentSquared(Vec3d point, Vec3d start, Vec3d segment, double lengthSquared) {
        if (lengthSquared <= 0) return point.squaredDistanceTo(start);
        double projection = Math.clamp(point.subtract(start).dotProduct(segment) / lengthSquared, 0, 1);
        return point.squaredDistanceTo(start.add(segment.multiply(projection)));
    }

    static Vec3d turnToward(Vec3d current, Vec3d desired, double maximumRadians) {
        if (current == null || desired == null || maximumRadians <= 0) return current;
        double currentAngle = Math.atan2(current.z, current.x);
        double desiredAngle = Math.atan2(desired.z, desired.x);
        double delta = desiredAngle - currentAngle;
        while (delta > Math.PI) delta -= Math.PI * 2;
        while (delta < -Math.PI) delta += Math.PI * 2;
        double angle = currentAngle + Math.clamp(delta, -maximumRadians, maximumRadians);
        return new Vec3d(Math.cos(angle), 0, Math.sin(angle));
    }

    static int scheduledDashTick(int chainIndex, int totalChains, int dashDuration) {
        return (int) (((2L * chainIndex + 1) * dashDuration) / (2L * totalChains));
    }

    static int configuredDashDuration(int configured, int bonus, double multiplier, int fixed) {
        return fixed > 0 ? fixed : Math.max(1, (int) Math.round((configured + bonus) * multiplier));
    }

    static int forkedTargetCap(int configured, int extra, int cap) {
        int value = Math.max(1, configured + Math.max(0, extra));
        return cap > 0 ? Math.min(value, cap) : value;
    }

    private static double projection(Vec3d point, Vec3d start, Vec3d segment, double lengthSquared) {
        return lengthSquared <= 0 ? 0 : Math.clamp(point.subtract(start).dotProduct(segment) / lengthSquared, 0, 1);
    }

    private static int chargeDuration(ActiveThunderBlitz ability) {
        int configured = ability.tuning.integer(s("DURATION_TICKS"),
                Math.max(1, Config.uniqueEffects.thunderbrand.chargeDuration));
        if (ability.tuning.has(s("THUNDERBRAND_CHARGE_REDUCTION_TICKS"))) {
            return Math.max(1, configured - ability.tuning.integer(s("THUNDERBRAND_CHARGE_REDUCTION_TICKS"), 0));
        }
        return ability.tuning.integer(s("DURATION_TICKS"), configured);
    }

    private static int dashDuration(ActiveThunderBlitz ability) {
        if (!ability.tuning.has(s("THUNDERBRAND_DASH_BONUS_TICKS"))
                && !ability.tuning.has(s("THUNDERBRAND_DASH_DURATION_MULTIPLIER"))
                && !ability.tuning.has(s("THUNDERBRAND_DASH_DURATION_TICKS"))) {
            return ability.tuning.integer(s("COUNT"), Math.max(1, Config.uniqueEffects.thunderbrand.dashDuration));
        }
        return configuredDashDuration(ability.tuning.integer(s("COUNT"),
                        Math.max(1, Config.uniqueEffects.thunderbrand.dashDuration)),
                ability.tuning.integer(s("THUNDERBRAND_DASH_BONUS_TICKS"), 0),
                ability.tuning.get(s("THUNDERBRAND_DASH_DURATION_MULTIPLIER"), 1),
                ability.tuning.integer(s("THUNDERBRAND_DASH_DURATION_TICKS"), 0));
    }

    private static int storedHitCap(ActiveThunderBlitz ability) {
        if (ability.tuning.flag(1 << 17)) return 0;
        return ability.tuning.integer(s("THUNDERBRAND_STORED_HIT_CAP"),
                ability.tuning.integer(s("CHARGE_CAP"), MAX_STORED_DAMAGE_INSTANCES));
    }

    private static int chainTargetCap(ActiveThunderBlitz ability) {
        int configured = ability.tuning.has(s("TARGET_CAP"))
                ? ability.tuning.integer(s("TARGET_CAP"), Config.uniqueEffects.thunderbrand.chainTargets)
                : Config.uniqueEffects.thunderbrand.chainTargets;
        return forkedTargetCap(configured,
                ability.tuning.integer(s("THUNDERBRAND_CHAIN_EXTRA_TARGETS"), 0),
                ability.tuning.integer(s("THUNDERBRAND_CHAIN_TARGET_CAP"), 0));
    }

    private static double chainRange(ActiveThunderBlitz ability) {
        double configured = ability.tuning.has(s("RANGE"))
                ? ability.tuning.get(s("RANGE"), Config.uniqueEffects.thunderbrand.chainRange)
                : Config.uniqueEffects.thunderbrand.chainRange;
        return Math.max(.5, configured + ability.tuning.get(s("THUNDERBRAND_CHAIN_RANGE_BONUS"), 0));
    }

    private static double collisionRadius(ActiveThunderBlitz ability) {
        double configured = ability.tuning.has(s("RADIUS"))
                ? ability.tuning.get(s("RADIUS"), Config.uniqueEffects.thunderbrand.radius)
                : Config.uniqueEffects.thunderbrand.radius;
        return Math.max(.1, configured + ability.tuning.get(s("THUNDERBRAND_COLLISION_RADIUS_BONUS"), 0));
    }

    private static float chainDamage(LivingEntity actor, ActiveThunderBlitz ability) {
        return baseDamage(actor, ability) * (float) ability.tuning.get(s("SECONDARY_DAMAGE_MULTIPLIER"), 1)
                * (float) ability.tuning.get(s("THUNDERBRAND_CHAIN_DAMAGE_MULTIPLIER"), 1);
    }

    private static float baseDamage(LivingEntity actor, ActiveThunderBlitz ability) {
        return HelperMethods.abilityScaledDamage("lightning", actor, ability.stack,
                Config.uniqueEffects.thunderbrand.damageScaling, Config.uniqueEffects.thunderbrand.spellScaling);
    }

    private static Vec3d resolveDashDirection(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        if (!(actor instanceof PlayerEntity)) {
            LivingEntity target = ability.targetId != null && world.getEntity(ability.targetId) instanceof LivingEntity living
                    && isValidTarget(living, actor) ? living : findNext(world, actor, actor.getPos(),
                    Math.max(8, Config.uniqueEffects.thunderbrand.chainRange), Set.of());
            if (target != null) return horizontalDirection(target.getPos().subtract(actor.getPos()), actor);
        }
        return horizontalDirection(actor instanceof PlayerEntity ? actor.getRotationVec(1) : ability.fallbackDirection, actor);
    }

    private static Vec3d horizontalDirection(Vec3d direction, LivingEntity actor) {
        Vec3d horizontal = direction == null ? Vec3d.ZERO : new Vec3d(direction.x, 0, direction.z);
        if (horizontal.lengthSquared() < .0001) {
            Vec3d rotation = actor.getRotationVec(1);
            horizontal = new Vec3d(rotation.x, 0, rotation.z);
        }
        if (horizontal.lengthSquared() < .0001) horizontal = Vec3d.fromPolar(0, actor.getYaw());
        return horizontal.normalize();
    }

    private static boolean validTarget(LivingEntity actor, LivingEntity target) {
        return target != null && target != actor && target.isAlive() && HelperMethods.checkAbilityTarget(target, actor);
    }

    private static boolean isValidTarget(LivingEntity target, LivingEntity actor) {
        return validTarget(actor, target);
    }

    private static boolean isStillHolding(LivingEntity actor, ActiveThunderBlitz ability) {
        return actor.getStackInHand(ability.hand).isOf(ItemsRegistry.THUNDERBRAND.get());
    }

    private static boolean isPlayerStillCharging(LivingEntity actor, ActiveThunderBlitz ability) {
        if (!(actor instanceof ServerPlayerEntity player)) return true;
        return player.isUsingItem() && player.getActiveHand() == ability.hand
                || PlayerWeaponAbilityChannelManager.isChanneling(player, ability.hand, ItemsRegistry.THUNDERBRAND.get());
    }

    private static void stopDashMovement(LivingEntity actor) {
        if (actor == null) return;
        actor.setVelocity(0, actor.getVelocity().y, 0);
        actor.velocityModified = true;
    }

    private static Vec3d midpoint(LivingEntity entity) {
        return entity.getPos().add(0, Math.max(.45, entity.getHeight() * .58), 0);
    }

    private static ActiveThunderBlitz state(ServerWorld world, UUID actor) {
        Map<UUID, ActiveThunderBlitz> active = ACTIVE.get(world);
        return active == null ? null : active.get(actor);
    }

    private static Memory removeMemory(ServerWorld world, UUID actor) {
        Map<UUID, Memory> memories = MEMORIES.get(world);
        Memory result = memories == null ? null : memories.remove(actor);
        if (memories != null && memories.isEmpty()) MEMORIES.remove(world);
        return result;
    }

    private static void sweepMemory(ServerWorld world, long now) {
        Map<UUID, Memory> memories = MEMORIES.get(world);
        if (memories == null) return;
        Iterator<Map.Entry<UUID, Memory>> iterator = memories.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Memory> entry = iterator.next();
            Memory memory = entry.getValue();
            Entity actor = world.getEntity(entry.getKey());
            if (memory.expiresAt >= now && actor instanceof LivingEntity living && living.isAlive()) continue;
            cancel(memory.ability.execution);
            iterator.remove();
        }
        if (memories.isEmpty()) MEMORIES.remove(world);
    }

    private static void sweepMarks(ServerWorld world, long now) {
        Map<MarkKey, Long> marks = MARKS.get(world);
        if (marks == null) return;
        marks.entrySet().removeIf(entry -> entry.getValue() < now
                || world.getEntity(entry.getKey().owner) == null || world.getEntity(entry.getKey().target) == null);
        if (marks.isEmpty()) MARKS.remove(world);
    }

    private static void sweepAbsorption(ServerWorld world, long now) {
        Map<UUID, AbsorptionGrant> grants = ABSORPTION.get(world);
        if (grants == null) return;
        Iterator<Map.Entry<UUID, AbsorptionGrant>> iterator = grants.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, AbsorptionGrant> entry = iterator.next();
            if (entry.getValue().expiresAt >= now) continue;
            if (world.getEntity(entry.getKey()) instanceof LivingEntity actor) removeGrant(actor, entry.getValue());
            iterator.remove();
        }
        if (grants.isEmpty()) ABSORPTION.remove(world);
    }

    private static void clearOwnedState(ServerWorld world, UUID actorId, LivingEntity actor) {
        Map<UUID, AbsorptionGrant> grants = ABSORPTION.get(world);
        AbsorptionGrant grant = grants == null ? null : grants.remove(actorId);
        if (grant != null && actor != null) removeGrant(actor, grant);
        if (grants != null && grants.isEmpty()) ABSORPTION.remove(world);
        Map<MarkKey, Long> marks = MARKS.get(world);
        if (marks != null) {
            marks.keySet().removeIf(key -> key.owner.equals(actorId) || key.target.equals(actorId));
            if (marks.isEmpty()) MARKS.remove(world);
        }
    }

    private static void removeGrant(LivingEntity actor, AbsorptionGrant grant) {
        float current = actor.getAbsorptionAmount();
        float remaining = Math.min(grant.amount, Math.max(0, current - grant.baseline));
        actor.setAbsorptionAmount(Math.max(0, current - remaining));
    }

    private static StatusEffectInstance copy(StatusEffectInstance effect) {
        return effect == null ? null : new StatusEffectInstance(effect);
    }

    private static void restore(LivingEntity actor, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> type,
                                StatusEffectInstance previous, int amplifier, int maximumDuration, int elapsed) {
        StatusEffectInstance current = actor.getStatusEffect(type);
        if (current != null && current.getAmplifier() == amplifier && current.getDuration() <= maximumDuration + 2) {
            actor.removeStatusEffect(type);
            if (previous != null && previous.getDuration() > elapsed) {
                actor.addStatusEffect(new StatusEffectInstance(previous.getEffectType(),
                        previous.getDuration() - elapsed, previous.getAmplifier(), previous.isAmbient(),
                        previous.shouldShowParticles(), previous.shouldShowIcon()), actor);
            }
        }
    }

    private static boolean damageSuppressed(ItemStack stack, LivingEntity actor, LivingEntity target, DamageSource source, float damage) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        boolean[] result = {false};
        WeaponImplicitRegistry.runSuppressed(() -> result[0] = CombatProvenanceApi.damage(stack, actor, target, source, damage));
        return result[0];
        }
    }

    private static void cancel(UniqueAbilityExecution execution) {
        if (execution != null && !execution.isTerminal()) UniqueAbilityApi.cancel(execution);
    }

    private static boolean has(Map<?, ?> values) {
        return values != null && !values.isEmpty();
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }

    private static void spawnChargeStartEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = midpoint(actor);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 20, .4, .4, .4, .08);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_BOW_CHARGE_LONG_VERSION.get(),
                actor.getSoundCategory(), .4F, .6F);
    }

    private static void spawnChargingEffects(ServerWorld world, LivingEntity actor, int stored) {
        Vec3d pos = midpoint(actor);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z,
                3 + Math.min(7, stored / 2), .38, .45, .38, .035);
        world.spawnParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + .08, actor.getZ(), 2, .35, .04, .35, .01);
    }

    private static void spawnAbsorbEffects(ServerWorld world, LivingEntity actor, int stored) {
        Vec3d pos = midpoint(actor);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 10, .32, .38, .32, .08);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 6, .22, .28, .22, .035);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_BLOCK_01.get(),
                actor.getSoundCategory(), .45F, .9F + Math.min(.55F, stored * .035F));
    }

    private static void spawnDashStartEffects(ServerWorld world, LivingEntity actor, int stored) {
        Vec3d pos = midpoint(actor);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 24 + stored, .45, .45, .45, .12);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(),
                actor.getSoundCategory(), .55F, 1.35F);
    }

    private static void spawnDashHitEffects(ServerWorld world, LivingEntity target) {
        Vec3d pos = midpoint(target);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 18, .35, .35, .35, .1);
        world.playSound(null, target.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(),
                target.getSoundCategory(), .25F, 1.2F);
    }

    private static void spawnDissipatedChainEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = midpoint(actor);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 8, .28, .3, .28, .08);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_01.get(),
                actor.getSoundCategory(), .2F, 1.6F);
    }

    private static void spawnDashEndEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = actor.getPos().add(0, .2, 0);
        world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 10, .35, .08, .35, .03);
    }

    private static void spawnCancelledEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = midpoint(actor);
        world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 5, .2, .2, .2, .01);
    }

    private enum Phase {
        CHARGING, DASHING
    }

    private static final class ActiveThunderBlitz {
        private final UUID actorId;
        private final UUID targetId;
        private final ItemStack stack;
        private final StormFrostWaterMasteryTuning tuning;
        private final UniqueAbilityExecution execution;
        private final Set<UUID> dashHitTargets = new HashSet<>();
        private final Set<UUID> chainHitTargets = new HashSet<>();
        private Hand hand;
        private Vec3d fallbackDirection;
        private long startedAt;
        private Phase phase = Phase.CHARGING;
        private int storedDamageInstances;
        private long dashStartedAt;
        private long defenseUntil;
        private Vec3d dashDirection;
        private Vec3d previousPosition;
        private int storedHitsReleased;
        private boolean releasesFinished;
        private boolean chargeEffectsRemoved;
        private StatusEffectInstance previousSlowness;
        private StatusEffectInstance previousFatigue;

        private ActiveThunderBlitz(UUID actorId, UUID targetId, ItemStack stack, Hand hand,
                                   Vec3d fallbackDirection, long startedAt, StormFrostWaterMasteryTuning tuning,
                                   UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.targetId = targetId;
            this.stack = stack;
            this.hand = hand;
            this.fallbackDirection = fallbackDirection;
            this.startedAt = startedAt;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private record Memory(ActiveThunderBlitz ability, long expiresAt) {
    }

    private record MarkKey(UUID owner, UUID target) {
    }

    private record AbsorptionGrant(float baseline, float amount, long expiresAt) {
    }
}
