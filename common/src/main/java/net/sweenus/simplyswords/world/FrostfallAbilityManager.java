package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FrostfallAbilityManager {
    private static final Map<ServerWorld, Map<UUID, Empowerment>> EMPOWERMENTS = new HashMap<>();

    private FrostfallAbilityManager() {
    }

    public static void grantPerfectReturn(ServerWorld world, LivingEntity owner, double multiplier, int durationTicks) {
        if (world == null || owner == null || multiplier <= 1 || durationTicks <= 0) return;
        EMPOWERMENTS.computeIfAbsent(world, ignored -> new HashMap<>()).put(owner.getUuid(),
                new Empowerment(multiplier, world.getTime() + durationTicks));
    }

    public static float modifyOutgoingMeleeDamage(LivingEntity target, DamageSource source, float amount) {
        if (target == null || source == null || amount <= 0 || !source.isIn(DamageTypeTags.IS_PLAYER_ATTACK)
                || !(source.getAttacker() instanceof LivingEntity owner)
                || source.getSource() != owner
                || !(owner.getWorld() instanceof ServerWorld world)) return amount;
        Map<UUID, Empowerment> states = EMPOWERMENTS.get(world);
        if (states == null) return amount;
        Empowerment empowerment = states.remove(owner.getUuid());
        if (states.isEmpty()) EMPOWERMENTS.remove(world);
        if (empowerment == null || world.getTime() > empowerment.expiresAt) return amount;
        return amount * (float) empowerment.multiplier;
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        Map<UUID, Empowerment> states = EMPOWERMENTS.get(world);
        if (states == null) return;
        states.remove(actor.getUuid());
        if (states.isEmpty()) EMPOWERMENTS.remove(world);
    }

    public static void clear(ServerWorld world) {
        EMPOWERMENTS.remove(world);
    }

    public static void clearAll() {
        EMPOWERMENTS.clear();
    }

    public static double resolveDirectMultiplier(Phase6AbilityTuning tuning, int flightTicks,
                                                 boolean slowed, boolean deadfall) {
        double multiplier = tuning.get(s("DAMAGE_MULTIPLIER"), 1)
                * tuning.get(s("FROSTFALL_DIRECT_DAMAGE_MULTIPLIER"), 1)
                * tuning.get(s("FROSTFALL_SKIRMISHER_DAMAGE_MULTIPLIER"), 1);
        int threshold = tuning.integer(s("FROSTFALL_HIGH_ARC_FLIGHT_TICKS"), 0);
        if (threshold > 0 && flightTicks >= threshold)
            multiplier += tuning.get(s("FROSTFALL_HIGH_ARC_DAMAGE_MULTIPLIER"), 0);
        if (slowed) multiplier *= tuning.get(s("FROSTFALL_SLOWED_DAMAGE_MULTIPLIER"), 1);
        if (deadfall) multiplier *= tuning.get(s("FROSTFALL_DEADFALL_DAMAGE_MULTIPLIER"), 1);
        return multiplier;
    }

    public static int resolvePulseCount(Phase6AbilityTuning tuning) {
        return tuning.integer(s("FROSTFALL_FIELD_PULSE_COUNT"),
                tuning.integer(s("PULSE_COUNT"), 5));
    }

    public static int resolvePulseCount(Phase6AbilityTuning throwTuning, Phase6AbilityTuning fieldTuning) {
        if (fieldTuning.has(s("FROSTFALL_FIELD_PULSE_COUNT")))
            return fieldTuning.integer(s("FROSTFALL_FIELD_PULSE_COUNT"), 5);
        if (fieldTuning.has(s("PULSE_COUNT"))) return fieldTuning.integer(s("PULSE_COUNT"), 5);
        return throwTuning.integer(s("PULSE_COUNT"), 5);
    }

    public static int resolveFieldDuration(Phase6AbilityTuning tuning, int pulseCount) {
        int fallback = Math.max(1, pulseCount) * 20;
        return tuning.integer(s("FROSTFALL_FIELD_DURATION_TICKS"),
                tuning.integer(s("DURATION_TICKS"), fallback));
    }

    public static int resolveFieldDuration(Phase6AbilityTuning throwTuning,
                                           Phase6AbilityTuning fieldTuning, int pulseCount) {
        if (fieldTuning.has(s("FROSTFALL_FIELD_DURATION_TICKS")))
            return fieldTuning.integer(s("FROSTFALL_FIELD_DURATION_TICKS"), Math.max(1, pulseCount) * 20);
        if (fieldTuning.has(s("DURATION_TICKS")))
            return fieldTuning.integer(s("DURATION_TICKS"), Math.max(1, pulseCount) * 20);
        return throwTuning.integer(s("DURATION_TICKS"), Math.max(1, pulseCount) * 20);
    }

    public static int pulseDueTick(Phase6AbilityTuning tuning, int pulseIndex, int pulseCount, int fieldDuration) {
        int glacierDelay = tuning.integer(s("FROSTFALL_GLACIER_DELAY_TICKS"), 0);
        if (pulseCount == 1 && glacierDelay > 0) return glacierDelay;
        return Math.max(1, (int) Math.ceil((double) pulseIndex * fieldDuration / Math.max(1, pulseCount)));
    }

    public static double resolvePulseRadius(Phase6AbilityTuning tuning, double configuredRadius,
                                            int pulseIndex, int pulseCount) {
        double base = tuning.get(s("RADIUS"), configuredRadius)
                + tuning.get(s("FROSTFALL_PULSE_RADIUS_BONUS"), 0);
        double progress = pulseCount <= 1 ? 1 : (double) (pulseIndex - 1) / (pulseCount - 1);
        return Math.max(.75, base - 5 + 4 * progress);
    }

    public static double resolvePulseRadius(Phase6AbilityTuning throwTuning, Phase6AbilityTuning fieldTuning,
                                            double configuredRadius, int pulseIndex, int pulseCount) {
        double base = fieldTuning.has(s("RADIUS")) ? fieldTuning.get(s("RADIUS"), configuredRadius)
                : throwTuning.get(s("RADIUS"), configuredRadius);
        base += fieldTuning.get(s("FROSTFALL_PULSE_RADIUS_BONUS"), 0);
        double progress = pulseCount <= 1 ? 1 : (double) (pulseIndex - 1) / (pulseCount - 1);
        return Math.max(.75, base - 5 + 4 * progress);
    }

    public static double resolvePulseMultiplier(Phase6AbilityTuning throwTuning,
                                                Phase6AbilityTuning fieldTuning,
                                                int pulseIndex, int pulseCount,
                                                boolean deadfall, boolean shatter) {
        double generic = fieldTuning.has(s("DAMAGE_MULTIPLIER"))
                ? fieldTuning.get(s("DAMAGE_MULTIPLIER"), 1)
                : throwTuning.get(s("DAMAGE_MULTIPLIER"), 1);
        double multiplier = generic
                * fieldTuning.get(s("FROSTFALL_PULSE_DAMAGE_MULTIPLIER"), 1)
                * fieldTuning.get(s("FROSTFALL_AVALANCHE_DAMAGE_MULTIPLIER"), 1)
                * fieldTuning.get(s("FROSTFALL_GLACIER_DAMAGE_MULTIPLIER"), 1)
                * ((double) pulseIndex / Math.max(1, pulseCount));
        if (deadfall) multiplier *= throwTuning.get(s("FROSTFALL_DEADFALL_PULSE_DAMAGE_MULTIPLIER"), 1);
        if (shatter) multiplier *= fieldTuning.get(s("FROSTFALL_SHATTER_DAMAGE_MULTIPLIER"), 1);
        return multiplier;
    }

    public static int resolvePulseSlowTicks(Phase6AbilityTuning tuning, int configuredTicks) {
        return Math.max(1, tuning.integer(s("STATUS_DURATION_TICKS"), configuredTicks)
                + tuning.integer(s("FROSTFALL_PULSE_SLOW_BONUS_TICKS"), 0));
    }

    public static int resolvePulseSlowTicks(Phase6AbilityTuning throwTuning,
                                            Phase6AbilityTuning fieldTuning, int configuredTicks) {
        int base = fieldTuning.has(s("STATUS_DURATION_TICKS"))
                ? fieldTuning.integer(s("STATUS_DURATION_TICKS"), configuredTicks)
                : throwTuning.integer(s("STATUS_DURATION_TICKS"), configuredTicks);
        return Math.max(1, base + fieldTuning.integer(s("FROSTFALL_PULSE_SLOW_BONUS_TICKS"), 0));
    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }

    private record Empowerment(double multiplier, long expiresAt) {
    }
}
