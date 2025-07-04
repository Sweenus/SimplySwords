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


    // There has to be a better way to loop sounds....
    private static final Map<Identifier, ScheduledExecutorService> soundSchedulers = new ConcurrentHashMap<>();

    public static void loopSound(LivingEntity entity, Identifier soundId, int soundDurationSeconds, int updateFrequencyTicks) {
        if (entity.getWorld().isClient()) return;

        ServerWorld serverWorld = (ServerWorld) entity.getWorld();
        SoundEvent soundEvent = SoundRegistry.SOUND.getRegistrar().get(soundId);

        if (soundSchedulers.containsKey(soundId)) {
            return;
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        soundSchedulers.put(soundId, scheduler);

        int totalTicks = soundDurationSeconds * 20;
        int[] elapsedTicks = {0};

        scheduler.scheduleAtFixedRate(() -> {
            elapsedTicks[0] += updateFrequencyTicks;

            if (entity.isAlive() && elapsedTicks[0] < totalTicks && entity.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.ELEMENTAL_VORTEX))) {
                float playbackProgress = (elapsedTicks[0] % totalTicks) / (float) totalTicks;
                float pitch = 1.0f + playbackProgress * 0.2f;

                // Play the sound, updating its position dynamically and simulating playback continuity
                serverWorld.playSound(null, entity.getBlockPos(), soundEvent, entity.getSoundCategory(), 1.0f, pitch);
            } else {
                stopLoopingSound(entity, soundId);
            }
        }, 0, updateFrequencyTicks * 50L, TimeUnit.MILLISECONDS);
    }

    public static void stopLoopingSound(LivingEntity entity, Identifier soundId) {
        ScheduledExecutorService scheduler = soundSchedulers.remove(soundId);
        if (scheduler != null) {
            scheduler.shutdownNow(); // Shut down the scheduler to stop the sound from playing
        }
    }
}

