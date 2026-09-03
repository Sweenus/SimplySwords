package net.sweenus.simplyswords.world;

import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.ShadowstingAfterimageVisualEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class ShadowstingShadowDanceManager {

    private static final int FINISH_TRANSLATION_TICKS = 8;
    private static final int AFTERIMAGE_LIFETIME = 6;
    private static final int PASSIVE_CLONE_DELAY_TICKS = 3;
    private static final int MAX_CLONE_CHAIN_DEPTH = 32;
    private static final Map<UUID, ActiveShadowDance> ACTIVE_DANCES = new HashMap<>();
    private static final Map<ServerWorld, List<PendingShadowCloneStrike>> PENDING_CLONE_STRIKES = new HashMap<>();
    private static final ThreadLocal<Integer> CURRENT_CLONE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Map<UUID, Integer> PASSIVE_PROCS = new HashMap<>();
    private static final Map<UUID, Long> PASSIVE_LOCKOUTS = new HashMap<>();
    private static final Map<UUID, Map<UUID, Long>> UMBRAL_MARKS = new HashMap<>();
    private static final Set<UUID> VEILED_OWNERS = new HashSet<>();
    private static boolean initialized;

    private ShadowstingShadowDanceManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        PlayerEvent.PLAYER_QUIT.register(ShadowstingShadowDanceManager::onPlayerQuit);
    }

    public static int cloneChance(int configured, Phase8AbilityTuning tuning) {
        return Math.clamp(configured
                + tuning.integer(Phase8AbilityTuning.Setting.SHADOW_CHANCE_BONUS, 0)
                - tuning.integer(Phase8AbilityTuning.Setting.SHADOW_CHANCE_PENALTY, 0), 0, 100);
    }

    public static int cloneDelay(int configured, Phase8AbilityTuning tuning) {
        return Math.max(1, configured
                + tuning.integer(Phase8AbilityTuning.Setting.SHADOW_CLONE_DELAY_BONUS_TICKS, 0));
    }

    public static double cloneDamageMultiplier(Phase8AbilityTuning tuning) {
        double multiplier = tuning.get(Phase8AbilityTuning.Setting.SHADOW_CLONE_DAMAGE_MULTIPLIER, 1);
        if (tuning.flag(1 << 7)) multiplier *= tuning.get(
                Phase8AbilityTuning.Setting.SHADOW_MIRROR_DAMAGE_MULTIPLIER, .45);
        if (tuning.flag(1 << 8)) multiplier *= tuning.get(
                Phase8AbilityTuning.Setting.SHADOW_FLAWLESS_DAMAGE_MULTIPLIER, 1.25);
        return multiplier;
    }

    public static int cloneCount(Phase8AbilityTuning tuning) {
        return tuning.flag(1 << 7)
                ? Math.max(1, tuning.integer(Phase8AbilityTuning.Setting.SHADOW_MIRROR_COUNT, 3)) : 1;
    }

    public static int danceDuration(int configuredDuration, int configuredInterval,
                                    Phase8AbilityTuning tuning) {
        double duration = configuredDuration
                + tuning.integer(Phase8AbilityTuning.Setting.SHADOW_DANCE_DURATION_BONUS_TICKS, 0);
        duration *= tuning.get(Phase8AbilityTuning.Setting.SHADOW_DANCE_DURATION_MULTIPLIER, 1);
        if (tuning.flag(1 << 26)) duration -= (double) strikeInterval(configuredInterval, tuning)
                * tuning.integer(Phase8AbilityTuning.Setting.SHADOW_KILLING_SKIPPED_STRIKES, 2);
        return Math.max(1, (int) Math.round(duration));
    }

    public static int strikeInterval(int configured, Phase8AbilityTuning tuning) {
        int floor = tuning.integer(Phase8AbilityTuning.Setting.SHADOW_DANCE_INTERVAL_FLOOR,
                Math.max(1, configured));
        return Math.max(Math.max(1, floor), configured
                + tuning.integer(Phase8AbilityTuning.Setting.SHADOW_DANCE_INTERVAL_BONUS, 0));
    }

    public static double danceRadius(double configured, Phase8AbilityTuning tuning) {
        return Math.max(1, configured
                + tuning.get(Phase8AbilityTuning.Setting.SHADOW_DANCE_RADIUS_BONUS, 0));
    }

    public static double danceStrikeMultiplier(Phase8AbilityTuning tuning) {
        double multiplier = 1;
        if (tuning.flag(1 << 16)) multiplier *= tuning.get(
                Phase8AbilityTuning.Setting.SHADOW_MACABRE_DAMAGE_MULTIPLIER, .65);
        if (tuning.flag(1 << 17)) multiplier *= tuning.get(
                Phase8AbilityTuning.Setting.SHADOW_WALTZ_DAMAGE_MULTIPLIER, 1.5);
        return multiplier;
    }

    public static double chainMultiplier(int chainSteps, Phase8AbilityTuning tuning) {
        if (!tuning.flag(1 << 12)) return 1;
        int cap = tuning.integer(Phase8AbilityTuning.Setting.SHADOW_CHAIN_STEP_CAP, 5);
        return 1 + Math.min(Math.max(0, cap), chainSteps)
                * tuning.get(Phase8AbilityTuning.Setting.SHADOW_CHAIN_DAMAGE_PER_STEP, .05);
    }

    public static double arrivalDistance(Phase8AbilityTuning tuning) {
        return 1.35 + (tuning.flag(1 << 18)
                ? tuning.get(Phase8AbilityTuning.Setting.SHADOW_ARRIVAL_DISTANCE_BONUS, 1) : 0);
    }

    public static int returnTicks(Phase8AbilityTuning tuning) {
        return Math.max(1, FINISH_TRANSLATION_TICKS
                + tuning.integer(Phase8AbilityTuning.Setting.SHADOW_RETURN_BONUS_TICKS, 0));
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        PENDING_CLONE_STRIKES.remove(world);
        String worldKey = world.getRegistryKey().getValue().toString();
        ACTIVE_DANCES.entrySet().removeIf(entry -> entry.getValue().worldKey.equals(worldKey));
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        UUID actorId = actor.getUuid();
        ACTIVE_DANCES.remove(actorId);
        PASSIVE_PROCS.remove(actorId);
        PASSIVE_LOCKOUTS.remove(actorId);
        UMBRAL_MARKS.remove(actorId);
        VEILED_OWNERS.remove(actorId);
        PENDING_CLONE_STRIKES.values().forEach(strikes ->
                strikes.removeIf(strike -> strike.ownerId().equals(actorId)));
        PENDING_CLONE_STRIKES.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (actor instanceof ServerPlayerEntity player) {
            player.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.SHADOW_DANCE));
        }
    }

    public static void clearAll() {
        ACTIVE_DANCES.clear();
        PENDING_CLONE_STRIKES.clear();
        PASSIVE_PROCS.clear();
        PASSIVE_LOCKOUTS.clear();
        UMBRAL_MARKS.clear();
        VEILED_OWNERS.clear();
    }

    public static boolean start(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
        return start(world, player, stack, Phase8AbilityTuning.EMPTY);
    }

    public static boolean start(ServerWorld world, ServerPlayerEntity player, ItemStack stack,
                                Phase8AbilityTuning tuning) {
        if (world == null || player == null || ACTIVE_DANCES.containsKey(player.getUuid())) {
            return false;
        }

        LivingEntity target = findRandomTarget(world, player, tuning);
        if (target == null) {
            spawnFailParticles(world, player);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                    SoundCategory.PLAYERS, 0.45F, 0.55F);
            return false;
        }

        int activeDuration = getActiveDurationTicks(tuning);
        ActiveShadowDance dance = new ActiveShadowDance(
                world.getRegistryKey().getValue().toString(),
                world.getTime() + activeDuration,
                world.getTime(),
                player.getPos(),
                stack.copy(),
                tuning
        );
        ACTIVE_DANCES.put(player.getUuid(), dance);
        dance.focusTarget = target.getUuid();
        player.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.SHADOW_DANCE),
                activeDuration + returnTicks(tuning) + 2,
                Math.max(0, activeDuration - 1),
                false,
                false,
                false
        ), player);
        if (tuning.flag(1 << 14)) player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE, activeDuration + returnTicks(tuning) + 2,
                tuning.integer(Phase8AbilityTuning.Setting.SHADOW_GUARD_AMPLIFIER, 0)), player);
        if (tuning.flag(1 << 20)) {
            world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(
                            tuning.get(Phase8AbilityTuning.Setting.SHADOW_SNARE_RADIUS, 3)),
                            entity -> entity != player && HelperMethods.checkAbilityTarget(entity, player))
                    .stream().limit(tuning.integer(Phase8AbilityTuning.Setting.SHADOW_SNARE_TARGET_CAP, 6))
                    .forEach(entity -> entity.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS,
                            tuning.integer(Phase8AbilityTuning.Setting.SHADOW_SNARE_DURATION_TICKS, 40),
                            tuning.integer(Phase8AbilityTuning.Setting.SHADOW_SNARE_AMPLIFIER, 1)), player));
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.75F, 1.25F);
        spawnDepartureParticles(world, player.getPos().add(0.0, player.getHeight() * 0.5, 0.0));
        performStrike(world, player, dance, target);
        return true;
    }

    public static boolean start(ServerWorld world, LivingEntity actor, LivingEntity target, ItemStack stack) {
        return start(world, actor, target, stack, Phase8AbilityTuning.EMPTY);
    }

    public static boolean start(ServerWorld world, LivingEntity actor, LivingEntity target, ItemStack stack,
                                Phase8AbilityTuning tuning) {
        if (world == null || actor == null || target == null || !actor.isAlive()
                || !target.isAlive() || !HelperMethods.checkAbilityTarget(target, actor)) {
            return false;
        }

        Vec3d previousPos = actor.getPos();
        int strikes = Math.max(1, getActiveDurationTicks(tuning) / Math.max(1, getStrikeIntervalTicks(tuning)));
        for (int i = 0; i < Math.min(6, strikes); i++) {
            if (!target.isAlive() || !HelperMethods.checkAbilityTarget(target, actor)) {
                break;
            }
            Vec3d strikePos = findStrikePosition(world, actor, target);
            Vec3d lookTarget = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
            spawnDepartureParticles(world, previousPos.add(0.0, actor.getHeight() * 0.5, 0.0));
            performWeaponStrike(actor, target, stack, danceStrikeMultiplier(tuning));
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundRegistry.DARK_SWORD_WHOOSH_01.get(),
                    SoundCategory.PLAYERS, 0.45F, 1.45F + world.random.nextFloat() * 0.25F);
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.PLAYERS, 0.35F, 0.85F + world.random.nextFloat() * 0.2F);
            spawnArrivalParticles(world, target, strikePos);
            spawnTrail(world, previousPos.add(0.0, actor.getHeight() * 0.5, 0.0), lookTarget);
            spawnShadowEcho(world, strikePos, lookTarget, actor.getHeight());
            previousPos = strikePos;
        }
        return true;
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        ActiveShadowDance dance = ACTIVE_DANCES.get(player.getUuid());
        if (dance == null) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        if (!dance.worldKey.equals(world.getRegistryKey().getValue().toString()) || !player.isAlive()) {
            finishNow(player);
            player.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.SHADOW_DANCE));
            return;
        }

        if (dance.finishing) {
            tickFinishTranslation(world, player, dance);
            return;
        }

        if (world.getTime() >= dance.endTick) {
            beginFinishTranslation(world, player, dance);
            return;
        }

        lockPlayer(player, dance);
        clearMobTargets(world, player);

        if (world.getTime() >= dance.nextStrikeTick) {
            LivingEntity target = dance.tuning.flag(1 << 17) && dance.focusTarget != null
                    && world.getEntity(dance.focusTarget) instanceof LivingEntity focused && focused.isAlive()
                    ? focused : findRandomTarget(world, player, dance.tuning);
            if (target != null) {
                performStrike(world, player, dance, target);
            } else {
                dance.nextStrikeTick = world.getTime() + getStrikeIntervalTicks(dance.tuning);
                spawnIdleParticles(world, player);
            }
        } else if (Config.general.enableModernFieldEffects && world.getTime() % 3L == 0L) {
            spawnIdleParticles(world, player);
        }
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return player != null && ACTIVE_DANCES.containsKey(player.getUuid());
    }

    public static boolean hasPendingCloneStrikes(ServerWorld world) {
        List<PendingShadowCloneStrike> strikes = PENDING_CLONE_STRIKES.get(world);
        return strikes != null && !strikes.isEmpty();
    }

    public static void tickCloneStrikes(ServerWorld world) {
        List<PendingShadowCloneStrike> strikes = PENDING_CLONE_STRIKES.get(world);
        if (strikes == null || strikes.isEmpty()) {
            return;
        }

        List<PendingShadowCloneStrike> dueStrikes = new java.util.ArrayList<>();
        Iterator<PendingShadowCloneStrike> iterator = strikes.iterator();
        while (iterator.hasNext()) {
            PendingShadowCloneStrike strike = iterator.next();
            if (world.getTime() < strike.triggerTick()) {
                continue;
            }
            iterator.remove();
            dueStrikes.add(strike);
        }
        if (strikes.isEmpty()) {
            PENDING_CLONE_STRIKES.remove(world);
        }
        for (PendingShadowCloneStrike strike : dueStrikes) {
            executePassiveCloneStrike(world, strike);
        }
    }

    public static void schedulePassiveCloneStrike(ServerWorld world, ServerPlayerEntity owner, LivingEntity target) {
        schedulePassiveCloneStrike(world, owner, target, Phase8AbilityTuning.EMPTY);
    }

    public static void schedulePassiveCloneStrike(ServerWorld world, ServerPlayerEntity owner, LivingEntity target,
                                                  Phase8AbilityTuning tuning) {
        if (world == null || owner == null || target == null || !owner.isAlive() || !target.isAlive()) {
            return;
        }

        int chainDepth = CURRENT_CLONE_DEPTH.get() + 1;
        if (CURRENT_CLONE_DEPTH.get() > 0 && (tuning.flag(1 << 7) || tuning.flag(1 << 8))) return;
        if (chainDepth > MAX_CLONE_CHAIN_DEPTH) {
            return;
        }

        List<PendingShadowCloneStrike> pending = PENDING_CLONE_STRIKES.computeIfAbsent(world,
                ignored -> new java.util.ArrayList<>());
        int delay = cloneDelay(PASSIVE_CLONE_DELAY_TICKS, tuning);
        double multiplier = cloneDamageMultiplier(tuning);
        for (int i = 0; i < cloneCount(tuning); i++) pending.add(new PendingShadowCloneStrike(
                owner.getUuid(), target.getUuid(), world.getTime() + delay,
                chainDepth, tuning, multiplier));
        int proc = PASSIVE_PROCS.merge(owner.getUuid(), 1, Integer::sum);
        if (tuning.flag(1 << 5) && proc % Math.max(1, tuning.integer(
                Phase8AbilityTuning.Setting.SHADOW_TWIN_INTERVAL, 4)) == 0) {
            pending.add(new PendingShadowCloneStrike(owner.getUuid(), target.getUuid(),
                    world.getTime() + tuning.integer(Phase8AbilityTuning.Setting.SHADOW_TWIN_DELAY_TICKS, 5),
                    chainDepth, tuning, multiplier * tuning.get(
                    Phase8AbilityTuning.Setting.SHADOW_TWIN_DAMAGE_MULTIPLIER, .5)));
        }
        if (tuning.flag(1 << 8)) PASSIVE_LOCKOUTS.put(owner.getUuid(), world.getTime()
                + tuning.integer(Phase8AbilityTuning.Setting.SHADOW_FLAWLESS_LOCKOUT_TICKS, 60));
    }

    public static boolean canPassiveProc(ServerWorld world, ServerPlayerEntity owner, Phase8AbilityTuning tuning) {
        return !tuning.flag(1 << 8) || PASSIVE_LOCKOUTS.getOrDefault(owner.getUuid(), 0L) <= world.getTime();
    }

    public static void applyUmbralMarkBonus(ServerWorld world, ServerPlayerEntity owner, LivingEntity target,
                                            Phase8AbilityTuning tuning) {
        if (!tuning.flag(1 << 3)) return;
        Map<UUID, Long> marks = UMBRAL_MARKS.get(owner.getUuid());
        if (marks == null) return;
        marks.values().removeIf(expiry -> expiry <= world.getTime());
        if (marks.isEmpty()) {
            UMBRAL_MARKS.remove(owner.getUuid());
            return;
        }
        if (marks.getOrDefault(target.getUuid(), 0L) <= world.getTime()) return;
        float bonus = (float) (HelperMethods.getEntityAttackDamage(owner)
                * (tuning.get(Phase8AbilityTuning.Setting.SHADOW_MARK_OUTGOING_MULTIPLIER, 1.08) - 1));
        if (bonus > 0) target.damage(owner.getDamageSources().playerAttack(owner), bonus);
    }

    public static void end(ServerPlayerEntity player) {
        finishNow(player);
    }

    private static void onPlayerQuit(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        clearActor(player);
    }

    private static void finishNow(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }

        if (ACTIVE_DANCES.remove(player.getUuid()) != null) {
            lockPlayer(player);
        }
    }

    private static void beginFinishTranslation(ServerWorld world, ServerPlayerEntity player, ActiveShadowDance dance) {
        if (dance.lastStrikePos == null) {
            finishNow(player);
            return;
        }

        if (dance.tuning.flag(1 << 15) && !dance.tuning.flag(1 << 25)) {
            float flourish = (float) dance.tuning.get(
                    Phase8AbilityTuning.Setting.SHADOW_FLOURISH_DAMAGE_MULTIPLIER, .6);
            float damage = HelperMethods.abilityScaledDamage("soul", player, dance.stack,
                    Config.uniqueEffects.shadowsting.damageScaling * flourish,
                    Config.uniqueEffects.shadowsting.spellScaling * flourish);
            world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(
                            dance.tuning.get(Phase8AbilityTuning.Setting.SHADOW_FLOURISH_RADIUS, 3)),
                            target -> HelperMethods.checkAbilityTarget(target, player))
                    .stream().limit(dance.tuning.integer(
                            Phase8AbilityTuning.Setting.SHADOW_FLOURISH_TARGET_CAP, 8))
                    .forEach(target -> target.damage(
                            player.getDamageSources().playerAttack(player), damage));
        }
        if (dance.tuning.flag(1 << 25)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,
                    dance.tuning.integer(Phase8AbilityTuning.Setting.SHADOW_NOCTURNE_DURATION_TICKS, 60), 0), player);
            finishNow(player);
            return;
        }
        if (dance.tuning.flag(1 << 26)) {
            player.networkHandler.requestTeleport(dance.lastStrikePos.x, dance.lastStrikePos.y,
                    dance.lastStrikePos.z, player.getYaw(), player.getPitch());
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    dance.tuning.integer(Phase8AbilityTuning.Setting.SHADOW_KILLING_RESISTANCE_TICKS, 30),
                    dance.tuning.integer(Phase8AbilityTuning.Setting.SHADOW_KILLING_RESISTANCE_AMPLIFIER, 2)), player);
            applyArrivalEffects(world, player, dance);
            finishNow(player);
            return;
        }
        dance.finishing = true;
        dance.finishStartTick = world.getTime();
        dance.finishStartPos = player.getPos();
        dance.finishEndPos = dance.lastStrikePos;
        float[] rotation = dance.lastLookTarget == null
                ? new float[]{player.getYaw(), player.getPitch()}
                : getFacingRotation(dance.finishEndPos.add(0.0, player.getEyeHeight(player.getPose()), 0.0), dance.lastLookTarget);
        dance.finishStartYaw = player.getYaw();
        dance.finishStartPitch = player.getPitch();
        dance.finishEndYaw = rotation[0];
        dance.finishEndPitch = rotation[1];

        spawnDepartureParticles(world, player.getPos().add(0.0, player.getHeight() * 0.5, 0.0));
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.25F, 0.65F);
        tickFinishTranslation(world, player, dance);
    }

    private static void tickFinishTranslation(ServerWorld world, ServerPlayerEntity player, ActiveShadowDance dance) {
        lockPlayer(player);
        if (dance.finishStartPos == null || dance.finishEndPos == null) {
            finishNow(player);
            return;
        }

        int translationTicks = returnTicks(dance.tuning);
        float progress = MathHelper.clamp((float) (world.getTime() - dance.finishStartTick + 1L) / translationTicks, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        Vec3d pos = dance.finishStartPos.lerp(dance.finishEndPos, eased);
        float yaw = MathHelper.lerpAngleDegrees(eased, dance.finishStartYaw, dance.finishEndYaw);
        float pitch = MathHelper.lerp(eased, dance.finishStartPitch, dance.finishEndPitch);

        player.networkHandler.requestTeleport(pos.x, pos.y, pos.z, yaw, pitch);
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
        player.setBodyYaw(yaw);
        if (Config.general.enableModernFieldEffects && world.getTime() % 2L == 0L) {
            spawnTrail(world, dance.finishStartPos.add(0.0, player.getHeight() * 0.5, 0.0), pos.add(0.0, player.getHeight() * 0.5, 0.0));
        }

        if (progress >= 1.0F) {
            spawnDepartureParticles(world, dance.finishEndPos.add(0.0, player.getHeight() * 0.5, 0.0));
            world.playSound(null, dance.finishEndPos.x, dance.finishEndPos.y, dance.finishEndPos.z,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS, 0.35F, 0.75F);
            applyArrivalEffects(world, player, dance);
            finishNow(player);
        }
    }

    private static void applyArrivalEffects(ServerWorld world, ServerPlayerEntity player, ActiveShadowDance dance) {
        if (dance.tuning.flag(1 << 19)) {
            world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(
                            dance.tuning.get(Phase8AbilityTuning.Setting.SHADOW_SMOKE_RADIUS, 2.5)),
                            target -> HelperMethods.checkAbilityTarget(target, player))
                    .stream().limit(dance.tuning.integer(
                            Phase8AbilityTuning.Setting.SHADOW_SMOKE_TARGET_CAP, 6))
                    .forEach(target -> target.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.BLINDNESS, dance.tuning.integer(
                            Phase8AbilityTuning.Setting.SHADOW_SMOKE_DURATION_TICKS, 20), 0), player));
        }
        if (dance.tuning.flag(1 << 23)) player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED,
                dance.tuning.integer(Phase8AbilityTuning.Setting.SHADOW_SPEED_DURATION_TICKS, 40),
                dance.tuning.integer(Phase8AbilityTuning.Setting.SHADOW_SPEED_AMPLIFIER, 1)), player);
    }

    private static void performStrike(ServerWorld world, ServerPlayerEntity player, ActiveShadowDance dance, LivingEntity target) {
        Vec3d previousPos = dance.lastVisualPos == null ? dance.anchorPos : dance.lastVisualPos;
        Vec3d strikePos = findStrikePosition(world, player, target, dance.tuning);
        Vec3d lookTarget = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);

        spawnDepartureParticles(world, previousPos.add(0.0, player.getHeight() * 0.5, 0.0));
        dance.lastStrikePos = strikePos;
        dance.lastVisualPos = strikePos;
        dance.lastLookTarget = lookTarget;
        lockPlayer(player);

        if (dance.lastTarget != null && !dance.lastTarget.equals(target.getUuid())) {
            dance.differentTargetChain++;
        }
        dance.lastTarget = target.getUuid();
        double multiplier = danceStrikeMultiplier(dance.tuning)
                * chainMultiplier(dance.differentTargetChain, dance.tuning);
        performWeaponStrike(player, target, dance.stack, multiplier);
        if (dance.tuning.flag(1 << 16)) performWeaponStrike(player, target, dance.stack, multiplier);
        if (dance.tuning.flag(1 << 22)) target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS,
                dance.tuning.integer(Phase8AbilityTuning.Setting.SHADOW_DISORIENT_DURATION_TICKS, 25),
                dance.tuning.integer(Phase8AbilityTuning.Setting.SHADOW_DISORIENT_AMPLIFIER, 1)), player);
        int absorption = dance.tuning.integer(Phase8AbilityTuning.Setting.SHADOW_REPRIEVE_ABSORPTION, 4);
        int absorptionCap = dance.tuning.integer(Phase8AbilityTuning.Setting.SHADOW_REPRIEVE_CAP, 12);
        if (dance.uniqueTargets.add(target.getUuid()) && dance.tuning.flag(1 << 24)
                && dance.uniqueTargets.size() % Math.max(1, dance.tuning.integer(
                        Phase8AbilityTuning.Setting.SHADOW_REPRIEVE_INTERVAL, 3)) == 0
                && dance.absorptionGranted + absorption <= absorptionCap && absorption > 0) {
            dance.absorptionGranted += absorption;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 80,
                    Math.max(0, dance.absorptionGranted / Math.max(1, absorption) - 1)), player);
        }

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundRegistry.DARK_SWORD_WHOOSH_01.get(),
                SoundCategory.PLAYERS, 0.55F, 1.45F + world.random.nextFloat() * 0.25F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 0.45F, 0.85F + world.random.nextFloat() * 0.2F);

        spawnArrivalParticles(world, target, strikePos);
        spawnTrail(world, previousPos.add(0.0, player.getHeight() * 0.5, 0.0), lookTarget);
        spawnShadowEcho(world, strikePos, lookTarget, player.getHeight());
        dance.nextStrikeTick = world.getTime() + getStrikeIntervalTicks(dance.tuning);
    }

    private static int getActiveDurationTicks(Phase8AbilityTuning tuning) {
        return danceDuration(Math.max(1, Config.uniqueEffects.shadowsting.duration / 2),
                Math.max(1, Config.uniqueEffects.shadowsting.strikeInterval / 2), tuning);
    }

    private static int getStrikeIntervalTicks(Phase8AbilityTuning tuning) {
        return strikeInterval(Math.max(1, Config.uniqueEffects.shadowsting.strikeInterval / 2), tuning);
    }

    private static void executePassiveCloneStrike(ServerWorld world, PendingShadowCloneStrike strike) {
        if (!(world.getEntity(strike.ownerId()) instanceof ServerPlayerEntity owner)
                || !(world.getEntity(strike.targetId()) instanceof LivingEntity target)
                || !owner.isAlive()
                || !target.isAlive()
                || !owner.getMainHandStack().isOf(ItemsRegistry.SHADOWSTING.get())) {
            return;
        }

        Vec3d strikePos = findStrikePosition(world, owner, target);
        Vec3d lookTarget = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
        spawnDepartureParticles(world, owner.getPos().add(0.0, owner.getHeight() * 0.5, 0.0));
        spawnArrivalParticles(world, target, strikePos);
        spawnTrail(world, owner.getPos().add(0.0, owner.getHeight() * 0.5, 0.0), lookTarget);
        spawnShadowEcho(world, strikePos, lookTarget, owner.getHeight());
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundRegistry.DARK_SWORD_WHOOSH_01.get(),
                SoundCategory.PLAYERS, 0.45F, 1.7F + world.random.nextFloat() * 0.25F);

        int previousDepth = CURRENT_CLONE_DEPTH.get();
        CURRENT_CLONE_DEPTH.set(strike.chainDepth());
        try {
            performWeaponStrike(owner, target, owner.getMainHandStack(), strike.damageMultiplier());
        } finally {
            CURRENT_CLONE_DEPTH.set(previousDepth);
        }
        if (strike.tuning().flag(1 << 3)) {
            Map<UUID, Long> marks = UMBRAL_MARKS.computeIfAbsent(owner.getUuid(), ignored -> new HashMap<>());
            marks.values().removeIf(expiry -> expiry <= world.getTime());
            marks.put(target.getUuid(), world.getTime() + strike.tuning().integer(
                    Phase8AbilityTuning.Setting.SHADOW_MARK_DURATION_TICKS, 60));
            if (marks.size() > 32) marks.entrySet().stream().min(Map.Entry.comparingByValue())
                    .ifPresent(entry -> marks.remove(entry.getKey()));
        }
        if (strike.tuning().flag(1 << 4)) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,
                    strike.tuning().integer(Phase8AbilityTuning.Setting.SHADOW_VEIL_DURATION_TICKS, 12),
                    0), owner);
            VEILED_OWNERS.add(owner.getUuid());
        }
        if (!target.isAlive() && strike.tuning().flag(1 << 6)) {
            Phase8CombatManager.scheduleCooldownRefund(world, owner, owner.getMainHandStack(),
                    strike.tuning().integer(Phase8AbilityTuning.Setting.SHADOW_KILL_REFUND_TICKS, 20));
        }
    }

    private static void performWeaponStrike(ServerPlayerEntity player, LivingEntity target, ItemStack stack,
                                            double multiplier) {
        if (player == null || target == null || !player.isAlive() || !target.isAlive()) {
            return;
        }

        DamageSource damageSource = player.getDamageSources().playerAttack(player);
        target.timeUntilRegen = 0;
        float damage = HelperMethods.abilityScaledDamage("soul", player, stack,
                Config.uniqueEffects.shadowsting.damageScaling * (float) multiplier,
                Config.uniqueEffects.shadowsting.spellScaling);
        if (target.damage(damageSource, damage) && !stack.isEmpty()) {
            stack.getItem().postHit(stack, target, player);
        }
    }

    private static void performWeaponStrike(LivingEntity actor, LivingEntity target, ItemStack stack,
                                            double multiplier) {
        if (actor == null || target == null || !actor.isAlive() || !target.isAlive() || !HelperMethods.checkAbilityTarget(target, actor)) {
            return;
        }
        float damage = HelperMethods.abilityScaledDamage("soul", actor, stack,
                Config.uniqueEffects.shadowsting.damageScaling * (float) multiplier,
                Config.uniqueEffects.shadowsting.spellScaling);
        damage = HelperMethods.applyNonPlayerAbilityDamageModifier(actor, damage);
        SimplySwordsAPI.applyEntityWeaponHit(stack, target, actor, damage);
    }

    public static boolean consumeVeil(ServerPlayerEntity owner, Phase8AbilityTuning tuning) {
        if (owner == null || !tuning.flag(1 << 4) || CURRENT_CLONE_DEPTH.get() > 0
                || !VEILED_OWNERS.remove(owner.getUuid())) {
            return false;
        }
        owner.removeStatusEffect(StatusEffects.INVISIBILITY);
        return true;
    }

    private static LivingEntity findRandomTarget(ServerWorld world, ServerPlayerEntity player,
                                                 Phase8AbilityTuning tuning) {
        double radius = danceRadius(Config.uniqueEffects.shadowsting.strikeRadius, tuning);
        Box box = new Box(player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box, entity ->
                entity != player
                        && entity.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkFriendlyFire(entity, player));

        if (targets.isEmpty()) {
            return null;
        }
        if (tuning.flag(1 << 13)) {
            double threshold = tuning.get(Phase8AbilityTuning.Setting.SHADOW_LOW_HEALTH_THRESHOLD, .4);
            List<LivingEntity> wounded = targets.stream()
                    .filter(target -> target.getHealth() / target.getMaxHealth() < threshold).toList();
            if (!wounded.isEmpty()) targets = new ArrayList<>(wounded);
        }
        return targets.get(world.random.nextInt(targets.size()));
    }

    private static Vec3d findStrikePosition(ServerWorld world, ServerPlayerEntity player, LivingEntity target) {
        return findStrikePosition(world, player, target, Phase8AbilityTuning.EMPTY);
    }

    private static Vec3d findStrikePosition(ServerWorld world, ServerPlayerEntity player, LivingEntity target,
                                            Phase8AbilityTuning tuning) {
        double distance = arrivalDistance(tuning);
        double baseAngle = world.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < 12; i++) {
            double angle = baseAngle + (Math.PI * 2.0 * i / 12.0);
            Vec3d offset = new Vec3d(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
            Vec3d candidate = target.getPos().add(offset);
            candidate = new Vec3d(candidate.x, target.getY(), candidate.z);
            if (isSafePosition(world, player, candidate)) {
                return candidate;
            }
        }

        Vec3d away = player.getPos().subtract(target.getPos());
        if (away.horizontalLengthSquared() < 0.001) {
            away = target.getRotationVec(1.0F).negate();
        }
        return target.getPos().add(away.normalize().multiply(distance));
    }

    private static Vec3d findStrikePosition(ServerWorld world, LivingEntity actor, LivingEntity target) {
        double baseAngle = world.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < 12; i++) {
            double angle = baseAngle + (Math.PI * 2.0 * i / 12.0);
            Vec3d offset = new Vec3d(Math.cos(angle) * 1.35, 0.0, Math.sin(angle) * 1.35);
            Vec3d candidate = target.getPos().add(offset);
            candidate = new Vec3d(candidate.x, target.getY(), candidate.z);
            if (isSafePosition(world, actor, candidate)) {
                return candidate;
            }
        }
        Vec3d away = actor.getPos().subtract(target.getPos());
        if (away.horizontalLengthSquared() < 0.001) {
            away = target.getRotationVec(1.0F).negate();
        }
        return target.getPos().add(away.normalize().multiply(1.35));
    }

    private static boolean isSafePosition(ServerWorld world, ServerPlayerEntity player, Vec3d pos) {
        Box playerBox = player.getBoundingBox().offset(pos.subtract(player.getPos()));
        if (!world.isSpaceEmpty(player, playerBox)) {
            return false;
        }
        return world.getBlockState(BlockPos.ofFloored(pos)).getFluidState().isEmpty();
    }

    private static boolean isSafePosition(ServerWorld world, LivingEntity actor, Vec3d pos) {
        Box actorBox = actor.getBoundingBox().offset(pos.subtract(actor.getPos()));
        if (!world.isSpaceEmpty(actor, actorBox)) {
            return false;
        }
        return world.getBlockState(BlockPos.ofFloored(pos)).getFluidState().isEmpty();
    }

    private static float[] getFacingRotation(Vec3d fromEye, Vec3d to) {
        Vec3d diff = to.subtract(fromEye);
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        return new float[]{yaw, pitch};
    }

    private static void lockPlayer(ServerPlayerEntity player) {
        player.clearActiveItem();
        player.setVelocity(0.0, 0.0, 0.0);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
        player.extinguish();
    }

    private static void lockPlayer(ServerPlayerEntity player, ActiveShadowDance dance) {
        lockPlayer(player);
        if (dance.anchorPos != null) {
            player.networkHandler.requestTeleport(dance.anchorPos.x, dance.anchorPos.y, dance.anchorPos.z, player.getYaw(), player.getPitch());
        }
    }

    private static void clearMobTargets(ServerWorld world, ServerPlayerEntity player) {
        double radius = Config.uniqueEffects.shadowsting.strikeRadius + 8.0;
        Box box = new Box(player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius);
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    private static void spawnDepartureParticles(ServerWorld world, Vec3d pos) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.15, pos.z, 8, 0.22, 0.28, 0.22, 0.06);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 0.35, pos.z, 2, 0.14, 0.18, 0.14, 0.02);
        } else {
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.1, pos.z, 2, 0.12, 0.16, 0.12, 0.006);
        }
    }

    private static void spawnArrivalParticles(ServerWorld world, LivingEntity target, Vec3d strikePos) {
        Vec3d targetPos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.5), 0.0);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, targetPos.x, targetPos.y + 0.15, targetPos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, targetPos.x, targetPos.y, targetPos.z, 8, 0.22, 0.2, 0.22, 0.018);
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, targetPos.x, targetPos.y, targetPos.z, 5, 0.18, 0.22, 0.18, 0.045);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, targetPos.x, targetPos.y + 0.15, targetPos.z, 2, 0.12, 0.16, 0.12, 0.018);
        } else {
            world.spawnParticles(ParticleTypes.SMOKE, strikePos.x, strikePos.y + 0.2, strikePos.z, 2, 0.12, 0.12, 0.12, 0.006);
        }
    }

    private static void spawnTrail(ServerWorld world, Vec3d from, Vec3d to) {
        if (!Config.general.enableModernFieldEffects || from == null || to == null) {
            return;
        }

        Vec3d delta = to.subtract(from);
        int steps = MathHelper.clamp((int) (delta.length() * 1.35), 3, 10);
        for (int i = 1; i < steps; i++) {
            double progress = (double) i / (double) steps;
            Vec3d pos = from.add(delta.multiply(progress));
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.12, pos.z, 1, 0.025, 0.025, 0.025, 0.018);
        }
    }

    private static void spawnShadowEcho(ServerWorld world, Vec3d strikePos, Vec3d lookTarget, float playerHeight) {
        if (!Config.general.enableModernFieldEffects || strikePos == null || lookTarget == null) {
            return;
        }

        Vec3d facing = lookTarget.subtract(strikePos);
        if (facing.horizontalLengthSquared() < 0.001) {
            facing = new Vec3d(0.0, 0.0, 1.0);
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(facing.z, facing.x)) - 90.0F);
        ShadowstingAfterimageVisualEntity afterimage = new ShadowstingAfterimageVisualEntity(world, strikePos.x, strikePos.y, strikePos.z, yaw, AFTERIMAGE_LIFETIME);
        world.spawnEntity(afterimage);

        Vec3d side = new Vec3d(-facing.z, 0.0, facing.x).normalize();
        double height = Math.max(1.45, playerHeight);
        for (int i = 0; i < 12; i++) {
            double y = strikePos.y + 0.15 + height * i / 12.0;
            double width = 0.12 + 0.18 * Math.sin((double) i / 12.0 * Math.PI);
            Vec3d left = strikePos.add(side.multiply(width)).add(0.0, y - strikePos.y, 0.0);
            Vec3d right = strikePos.subtract(side.multiply(width)).add(0.0, y - strikePos.y, 0.0);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, left.x, left.y, left.z, 1, 0.012, 0.012, 0.012, 0.014);
            if (i % 2 == 0) {
                world.spawnParticles(ParticleTypes.SCULK_SOUL, right.x, right.y, right.z, 1, 0.01, 0.01, 0.01, 0.006);
            }
        }
    }

    private static void spawnIdleParticles(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = player.getPos().add(0.0, 0.18, 0.0);
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 1, 0.18, 0.05, 0.18, 0.018);
            if (world.getTime() % 4 == 0) {
                world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 0.18, pos.z, 1, 0.08, 0.04, 0.08, 0.006);
            }
        } else {
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.08, 0.04, 0.08, 0.004);
        }
    }

    private static void spawnFailParticles(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = player.getPos().add(0.0, 0.8, 0.0);
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 3, 0.16, 0.14, 0.16, 0.02);
        } else {
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 2, 0.12, 0.1, 0.12, 0.006);
        }
    }

    private static final class ActiveShadowDance {
        private final String worldKey;
        private final long endTick;
        private final Vec3d anchorPos;
        private final ItemStack stack;
        private final Phase8AbilityTuning tuning;
        private long nextStrikeTick;
        private Vec3d lastStrikePos;
        private Vec3d lastVisualPos;
        private Vec3d lastLookTarget;
        private boolean finishing;
        private long finishStartTick;
        private Vec3d finishStartPos;
        private Vec3d finishEndPos;
        private float finishStartYaw;
        private float finishStartPitch;
        private float finishEndYaw;
        private float finishEndPitch;
        private UUID focusTarget;
        private UUID lastTarget;
        private int differentTargetChain;
        private int absorptionGranted;
        private final Set<UUID> uniqueTargets = new HashSet<>();

        private ActiveShadowDance(String worldKey, long endTick, long nextStrikeTick, Vec3d anchorPos,
                                  ItemStack stack, Phase8AbilityTuning tuning) {
            this.worldKey = worldKey;
            this.endTick = endTick;
            this.nextStrikeTick = nextStrikeTick;
            this.anchorPos = anchorPos;
            this.stack = stack;
            this.tuning = tuning;
            this.lastVisualPos = anchorPos;
        }
    }

    private record PendingShadowCloneStrike(UUID ownerId, UUID targetId, long triggerTick, int chainDepth,
                                            Phase8AbilityTuning tuning, double damageMultiplier) {
    }
}
