package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
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
    private static final int WILDFIRE = 64;
    private static final int TWIN_FLAMES = 32;
    private static final int WARNING_FLICKER = 256;
    private static final int PHOENIX_BLOW = 1024;
    private static final Map<ServerWorld, Map<UUID, WickState>> WICK = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, ReviveState>> REVIVE = new HashMap<>();

    private Phase2CombatStateManager() {
    }

    public static void recordWick(ServerWorld world, LivingEntity actor,
                                  Phase2AbilityTuning tuning, int duration) {
        Map<UUID, WickState> states = WICK.computeIfAbsent(world, ignored -> new HashMap<>());
        if (states.size() >= MAX_STATES_PER_WORLD && !states.containsKey(actor.getUuid())) {
            states.values().stream().min(Comparator.comparingLong(state -> state.expiresAt))
                    .ifPresent(state -> states.remove(state.actorId));
        }
        WickState previous = states.get(actor.getUuid());
        WickState replacement = new WickState(actor.getUuid(), tuning,
                world.getTime() + Math.max(1, duration));
        if (previous != null) {
            replacement.conserveReady = previous.conserveReady;
            replacement.twinReady = previous.twinReady;
            replacement.guardReady = previous.guardReady;
        }
        states.put(actor.getUuid(), replacement);
    }

    public static int frenzyGrant(ServerWorld world, LivingEntity actor,
                                  Phase2AbilityTuning tuning, boolean dualWielding) {
        int mode = tuning.integer(Phase2AbilityTuning.Setting.MODE, 0);
        Map<UUID, WickState> states = WICK.get(world);
        WickState state = states == null ? null : states.get(actor.getUuid());
        long now = world.getTime();
        if (!dualWielding || (mode & TWIN_FLAMES) == 0 || state == null || now < state.twinReady) return 1;
        state.twinReady = now + tuning.integer(Phase2AbilityTuning.Setting.TWIN_LOCKOUT_TICKS, 40);
        return Math.max(1, tuning.integer(Phase2AbilityTuning.Setting.TWIN_STACK_GRANT, 2));
    }

    public static void recordRevive(ServerWorld world, LivingEntity actor, Phase2AbilityTuning tuning) {
        int window = tuning.integer(Phase2AbilityTuning.Setting.REVIVE_WINDOW_TICKS, 0);
        if ((tuning.integer(Phase2AbilityTuning.Setting.MODE, 0) & PHOENIX_BLOW) == 0 || window <= 0) return;
        Map<UUID, ReviveState> states = REVIVE.computeIfAbsent(world, ignored -> new HashMap<>());
        if (states.size() >= MAX_STATES_PER_WORLD && !states.containsKey(actor.getUuid())) {
            states.values().stream().min(Comparator.comparingLong(state -> state.expiresAt))
                    .ifPresent(state -> states.remove(state.actorId));
        }
        states.put(actor.getUuid(), new ReviveState(actor.getUuid(), tuning, world.getTime() + window));
    }

    public static void applyPhoenixBlow(ServerPlayerEntity actor, LivingEntity target,
                                        ItemStack stack, float baseDamage) {
        Map<UUID, ReviveState> states = REVIVE.get(actor.getServerWorld());
        ReviveState state = states == null ? null : states.remove(actor.getUuid());
        if (state == null || state.expiresAt < actor.getWorld().getTime()) return;
        Phase2AbilityTuning tuning = state.tuning;
        float damage = baseDamage * (float) tuning.get(Phase2AbilityTuning.Setting.DAMAGE_MULTIPLIER, 0);
        int fireTicks = tuning.integer(Phase2AbilityTuning.Setting.REVIVE_FIRE_TICKS, 0);
        if (fireTicks > 0) target.setOnFireFor(Math.max(1, fireTicks / 20));
        if (damage <= 0) return;
        SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(actor.getServerWorld(), actor, stack,
                target, damage, SpellScalingProfile.FIRE);
    }

    public static float modifyIncomingDamage(LivingEntity entity, DamageSource source, float amount) {
        if (WICK.isEmpty() || !(entity instanceof ServerPlayerEntity player)) return amount;
        WickState state = state(player);
        if (state == null) return amount;
        Phase2AbilityTuning tuning = state.tuning;
        int mode = tuning.integer(Phase2AbilityTuning.Setting.MODE, 0);
        long now = player.getWorld().getTime();
        if ((mode & WARNING_FLICKER) != 0 && now >= state.guardReady
                && crossedLowHealth(player.getHealth(), player.getMaxHealth(), amount,
                tuning.integer(Phase2AbilityTuning.Setting.LOW_HEALTH_PERCENT, 30))) {
            state.guardReady = now + tuning.integer(Phase2AbilityTuning.Setting.GUARD_LOCKOUT_TICKS, 300);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    tuning.integer(Phase2AbilityTuning.Setting.GUARD_STATUS_TICKS, 60), 0));
        }
        double reduction = tuning.get(Phase2AbilityTuning.Setting.DAMAGE_REDUCTION, 0);
        if (reduction <= 0 || !player.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FRENZY))) {
            return amount;
        }
        return amount * (1.0F - (float) reduction);
    }

    public static void applyWickFrenzyHit(ServerPlayerEntity actor, LivingEntity target,
                                           ItemStack stack, float baseDamage) {
        StatusEffectInstance frenzy = actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FRENZY));
        if (frenzy == null) return;
        WickState state = state(actor);
        Phase2AbilityTuning tuning = state == null ? Phase2AbilityTuning.EMPTY : state.tuning;
        int mode = tuning.integer(Phase2AbilityTuning.Setting.MODE, 0);
        float multiplier = (float) tuning.get(Phase2AbilityTuning.Setting.MELEE_BONUS_PER_STACK, 1);
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
            if (target.isOnFire()) multiplier *= 1 + tuning.get(Phase2AbilityTuning.Setting.BURN_DAMAGE_BONUS, .2);
            target.setOnFireFor(Math.max(1, tuning.integer(Phase2AbilityTuning.Setting.FIRE_TICKS, 40) / 20));
        }
        DamageSource source = actor.getDamageSources().playerAttack(actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(actor.getServerWorld(), stack, target,
                source, baseDamage * multiplier);
        damage *= armorIgnoreMultiplier(damage, target.getArmor(), armorToughness(target),
                (float) tuning.get(Phase2AbilityTuning.Setting.ARMOR_IGNORE, 0));
        boolean damaged = target.damage(source, damage);
        if (damaged && (mode & WILDFIRE) != 0) burst(actor, target, stack, baseDamage, tuning);
        boolean conserve = !target.isAlive() && state != null && (mode & 4) != 0
                && (mode & WILDFIRE) == 0 && now >= state.conserveReady;
        if (conserve) state.conserveReady = now + tuning.integer(Phase2AbilityTuning.Setting.LOCKOUT_TICKS, 20);
        if (!conserve) {
            int consumption = (mode & WILDFIRE) != 0 && (mode & TWIN_FLAMES) == 0 ? 2 : 1;
            for (int index = 0; index < consumption; index++) {
                HelperMethods.decrementStatusEffect(actor, EffectRegistry.getReference(EffectRegistry.FRENZY));
            }
        }
    }

    static boolean crossedLowHealth(float current, float maximum, float amount, int percent) {
        if (percent <= 0 || maximum <= 0 || amount <= 0) return false;
        float threshold = maximum * percent / 100.0F;
        return current > threshold && current - amount <= threshold;
    }

    static float armorIgnoreMultiplier(float damage, float armor, float toughness, float ignored) {
        if (ignored <= 0 || armor <= 0 || damage <= 0) return 1.0F;
        float full = afterArmor(damage, armor, toughness);
        if (full <= 0) return 1.0F;
        return afterArmor(damage, Math.max(0, armor - ignored), toughness) / full;
    }

    private static float afterArmor(float damage, float armor, float toughness) {
        float divisor = 2.0F + toughness / 4.0F;
        float effective = MathHelper.clamp(armor - damage / divisor, armor * 0.2F, 20.0F);
        return damage * (1.0F - effective / 25.0F);
    }

    private static float armorToughness(LivingEntity target) {
        return (float) target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
    }

    public static boolean hasActive(ServerWorld world) {
        return !WICK.getOrDefault(world, Map.of()).isEmpty()
                || !REVIVE.getOrDefault(world, Map.of()).isEmpty();
    }

    public static void tick(ServerWorld world) {
        long now = world.getTime();
        Map<UUID, WickState> states = WICK.get(world);
        if (states != null) {
            states.values().removeIf(state -> state.expiresAt < now);
            if (states.isEmpty()) WICK.remove(world);
        }
        Map<UUID, ReviveState> revives = REVIVE.get(world);
        if (revives != null) {
            revives.values().removeIf(state -> state.expiresAt < now);
            if (revives.isEmpty()) REVIVE.remove(world);
        }
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
        double radius = tuning.get(Phase2AbilityTuning.Setting.MELEE_IMPACT_RADIUS, 0);
        int cap = tuning.integer(Phase2AbilityTuning.Setting.MELEE_IMPACT_TARGET_CAP, 0);
        float damage = baseDamage * (float) tuning.get(
                Phase2AbilityTuning.Setting.MELEE_IMPACT_DAMAGE_MULTIPLIER, 0);
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
        private long twinReady;
        private long guardReady;
        private int chain;

        private WickState(UUID actorId, Phase2AbilityTuning tuning, long expiresAt) {
            this.actorId = actorId;
            this.tuning = tuning;
            this.expiresAt = expiresAt;
        }
    }

    private static final class ReviveState {
        private final UUID actorId;
        private final Phase2AbilityTuning tuning;
        private final long expiresAt;

        private ReviveState(UUID actorId, Phase2AbilityTuning tuning, long expiresAt) {
            this.actorId = actorId;
            this.tuning = tuning;
            this.expiresAt = expiresAt;
        }
    }
}
