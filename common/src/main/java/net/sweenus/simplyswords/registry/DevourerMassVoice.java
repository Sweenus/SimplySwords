package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

public enum DevourerMassVoice {
    AGGRESSIVE_SNARL(SoundRegistry.DEVOURER_MASS_AGGRESSIVE_SNARL, 155),
    BELLOW_VARIATION_01(SoundRegistry.DEVOURER_MASS_BELLOW_VARIATION_01, 179),
    BELLOW_VARIATION_02(SoundRegistry.DEVOURER_MASS_BELLOW_VARIATION_02, 192),
    HEAVY_BREATHING(SoundRegistry.DEVOURER_MASS_HEAVY_BREATHING, 183),
    WARNING_RUMBLE(SoundRegistry.DEVOURER_MASS_WARNING_RUMBLE, 170);

    private static final DevourerMassVoice[] VALUES = values();

    private final RegistrySupplier<SoundEvent> sound;
    private final int durationTicks;

    DevourerMassVoice(RegistrySupplier<SoundEvent> sound, int durationTicks) {
        this.sound = sound;
        this.durationTicks = durationTicks;
    }

    public SoundEvent getSound() {
        return sound.get();
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public static DevourerMassVoice fromIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : null;
    }

    public static DevourerMassVoice randomDifferent(Random random, int previousIndex) {
        if (previousIndex < 0 || previousIndex >= VALUES.length) {
            return VALUES[random.nextInt(VALUES.length)];
        }
        int index = random.nextInt(VALUES.length - 1);
        if (index >= previousIndex) {
            index++;
        }
        return VALUES[index];
    }
}
