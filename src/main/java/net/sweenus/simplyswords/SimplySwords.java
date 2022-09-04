package net.sweenus.simplyswords;

import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.effect.ModEffects;
import net.sweenus.simplyswords.item.ModItems;
import net.sweenus.simplyswords.item.MythicMetalsCompat;
import net.sweenus.simplyswords.util.ModLootTableModifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SimplySwords implements ModInitializer {
    public static final String MOD_ID = "simplyswords";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        ModItems.registerModItems();

		if (FabricLoader.getInstance().isModLoaded("mythicmetals")) {
			MythicMetalsCompat.registerModItems();
		}

        ModEffects.registerEffects();
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "burn"), ModEffects.BURN);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "plague"), ModEffects.PLAGUE);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "wildfire"), ModEffects.WILDFIRE);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "storm"), ModEffects.STORM);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "electric"), ModEffects.ELECTRIC);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "omen"), ModEffects.OMEN);
        Registry.register(Registry.STATUS_EFFECT, new Identifier("simplyswords", "watcher"), ModEffects.WATCHER);

        ModLootTableModifiers.modifyLootTables();



        //CONFIG

        SimplySwordsConfig.init();

        String defaultConfig = "{\n" + "  \"regen_simplyswords_config_file\": false\n" + "}";

        File configFile = Config.createFile("config/simplyswords/backupconfig.json", defaultConfig, false);
        JsonObject json = Config.getJsonObject(Config.readFile(configFile));

        SimplySwordsConfig.generateConfigs(json == null || !json.has("regen_simplyswords_config_file") || json.get("regen_simplyswords_config_file").getAsBoolean());
        SimplySwordsConfig.loadConfig();


    }
}