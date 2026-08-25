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
import java.util.Map;
import java.util.UUID;

public final class Phase5FlamewindManager {
    private static final Map<UUID, SeedSnapshot> SEEDS = new HashMap<>();

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
        SEEDS.put(target.getUuid(), new SeedSnapshot(context.actor().getUuid(), context.stack().copy(),
                tuning, execution, 0));
        FlamewindVisualManager.refreshSeed(context.world(), target);
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, Phase5UniqueAbilities.HIT, target, 1, 0);
        return true;
    }

    public static SeedSnapshot snapshot(LivingEntity target) {
        return target == null ? null : SEEDS.get(target.getUuid());
    }

    public static void inherit(LivingEntity source, LivingEntity target) {
        SeedSnapshot snapshot = snapshot(source);
        if (snapshot != null) SEEDS.put(target.getUuid(), snapshot.nextGeneration());
    }

    public static void remove(UUID targetId) {
        SEEDS.remove(targetId);
    }

    public static boolean hasOwned(LivingEntity actor) {
        if (actor == null) return false;
        return SEEDS.values().stream().anyMatch(seed -> seed.ownerId.equals(actor.getUuid()));
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
