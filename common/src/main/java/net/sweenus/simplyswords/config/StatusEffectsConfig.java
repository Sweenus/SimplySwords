package net.sweenus.simplyswords.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "status_effects")
public class StatusEffectsConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean enablePlayerExCompatibility = false;
    @ConfigEntry.Gui.Tooltip
    public boolean treeResetOnDeath = false;

    @ConfigEntry.Gui.Tooltip
    public boolean removeUnlockRestrictions = false;

}
