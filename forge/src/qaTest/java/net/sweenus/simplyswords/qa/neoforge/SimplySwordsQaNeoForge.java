package net.sweenus.simplyswords.qa.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.sweenus.simplyswords.qa.QaClient;
import net.sweenus.simplyswords.qa.QaMod;

@Mod(QaMod.MOD_ID)
public final class SimplySwordsQaNeoForge {
    public SimplySwordsQaNeoForge() {
        QaMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) QaClient.init();
    }
}
