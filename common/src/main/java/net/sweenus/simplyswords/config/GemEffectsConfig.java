package net.sweenus.simplyswords.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "gem_effects")
public class GemEffectsConfig implements ConfigData {

    @ConfigEntry.Gui.PrefixText
    public boolean enableEcho = true;
    public boolean enableBerserk = true;
    public boolean enableRadiance = true;
    public boolean enableOnslaught = true;
    public boolean enableNullification = true;

}
