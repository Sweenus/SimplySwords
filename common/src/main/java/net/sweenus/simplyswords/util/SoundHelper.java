package net.sweenus.simplyswords.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SoundHelper {

    private static final Map<Identifier, ScheduledExecutorService> soundSchedulers = new ConcurrentHashMap<>();

    public static void loopSound(LivingEntity entity, Identifier soundId, int soundDurationSeconds) {
        if (entity.getWorld().isClient()) return; // Only execute on the server side

        ServerWorld serverWorld = (ServerWorld) entity.getWorld();
        SoundEvent soundEvent = SoundRegistry.SOUND.getRegistrar().get(soundId);

        // If the sound is already playing, do nothing
        if (soundSchedulers.containsKey(soundId)) {
            return;
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        soundSchedulers.put(soundId, scheduler);

        // Schedule the sound to play repeatedly
        scheduler.scheduleAtFixedRate(() -> {
            if (entity.isAlive() && entity.hasStatusEffect(EffectRegistry.ELEMENTAL_VORTEX.get())) {
                playSound(serverWorld, entity, soundEvent);
            } else {
                stopLoopingSound(entity, soundId); // Stop the scheduler if the entity is no longer valid
            }
        }, 0, soundDurationSeconds, TimeUnit.SECONDS);
    }

    private static void playSound(ServerWorld serverWorld, LivingEntity entity, SoundEvent soundEvent) {
        serverWorld.playSound(null, entity.getBlockPos(), soundEvent, entity.getSoundCategory(), 1.0f, 1.0f);
    }

    public static void stopLoopingSound(LivingEntity entity, Identifier soundId) {
        ScheduledExecutorService scheduler = soundSchedulers.remove(soundId);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}