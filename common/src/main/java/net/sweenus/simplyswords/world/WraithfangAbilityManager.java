package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WraithfangAbilityManager {
    private static final int MAX_STATES_PER_WORLD = 256;
    private static final int SOUL_CADENCE = 1024;
    private static final int RESTLESS_SPIRIT = 2048;
    private static final int FRENZIED_WRAITH = 4096;
    private static final int SILENT_WRAITH = 8192;
    private static final Map<ServerWorld, Map<UUID, State>> STATES = new HashMap<>();

    private WraithfangAbilityManager() {
    }

    public static double recordThrow(ServerWorld world, LivingEntity actor, ItemStack stack,
                                     WraithfangTuningSnapshot tuning) {
        State state = stateForWrite(world, actor, stack, tuning);
        long now = world.getTime();
        state.expiresAt = now + Math.max(2400, tuning.returnMeleeWindowTicks() + 20L);
        state.cooldownTotal = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                stack, actor, Math.max(1, tuning.cooldownTicks()));
        if (tuning.projectileGuardTicks() > 0) {
            state.guardUntil = now + tuning.projectileGuardTicks();
        }
        double multiplier = previewAlternation(state, Action.THROW, now);
        commitAlternation(state, Action.THROW, now);
        return multiplier;
    }

    public static boolean tryBurst(ServerWorld world, LivingEntity actor, int lockoutTicks) {
        if (lockoutTicks <= 0) return true;
        State state = state(world, actor.getUuid());
        if (state == null) return true;
        long now = world.getTime();
        if (now < state.burstReadyAt) return false;
        state.burstReadyAt = now + lockoutTicks;
        return true;
    }

    public static void onReturn(ServerWorld world, LivingEntity actor, ItemStack stack,
                                WraithfangTuningSnapshot tuning) {
        State state = stateForWrite(world, actor, stack, tuning);
        if (tuning.hasteDurationTicks() > 0) {
            grantHaste(actor, state, tuning.hasteDurationTicks(), tuning.hasteAmplifier());
        }
        if (tuning.hasMode(SILENT_WRAITH) && tuning.returnMeleeWindowTicks() > 0
                && tuning.returnMeleeDamageMultiplier() > 0) {
            state.silentUntil = world.getTime() + tuning.returnMeleeWindowTicks();
        }
    }

    public static void onPursuitComplete(ServerWorld world, LivingEntity actor, ItemStack stack,
                                         WraithfangTuningSnapshot tuning) {
        State state = stateForWrite(world, actor, stack, tuning);
        if (tuning.hasteDurationTicks() > 0) {
            grantHaste(actor, state, tuning.hasteDurationTicks(), tuning.hasteAmplifier());
        }
    }

    public static void onThrownKill(ServerWorld world, LivingEntity actor, ItemStack stack,
                                    WraithfangTuningSnapshot tuning) {
        State state = stateForWrite(world, actor, stack, tuning);
        if (tuning.killHasteDurationTicks() > 0) {
            grantHaste(actor, state, tuning.killHasteDurationTicks(), tuning.killHasteAmplifier());
        }
        refundCooldown(actor, state, tuning.killCooldownRefundTicks());
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world) || !source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            return amount;
        }
        State state = state(world, target.getUuid());
        if (state == null || world.getTime() >= state.guardUntil) return amount;
        return amount * (1.0F - (float) state.tuning.projectileDamageReduction());
    }

    public static float modifyOutgoingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world)
                || !source.isIn(DamageTypeTags.IS_PLAYER_ATTACK)
                || !(source.getAttacker() instanceof LivingEntity actor)) return amount;
        ItemStack stack = weaponStack(source, actor);
        if (stack.isEmpty()) return amount;
        State state = state(world, actor.getUuid());
        if (state == null) return amount;
        double multiplier = previewAlternation(state, Action.MELEE, world.getTime());
        if (actor.hasStatusEffect(StatusEffects.HASTE)) {
            multiplier *= 1 + state.tuning.meleeHasteBonus();
            if (state.tuning.hasMode(FRENZIED_WRAITH)) {
                multiplier *= state.tuning.hasteDamageMultiplier();
            }
        }
        return amount * (float) multiplier;
    }

    public static void onMeleeDamageApplied(LivingEntity target, DamageSource source) {
        if (!(target.getWorld() instanceof ServerWorld world)
                || !source.isIn(DamageTypeTags.IS_PLAYER_ATTACK)
                || !(source.getAttacker() instanceof LivingEntity actor)) return;
        ItemStack stack = weaponStack(source, actor);
        if (stack.isEmpty()) return;
        State state = state(world, actor.getUuid());
        if (state == null) return;
        if (actor instanceof ServerPlayerEntity player) {
            applySilentStrike(player, target, stack,
                    (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
        }
        commitAlternation(state, Action.MELEE, world.getTime());
    }

    public static void applySilentStrike(ServerPlayerEntity actor, LivingEntity target, ItemStack stack,
                                         float baseDamage) {
        State state = state(actor.getServerWorld(), actor.getUuid());
        if (state == null || actor.getWorld().getTime() > state.silentUntil
                || state.tuning.returnMeleeDamageMultiplier() <= 0) return;
        state.silentUntil = 0;
        float damage = baseDamage * (float) state.tuning.returnMeleeDamageMultiplier();
        SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(actor.getServerWorld(), actor, stack,
                target, damage, SpellScalingProfile.SOUL);
    }

    public static void onTargetDeath(LivingEntity target, DamageSource source) {
        if (!(target.getWorld() instanceof ServerWorld world)
                || !source.isIn(DamageTypeTags.IS_PLAYER_ATTACK)
                || !(source.getAttacker() instanceof LivingEntity actor)
                || !actor.hasStatusEffect(StatusEffects.HASTE)) return;
        ItemStack stack = weaponStack(source, actor);
        if (stack.isEmpty()) return;
        State state = state(world, actor.getUuid());
        if (state == null || !state.tuning.hasMode(RESTLESS_SPIRIT)) return;
        int remainingBudget = Math.max(0, state.tuning.hasteKillRefundCapTicks() - state.hasteRefundUsed);
        int refund = Math.min(remainingBudget, state.tuning.hasteKillRefundTicks());
        if (refund <= 0) return;
        state.hasteRefundUsed += refund;
        refundCooldown(actor, state, refund);
    }

    public static double projectileDamageMultiplier(LivingEntity actor, WraithfangTuningSnapshot tuning,
                                                    double alternationMultiplier) {
        double result = alternationMultiplier;
        if (actor.hasStatusEffect(StatusEffects.HASTE) && tuning.hasMode(FRENZIED_WRAITH)) {
            result *= tuning.hasteDamageMultiplier();
        }
        return result;
    }

    public static boolean hasActive(ServerWorld world) {
        return !STATES.getOrDefault(world, Map.of()).isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, State> states = STATES.get(world);
        if (states == null) return;
        long now = world.getTime();
        states.values().removeIf(state -> state.expiresAt < now
                || !(world.getEntity(state.actorId) instanceof LivingEntity living) || !living.isAlive());
        if (states.isEmpty()) STATES.remove(world);
    }

    public static void clear(ServerWorld world) {
        STATES.remove(world);
    }

    public static void clearAll() {
        STATES.clear();
    }

    static int nextAlternationStacks(int current, boolean withinWindow, boolean alternating) {
        if (!withinWindow || !alternating) return 0;
        return Math.min(4, current + 1);
    }

    static int remainingAfterRefund(int total, float progress, int refund) {
        return Math.max(0, Math.round(Math.max(0, progress) * Math.max(1, total)) - Math.max(0, refund));
    }

    private static double previewAlternation(State state, Action action, long now) {
        WraithfangTuningSnapshot tuning = state.tuning;
        if (!tuning.hasMode(SOUL_CADENCE) || tuning.alternationWindowTicks() <= 0) return 1;
        boolean within = now - state.lastActionAt <= tuning.alternationWindowTicks();
        int stacks = nextAlternationStacks(state.alternationStacks, within,
                state.lastAction != null && state.lastAction != action);
        return 1 + Math.min(tuning.alternationBonusCap(), stacks * tuning.alternationBonusPerStack());
    }

    private static void commitAlternation(State state, Action action, long now) {
        WraithfangTuningSnapshot tuning = state.tuning;
        if (!tuning.hasMode(SOUL_CADENCE) || tuning.alternationWindowTicks() <= 0) return;
        boolean within = now - state.lastActionAt <= tuning.alternationWindowTicks();
        state.alternationStacks = nextAlternationStacks(state.alternationStacks, within,
                state.lastAction != null && state.lastAction != action);
        state.lastAction = action;
        state.lastActionAt = now;
    }

    private static void grantHaste(LivingEntity actor, State state, int duration, int amplifier) {
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,
                Math.max(1, duration), Math.max(0, amplifier), false, false, true), actor);
        state.hasteRefundUsed = 0;
    }

    private static void refundCooldown(LivingEntity actor, State state, int refund) {
        if (!(actor instanceof ServerPlayerEntity player) || refund <= 0) return;
        int remaining = remainingAfterRefund(state.cooldownTotal,
                player.getItemCooldownManager().getCooldownProgress(ItemsRegistry.WRAITHFANG.get(), 0), refund);
        player.getItemCooldownManager().set(ItemsRegistry.WRAITHFANG.get(), remaining);
    }

    private static ItemStack weaponStack(DamageSource source, LivingEntity actor) {
        ItemStack sourceStack = source.getWeaponStack();
        if (sourceStack != null && sourceStack.isOf(ItemsRegistry.WRAITHFANG.get())) return sourceStack;
        ItemStack mainHandStack = actor.getMainHandStack();
        return mainHandStack.isOf(ItemsRegistry.WRAITHFANG.get()) ? mainHandStack : ItemStack.EMPTY;
    }

    private static State stateForWrite(ServerWorld world, LivingEntity actor, ItemStack stack,
                                       WraithfangTuningSnapshot tuning) {
        Map<UUID, State> states = STATES.computeIfAbsent(world, ignored -> new HashMap<>());
        if (states.size() >= MAX_STATES_PER_WORLD && !states.containsKey(actor.getUuid())) {
            states.values().stream().min(Comparator.comparingLong(value -> value.expiresAt))
                    .ifPresent(value -> states.remove(value.actorId));
        }
        State state = states.computeIfAbsent(actor.getUuid(), ignored -> new State(actor.getUuid()));
        state.stack = stack.copy();
        state.tuning = tuning;
        return state;
    }

    private static State state(ServerWorld world, UUID actorId) {
        Map<UUID, State> states = STATES.get(world);
        if (states == null) return null;
        State state = states.get(actorId);
        if (state != null && state.expiresAt < world.getTime()) {
            states.remove(actorId);
            return null;
        }
        return state;
    }

    private enum Action { THROW, MELEE }

    private static final class State {
        private final UUID actorId;
        private ItemStack stack = ItemStack.EMPTY;
        private WraithfangTuningSnapshot tuning = WraithfangTuningSnapshot.from(null);
        private long expiresAt;
        private long guardUntil;
        private long burstReadyAt;
        private long silentUntil;
        private long lastActionAt = Long.MIN_VALUE / 2;
        private Action lastAction;
        private int alternationStacks;
        private int cooldownTotal = 20;
        private int hasteRefundUsed;

        private State(UUID actorId) {
            this.actorId = actorId;
        }
    }
}
