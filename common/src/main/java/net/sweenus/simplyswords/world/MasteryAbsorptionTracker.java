package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// Absorption granted for a stated duration, tracked so it can be taken back when that duration ends.
public final class MasteryAbsorptionTracker {
    private static final Map<UUID, Grant> GRANTS = new HashMap<>();
    private static final Identifier ABILITY_ABSORPTION_CAPACITY =
            Identifier.of(SimplySwords.MOD_ID, "ability_absorption_capacity");

    private MasteryAbsorptionTracker() {
    }

    public static boolean hasActive(ServerWorld world) {
        if (world == null) return false;
        RegistryKey<World> key = world.getRegistryKey();
        return GRANTS.values().stream().anyMatch(grant -> grant.world.equals(key));
    }

    public static void grant(LivingEntity owner, float amount, int ticks, float cap) {
        if (owner == null || amount <= 0 || ticks <= 0 || !(owner.getWorld() instanceof ServerWorld world)) return;
        tick(owner);
        Grant existing = GRANTS.get(owner.getUuid());
        float already = existing == null ? 0 : existing.amount;
        float room = cap > 0 ? cap - already : amount;
        float wanted = Math.min(amount, Math.max(0, room));
        if (wanted <= 0) return;
        float applied = grantCapped(owner, wanted, Config.uniqueEffects.abilityAbsorptionCap);
        if (applied <= 0) return;
        Grant record = existing != null ? existing
                : GRANTS.computeIfAbsent(owner.getUuid(), ignored -> new Grant(world.getRegistryKey()));
        record.amount += applied;
        record.expiresAt = world.getTime() + ticks;
    }

    static float grantCapped(LivingEntity owner, float amount, float cap) {
        if (owner == null || amount <= 0.0F || cap <= 0.0F) return 0.0F;
        float effectiveCap = Math.min(Math.max(0.0F, cap),
                Math.max(0.0F, Config.uniqueEffects.abilityAbsorptionCap));
        float before = owner.getAbsorptionAmount();
        if (before >= effectiveCap) return 0.0F;
        EntityAttributeInstance capacity = owner.getAttributeInstance(EntityAttributes.GENERIC_MAX_ABSORPTION);
        if (capacity == null) return 0.0F;
        capacity.removeModifier(ABILITY_ABSORPTION_CAPACITY);
        double needed = requiredCapacity(capacity.getValue(), effectiveCap);
        if (needed > 0.0) {
            capacity.addPersistentModifier(new EntityAttributeModifier(ABILITY_ABSORPTION_CAPACITY,
                    needed, EntityAttributeModifier.Operation.ADD_VALUE));
        }
        owner.setAbsorptionAmount(Math.min(effectiveCap, before + amount));
        return Math.max(0.0F, owner.getAbsorptionAmount() - before);
    }

    static double requiredCapacity(double currentMaximum, float abilityCap) {
        return Math.max(0.0, Math.max(0.0F, abilityCap) - Math.max(0.0, currentMaximum));
    }

    public static void tick(LivingEntity owner) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) return;
        Grant grant = GRANTS.get(owner.getUuid());
        if (grant == null || world.getTime() < grant.expiresAt) return;
        GRANTS.remove(owner.getUuid());
        owner.setAbsorptionAmount(Math.max(0, owner.getAbsorptionAmount() - grant.amount));
    }

    // Expires grants held by entities that do not tick themselves, such as non-player allies.
    public static void sweep(ServerWorld world) {
        long now = world.getTime();
        Iterator<Map.Entry<UUID, Grant>> iterator = GRANTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Grant> entry = iterator.next();
            Grant grant = entry.getValue();
            if (!grant.world.equals(world.getRegistryKey()) || now < grant.expiresAt) continue;
            Entity entity = world.getEntity(entry.getKey());
            if (entity instanceof LivingEntity living) {
                living.setAbsorptionAmount(Math.max(0, living.getAbsorptionAmount() - grant.amount));
            }
            iterator.remove();
        }
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        GRANTS.entrySet().removeIf(entry -> entry.getValue().world.equals(world.getRegistryKey()));
    }

    public static void clear(LivingEntity owner) {
        if (owner == null) return;
        Grant grant = GRANTS.remove(owner.getUuid());
        if (grant != null) {
            owner.setAbsorptionAmount(Math.max(0, owner.getAbsorptionAmount() - grant.amount));
        }
    }

    public static void clearAll() {
        GRANTS.clear();
    }

    private static final class Grant {
        private final RegistryKey<World> world;
        private float amount;
        private long expiresAt;

        private Grant(RegistryKey<World> world) {
            this.world = world;
        }
    }
}
