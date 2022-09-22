package net.sweenus.simplyswords;

import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.effect.ModEffects;
import net.sweenus.simplyswords.item.GobberCompat;
import net.sweenus.simplyswords.item.ModItems;
import net.sweenus.simplyswords.item.MythicMetalsCompat;
import net.sweenus.simplyswords.util.ModLootTableModifiers;
import net.sweenus.simplyswords.util.SoundRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SimplySwords implements ModInitializer {
    public static final String MOD_ID = "simplyswords";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    //Sound
    public static final Identifier SWING_OMEN_ONE = new Identifier("simplyswords:swing_omen_one");
    public static SoundEvent EVENT_OMEN_ONE = new SoundEvent(SWING_OMEN_ONE);
    public static final Identifier SWING_OMEN_TWO = new Identifier("simplyswords:swing_omen_two");
    public static SoundEvent EVENT_OMEN_TWO = new SoundEvent(SWING_OMEN_TWO);

    @Override
    public void onInitialize() {

        //CONFIG

        SimplySwordsConfig.init();

        String defaultConfig = """
                {
                  "regen_simplyswords_config_file": false
                }""";

        File configFile = Config.createFile("config/simplyswords/backupconfig.json", defaultConfig, false);
        JsonObject json = Config.getJsonObject(Config.readFile(configFile));

        SimplySwordsConfig.generateConfigs(json == null || !json.has("regen_simplyswords_config_file") || json.get("regen_simplyswords_config_file").getAsBoolean());
        SimplySwordsConfig.loadConfig();

        ModItems.registerModItems();

        if (FabricLoader.getInstance().isModLoaded("mythicmetals")) {
            MythicMetalsCompat.registerModItems();
        }
        if (FabricLoader.getInstance().isModLoaded("gobber2")) {
            GobberCompat.registerModItems();
        }

        //REGISTER

        ModEffects.registerEffects();
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "burn"), ModEffects.BURN);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "plague"), ModEffects.PLAGUE);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "wildfire"), ModEffects.WILDFIRE);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "storm"), ModEffects.STORM);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "electric"), ModEffects.ELECTRIC);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "omen"), ModEffects.OMEN);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "watcher"), ModEffects.WATCHER);

        ModLootTableModifiers.modifyLootTables();

        //SOUND

        SoundRegister.registerSounds();

        Registry.register(Registry.SOUND_EVENT, SimplySwords.SWING_OMEN_ONE, EVENT_OMEN_ONE);
        Registry.register(Registry.SOUND_EVENT, SimplySwords.SWING_OMEN_TWO, EVENT_OMEN_TWO);




    }
}