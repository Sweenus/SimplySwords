package net.sweenus.simplyswords.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "gem_effects")
public class GemEffectsConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean enablePlayerExCompatibility = false;
    @ConfigEntry.Gui.Tooltip
    public boolean treeResetOnDeath = false;

    @ConfigEntry.Gui.Tooltip
    public boolean removeUnlockRestrictions = false;

}
