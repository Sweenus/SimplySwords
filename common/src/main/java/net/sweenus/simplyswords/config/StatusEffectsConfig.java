package net.sweenus.simplyswords.config;


import me.fzzyhmstrs.fzzy_config.config.Config;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

public class StatusEffectsConfig extends Config {

    public StatusEffectsConfig() {
        super(new Identifier(SimplySwords.MOD_ID, "status_effects"));
    }

    public int echoDamage = 2;

}