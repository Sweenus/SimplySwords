package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.Phase7AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase7UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BrambleRootVisualEntity;
import net.sweenus.simplyswords.item.custom.StealSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BramblethornAbilityManager {

    private static final String VISUAL_TAG = "simplyswords_bramble_root_visual";
    private static final int PHASE_GROWING = 0;
    private static final int PHASE_BINDING = 1;
    private static final int PHASE_LIFTING = 2;
    private static final int PHASE_SLAMMING = 3;
    private static final int LIFT_TICKS = 8;
    private static final int SLAM_TIMEOUT_TICKS = 18;
    private static final int FADE_TICKS = 12;
    private static final int HUNT_VISUAL_FADE_TICKS = 6;
    private static final double MAX_GRASP_RADIUS = 32.0;
    private static final double MAX_HUNT_RADIUS = 8.0;

    private static final Map<ServerWorld, Map<UUID, ActiveGrasp>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, HuntMark>> HUNT_MARKS = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveHunt>> ACTIVE_HUNTS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, HuntSequence>> HUNT_STEPS = new HashMap<>();
    private static final ThreadLocal<Boolean> PROPAGATING_DAMAGE = ThreadLocal.withInitial(() -> false);

    private BramblethornAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && context.stack().isOf(ItemsRegistry.BRAMBLETHORN.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && getActive(context.world(), context.actor().getUuid()) == null
                && resolveTarget(context) != null;
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        LivingEntity anchorTarget = resolveTarget(context);
        if (anchorTarget == null) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        LivingEntity principal = context.sourcePlayer() == null ? actor : context.sourcePlayer();
        UniqueAbilityExecution execution = Phase7CombatManager.beginActive(
                Phase7UniqueAbilities.BRAMBLE_GRASP, context, Config.uniqueEffects.bramblethorn.cooldown);
        Phase7AbilityTuning tuning = Phase7UniqueAbilities.tuning(execution);
        double targetRange = graspRange(Config.uniqueEffects.bramblethorn.targetRange, tuning);
        if (actor.squaredDistanceTo(anchorTarget) > targetRange * targetRange) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
        Vec3d center = anchorTarget.getPos();
        List<LivingEntity> targets = findTargets(context, anchorTarget, center, tuning);
        if (targets.isEmpty()) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }

        ItemStack stack = context.stack().copy();
        float slamDamage = HelperMethods.abilityScaledDamage(
                SpellScalingProfile.NATURE, actor, stack,
                Math.max(0.0F, Config.uniqueEffects.bramblethorn.slamDamageScaling),
                Math.max(0.0F, Config.uniqueEffects.bramblethorn.slamSpellScaling));
        slamDamage *= (float) slamDamageMultiplier(tuning);
        long now = world.getTime();
        int growingTicks = graspTravelTicks(Config.uniqueEffects.bramblethorn.rootTravelTicks, tuning);
        int bindingTicks = bindingDuration(Config.uniqueEffects.bramblethorn.bindingDuration, tuning);
        ActiveGrasp grasp = new ActiveGrasp(
                actor.getUuid(), principal.getUuid(), stack, center, now,
                now + growingTicks, now + growingTicks + bindingTicks, slamDamage, tuning, execution);

        int lifetime = growingTicks + bindingTicks + LIFT_TICKS + SLAM_TIMEOUT_TICKS + FADE_TICKS + 10;
        BrambleRootVisualEntity core = new BrambleRootVisualEntity(
                world, null, null, BrambleRootVisualEntity.MODE_CORE, center, lifetime);
        core.addCommandTag(VISUAL_TAG);
        if (world.spawnEntity(core)) {
            grasp.coreVisualId = core.getUuid();
        }

        for (int i = 0; i < targets.size(); i++) {
            LivingEntity target = targets.get(i);
            double angle = i == 0 ? 0.0 : Math.PI * 2.0 * (i - 1) / Math.max(1, targets.size() - 1);
            double slotRadius = i == 0 ? 0.0 : Math.max(0.85, Math.min(1.8, target.getWidth() * 0.8 + 0.65));
            Vec3d slot = center.add(Math.cos(angle) * slotRadius, 0.0, Math.sin(angle) * slotRadius);
            int mode = i == 0 ? BrambleRootVisualEntity.MODE_PRIMARY : BrambleRootVisualEntity.MODE_BRANCH;
            Entity source = i == 0 ? actor : null;
            BrambleRootVisualEntity visual = new BrambleRootVisualEntity(
                    world, source, target, mode, center, lifetime);
            visual.addCommandTag(VISUAL_TAG);
            UUID visualId = world.spawnEntity(visual) ? visual.getUuid() : null;
            grasp.targets.add(new BoundTarget(target.getUuid(), visualId, slot));
        }

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), grasp);
        UniqueAbilityApi.start(execution);
        if (has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_BARKSKIN_ABSORPTION)
                && !has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_BRIAR_DAMAGE_MULTIPLIER)) {
            float absorption = (float) tuning.get(
                    Phase7AbilityTuning.Setting.BRAMBLE_BARKSKIN_ABSORPTION, 4);
            Phase4AbsorptionTracker.grant(actor, absorption, tuning.integer(
                    Phase7AbilityTuning.Setting.BRAMBLE_BARKSKIN_DURATION_TICKS, 80), absorption);
        }
        actor.swingHand(context.hand() == null ? net.minecraft.util.Hand.MAIN_HAND : context.hand(), true);
        spawnActivationEffects(world, actor, center);
        return true;
    }

    public static void onWeaponHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (stack == null || stack.isEmpty() || target == null || attacker == null
                || attacker.getWorld().isClient()
                || !(attacker.getWorld() instanceof ServerWorld world)
                || !stack.isOf(ItemsRegistry.BRAMBLETHORN.get())
                || !AwakeningApi.isAbilityUnlocked(stack)
                || !target.isAlive() || target == attacker
                || !HelperMethods.checkAbilityTarget(target, attacker)) {
            return;
        }

        long now = world.getTime();
        UniqueAbilityExecution execution = Phase7CombatManager.beginPassive(
                Phase7UniqueAbilities.BRAMBLE_HUNT, world, stack, attacker, target);
        Phase7AbilityTuning tuning = Phase7UniqueAbilities.tuning(execution);
        int memoryDuration = huntMemoryDuration(
                Config.uniqueEffects.bramblethorn.huntMemoryDuration, tuning);
        Map<UUID, HuntMark> marks = HUNT_MARKS.computeIfAbsent(world, ignored -> new HashMap<>());
        HuntMark previousMark = marks.get(attacker.getUuid());
        LivingEntity previousTarget = previousMark == null || previousMark.expiresAt <= now
                ? null : resolveLiving(world, previousMark.targetId);

        if (previousTarget == null || !previousTarget.isAlive() || previousTarget.isRemoved()
                || previousTarget == target) {
            long nextProcAt = previousMark == null ? now : previousMark.nextProcAt;
            marks.put(attacker.getUuid(),
                    new HuntMark(target.getUuid(), now + memoryDuration, nextProcAt));
            spawnHuntMarkEffects(world, target, previousTarget != target);
            UniqueAbilityApi.finish(execution, Phase7UniqueAbilities.FINISH, 0);
            return;
        }

        double maximumRange = huntRange(Config.uniqueEffects.bramblethorn.huntMaximumRange, tuning);
        boolean inRange = previousTarget.squaredDistanceTo(target) <= maximumRange * maximumRange;
        long nextProcAt = previousMark.nextProcAt;
        if (inRange && now >= nextProcAt) {
            float sequenceMultiplier = huntSequenceMultiplier(world, attacker, target, tuning);
            int projectileCount = Math.max(1, tuning.integer(
                    Phase7AbilityTuning.Setting.BRAMBLE_WALTZ_PROJECTILE_COUNT, 1));
            float projectileMultiplier = (float) tuning.get(
                    Phase7AbilityTuning.Setting.BRAMBLE_WALTZ_DAMAGE_MULTIPLIER, 1);
            for (int projectile = 0; projectile < projectileCount; projectile++) {
                UniqueAbilityExecution current = projectile == 0 ? execution
                        : Phase7CombatManager.beginPassive(Phase7UniqueAbilities.BRAMBLE_HUNT,
                        world, stack, attacker, target);
                launchHunt(world, attacker, stack, previousTarget, target,
                        Phase7UniqueAbilities.tuning(current), current,
                        sequenceMultiplier * projectileMultiplier);
            }
            nextProcAt = now + SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                    stack, attacker, huntCooldown(Config.uniqueEffects.bramblethorn.huntCooldown, tuning));
        } else {
            spawnHuntMarkEffects(world, target, true);
            UniqueAbilityApi.finish(execution, Phase7UniqueAbilities.FINISH, 0);
        }

        marks.put(attacker.getUuid(),
                new HuntMark(target.getUuid(), now + memoryDuration, nextProcAt));
    }

    public static LivingEntity findPlayerTarget(PlayerEntity player) {
        if (player == null || !player.isAlive()) {
            return null;
        }
        double range = Math.max(1.0, Config.uniqueEffects.bramblethorn.targetRange + 4.0);
        return StealSwordItem.findLenientTarget(player, range,
                target -> isEligibleTarget(player, null, player.getWorld(), target));
    }

    public static boolean isActive(LivingEntity actor) {
        return actor != null
                && actor.getWorld() instanceof ServerWorld world
                && getActive(world, actor.getUuid()) != null;
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        Map<UUID, ActiveGrasp> grasps = ACTIVE.remove(world);
        if (grasps != null) {
            for (ActiveGrasp grasp : grasps.values()) {
                discardVisuals(world, grasp);
                UniqueAbilityApi.finish(grasp.execution, Phase7UniqueAbilities.FINISH,
                        grasp.targets.size());
            }
        }
        List<ActiveHunt> hunts = ACTIVE_HUNTS.remove(world);
        if (hunts != null) {
            for (ActiveHunt hunt : hunts) {
                discardHuntVisual(world, hunt.visualId);
                UniqueAbilityApi.finish(hunt.execution, Phase7UniqueAbilities.FINISH,
                        hunt.hitTargets.size());
            }
        }
        HUNT_MARKS.remove(world);
        HUNT_STEPS.remove(world);
    }

    public static void clearAll() {
        Set<ServerWorld> worlds = new HashSet<>(ACTIVE.keySet());
        worlds.addAll(HUNT_MARKS.keySet());
        worlds.addAll(ACTIVE_HUNTS.keySet());
        worlds.addAll(HUNT_STEPS.keySet());
        for (ServerWorld world : worlds) clear(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        UUID actorId = actor.getUuid();
        Map<UUID, ActiveGrasp> grasps = ACTIVE.get(world);
        if (grasps != null) {
            ActiveGrasp grasp = grasps.remove(actorId);
            if (grasp != null) {
                discardVisuals(world, grasp);
                UniqueAbilityApi.finish(grasp.execution, Phase7UniqueAbilities.FINISH,
                        grasp.targets.size());
            }
            if (grasps.isEmpty()) ACTIVE.remove(world);
        }
        Map<UUID, HuntMark> marks = HUNT_MARKS.get(world);
        if (marks != null) {
            marks.remove(actorId);
            if (marks.isEmpty()) HUNT_MARKS.remove(world);
        }
        Map<UUID, HuntSequence> steps = HUNT_STEPS.get(world);
        if (steps != null) {
            steps.remove(actorId);
            if (steps.isEmpty()) HUNT_STEPS.remove(world);
        }
        List<ActiveHunt> hunts = ACTIVE_HUNTS.get(world);
        if (hunts != null) {
            hunts.removeIf(hunt -> {
                if (!hunt.attackerId.equals(actorId)) return false;
                discardHuntVisual(world, hunt.visualId);
                UniqueAbilityApi.finish(hunt.execution, Phase7UniqueAbilities.FINISH,
                        hunt.hitTargets.size());
                return true;
            });
            if (hunts.isEmpty()) ACTIVE_HUNTS.remove(world);
        }
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveGrasp> active = ACTIVE.get(world);
        Map<UUID, HuntMark> marks = HUNT_MARKS.get(world);
        List<ActiveHunt> hunts = ACTIVE_HUNTS.get(world);
        Map<UUID, HuntSequence> steps = HUNT_STEPS.get(world);
        return active != null && !active.isEmpty()
                || marks != null && !marks.isEmpty()
                || hunts != null && !hunts.isEmpty()
                || steps != null && !steps.isEmpty();
    }

    public static void tick(ServerWorld world) {
        tickActiveGrasps(world);
        tickHunts(world);
        tickHuntMarks(world);
        tickHuntSteps(world);
    }

    private static void tickActiveGrasps(ServerWorld world) {
        Map<UUID, ActiveGrasp> activeByOwner = ACTIVE.get(world);
        if (activeByOwner == null || activeByOwner.isEmpty()) {
            return;
        }

        Iterator<ActiveGrasp> iterator = activeByOwner.values().iterator();
        while (iterator.hasNext()) {
            ActiveGrasp grasp = iterator.next();
            Entity ownerEntity = world.getEntity(grasp.actorId);
            if (!(ownerEntity instanceof LivingEntity actor) || !actor.isAlive() || actor.isRemoved()) {
                fadeVisuals(world, grasp);
                UniqueAbilityApi.finish(grasp.execution, Phase7UniqueAbilities.FINISH, grasp.targets.size());
                iterator.remove();
                continue;
            }

            removeInvalidTargets(world, grasp);
            if (grasp.targets.isEmpty()) {
                fadeVisuals(world, grasp);
                UniqueAbilityApi.finish(grasp.execution, Phase7UniqueAbilities.FINISH, 0);
                iterator.remove();
                continue;
            }

            long now = world.getTime();
            if (grasp.phase == PHASE_GROWING && now >= grasp.growthEndsAt) {
                grasp.phase = PHASE_BINDING;
                setAllVisualPhases(world, grasp, BrambleRootVisualEntity.PHASE_BIND);
                for (BoundTarget bound : grasp.targets) {
                    LivingEntity target = resolveLiving(world, bound.targetId);
                    if (target != null) {
                        target.stopRiding();
                    }
                }
                applyBindingBenefits(actor, grasp);
                spawnBindingEffects(world, grasp.center);
            }

            if (grasp.phase == PHASE_BINDING) {
                tickBinding(world, actor, grasp);
                if (now >= grasp.bindingEndsAt) {
                    if (has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_BRIAR_DAMAGE_MULTIPLIER)) {
                        applyBriarSacrifice(world, actor, grasp);
                        fadeVisuals(world, grasp);
                        UniqueAbilityApi.finish(grasp.execution, Phase7UniqueAbilities.FINISH,
                                grasp.targets.size());
                        iterator.remove();
                        continue;
                    }
                    if (has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_TANGLED_DISABLE_SLAM)) {
                        fadeVisuals(world, grasp);
                        UniqueAbilityApi.finish(grasp.execution, Phase7UniqueAbilities.FINISH,
                                grasp.targets.size());
                        iterator.remove();
                        continue;
                    }
                    beginLift(world, grasp);
                }
            } else if (grasp.phase == PHASE_LIFTING) {
                tickLift(world, grasp);
                if (now - grasp.phaseStartedAt >= LIFT_TICKS) {
                    beginSlam(world, grasp);
                }
            } else if (grasp.phase == PHASE_SLAMMING && tickSlam(world, actor, grasp)) {
                fadeVisuals(world, grasp);
                UniqueAbilityApi.finish(grasp.execution, Phase7UniqueAbilities.FINISH, grasp.targets.size());
                iterator.remove();
            }
        }

        if (activeByOwner.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static void launchHunt(ServerWorld world, LivingEntity attacker, ItemStack stack,
                                   LivingEntity previousTarget, LivingEntity target,
                                   Phase7AbilityTuning tuning, UniqueAbilityExecution execution,
                                   float damageMultiplier) {
        int travelTicks = Math.max(1, Config.uniqueEffects.bramblethorn.huntTravelTicks);
        float damage = HelperMethods.abilityScaledDamage(
                SpellScalingProfile.NATURE, attacker, stack,
                Math.max(0.0F, Config.uniqueEffects.bramblethorn.huntDamageScaling),
                Math.max(0.0F, Config.uniqueEffects.bramblethorn.huntSpellScaling));
        damage *= (float) huntDamageMultiplier(tuning);
        damage *= damageMultiplier;
        float effectMultiplier = AwakeningApi.getEffectMultiplier(stack);
        int configuredSlowDuration = huntSlowDuration(
                Config.uniqueEffects.bramblethorn.huntSlowDuration, tuning);
        int slowDuration = configuredSlowDuration <= 0 || effectMultiplier <= 0.0F
                ? 0 : Math.max(1, Math.round(configuredSlowDuration * effectMultiplier));
        Vec3d origin = previousTarget.getPos();

        BrambleRootVisualEntity visual = new BrambleRootVisualEntity(
                world, null, target, BrambleRootVisualEntity.MODE_HUNT, origin,
                travelTicks + HUNT_VISUAL_FADE_TICKS + 2);
        visual.addCommandTag(VISUAL_TAG);
        UUID visualId = world.spawnEntity(visual) ? visual.getUuid() : null;

        ActiveHunt hunt = new ActiveHunt(
                attacker.getUuid(), target.getUuid(), stack.copy(), origin,
                world.getTime(), travelTicks,
                huntWidth(Config.uniqueEffects.bramblethorn.huntHitRadius, tuning),
                damage, slowDuration,
                huntSlowAmplifier(Config.uniqueEffects.bramblethorn.huntSlowAmplifier, tuning),
                huntTargetCap(tuning),
                has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_PREDATOR_TARGET_ONLY), visualId, execution);
        ACTIVE_HUNTS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(hunt);

        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,
                        net.minecraft.block.Blocks.ROOTED_DIRT.getDefaultState()),
                origin.x, origin.y + 0.08, origin.z,
                10, 0.28, 0.08, 0.28, 0.08);
        world.spawnParticles(ParticleTypes.COMPOSTER,
                origin.x, origin.y + 0.22, origin.z,
                8, 0.32, 0.18, 0.32, 0.04);
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BLOCK_CAVE_VINES_BREAK, SoundCategory.PLAYERS,
                0.75F, 0.72F + world.random.nextFloat() * 0.12F);
    }

    private static void tickHunts(ServerWorld world) {
        List<ActiveHunt> hunts = ACTIVE_HUNTS.get(world);
        if (hunts == null || hunts.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<ActiveHunt> iterator = hunts.iterator();
        while (iterator.hasNext()) {
            ActiveHunt hunt = iterator.next();
            LivingEntity attacker = resolveLiving(world, hunt.attackerId);
            LivingEntity target = resolveLiving(world, hunt.targetId);
            if (attacker == null || !attacker.isAlive() || attacker.isRemoved()
                    || target == null || !target.isAlive() || target.isRemoved()) {
                discardHuntVisual(world, hunt.visualId);
                UniqueAbilityApi.finish(hunt.execution, Phase7UniqueAbilities.FINISH, hunt.hitTargets.size());
                iterator.remove();
                continue;
            }

            float progress = easeOutCubic(MathHelper.clamp(
                    (now - hunt.startedAt) / (float) hunt.travelTicks, 0.0F, 1.0F));
            if (progress > hunt.previousProgress) {
                Vec3d destination = target.getPos();
                Vec3d segmentStart = hunt.origin.lerp(destination, hunt.previousProgress);
                Vec3d segmentEnd = hunt.origin.lerp(destination, progress);
                sweepHuntSegment(world, hunt, attacker, segmentStart, segmentEnd);
                hunt.previousProgress = progress;
            }

            if (progress >= 1.0F) {
                UniqueAbilityApi.finish(hunt.execution, Phase7UniqueAbilities.FINISH, hunt.hitTargets.size());
                iterator.remove();
            }
        }

        if (hunts.isEmpty()) {
            ACTIVE_HUNTS.remove(world);
        }
    }

    private static void sweepHuntSegment(ServerWorld world, ActiveHunt hunt,
                                         LivingEntity attacker, Vec3d start, Vec3d end) {
        Box searchBox = new Box(start, end).expand(hunt.hitRadius + 1.5);
        List<LivingEntity> candidates = new ArrayList<>(world.getEntitiesByClass(
                LivingEntity.class, searchBox, EntityPredicates.VALID_LIVING_ENTITY));
        candidates.sort(Comparator.comparingDouble(candidate -> pathProgress(candidate.getPos(), start, end)));
        for (LivingEntity candidate : candidates) {
            if (candidate == attacker || hunt.hitTargets.contains(candidate.getUuid())
                    || hunt.targetOnly && !candidate.getUuid().equals(hunt.targetId)
                    || !HelperMethods.checkAbilityTarget(candidate, attacker)) {
                continue;
            }
            boolean designatedTarget = candidate.getUuid().equals(hunt.targetId);
            boolean targetAlreadyHit = hunt.hitTargets.contains(hunt.targetId);
            int available = hunt.targetCap - hunt.hitTargets.size();
            if (available <= 0 || !designatedTarget && !targetAlreadyHit && available <= 1) {
                continue;
            }
            Vec3d sample = new Vec3d(candidate.getX(), candidate.getBodyY(0.25), candidate.getZ());
            double collisionRadius = hunt.hitRadius + candidate.getWidth() * 0.5;
            if (distanceSquaredToSegment(sample, start, end)
                    > collisionRadius * collisionRadius) {
                continue;
            }
            hunt.hitTargets.add(candidate.getUuid());
            applyHuntHit(world, hunt, attacker, candidate);
        }

        Vec3d midpoint = start.lerp(end, 0.5);
        world.spawnParticles(ParticleTypes.COMPOSTER,
                midpoint.x, midpoint.y + 0.12, midpoint.z,
                2, Math.abs(end.x - start.x) * 0.25 + 0.05,
                0.06, Math.abs(end.z - start.z) * 0.25 + 0.05, 0.01);
    }

    private static void applyHuntHit(ServerWorld world, ActiveHunt hunt,
                                     LivingEntity attacker, LivingEntity target) {
        Vec3d velocity = target.getVelocity();
        boolean damaged = false;
        if (hunt.damage > 0.0F) {
            DamageSource source = world.getDamageSources().indirectMagic(attacker, attacker);
            float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                    world, hunt.stack, target, source, hunt.damage);
            boolean[] result = {false};
            WeaponImplicitRegistry.runSuppressed(() ->
                    result[0] = HelperMethods.damageThroughIframes(target, source, finalDamage));
            damaged = result[0];
        }
        if (damaged) {
            target.setVelocity(velocity);
            target.velocityModified = true;
            target.velocityDirty = true;
        }
        if (hunt.slowDuration > 0) {
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, hunt.slowDuration, hunt.slowAmplifier,
                    false, true, true), attacker);
        }
        UniqueAbilityApi.emit(hunt.execution, UniqueAbilityPhase.HIT, Phase7UniqueAbilities.HIT,
                target, damaged ? 1 : 0, hunt.damage);

        Vec3d center = target.getBoundingBox().getCenter();
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,
                        net.minecraft.block.Blocks.ROOTED_DIRT.getDefaultState()),
                center.x, target.getY() + 0.08, center.z,
                8, target.getWidth() * 0.35, 0.08,
                target.getWidth() * 0.35, 0.08);
        world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                center.x, center.y, center.z,
                6, target.getWidth() * 0.3, target.getHeight() * 0.22,
                target.getWidth() * 0.3, 0.01);
    }

    private static void tickHuntMarks(ServerWorld world) {
        Map<UUID, HuntMark> marks = HUNT_MARKS.get(world);
        if (marks == null || marks.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<UUID, HuntMark>> iterator = marks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HuntMark> entry = iterator.next();
            LivingEntity attacker = resolveLiving(world, entry.getKey());
            LivingEntity target = resolveLiving(world, entry.getValue().targetId);
            if (entry.getValue().expiresAt <= now
                    || attacker == null || !attacker.isAlive() || attacker.isRemoved()
                    || target == null || !target.isAlive() || target.isRemoved()) {
                iterator.remove();
                continue;
            }
            if ((now + target.getId()) % 10L == 0L) {
                world.spawnParticles(ParticleTypes.COMPOSTER,
                        target.getX(), target.getBodyY(0.35), target.getZ(),
                        2, target.getWidth() * 0.4, target.getHeight() * 0.22,
                        target.getWidth() * 0.4, 0.01);
                world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        target.getX(), target.getY() + 0.12, target.getZ(),
                        1, target.getWidth() * 0.35, 0.04,
                        target.getWidth() * 0.35, 0.005);
            }
        }

        if (marks.isEmpty()) {
            HUNT_MARKS.remove(world);
        }
    }

    private static void spawnHuntMarkEffects(ServerWorld world, LivingEntity target,
                                             boolean selected) {
        int count = selected ? 6 : 2;
        world.spawnParticles(ParticleTypes.COMPOSTER,
                target.getX(), target.getBodyY(0.35), target.getZ(),
                count, target.getWidth() * 0.4, target.getHeight() * 0.22,
                target.getWidth() * 0.4, 0.025);
        if (selected) {
            world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    target.getX(), target.getY() + 0.12, target.getZ(),
                    4, target.getWidth() * 0.42, 0.08,
                    target.getWidth() * 0.42, 0.008);
            world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_CAVE_VINES_PLACE,
                    SoundCategory.PLAYERS, 0.4F, 1.05F);
        }
    }

    private static void discardHuntVisual(ServerWorld world, UUID visualId) {
        Entity visual = visualId == null ? null : world.getEntity(visualId);
        if (visual instanceof BrambleRootVisualEntity) {
            visual.discard();
        }
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

    private static double pathProgress(Vec3d point, Vec3d start, Vec3d end) {
        Vec3d segment = end.subtract(start);
        double lengthSquared = segment.lengthSquared();
        if (lengthSquared < 0.0001) return 0.0;
        return Math.clamp(point.subtract(start).dotProduct(segment) / lengthSquared, 0.0, 1.0);
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - MathHelper.clamp(value, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse;
    }

    private static float huntSequenceMultiplier(ServerWorld world, LivingEntity attacker,
                                                LivingEntity target, Phase7AbilityTuning tuning) {
        if (!has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_CROSSCUT_TARGET_COUNT)) return 1.0F;
        long now = world.getTime();
        int windowTicks = Math.max(1, tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_CROSSCUT_WINDOW_TICKS, 100));
        int targetCount = Math.max(2, tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_CROSSCUT_TARGET_COUNT, 3));
        HuntSequence sequence = HUNT_STEPS.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(attacker.getUuid(), ignored -> new HuntSequence());
        sequence.windowTicks = windowTicks;
        while (!sequence.steps.isEmpty() && now - sequence.steps.getFirst().tick > windowTicks) {
            sequence.steps.removeFirst();
        }
        sequence.steps.addLast(new HuntStep(target.getUuid(), now));
        while (sequence.steps.size() > targetCount) sequence.steps.removeFirst();
        if (sequence.steps.size() < targetCount) return 1.0F;
        Set<UUID> distinctTargets = new HashSet<>();
        for (HuntStep step : sequence.steps) distinctTargets.add(step.targetId);
        boolean distinct = distinctTargets.size() == targetCount;
        if (distinct) {
            sequence.steps.clear();
            return (float) tuning.get(
                    Phase7AbilityTuning.Setting.BRAMBLE_CROSSCUT_DAMAGE_MULTIPLIER, 1.35);
        }
        return 1.0F;
    }

    private static void tickHuntSteps(ServerWorld world) {
        Map<UUID, HuntSequence> sequences = HUNT_STEPS.get(world);
        if (sequences == null) return;
        long now = world.getTime();
        sequences.entrySet().removeIf(entry -> entry.getValue().steps.isEmpty()
                || now - entry.getValue().steps.getLast().tick > entry.getValue().windowTicks);
        if (sequences.isEmpty()) HUNT_STEPS.remove(world);
    }

    public static void onBoundTargetDamaged(LivingEntity damagedTarget, DamageSource source, float amount) {
        if (damagedTarget == null || source == null || amount <= 0.0F
                || damagedTarget.getWorld().isClient()
                || !(damagedTarget.getWorld() instanceof ServerWorld world)
                || PROPAGATING_DAMAGE.get()
                || !(source.getAttacker() instanceof LivingEntity sourceAttacker)) {
            return;
        }

        var delegated = SimplySwordsAPI.getDelegatedWeaponHitContext();
        LivingEntity attacker = delegated != null && delegated.actor() != null
                ? delegated.actor() : sourceAttacker;
        if ((delegated == null && source.getSource() != sourceAttacker)
                || !isBrambleWeaponHit(source, attacker)) {
            return;
        }

        Map<UUID, ActiveGrasp> activeByOwner = ACTIVE.get(world);
        if (activeByOwner == null) {
            return;
        }

        ActiveGrasp grasp = activeByOwner.get(attacker.getUuid());
        if (grasp == null || grasp.phase != PHASE_BINDING
                || !grasp.containsTarget(damagedTarget.getUuid())
                || grasp.lastPropagationTick == world.getTime()) {
            return;
        }

        grasp.lastPropagationTick = world.getTime();
        float sharedDamage = amount * (float) sharedDamageRatio(
                Config.uniqueEffects.bramblethorn.sharedDamageRatio, grasp.tuning);
        if (sharedDamage <= 0.0F) {
            return;
        }

        LivingEntity principal = resolveLiving(world, grasp.principalId);
        if (principal == null) {
            principal = attacker;
        }
        DamageSource echoSource = world.getDamageSources().indirectMagic(attacker, principal);
        PROPAGATING_DAMAGE.set(true);
        try {
            for (BoundTarget bound : grasp.targets) {
                if (bound.targetId.equals(damagedTarget.getUuid())) {
                    continue;
                }
                LivingEntity target = resolveLiving(world, bound.targetId);
                if (target == null || !target.isAlive()) {
                    continue;
                }
                float finalDamage = HelperMethods.applyNonPlayerAbilityDamageModifier(attacker, sharedDamage);
                finalDamage = HelperMethods.applyWeaponAbilityDamageToPlayersModifier(target, finalDamage);
                Vec3d velocity = target.getVelocity();
                float damage = finalDamage;
                WeaponImplicitRegistry.runSuppressed(() ->
                        HelperMethods.damageThroughIframes(target, echoSource, damage));
                target.setVelocity(velocity);
                target.velocityModified = true;
            }
        } finally {
            PROPAGATING_DAMAGE.set(false);
        }
        triggerVisualPulses(world, grasp);
        spawnPropagationEffects(world, damagedTarget);
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world)) return amount;
        Map<UUID, ActiveGrasp> states = ACTIVE.get(world);
        if (states == null) return amount;
        ActiveGrasp own = states.get(target.getUuid());
        if (own != null && own.phase == PHASE_BINDING
                && has(own.tuning, Phase7AbilityTuning.Setting.BRAMBLE_ROOTED_GUARD_TARGET_COUNT)
                && !has(own.tuning, Phase7AbilityTuning.Setting.BRAMBLE_BRIAR_DAMAGE_MULTIPLIER)
                && own.targets.size() >= own.tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_ROOTED_GUARD_TARGET_COUNT, 3)
                && isDirectMelee(source)) {
            amount *= own.tuning.get(
                    Phase7AbilityTuning.Setting.BRAMBLE_ROOTED_GUARD_INCOMING_MULTIPLIER, .88);
        }
        if (source.getAttacker() instanceof LivingEntity attacker) {
            for (ActiveGrasp grasp : states.values()) {
                LivingEntity owner = resolveLiving(world, grasp.actorId);
                if (owner != null && has(grasp.tuning,
                        Phase7AbilityTuning.Setting.BRAMBLE_SHELTERWOOD_OUTGOING_MULTIPLIER)
                        && !has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_BRIAR_DAMAGE_MULTIPLIER)
                        && grasp.containsTarget(attacker.getUuid())
                        && (target == owner || !HelperMethods.checkAbilityTarget(target, owner))) {
                    amount *= grasp.tuning.get(
                            Phase7AbilityTuning.Setting.BRAMBLE_SHELTERWOOD_OUTGOING_MULTIPLIER, .9);
                    break;
                }
            }
        }
        return amount;
    }

    public static void onDamageApplied(LivingEntity target, DamageSource source) {
        if (!(target.getWorld() instanceof ServerWorld world) || !isDirectMelee(source)
                || !(source.getAttacker() instanceof LivingEntity attacker)) return;
        ActiveGrasp grasp = getActive(world, target.getUuid());
        if (grasp == null || grasp.phase != PHASE_BINDING
                || !has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_RETORT_DAMAGE_MULTIPLIER)
                || !grasp.containsTarget(attacker.getUuid())
                || world.getTime() < grasp.retortCooldowns.getOrDefault(attacker.getUuid(), 0L)) return;
        grasp.retortCooldowns.put(attacker.getUuid(), world.getTime() + grasp.tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_RETORT_LOCKOUT_TICKS, 30));
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.NATURE, target,
                grasp.stack, Config.uniqueEffects.bramblethorn.huntDamageScaling,
                Config.uniqueEffects.bramblethorn.huntSpellScaling)
                * (float) grasp.tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_RETORT_DAMAGE_MULTIPLIER, .2);
        DamageSource retaliation = world.getDamageSources().indirectMagic(target, target);
        float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                world, grasp.stack, attacker, retaliation, damage);
        PROPAGATING_DAMAGE.set(true);
        try {
            WeaponImplicitRegistry.runSuppressed(() ->
                    HelperMethods.damageThroughIframes(attacker, retaliation, finalDamage));
        } finally {
            PROPAGATING_DAMAGE.set(false);
        }
    }

    private static boolean isDirectMelee(DamageSource source) {
        return source != null && (source.isOf(DamageTypes.PLAYER_ATTACK)
                || source.isOf(DamageTypes.MOB_ATTACK)
                || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO));
    }

    private static void tickBinding(ServerWorld world, LivingEntity actor, ActiveGrasp grasp) {
        for (BoundTarget bound : grasp.targets) {
            LivingEntity target = resolveLiving(world, bound.targetId);
            if (target == null) {
                continue;
            }
            pullTarget(target, bound.slot, grasp.tuning);
            if ((world.getTime() + target.getId()) % 8L == 0L) {
                world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        target.getX(), target.getBodyY(0.45), target.getZ(),
                        2, target.getWidth() * 0.35, target.getHeight() * 0.22,
                        target.getWidth() * 0.35, 0.005);
            }
        }
        if ((world.getTime() + actor.getId()) % 10L == 0L) {
            world.playSound(null, grasp.center.x, grasp.center.y, grasp.center.z,
                    SoundEvents.BLOCK_CAVE_VINES_STEP, SoundCategory.PLAYERS,
                    0.45F, 0.65F + world.random.nextFloat() * 0.15F);
        }
    }

    private static void applyBindingBenefits(LivingEntity actor, ActiveGrasp grasp) {
        if (has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_BRIAR_DAMAGE_MULTIPLIER)) return;
        if (has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_THORNWARD_TARGET_COUNT)
                && grasp.targets.size() >= grasp.tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_THORNWARD_TARGET_COUNT, 6)) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    grasp.tuning.integer(Phase7AbilityTuning.Setting.BRAMBLE_THORNWARD_DURATION_TICKS, 100),
                    grasp.tuning.integer(Phase7AbilityTuning.Setting.BRAMBLE_THORNWARD_AMPLIFIER, 0),
                    false, true, true));
        }
        if (has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_ANCIENT_RESISTANCE_AMPLIFIER)) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    Math.max(1, (int) (grasp.bindingEndsAt - actor.getWorld().getTime())),
                    grasp.tuning.integer(
                            Phase7AbilityTuning.Setting.BRAMBLE_ANCIENT_RESISTANCE_AMPLIFIER, 1),
                    false, true, true));
        }
    }

    private static void beginLift(ServerWorld world, ActiveGrasp grasp) {
        grasp.phase = PHASE_LIFTING;
        grasp.phaseStartedAt = world.getTime();
        setAllVisualPhases(world, grasp, BrambleRootVisualEntity.PHASE_LIFT);
        double liftForce = Math.max(0.0, Config.uniqueEffects.bramblethorn.liftForce)
                * liftForceMultiplier(grasp.tuning);
        for (BoundTarget bound : grasp.targets) {
            LivingEntity target = resolveLiving(world, bound.targetId);
            if (target == null) {
                continue;
            }
            bound.lifted = canLift(world, target);
            if (bound.lifted) {
                target.setVelocity(target.getVelocity().x * 0.2, liftForce,
                        target.getVelocity().z * 0.2);
                target.velocityModified = true;
                target.fallDistance = 0.0F;
            }
        }
        world.playSound(null, grasp.center.x, grasp.center.y, grasp.center.z,
                SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.PLAYERS, 1.15F, 0.58F);
    }

    private static void beginSlam(ServerWorld world, ActiveGrasp grasp) {
        grasp.phase = PHASE_SLAMMING;
        grasp.phaseStartedAt = world.getTime();
        setAllVisualPhases(world, grasp, BrambleRootVisualEntity.PHASE_SLAM);
        double slamVelocity = Math.max(0.1, Config.uniqueEffects.bramblethorn.slamVelocity);
        for (BoundTarget bound : grasp.targets) {
            LivingEntity target = resolveLiving(world, bound.targetId);
            if (target == null) {
                continue;
            }
            if (bound.lifted) {
                target.setVelocity(target.getVelocity().x * 0.1, -slamVelocity,
                        target.getVelocity().z * 0.1);
                target.velocityModified = true;
                target.fallDistance = 0.0F;
            }
        }
        world.playSound(null, grasp.center.x, grasp.center.y, grasp.center.z,
                SoundEvents.BLOCK_CAVE_VINES_BREAK, SoundCategory.PLAYERS, 1.25F, 0.48F);
    }

    private static void tickLift(ServerWorld world, ActiveGrasp grasp) {
        for (BoundTarget bound : grasp.targets) {
            if (!bound.lifted) {
                continue;
            }
            LivingEntity target = resolveLiving(world, bound.targetId);
            if (target != null) {
                target.fallDistance = 0.0F;
                target.setVelocity(target.getVelocity().multiply(0.76, 1.0, 0.76));
                target.velocityModified = true;
            }
        }
    }

    private static boolean tickSlam(ServerWorld world, LivingEntity actor, ActiveGrasp grasp) {
        long age = world.getTime() - grasp.phaseStartedAt;
        boolean allSettled = true;
        for (BoundTarget bound : grasp.targets) {
            if (bound.slammed) {
                continue;
            }
            LivingEntity target = resolveLiving(world, bound.targetId);
            if (target == null || !target.isAlive()) {
                bound.slammed = true;
                continue;
            }
            target.fallDistance = 0.0F;
            boolean impact = !bound.lifted || (age > 1L && target.isOnGround()) || age >= SLAM_TIMEOUT_TICKS;
            if (!impact) {
                allSettled = false;
                continue;
            }
            applySlamDamage(world, actor, grasp, target);
            bound.slammed = true;
            spawnImpactEffects(world, target.getPos(), target.getWidth());
        }
        return allSettled || age >= SLAM_TIMEOUT_TICKS;
    }

    private static void applySlamDamage(ServerWorld world, LivingEntity actor, ActiveGrasp grasp,
                                        LivingEntity target) {
        LivingEntity principal = resolveLiving(world, grasp.principalId);
        if (principal == null) {
            principal = actor;
        }
        DamageSource source = world.getDamageSources().indirectMagic(actor, principal);
        float damage = HelperMethods.applyAbilityDamageEnchantments(
                world, grasp.stack, target, source, grasp.slamDamage);
        PROPAGATING_DAMAGE.set(true);
        try {
            WeaponImplicitRegistry.runSuppressed(() ->
                    HelperMethods.damageThroughIframes(target, source, damage));
        } finally {
            PROPAGATING_DAMAGE.set(false);
        }
        UniqueAbilityApi.emit(grasp.execution, UniqueAbilityPhase.HIT, Phase7UniqueAbilities.HIT,
                target, 1, damage);
    }

    private static void pullTarget(LivingEntity target, Vec3d slot, Phase7AbilityTuning tuning) {
        if (has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_TANGLED_DISABLE_PULL)) return;
        Vec3d offset = slot.subtract(target.getPos());
        double horizontalDistance = offset.horizontalLength();
        if (horizontalDistance < 0.18) {
            target.setVelocity(target.getVelocity().multiply(0.52, 1.0, 0.52));
            target.velocityModified = true;
            return;
        }
        double size = Math.max(1.0, Math.max(target.getWidth(), target.getHeight() / 1.8));
        double resistance = MathHelper.clamp(
                target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
        double multiplier = MathHelper.clamp(1.0 / (size * (1.0 + resistance * 2.0)), 0.12, 1.0);
        double strength = Math.min(0.38,
                pullStrength(Config.uniqueEffects.bramblethorn.pullStrength, tuning)
                        * multiplier * Math.min(2.0, 0.75 + horizontalDistance * 0.18));
        Vec3d direction = new Vec3d(offset.x, 0.0, offset.z).normalize();
        target.addVelocity(direction.x * strength, 0.0, direction.z * strength);
        target.velocityModified = true;
        target.velocityDirty = true;
    }

    private static boolean canLift(ServerWorld world, LivingEntity target) {
        if (target.getWidth() > Math.max(0.1F, Config.uniqueEffects.bramblethorn.maximumLiftWidth)
                || target.getHeight() > Math.max(0.1F, Config.uniqueEffects.bramblethorn.maximumLiftHeight)
                || target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)
                > MathHelper.clamp(Config.uniqueEffects.bramblethorn.maximumLiftResistance, 0.0, 1.0)) {
            return false;
        }
        double clearance = Math.max(0.75, Config.uniqueEffects.bramblethorn.liftClearance);
        return world.isSpaceEmpty(target, target.getBoundingBox().offset(0.0, clearance, 0.0));
    }

    public static double graspRange(double configured, Phase7AbilityTuning tuning) {
        return Math.clamp(Math.max(1.0, configured) + tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_GRASP_RANGE_BONUS, 0), 1.0, 128.0);
    }

    public static double graspRadius(double configured, Phase7AbilityTuning tuning) {
        return Math.clamp(Math.max(0.5, configured) + tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_GRASP_RADIUS_BONUS, 0), 0.5, MAX_GRASP_RADIUS);
    }

    public static int graspTravelTicks(int configured, Phase7AbilityTuning tuning) {
        return Math.max(1, configured + tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_GRASP_TRAVEL_TICK_BONUS, 0));
    }

    public static int bindingDuration(int configured, Phase7AbilityTuning tuning) {
        double duration = Math.max(1, configured) + tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_BIND_DURATION_BONUS_TICKS, 0);
        return Math.max(1, (int) Math.round(duration * tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_ANCIENT_BIND_DURATION_MULTIPLIER, 1)));
    }

    public static double pullStrength(double configured, Phase7AbilityTuning tuning) {
        return Math.max(0.0, configured) * tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_PULL_MULTIPLIER, 1);
    }

    public static double sharedDamageRatio(double configured, Phase7AbilityTuning tuning) {
        double ratio = Math.clamp(configured, 0.0, 1.0);
        if (has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_SHARED_DAMAGE_RATIO)) {
            ratio = tuning.get(Phase7AbilityTuning.Setting.BRAMBLE_SHARED_DAMAGE_RATIO, ratio);
        }
        if (has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_TANGLED_SHARED_DAMAGE_RATIO)) {
            ratio = tuning.get(Phase7AbilityTuning.Setting.BRAMBLE_TANGLED_SHARED_DAMAGE_RATIO, ratio);
        }
        return Math.clamp(ratio * tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_ANCIENT_SHARED_DAMAGE_MULTIPLIER, 1), 0.0, 1.0);
    }

    public static double slamDamageMultiplier(Phase7AbilityTuning tuning) {
        return tuning.get(Phase7AbilityTuning.Setting.BRAMBLE_SLAM_DAMAGE_MULTIPLIER, 1)
                * tuning.get(Phase7AbilityTuning.Setting.BRAMBLE_HANGMAN_SLAM_DAMAGE_MULTIPLIER, 1);
    }

    public static double liftForceMultiplier(Phase7AbilityTuning tuning) {
        return tuning.get(Phase7AbilityTuning.Setting.BRAMBLE_LIFT_FORCE_MULTIPLIER, 1)
                * tuning.get(Phase7AbilityTuning.Setting.BRAMBLE_HANGMAN_LIFT_FORCE_MULTIPLIER, 1);
    }

    public static int graspTargetCap(int configured, Phase7AbilityTuning tuning) {
        int cap = Math.clamp(configured, 1, 12);
        if (has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_TANGLED_TARGET_CAP)) {
            cap = tuning.integer(Phase7AbilityTuning.Setting.BRAMBLE_TANGLED_TARGET_CAP, 10);
        }
        if (has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_HANGMAN_TARGET_CAP)) {
            cap = tuning.integer(Phase7AbilityTuning.Setting.BRAMBLE_HANGMAN_TARGET_CAP, 1);
        }
        return Math.clamp(cap, 1, 12);
    }

    public static int huntMemoryDuration(int configured, Phase7AbilityTuning tuning) {
        if (has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_PREDATOR_MEMORY_TICKS)) {
            return Math.max(1, tuning.integer(
                    Phase7AbilityTuning.Setting.BRAMBLE_PREDATOR_MEMORY_TICKS, 30));
        }
        return Math.max(1, configured + tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_HUNT_MEMORY_BONUS_TICKS, 0));
    }

    public static int huntCooldown(int configured, Phase7AbilityTuning tuning) {
        return Math.max(4, configured + tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_HUNT_COOLDOWN_BONUS_TICKS, 0));
    }

    public static double huntRange(double configured, Phase7AbilityTuning tuning) {
        return Math.clamp(Math.max(0.5, configured) + tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_HUNT_RANGE_BONUS, 0), 0.5, 128.0);
    }

    public static double huntWidth(double configured, Phase7AbilityTuning tuning) {
        return Math.clamp(Math.max(0.1, configured) + tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_HUNT_WIDTH_BONUS, 0), 0.1, MAX_HUNT_RADIUS);
    }

    public static double huntDamageMultiplier(Phase7AbilityTuning tuning) {
        return tuning.get(Phase7AbilityTuning.Setting.BRAMBLE_HUNT_DAMAGE_MULTIPLIER, 1)
                * tuning.get(Phase7AbilityTuning.Setting.BRAMBLE_PREDATOR_DAMAGE_MULTIPLIER, 1);
    }

    public static int huntSlowDuration(int configured, Phase7AbilityTuning tuning) {
        if (has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_WALTZ_DISABLE_SLOW)) return 0;
        return Math.max(0, configured + tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_HUNT_SLOW_DURATION_BONUS_TICKS, 0));
    }

    public static int huntSlowAmplifier(int configured, Phase7AbilityTuning tuning) {
        return Math.max(Math.max(0, configured), tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_HUNT_MIN_SLOW_AMPLIFIER, 0));
    }

    public static int huntTargetCap(Phase7AbilityTuning tuning) {
        if (has(tuning, Phase7AbilityTuning.Setting.BRAMBLE_PREDATOR_TARGET_ONLY)) return 1;
        return Math.clamp(tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_HUNT_PATH_TARGET_CAP, 1), 1, 16);
    }

    public static int remainingCooldown(int effectiveTotal, long elapsedTicks, int refundedTicks) {
        return Math.max(0, effectiveTotal - (int) Math.max(0, elapsedTicks) - Math.max(0, refundedTicks));
    }

    private static boolean has(Phase7AbilityTuning tuning, Phase7AbilityTuning.Setting setting) {
        return tuning != null && tuning.has(setting);
    }

    private static boolean isBrambleWeaponHit(DamageSource source, LivingEntity actor) {
        ItemStack sourceStack = source.getWeaponStack();
        if (sourceStack != null && sourceStack.isOf(ItemsRegistry.BRAMBLETHORN.get())) {
            return true;
        }
        return actor != null && (actor.getMainHandStack().isOf(ItemsRegistry.BRAMBLETHORN.get())
                || actor.getOffHandStack().isOf(ItemsRegistry.BRAMBLETHORN.get()));
    }

    private static List<LivingEntity> findTargets(WeaponAbilityContext context,
                                                   LivingEntity anchorTarget, Vec3d center,
                                                   Phase7AbilityTuning tuning) {
        double radius = graspRadius(Config.uniqueEffects.bramblethorn.captureRadius, tuning);
        int maximum = graspTargetCap(Config.uniqueEffects.bramblethorn.maximumTargets, tuning);
        Box box = Box.of(center.add(0.0, anchorTarget.getHeight() * 0.5, 0.0),
                radius * 2.0, Math.max(4.0, radius * 1.5), radius * 2.0);
        List<LivingEntity> candidates = new ArrayList<>(context.world().getEntitiesByClass(
                LivingEntity.class, box,
                target -> EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && isEligibleTarget(context.actor(), context.sourcePlayer(), context.world(), target)
                        && target.getPos().squaredDistanceTo(center) <= radius * radius));
        candidates.remove(anchorTarget);
        candidates.sort(Comparator.comparingDouble(target -> target.squaredDistanceTo(center)));
        candidates.addFirst(anchorTarget);
        if (candidates.size() > maximum) {
            return new ArrayList<>(candidates.subList(0, maximum));
        }
        return candidates;
    }

    private static LivingEntity resolveTarget(WeaponAbilityContext context) {
        if (context == null || context.actor() == null) {
            return null;
        }
        if (context.activationSource() == WeaponAbilityActivationSource.PLAYER
                && context.actor() instanceof PlayerEntity player) {
            return findPlayerTarget(player);
        }
        LivingEntity target = context.target();
        return isEligibleTarget(context.actor(), context.sourcePlayer(), context.world(), target)
                ? target : null;
    }

    private static boolean isEligibleTarget(LivingEntity actor, LivingEntity principal,
                                            net.minecraft.world.World world, LivingEntity target) {
        return actor != null
                && world != null
                && target != null
                && target.isAlive()
                && !target.isRemoved()
                && target != actor
                && target.getWorld() == world
                && HelperMethods.checkAbilityTarget(target, actor)
                && (principal == null || target != principal
                && HelperMethods.checkAbilityTarget(target, principal));
    }

    private static void removeInvalidTargets(ServerWorld world, ActiveGrasp grasp) {
        Iterator<BoundTarget> iterator = grasp.targets.iterator();
        while (iterator.hasNext()) {
            BoundTarget bound = iterator.next();
            LivingEntity target = resolveLiving(world, bound.targetId);
            if (target != null && target.isAlive() && !target.isRemoved()) {
                continue;
            }
            if (target != null && !target.isAlive()) applyBindKill(world, grasp);
            beginVisualFade(world, bound.visualId);
            iterator.remove();
        }
    }

    private static void applyBindKill(ServerWorld world, ActiveGrasp grasp) {
        LivingEntity actor = resolveLiving(world, grasp.actorId);
        if (actor == null) return;
        if (has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_SAP_RISE_ABSORPTION)
                && !has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_BRIAR_DAMAGE_MULTIPLIER)) {
            float cap = (float) grasp.tuning.get(Phase7AbilityTuning.Setting.BRAMBLE_SAP_RISE_CAP, 8);
            float amount = Math.min((float) grasp.tuning.get(
                    Phase7AbilityTuning.Setting.BRAMBLE_SAP_RISE_ABSORPTION, 2),
                    Math.max(0.0F, cap - grasp.sapAbsorptionGranted));
            if (amount > 0.0F) {
                actor.setAbsorptionAmount(actor.getAbsorptionAmount() + amount);
                grasp.sapAbsorptionGranted += amount;
            }
        }
        if (has(grasp.tuning, Phase7AbilityTuning.Setting.BRAMBLE_RECLAIMED_REFUND_TICKS)) {
            int refund = Math.min(grasp.tuning.integer(
                            Phase7AbilityTuning.Setting.BRAMBLE_RECLAIMED_REFUND_TICKS, 12),
                    Math.max(0, grasp.tuning.integer(
                            Phase7AbilityTuning.Setting.BRAMBLE_RECLAIMED_REFUND_CAP_TICKS, 48)
                            - grasp.refunded));
            grasp.refunded += refund;
            if (actor instanceof PlayerEntity player) {
                int effectiveTotal = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                        grasp.stack, actor, grasp.execution.cooldownTicks(
                                Config.uniqueEffects.bramblethorn.cooldown));
                player.getItemCooldownManager().set(grasp.stack.getItem(), remainingCooldown(
                        effectiveTotal, world.getTime() - grasp.startedAt, grasp.refunded));
            } else {
                WeaponAbilityCooldownManager.reduceCooldown(world, actor, grasp.stack, refund);
            }
        }
        UniqueAbilityApi.emit(grasp.execution, UniqueAbilityPhase.HIT, Phase7UniqueAbilities.KILL,
                null, 1, grasp.refunded);
    }

    private static void applyBriarSacrifice(ServerWorld world, LivingEntity actor, ActiveGrasp grasp) {
        LivingEntity principal = resolveLiving(world, grasp.principalId);
        if (principal == null) principal = actor;
        LivingEntity damagePrincipal = principal;
        double radius = Math.min(MAX_HUNT_RADIUS, grasp.tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_BRIAR_RADIUS, 2));
        float damage = grasp.slamDamage * (float) grasp.tuning.get(
                Phase7AbilityTuning.Setting.BRAMBLE_BRIAR_DAMAGE_MULTIPLIER, .5);
        int cap = Math.max(1, grasp.tuning.integer(
                Phase7AbilityTuning.Setting.BRAMBLE_BRIAR_TARGET_CAP, 16));
        Set<UUID> damaged = new HashSet<>();
        for (BoundTarget bound : grasp.targets) {
            LivingEntity center = resolveLiving(world, bound.targetId);
            if (center == null || !center.isAlive()) continue;
            List<LivingEntity> candidates = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class,
                    center.getBoundingBox().expand(radius), candidate -> candidate.isAlive()
                            && HelperMethods.checkAbilityTarget(candidate, actor)
                            && !damaged.contains(candidate.getUuid())
                            && candidate.squaredDistanceTo(center) <= radius * radius));
            candidates.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(center)));
            for (LivingEntity target : candidates) {
                if (damaged.size() >= cap) break;
                damaged.add(target.getUuid());
                DamageSource source = world.getDamageSources().indirectMagic(actor, damagePrincipal);
                float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                        world, grasp.stack, target, source, damage);
                WeaponImplicitRegistry.runSuppressed(() ->
                        HelperMethods.damageThroughIframes(target, source, finalDamage));
            }
            if (damaged.size() >= cap) break;
        }
    }

    private static void triggerVisualPulses(ServerWorld world, ActiveGrasp grasp) {
        triggerVisualPulse(world, grasp.coreVisualId);
        for (BoundTarget bound : grasp.targets) {
            triggerVisualPulse(world, bound.visualId);
        }
    }

    private static void triggerVisualPulse(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        if (entity instanceof BrambleRootVisualEntity visual) {
            visual.triggerPulse();
        }
    }

    private static void setAllVisualPhases(ServerWorld world, ActiveGrasp grasp, int phase) {
        setVisualPhase(world, grasp.coreVisualId, phase);
        for (BoundTarget bound : grasp.targets) {
            setVisualPhase(world, bound.visualId, phase);
        }
    }

    private static void setVisualPhase(ServerWorld world, UUID id, int phase) {
        Entity entity = id == null ? null : world.getEntity(id);
        if (entity instanceof BrambleRootVisualEntity visual) {
            visual.setPhase(phase);
        }
    }

    private static void fadeVisuals(ServerWorld world, ActiveGrasp grasp) {
        beginVisualFade(world, grasp.coreVisualId);
        for (BoundTarget bound : grasp.targets) {
            beginVisualFade(world, bound.visualId);
        }
    }

    private static void discardVisuals(ServerWorld world, ActiveGrasp grasp) {
        discardVisual(world, grasp.coreVisualId);
        for (BoundTarget bound : grasp.targets) discardVisual(world, bound.visualId);
    }

    private static void discardVisual(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        if (entity instanceof BrambleRootVisualEntity) entity.discard();
    }

    private static void beginVisualFade(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        if (entity instanceof BrambleRootVisualEntity visual) {
            visual.beginFade(FADE_TICKS);
        }
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity actor, Vec3d center) {
        world.playSound(null, actor.getBlockPos(), SoundEvents.BLOCK_ROOTED_DIRT_PLACE,
                SoundCategory.PLAYERS, 1.0F, 0.62F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SPORE_BLOSSOM_PLACE,
                SoundCategory.PLAYERS, 0.85F, 0.72F);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,
                        net.minecraft.block.Blocks.ROOTED_DIRT.getDefaultState()),
                center.x, center.y + 0.12, center.z, 26, 0.65, 0.12, 0.65, 0.08);
    }

    private static void spawnBindingEffects(ServerWorld world, Vec3d center) {
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_CAVE_VINES_PLACE,
                SoundCategory.PLAYERS, 1.05F, 0.55F);
        world.spawnParticles(ParticleTypes.COMPOSTER, center.x, center.y + 0.3, center.z,
                24, 1.4, 0.3, 1.4, 0.08);
        world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 0.8, center.z,
                30, 1.8, 0.65, 1.8, 0.015);
    }

    private static void spawnPropagationEffects(ServerWorld world, LivingEntity source) {
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                source.getX(), source.getBodyY(0.55), source.getZ(),
                5, source.getWidth() * 0.32, source.getHeight() * 0.22,
                source.getWidth() * 0.32, 0.01);
        world.playSound(null, source.getBlockPos(), SoundEvents.BLOCK_CAVE_VINES_PICK_BERRIES,
                SoundCategory.PLAYERS, 0.45F, 0.75F);
    }

    private static void spawnImpactEffects(ServerWorld world, Vec3d position, float width) {
        double spread = Math.max(0.45, width * 0.7);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,
                        net.minecraft.block.Blocks.ROOTED_DIRT.getDefaultState()),
                position.x, position.y + 0.12, position.z,
                30, spread, 0.18, spread, 0.16);
        world.spawnParticles(ParticleTypes.COMPOSTER,
                position.x, position.y + 0.35, position.z,
                18, spread, 0.35, spread, 0.12);
        world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                position.x, position.y + 0.75, position.z,
                22, spread * 1.3, 0.55, spread * 1.3, 0.025);
        world.playSound(null, position.x, position.y, position.z,
                SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.PLAYERS, 1.1F, 0.52F);
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static ActiveGrasp getActive(ServerWorld world, UUID actorId) {
        Map<UUID, ActiveGrasp> active = ACTIVE.get(world);
        return active == null ? null : active.get(actorId);
    }

    private static final class ActiveGrasp {
        private final UUID actorId;
        private final UUID principalId;
        private final ItemStack stack;
        private final Vec3d center;
        private final long startedAt;
        private final long growthEndsAt;
        private final long bindingEndsAt;
        private final float slamDamage;
        private final Phase7AbilityTuning tuning;
        private final UniqueAbilityExecution execution;
        private final List<BoundTarget> targets = new ArrayList<>();
        private UUID coreVisualId;
        private int phase = PHASE_GROWING;
        private long phaseStartedAt;
        private long lastPropagationTick = Long.MIN_VALUE;
        private int refunded;
        private float sapAbsorptionGranted;
        private final Map<UUID, Long> retortCooldowns = new HashMap<>();

        private ActiveGrasp(UUID actorId, UUID principalId, ItemStack stack, Vec3d center,
                            long startedAt, long growthEndsAt, long bindingEndsAt,
                            float slamDamage, Phase7AbilityTuning tuning,
                            UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.principalId = principalId;
            this.stack = stack;
            this.center = center;
            this.startedAt = startedAt;
            this.growthEndsAt = growthEndsAt;
            this.bindingEndsAt = bindingEndsAt;
            this.slamDamage = slamDamage;
            this.tuning = tuning;
            this.execution = execution;
            this.phaseStartedAt = startedAt;
        }

        private boolean containsTarget(UUID targetId) {
            for (BoundTarget target : targets) {
                if (target.targetId.equals(targetId)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class BoundTarget {
        private final UUID targetId;
        private final UUID visualId;
        private final Vec3d slot;
        private boolean lifted;
        private boolean slammed;

        private BoundTarget(UUID targetId, UUID visualId, Vec3d slot) {
            this.targetId = targetId;
            this.visualId = visualId;
            this.slot = slot;
        }
    }

    private static final class HuntMark {
        private final UUID targetId;
        private final long expiresAt;
        private final long nextProcAt;

        private HuntMark(UUID targetId, long expiresAt, long nextProcAt) {
            this.targetId = targetId;
            this.expiresAt = expiresAt;
            this.nextProcAt = nextProcAt;
        }
    }

    private static final class ActiveHunt {
        private final UUID attackerId;
        private final UUID targetId;
        private final ItemStack stack;
        private final Vec3d origin;
        private final long startedAt;
        private final int travelTicks;
        private final double hitRadius;
        private final float damage;
        private final int slowDuration;
        private final int slowAmplifier;
        private final UUID visualId;
        private final int targetCap;
        private final boolean targetOnly;
        private final UniqueAbilityExecution execution;
        private final Set<UUID> hitTargets = new HashSet<>();
        private float previousProgress;

        private ActiveHunt(UUID attackerId, UUID targetId, ItemStack stack,
                           Vec3d origin, long startedAt, int travelTicks,
                           double hitRadius, float damage, int slowDuration,
                           int slowAmplifier, int targetCap, boolean targetOnly, UUID visualId,
                           UniqueAbilityExecution execution) {
            this.attackerId = attackerId;
            this.targetId = targetId;
            this.stack = stack;
            this.origin = origin;
            this.startedAt = startedAt;
            this.travelTicks = travelTicks;
            this.hitRadius = hitRadius;
            this.damage = damage;
            this.slowDuration = slowDuration;
            this.slowAmplifier = slowAmplifier;
            this.targetCap = targetCap;
            this.targetOnly = targetOnly;
            this.visualId = visualId;
            this.execution = execution;
        }
    }

    private record HuntStep(UUID targetId, long tick) {
    }

    private static final class HuntSequence {
        private final ArrayDeque<HuntStep> steps = new ArrayDeque<>();
        private int windowTicks = 100;
    }
}
