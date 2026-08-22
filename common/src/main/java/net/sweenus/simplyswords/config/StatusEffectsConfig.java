package net.sweenus.simplyswords.config;


import me.fzzyhmstrs.fzzy_config.annotations.Version;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

@Version(version = ResettingConfig.CURRENT_SCHEMA_VERSION)
public class StatusEffectsConfig extends ResettingConfig {

    public StatusEffectsConfig() {
        super(Identifier.of(SimplySwords.MOD_ID, "status_effects"));
    }

    public int echoDamage = 2;

}
