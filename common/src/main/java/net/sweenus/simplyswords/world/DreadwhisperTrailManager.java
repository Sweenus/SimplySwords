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
import net.sweenus.simplyswords.api.ability.Phase3AbilityTuning;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class DreadwhisperTrailManager {
    private static final Map<ServerWorld, Map<UUID, TrailState>> TRAILS = new HashMap<>();

    private DreadwhisperTrailManager() {
    }

    // Living Shadow: the trail follows its owner and keeps biting after the dash ends.
    public static void start(ServerWorld world, LivingEntity owner, ItemStack stack,
                             Phase3AbilityTuning tuning, Vec3d origin, float baseDamage) {
        int ticks = tuning.integer(Phase3AbilityTuning.Setting.LIVING_SHADOW_TICKS, 0);
        double multiplier = tuning.get(Phase3AbilityTuning.Setting.LIVING_SHADOW_MULTIPLIER, 0);
        if (ticks <= 0 || multiplier <= 0) return;
        TrailState state = state(world, owner.getUuid());
        state.expiresAt = world.getTime() + ticks;
        state.interval = Math.max(1, tuning.integer(Phase3AbilityTuning.Setting.LIVING_SHADOW_INTERVAL, 20));
        state.nextPulse = world.getTime() + state.interval;
        state.damage = (float) (baseDamage * multiplier);
        state.targetCap = Math.max(1, tuning.integer(Phase3AbilityTuning.Setting.LIVING_SHADOW_TARGET_CAP, 12));
        state.stack = stack.copy();
        state.stainRadius = Math.max(1.0, tuning.get(Phase3AbilityTuning.Setting.REND_WIDTH, 4.5));
        state.lastPosition = origin;
    }

    // Veiled Passage and Fading Footprint both persist briefly past the dash.
    public static void startVeil(ServerWorld world, LivingEntity owner, Phase3AbilityTuning tuning) {
        int veil = tuning.integer(Phase3AbilityTuning.Setting.VEIL_DURATION_TICKS, 0);
        int footprint = tuning.integer(Phase3AbilityTuning.Setting.FOOTPRINT_TICKS, 0);
        if (veil <= 0 && footprint <= 0) return;
        TrailState state = state(world, owner.getUuid());
        long now = world.getTime();
        if (veil > 0) state.veilUntil = now + veil;
        if (footprint > 0) {
            state.footprintUntil = now + footprint;
            state.footprintReduction = tuning.get(
                    Phase3AbilityTuning.Setting.FOOTPRINT_PROJECTILE_REDUCTION, 0);
        }
    }

    public static boolean isVeiled(LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) return false;
        TrailState state = peek(world, owner.getUuid());
        return state != null && world.getTime() <= state.veilUntil;
    }

    public static float modifyIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (actor == null || source == null || amount <= 0.0F
                || !(actor.getWorld() instanceof ServerWorld world)) {
            return amount;
        }
        TrailState state = peek(world, actor.getUuid());
        if (state == null || state.footprintReduction <= 0 || world.getTime() > state.footprintUntil
                || !source.isIn(DamageTypeTags.IS_PROJECTILE)
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
                iterator.remove();
                continue;
            }
            if (now <= state.footprintUntil && state.footprintReduction > 0
                    && GloamStainManager.isOnOwnerGloam(world, owner.getUuid(), owner)) {
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 0,
                        false, false, true), owner);
            }
            if (state.damage > 0 && now <= state.expiresAt) {
                Vec3d position = owner.getPos();
                if (position.squaredDistanceTo(state.lastPosition) > 0.25) {
                    GloamStainManager.createPatch(world, owner.getUuid(),
                            new Vec3d(position.x, LivyatanWaveManager.findGroundTopY(
                                    world, position.x, position.z, position.y + 1.5), position.z),
                            state.stainRadius * 0.5, 40, 20, 0);
                    state.lastPosition = position;
                }
                if (now >= state.nextPulse) {
                    state.nextPulse = now + state.interval;
                    pulse(world, owner, state);
                }
            }
            if (now > state.expiresAt && now > state.veilUntil && now > state.footprintUntil) {
                iterator.remove();
            }
        }
        if (states.isEmpty()) TRAILS.remove(world);
    }

    private static void pulse(ServerWorld world, LivingEntity owner, TrailState state) {
        int affected = 0;
        Box search = owner.getBoundingBox().expand(state.stainRadius * 2.0);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, search,
                candidate -> candidate != owner && candidate.isAlive() && !candidate.isRemoved()
                        && HelperMethods.checkAbilityTarget(candidate, owner)
                        && GloamStainManager.isOnOwnerGloam(world, owner.getUuid(), candidate))) {
            SimplySwordsAPI.applyEntityWeaponHit(state.stack, target, owner, state.damage);
            if (++affected >= state.targetCap) break;
        }
    }

    public static void clear(ServerWorld world) {
        TRAILS.remove(world);
    }

    public static void clearAll() {
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

    private static final class TrailState {
        private long expiresAt = Long.MIN_VALUE;
        private long nextPulse = Long.MIN_VALUE;
        private int interval = 20;
        private float damage;
        private int targetCap = 12;
        private double stainRadius = 4.5;
        private ItemStack stack = ItemStack.EMPTY;
        private Vec3d lastPosition = Vec3d.ZERO;
        private long veilUntil = Long.MIN_VALUE;
        private long footprintUntil = Long.MIN_VALUE;
        private double footprintReduction;
    }
}
