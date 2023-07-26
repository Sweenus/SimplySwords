package net.sweenus.simplyswords.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimplySwordsConfig {
    private static final HashMap<String, Float> LOOT_OPTIONS = new LinkedHashMap<>();

    public static float getLootModifiers(String key) {
        if (!LOOT_OPTIONS.containsKey(key)) {
            //System.out.println(key);
            return 0f;
        }
        return LOOT_OPTIONS.get(key);
    }

    public static boolean getLootList (String id) {
        if (!LOOT_OPTIONS.isEmpty()) {
            //System.out.println(id);
            return LOOT_OPTIONS.toString().contains(id);
        }
        return false;
    }

    public static void init() {

        LOOT_OPTIONS.put("minecraft:entities/wither", 0.05f);
        LOOT_OPTIONS.put("minecraft:entities/ender_dragon", 0.5f);
        LOOT_OPTIONS.put("minecraft:chests/ruined_portal", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_armorer", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_butcher", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_cartographer", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_desert_house", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_fisher", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_fletcher", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_mason", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_plains_house", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_savanna_house", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_shepard", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_snowy_house", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_taiga_house", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_tannery", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_temple", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_toolsmith", 0f);
        LOOT_OPTIONS.put("minecraft:chests/village/village_weaponsmith", 0f);

    }

    public static void loadConfig() {
        //System.out.println("Loading common Simply Swords config");
        JsonObject json;
        json = Config.getJsonObject(Config.readFile(new File("config/simplyswords_extra/loot_config.json5")));
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            LOOT_OPTIONS.put(entry.getKey(), entry.getValue().getAsFloat());
        }
    }


    public static void generateConfigs(boolean overwrite) {
        //System.out.println("Generating common Simply Swords config");
        StringBuilder config;
        int i;
        config = new StringBuilder("{\n");
        i = 0;
        for (String key : LOOT_OPTIONS.keySet()) {
            if (i == 0) {
                config.append("// --------------------------------------------- LOOT CONFIGURATION ----------------------------------------------------------------- \n");
                config.append("// This config allows for the tweaking of loot injection for UNIQUE weapons. \n");
                config.append("// Standard iron > diamond gear, and Runic Tablets, are controlled by the loot modifiers in the general_config, not here.  \n");
                config.append("// ---------------------------------------------------------------------------------------------------------------------------------- \n");
                config.append("// --------------------------------------------- How does it work? ------------------------------------------------------------------ \n");
                config.append("// If 'add_weapons_to_loot_tables' is enabled in the simplyswords_main config, Simply Swords will attempt to inject its loot into    \n");
                config.append("// any loot table that contains 'chests' in its filepath. This includes loot tables from other mods.\n");
                config.append("// Any values provided in this config will override the aforementioned process, acting as both a whitelist, and a blacklist.\n");
                config.append("\n");
                config.append("// Each line must be made up of a string containing the namespace, path, and filename, in addition to a float value.        \n");
                config.append("// The float value provided will determine the chance of the loot appearing in the loot table. Where 0.0 is %0, and 1.0 is %100.\n");
                config.append("// If the float value provided is 0.0 the items will be blacklisted from this loot table and skipped entirely by the loot injection.\n");
                config.append("\n");
                config.append("// Items can also be injected into entity loot tables, as seen in the Wither example below.\n");
                config.append("// ---------------------------------------------------------------------------------------------------------------------------------- \n");
                config.append("\n");
            }
            config.append("  \"").append(key).append("\": ").append(LOOT_OPTIONS.get(key));
            ++i;
            if (i < LOOT_OPTIONS.size()) {
                config.append(",");
            }
            config.append("\n");
        }
        config.append("}");
        Config.createFile("config/simplyswords_extra/loot_config.json5", config.toString(), overwrite);
    }
}