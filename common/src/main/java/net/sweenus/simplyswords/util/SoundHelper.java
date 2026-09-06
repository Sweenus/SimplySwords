package net.sweenus.simplyswords.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SoundHelper {


    // There has to be a better way to loop sounds....
    private static final Map<SoundKey, LoopingSound> soundSchedulers = new ConcurrentHashMap<>();

    public static void loopSound(LivingEntity entity, Identifier soundId, int soundDurationSeconds, int updateFrequencyTicks) {
        loopSound(entity, soundId, soundDurationSeconds, updateFrequencyTicks, entity.getPos());
    }

    public static void loopSound(LivingEntity entity, Identifier soundId, int soundDurationSeconds,
                                 int updateFrequencyTicks, Vec3d position) {
        if (entity.getWorld().isClient()) return;

        ServerWorld serverWorld = (ServerWorld) entity.getWorld();
        SoundEvent soundEvent = SoundRegistry.SOUND.getRegistrar().get(soundId);
        SoundKey key = new SoundKey(entity.getUuid(), soundId);
        LoopingSound existing = soundSchedulers.get(key);

        if (existing != null) {
            existing.position = position;
            return;
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        LoopingSound loopingSound = new LoopingSound(scheduler, position);
        existing = soundSchedulers.putIfAbsent(key, loopingSound);
        if (existing != null) {
            existing.position = position;
            scheduler.shutdownNow();
            return;
        }

        int totalTicks = soundDurationSeconds * 20;
        int[] elapsedTicks = {0};

        scheduler.scheduleAtFixedRate(() -> {
            elapsedTicks[0] += updateFrequencyTicks;

            if (entity.isAlive() && elapsedTicks[0] < totalTicks && entity.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.ELEMENTAL_VORTEX))) {
                float playbackProgress = (elapsedTicks[0] % totalTicks) / (float) totalTicks;
                float pitch = 1.0f + playbackProgress * 0.2f;

                // Play the sound, updating its position dynamically and simulating playback continuity
                Vec3d currentPosition = loopingSound.position;
                serverWorld.playSound(null, currentPosition.x, currentPosition.y, currentPosition.z,
                        soundEvent, entity.getSoundCategory(), 1.0f, pitch);
            } else {
                stopLoopingSound(entity, soundId);
            }
        }, 0, updateFrequencyTicks * 50L, TimeUnit.MILLISECONDS);
    }

    public static void stopLoopingSound(LivingEntity entity, Identifier soundId) {
        if (entity == null) return;
        LoopingSound loopingSound = soundSchedulers.remove(new SoundKey(entity.getUuid(), soundId));
        if (loopingSound != null) {
            loopingSound.scheduler.shutdownNow(); // Shut down the scheduler to stop the sound from playing
        }
    }

    private record SoundKey(UUID sourceId, Identifier soundId) {
    }

    private static final class LoopingSound {
        private final ScheduledExecutorService scheduler;
        private volatile Vec3d position;

        private LoopingSound(ScheduledExecutorService scheduler, Vec3d position) {
            this.scheduler = scheduler;
            this.position = position;
        }
    }
}
