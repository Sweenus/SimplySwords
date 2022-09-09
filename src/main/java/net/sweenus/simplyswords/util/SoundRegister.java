package net.sweenus.simplyswords.util;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;

import java.util.List;

public class SoundRegister {
    public static List<String> soundKeys = List.of(
            "sound_name",
            "sound_name2"
    );

    public static void registerSounds() {
        for (var soundKey: soundKeys) {
            var soundId = new Identifier(SimplySwords.MOD_ID, soundKey);
            var soundEvent = new SoundEvent(soundId);
            Registry.register(Registry.SOUND_EVENT, soundId, soundEvent);
        }
    }
}

