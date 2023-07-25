package net.sweenus.simplyswords.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;

import java.nio.file.Path;

import static net.sweenus.simplyswords.SimplySwords.MOD_ID;

public class SimplySwordsExpectPlatformImpl {
    /**
     * This is our actual method to {@link SimplySwordsExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
    public static String getVersion() {return FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata().getVersion().toString();}
    public static float getSpellPowerDamage(float damageModifier, PlayerEntity player, String magicSchool) {return FabricHelperMethods.useSpellAttributeScaling(damageModifier, player, magicSchool);}
}
