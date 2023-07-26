package net.sweenus.simplyswords.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "general")
public class GeneralConfig implements ConfigData {


    public boolean enableWeaponImpactSounds = true;
    public boolean enableWeaponFootfalls = true;
    public boolean enablePassiveParticles = true;
    @ConfigEntry.Gui.Tooltip
    public boolean enableUniqueGemSockets = true;
    public boolean compatGobberEndWeaponsUnbreakable = true;
    @ConfigEntry.Gui.Tooltip
    public boolean compatEnableSpellPowerScaling = true;


}
