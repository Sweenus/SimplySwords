package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChompolotlMasteryManager {
    private static final Map<UUID, OwnerState> OWNERS = new HashMap<>();
    private static final java.util.Set<LivingEntity> DEATHS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private ChompolotlMasteryManager() {
    }

    private static OwnerState state(LivingEntity owner) {
        OwnerState previous = OWNERS.get(owner.getUuid());
        if (previous != null && (previous.owner != owner || previous.world != owner.getWorld())) clearOwner(previous.owner);
        return OWNERS.computeIfAbsent(owner.getUuid(), ignored -> new OwnerState(owner));
    }

    private static OwnerState existing(LivingEntity owner) {
        OwnerState previous = OWNERS.get(owner.getUuid());
        if (previous == null) return null;
        if (previous.owner != owner || previous.world != owner.getWorld()) {
            clearOwner(previous.owner);
            return null;
        }
        return previous;
    }

    public static void register(SimplySwordsAxolotlEntity summon, boolean replaceGuardian) {
        LivingEntity owner = summon.getOwner();
        if (owner == null || !owner.isAlive() || owner.isRemoved()) return;
        OwnerState state = state(owner);
        if (summon.isEternalGuardian()) {
            for (SimplySwordsAxolotlEntity previous : List.copyOf(state.summons.values())) {
                if (previous != summon && previous.isEternalGuardian() && previous.isAlive() && !previous.isRemoved()) {
                    if (!replaceGuardian && previous.getUuid().toString().compareTo(summon.getUuid().toString()) < 0) {
                        summon.discard();
                        return;
                    }
                    previous.discard();
                }
            }
        }
        state.summons.put(summon.getUuid(), summon);
    }

    public static void remove(SimplySwordsAxolotlEntity summon) {
        OwnerState state = OWNERS.get(summon.getOwnerUuid());
        if (state != null) state.summons.remove(summon.getUuid());
    }

    public static List<SimplySwordsAxolotlEntity> owned(LivingEntity owner) {
        OwnerState state = existing(owner);
        if (state == null) return List.of();
        state.summons.values().removeIf(summon -> !valid(summon, owner));
        return state.summons.values().stream().sorted(Comparator.comparing(summon -> summon.getUuid().toString())).toList();
    }

    private static boolean valid(SimplySwordsAxolotlEntity summon, LivingEntity owner) {
        return summon.isAlive() && !summon.isRemoved() && !summon.isExpired()
                && summon.getWorld() == owner.getWorld() && owner.isAlive() && !owner.isRemoved();
    }

    public static boolean procReady(LivingEntity owner) {
        OwnerState state = existing(owner);
        return state == null || owner.getWorld().getTime() >= state.procReady;
    }

    public static void setProcCooldown(LivingEntity owner, int ticks) {
        state(owner).procReady = owner.getWorld().getTime() + Math.max(0, ticks);
    }

    public static LivingEntity currentTarget(LivingEntity owner) {
        OwnerState state = existing(owner);
        if (state == null || state.target == null) return null;
        if (!(state.world.getEntity(state.target) instanceof LivingEntity target) || !target.isAlive()
                || target.isRemoved() || !HelperMethods.checkAbilityTarget(target, owner)) {
            state.target = null;
            return null;
        }
        return target;
    }

    public static void onDamageApplied(LivingEntity target, DamageSource source) {
        if (!(source.getAttacker() instanceof LivingEntity owner) || owner instanceof net.minecraft.entity.Tameable
                || !(owner.getWorld() instanceof ServerWorld) || !HelperMethods.checkAbilityTarget(target, owner)) return;
        OwnerState state = existing(owner);
        if (state != null && (LongPathFinalFormsMasteryCombatManager.melee(source)
                || source.isIn(DamageTypeTags.IS_PROJECTILE))) {
            state.target = target.isAlive() ? target.getUuid() : null;
        }
    }

    public static void firstBite(SimplySwordsAxolotlEntity summon, int percent) {
        LivingEntity owner = summon.getOwner();
        if (owner == null || percent <= 0) return;
        OwnerState state = existing(owner);
        if (state == null) return;
        long now = state.world.getTime();
        if (now < state.refundReady) return;
        state.refundReady = now + 100;
        SimplySwordsAPI.reduceWeaponCooldown(owner, summon.getCastingStack(), summon.getActiveCooldown(),
                summon.getActiveCooldown() * percent / 100);
    }

    public static void onDeath(LivingEntity target, DamageSource source) {
        if (!DEATHS.add(target)) return;
        SimplySwordsAxolotlEntity summon = source.getSource() instanceof SimplySwordsAxolotlEntity origin ? origin
                : source.getAttacker() instanceof SimplySwordsAxolotlEntity attacker ? attacker : null;
        if (summon != null && summon.getOwner() != null && valid(summon, summon.getOwner())
                && HelperMethods.checkAbilityTarget(target, summon.getOwner())) summon.onMasteryKill(target);
        clearOwner(target);
    }

    public static void victory(SimplySwordsAxolotlEntity summon, int required, int window, int refund) {
        LivingEntity owner = summon.getOwner();
        if (owner == null || required <= 0) return;
        OwnerState state = existing(owner);
        if (state == null) return;
        long now = state.world.getTime();
        state.kills.removeIf(tick -> now - tick > window);
        state.kills.addLast(now);
        if (state.kills.size() < required) return;
        for (int i = 0; i < required; i++) state.kills.removeFirst();
        SimplySwordsAPI.reduceWeaponCooldown(owner, summon.getCastingStack(), summon.getActiveCooldown(), refund);
    }

    public static float intercept(LivingEntity owner, DamageSource source, float amount) {
        if (!(owner.getWorld() instanceof ServerWorld) || amount <= 0 || !Float.isFinite(amount)
                || existing(owner) == null || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        SimplySwordsAxolotlEntity guardian = owned(owner).stream()
                .filter(SimplySwordsAxolotlEntity::isEternalGuardian)
                .filter(summon -> owner.squaredDistanceTo(summon) <= summon.getInterceptionRange() * summon.getInterceptionRange())
                .min(Comparator.comparingDouble((SimplySwordsAxolotlEntity summon) -> owner.squaredDistanceTo(summon))
                        .thenComparing(summon -> summon.getUuid().toString())).orElse(null);
        if (guardian == null) return amount;
        return amount - guardian.interceptDamage(source, amount);
    }

    public static void charge(ServerPlayerEntity rider, int entityId, int action) {
        SimplySwordsAxolotlEntity summon = mount(rider, entityId);
        if (summon != null) summon.handleWaveCharge(rider, action);
    }

    public static void leap(ServerPlayerEntity rider, int entityId) {
        SimplySwordsAxolotlEntity summon = mount(rider, entityId);
        if (summon != null) summon.tryMountLeap(rider);
    }

    private static SimplySwordsAxolotlEntity mount(ServerPlayerEntity rider, int entityId) {
        if (!(rider.getVehicle() instanceof SimplySwordsAxolotlEntity summon) || summon.getId() != entityId
                || summon.getControllingPassenger() != rider || !summon.isRavager() || !rider.isAlive()
                || IncapacitatingStatusEffectRegistry.isIncapacitated(rider)) return null;
        return summon;
    }

    public static boolean validShoulder(NbtCompound nbt, ServerWorld world) {
        if (!"simplyswords:simplyaxolotlentity".equals(nbt.getString("id"))) return false;
        if (!nbt.contains("ChompExpiresAt")) nbt.putLong("ChompExpiresAt", world.getTime()
                + Math.max(1, nbt.contains("MasteryLifespan") ? nbt.getInt("MasteryLifespan") : SimplySwordsAxolotlEntity.lifespan));
        if (!nbt.contains("ChompWorld")) nbt.putString("ChompWorld", world.getRegistryKey().getValue().toString());
        return nbt.getLong("ChompExpiresAt") > world.getTime()
                && nbt.getString("ChompWorld").equals(world.getRegistryKey().getValue().toString());
    }

    public static void tick(ServerWorld world) {
        for (OwnerState state : List.copyOf(OWNERS.values())) {
            if (state.world != world) continue;
            if (!state.owner.isAlive() || state.owner.isRemoved() || state.owner.getWorld() != world) {
                clearOwner(state.owner);
                continue;
            }
            state.summons.values().removeIf(summon -> !valid(summon, state.owner));
            currentTarget(state.owner);
            state.kills.removeIf(tick -> world.getTime() - tick > 200);
            if (state.summons.isEmpty() && state.target == null && state.kills.isEmpty()
                    && Math.max(state.procReady, state.refundReady) <= world.getTime())
                OWNERS.remove(state.owner.getUuid());
        }
    }

    public static void clearOwner(LivingEntity owner) {
        OwnerState state = OWNERS.remove(owner.getUuid());
        if (state != null) List.copyOf(state.summons.values()).forEach(SimplySwordsAxolotlEntity::discard);
    }

    public static void clear(ServerWorld world) {
        OWNERS.values().stream().filter(state -> state.world == world).map(state -> state.owner).toList()
                .forEach(ChompolotlMasteryManager::clearOwner);
        DEATHS.removeIf(entity -> entity.getWorld() == world);
    }

    public static void clearAll() {
        OWNERS.values().stream().map(state -> state.owner).toList().forEach(ChompolotlMasteryManager::clearOwner);
        DEATHS.clear();
    }

    private static final class OwnerState {
        private final LivingEntity owner;
        private final ServerWorld world;
        private final Map<UUID, SimplySwordsAxolotlEntity> summons = new HashMap<>();
        private final ArrayDeque<Long> kills = new ArrayDeque<>();
        private UUID target;
        private long procReady;
        private long refundReady;

        private OwnerState(LivingEntity owner) {
            this.owner = owner;
            this.world = (ServerWorld) owner.getWorld();
        }
    }
}
