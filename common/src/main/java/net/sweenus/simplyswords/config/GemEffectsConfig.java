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

    @ConfigEntry.Gui.PrefixText
    public boolean enablePrecise = true;
    @ConfigEntry.BoundedDiscrete(max = 100)
    public int preciseChance = 30;
    public boolean enableMighty = true;
    @ConfigEntry.BoundedDiscrete(max = 100)
    public int mightyChance = 30;
    public boolean enableStealthy = true;
    @ConfigEntry.BoundedDiscrete(max = 100)
    public int stealthyChance = 30;
    public boolean enableRenewed = true;
    @ConfigEntry.BoundedDiscrete(max = 100)
    public int renewedChance = 30;
    public boolean enableAccelerant = true;
    public boolean enableLeaping = true;
    @ConfigEntry.BoundedDiscrete(max = 100)
    public int leapingChance = 65;
    public boolean enableSpellshield = true;
    @ConfigEntry.BoundedDiscrete(max = 100)
    public int spellshieldChance = 15;
    public boolean enableSpellforged = true;
    public boolean enableSoulshock = true;
    public boolean enableSpellStandard = true;
    public boolean enableWarStandard = true;
    public boolean enableDeception = true;

}
