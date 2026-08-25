package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.ability.Phase2AbilityTuning;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Phase2CombatStateManager {
    private static final int MAX_STATES_PER_WORLD = 256;
    private static final Map<ServerWorld, Map<UUID, WickState>> WICK = new HashMap<>();

    private Phase2CombatStateManager() {
    }

    public static void recordWick(ServerWorld world, LivingEntity actor,
                                  Phase2AbilityTuning tuning, int duration) {
        Map<UUID, WickState> states = WICK.computeIfAbsent(world, ignored -> new HashMap<>());
        if (states.size() >= MAX_STATES_PER_WORLD && !states.containsKey(actor.getUuid())) {
            states.values().stream().min(Comparator.comparingLong(state -> state.expiresAt))
                    .ifPresent(state -> states.remove(state.actorId));
        }
        states.put(actor.getUuid(), new WickState(actor.getUuid(), tuning, world.getTime() + Math.max(1, duration)));
    }

    public static void applyWickFrenzyHit(ServerPlayerEntity actor, LivingEntity target,
                                           ItemStack stack, float baseDamage) {
        StatusEffectInstance frenzy = actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FRENZY));
        if (frenzy == null) return;
        WickState state = state(actor);
        Phase2AbilityTuning tuning = state == null ? Phase2AbilityTuning.EMPTY : state.tuning;
        int mode = tuning.integer(Phase2AbilityTuning.Setting.MODE, 0);
        int levels = frenzy.getAmplifier() + 1;
        float multiplier = (float) tuning.get(Phase2AbilityTuning.Setting.MELEE_BONUS_PER_STACK, 1);
        if ((mode & 128) != 0) multiplier = 1.75F;
        long now = actor.getWorld().getTime();
        if (state != null && (mode & 16) != 0) {
            if (now - state.lastHitTick <= tuning.integer(Phase2AbilityTuning.Setting.DURATION_TICKS, 40)) {
                state.chain = Math.min(4, state.chain + 1);
            } else {
                state.chain = 1;
            }
            state.lastHitTick = now;
            multiplier *= 1 + Math.min(tuning.get(Phase2AbilityTuning.Setting.BONUS_CAP, .2),
                    state.chain * tuning.get(Phase2AbilityTuning.Setting.BONUS_PER_TRIGGER, .05));
        }
        if ((mode & 8) != 0) {
            if (target.isOnFire()) multiplier *= 1 + tuning.get(Phase2AbilityTuning.Setting.BONUS_PER_TRIGGER, .2);
            target.setOnFireFor(Math.max(1, tuning.integer(Phase2AbilityTuning.Setting.FIRE_TICKS, 40) / 20));
        }
        DamageSource source = actor.getDamageSources().playerAttack(actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(actor.getServerWorld(), stack, target,
                source, baseDamage * multiplier);
        boolean damaged = target.damage(source, damage);
        if (damaged && (mode & 64) != 0) burst(actor, target, stack, baseDamage, tuning);
        boolean conserve = !target.isAlive() && state != null && (mode & 4) != 0 && now >= state.conserveReady;
        if (conserve) state.conserveReady = now + tuning.integer(Phase2AbilityTuning.Setting.LOCKOUT_TICKS, 20);
        if (!conserve) {
            int consumption = (mode & 64) != 0 ? 2 : 1;
            for (int index = 0; index < consumption; index++) {
                HelperMethods.decrementStatusEffect(actor, EffectRegistry.getReference(EffectRegistry.FRENZY));
            }
        }
    }

    public static boolean hasActive(ServerWorld world) {
        return !WICK.getOrDefault(world, Map.of()).isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, WickState> states = WICK.get(world);
        if (states == null) return;
        long now = world.getTime();
        states.values().removeIf(state -> state.expiresAt < now);
        if (states.isEmpty()) WICK.remove(world);
    }

    private static WickState state(ServerPlayerEntity actor) {
        Map<UUID, WickState> states = WICK.get(actor.getServerWorld());
        WickState state = states == null ? null : states.get(actor.getUuid());
        if (state != null && state.expiresAt < actor.getWorld().getTime()) {
            states.remove(actor.getUuid());
            return null;
        }
        return state;
    }

    private static void burst(ServerPlayerEntity actor, LivingEntity direct, ItemStack stack,
                              float baseDamage, Phase2AbilityTuning tuning) {
        double radius = tuning.get(Phase2AbilityTuning.Setting.IMPACT_RADIUS, 0);
        int cap = tuning.integer(Phase2AbilityTuning.Setting.IMPACT_TARGET_CAP, 0);
        float damage = baseDamage * (float) tuning.get(Phase2AbilityTuning.Setting.IMPACT_DAMAGE_MULTIPLIER, 0);
        if (radius <= 0 || cap <= 0 || damage <= 0) return;
        var targets = actor.getServerWorld().getEntitiesByClass(LivingEntity.class,
                new Box(direct.getPos(), direct.getPos()).expand(radius), candidate -> candidate != actor
                        && candidate != direct && candidate.isAlive()
                        && candidate.squaredDistanceTo(direct) <= radius * radius
                        && HelperMethods.checkAbilityTarget(candidate, actor));
        targets.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(direct)));
        for (int index = 0; index < Math.min(cap, targets.size()); index++) {
            SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(actor.getServerWorld(), actor, stack,
                    targets.get(index), damage, SpellScalingProfile.FIRE);
        }
    }

    private static final class WickState {
        private final UUID actorId;
        private final Phase2AbilityTuning tuning;
        private final long expiresAt;
        private long lastHitTick = Long.MIN_VALUE / 2;
        private long conserveReady;
        private int chain;

        private WickState(UUID actorId, Phase2AbilityTuning tuning, long expiresAt) {
            this.actorId = actorId;
            this.tuning = tuning;
            this.expiresAt = expiresAt;
        }
    }
}
