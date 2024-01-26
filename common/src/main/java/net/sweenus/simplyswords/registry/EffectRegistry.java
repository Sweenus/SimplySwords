package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.effect.*;

public class EffectRegistry {

    public static final DeferredRegister<StatusEffect> EFFECT = DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.STATUS_EFFECT);

    public static final RegistrySupplier<StatusEffect> WILDFIRE = EFFECT.register("wildfire", () ->
            new WildfireEffect(StatusEffectCategory.HARMFUL, 1124687));
    public static final RegistrySupplier<StatusEffect> STORM = EFFECT.register("storm", () ->
            new StormEffect(StatusEffectCategory.HARMFUL, 1124687));

    public static final RegistrySupplier<StatusEffect> FREEZE = EFFECT.register("freeze", () ->
            new FreezeEffect(StatusEffectCategory.HARMFUL, 1124687));
    public static final RegistrySupplier<StatusEffect> WARD = EFFECT.register("ward", () ->
            new WardEffect(StatusEffectCategory.BENEFICIAL, 1124687));
    public static final RegistrySupplier<StatusEffect> IMMOLATION = EFFECT.register("immolation", () ->
            new ImmolationEffect(StatusEffectCategory.BENEFICIAL, 1124687));
    public static final RegistrySupplier<StatusEffect> ECHO = EFFECT.register("echo", () ->
            new EchoEffect(StatusEffectCategory.HARMFUL, 1124687));
    public static final RegistrySupplier<StatusEffect> ONSLAUGHT = EFFECT.register("onslaught", () ->
            new OnslaughtEffect(StatusEffectCategory.BENEFICIAL, 1124687));
    public static final RegistrySupplier<StatusEffect> BATTLE_FATIGUE = EFFECT.register("battle_fatigue", () ->
            new BattleFatigueEffect(StatusEffectCategory.HARMFUL, 1124687));
    public static final RegistrySupplier<StatusEffect> FATAL_FLICKER = EFFECT.register("fatal_flicker", () ->
            new FatalFlickerEffect(StatusEffectCategory.BENEFICIAL, 1124687));

    public static final RegistrySupplier<StatusEffect> SMOULDERING = EFFECT.register("smouldering", () ->
            new SmoulderingEffect(StatusEffectCategory.HARMFUL, 1124687));

}