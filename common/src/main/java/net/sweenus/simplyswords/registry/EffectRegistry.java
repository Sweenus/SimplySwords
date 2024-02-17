package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
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
    public static final RegistrySupplier<StatusEffect> FRENZY = EFFECT.register("frenzy", () ->
            new FrenzyEffect(StatusEffectCategory.BENEFICIAL, 1124687)
                    .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED,
                            "54e1b9b9-6de9-49bb-a716-564b3d375892",
                            0.8,
                            EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final RegistrySupplier<StatusEffect> VOIDCLOAK = EFFECT.register("voidcloak", () ->
            new VoidcloakEffect(StatusEffectCategory.BENEFICIAL, 1124687)
                    .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED,
                            "d07f34ad-6367-4a86-b47a-736947e2c008",
                            0.1,
                            EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final RegistrySupplier<StatusEffect> VOIDASSAULT = EFFECT.register("void_assault", () ->
            new VoidAssaultEffect(StatusEffectCategory.HARMFUL, 1124687));

    public static final RegistrySupplier<StatusEffect> FIRE_VORTEX = EFFECT.register("fire_vortex", () ->
            new FireVortexEffect(StatusEffectCategory.HARMFUL, 1124687)
                    .addAttributeModifier(EntityAttributes.GENERIC_ARMOR,
                            "f20d79bc-5f73-49d3-9e3f-30bf9a8da15a",
                            -0.01,
                            EntityAttributeModifier.Operation.MULTIPLY_TOTAL)
                    .addAttributeModifier(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
                            "3f68cd63-6fc5-4a23-87bd-c5902579d9db",
                            85,
                            EntityAttributeModifier.Operation.MULTIPLY_TOTAL));

    public static final RegistrySupplier<StatusEffect> FROST_VORTEX = EFFECT.register("frost_vortex", () ->
            new FrostVortexEffect(StatusEffectCategory.HARMFUL, 1124687)
                    .addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                            "d0814391-9325-441e-bc7e-ace3f8f89a21",
                            -0.01,
                            EntityAttributeModifier.Operation.MULTIPLY_TOTAL)
                    .addAttributeModifier(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
                            "8b1fc18c-9539-4718-af61-224d0ccd274f",
                            85,
                            EntityAttributeModifier.Operation.MULTIPLY_TOTAL));

    public static final RegistrySupplier<StatusEffect> ELEMENTAL_VORTEX = EFFECT.register("elemental_vortex", () ->
            new ElementalVortexEffect(StatusEffectCategory.BENEFICIAL, 1124687));

    public static final RegistrySupplier<StatusEffect> FLAMESEED = EFFECT.register("flameseed", () ->
            new FlameSeedEffect(StatusEffectCategory.HARMFUL, 1124687));

    public static final RegistrySupplier<StatusEffect> RIBBONWRATH = EFFECT.register("ribbonwrath", () ->
            new RibbonwrathEffect(StatusEffectCategory.HARMFUL, 1124687)
                    .addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                            "325de159-03bd-421c-8dd0-53e0090857ed",
                            -0.05,
                            EntityAttributeModifier.Operation.MULTIPLY_TOTAL));

    public static final RegistrySupplier<StatusEffect> RIBBONCLEAVE = EFFECT.register("ribboncleave", () ->
            new RibboncleaveEffect(StatusEffectCategory.BENEFICIAL, 1124687)
                    .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                            "c8fb5e9f-c446-4475-b73f-a2290196210f",
                            2,
                            EntityAttributeModifier.Operation.MULTIPLY_TOTAL));

}