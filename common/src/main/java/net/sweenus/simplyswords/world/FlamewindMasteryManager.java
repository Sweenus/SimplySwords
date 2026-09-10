package net.sweenus.simplyswords.world;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.FlameSeedEffect;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.minecraft.predicate.entity.EntityPredicates;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class FlamewindMasteryManager {
    private static final Identifier DRAFT_ID = Identifier.of("simplyswords", "flamewind_furnace_draft");
    private static final Map<ServerWorld, Map<UUID, SeedSnapshot>> SEEDS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, OwnerState>> OWNERS = new HashMap<>();

    private FlamewindMasteryManager() {
    }

    public static boolean activate(WeaponAbilityContext context, LivingEntity target) {
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginActive(FireForgeMasteryAbilities.FLAMEWIND_SEED,
                context, Config.uniqueEffects.flamewind.cooldown);
        UniqueAbilityApi.start(execution);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        if (tuning.flag(1 << 21) && hasOwned(context.actor())) {
            OwnerState owner = owner(context.world(), context.actor());
            long now = context.world().getTime();
            if (now < owner.recastReadyAt) {
                UniqueAbilityApi.cancel(execution);
                return false;
            }
            owner.recastReadyAt = now + tuning.integer(s("FLAMEWIND_RECAST_LOCKOUT_TICKS"), 0);
            owner.releaseKills = 0;
            owner.manualRelease = true;
            int affected;
            try {
                affected = FlameSeedEffect.detonateOwned(context.world(), context.actor(),
                        tuning.flag(1 << 25) ? 3 : 64);
            } finally {
                owner.manualRelease = false;
            }
            applyReleaseReset(execution, tuning, owner);
            UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, affected);
            return affected > 0;
        }
        if (target == null || !HelperMethods.checkAbilityTarget(target, context.actor())) {
            target = findSeedTarget(context.world(), context.actor(),
                    tuning.get(s("FLAMEWIND_SEED_RANGE"), 10)
                            + tuning.get(s("FLAMEWIND_RETARGET_RANGE"), 0));
        }
        if (target == null || !HelperMethods.checkAbilityTarget(target, context.actor())) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
        owner(context.world(), context.actor()).castRefunded = 0;
        int duration = tuning.integer(s("DURATION_TICKS"), 101);
        int spread = tuning.integer(s("SPREAD_CAP"), Config.uniqueEffects.flamewind.spreadCap);
        SimplySwordsStatusEffectInstance effect = new SimplySwordsStatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.FLAMESEED), duration, 0, false, false, true);
        effect.setSourceEntity(context.actor());
        effect.setAdditionalData(spread);
        target.addStatusEffect(effect);
        seeds(context.world()).put(target.getUuid(), new SeedSnapshot(context.actor().getUuid(), context.stack().copy(),
                tuning, execution, 0));
        FlamewindVisualManager.refreshSeed(context.world(), target);
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, FireForgeMasteryAbilities.HIT, target, 1, 0);
        return true;
    }

    public static SeedSnapshot snapshot(LivingEntity target) {
        if (target == null || !(target.getWorld() instanceof ServerWorld world)) return null;
        Map<UUID, SeedSnapshot> seeds = SEEDS.get(world);
        return seeds == null ? null : seeds.get(target.getUuid());
    }

    public static void inherit(LivingEntity source, LivingEntity target) {
        inherit(snapshot(source), target);
    }

    public static void inherit(SeedSnapshot snapshot, LivingEntity target) {
        if (snapshot != null && target.getWorld() instanceof ServerWorld world) {
            seeds(world).put(target.getUuid(), snapshot.nextGeneration());
        }
    }

    public static void remove(UUID targetId) {
        if (targetId == null) return;
        for (ServerWorld world : List.copyOf(SEEDS.keySet())) remove(world, targetId);
    }

    public static void remove(ServerWorld world, UUID targetId) {
        if (world == null || targetId == null) return;
        Map<UUID, SeedSnapshot> seeds = SEEDS.get(world);
        if (seeds == null) return;
        SeedSnapshot removed = seeds.remove(targetId);
        if (seeds.isEmpty()) SEEDS.remove(world);
        if (removed != null && world.getEntity(removed.ownerId()) instanceof LivingEntity owner) {
            refreshDraft(world, owner, FireForgeMasteryTuning.EMPTY);
        }
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 20 != 0) return;
        Map<UUID, OwnerState> owners = OWNERS.get(world);
        if (owners == null) return;
        for (UUID id : List.copyOf(owners.keySet())) {
            if (world.getEntity(id) instanceof LivingEntity owner) {
                refreshDraft(world, owner, FireForgeMasteryTuning.EMPTY);
            } else {
                owners.remove(id);
            }
        }
        if (owners.isEmpty()) OWNERS.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        removeDraft(actor);
        OWNERS.values().forEach(owners -> owners.remove(actor.getUuid()));
        OWNERS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static boolean hasOwned(LivingEntity actor) {
        return actor != null && actor.getWorld() instanceof ServerWorld world
                && !ownedSeeds(world, actor, 1).isEmpty();
    }

    public static List<LivingEntity> ownedSeeds(ServerWorld world, LivingEntity owner, int limit) {
        if (world == null || owner == null || limit <= 0) return List.of();
        Map<UUID, SeedSnapshot> seeds = SEEDS.get(world);
        if (seeds == null || seeds.isEmpty()) return List.of();
        pruneSeeds(seeds, targetId -> world.getEntity(targetId) instanceof LivingEntity target
                && target.isAlive()
                && target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED)));
        if (seeds.isEmpty()) {
            SEEDS.remove(world);
            return List.of();
        }
        return seeds.entrySet().stream()
                .filter(entry -> entry.getValue().ownerId.equals(owner.getUuid()))
                .map(entry -> world.getEntity(entry.getKey()))
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .sorted(java.util.Comparator.comparingDouble((LivingEntity target) -> owner.squaredDistanceTo(target))
                        .thenComparing(target -> target.getUuid().toString()))
                .limit(Math.min(64, limit))
                .toList();
    }

    private static Map<UUID, SeedSnapshot> seeds(ServerWorld world) {
        return SEEDS.computeIfAbsent(world, ignored -> new HashMap<>());
    }

    static void pruneSeeds(Map<UUID, SeedSnapshot> seeds, Predicate<UUID> isLive) {
        seeds.keySet().removeIf(targetId -> !isLive.test(targetId));
    }

    public record SeedSnapshot(UUID ownerId, ItemStack stack, FireForgeMasteryTuning tuning,
                               UniqueAbilityExecution execution, int generation) {
        private SeedSnapshot nextGeneration() {
            return new SeedSnapshot(ownerId, stack.copy(), tuning, execution, generation + 1);
        }
    }


    private static LivingEntity findSeedTarget(ServerWorld world, LivingEntity actor, double range) {
        if (range <= 0) return null;
        return world.getOtherEntities(actor, HelperMethods.createBox(actor, range),
                        EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(candidate -> HelperMethods.checkAbilityTarget(candidate, actor)
                        && !candidate.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED))
                        && actor.squaredDistanceTo(candidate) <= range * range)
                .min(java.util.Comparator.comparingDouble(actor::squaredDistanceTo))
                .orElse(null);
    }

    public static OwnerState owner(ServerWorld world, LivingEntity actor) {
        return OWNERS.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(actor.getUuid(), ignored -> new OwnerState());
    }

    public static OwnerState ownerIfPresent(ServerWorld world, UUID actorId) {
        Map<UUID, OwnerState> owners = OWNERS.get(world);
        return owners == null ? null : owners.get(actorId);
    }

    // Spark Harvest: each detonation shortens the cast's own cooldown, up to a per-cast ceiling.
    public static void onDetonation(ServerWorld world, LivingEntity owner, ItemStack stack,
                                    FireForgeMasteryTuning tuning) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        if (owner == null || stack == null) return;
        OwnerState state = owner(world, owner);
        long now = world.getTime();
        int refund = tuning.integer(s("FLAMEWIND_HARVEST_REFUND_TICKS"), 0);
        if (refund > 0) {
            int cap = tuning.integer(s("FLAMEWIND_HARVEST_REFUND_CAP_TICKS"), 0);
            int applied = nextRefund(state.castRefunded, refund, cap);
            if (applied > 0) {
                state.castRefunded += applied;
                SimplySwordsAPI.reduceWeaponCooldown(owner, stack,
                        tuning.integer(s("COOLDOWN_TICKS"), Config.uniqueEffects.flamewind.cooldown), applied);
            }
        }
        int flashCount = tuning.integer(s("FLAMEWIND_FLASH_COUNT"), 0);
        if (flashCount > 0) {
            int window = tuning.integer(s("FLAMEWIND_FLASH_WINDOW_TICKS"), 20);
            if (now - state.lastFlashAt > window) state.flashes = 0;
            state.flashes++;
            state.lastFlashAt = now;
            if (state.flashes >= flashCount) {
                state.flashes = 0;
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,
                        tuning.integer(s("FLAMEWIND_FLASH_HASTE_TICKS"), 80),
                        tuning.integer(s("FLAMEWIND_FLASH_HASTE_AMPLIFIER"), 1)), owner);
            }
        }
        if (state.manualRelease) {
            float absorption = (float) tuning.get(s("FLAMEWIND_RESERVE_ABSORPTION"), 0);
            if (absorption > 0) {
                MasteryAbsorptionTracker.grant(owner, "flamewind/reserve", absorption,
                        tuning.integer(s("FLAMEWIND_RESERVE_ABSORPTION_TICKS"), 80),
                        (float) tuning.get(s("FLAMEWIND_RESERVE_ABSORPTION_CAP"), absorption));
            }
        }
        state.lastDetonationAt = now;
        }
    }

    // Chain Flash: a detonation landing close behind another hits harder.
    public static double chainBonus(ServerWorld world, LivingEntity owner, FireForgeMasteryTuning tuning) {
        OwnerState state = owner == null ? null : ownerIfPresent(world, owner.getUuid());
        int window = tuning.integer(s("FLAMEWIND_CHAIN_WINDOW_TICKS"), 0);
        if (state == null || window <= 0 || state.lastDetonationAt == 0) return 1;
        return world.getTime() - state.lastDetonationAt <= window
                ? tuning.get(s("FLAMEWIND_CHAIN_DAMAGE_MULTIPLIER"), 1) : 1;
    }

    public static void onReleaseKill(ServerWorld world, LivingEntity owner) {
        OwnerState state = owner == null ? null : ownerIfPresent(world, owner.getUuid());
        if (state != null && state.manualRelease) state.releaseKills++;
    }

    // Ashen Reset: enough kills in one release refunds a share of what is left.
    private static void applyReleaseReset(UniqueAbilityExecution execution, FireForgeMasteryTuning tuning,
                                          OwnerState owner) {
        int required = tuning.integer(s("FLAMEWIND_RESET_KILL_COUNT"), 0);
        double fraction = tuning.get(s("FLAMEWIND_RESET_REFUND_FRACTION"), 0);
        if (required <= 0 || fraction <= 0 || owner.releaseKills < required) return;
        execution.requestCooldownRefundFraction(fraction);
    }

    // Furnace Draft: attack speed scaling with the seeds this wielder currently holds.
    public static void refreshDraft(ServerWorld world, LivingEntity owner, FireForgeMasteryTuning tuning) {
        EntityAttributeInstance attackSpeed = owner.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed == null) return;
        if (!owner.isAlive() || owner.isRemoved() || owner.getWorld() != world) {
            removeDraft(owner);
            return;
        }
        double bonus = 0;
        double cap = 0;
        for (LivingEntity seed : ownedSeeds(world, owner, 64)) {
            SeedSnapshot snapshot = snapshot(seed);
            if (snapshot == null) continue;
            FireForgeMasteryTuning source = snapshot.tuning();
            double range = source.get(s("FLAMEWIND_DRAFT_RANGE"), 8);
            double perSeed = source.get(s("FLAMEWIND_DRAFT_PER_SEED"), 0);
            if (perSeed <= 0 || owner.squaredDistanceTo(seed) > range * range) continue;
            bonus += perSeed;
            cap = Math.max(cap, source.get(s("FLAMEWIND_DRAFT_CAP"), 0));
        }
        if (cap > 0) bonus = Math.min(cap, bonus);
        EntityAttributeModifier current = attackSpeed.getModifier(DRAFT_ID);
        if (bonus <= 0) {
            if (current != null) attackSpeed.removeModifier(DRAFT_ID);
            return;
        }
        owner(world, owner);
        if (current != null && current.value() == bonus) return;
        attackSpeed.removeModifier(DRAFT_ID);
        attackSpeed.addTemporaryModifier(new EntityAttributeModifier(DRAFT_ID, bonus,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeDraft(LivingEntity owner) {
        EntityAttributeInstance attackSpeed = owner.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(DRAFT_ID);
    }

    // Stormfront pushes outward; Converging Flame's positive pull flips it inward.
    public static double detonationKnockback(double pullStrength, double knockbackMultiplier) {
        double direction = pullStrength > 0 ? -1 : 1;
        return 0.28 * direction * Math.max(0, knockbackMultiplier);
    }

    static double draftBonus(int seeds, double perSeed, double cap) {
        double bonus = Math.max(0, seeds) * Math.max(0, perSeed);
        return cap > 0 ? Math.min(cap, bonus) : bonus;
    }

    static int nextRefund(int alreadyRefunded, int refund, int cap) {
        if (cap <= 0) return Math.max(0, refund);
        return Math.max(0, Math.min(refund, cap - Math.max(0, alreadyRefunded)));
    }

    public static int spreadDuration(int baseDuration, double fraction) {
        return fraction <= 0 ? baseDuration
                : Math.max(1, (int) Math.round(baseDuration * Math.min(1, fraction)));
    }

    public static double generationMultiplier(double perGeneration, int generation, int cap) {
        if (perGeneration <= 0 || generation <= 0) return 1;
        int steps = cap > 0 ? Math.min(generation, cap) : generation;
        return Math.pow(perGeneration, steps);
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        Map<UUID, SeedSnapshot> seeds = SEEDS.remove(world);
        if (seeds != null) {
            seeds.values().forEach(snapshot -> UniqueAbilityApi.cancel(snapshot.execution()));
        }
        Map<UUID, OwnerState> owners = OWNERS.remove(world);
        if (owners != null) {
            owners.keySet().forEach(id -> {
                if (world.getEntity(id) instanceof LivingEntity owner) removeDraft(owner);
            });
        }
    }

    public static void clearAll() {
        SEEDS.values().forEach(seeds -> seeds.values()
                .forEach(snapshot -> UniqueAbilityApi.cancel(snapshot.execution())));
        SEEDS.clear();
        OWNERS.clear();
    }

    public static final class OwnerState {
        private long recastReadyAt;
        private long lastDetonationAt;
        private long lastFlashAt;
        private int flashes;
        private int releaseKills;
        private int castRefunded;
        private boolean manualRelease;

        public boolean isManualRelease() {
            return manualRelease;
        }
    }

    private static FireForgeMasteryTuning.Setting s(String name) {
        return FireForgeMasteryTuning.Setting.valueOf(name);
    }
}
