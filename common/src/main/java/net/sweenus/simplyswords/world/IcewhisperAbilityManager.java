package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// Owner-side Icewhisper state for the nodes whose triggers sit outside a single aura pulse or comet impact.
public final class IcewhisperAbilityManager {
    public static final int MODE_KILLING_COLD = 1 << 7;
    public static final int MODE_WHITE_EXPANSE = 1 << 8;
    public static final int MODE_TWIN_WAKE = 1 << 13;
    public static final int MODE_HUNTERS_SKY = 1 << 14;
    public static final int MODE_FRACTURE = 1 << 15;
    public static final int MODE_FROST_WARD = 1 << 18;
    public static final int MODE_SNOWBLIND = 1 << 19;
    public static final int MODE_ICE_ARMOR = 1 << 20;
    public static final int MODE_WINTER_STEP = 1 << 21;
    public static final int MODE_FROZEN_REBUKE = 1 << 22;
    public static final int MODE_VEILED_GROUND = 1 << 23;
    public static final int MODE_LAST_SNOW = 1 << 24;
    public static final int MODE_SNOWGLOBE = 1 << 25;
    public static final int MODE_BLACK_ICE = 1 << 26;

    private static final Identifier ATTACK_SLOW_ID = Identifier.of("simplyswords", "icewhisper_veiled_ground");
    private static final int AURA_TUNING_LIFETIME = 80;
    private static final int AFFECTED_LIFETIME = 50;
    private static final int STATE_LIFETIME = 2400;
    private static final int MAX_STATES = 32;
    private static final int MAX_TRACKED_TARGETS = 64;
    private static final Map<UUID, OwnerState> STATES = new HashMap<>();
    private static final Map<UUID, AttackSlow> ATTACK_SLOWS = new HashMap<>();
    private static boolean rebuking;

    private IcewhisperAbilityManager() {
    }

    public static boolean hasPending() {
        return !ATTACK_SLOWS.isEmpty();
    }

    // Permafrost resolves its tuning once per pulse; every owner-side node reads that snapshot.
    public static void onAuraPulse(ServerWorld world, LivingEntity owner, StormFrostWaterMasteryTuning tuning, double radius) {
        if (owner == null || world == null) {
            return;
        }
        long now = world.getTime();
        OwnerState state = state(owner, now);
        state.auraTuning = tuning;
        state.auraTuningExpiresAt = now + AURA_TUNING_LIFETIME;
        state.auraRadius = radius;
        state.affected.values().removeIf(deadline -> deadline <= now);
        state.dwellSince.values().removeIf(deadline -> deadline + AFFECTED_LIFETIME <= now);
        state.dwellReadyAt.values().removeIf(deadline -> deadline <= now);
        state.freezeFloor.values().removeIf(floor -> floor.expiresAt <= now);
    }

    public static void markAffected(LivingEntity owner, LivingEntity target, long now) {
        if (owner == null || target == null) {
            return;
        }
        OwnerState state = STATES.get(owner.getUuid());
        if (state == null) {
            return;
        }
        if (state.affected.size() >= MAX_TRACKED_TARGETS) {
            state.affected.values().removeIf(deadline -> deadline <= now);
        }
        if (state.affected.size() < MAX_TRACKED_TARGETS) {
            state.affected.put(target.getUuid(), now + AFFECTED_LIFETIME);
        }
    }

    public static boolean isAuraAffected(LivingEntity owner, LivingEntity target) {
        if (owner == null || target == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        OwnerState state = STATES.get(owner.getUuid());
        if (state == null) {
            return false;
        }
        Long deadline = state.affected.get(target.getUuid());
        return deadline != null && deadline > world.getTime();
    }

    // Snowglobe contains comets inside the live Permafrost radius, which only the aura pass knows.
    public static double auraRadius(LivingEntity owner, double fallback) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return fallback;
        }
        OwnerState state = STATES.get(owner.getUuid());
        return state != null && state.auraTuningExpiresAt > world.getTime() && state.auraRadius > 0
                ? state.auraRadius : fallback;
    }

    // Cold Snap: an enemy that has been in Permafrost for the dwell window takes one extra pulse.
    public static double dwellMultiplier(LivingEntity owner, LivingEntity target, long now,
                                         StormFrostWaterMasteryTuning tuning) {
        double multiplier = tuning.get(s("ICEWHISPER_DWELL_DAMAGE_MULTIPLIER"), 0);
        int dwell = tuning.integer(s("ICEWHISPER_DWELL_TICKS"), 0);
        if (owner == null || target == null || multiplier <= 0 || dwell <= 0) {
            return 0;
        }
        OwnerState state = state(owner, now);
        long since = state.dwellSince.computeIfAbsent(target.getUuid(), ignored -> now);
        if (now - since < dwell) {
            return 0;
        }
        Long readyAt = state.dwellReadyAt.get(target.getUuid());
        if (readyAt != null && readyAt > now) {
            return 0;
        }
        state.dwellReadyAt.put(target.getUuid(),
                now + Math.max(1, tuning.integer(s("ICEWHISPER_DWELL_LOCKOUT_TICKS"), dwell)));
        return multiplier;
    }

    // Hoarfrost accrues freezing that vanilla's two-per-tick thaw would otherwise erase between pulses.
    public static int accrueFreeze(LivingEntity owner, LivingEntity target, long now, int perPulse, int cap) {
        if (owner == null || target == null || perPulse <= 0 || cap <= 0) {
            return 0;
        }
        OwnerState state = state(owner, now);
        FreezeFloor floor = state.freezeFloor.computeIfAbsent(target.getUuid(), ignored -> new FreezeFloor());
        floor.ticks = Math.min(cap, Math.max(floor.ticks, target.getFrozenTicks()) + perPulse);
        floor.expiresAt = now + AFFECTED_LIFETIME;
        if (state.freezeFloor.size() > MAX_TRACKED_TARGETS) {
            state.freezeFloor.values().removeIf(tracked -> tracked.expiresAt <= now);
        }
        return floor.ticks;
    }

    // Veiled Ground holds a managed modifier rather than a status effect, and releases it on expiry.
    public static void applyAttackSlow(LivingEntity target, double multiplier, int ticks, long now) {
        if (target == null || ticks <= 0 || multiplier <= 0 || multiplier >= 1) {
            return;
        }
        EntityAttributeInstance attackSpeed = target.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }
        double bonus = multiplier - 1;
        EntityAttributeModifier current = attackSpeed.getModifier(ATTACK_SLOW_ID);
        if (current == null || current.value() != bonus) {
            attackSpeed.removeModifier(ATTACK_SLOW_ID);
            attackSpeed.addTemporaryModifier(new EntityAttributeModifier(ATTACK_SLOW_ID, bonus,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        ATTACK_SLOWS.put(target.getUuid(), new AttackSlow(now + ticks));
    }

    public static void sweepAttackSlows(ServerWorld world) {
        if (world == null || ATTACK_SLOWS.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<Map.Entry<UUID, AttackSlow>> iterator = ATTACK_SLOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, AttackSlow> entry = iterator.next();
            if (entry.getValue().expiresAt > now) {
                continue;
            }
            if (world.getEntity(entry.getKey()) instanceof LivingEntity target) {
                removeAttackSlow(target);
                iterator.remove();
            } else if (entry.getValue().expiresAt + STATE_LIFETIME <= now) {
                iterator.remove();
            }
        }
    }

    public static void onStormStarted(LivingEntity owner, StormFrostWaterMasteryTuning tuning, long expiresAt) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return;
        }
        OwnerState state = state(owner, world.getTime());
        state.stormTuning = tuning;
        state.stormExpiresAt = expiresAt;
        state.lastSnowUsed = false;
        state.fractureStacks = 0;
        state.fractureExpiresAt = 0;
    }

    public static void onStormEnded(UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        OwnerState state = STATES.get(ownerId);
        if (state != null) {
            state.stormTuning = null;
            state.stormExpiresAt = 0;
        }
    }

    public static boolean isStormActive(LivingEntity owner) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        OwnerState state = STATES.get(owner.getUuid());
        return state != null && state.stormTuning != null && state.stormExpiresAt > world.getTime();
    }

    public static boolean consumeLastSnow(LivingEntity owner) {
        if (owner == null) {
            return false;
        }
        OwnerState state = STATES.get(owner.getUuid());
        if (state == null || state.lastSnowUsed) {
            return false;
        }
        state.lastSnowUsed = true;
        return true;
    }

    // Fracture reads the stacks built by earlier hits and then counts this one.
    public static double fractureMultiplier(LivingEntity owner, long now, StormFrostWaterMasteryTuning tuning) {
        double perStack = tuning.get(s("ICEWHISPER_FRACTURE_PER_STACK_MULTIPLIER"), 0);
        int window = tuning.integer(s("ICEWHISPER_FRACTURE_WINDOW_TICKS"), 0);
        int cap = tuning.integer(s("ICEWHISPER_FRACTURE_STACK_CAP"), 0);
        if (owner == null || perStack <= 0 || window <= 0 || cap <= 0) {
            return 1;
        }
        OwnerState state = state(owner, now);
        if (state.fractureExpiresAt <= now) {
            state.fractureStacks = 0;
        }
        double multiplier = 1 + Math.min(cap, state.fractureStacks) * perStack;
        state.fractureStacks = Math.min(cap, state.fractureStacks + 1);
        state.fractureExpiresAt = now + window;
        return multiplier;
    }

    public static float modifyIncomingDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim == null || amount <= 0 || !(victim.getWorld() instanceof ServerWorld world)) {
            return amount;
        }
        OwnerState state = STATES.get(victim.getUuid());
        if (state == null || state.auraTuning == null || world.getTime() > state.auraTuningExpiresAt) {
            return amount;
        }
        if (state.auraTuning.flag(MODE_ICE_ARMOR) && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            amount *= (float) state.auraTuning.get(s("ICEWHISPER_PROJECTILE_INCOMING_MULTIPLIER"), 1);
        }
        if (state.auraTuning.flag(MODE_WHITE_EXPANSE)
                && source.getAttacker() instanceof LivingEntity attacker
                && isAuraAffected(victim, attacker)) {
            amount *= (float) state.auraTuning.get(s("ICEWHISPER_AURA_INCOMING_MULTIPLIER"), 1);
        }
        return amount;
    }

    // Frozen Rebuke only answers melee taken while this wielder's own comet storm is running.
    public static void onDamageApplied(LivingEntity victim, DamageSource source) {
        if (rebuking || victim == null || !(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        OwnerState state = STATES.get(victim.getUuid());
        if (state == null
                || state.stormTuning == null
                || world.getTime() > state.stormExpiresAt
                || !state.stormTuning.flag(MODE_FROZEN_REBUKE)
                || world.getTime() < state.rebukeReadyAt
                || source.isIn(DamageTypeTags.IS_PROJECTILE)
                || !(source.getAttacker() instanceof LivingEntity attacker)
                || attacker == victim
                || !attacker.isAlive()
                || !HelperMethods.checkAbilityTarget(attacker, victim)) {
            return;
        }
        int freeze = state.stormTuning.integer(s("ICEWHISPER_REBUKE_FREEZE_TICKS"), 0);
        if (freeze <= 0) {
            return;
        }
        state.rebukeReadyAt = world.getTime()
                + Math.max(1, state.stormTuning.integer(s("ICEWHISPER_REBUKE_LOCKOUT_TICKS"), 30));
        rebuking = true;
        try {
            attacker.setFrozenTicks(Math.min(attacker.getMinFreezeDamageTicks(),
                    attacker.getFrozenTicks() + freeze));
        } finally {
            rebuking = false;
        }
        world.spawnParticles(net.minecraft.particle.ParticleTypes.SNOWFLAKE,
                attacker.getX(), attacker.getBodyY(0.6), attacker.getZ(), 12, 0.28, 0.32, 0.28, 0.02);
    }

    public static void clear(ServerWorld world) {
        if (world == null) {
            return;
        }
        STATES.keySet().removeIf(uuid -> world.getEntity(uuid) != null);
        ATTACK_SLOWS.keySet().removeIf(uuid -> {
            if (world.getEntity(uuid) instanceof LivingEntity target) {
                removeAttackSlow(target);
                return true;
            }
            return false;
        });
    }

    public static void clearAll() {
        STATES.clear();
        ATTACK_SLOWS.clear();
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) {
            return;
        }
        STATES.remove(actor.getUuid());
        if (ATTACK_SLOWS.remove(actor.getUuid()) != null) {
            removeAttackSlow(actor);
        }
    }

    private static void removeAttackSlow(LivingEntity target) {
        EntityAttributeInstance attackSpeed = target.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SLOW_ID);
        }
    }

    private static OwnerState state(LivingEntity owner, long now) {
        Iterator<Map.Entry<UUID, OwnerState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt <= now) {
                iterator.remove();
            }
        }
        OwnerState state = STATES.computeIfAbsent(owner.getUuid(), ignored -> new OwnerState());
        state.expiresAt = now + STATE_LIFETIME;
        if (STATES.size() > MAX_STATES) {
            STATES.entrySet().stream()
                    .sorted(Comparator.comparingLong(entry -> entry.getValue().expiresAt))
                    .limit(STATES.size() - (long) MAX_STATES)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(STATES::remove);
        }
        return state;
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }

    private static final class FreezeFloor {
        private int ticks;
        private long expiresAt;
    }

    private record AttackSlow(long expiresAt) {
    }

    private static final class OwnerState {
        private final Map<UUID, Long> affected = new HashMap<>();
        private final Map<UUID, Long> dwellSince = new HashMap<>();
        private final Map<UUID, Long> dwellReadyAt = new HashMap<>();
        private final Map<UUID, FreezeFloor> freezeFloor = new HashMap<>();
        private StormFrostWaterMasteryTuning auraTuning;
        private long auraTuningExpiresAt;
        private double auraRadius;
        private StormFrostWaterMasteryTuning stormTuning;
        private long stormExpiresAt;
        private long rebukeReadyAt;
        private long fractureExpiresAt;
        private int fractureStacks;
        private boolean lastSnowUsed;
        private long expiresAt;
    }
}
