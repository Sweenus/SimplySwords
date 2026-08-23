package net.sweenus.simplyswords.qa.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.sweenus.simplyswords.qa.QaClient;
import net.sweenus.simplyswords.qa.QaMod;

@Mod(QaMod.MOD_ID)
public final class SimplySwordsQaForge {
    public SimplySwordsQaForge() {
        QaMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) QaClient.init();
    }
}
