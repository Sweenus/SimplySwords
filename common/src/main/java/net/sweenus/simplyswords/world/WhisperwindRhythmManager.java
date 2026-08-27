package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.ability.Phase3AbilityTuning;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WhisperwindRhythmManager {
    private static final Map<ServerWorld, Map<UUID, RhythmState>> STATES = new HashMap<>();

    private WhisperwindRhythmManager() {
    }

    // Rhythmic Cuts: each failed refresh raises the next roll; Reprise guarantees one after a kill.
    public static int resolveChance(ServerWorld world, LivingEntity owner, Phase3AbilityTuning tuning,
                                    int baseChance) {
        RhythmState state = state(world, owner.getUuid());
        long now = world.getTime();
        int repriseWindow = tuning.integer(Phase3AbilityTuning.Setting.REPRISE_WINDOW_TICKS, 0);
        if (repriseWindow > 0 && state.repriseUntil >= now) {
            state.repriseUntil = Long.MIN_VALUE;
            return 100;
        }
        double perFailure = tuning.get(Phase3AbilityTuning.Setting.RHYTHM_CHANCE_PER_FAILURE, 0);
        if (perFailure <= 0) return baseChance;
        int window = tuning.integer(Phase3AbilityTuning.Setting.RHYTHM_WINDOW_TICKS, 100);
        if (state.failuresExpire < now) state.failures = 0;
        state.failuresExpire = now + window;
        double cap = tuning.get(Phase3AbilityTuning.Setting.RHYTHM_CHANCE_CAP, 0);
        return baseChance + (int) Math.min(cap, state.failures * perFailure);
    }

    public static void recordRefreshResult(ServerWorld world, LivingEntity owner, boolean refreshed) {
        RhythmState state = state(world, owner.getUuid());
        if (refreshed) {
            state.failures = 0;
        } else {
            state.failures++;
        }
    }

    // Still Wind: every third attack in the window primes a single-target strike.
    public static boolean primeStillWind(ServerWorld world, LivingEntity owner, Phase3AbilityTuning tuning) {
        int threshold = tuning.integer(Phase3AbilityTuning.Setting.STILL_WIND_THRESHOLD, 0);
        if (threshold <= 0) return false;
        RhythmState state = state(world, owner.getUuid());
        long now = world.getTime();
        int window = Math.max(1, tuning.integer(Phase3AbilityTuning.Setting.STILL_WIND_WINDOW_TICKS, 80));
        if (state.attacksExpire < now) state.attacks = 0;
        state.attacksExpire = now + window;
        state.attacks++;
        if (state.attacks < threshold) return false;
        state.attacks = 0;
        state.stillWindPrimed = true;
        return true;
    }

    public static boolean consumeStillWind(ServerWorld world, LivingEntity owner) {
        RhythmState state = state(world, owner.getUuid());
        boolean primed = state.stillWindPrimed;
        state.stillWindPrimed = false;
        return primed;
    }

    // Dancing Gale: a refresh shortens the cooldown instead of clearing it.
    public static boolean tryPartialRefresh(ServerWorld world, LivingEntity owner, ItemStack stack,
                                            Phase3AbilityTuning tuning, int totalCooldownTicks) {
        int refund = tuning.integer(Phase3AbilityTuning.Setting.REFRESH_REFUND_TICKS, 0);
        if (refund <= 0) return false;
        RhythmState state = state(world, owner.getUuid());
        long now = world.getTime();
        int interval = Math.max(1, tuning.integer(Phase3AbilityTuning.Setting.REFRESH_INTERVAL_TICKS, 20));
        if (now < state.refreshReady) return true;
        state.refreshReady = now + interval;
        net.sweenus.simplyswords.api.SimplySwordsAPI.reduceWeaponCooldown(owner, stack,
                totalCooldownTicks, refund);
        int speedTicks = tuning.integer(Phase3AbilityTuning.Setting.REFRESH_SPEED_TICKS, 0);
        if (speedTicks > 0) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, speedTicks,
                    Math.max(0, tuning.integer(Phase3AbilityTuning.Setting.REFRESH_SPEED_AMPLIFIER, 1)),
                    false, true, true), owner);
        }
        return true;
    }

    // Perfect Tempo: dashing soon after a refresh sharpens the next strike.
    public static void recordRefresh(ServerWorld world, LivingEntity owner) {
        state(world, owner.getUuid()).lastRefresh = world.getTime();
    }

    public static double tempoBonus(ServerWorld world, LivingEntity owner, Phase3AbilityTuning tuning) {
        double bonus = tuning.get(Phase3AbilityTuning.Setting.TEMPO_DAMAGE_BONUS, 0);
        int window = tuning.integer(Phase3AbilityTuning.Setting.TEMPO_WINDOW_TICKS, 0);
        if (bonus <= 0 || window <= 0) return 0;
        RhythmState state = state(world, owner.getUuid());
        if (world.getTime() - state.lastRefresh > window) return 0;
        int hasteTicks = tuning.integer(Phase3AbilityTuning.Setting.TEMPO_HASTE_TICKS, 0);
        if (hasteTicks > 0) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, hasteTicks, 0,
                    false, true, true), owner);
        }
        return bonus;
    }

    // Reprise: a Delayed strike kill arms the next refresh.
    public static void recordStrikeKill(ServerWorld world, LivingEntity owner, Phase3AbilityTuning tuning) {
        int window = tuning.integer(Phase3AbilityTuning.Setting.REPRISE_WINDOW_TICKS, 0);
        if (window <= 0) return;
        state(world, owner.getUuid()).repriseUntil = world.getTime() + window;
    }

    // Windbreak: dashing clears Slowness and softens incoming projectiles briefly.
    public static void startWindbreak(ServerWorld world, LivingEntity owner, Phase3AbilityTuning tuning) {
        int ticks = tuning.integer(Phase3AbilityTuning.Setting.WINDBREAK_TICKS, 0);
        if (ticks <= 0) return;
        owner.removeStatusEffect(StatusEffects.SLOWNESS);
        RhythmState state = state(world, owner.getUuid());
        state.windbreakUntil = world.getTime() + ticks;
        state.windbreakReduction = tuning.get(Phase3AbilityTuning.Setting.WINDBREAK_PROJECTILE_REDUCTION, 0);
    }

    // Wind Wake: a short burst of speed and footing after the dash.
    public static void startWake(ServerWorld world, LivingEntity owner, Phase3AbilityTuning tuning) {
        int ticks = tuning.integer(Phase3AbilityTuning.Setting.WAKE_DURATION_TICKS, 0);
        if (ticks <= 0) return;
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ticks,
                Math.max(0, tuning.integer(Phase3AbilityTuning.Setting.WAKE_SPEED_AMPLIFIER, 1)),
                false, true, true), owner);
        RhythmState state = state(world, owner.getUuid());
        state.wakeUntil = world.getTime() + ticks;
        state.wakeResistance = tuning.get(Phase3AbilityTuning.Setting.WAKE_KNOCKBACK_RESISTANCE, 0);
    }

    public static double knockbackResistanceBonus(LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) return 0;
        Map<UUID, RhythmState> states = STATES.get(world);
        RhythmState state = states == null ? null : states.get(owner.getUuid());
        if (state == null || world.getTime() > state.wakeUntil) return 0;
        return state.wakeResistance;
    }

    public static float modifyIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (actor == null || source == null || amount <= 0.0F
                || !(actor.getWorld() instanceof ServerWorld world)) {
            return amount;
        }
        Map<UUID, RhythmState> states = STATES.get(world);
        RhythmState state = states == null ? null : states.get(actor.getUuid());
        if (state == null) return amount;
        long now = world.getTime();
        if (state.windbreakReduction > 0 && now <= state.windbreakUntil
                && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            amount *= (float) Math.max(0.0, 1.0 - state.windbreakReduction);
        }
        if (state.fallReduction > 0 && source.isIn(DamageTypeTags.IS_FALL)
                && isReady(actor)) {
            amount *= (float) Math.max(0.0, 1.0 - state.fallReduction);
        }
        return amount;
    }

    // Light on Foot: a standing buff while Fatal Flicker is off cooldown.
    public static void tickHolder(ServerWorld world, LivingEntity owner, ItemStack stack,
                                  Phase3AbilityTuning tuning) {
        int amplifier = tuning.integer(Phase3AbilityTuning.Setting.READY_SPEED_AMPLIFIER, -1);
        double fall = tuning.get(Phase3AbilityTuning.Setting.FALL_DAMAGE_REDUCTION, 0);
        if (amplifier < 0 && fall <= 0) return;
        RhythmState state = state(world, owner.getUuid());
        state.fallReduction = fall;
        state.ready = isReady(owner) || !(owner instanceof net.minecraft.entity.player.PlayerEntity player)
                || !player.getItemCooldownManager().isCoolingDown(stack.getItem());
        if (amplifier >= 0 && state.ready) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, amplifier,
                    false, false, true), owner);
        }
    }

    private static boolean isReady(LivingEntity owner) {
        ItemStack stack = owner.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.WHISPERWIND.get()) || !AwakeningApi.isAbilityUnlocked(stack)) return false;
        return !(owner instanceof net.minecraft.entity.player.PlayerEntity player)
                || !player.getItemCooldownManager().isCoolingDown(stack.getItem());
    }

    public static void clear(ServerWorld world) {
        STATES.remove(world);
    }

    public static void clearAll() {
        STATES.clear();
    }

    private static RhythmState state(ServerWorld world, UUID ownerId) {
        return STATES.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(ownerId, ignored -> new RhythmState());
    }

    private static final class RhythmState {
        private int failures;
        private long failuresExpire = Long.MIN_VALUE;
        private int attacks;
        private long attacksExpire = Long.MIN_VALUE;
        private boolean stillWindPrimed;
        private long refreshReady = Long.MIN_VALUE;
        private long lastRefresh = Long.MIN_VALUE;
        private long repriseUntil = Long.MIN_VALUE;
        private long windbreakUntil = Long.MIN_VALUE;
        private double windbreakReduction;
        private long wakeUntil = Long.MIN_VALUE;
        private double wakeResistance;
        private double fallReduction;
        private boolean ready;
    }
}
