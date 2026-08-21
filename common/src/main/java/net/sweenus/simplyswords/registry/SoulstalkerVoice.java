package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

public enum SoulstalkerVoice {
    LEAP_ATTACK_LAND(SoundRegistry.SOULSTALKER_LEAP_ATTACK_LAND, 100, false),
    IDLE_WARNING_RATTLE(SoundRegistry.SOULSTALKER_IDLE_WARNING_RATTLE, 164, true),
    IDLE_WET_BREATHING(SoundRegistry.SOULSTALKER_IDLE_WET_BREATHING, 326, true),
    IDLE_LOW_HISS(SoundRegistry.SOULSTALKER_IDLE_LOW_HISS, 143, true);

    private static final SoulstalkerVoice[] VALUES = values();
    private static final SoulstalkerVoice[] IDLE_VOICES = {
            IDLE_WARNING_RATTLE, IDLE_WET_BREATHING, IDLE_LOW_HISS
    };

    private final RegistrySupplier<SoundEvent> sound;
    private final int durationTicks;
    private final boolean idle;

    SoulstalkerVoice(RegistrySupplier<SoundEvent> sound, int durationTicks, boolean idle) {
        this.sound = sound;
        this.durationTicks = durationTicks;
        this.idle = idle;
    }

    public SoundEvent getSound() {
        return sound.get();
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public boolean isIdle() {
        return idle;
    }

    public static SoulstalkerVoice fromIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : null;
    }

    public static SoulstalkerVoice randomIdleDifferent(Random random, int previousIndex) {
        int previousIdle = -1;
        for (int index = 0; index < IDLE_VOICES.length; index++) {
            if (IDLE_VOICES[index].ordinal() == previousIndex) {
                previousIdle = index;
                break;
            }
        }
        if (previousIdle < 0) {
            return IDLE_VOICES[random.nextInt(IDLE_VOICES.length)];
        }
        int index = random.nextInt(IDLE_VOICES.length - 1);
        if (index >= previousIdle) {
            index++;
        }
        return IDLE_VOICES[index];
    }
}
