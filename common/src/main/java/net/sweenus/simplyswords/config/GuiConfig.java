package net.sweenus.simplyswords.config;

import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

@Version(version = ResettingConfig.CURRENT_SCHEMA_VERSION)
public class GuiConfig extends ResettingConfig {

    public GuiConfig() {
        super(new Identifier(SimplySwords.MOD_ID, "gui"));
    }

    @ValidatedInt.Restrict(min = -2000, max = 2000)
    public int xOffset = 0;
    @ValidatedInt.Restrict(min = -2000, max = 2000)
    public int yOffset = 0;
    @ValidatedFloat.Restrict(min = 0.25F, max = 4.0F)
    public float scale = 1.0F;
}
