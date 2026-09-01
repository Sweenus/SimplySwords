package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// Owner-side Mjolnir state for the nodes whose triggers sit outside a running storm execution.
public final class MjolnirCombatManager {
    private static final int RESOLVE_INTERVAL = 5;
    private static final int STATE_LIFETIME = 2400;
    private static final int CONDUCTIVE_MEMORY_TICKS = 60;
    private static final Map<UUID, OwnerState> STATES = new HashMap<>();
    private static boolean rebuking;

    private MjolnirCombatManager() {
    }

    public static void tickHeld(ItemStack stack, LivingEntity owner) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Phase4AbsorptionTracker.tick(owner);
        long now = world.getTime();
        OwnerState state = state(owner, now);
        state.conductiveMemory.values().removeIf(deadline -> deadline <= now);
        if (state.fallProtectionUntil >= now) {
            owner.fallDistance = 0;
        }
        if (owner.age % RESOLVE_INTERVAL != 0) {
            return;
        }
        UniqueAbilityExecution execution = Phase6CombatManager.beginPassive(
                Phase6UniqueAbilities.MJOLNIR_STORM, world, stack, owner, null);
        state.tuning = Phase6UniqueAbilities.tuning(execution);
        state.tuningExpiresAt = now + RESOLVE_INTERVAL * 4L;
        UniqueAbilityApi.finish(execution, Phase6UniqueAbilities.FINISH, 0);
    }

    public static void onStormStarted(LivingEntity owner, Phase6AbilityTuning tuning) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return;
        }
        OwnerState state = state(owner, world.getTime());
        state.tuning = tuning;
        state.tuningExpiresAt = world.getTime() + STATE_LIFETIME;
    }

    public static void grantFallProtection(LivingEntity owner, long until) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return;
        }
        OwnerState state = state(owner, world.getTime());
        state.fallProtectionUntil = Math.max(state.fallProtectionUntil, until);
    }

    public static void armChargedAdvance(LivingEntity owner, double multiplier, long now) {
        if (owner == null || multiplier <= 1) {
            return;
        }
        OwnerState state = state(owner, now);
        state.advanceMultiplier = multiplier;
        world(owner).ifPresent(world -> world.spawnParticles(
                net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                owner.getX(), owner.getBodyY(0.7), owner.getZ(), 8, 0.3, 0.3, 0.3, 0.06));
    }

    public static void rememberConductive(LivingEntity owner, LivingEntity target, long now) {
        if (owner == null || target == null) {
            return;
        }
        OwnerState state = state(owner, now);
        state.conductiveMemory.put(target.getUuid(), now + CONDUCTIVE_MEMORY_TICKS);
    }

    public static float modifyIncomingDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim == null || amount <= 0 || !(victim.getWorld() instanceof ServerWorld world)) {
            return amount;
        }
        OwnerState state = STATES.get(victim.getUuid());
        if (state == null || state.tuning == null || world.getTime() > state.tuningExpiresAt) {
            return amount;
        }
        if (source.isIn(DamageTypeTags.IS_FALL) && state.fallProtectionUntil >= world.getTime()) {
            return 0;
        }
        if (!state.tuning.flag(MjolnirStormManager.MODE_CONDUCTIVE_GUARD)) {
            return amount;
        }
        if (source.getAttacker() instanceof LivingEntity attacker
                && attacker.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM))) {
            return amount * (float) scoped(state.tuning, s("MJOLNIR_CONDUCTIVE_INCOMING_MULTIPLIER"),
                    s("INCOMING_MULTIPLIER"), 1);
        }
        return amount;
    }

    public static float modifyOutgoingDamage(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0 || !(source.getAttacker() instanceof LivingEntity attacker)) {
            return amount;
        }
        if (!(attacker.getWorld() instanceof ServerWorld world) || victim == null) {
            return amount;
        }
        OwnerState state = STATES.get(attacker.getUuid());
        if (state == null
                || state.tuning == null
                || world.getTime() > state.tuningExpiresAt
                || state.advanceMultiplier <= 1
                || !state.tuning.flag(MjolnirStormManager.MODE_CHARGED_ADVANCE)) {
            return amount;
        }
        float multiplier = (float) state.advanceMultiplier;
        state.advanceMultiplier = 0;
        return amount * multiplier;
    }

    public static void onDamageApplied(LivingEntity victim, DamageSource source) {
        if (rebuking || victim == null || !(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        OwnerState state = STATES.get(victim.getUuid());
        if (state == null
                || state.tuning == null
                || world.getTime() > state.tuningExpiresAt
                || !state.tuning.flag(MjolnirStormManager.MODE_THUNDER_REBUKE)
                || world.getTime() < state.rebukeReadyAt
                || source.isIn(DamageTypeTags.IS_PROJECTILE)
                || !(source.getAttacker() instanceof LivingEntity attacker)
                || attacker == victim
                || !attacker.isAlive()
                || !HelperMethods.checkAbilityTarget(attacker, victim)) {
            return;
        }
        state.rebukeReadyAt = world.getTime()
                + state.tuning.integer(s("MJOLNIR_REBUKE_LOCKOUT_TICKS"), 30);
        ItemStack stack = victim.getMainHandStack();
        float damage = HelperMethods.abilityScaledDamage("lightning", victim, stack,
                Config.uniqueEffects.mjolnir.damageScaling, Config.uniqueEffects.mjolnir.spellScaling)
                * (float) scoped(state.tuning, s("MJOLNIR_REBUKE_DAMAGE_MULTIPLIER"),
                s("SECONDARY_DAMAGE_MULTIPLIER"), 0.2);
        DamageSource reprisal = victim.getDamageSources().indirectMagic(victim, victim);
        rebuking = true;
        try {
            HelperMethods.damageThroughIframes(attacker, reprisal,
                    HelperMethods.applyAbilityDamageEnchantments(world, stack, attacker, reprisal, damage));
        } finally {
            rebuking = false;
        }
        world.spawnParticles(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                attacker.getX(), attacker.getBodyY(0.6), attacker.getZ(), 12, 0.28, 0.32, 0.28, 0.1);
        world.playSound(null, attacker.getBlockPos(),
                SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_02.get(),
                attacker.getSoundCategory(), 0.45F, 1.25F);
    }

    public static void onDeath(LivingEntity victim, DamageSource source) {
        if (victim == null || !(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (!(source.getAttacker() instanceof LivingEntity killer)) {
            return;
        }
        OwnerState state = STATES.get(killer.getUuid());
        if (state == null
                || state.tuning == null
                || world.getTime() > state.tuningExpiresAt
                || !state.tuning.flag(MjolnirStormManager.MODE_FLASH_STEP)) {
            return;
        }
        Long remembered = state.conductiveMemory.remove(victim.getUuid());
        boolean conductive = victim.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM))
                || (remembered != null && remembered >= world.getTime());
        if (!conductive) {
            return;
        }
        int duration = state.tuning.integer(s("MJOLNIR_FLASH_SPEED_TICKS"), 60);
        if (duration > 0) {
            killer.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration,
                    state.tuning.integer(s("MJOLNIR_FLASH_SPEED_AMPLIFIER"), 1)), killer);
        }
        int refund = scopedInt(state.tuning, s("MJOLNIR_FLASH_REFUND_TICKS"), s("REFUND_TICKS"), 0);
        if (refund > 0) {
            SimplySwordsAPI.reduceWeaponCooldown(killer, new ItemStack(ItemsRegistry.MJOLNIR.get()),
                    Config.uniqueEffects.mjolnir.cooldown, refund);
        }
    }

    public static void clear(ServerWorld world) {
        if (world == null) {
            return;
        }
        STATES.keySet().removeIf(uuid -> world.getEntity(uuid) != null);
    }

    public static void clearAll() {
        STATES.clear();
    }

    public static void clearActor(LivingEntity actor) {
        if (actor != null) {
            STATES.remove(actor.getUuid());
        }
    }

    private static java.util.Optional<ServerWorld> world(LivingEntity owner) {
        return owner.getWorld() instanceof ServerWorld world
                ? java.util.Optional.of(world)
                : java.util.Optional.empty();
    }

    private static OwnerState state(LivingEntity owner, long now) {
        Iterator<Map.Entry<UUID, OwnerState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt <= now) {
                iterator.remove();
            }
        }
        OwnerState state = STATES.computeIfAbsent(owner.getUuid(), ignored -> new OwnerState());
        state.expiresAt = now + STATE_LIFETIME;
        if (STATES.size() > 32) {
            STATES.entrySet().stream()
                    .sorted(Comparator.comparingLong(entry -> entry.getValue().expiresAt))
                    .limit(STATES.size() - 32L)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(STATES::remove);
        }
        return state;
    }

    private static double scoped(Phase6AbilityTuning tuning, Phase6AbilityTuning.Setting scoped,
                                 Phase6AbilityTuning.Setting legacy, double fallback) {
        return tuning.has(scoped) ? tuning.get(scoped, fallback) : tuning.get(legacy, fallback);
    }

    private static int scopedInt(Phase6AbilityTuning tuning, Phase6AbilityTuning.Setting scoped,
                                 Phase6AbilityTuning.Setting legacy, int fallback) {
        return tuning.has(scoped) ? tuning.integer(scoped, fallback) : tuning.integer(legacy, fallback);
    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }

    private static final class OwnerState {
        private final Map<UUID, Long> conductiveMemory = new HashMap<>();
        private Phase6AbilityTuning tuning;
        private long tuningExpiresAt;
        private long expiresAt;
        private long fallProtectionUntil;
        private long rebukeReadyAt;
        private double advanceMultiplier;
    }
}
