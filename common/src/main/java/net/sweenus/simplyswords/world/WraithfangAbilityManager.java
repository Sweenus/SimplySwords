package net.sweenus.simplyswords.world;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.ability.*;
import net.sweenus.simplyswords.entity.WraithfangEntity;
import net.sweenus.simplyswords.item.custom.WraithfangSwordItem;
import net.sweenus.simplyswords.mixin.ItemCooldownEntryAccessor;
import net.sweenus.simplyswords.mixin.ItemCooldownManagerAccessor;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import java.util.*;
import static net.sweenus.simplyswords.world.WraithfangTuningSnapshot.*;

public final class WraithfangAbilityManager {
    private static final Map<ServerWorld, Map<UUID, State>> STATES = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, UUID>> ACTIVE = new HashMap<>();
    private static final ThreadLocal<Attack> ATTACK = new ThreadLocal<>();

    private WraithfangAbilityManager() { }

    public static UUID weaponId(ItemStack stack) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!data.containsUuid("wraithfang_weapon")) {
            data.putUuid("wraithfang_weapon", UUID.randomUUID());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        }
        return data.getUuid("wraithfang_weapon");
    }

    private static State state(ServerWorld world, LivingEntity actor) {
        return STATES.getOrDefault(world, Map.of()).get(actor.getUuid());
    }

    private static State obtain(ServerWorld world, LivingEntity actor, ItemStack stack) {
        Map<UUID, State> states = STATES.computeIfAbsent(world, ignored -> new HashMap<>());
        State state = states.computeIfAbsent(actor.getUuid(), ignored -> new State());
        UUID id = weaponId(stack);
        if (!id.equals(state.weaponId)) {
            if (state.followUntil > 0) recover(actor, state);
            int budget = state.refundUsed;
            state = new State();
            state.refundUsed = budget;
            states.put(actor.getUuid(), state);
            state.weaponId = id;
        }
        state.stack = stack.copy();
        state.expires = world.getTime() + 2400;
        return state;
    }

    public static boolean hasActiveThrow(ServerWorld world, LivingEntity actor) {
        UUID projectile = ACTIVE.getOrDefault(world, Map.of()).get(actor.getUuid());
        return projectile != null && world.getEntity(projectile) instanceof WraithfangEntity;
    }

    public static boolean canFollowUp(ServerWorld world, LivingEntity actor, ItemStack stack) {
        State state = state(world, actor);
        return state != null && state.followUntil > world.getTime() && state.weaponId.equals(weaponId(stack));
    }

    public static double recordThrow(ServerWorld world, LivingEntity actor, ItemStack stack, WraithfangTuningSnapshot tuning) {
        boolean follow = canFollowUp(world, actor, stack);
        State state = obtain(world, actor, stack);
        state.followUntil = 0;
        state.second = follow;
        state.tuning = tuning;
        state.recovery = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, actor, tuning.cooldownTicks());
        double multiplier = alternation(state, tuning, true, world.getTime(), true);
        if (state.readyUntil > world.getTime()) multiplier *= 1.25;
        state.readyUntil = 0;
        return multiplier * (follow ? .75 : 1);
    }

    public static void launched(ServerWorld world, LivingEntity actor, WraithfangEntity projectile) {
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), projectile.getUuid());
        State state = state(world, actor);
        if (state == null || state.weaponId == null) {
            state = obtain(world, actor, projectile.stack);
            state.tuning = projectile.masteryTuning();
            state.recovery = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(state.stack, actor, state.tuning.cooldownTicks());
        }
        state.projectile = projectile.getUuid();
    }

    public static void onGroundPickup(ServerWorld world, LivingEntity actor, ItemStack stack, WraithfangTuningSnapshot tuning) {
        State state = obtain(world, actor, stack);
        state.projectile = null;
        Map<UUID, UUID> active = ACTIVE.get(world);
        if (active != null) active.remove(actor.getUuid());
        if (tuning.hasMode(ENDLESS)) {
            state.recovery = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, actor, tuning.cooldownTicks());
            recover(actor, state);
        }
    }

    public static boolean isSecond(ServerWorld world, LivingEntity actor) {
        State state = state(world, actor);
        return state != null && state.second;
    }

    public static void onReturn(ServerWorld world, LivingEntity actor, ItemStack stack,
                                WraithfangTuningSnapshot tuning, float baseDamage, boolean second) {
        Map<UUID, UUID> active = ACTIVE.get(world);
        if (active != null) active.remove(actor.getUuid());
        State state = obtain(world, actor, stack);
        state.projectile = null;
        state.tuning = tuning;
        state.baseDamage = baseDamage;
        grantHaste(actor, tuning.hasteDurationTicks() + (tuning.hasMode(RETURN_BEAT) ? 60 : 0), tuning.hasteAmplifier());
        state.catchUntil = world.getTime() + 80;
        if (tuning.hasMode(FLURRY)) {
            state.charges = 3;
            state.chargesUntil = world.getTime() + 160;
            message(actor, "message.simplyswords.wraithfang.soulflurry", 3);
        }
        if (tuning.hasMode(ENDLESS) && actor instanceof ServerPlayerEntity) {
            state.recovery = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, actor, tuning.cooldownTicks());
            if (second) recover(actor, state);
            else {
                state.followUntil = world.getTime() + 40;
                message(actor, "message.simplyswords.wraithfang.endless");
            }
        }
    }

    public static void onPursuitComplete(ServerWorld world, LivingEntity actor, ItemStack stack, WraithfangTuningSnapshot tuning) {
        grantHaste(actor, tuning.hasteDurationTicks(), tuning.hasteAmplifier());
    }

    public static void onThrownKill(ServerWorld world, LivingEntity actor, ItemStack stack, WraithfangTuningSnapshot tuning) {
        grantHaste(actor, tuning.killHasteDurationTicks(), tuning.killHasteAmplifier());
        SimplySwordsAPI.reduceWeaponCooldown(actor, stack, Math.max(1, tuning.cooldownTicks()), tuning.killCooldownRefundTicks());
    }

    public static boolean tryBurst(ServerWorld world, LivingEntity actor, int lockoutTicks) {
        State state = STATES.computeIfAbsent(world, ignored -> new HashMap<>()).computeIfAbsent(actor.getUuid(), ignored -> new State());
        if (state.burstUntil > world.getTime()) return false;
        state.burstUntil = world.getTime() + lockoutTicks;
        state.expires = world.getTime() + 2400;
        return true;
    }

    public static void safeLanding(ServerWorld world, LivingEntity actor) {
        State state = STATES.computeIfAbsent(world, ignored -> new HashMap<>()).computeIfAbsent(actor.getUuid(), ignored -> new State());
        state.safeUntil = world.getTime() + 40;
        state.expires = world.getTime() + 2400;
        actor.fallDistance = 0;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (target.getWorld() instanceof ServerWorld world && source.isIn(DamageTypeTags.IS_FALL)) {
            State state = state(world, target);
            if (state != null && state.safeUntil > world.getTime()) return 0;
        }
        return amount;
    }

    public static void markDestination(ServerWorld world, LivingEntity owner, LivingEntity target, ItemStack stack) {
        State state = obtain(world, owner, stack);
        state.destination = target.getUuid();
        state.destinationUntil = world.getTime() + 80;
    }

    public static float hauntedMultiplier(ServerWorld world, LivingEntity owner, LivingEntity target) {
        State state = state(world, owner);
        return state != null && state.haunted.getOrDefault(target.getUuid(), 0L) > world.getTime() ? 1.15F : 1;
    }

    public static void markHaunted(ServerWorld world, LivingEntity owner, LivingEntity target) {
        State state = STATES.computeIfAbsent(world, ignored -> new HashMap<>()).computeIfAbsent(owner.getUuid(), ignored -> new State());
        state.haunted.put(target.getUuid(), world.getTime() + 80);
        state.expires = world.getTime() + 2400;
    }

    public static void beginAttack(LivingEntity actor) {
        Attack current = ATTACK.get();
        if (current != null && current.actor == actor) current.depth++;
        else ATTACK.set(new Attack(actor));
    }

    public static void endAttack() {
        Attack current = ATTACK.get();
        if (current != null && --current.depth == 0) ATTACK.remove();
    }

    private static ItemStack meleeStack(DamageSource source) {
        if (!source.isIn(DamageTypeTags.IS_PLAYER_ATTACK) || source.getSource() != source.getAttacker()
                || SimplySwordsAPI.getDelegatedWeaponHitContext() != null) return ItemStack.EMPTY;
        ItemStack stack = source.getWeaponStack();
        return stack != null && stack.isOf(ItemsRegistry.WRAITHFANG.get()) ? stack : ItemStack.EMPTY;
    }

    private static WraithfangTuningSnapshot meleeTuning(ServerWorld world, LivingEntity actor, LivingEntity target, ItemStack stack) {
        UniqueAbilityExecution outer = UniqueAbilityApi.takeStartedExecution();
        try {
            UniqueAbilityExecution execution = UniqueAbilityApi.begin(AbyssalSpectralMasteryAbilities.WRAITHFANG_MELEE,
                    UniqueAbilityContext.passive(world, stack, actor, target, null),
                    builder -> builder.set(AbyssalSpectralMasteryAbilities.TUNING, WraithfangSwordItem.baseTuning(actor, 1.5, 1)));
            WraithfangTuningSnapshot tuning = WraithfangTuningSnapshot.from(execution);
            UniqueAbilityApi.start(execution);
            UniqueAbilityApi.finish(execution, execution.definition().id(), 0);
            return tuning;
        } finally {
            UniqueAbilityApi.publishStartedExecution(outer);
        }
    }

    public static float modifyOutgoingDamage(LivingEntity target, DamageSource source, float amount) {
        ItemStack stack = meleeStack(source);
        if (stack.isEmpty() || !(source.getAttacker() instanceof LivingEntity actor)
                || !(target.getWorld() instanceof ServerWorld world)) return amount;
        State state = obtain(world, actor, stack);
        Attack attack = ATTACK.get();
        WraithfangTuningSnapshot tuning = attack != null && attack.tuning != null ? attack.tuning : meleeTuning(world, actor, target, stack);
        double multiplier;
        if (attack != null && attack.tuning != null) multiplier = attack.multiplier;
        else {
            multiplier = alternation(state, tuning, false, world.getTime(), false);
            if (actor.hasStatusEffect(StatusEffects.HASTE)) multiplier *= 1 + tuning.meleeHasteBonus();
            if (attack != null) { attack.tuning = tuning; attack.multiplier = multiplier; }
        }
        if (tuning.hasMode(DESTINATION) && state.destinationUntil > world.getTime()
                && target.getUuid().equals(state.destination)) multiplier *= 1.1;
        return amount * (float) multiplier;
    }

    public static void onMeleeDamageApplied(LivingEntity target, DamageSource source) {
        ItemStack stack = meleeStack(source);
        if (stack.isEmpty() || !(source.getAttacker() instanceof LivingEntity actor)
                || !(target.getWorld() instanceof ServerWorld world)) return;
        Attack attack = ATTACK.get();
        if (attack != null && attack.applied) return;
        if (attack != null) attack.applied = true;
        State state = obtain(world, actor, stack);
        WraithfangTuningSnapshot tuning = attack != null && attack.tuning != null ? attack.tuning : meleeTuning(world, actor, target, stack);
        alternation(state, tuning, false, world.getTime(), true);
        if (tuning.hasMode(READIED) && state.catchUntil > world.getTime()) {
            state.catchUntil = 0;
            state.readyUntil = world.getTime() + 100;
        }
        if (tuning.hasMode(FLURRY) && state.charges > 0 && state.chargesUntil > world.getTime()) {
            state.charges--;
            WraithfangEntity echo = new WraithfangEntity(world, actor, stack.copy());
            echo.configureEcho(state.baseDamage * .6F);
            echo.setPosition(actor.getEyePos().add(0, -.25, 0));
            var direction = target.getBoundingBox().getCenter().subtract(echo.getPos()).normalize();
            echo.setVelocity(direction.multiply(1.8));
            world.spawnEntity(echo);
            message(actor, "message.simplyswords.wraithfang.soulflurry", state.charges);
        }
    }

    public static void onTargetDeath(LivingEntity target, DamageSource source) {
        ItemStack stack = meleeStack(source);
        if (stack.isEmpty() || !(source.getAttacker() instanceof LivingEntity actor)
                || !(target.getWorld() instanceof ServerWorld world) || !actor.hasStatusEffect(StatusEffects.HASTE)) return;
        WraithfangTuningSnapshot tuning = meleeTuning(world, actor, target, stack);
        if (!tuning.hasMode(RESTLESS)) return;
        State state = obtain(world, actor, stack);
        int refund = Math.min(tuning.hasteKillRefundTicks(), Math.max(0, tuning.hasteKillRefundCapTicks() - state.refundUsed));
        int actual = Math.min(refund, remaining(actor, stack));
        state.refundUsed += actual;
        SimplySwordsAPI.reduceWeaponCooldown(actor, stack, 1, actual);
    }

    private static double alternation(State state, WraithfangTuningSnapshot tuning, boolean thrown, long now, boolean commit) {
        if (!tuning.hasMode(CADENCE)) return 1;
        int stacks = nextAlternationStacks(state.alternations,
                now - state.lastAction <= tuning.alternationWindowTicks(), state.lastThrow != thrown);
        if (commit) { state.alternations = stacks; state.lastAction = now; state.lastThrow = thrown; }
        return 1 + Math.min(tuning.alternationBonusCap(), stacks * tuning.alternationBonusPerStack());
    }

    static int nextAlternationStacks(int current, boolean withinWindow, boolean alternating) {
        if (!withinWindow || !alternating) return 0;
        return Math.min(4, current + 1);
    }

    private static void grantHaste(LivingEntity actor, int duration, int amplifier) {
        if (duration > 0) actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, amplifier, false, false, true));
    }

    private static int remaining(LivingEntity actor, ItemStack stack) {
        if (!(actor instanceof ServerPlayerEntity player)) return 0;
        ItemCooldownManagerAccessor manager = (ItemCooldownManagerAccessor) player.getItemCooldownManager();
        Object entry = manager.simplyswords$getEntries().get(stack.getItem());
        return entry instanceof ItemCooldownEntryAccessor cooldown ? Math.max(0, cooldown.simplyswords$getEndTick() - manager.simplyswords$getTick()) : 0;
    }

    private static void recover(LivingEntity actor, State state) {
        if (actor instanceof ServerPlayerEntity player && !state.stack.isEmpty()) {
            player.getItemCooldownManager().set(state.stack.getItem(), Math.max(remaining(actor, state.stack), state.recovery));
        }
        state.followUntil = 0;
    }

    public static void message(LivingEntity actor, String message, Object... args) {
        if (actor instanceof ServerPlayerEntity player) player.sendMessage(Text.translatable(message, args), true);
    }

    public static boolean hasActive(ServerWorld world) { return !STATES.getOrDefault(world, Map.of()).isEmpty() || ACTIVE.containsKey(world); }

    public static void tick(ServerWorld world) {
        Map<UUID, UUID> active = ACTIVE.get(world);
        if (active != null) {
            active.entrySet().removeIf(entry -> !(world.getEntity(entry.getKey()) instanceof LivingEntity owner)
                    || !owner.isAlive() || !(world.getEntity(entry.getValue()) instanceof WraithfangEntity));
            if (active.isEmpty()) ACTIVE.remove(world);
        }
        Map<UUID, State> states = STATES.get(world);
        if (states == null) return;
        states.entrySet().removeIf(entry -> {
            if (!(world.getEntity(entry.getKey()) instanceof LivingEntity actor) || !actor.isAlive()) return true;
            State state = entry.getValue();
            state.haunted.values().removeIf(deadline -> deadline <= world.getTime());
            if (!actor.hasStatusEffect(StatusEffects.HASTE)) state.refundUsed = 0;
            if (state.followUntil > 0 && state.followUntil <= world.getTime()) recover(actor, state);
            if (state.projectile != null && world.getEntity(state.projectile) == null) {
                if (state.tuning.hasMode(ENDLESS)) recover(actor, state);
                state.projectile = null;
            }
            return state.expires < world.getTime() && state.projectile == null
                    && !(state.refundUsed > 0 && actor.hasStatusEffect(StatusEffects.HASTE));
        });
        if (states.isEmpty()) STATES.remove(world);
    }

    public static void clear(ServerWorld world) { STATES.remove(world); ACTIVE.remove(world); }
    public static void clearAll() { STATES.clear(); ACTIVE.clear(); ATTACK.remove(); }

    private static final class Attack {
        final LivingEntity actor;
        int depth = 1;
        boolean applied;
        WraithfangTuningSnapshot tuning;
        double multiplier;
        Attack(LivingEntity actor) { this.actor = actor; }
    }

    private static final class State {
        final Map<UUID, Long> haunted = new HashMap<>();
        UUID weaponId, projectile, destination;
        ItemStack stack = ItemStack.EMPTY;
        WraithfangTuningSnapshot tuning = WraithfangTuningSnapshot.from(null);
        long expires, burstUntil, safeUntil, followUntil, chargesUntil, catchUntil, readyUntil, destinationUntil;
        long lastAction = Long.MIN_VALUE / 2;
        boolean lastThrow, second;
        int alternations, charges, refundUsed, recovery;
        float baseDamage;
    }
}
