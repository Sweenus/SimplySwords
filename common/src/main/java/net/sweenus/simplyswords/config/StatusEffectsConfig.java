package net.sweenus.simplyswords.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "status_effects")
public class StatusEffectsConfig implements ConfigData {

    public int echoDamage = 2;

}
