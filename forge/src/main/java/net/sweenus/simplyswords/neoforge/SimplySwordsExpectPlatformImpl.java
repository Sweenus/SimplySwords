package net.sweenus.simplyswords.neoforge;


import net.minecraft.entity.player.PlayerEntity;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;

import java.nio.file.Path;

import static net.sweenus.simplyswords.SimplySwords.MOD_ID;

public class SimplySwordsExpectPlatformImpl {
    /**
     * This is our actual method to {@link SimplySwordsExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static String getVersion() {return ModList.get().getModContainerById(MOD_ID).map(it -> it.getModInfo().getVersion().toString()).orElseThrow();}

    public static float getSpellPowerDamage(float damageModifier, PlayerEntity player, String magicSchool) {return ForgeHelperMethods.useSpellAttributeScaling(damageModifier, player, magicSchool);}
}
