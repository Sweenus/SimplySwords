package net.sweenus.simplyswords.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "runic_effects")
public class RunicEffectsConfig implements ConfigData {

    @ConfigEntry.Gui.PrefixText
    public boolean enableActiveDefence = true;
    public boolean enableFloat = true;
    public boolean enableGreaterFloat = true;
    public boolean enableFreeze = true;
    public boolean enableShielding = true;
    public boolean enableGreaterShielding = true;
    public boolean enableSlow = true;
    public boolean enableGreaterSlow = true;
    public boolean enableStoneskin = true;
    public boolean enableGreaterStoneskin = true;
    public boolean enableSwiftness = true;
    public boolean enableGreaterSwiftness = true;
    public boolean enableTrailblaze = true;
    public boolean enableGreaterTrailblaze = true;
    public boolean enableWeaken = true;
    public boolean enableGreaterWeaken = true;
    public boolean enableZephyr = true;
    public boolean enableGreaterZephyr = true;
    public boolean enableFrostWard = true;
    public boolean enableWildfire = true;
    public boolean enableUnstable = true;
    public boolean enableMomentum = true;
    public boolean enableGreaterMomentum = true;
    public boolean enableImbued = true;
    public boolean enableGreaterImbued = true;
    public boolean enablePincushion = true;
    public boolean enableGreaterPincushion = true;
    public boolean enableWard = true;
    public boolean enableImmolate = true;


    @ConfigEntry.Gui.PrefixText
    public float swiftnessChance = 15f;
    public float swiftnessDuration= 300f;

    @ConfigEntry.Gui.PrefixText
    public float slowChance = 50f;
    public float slowDuration= 50f;

    @ConfigEntry.Gui.PrefixText
    public float poisonChance = 15f;
    public float poisonDuration = 150f;

    @ConfigEntry.Gui.PrefixText
    public float freezeChance = 15f;
    public float freezeDuration = 120f;

    @ConfigEntry.Gui.PrefixText
    public float wildfireChance = 10f;
    public float wildfireDuration = 180f;
    public float wildfireRadius = 10;

    @ConfigEntry.Gui.PrefixText
    public float floatChance = 15f;
    public float floatDuration = 50f;

    @ConfigEntry.Gui.PrefixText
    public float zephyrChance = 15f;
    public float zephyrDuration = 180f;

    @ConfigEntry.Gui.PrefixText
    public float shieldingChance = 15f;
    public float shieldingDuration = 120f;

    @ConfigEntry.Gui.PrefixText
    public float stoneskinChance = 15f;
    public float stoneskinDuration = 60f;

    @ConfigEntry.Gui.PrefixText
    public float trailblazeChance = 15f;
    public float trailblazeDuration = 120;

    @ConfigEntry.Gui.PrefixText
    public float weakenChance = 15f;
    public float weakenDuration = 120f;

    @ConfigEntry.Gui.PrefixText
    public float unstableFrequency = 140f;
    public float unstableDuration = 140f;

    @ConfigEntry.Gui.PrefixText
    public float activeDefenceFrequency = 20f;
    public float activeDefenceRadius = 5f;

    @ConfigEntry.Gui.PrefixText
    public float frostWardFrequency = 20f;
    public float frostWardRadius = 5f;
    public float frostWardDuration = 60f;

    @ConfigEntry.Gui.PrefixText
    public float momentumCooldown = 140f;

    @ConfigEntry.Gui.PrefixText
    public float imbuedChance = 15;


}
