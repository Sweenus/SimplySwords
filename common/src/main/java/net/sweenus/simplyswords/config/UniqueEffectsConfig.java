package net.sweenus.simplyswords.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "unique_effects")
public class UniqueEffectsConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public float abilityAbsorptionCap = 20f;

    @ConfigEntry.Gui.PrefixText
    public float watcherChance = 5f;
    public float watcherRestoreAmount = 0.5f;
    public float watcherRadius = 8f;
    public float omenChance = 5f;
    public float omenAbsorptionCap = 20f;
    public float omenInstantKillThreshold = 0.25f;

    @ConfigEntry.Gui.PrefixText
    public float stealChance = 25f;
    public float stealDuration = 400f;
    public float stealInvisDuration = 120f;
    public float stealBlindDuration = 200f;
    public float stealRadius = 30f;
    public float stealSpellScaling = 2.6f;

    @ConfigEntry.Gui.PrefixText
    public float soulMeldChance = 75f;
    public float soulMeldDuration = 250f;
    public float soulMeldRadius = 5f;

    @ConfigEntry.Gui.PrefixText
    public float soulrendChance = 85f;
    public float soulrendDuration = 500f;
    public float soulrendDamageMulti = 3f;
    public float soulrendHealMulti = 0.5f;
    public float soulrendRadius = 10f;
    public float soulrendMaxStacks = 8f;
    public float soulrendDamageSpellScaling = 0.4f;

    @ConfigEntry.Gui.PrefixText
    public float ferocityChance = 75f;
    public float ferocityDuration = 100f;
    public float ferocityMaxStacks = 15f;
    public float ferocityStrengthTier = 2f;

    @ConfigEntry.Gui.PrefixText
    public float emberIreChance = 30f;
    public float emberIreDuration = 150f;

    @ConfigEntry.Gui.PrefixText
    public float volcanicFuryChance = 25f;
    public float volcanicFuryRadius = 3f;
    public float volcanicFuryCooldown = 300f;
    public float volcanicFuryDamage = 3f;
    public float volcanicFurySpellScaling = 1.4f;

    @ConfigEntry.Gui.PrefixText
    public float stormChance = 15f;
    public float stormRadius = 10f;

    @ConfigEntry.Gui.PrefixText
    public float plagueChance = 55f;

    @ConfigEntry.Gui.PrefixText
    public float brimstoneChance = 15f;

    @ConfigEntry.Gui.PrefixText
    public float brambleChance = 45f;
    public float brambleRadius = 10f;

    @ConfigEntry.Gui.PrefixText
    public float soultetherRange = 32f;
    public float soultetherRadius = 8f;
    public float soultetherDuration = 120f;
    public float soultetherIgniteDuration = 120f;
    public float soultetherResistanceDuration = 60f;

    @ConfigEntry.Gui.PrefixText
    public float frostFuryCooldown = 380f;
    public float frostFuryRadius = 3f;
    public float frostFuryDamage = 18f;
    public float frostFuryChance = 15f;
    public float frostFuryDuration = 80f;
    public float frostFurySpellScaling = 1.4f;

    @ConfigEntry.Gui.PrefixText
    public float moltenRoarCooldown = 320f;
    public float moltenRoarRadius = 5f;
    public float moltenRoarKnockbackStrength = 5f;
    public float moltenRoarChance = 15f;
    public float moltenRoarDuration = 100f;

    @ConfigEntry.Gui.PrefixText
    public float frostShatterRadius = 3f;
    public float frostShatterDamage = 18f;
    public float frostShatterChance = 15f;
    public float frostShatterDuration = 80f;
    public float frostShatterSpellScaling = 1.7f;

    @ConfigEntry.Gui.PrefixText
    public float permafrostRadius = 4f;
    public float permafrostDamage = 1f;
    public float permafrostCooldown = 600f;
    public float permafrostDuration = 200f;
    public float permafrostSpellScaling = 0.9f;

    @ConfigEntry.Gui.PrefixText
    public float arcaneAssaultRadius = 6f;
    public float arcaneAssaultDamage = 1f;
    public float arcaneAssaultCooldown = 220;
    public float arcaneAssaultChance = 25f;
    public float arcaneAssaultDuration = 120f;
    public float arcaneAssaultSpellScaling = 1.4f;

    @ConfigEntry.Gui.PrefixText
    public float thunderBlitzRadius = 2f;
    public float thunderBlitzDamage = 3f;
    public float thunderBlitzCooldown = 250;
    public float thunderBlitzChance = 15f;
    public float thunderBlitzSpellScaling = 1.7f;

    @ConfigEntry.Gui.PrefixText
    public float stormJoltCooldown = 100f;
    public float stormJoltChance = 15f;

    @ConfigEntry.Gui.PrefixText
    public float soulAnguishRadius = 3f;
    public float soulAnguishAbsorptionCap = 8f;
    public float soulAnguishDamage = 4f;
    public float soulAnguishCooldown = 700;
    public float soulAnguishDuration = 200f;
    public float soulAnguishHeal = 0.5f;
    public float soulAnguishRange = 22f;
    public float soulAnguishSpellScaling = 1.6f;

    @ConfigEntry.Gui.PrefixText
    public float shockDeflectBlockDuration = 35f;
    public float shockDeflectDamage = 12f;
    public float shockDeflectCooldown = 90;
    public float shockDeflectParryDuration = 10f;
    public float shockDeflectSpellScaling = 2.3f;

    @ConfigEntry.Gui.PrefixText
    public float shadowmistCooldown = 200f;
    public float shadowmistChance = 25f;
    public float shadowmistDamageMulti = 0.8f;
    public float shadowmistBlindDuration = 60f;
    public float shadowmistRadius = 4f;

    @ConfigEntry.Gui.PrefixText
    public float abyssalStandardCooldown = 700;
    public float abyssalStandardChance = 15f;
    public float abyssalStandardDamage = 3f;
    public float abyssalStandardSpellScaling = 1.2f;

    @ConfigEntry.Gui.PrefixText
    public float righteousStandardCooldown = 700f;
    public float righteousStandardChance = 15f;
    public float righteousStandardDamage = 3f;
    public float righteousStandardSpellScaling = 1.1f;
    public float righteousStandardSpellScalingHeal = 1.3f;

    @ConfigEntry.Gui.PrefixText
    public float fatalFlickerCooldown = 175f;
    public float fatalFlickerChance = 15f;
    public float fatalFlickerRadius = 3f;
    public float fatalFlickerMaxStacks = 99f;
    public float fatalFlickerDashVelocity = 3f;

    @ConfigEntry.Gui.PrefixText
    public float smoulderCooldown = 80f;
    public float smoulderMaxStacks = 5f;
    public float smoulderHeal = 15f;
    public float smoulderSpellScaling = 1.0f;

}
