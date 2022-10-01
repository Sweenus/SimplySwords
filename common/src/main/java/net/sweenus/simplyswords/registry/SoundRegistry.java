package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;

public class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUND = DeferredRegister.create(SimplySwords.MOD_ID, Registry.SOUND_EVENT_KEY);

    public static final RegistrySupplier<SoundEvent> SWING_SMALL = SOUND.register("swing_small", () ->
            new SoundEvent(new Identifier(SimplySwords.MOD_ID, "swing_small")));
    public static final RegistrySupplier<SoundEvent> SWING_SCIFI = SOUND.register("swing_scifi", () ->
            new SoundEvent(new Identifier(SimplySwords.MOD_ID, "swing_scifi")));
    public static final RegistrySupplier<SoundEvent> SWING_NORMAL = SOUND.register("swing_normal", () ->
            new SoundEvent(new Identifier(SimplySwords.MOD_ID, "swing_normal")));
    public static final RegistrySupplier<SoundEvent> SWING_WOOSH = SOUND.register("swing_woosh", () ->
            new SoundEvent(new Identifier(SimplySwords.MOD_ID, "swing_woosh")));
    public static final RegistrySupplier<SoundEvent> THROW_MJOLNIR = SOUND.register("throw_mjolnir", () ->
            new SoundEvent(new Identifier(SimplySwords.MOD_ID, "throw_mjolnir")));
    public static final RegistrySupplier<SoundEvent> THROW_MJOLNIR_SHORT = SOUND.register("throw_mjolnir_short", () ->
            new SoundEvent(new Identifier(SimplySwords.MOD_ID, "throw_mjolnir_short")));
    public static final RegistrySupplier<SoundEvent> THROW_MJOLNIR_LONG = SOUND.register("throw_mjolnir_long", () ->
            new SoundEvent(new Identifier(SimplySwords.MOD_ID, "throw_mjolnir_long")));
    public static final RegistrySupplier<SoundEvent> SWING_OMEN_ONE = SOUND.register("swing_omen_one", () ->
            new SoundEvent(new Identifier(SimplySwords.MOD_ID, "swing_omen_one")));
    public static final RegistrySupplier<SoundEvent> SWING_OMEN_TWO = SOUND.register("swing_omen_two", () ->
            new SoundEvent(new Identifier(SimplySwords.MOD_ID, "swing_omen_two")));

}