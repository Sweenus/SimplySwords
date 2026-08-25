package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase5AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase5UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.FlameSeedEffect;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class Phase5FlamewindManager {
    private static final Map<ServerWorld, Map<UUID, SeedSnapshot>> SEEDS = new HashMap<>();

    private Phase5FlamewindManager() {
    }

    public static boolean activate(WeaponAbilityContext context, LivingEntity target) {
        UniqueAbilityExecution execution = Phase5CombatManager.beginActive(Phase5UniqueAbilities.FLAMEWIND_SEED,
                context, Config.uniqueEffects.flamewind.cooldown);
        UniqueAbilityApi.start(execution);
        Phase5AbilityTuning tuning = Phase5UniqueAbilities.tuning(execution);
        if (tuning.flag(1 << 21) && hasOwned(context.actor())) {
            int affected = FlameSeedEffect.detonateOwned(context.world(), context.actor(),
                    tuning.flag(1 << 25) ? 3 : 64);
            UniqueAbilityApi.finish(execution, Phase5UniqueAbilities.FINISH, affected);
            return affected > 0;
        }
        if (target == null || !HelperMethods.checkAbilityTarget(target, context.actor())) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
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
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, Phase5UniqueAbilities.HIT, target, 1, 0);
        return true;
    }

    public static SeedSnapshot snapshot(LivingEntity target) {
        if (target == null || !(target.getWorld() instanceof ServerWorld world)) return null;
        Map<UUID, SeedSnapshot> seeds = SEEDS.get(world);
        return seeds == null ? null : seeds.get(target.getUuid());
    }

    public static void inherit(LivingEntity source, LivingEntity target) {
        SeedSnapshot snapshot = snapshot(source);
        if (snapshot != null && target.getWorld() instanceof ServerWorld world) {
            seeds(world).put(target.getUuid(), snapshot.nextGeneration());
        }
    }

    public static void remove(UUID targetId) {
        if (targetId == null) return;
        SEEDS.values().forEach(seeds -> seeds.remove(targetId));
        SEEDS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void remove(ServerWorld world, UUID targetId) {
        if (world == null || targetId == null) return;
        Map<UUID, SeedSnapshot> seeds = SEEDS.get(world);
        if (seeds == null) return;
        seeds.remove(targetId);
        if (seeds.isEmpty()) SEEDS.remove(world);
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

    public record SeedSnapshot(UUID ownerId, ItemStack stack, Phase5AbilityTuning tuning,
                               UniqueAbilityExecution execution, int generation) {
        private SeedSnapshot nextGeneration() {
            return new SeedSnapshot(ownerId, stack.copy(), tuning, execution, generation + 1);
        }
    }

    private static Phase5AbilityTuning.Setting s(String name) {
        return Phase5AbilityTuning.Setting.valueOf(name);
    }
}
