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
import net.sweenus.simplyswords.entity.DevourerMassVisualEntity;
import net.sweenus.simplyswords.registry.DevourerMassVoice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class DevourerMassVoiceClientState {
    private static final Map<UUID, VoiceState> VOICE_STATES = new HashMap<>();

    private static ClientWorld activeWorld;
    private static boolean initialized;

    private DevourerMassVoiceClientState() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientTickEvent.CLIENT_POST.register(DevourerMassVoiceClientState::tick);
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

        Set<UUID> liveMasses = new HashSet<>();
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof DevourerMassVisualEntity mass)) {
                continue;
            }
            liveMasses.add(mass.getUuid());
            VoiceState state = VOICE_STATES.get(mass.getUuid());
            if (state == null) {
                VOICE_STATES.put(mass.getUuid(), new VoiceState(mass));
            } else {
                state.tick(client, mass);
            }
        }

        VOICE_STATES.entrySet().removeIf(entry -> {
            if (liveMasses.contains(entry.getKey())) {
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

    private static final class VoiceState {
        private int lastSequence;
        private MassVoiceSound activeSound;

        private VoiceState(DevourerMassVisualEntity mass) {
            this.lastSequence = mass.getVoiceSequence();
        }

        private void tick(MinecraftClient client, DevourerMassVisualEntity mass) {
            if (!mass.isAlive() || mass.isCollapsing()) {
                stop(client);
                lastSequence = mass.getVoiceSequence();
                return;
            }
            int sequence = mass.getVoiceSequence();
            if (sequence == lastSequence) {
                return;
            }
            lastSequence = sequence;
            DevourerMassVoice voice = DevourerMassVoice.fromIndex(mass.getVoiceIndex());
            if (voice == null) {
                return;
            }
            stop(client);
            activeSound = new MassVoiceSound(mass, voice);
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

    private static final class MassVoiceSound extends MovingSoundInstance {
        private final DevourerMassVisualEntity mass;

        private MassVoiceSound(DevourerMassVisualEntity mass, DevourerMassVoice voice) {
            super(voice.getSound(), SoundCategory.PLAYERS, Random.create());
            this.mass = mass;
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
            if (!mass.isAlive() || mass.isCollapsing()) {
                setDone();
                return;
            }
            updatePosition();
        }

        private void updatePosition() {
            this.x = mass.getX();
            this.y = mass.getY();
            this.z = mass.getZ();
        }

        private void stopNow() {
            setDone();
        }
    }
}
