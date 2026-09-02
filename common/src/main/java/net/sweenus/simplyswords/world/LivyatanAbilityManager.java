package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LivyatanAbilityManager {
    private static final int THROW_SOURCE = 1;
    private static final int RETURN_SOURCE = 2;
    private static final int WAVE_SOURCE = 4;
    private static final Map<ServerWorld, WorldState> STATES = new HashMap<>();

    private LivyatanAbilityManager() {
    }

    public static WavePlan prepareWave(ServerWorld world, LivingEntity actor, Phase6AbilityTuning tuning) {
        WorldState worldState = STATES.computeIfAbsent(world, ignored -> new WorldState());
        ActorState state = worldState.actors.computeIfAbsent(actor.getUuid(), ignored -> new ActorState());
        int required = tuning.integer(s("LIVYATAN_DOUBLE_SWING_COUNT"), 0);
        boolean secondary = false;
        if (required > 0) {
            state.swingCount++;
            if (state.swingCount >= required) {
                state.swingCount = 0;
                secondary = true;
            }
        }
        boolean unbound = tuning.has(s("LIVYATAN_UNBOUND_WAVE_DAMAGE_MULTIPLIER"));
        boolean lightning = unbound && state.nextLightning;
        if (unbound) state.nextLightning = !state.nextLightning;
        return new WavePlan(secondary, lightning);
    }

    public static double waveDamageMultiplier(ServerWorld world, LivingEntity actor, LivingEntity target,
                                              Phase6AbilityTuning tuning) {
        TargetState state = targetState(world, actor.getUuid(), target.getUuid(), false);
        if (state == null || state.markDeadline < world.getTime()) return 1;
        return tuning.get(s("LIVYATAN_MARK_WAVE_DAMAGE_MULTIPLIER"), 1);
    }

    public static void recordThrowHit(ServerWorld world, LivingEntity actor, ItemStack stack,
                                      LivingEntity target, Phase6AbilityTuning tuning) {
        recordSource(world, actor, stack, target, tuning, THROW_SOURCE);
    }

    public static void recordReturnHit(ServerWorld world, LivingEntity actor, ItemStack stack,
                                       LivingEntity target, Phase6AbilityTuning tuning) {
        recordSource(world, actor, stack, target, tuning, RETURN_SOURCE);
    }

    public static void recordWaveHit(ServerWorld world, LivingEntity actor, ItemStack stack,
                                     LivingEntity target, Phase6AbilityTuning tuning) {
        int required = tuning.integer(s("LIVYATAN_SURGE_HIT_COUNT"), 0);
        int window = tuning.integer(s("LIVYATAN_SURGE_WINDOW_TICKS"), 0);
        if (required > 0 && window > 0) {
            TargetState state = targetState(world, actor.getUuid(), target.getUuid(), true);
            long now = world.getTime();
            if (state.surgeDeadline < now) state.surgeHits = 0;
            state.surgeDeadline = now + window;
            state.surgeHits++;
            if (state.surgeHits >= required) {
                state.surgeHits = 0;
                float damage = returnLightningDamage(actor, stack, tuning)
                        * (float) tuning.get(s("LIVYATAN_SURGE_DAMAGE_MULTIPLIER"), 0);
                strikeLightning(world, actor, stack, target, damage, tuning);
            }
        }
        recordSource(world, actor, stack, target, tuning, WAVE_SOURCE);
    }

    public static void recordPull(ServerWorld world, LivingEntity actor, LivingEntity target,
                                  Phase6AbilityTuning tuning) {
        TargetState state = targetState(world, actor.getUuid(), target.getUuid(), true);
        long now = world.getTime();
        state.pullDeadline = now + 200;
        int markTicks = tuning.integer(s("LIVYATAN_MARK_DURATION_TICKS"), 0);
        if (markTicks > 0) state.markDeadline = now + markTicks;
        double rootDistance = tuning.get(s("LIVYATAN_RETURN_ROOT_DISTANCE"), 0);
        int rootTicks = tuning.integer(s("LIVYATAN_RETURN_ROOT_TICKS"), 0);
        if (rootDistance <= 0 || rootTicks <= 0 || state.rooted) return;
        if (state.pullStart == null) state.pullStart = target.getPos();
        if (horizontalDistanceSquared(state.pullStart, target.getPos()) >= rootDistance * rootDistance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, rootTicks, 255), actor);
            state.rooted = true;
        }
    }

    public static float returnLightningDamage(LivingEntity actor, ItemStack stack, Phase6AbilityTuning tuning) {
        float base = HelperMethods.abilityScaledDamage("lightning", actor, stack,
                Config.uniqueEffects.livyatan.returnLightningDamageScaling,
                Config.uniqueEffects.livyatan.returnLightningSpellScaling);
        return base * (float) returnLightningMultiplier(tuning);
    }

    public static double returnRadius(double configured, Phase6AbilityTuning tuning) {
        return configured + tuning.get(s("LIVYATAN_RETURN_RADIUS_BONUS"), 0);
    }

    public static double returnPull(double configured, Phase6AbilityTuning tuning) {
        if (tuning.has(s("LIVYATAN_THUNDERHEAD_LIGHTNING_MULTIPLIER"))) return 0;
        return configured * tuning.get(s("LIVYATAN_RETURN_PULL_MULTIPLIER"), 1)
                * tuning.get(s("LIVYATAN_MAELSTROM_PULL_MULTIPLIER"), 1);
    }

    public static int returnLightningChance(int configured, Phase6AbilityTuning tuning) {
        if (tuning.has(s("LIVYATAN_MAELSTROM_ROTATIONS"))) return 0;
        if (tuning.has(s("LIVYATAN_THUNDERHEAD_LIGHTNING_MULTIPLIER"))) return 100;
        return Math.clamp(configured + tuning.integer(s("LIVYATAN_RETURN_LIGHTNING_CHANCE_BONUS"), 0), 0, 100);
    }

    public static double returnLightningMultiplier(Phase6AbilityTuning tuning) {
        return tuning.get(s("LIVYATAN_RETURN_LIGHTNING_DAMAGE_MULTIPLIER"), 1)
                * tuning.get(s("LIVYATAN_THUNDERHEAD_LIGHTNING_MULTIPLIER"), 1);
    }

    public static int activeCooldown(int configured, Phase6AbilityTuning tuning) {
        return Math.max(0, configured + tuning.integer(s("LIVYATAN_ACTIVE_COOLDOWN_BONUS_TICKS"), 0));
    }

    public static void strikeLightning(ServerWorld world, LivingEntity actor, ItemStack stack,
                                       LivingEntity target, float damage, Phase6AbilityTuning tuning) {
        if (damage <= 0 || !target.isAlive()) return;
        ChainLightningVisualManager.damageSkyBolt(world, actor, stack, target, damage,
                Config.uniqueEffects.livyatan.returnLightningSkyHeight,
                ChainLightningVisualManager.STORMBRINGER_SETTINGS);
        double range = tuning.get(s("LIVYATAN_CONDUCTION_RANGE"), 0);
        int count = tuning.integer(s("LIVYATAN_CONDUCTION_TARGET_COUNT"), 0);
        float chainedDamage = damage * (float) tuning.get(s("LIVYATAN_CONDUCTION_DAMAGE_MULTIPLIER"), 0);
        if (range <= 0 || count <= 0 || chainedDamage <= 0) return;
        targets(world, target.getPos(), range, actor, count, target.getUuid()).forEach(chained ->
                ChainLightningVisualManager.damageSkyBolt(world, actor, stack, chained, chainedDamage,
                        Config.uniqueEffects.livyatan.returnLightningSkyHeight,
                        ChainLightningVisualManager.STORMBRINGER_SETTINGS));
    }

    public static List<LivingEntity> targets(ServerWorld world, Vec3d center, double radius,
                                              LivingEntity actor, int cap, UUID excluded) {
        if (radius <= 0 || cap <= 0) return List.of();
        Box box = Box.of(center, radius * 2, Math.max(3, radius), radius * 2);
        return world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive).stream()
                .filter(target -> !target.getUuid().equals(excluded))
                .filter(target -> HelperMethods.checkAbilityTarget(target, actor))
                .filter(target -> target.squaredDistanceTo(center) <= radius * radius)
                .sorted(Comparator.comparingDouble(target -> target.squaredDistanceTo(center)))
                .limit(cap).toList();
    }

    public static Vec3d steer(ServerWorld world, LivingEntity actor, Vec3d center, Vec3d forward,
                              Phase6AbilityTuning tuning) {
        double range = tuning.get(s("LIVYATAN_STEERING_RANGE"), 0);
        double degrees = tuning.get(s("LIVYATAN_STEERING_DEGREES"), 0);
        if (range <= 0 || degrees <= 0) return forward;
        LivingEntity target = targets(world, center, range, actor, 1, actor.getUuid()).stream()
                .findFirst().orElse(null);
        if (target == null) return forward;
        Vec3d desired = new Vec3d(target.getX() - center.x, 0, target.getZ() - center.z);
        if (desired.lengthSquared() <= 1.0E-6) return forward;
        desired = desired.normalize();
        double currentAngle = Math.atan2(forward.z, forward.x);
        double targetAngle = Math.atan2(desired.z, desired.x);
        double delta = Math.atan2(Math.sin(targetAngle - currentAngle), Math.cos(targetAngle - currentAngle));
        double limit = Math.toRadians(degrees);
        double angle = currentAngle + Math.clamp(delta, -limit, limit);
        return new Vec3d(Math.cos(angle), 0, Math.sin(angle));
    }

    public static void finishReturn(ServerWorld world, UUID actorId) {
        WorldState state = STATES.get(world);
        if (state == null) return;
        state.targets.forEach((key, value) -> {
            if (key.actorId.equals(actorId)) {
                value.pullStart = null;
                value.rooted = false;
                value.pullDeadline = 0;
            }
        });
    }

    public static void tick(ServerWorld world) {
        WorldState state = STATES.get(world);
        if (state == null) return;
        long now = world.getTime();
        state.targets.entrySet().removeIf(entry -> entry.getValue().expiry() < now);
        if (state.targets.isEmpty() && state.actors.isEmpty()) STATES.remove(world);
    }

    public static boolean hasExpiringState(ServerWorld world) {
        WorldState state = STATES.get(world);
        return state != null && !state.targets.isEmpty();
    }

    public static void clear(ServerWorld world) {
        STATES.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (!(actor.getWorld() instanceof ServerWorld world)) return;
        WorldState state = STATES.get(world);
        if (state == null) return;
        UUID actorId = actor.getUuid();
        state.actors.remove(actorId);
        state.targets.keySet().removeIf(key -> key.actorId.equals(actorId));
        if (state.targets.isEmpty() && state.actors.isEmpty()) STATES.remove(world);
    }

    public static void clearAll() {
        STATES.clear();
    }

    private static void recordSource(ServerWorld world, LivingEntity actor, ItemStack stack,
                                     LivingEntity target, Phase6AbilityTuning tuning, int source) {
        int window = tuning.integer(s("LIVYATAN_PERFECT_STORM_WINDOW_TICKS"), 0);
        if (window <= 0) return;
        TargetState state = targetState(world, actor.getUuid(), target.getUuid(), true);
        long now = world.getTime();
        if (state.perfectDeadline < now) state.perfectSources = 0;
        state.perfectDeadline = now + window;
        state.perfectSources |= source;
        if (state.perfectSources != (THROW_SOURCE | RETURN_SOURCE | WAVE_SOURCE)) return;
        state.perfectSources = 0;
        burst(world, actor, stack, target, tuning);
    }

    private static void burst(ServerWorld world, LivingEntity actor, ItemStack stack,
                              LivingEntity origin, Phase6AbilityTuning tuning) {
        double radius = tuning.get(s("LIVYATAN_PERFECT_STORM_RADIUS"), 0);
        int cap = tuning.integer(s("LIVYATAN_PERFECT_STORM_TARGET_CAP"), 0);
        float base = HelperMethods.abilityScaledDamage("frost", actor, stack,
                Config.uniqueEffects.livyatan.damageScaling, Config.uniqueEffects.livyatan.spellScaling);
        float raw = base * (float) tuning.get(s("LIVYATAN_PERFECT_STORM_DAMAGE_MULTIPLIER"), 0);
        if (radius <= 0 || cap <= 0 || raw <= 0) return;
        DamageSource source = world.getDamageSources().indirectMagic(actor, actor);
        List<LivingEntity> victims = targets(world, origin.getPos(), radius, actor, cap, actor.getUuid());
        for (LivingEntity victim : victims) {
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, victim, source, raw);
            WeaponImplicitRegistry.runSuppressed(() -> HelperMethods.damageThroughIframes(victim, source, damage));
        }
    }

    private static TargetState targetState(ServerWorld world, UUID actor, UUID target, boolean create) {
        WorldState state = create ? STATES.computeIfAbsent(world, ignored -> new WorldState()) : STATES.get(world);
        if (state == null) return null;
        ActorTarget key = new ActorTarget(actor, target);
        return create ? state.targets.computeIfAbsent(key, ignored -> new TargetState()) : state.targets.get(key);
    }

    private static double horizontalDistanceSquared(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    public record WavePlan(boolean secondary, boolean lightning) {
    }

    private static final class WorldState {
        private final Map<UUID, ActorState> actors = new HashMap<>();
        private final Map<ActorTarget, TargetState> targets = new HashMap<>();
    }

    private static final class ActorState {
        private int swingCount;
        private boolean nextLightning;
    }

    private static final class TargetState {
        private int surgeHits;
        private long surgeDeadline;
        private long markDeadline;
        private int perfectSources;
        private long perfectDeadline;
        private long pullDeadline;
        private Vec3d pullStart;
        private boolean rooted;

        private long expiry() {
            return Math.max(pullDeadline, Math.max(surgeDeadline, Math.max(markDeadline, perfectDeadline)));
        }
    }

    private record ActorTarget(UUID actorId, UUID targetId) {
    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }
}
