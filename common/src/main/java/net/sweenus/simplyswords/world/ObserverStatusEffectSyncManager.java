package net.sweenus.simplyswords.world;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.ObserverStatusEffectSyncRegistry;
import net.sweenus.simplyswords.network.ObserverStatusEffectsPacket;

import java.util.ArrayList;
import java.util.List;

public final class ObserverStatusEffectSyncManager {

    private static boolean initialized;

    private ObserverStatusEffectSyncManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PlayerEvent.PLAYER_JOIN.register(ObserverStatusEffectSyncManager::sendSnapshot);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd) -> sendSnapshot(player));
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> sendSnapshot(player));
        EntityEvent.ADD.register((entity, world) -> {
            if (entity instanceof LivingEntity livingEntity && world instanceof ServerWorld serverWorld) {
                syncEntity(serverWorld, livingEntity);
            }
            return EventResult.pass();
        });
    }

    public static void syncApplied(LivingEntity entity, StatusEffectInstance instance) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Identifier effectId = effectId(instance);
        if (!ObserverStatusEffectSyncRegistry.isRegistered(effectId)) {
            return;
        }
        sendToWorld(world, false, List.of(activeEntry(entity, effectId, instance)));
    }

    public static void syncRemoved(LivingEntity entity, StatusEffectInstance instance) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Identifier effectId = effectId(instance);
        if (!ObserverStatusEffectSyncRegistry.isRegistered(effectId)) {
            return;
        }
        sendToWorld(world, false, List.of(ObserverStatusEffectsPacket.Entry.removed(entity.getUuid(), effectId)));
    }

    public static void removeEntity(LivingEntity entity) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }
        List<ObserverStatusEffectsPacket.Entry> removals = new ArrayList<>();
        for (StatusEffectInstance instance : entity.getStatusEffects()) {
            Identifier effectId = effectId(instance);
            if (ObserverStatusEffectSyncRegistry.isRegistered(effectId)) {
                removals.add(ObserverStatusEffectsPacket.Entry.removed(entity.getUuid(), effectId));
            }
        }
        sendToWorld(world, false, removals);
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        List<ObserverStatusEffectsPacket.Entry> entries = collectWorldEntries(world);
        new ObserverStatusEffectsPacket(
                world.getRegistryKey().getValue(),
                world.getTime(),
                true,
                entries).sendTo(player);
    }

    private static void syncEntity(ServerWorld world, LivingEntity entity) {
        List<ObserverStatusEffectsPacket.Entry> entries = new ArrayList<>();
        for (StatusEffectInstance instance : entity.getStatusEffects()) {
            Identifier effectId = effectId(instance);
            if (ObserverStatusEffectSyncRegistry.isRegistered(effectId)) {
                entries.add(activeEntry(entity, effectId, instance));
            }
        }
        sendToWorld(world, false, entries);
    }

    private static List<ObserverStatusEffectsPacket.Entry> collectWorldEntries(ServerWorld world) {
        List<ObserverStatusEffectsPacket.Entry> entries = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof LivingEntity livingEntity) || entity.isRemoved()) {
                continue;
            }
            for (StatusEffectInstance instance : livingEntity.getStatusEffects()) {
                Identifier effectId = effectId(instance);
                if (ObserverStatusEffectSyncRegistry.isRegistered(effectId)) {
                    entries.add(activeEntry(livingEntity, effectId, instance));
                }
            }
        }
        return entries;
    }

    private static ObserverStatusEffectsPacket.Entry activeEntry(LivingEntity entity, Identifier effectId,
                                                                  StatusEffectInstance instance) {
        return new ObserverStatusEffectsPacket.Entry(
                entity.getUuid(),
                effectId,
                true,
                instance.getAmplifier(),
                instance.getDuration(),
                instance.isInfinite(),
                instance.isAmbient(),
                instance.shouldShowParticles(),
                instance.shouldShowIcon());
    }

    private static Identifier effectId(StatusEffectInstance instance) {
        return instance == null ? null : Registries.STATUS_EFFECT.getId(instance.getEffectType());
    }

    private static void sendToWorld(ServerWorld world, boolean replaceAll,
                                    List<ObserverStatusEffectsPacket.Entry> entries) {
        if (world == null || entries == null || entries.isEmpty()) {
            return;
        }
        new ObserverStatusEffectsPacket(
                world.getRegistryKey().getValue(),
                world.getTime(),
                replaceAll,
                entries).sendTo(world.getPlayers());
    }
}
