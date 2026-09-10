package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryAbilities;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DreadwhisperTrailManager {
    private static final double FOLLOW_STEP_SQUARED = 0.25;
    private static final double VERTICAL_REACH = 6.0;
    private static final Map<ServerWorld, Map<UUID, TrailState>> TRAILS = new HashMap<>();

    private DreadwhisperTrailManager() {
    }

    // Living Shadow: the trail follows its owner and keeps biting after the dash ends.
    public static boolean start(ServerWorld world, LivingEntity owner, ItemStack stack,
                                StormSoulMasteryTuning tuning, UUID castId, Vec3d castStart,
                                Vec3d castEnd, float baseDamage, UniqueAbilityExecution execution,
                                int reportedHits) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(execution == null ? null : execution.provenance())) {
        int ticks = tuning.integer(StormSoulMasteryTuning.Setting.LIVING_SHADOW_TICKS, 0);
        double multiplier = tuning.get(StormSoulMasteryTuning.Setting.LIVING_SHADOW_MULTIPLIER, 0);
        if (ticks <= 0 || multiplier <= 0) return false;
        TrailState state = state(world, owner.getUuid());
        state.interval = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.LIVING_SHADOW_INTERVAL, 20));
        state.pulsesRemaining = Math.max(1, ticks / state.interval);
        state.nextPulse = world.getTime() + state.interval;
        state.damage = (float) (baseDamage * multiplier);
        state.targetCap = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.LIVING_SHADOW_TARGET_CAP, 12));
        state.stack = stack.copy();
        state.castId = castId;
        state.stainRadius = DreadwhisperAbilityManager.rendWidth(tuning) * 0.5;
        state.stainDuration = DreadwhisperAbilityManager.stainDuration(tuning);
        state.slowAmplifier = DreadwhisperAbilityManager.trailSlowAmplifier(tuning);
        state.behavior = DreadwhisperAbilityManager.trailBehavior(castId, tuning);
        state.lastPosition = castEnd;
        state.castBounds = new Box(castStart, castEnd)
                .expand(state.stainRadius, VERTICAL_REACH, state.stainRadius);
        state.execution = execution;
        state.reportedHits = Math.max(0, reportedHits);
        return true;
        }
    }

    // Veiled Passage and Fading Footprint both persist briefly past the dash.
    public static void startVeil(ServerWorld world, LivingEntity owner, StormSoulMasteryTuning tuning) {
        int veil = tuning.integer(StormSoulMasteryTuning.Setting.VEIL_DURATION_TICKS, 0);
        int footprint = tuning.integer(StormSoulMasteryTuning.Setting.FOOTPRINT_TICKS, 0);
        if (veil <= 0 && footprint <= 0) return;
        TrailState state = state(world, owner.getUuid());
        long now = world.getTime();
        if (veil > 0) state.veilUntil = now + veil;
        if (footprint > 0) {
            state.footprintUntil = now + footprint;
            state.footprintReduction = tuning.get(
                    StormSoulMasteryTuning.Setting.FOOTPRINT_PROJECTILE_REDUCTION, 0);
        }
    }

    public static int grantShelter(ServerWorld world, LivingEntity owner, int amount, int ticks) {
        if (world == null || owner == null || amount <= 0) return 0;
        float cap = Math.max(0.0F, Config.uniqueEffects.abilityAbsorptionCap);
        float current = owner.getAbsorptionAmount();
        float granted = Math.min(amount, Math.max(0.0F, cap - current));
        if (granted <= 0.0F) return 0;
        owner.setAbsorptionAmount(current + granted);
        TrailState state = state(world, owner.getUuid());
        state.shelter.add(new ShelterGrant(world.getTime() + Math.max(1, ticks), granted));
        state.shelterTracked += granted;
        return (int) granted;
    }

    public static boolean isVeiled(LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) return false;
        TrailState state = peek(world, owner.getUuid());
        return state != null && world.getTime() < state.veilUntil;
    }

    public static float modifyIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (actor == null || source == null || amount <= 0.0F
                || !(actor.getWorld() instanceof ServerWorld world)) {
            return amount;
        }
        TrailState state = peek(world, actor.getUuid());
        if (state == null || state.footprintReduction <= 0 || world.getTime() >= state.footprintUntil
                || !source.isIn(DamageTypeTags.IS_PROJECTILE)
                || !actor.isOnGround()
                || !GloamStainManager.isOnOwnerGloam(world, actor.getUuid(), actor)) {
            return amount;
        }
        return (float) Math.max(0.0, amount * (1.0 - state.footprintReduction));
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, TrailState> states = TRAILS.get(world);
        return states != null && !states.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, TrailState> states = TRAILS.get(world);
        if (states == null || states.isEmpty()) return;
        long now = world.getTime();
        Iterator<Map.Entry<UUID, TrailState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrailState> entry = iterator.next();
            TrailState state = entry.getValue();
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity owner) || !owner.isAlive()) {
                release(state, true);
                iterator.remove();
                continue;
            }
            tickShelter(owner, state, now);
            tickFootprint(world, owner, state, now);
            if (state.pulsesRemaining > 0) {
                extendCastTrail(world, owner, state);
                if (now >= state.nextPulse) {
                    state.nextPulse = now + state.interval;
                    state.pulsesRemaining--;
                    pulse(world, owner, state);
                    if (state.pulsesRemaining <= 0) release(state, false);
                }
            }
            if (state.pulsesRemaining <= 0 && now >= state.veilUntil && now >= state.footprintUntil
                    && state.shelter.isEmpty() && state.speedExpiry == Long.MIN_VALUE) {
                release(state, false);
                iterator.remove();
            }
        }
        if (states.isEmpty()) TRAILS.remove(world);
    }

    private static void extendCastTrail(ServerWorld world, LivingEntity owner, TrailState state) {
        Vec3d position = owner.getPos();
        if (position.squaredDistanceTo(state.lastPosition) <= FOLLOW_STEP_SQUARED) return;
        GloamStainManager.createPatch(world, owner.getUuid(),
                new Vec3d(position.x, LivyatanWaveManager.findGroundTopY(
                        world, position.x, position.z, position.y + 1.5), position.z),
                state.stainRadius, state.stainDuration,
                Math.max(1, Config.uniqueEffects.dreadwhisper.stainFadeDuration),
                state.slowAmplifier, state.behavior);
        state.lastPosition = position;
        state.castBounds = state.castBounds.union(new Box(position, position)
                .expand(state.stainRadius, VERTICAL_REACH, state.stainRadius));
    }

    private static void tickFootprint(ServerWorld world, LivingEntity owner, TrailState state, long now) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(state == null ? null : CombatProvenanceApi.from(state.stack, null))) {
        boolean sheltered = state.footprintReduction > 0 && now < state.footprintUntil
                && owner.isOnGround()
                && GloamStainManager.isOnOwnerGloam(world, owner.getUuid(), owner);
        if (sheltered) {
            int duration = (int) Math.max(1L, Math.min(20L, state.footprintUntil - now));
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 0,
                    false, false, true), owner);
            state.speedExpiry = now + duration;
            return;
        }
        if (state.speedExpiry == Long.MIN_VALUE) return;
        StatusEffectInstance speed = owner.getStatusEffect(StatusEffects.SPEED);
        if (speed != null && speed.getAmplifier() == 0
                && speed.getDuration() <= Math.max(0L, state.speedExpiry - now)) {
            owner.removeStatusEffect(StatusEffects.SPEED);
        }
        state.speedExpiry = Long.MIN_VALUE;
        }
    }

    private static void tickShelter(LivingEntity owner, TrailState state, long now) {
        if (state.shelter.isEmpty()) return;
        float absorption = owner.getAbsorptionAmount();
        if (absorption < state.shelterTracked) {
            float consumed = state.shelterTracked - absorption;
            state.shelterTracked = absorption;
            Iterator<ShelterGrant> iterator = state.shelter.iterator();
            while (iterator.hasNext() && consumed > 0.0F) {
                ShelterGrant grant = iterator.next();
                float taken = Math.min(grant.amount, consumed);
                grant.amount -= taken;
                consumed -= taken;
                if (grant.amount <= 0.0F) iterator.remove();
            }
        }
        Iterator<ShelterGrant> iterator = state.shelter.iterator();
        while (iterator.hasNext()) {
            ShelterGrant grant = iterator.next();
            if (now < grant.expiry) continue;
            float removed = Math.min(grant.amount, owner.getAbsorptionAmount());
            if (removed > 0.0F) {
                owner.setAbsorptionAmount(owner.getAbsorptionAmount() - removed);
                state.shelterTracked = Math.max(0.0F, state.shelterTracked - removed);
            }
            iterator.remove();
        }
    }

    private static void pulse(ServerWorld world, LivingEntity owner, TrailState state) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(state == null ? null : CombatProvenanceApi.from(state.stack, null))) {
        if (state.damage <= 0 || state.castId == null) return;
        int affected = 0;
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, state.castBounds,
                candidate -> candidate != owner && candidate.isAlive() && !candidate.isRemoved()
                        && candidate.isOnGround()
                        && HelperMethods.checkAbilityTarget(candidate, owner)
                        && GloamStainManager.isOnSourceGloam(world, state.castId, candidate));
        for (LivingEntity target : targets) {
            if (affected >= state.targetCap) break;
            if (!SimplySwordsAPI.applyEntityWeaponHit(state.stack, target, owner, state.damage)) continue;
            affected++;
            if (state.execution != null) {
                UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT,
                        StormSoulMasteryAbilities.HIT, target, 1, state.damage);
            }
        }
        state.reportedHits += affected;
        }
    }

    private static void release(TrailState state, boolean cancelled) {
        if (state.execution == null) return;
        UniqueAbilityExecution execution = state.execution;
        state.execution = null;
        state.pulsesRemaining = 0;
        if (cancelled) {
            UniqueAbilityApi.cancel(execution);
        } else {
            UniqueAbilityApi.finish(execution, StormSoulMasteryAbilities.FINISH, state.reportedHits);
        }
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        Map<UUID, TrailState> states = TRAILS.get(world);
        TrailState state = states == null ? null : states.remove(actor.getUuid());
        if (state != null) release(state, true);
        if (states != null && states.isEmpty()) TRAILS.remove(world);
    }

    public static void clear(ServerWorld world) {
        Map<UUID, TrailState> states = TRAILS.remove(world);
        if (states != null) states.values().forEach(state -> release(state, true));
    }

    public static void clearAll() {
        TRAILS.values().forEach(states -> states.values().forEach(state -> release(state, true)));
        TRAILS.clear();
    }

    private static TrailState peek(ServerWorld world, UUID ownerId) {
        Map<UUID, TrailState> states = TRAILS.get(world);
        return states == null ? null : states.get(ownerId);
    }

    private static TrailState state(ServerWorld world, UUID ownerId) {
        return TRAILS.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(ownerId, ignored -> new TrailState());
    }

    private static final class ShelterGrant {
        private final long expiry;
        private float amount;

        private ShelterGrant(long expiry, float amount) {
            this.expiry = expiry;
            this.amount = amount;
        }
    }

    private static final class TrailState {
        private long nextPulse = Long.MIN_VALUE;
        private int interval = 20;
        private int pulsesRemaining;
        private float damage;
        private int targetCap = 12;
        private double stainRadius = 4.5;
        private int stainDuration = 240;
        private int slowAmplifier;
        private GloamStainManager.PatchBehavior behavior = GloamStainManager.PatchBehavior.NONE;
        private UUID castId;
        private ItemStack stack = ItemStack.EMPTY;
        private Vec3d lastPosition = Vec3d.ZERO;
        private Box castBounds = new Box(Vec3d.ZERO, Vec3d.ZERO);
        private long veilUntil = Long.MIN_VALUE;
        private long footprintUntil = Long.MIN_VALUE;
        private long speedExpiry = Long.MIN_VALUE;
        private double footprintReduction;
        private final List<ShelterGrant> shelter = new ArrayList<>();
        private float shelterTracked;
        private UniqueAbilityExecution execution;
        private int reportedHits;
    }
}
