package net.sweenus.simplyswords.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.sweenus.simplyswords.forge.event.SimplySwordsClientEvents;

@Mod("simplyswords")
public class SimplySwordsClientForge {
    public SimplySwordsClientForge() {
        MinecraftForge.EVENT_BUS.register(new SimplySwordsClientEvents());
    }
}
