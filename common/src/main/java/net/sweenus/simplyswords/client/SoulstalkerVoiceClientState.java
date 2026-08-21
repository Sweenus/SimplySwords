package net.sweenus.simplyswords.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;
import net.sweenus.simplyswords.entity.SoulstalkerStrideEntity;
import net.sweenus.simplyswords.registry.SoulstalkerVoice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class SoulstalkerVoiceClientState {
    private static final Map<UUID, VoiceState> VOICE_STATES = new HashMap<>();

    private static ClientWorld activeWorld;
    private static boolean initialized;

    private SoulstalkerVoiceClientState() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientTickEvent.CLIENT_POST.register(SoulstalkerVoiceClientState::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> stopAll(MinecraftClient.getInstance()));
    }

    private static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            stopAll(client);
            return;
        }
        if (client.world != activeWorld) {
            stopAll(client);
            activeWorld = client.world;
        }

        Set<UUID> liveStrides = new HashSet<>();
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof SoulstalkerStrideEntity stride)) {
                continue;
            }
            liveStrides.add(stride.getUuid());
            VoiceState state = VOICE_STATES.get(stride.getUuid());
            if (state == null) {
                VOICE_STATES.put(stride.getUuid(), new VoiceState(stride));
            } else {
                state.tick(client, stride);
            }
        }

        VOICE_STATES.entrySet().removeIf(entry -> {
            if (liveStrides.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().stop(client);
            return true;
        });
    }

    private static void stopAll(MinecraftClient client) {
        if (client != null) {
            VOICE_STATES.values().forEach(state -> state.stop(client));
        } else {
            VOICE_STATES.values().forEach(VoiceState::stopNow);
        }
        VOICE_STATES.clear();
        activeWorld = null;
    }

    private static boolean hasValidRider(SoulstalkerStrideEntity stride) {
        Entity rider = stride.getControllingPassenger();
        return stride.isAlive() && !stride.isRemoved()
                && rider != null && rider.isAlive();
    }

    private static final class VoiceState {
        private int lastSequence;
        private StrideVoiceSound activeSound;

        private VoiceState(SoulstalkerStrideEntity stride) {
            this.lastSequence = stride.getVoiceSequence();
        }

        private void tick(MinecraftClient client, SoulstalkerStrideEntity stride) {
            if (!hasValidRider(stride)) {
                stop(client);
                lastSequence = stride.getVoiceSequence();
                return;
            }
            int sequence = stride.getVoiceSequence();
            if (sequence == lastSequence) {
                return;
            }
            lastSequence = sequence;
            SoulstalkerVoice voice = SoulstalkerVoice.fromIndex(stride.getVoiceIndex());
            if (voice == null || (voice.isIdle() && stride.isLeapInProgress())) {
                return;
            }
            stop(client);
            activeSound = new StrideVoiceSound(stride, voice);
            client.getSoundManager().play(activeSound);
        }

        private void stop(MinecraftClient client) {
            if (activeSound == null) {
                return;
            }
            activeSound.stopNow();
            client.getSoundManager().stop(activeSound);
            activeSound = null;
        }

        private void stopNow() {
            if (activeSound != null) {
                activeSound.stopNow();
                activeSound = null;
            }
        }
    }

    private static final class StrideVoiceSound extends MovingSoundInstance {
        private final SoulstalkerStrideEntity stride;
        private final SoulstalkerVoice voice;

        private StrideVoiceSound(SoulstalkerStrideEntity stride, SoulstalkerVoice voice) {
            super(voice.getSound(), SoundCategory.PLAYERS, Random.create());
            this.stride = stride;
            this.voice = voice;
            this.repeat = false;
            this.relative = false;
            this.attenuationType = SoundInstance.AttenuationType.LINEAR;
            this.volume = 0.8F;
            this.pitch = 1.0F;
            updatePosition();
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }

        @Override
        public void tick() {
            if (!hasValidRider(stride) || (voice.isIdle() && stride.isLeapInProgress())) {
                setDone();
                return;
            }
            updatePosition();
        }

        private void updatePosition() {
            this.x = stride.getX();
            this.y = stride.getBodyY(0.45);
            this.z = stride.getZ();
        }

        private void stopNow() {
            setDone();
        }
    }
}
