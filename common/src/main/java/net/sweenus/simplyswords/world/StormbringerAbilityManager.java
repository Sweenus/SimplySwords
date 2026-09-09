package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.component.ParryComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StormbringerAbilityManager {
    private static final int MAX_CHAIN_TARGETS = 16;
    private static final int COMBAT_GRACE_TICKS = 100;
    private static final ThreadLocal<Boolean> SUPPRESS_CHAIN = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, OwnerState> OWNERS = new HashMap<>();
    private static final Map<MarkKey, MarkState> MARKS = new HashMap<>();

    private StormbringerAbilityManager() {
    }

    public static void tryTriggerChainLightning(ItemStack stack, LivingEntity target, ServerPlayerEntity player) {
        if (SUPPRESS_CHAIN.get() || stack == null || stack.isEmpty() || target == null || player == null
                || !stack.isOf(ItemsRegistry.STORMBRINGER.get()) || !AwakeningApi.isAbilityUnlocked(stack)) return;
        ServerWorld world = player.getServerWorld();
        long now = world.getTime();
        OwnerState state = ownerState(player);
        if (state.lastAttemptTick == now) return;
        state.lastAttemptTick = now;
        if (now < state.nextChainTick) return;

        UniqueAbilityExecution execution = StormFrostWaterMasteryCombatManager.beginPassive(
                StormFrostWaterMasteryAbilities.STORMBRINGER_CHAIN, world, stack, player, target);
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryAbilities.tuning(execution);
        observeTuning(player, tuning);
        try {
            int chargeCap = chargeCap(tuning);
            ParryComponent component = normalizeCharges(stack, chargeCap);
            int stormCharges = component.stormCharges();
            if (stormCharges <= 0) return;

            float baseDamage = HelperMethods.abilityScaledDamage("lightning", player, stack,
                    Config.uniqueEffects.stormbringer.chainLightningDamageScaling,
                    Config.uniqueEffects.stormbringer.chainLightningSpellScaling);
            baseDamage *= (float) value(tuning, s("STORMBRINGER_CHAIN_DAMAGE_MULTIPLIER"),
                    s("DAMAGE_MULTIPLIER"), 1);
            boolean fullBattery = tuning.flag(1 << 15) && stormCharges >= chargeCap;
            if (fullBattery) {
                baseDamage *= (float) value(tuning, s("STORMBRINGER_FULL_DAMAGE_MULTIPLIER"), null, 1.25);
            }
            if (tuning.flag(1 << 16)) {
                baseDamage *= (float) supercellMultiplier(stormCharges, value(tuning,
                        s("STORMBRINGER_SUPERCELL_DAMAGE_PER_CHARGE"), s("PER_STACK_MULTIPLIER"), .04));
            }

            boolean focused = tuning.flag(1 << 25);
            boolean rolling = tuning.flag(1 << 26);
            int activeTargetCap = rolling
                    ? integer(tuning, s("STORMBRINGER_ROLLING_TARGET_CAP"), s("TARGET_CAP"), 10)
                    : tuning.has(s("TARGET_CAP")) ? tuning.integer(s("TARGET_CAP"), MAX_CHAIN_TARGETS)
                    : MAX_CHAIN_TARGETS;
            int targetCount = chainTargetCount(stormCharges,
                    activeTargetCap,
                    focused ? 0 : integer(tuning, s("STORMBRINGER_FORK_EXTRA_TARGETS"), null, 0), focused);
            double range = value(tuning, s("STORMBRINGER_CHAIN_RANGE"), s("RANGE"),
                    Config.uniqueEffects.stormbringer.chainLightningRange);
            if (rolling) {
                range += value(tuning, s("STORMBRINGER_ROLLING_RANGE_BONUS"), null, 3);
            }
            List<LivingEntity> chain = ChainLightningVisualManager.chainTargets(world, player, target,
                    targetCount, range, Math.max(1, Config.uniqueEffects.stormbringer.chainSearchCap));
            if (chain.isEmpty()) return;

            Set<UUID> damaged = new HashSet<>();
            Set<UUID> killed = new HashSet<>();
            List<LivingEntity> visualHits = new ArrayList<>();
            SUPPRESS_CHAIN.set(true);
            if (focused) {
                LivingEntity primary = chain.getFirst();
                boolean preMarked = isMarked(world, player, primary);
                int hitCount = integer(tuning, s("STORMBRINGER_FOCUSED_HIT_COUNT"), s("COUNT"), 3);
                float hitDamage = baseDamage * (float) value(tuning,
                        s("STORMBRINGER_FOCUSED_HIT_DAMAGE_MULTIPLIER"), s("SECONDARY_DAMAGE_MULTIPLIER"), .55);
                if (preMarked) hitDamage *= (float) value(tuning,
                        s("STORMBRINGER_MARK_DAMAGE_MULTIPLIER"), s("OUTGOING_MULTIPLIER"), 1.15);
                for (int hit = 0; hit < hitCount && primary.isAlive(); hit++) {
                    if (damage(world, player, stack, primary, hitDamage, killed)) {
                        damaged.add(primary.getUuid());
                    }
                }
                if (damaged.contains(primary.getUuid())) {
                    visualHits.add(primary);
                    applyMark(world, player, primary, tuning);
                }
            } else {
                double rollingFactor = rolling ? value(tuning,
                        s("STORMBRINGER_ROLLING_JUMP_MULTIPLIER"), s("PER_STACK_MULTIPLIER"), .82) : 1;
                for (int index = 0; index < chain.size(); index++) {
                    LivingEntity chainTarget = chain.get(index);
                    float hitDamage = baseDamage * (float) rollingMultiplier(index, rollingFactor);
                    if (isMarked(world, player, chainTarget)) {
                        hitDamage *= (float) value(tuning,
                                s("STORMBRINGER_MARK_DAMAGE_MULTIPLIER"), s("OUTGOING_MULTIPLIER"), 1.15);
                    }
                    if (damage(world, player, stack, chainTarget, hitDamage, killed)) {
                        damaged.add(chainTarget.getUuid());
                        visualHits.add(chainTarget);
                        applyMark(world, player, chainTarget, tuning);
                    }
                }
                if (tuning.flag(1 << 22) && !chain.isEmpty()
                        && damaged.contains(chain.getLast().getUuid())) {
                    damageGroundCurrent(world, player, stack, chain, baseDamage, tuning, damaged, killed);
                }
            }
            SUPPRESS_CHAIN.set(false);
            if (damaged.isEmpty()) return;
            ChainLightningVisualManager.spawnStormbringerChainEffects(world, player, visualHits);

            boolean free = tuning.flag(1 << 10) && !fullBattery && now >= state.nextFreeChainTick;
            int consumed = resolvedChargeCost(fullBattery, free, stormCharges,
                    integer(tuning, s("STORMBRINGER_FULL_CHARGE_COST"), s("COUNT"), 2));
            if (free) {
                state.nextFreeChainTick = now + integer(tuning,
                        s("STORMBRINGER_FREE_CHAIN_LOCKOUT_TICKS"), s("LOCKOUT_TICKS"), 80);
            }
            ParryComponent updated = component;
            for (int i = 0; i < consumed; i++) updated = updated.consumeStormCharge();
            if (tuning.flag(1 << 23) && !killed.isEmpty()) {
                int refund = boundedKillRefund(killed.size(), integer(tuning,
                        s("STORMBRINGER_KILL_REFUND_CAP"), s("COUNT"), 2));
                updated = updated.gainBlockedStormCharges(refund, chargeCap);
            }
            stack.set(ComponentTypeRegistry.PARRY.get(), updated);

            if (consumed > 0 && tuning.flag(1 << 13) && damaged.contains(target.getUuid())) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                        integer(tuning, s("STORMBRINGER_RESIDUAL_SLOW_TICKS"),
                                s("STATUS_DURATION_TICKS"), 30), 0), player);
            }
            if (consumed > 0 && tuning.flag(1 << 14)) recordSpentCharges(player, tuning, consumed);

            int cooldown = tuning.has(s("STORMBRINGER_CHAIN_COOLDOWN_MULTIPLIER"))
                    ? Math.max(1, Math.round(Config.uniqueEffects.stormbringer.chainLightningCooldown
                    * (float) tuning.get(s("STORMBRINGER_CHAIN_COOLDOWN_MULTIPLIER"), 1)))
                    : tuning.integer(s("COOLDOWN_TICKS"), Config.uniqueEffects.stormbringer.chainLightningCooldown);
            if (tuning.flag(1 << 24)) {
                cooldown = resolvedChainCooldown(cooldown, damaged.size(),
                        integer(tuning, s("STORMBRINGER_CADENCE_REFUND_PER_TARGET"), s("REFUND_TICKS"), 4),
                        integer(tuning, s("STORMBRINGER_CADENCE_MIN_COOLDOWN_TICKS"),
                                s("COOLDOWN_TICKS"), 8));
            }
            state.nextChainTick = now + SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, player, cooldown);
            markCombat(player);
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, StormFrostWaterMasteryAbilities.HIT,
                    target, damaged.size(), baseDamage);
        } finally {
            SUPPRESS_CHAIN.set(false);
            UniqueAbilityApi.finish(execution, StormFrostWaterMasteryAbilities.FINISH, 0);
        }
    }

    public static float modifyOutgoingMeleeDamage(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0 || target == null || source == null || !source.isIn(DamageTypeTags.IS_PLAYER_ATTACK)
                || !(source.getAttacker() instanceof ServerPlayerEntity player) || SUPPRESS_CHAIN.get()) return amount;
        ItemStack stack = source.getWeaponStack();
        if (stack == null || !stack.isOf(ItemsRegistry.STORMBRINGER.get())) stack = player.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.STORMBRINGER.get()) || !AwakeningApi.isAbilityUnlocked(stack)) return amount;
        StormFrostWaterMasteryTuning tuning = resolveChainTuning(player, stack, target);
        int charges = Math.min(stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT).stormCharges(),
                chargeCap(tuning));
        if (!tuning.flag(1 << 11) || charges < integer(tuning,
                s("STORMBRINGER_MELEE_CHARGE_THRESHOLD"), s("COUNT"), 5)) return amount;
        markCombat(player);
        return amount * (float) value(tuning, s("STORMBRINGER_MELEE_DAMAGE_MULTIPLIER"),
                s("OUTGOING_MULTIPLIER"), 1.08);
    }

    public static ParryComponent gainStormCharges(ServerPlayerEntity player, ItemStack stack,
                                                   StormFrostWaterMasteryTuning tuning, int amount, boolean perfect) {
        int cap = chargeCap(tuning);
        ParryComponent component = normalizeCharges(stack, cap);
        int resolved = scaledChargeGain(amount, value(tuning,
                s("STORMBRINGER_CHARGE_GAIN_MULTIPLIER"), null, 1));
        OwnerState state = ownerState(player);
        observeTuning(player, tuning);
        long now = player.getServerWorld().getTime();
        if (resolved > 0 && component.stormCharges() >= cap && tuning.flag(1 << 12)
                && now >= state.nextOverflowTick) {
            float absorption = (float) value(tuning, s("STORMBRINGER_OVERFLOW_ABSORPTION"),
                    s("ABSORPTION"), 2);
            int duration = integer(tuning, s("STORMBRINGER_OVERFLOW_DURATION_TICKS"),
                    s("STATUS_DURATION_TICKS"), 60);
            MasteryAbsorptionTracker.grant(player, "stormbringer/overflow", absorption, duration, absorption);
            state.nextOverflowTick = now + integer(tuning,
                    s("STORMBRINGER_OVERFLOW_LOCKOUT_TICKS"), s("LOCKOUT_TICKS"), 40);
        }
        ParryComponent updated = perfect
                ? component.gainStormCharges(resolved, cap)
                : component.gainBlockedStormCharges(resolved, cap);
        stack.set(ComponentTypeRegistry.PARRY.get(), updated);
        markCombat(player);
        return updated;
    }

    public static void onDamageApplied(LivingEntity target) {
        if (!(target instanceof ServerPlayerEntity player)) return;
        if (player.getMainHandStack().isOf(ItemsRegistry.STORMBRINGER.get())
                || player.getOffHandStack().isOf(ItemsRegistry.STORMBRINGER.get())) markCombat(player);
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        MasteryAbsorptionTracker.tick(player);
        long now = player.getServerWorld().getTime();
        if (player.age % 20 == 0) {
            RegistryKey<World> key = player.getWorld().getRegistryKey();
            MARKS.entrySet().removeIf(entry -> entry.getValue().world.equals(key)
                    && now >= entry.getValue().expiresAt);
        }
        ItemStack stack = player.getMainHandStack().isOf(ItemsRegistry.STORMBRINGER.get())
                ? player.getMainHandStack() : player.getOffHandStack();
        if (!stack.isOf(ItemsRegistry.STORMBRINGER.get()) || !AwakeningApi.isAbilityUnlocked(stack)) return;
        ParryComponent component = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT);
        if (component.stormCharges() <= 0) return;
        OwnerState state = ownerState(player);
        if (state.tuning == null || now >= state.nextTuningRefreshTick) {
            state.tuning = resolveChainTuning(player, stack, null);
            state.nextTuningRefreshTick = now + 20;
        }
        StormFrostWaterMasteryTuning tuning = state.tuning;
        int cap = chargeCap(tuning);
        component = normalizeCharges(stack, cap);
        if (!tuning.flag(1 << 16) || component.stormCharges() <= 0) return;
        int interval = integer(tuning, s("STORMBRINGER_SUPERCELL_DECAY_TICKS"),
                s("INTERVAL_TICKS"), 40);
        if (!shouldDecay(now, state.lastCombatTick, COMBAT_GRACE_TICKS)) {
            state.nextDecayTick = now + interval;
            return;
        }
        if (state.nextDecayTick == Long.MIN_VALUE) state.nextDecayTick = now + interval;
        while (component.stormCharges() > 0 && now >= state.nextDecayTick) {
            component = component.consumeStormCharge();
            state.nextDecayTick += interval;
        }
        stack.set(ComponentTypeRegistry.PARRY.get(), component);
    }

    public static void observeTuning(ServerPlayerEntity player, StormFrostWaterMasteryTuning tuning) {
        OwnerState state = ownerState(player);
        state.tuning = tuning;
        state.nextTuningRefreshTick = player.getServerWorld().getTime() + 20;
    }

    public static void markCombat(ServerPlayerEntity player) {
        OwnerState state = ownerState(player);
        long now = player.getServerWorld().getTime();
        state.lastCombatTick = now;
        int interval = state.tuning == null ? 40 : integer(state.tuning,
                s("STORMBRINGER_SUPERCELL_DECAY_TICKS"), s("INTERVAL_TICKS"), 40);
        state.nextDecayTick = now + interval;
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        RegistryKey<World> key = world.getRegistryKey();
        OWNERS.entrySet().removeIf(entry -> entry.getValue().world.equals(key));
        MARKS.entrySet().removeIf(entry -> entry.getValue().world.equals(key));
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        UUID id = actor.getUuid();
        OWNERS.remove(id);
        MARKS.entrySet().removeIf(entry -> entry.getKey().owner.equals(id) || entry.getKey().target.equals(id));
        MasteryAbsorptionTracker.clear(actor);
    }

    public static void clearAll() {
        OWNERS.clear();
        MARKS.clear();
        SUPPRESS_CHAIN.remove();
    }

    static int scaledChargeGain(int amount, double multiplier) {
        return Math.max(0, (int) Math.ceil(Math.max(0, amount) * Math.max(0, multiplier)));
    }

    static int chainTargetCount(int charges, int cap, int forkExtras, boolean focused) {
        if (focused) return charges > 0 ? 1 : 0;
        return Math.clamp(Math.max(0, charges) + Math.max(0, forkExtras), 0,
                Math.min(MAX_CHAIN_TARGETS, Math.max(1, cap)));
    }

    static int resolvedChainCooldown(int base, int distinctHits, int perTarget, int minimum) {
        return Math.max(Math.max(1, minimum), Math.max(1, base) - Math.max(0, distinctHits) * Math.max(0, perTarget));
    }

    static double rollingMultiplier(int jumpIndex, double factor) {
        return Math.pow(Math.max(0, factor), Math.max(0, jumpIndex));
    }

    static int resolvedChargeCost(boolean fullBattery, boolean freeAvailable, int charges, int fullCost) {
        if (fullBattery) return Math.min(Math.max(0, charges), Math.max(0, fullCost));
        return freeAvailable || charges <= 0 ? 0 : 1;
    }

    static int boundedKillRefund(int kills, int cap) {
        return Math.min(Math.max(0, kills), Math.max(0, cap));
    }

    static double supercellMultiplier(int charges, double perCharge) {
        return 1 + Math.max(0, charges) * Math.max(0, perCharge);
    }

    static boolean shouldDecay(long now, long lastCombat, int graceTicks) {
        return lastCombat == Long.MIN_VALUE || now - lastCombat > Math.max(0, graceTicks);
    }

    private static void damageGroundCurrent(ServerWorld world, ServerPlayerEntity player, ItemStack stack,
                                            List<LivingEntity> chain, float baseDamage, StormFrostWaterMasteryTuning tuning,
                                            Set<UUID> damaged, Set<UUID> killed) {
        LivingEntity origin = chain.getLast();
        double radius = value(tuning, s("STORMBRINGER_BURST_RADIUS"), s("RADIUS"), 2);
        int cap = integer(tuning, s("STORMBRINGER_BURST_TARGET_CAP"), s("TARGET_CAP"), 6);
        Set<UUID> excluded = chain.stream().map(LivingEntity::getUuid)
                .collect(java.util.stream.Collectors.toSet());
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                        new Box(origin.getPos(), origin.getPos()).expand(radius), candidate ->
                                candidate != player && candidate.isAlive()
                                        && !excluded.contains(candidate.getUuid())
                                        && EntityPredicates.VALID_LIVING_ENTITY.test(candidate)
                                        && HelperMethods.checkAbilityTarget(candidate, player))
                .stream()
                .sorted(Comparator.comparing(LivingEntity::getUuid))
                .limit(Math.max(1, Config.uniqueEffects.stormbringer.chainSearchCap))
                .sorted(Comparator.comparingDouble((LivingEntity candidate) -> candidate.squaredDistanceTo(origin))
                        .thenComparing(LivingEntity::getUuid))
                .limit(cap)
                .toList();
        float damage = baseDamage * (float) value(tuning,
                s("STORMBRINGER_BURST_DAMAGE_MULTIPLIER"), s("SECONDARY_DAMAGE_MULTIPLIER"), .25);
        List<LivingEntity> visualHits = new ArrayList<>();
        for (LivingEntity candidate : targets) {
            if (damage(world, player, stack, candidate, damage, killed)) {
                damaged.add(candidate.getUuid());
                visualHits.add(candidate);
                applyMark(world, player, candidate, tuning);
            }
        }
        ChainLightningVisualManager.spawnStormbringerBurstEffects(world, origin, visualHits);
    }

    private static boolean damage(ServerWorld world, ServerPlayerEntity player, ItemStack stack,
                                  LivingEntity target, float damage, Set<UUID> killed) {
        boolean alive = target.isAlive();
        boolean result = ChainLightningVisualManager.damageBoltTarget(world, player, stack, target, damage);
        if (result && alive && !target.isAlive()) killed.add(target.getUuid());
        return result;
    }

    private static void applyMark(ServerWorld world, ServerPlayerEntity owner, LivingEntity target,
                                  StormFrostWaterMasteryTuning tuning) {
        if (!tuning.flag(1 << 21) || !target.isAlive()) return;
        int duration = integer(tuning, s("STORMBRINGER_MARK_DURATION_TICKS"),
                s("STATUS_DURATION_TICKS"), 80);
        MARKS.put(new MarkKey(owner.getUuid(), target.getUuid()),
                new MarkState(world.getRegistryKey(), world.getTime() + duration));
    }

    private static boolean isMarked(ServerWorld world, ServerPlayerEntity owner, LivingEntity target) {
        MarkKey key = new MarkKey(owner.getUuid(), target.getUuid());
        MarkState mark = MARKS.get(key);
        if (mark == null) return false;
        if (!mark.world.equals(world.getRegistryKey()) || world.getTime() >= mark.expiresAt) {
            MARKS.remove(key);
            return false;
        }
        return true;
    }

    private static void recordSpentCharges(ServerPlayerEntity player, StormFrostWaterMasteryTuning tuning, int spent) {
        OwnerState state = ownerState(player);
        long now = player.getServerWorld().getTime();
        int window = integer(tuning, s("STORMBRINGER_RHYTHM_WINDOW_TICKS"), s("LOCKOUT_TICKS"), 100);
        int required = integer(tuning, s("STORMBRINGER_RHYTHM_SPEND_COUNT"), s("COUNT"), 3);
        while (!state.spentAt.isEmpty() && state.spentAt.peekFirst() < now - window) state.spentAt.removeFirst();
        for (int i = 0; i < spent; i++) state.spentAt.addLast(now);
        while (required > 0 && state.spentAt.size() >= required) {
            for (int i = 0; i < required; i++) state.spentAt.removeFirst();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,
                    integer(tuning, s("STORMBRINGER_RHYTHM_HASTE_TICKS"),
                            s("STATUS_DURATION_TICKS"), 80), 0), player);
        }
    }

    private static StormFrostWaterMasteryTuning resolveChainTuning(ServerPlayerEntity player, ItemStack stack,
                                                           LivingEntity target) {
        UniqueAbilityExecution execution = StormFrostWaterMasteryCombatManager.beginPassive(
                StormFrostWaterMasteryAbilities.STORMBRINGER_CHAIN, player.getServerWorld(), stack, player, target);
        try {
            StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryAbilities.tuning(execution);
            observeTuning(player, tuning);
            return tuning;
        } finally {
            UniqueAbilityApi.finish(execution, StormFrostWaterMasteryAbilities.FINISH, 0);
        }
    }

    private static OwnerState ownerState(ServerPlayerEntity player) {
        RegistryKey<World> world = player.getWorld().getRegistryKey();
        OwnerState state = OWNERS.computeIfAbsent(player.getUuid(), ignored -> new OwnerState(world));
        if (!state.world.equals(world)) {
            UUID owner = player.getUuid();
            MARKS.entrySet().removeIf(entry -> entry.getKey().owner.equals(owner));
            state.world = world;
            state.nextChainTick = Long.MIN_VALUE;
            state.nextFreeChainTick = Long.MIN_VALUE;
            state.nextOverflowTick = Long.MIN_VALUE;
            state.nextDecayTick = Long.MIN_VALUE;
            state.lastAttemptTick = Long.MIN_VALUE;
            state.spentAt.clear();
        }
        return state;
    }

    private static ParryComponent normalizeCharges(ItemStack stack, int cap) {
        ParryComponent component = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT);
        ParryComponent normalized = normalizedChargeState(component, cap);
        if (normalized.equals(component)) return component;
        stack.set(ComponentTypeRegistry.PARRY.get(), normalized);
        return normalized;
    }

    static ParryComponent normalizedChargeState(ParryComponent component, int cap) {
        return component.withStormChargeCapacity(cap);
    }

    private static int chargeCap(StormFrostWaterMasteryTuning tuning) {
        return integer(tuning, s("STORMBRINGER_CHARGE_CAP"), s("CHARGE_CAP"),
                Math.max(1, Config.uniqueEffects.stormbringer.maxStormCharges));
    }

    private static double value(StormFrostWaterMasteryTuning tuning, StormFrostWaterMasteryTuning.Setting scoped,
                                StormFrostWaterMasteryTuning.Setting generic, double fallback) {
        if (tuning.has(scoped)) return tuning.get(scoped, fallback);
        return generic != null ? tuning.get(generic, fallback) : fallback;
    }

    private static int integer(StormFrostWaterMasteryTuning tuning, StormFrostWaterMasteryTuning.Setting scoped,
                               StormFrostWaterMasteryTuning.Setting generic, int fallback) {
        return (int) Math.round(value(tuning, scoped, generic, fallback));
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }

    private record MarkKey(UUID owner, UUID target) {
    }

    private record MarkState(RegistryKey<World> world, long expiresAt) {
    }

    private static final class OwnerState {
        private RegistryKey<World> world;
        private long nextChainTick = Long.MIN_VALUE;
        private long nextFreeChainTick = Long.MIN_VALUE;
        private long nextOverflowTick = Long.MIN_VALUE;
        private long nextDecayTick = Long.MIN_VALUE;
        private long nextTuningRefreshTick = Long.MIN_VALUE;
        private long lastCombatTick = Long.MIN_VALUE;
        private long lastAttemptTick = Long.MIN_VALUE;
        private StormFrostWaterMasteryTuning tuning;
        private final Deque<Long> spentAt = new ArrayDeque<>();

        private OwnerState(RegistryKey<World> world) {
            this.world = world;
        }
    }
}
