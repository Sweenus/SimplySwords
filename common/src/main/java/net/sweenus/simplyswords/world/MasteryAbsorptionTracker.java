package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class MasteryAbsorptionTracker {
    private static final Map<LivingEntity, Ledger> GRANTS = new WeakHashMap<>();
    private static final int MAX_SOURCES = 128;
    private static final String NBT_KEY = "SimplySwordsMasteryAbsorption";
    private static final Identifier ABILITY_ABSORPTION_CAPACITY =
            Identifier.of(SimplySwords.MOD_ID, "ability_absorption_capacity");
    private static final Identifier MASTERY_ABSORPTION_CAPACITY =
            Identifier.of(SimplySwords.MOD_ID, "mastery_absorption_capacity");

    private MasteryAbsorptionTracker() {
    }

    public static boolean hasActive(ServerWorld world) {
        return world != null && GRANTS.keySet().stream().anyMatch(owner -> owner.getWorld() == world);
    }

    public static void grant(LivingEntity owner, float amount, int ticks, float cap) {
        grant(owner, "legacy", amount, ticks, cap);
    }

    public static void grant(LivingEntity owner, String source, float amount, int ticks, float cap) {
        if (owner == null || !owner.isAlive() || !Float.isFinite(amount) || !Float.isFinite(cap)
                || amount <= 0 || ticks <= 0 || source == null || source.isBlank() || source.length() > 128
                || !(owner.getWorld() instanceof ServerWorld world)) return;
        tick(owner);
        Ledger ledger = GRANTS.computeIfAbsent(owner, ignored -> new Ledger());
        Grant existing = ledger.sources.get(source);
        if (existing == null && ledger.sources.size() >= MAX_SOURCES) return;
        float already = existing == null ? 0 : existing.amount;
        float wanted = Math.min(amount, Math.max(0, cap > 0 ? cap - already : amount));
        float applied = addAbsorption(owner, wanted, Config.uniqueEffects.abilityAbsorptionCap,
                MASTERY_ABSORPTION_CAPACITY, false);
        if (already + applied > 0) {
            ledger.sources.put(source, new Grant(already + applied, time(world) + ticks));
        }
        if (ledger.sources.isEmpty()) GRANTS.remove(owner);
    }

    static float grantCapped(LivingEntity owner, float amount, float cap) {
        return addAbsorption(owner, amount, cap, ABILITY_ABSORPTION_CAPACITY, true);
    }

    private static float addAbsorption(LivingEntity owner, float amount, float cap,
                                       Identifier capacityId, boolean persistent) {
        if (owner == null || !Float.isFinite(amount) || !Float.isFinite(cap) || amount <= 0 || cap <= 0) return 0;
        float effectiveCap = Math.min(cap, Math.max(0, Config.uniqueEffects.abilityAbsorptionCap));
        float before = owner.getAbsorptionAmount();
        if (before >= effectiveCap) return 0;
        EntityAttributeInstance capacity = owner.getAttributeInstance(EntityAttributes.GENERIC_MAX_ABSORPTION);
        if (capacity == null) return 0;
        capacity.removeModifier(capacityId);
        double needed = requiredCapacity(capacity.getValue(), effectiveCap);
        if (needed > 0) {
            EntityAttributeModifier modifier = new EntityAttributeModifier(capacityId, needed,
                    EntityAttributeModifier.Operation.ADD_VALUE);
            if (persistent) capacity.addPersistentModifier(modifier);
            else capacity.addTemporaryModifier(modifier);
        }
        owner.setAbsorptionAmount(Math.min(effectiveCap, before + amount));
        return Math.max(0, owner.getAbsorptionAmount() - before);
    }

    static double requiredCapacity(double currentMaximum, float abilityCap) {
        return Math.max(0, Math.max(0, abilityCap) - Math.max(0, currentMaximum));
    }

    public static void consume(LivingEntity owner, float nextAmount) {
        if (!(owner.getWorld() instanceof ServerWorld) || !Float.isFinite(nextAmount)) return;
        Ledger ledger = GRANTS.get(owner);
        if (ledger == null || ledger.adjusting) return;
        float spent = Math.max(0, owner.getAbsorptionAmount() - nextAmount);
        if (spent <= 0) return;
        var ordered = ledger.sources.entrySet().stream().sorted(Comparator
                .comparingLong((Map.Entry<String, Grant> entry) -> entry.getValue().expiresAt)
                .thenComparing(Map.Entry::getKey)).toList();
        for (var entry : ordered) {
            Grant grant = entry.getValue();
            float consumed = Math.min(spent, grant.amount);
            grant.amount -= consumed;
            spent -= consumed;
            if (grant.amount <= 0) ledger.sources.remove(entry.getKey());
            if (spent <= 0) break;
        }
        if (ledger.sources.isEmpty()) GRANTS.remove(owner);
    }

    public static void updateCapacity(LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld)) return;
        EntityAttributeInstance capacity = owner.getAttributeInstance(EntityAttributes.GENERIC_MAX_ABSORPTION);
        if (capacity == null || capacity.getModifier(MASTERY_ABSORPTION_CAPACITY) == null) return;
        restoreCapacity(owner, capacity);
    }

    private static void restoreCapacity(LivingEntity owner, EntityAttributeInstance capacity) {
        capacity.removeModifier(MASTERY_ABSORPTION_CAPACITY);
        double needed = requiredCapacity(capacity.getValue(), owner.getAbsorptionAmount());
        if (needed > 0) capacity.addTemporaryModifier(new EntityAttributeModifier(MASTERY_ABSORPTION_CAPACITY,
                needed, EntityAttributeModifier.Operation.ADD_VALUE));
    }

    public static void tick(LivingEntity owner) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) return;
        Ledger ledger = GRANTS.get(owner);
        if (ledger == null) return;
        long now = time(world);
        float expired = 0;
        var iterator = ledger.sources.values().iterator();
        while (iterator.hasNext()) {
            Grant grant = iterator.next();
            if (owner.isAlive() && now < grant.expiresAt) continue;
            expired += grant.amount;
            iterator.remove();
        }
        removeAbsorption(owner, ledger, expired);
        if (ledger.sources.isEmpty()) GRANTS.remove(owner);
    }

    public static void sweep(ServerWorld world) {
        for (LivingEntity owner : new ArrayList<>(GRANTS.keySet())) {
            if (owner.getWorld() != world) continue;
            if (owner.isRemoved()) GRANTS.remove(owner);
            else tick(owner);
        }
    }

    public static void clear(ServerWorld world) {
        for (LivingEntity owner : new ArrayList<>(GRANTS.keySet())) {
            if (owner.getWorld() == world) clear(owner);
        }
    }

    public static void clear(LivingEntity owner) {
        if (owner == null) return;
        Ledger ledger = GRANTS.get(owner);
        if (ledger == null) return;
        float amount = (float) ledger.sources.values().stream().mapToDouble(grant -> grant.amount).sum();
        ledger.sources.clear();
        removeAbsorption(owner, ledger, amount);
        GRANTS.remove(owner);
    }

    public static void clear(LivingEntity owner, String source) {
        Ledger ledger = GRANTS.get(owner);
        if (ledger == null) return;
        Grant grant = ledger.sources.remove(source);
        if (grant != null) removeAbsorption(owner, ledger, grant.amount);
        if (ledger.sources.isEmpty()) GRANTS.remove(owner);
    }

    public static void clearAll() {
        for (LivingEntity owner : new ArrayList<>(GRANTS.keySet())) clear(owner);
    }

    public static void writeNbt(LivingEntity owner, NbtCompound nbt) {
        if (!(owner.getWorld() instanceof ServerWorld)) return;
        Ledger ledger = GRANTS.get(owner);
        nbt.remove(NBT_KEY);
        if (ledger == null || ledger.sources.isEmpty()) return;
        NbtList entries = new NbtList();
        ledger.sources.forEach((source, grant) -> {
            NbtCompound entry = new NbtCompound();
            entry.putString("source", source);
            entry.putFloat("amount", grant.amount);
            entry.putLong("expires_at", grant.expiresAt);
            entries.add(entry);
        });
        nbt.put(NBT_KEY, entries);
    }

    public static void readNbt(LivingEntity owner, NbtCompound nbt) {
        if (!(owner.getWorld() instanceof ServerWorld)) return;
        GRANTS.remove(owner);
        NbtList entries = nbt.getList(NBT_KEY, NbtElement.COMPOUND_TYPE);
        Ledger ledger = new Ledger();
        float remaining = Math.max(0, owner.getAbsorptionAmount());
        for (int i = 0; i < Math.min(entries.size(), MAX_SOURCES); i++) {
            NbtCompound entry = entries.getCompound(i);
            String source = entry.getString("source");
            float amount = entry.getFloat("amount");
            if (source.isBlank() || source.length() > 128 || ledger.sources.containsKey(source)
                    || !Float.isFinite(amount) || amount <= 0) continue;
            amount = Math.min(remaining, amount);
            if (amount <= 0) break;
            ledger.sources.put(source, new Grant(amount, entry.getLong("expires_at")));
            remaining -= amount;
        }
        if (ledger.sources.isEmpty()) return;
        GRANTS.put(owner, ledger);
        EntityAttributeInstance capacity = owner.getAttributeInstance(EntityAttributes.GENERIC_MAX_ABSORPTION);
        if (capacity != null) restoreCapacity(owner, capacity);
        tick(owner);
    }

    private static void removeAbsorption(LivingEntity owner, Ledger ledger, float amount) {
        if (amount <= 0) return;
        ledger.adjusting = true;
        try {
            owner.setAbsorptionAmount(Math.max(0, owner.getAbsorptionAmount() - amount));
        } finally {
            ledger.adjusting = false;
        }
    }

    private static long time(ServerWorld world) {
        ServerWorld overworld = world.getServer().getOverworld();
        return (overworld == null ? world : overworld).getTime();
    }

    private static final class Ledger {
        private final Map<String, Grant> sources = new LinkedHashMap<>();
        private boolean adjusting;
    }

    private static final class Grant {
        private float amount;
        private final long expiresAt;

        private Grant(float amount, long expiresAt) {
            this.amount = amount;
            this.expiresAt = expiresAt;
        }
    }
}
