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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Smoother;
import net.minecraft.util.math.random.Random;
import net.sweenus.simplyswords.entity.IonboundStormscaleVisualEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class IonboundBeamClientState {
    private static final double SEARCH_RADIUS = 32.0;
    private static final double LOOK_SCALE = 0.125;
    private static final float FOV_IN_TICKS = 8.0F;
    private static final float FOV_OUT_TICKS = 30.0F;
    private static final double MAX_FOV_ADDITION = 10.0;

    private static final Map<UUID, BeamChannelSound> CHANNEL_SOUNDS = new HashMap<>();
    private static final Smoother LOOK_X_SMOOTHER = new Smoother();
    private static final Smoother LOOK_Y_SMOOTHER = new Smoother();
    private static ClientWorld activeWorld;
    private static boolean ownedBeamActive;
    private static float previousFovIntensity;
    private static float fovIntensity;
    private static boolean initialized;
    private static double lookTimeDelta;
    private static boolean lookSmoothingActive;

    private IonboundBeamClientState() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientTickEvent.CLIENT_POST.register(IonboundBeamClientState::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> stopAllSounds());
    }

    private static void tick(MinecraftClient client) {
        ownedBeamActive = false;
        if (client == null || client.player == null || client.world == null) {
            stopAllSounds();
            return;
        }
        if (client.world != activeWorld) {
            stopAllSounds();
            activeWorld = client.world;
        }

        int ownerId = client.player.getId();
        List<IonboundStormscaleVisualEntity> beams = client.world.getEntitiesByClass(
                IonboundStormscaleVisualEntity.class,
                client.player.getBoundingBox().expand(SEARCH_RADIUS), visual -> visual.isAlive()
                        && visual.getKind() == IonboundStormscaleVisualEntity.BEAM);
        Set<UUID> liveBeams = new HashSet<>();
        for (IonboundStormscaleVisualEntity beam : beams) {
            liveBeams.add(beam.getUuid());
            if (beam.getOwnerId() == ownerId) ownedBeamActive = true;
            CHANNEL_SOUNDS.computeIfAbsent(beam.getUuid(), id -> {
                BeamChannelSound sound = new BeamChannelSound(beam);
                client.getSoundManager().play(sound);
                return sound;
            });
        }
        CHANNEL_SOUNDS.entrySet().removeIf(entry -> {
            if (liveBeams.contains(entry.getKey())) return false;
            entry.getValue().stopNow();
            return true;
        });
        updateFovIntensity();
    }

    public static void updateLookTiming(double timeDelta) {
        lookTimeDelta = timeDelta;
        if (ownedBeamActive == lookSmoothingActive) return;
        lookSmoothingActive = ownedBeamActive;
        clearLookSmoothing();
    }

    public static double scaleLookDeltaX(double delta) {
        return smoothAndScaleLookDelta(LOOK_X_SMOOTHER, delta);
    }

    public static double scaleLookDeltaY(double delta) {
        return smoothAndScaleLookDelta(LOOK_Y_SMOOTHER, delta);
    }

    private static double smoothAndScaleLookDelta(Smoother smoother, double delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        double value = delta;
        if (client.options != null && !client.options.smoothCameraEnabled) {
            double sensitivity = client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
            value = smoother.smooth(delta, lookTimeDelta * sensitivity * sensitivity * sensitivity * 8.0);
        }
        return value * LOOK_SCALE;
    }

    private static void clearLookSmoothing() {
        LOOK_X_SMOOTHER.clear();
        LOOK_Y_SMOOTHER.clear();
    }

    public static boolean isOwnedBeamActive() {
        return ownedBeamActive;
    }

    public static double getFovAddition(float tickDelta) {
        float intensity = MathHelper.lerp(MathHelper.clamp(tickDelta, 0.0F, 1.0F),
                previousFovIntensity, fovIntensity);
        float eased = intensity * intensity * (3.0F - 2.0F * intensity);
        return MAX_FOV_ADDITION * eased;
    }

    private static void updateFovIntensity() {
        previousFovIntensity = fovIntensity;
        if (ownedBeamActive) {
            fovIntensity = Math.min(1.0F, fovIntensity + 1.0F / FOV_IN_TICKS);
        } else {
            fovIntensity = Math.max(0.0F, fovIntensity - 1.0F / FOV_OUT_TICKS);
        }
    }

    private static void stopAllSounds() {
        ownedBeamActive = false;
        CHANNEL_SOUNDS.values().forEach(BeamChannelSound::stopNow);
        CHANNEL_SOUNDS.clear();
        activeWorld = null;
        previousFovIntensity = 0.0F;
        fovIntensity = 0.0F;
        lookTimeDelta = 0.0;
        lookSmoothingActive = false;
        clearLookSmoothing();
    }

    private static final class BeamChannelSound extends MovingSoundInstance {
        private final IonboundStormscaleVisualEntity beam;

        private BeamChannelSound(IonboundStormscaleVisualEntity beam) {
            super(SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(), SoundCategory.PLAYERS, Random.create());
            this.beam = beam;
            this.repeat = false;
            this.relative = false;
            this.attenuationType = SoundInstance.AttenuationType.LINEAR;
            this.volume = 0.72F;
            this.pitch = 0.82F;
            updatePosition();
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }

        @Override
        public void tick() {
            if (!beam.isAlive() || beam.getKind() != IonboundStormscaleVisualEntity.BEAM) {
                setDone();
                return;
            }
            updatePosition();
        }

        private void updatePosition() {
            Entity owner = beam.getOwner();
            Entity source = owner == null ? beam : owner;
            this.x = source.getX();
            this.y = source.getBodyY(0.55);
            this.z = source.getZ();
        }

        private void stopNow() {
            setDone();
        }
    }
}
