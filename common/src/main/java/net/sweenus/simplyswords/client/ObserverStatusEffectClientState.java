package net.sweenus.simplyswords.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.ObserverStatusEffectSnapshot;
import net.sweenus.simplyswords.network.ObserverStatusEffectsPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class ObserverStatusEffectClientState {

    private static final Map<Identifier, Map<UUID, Map<Identifier, StoredEffect>>> EFFECTS = new HashMap<>();

    private ObserverStatusEffectClientState() {
    }

    public static synchronized void apply(ObserverStatusEffectsPacket packet) {
        if (packet.replaceAll()) {
            EFFECTS.clear();
        }

        Map<UUID, Map<Identifier, StoredEffect>> dimensionEffects =
                EFFECTS.computeIfAbsent(packet.dimensionId(), ignored -> new HashMap<>());
        for (ObserverStatusEffectsPacket.Entry entry : packet.entries()) {
            if (!entry.active()) {
                Map<Identifier, StoredEffect> entityEffects = dimensionEffects.get(entry.entityId());
                if (entityEffects == null) {
                    continue;
                }
                entityEffects.remove(entry.effectId());
                if (entityEffects.isEmpty()) {
                    dimensionEffects.remove(entry.entityId());
                }
                continue;
            }

            long expiryTick = entry.infinite()
                    ? Long.MAX_VALUE
                    : packet.serverWorldTime() + Math.max(0, entry.duration());
            dimensionEffects
                    .computeIfAbsent(entry.entityId(), ignored -> new HashMap<>())
                    .put(entry.effectId(), new StoredEffect(
                            entry.amplifier(),
                            expiryTick,
                            entry.infinite(),
                            entry.ambient(),
                            entry.showParticles(),
                            entry.showIcon()));
        }

        if (dimensionEffects.isEmpty()) {
            EFFECTS.remove(packet.dimensionId());
        }
    }

    public static synchronized Optional<ObserverStatusEffectSnapshot> get(LivingEntity entity, Identifier effectId) {
        if (entity == null || effectId == null || entity.getWorld() == null) {
            return Optional.empty();
        }

        Identifier dimensionId = entity.getWorld().getRegistryKey().getValue();
        Map<UUID, Map<Identifier, StoredEffect>> dimensionEffects = EFFECTS.get(dimensionId);
        if (dimensionEffects == null) {
            return Optional.empty();
        }
        Map<Identifier, StoredEffect> entityEffects = dimensionEffects.get(entity.getUuid());
        if (entityEffects == null) {
            return Optional.empty();
        }
        StoredEffect stored = entityEffects.get(effectId);
        if (stored == null) {
            return Optional.empty();
        }

        long worldTime = entity.getWorld().getTime();
        if (!stored.infinite() && stored.expiryTick() <= worldTime) {
            entityEffects.remove(effectId);
            if (entityEffects.isEmpty()) {
                dimensionEffects.remove(entity.getUuid());
            }
            if (dimensionEffects.isEmpty()) {
                EFFECTS.remove(dimensionId);
            }
            return Optional.empty();
        }

        int remainingDuration = stored.infinite()
                ? -1
                : (int) Math.min(Integer.MAX_VALUE, stored.expiryTick() - worldTime);
        return Optional.of(new ObserverStatusEffectSnapshot(
                stored.amplifier(),
                remainingDuration,
                stored.infinite(),
                stored.ambient(),
                stored.showParticles(),
                stored.showIcon()));
    }

    public static synchronized void clear() {
        EFFECTS.clear();
    }

    private record StoredEffect(int amplifier,
                                long expiryTick,
                                boolean infinite,
                                boolean ambient,
                                boolean showParticles,
                                boolean showIcon) {
    }
}
