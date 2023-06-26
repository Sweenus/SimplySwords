package net.sweenus.simplyswords.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimplySwordsConfig {
    private static final HashMap<String, Boolean> BOOLEAN_OPTIONS = new LinkedHashMap<>();
    private static final HashMap<String, Float> GENERAL_OPTIONS = new LinkedHashMap<>();
    private static final HashMap<String, Float> FLOAT_OPTIONS = new LinkedHashMap<>();
    private static final HashMap<String, Float> WEAPON_OPTIONS = new LinkedHashMap<>();
    private static final HashMap<String, Float> LOOT_OPTIONS = new LinkedHashMap<>();
    private static final int runicOptionsCount = 31;

    public static boolean getBooleanValue(String key) {
        if (!BOOLEAN_OPTIONS.containsKey(key)) {
            System.out.println(key);
        }
        return BOOLEAN_OPTIONS.getOrDefault(key, null);
    }

    public static float getGeneralSettings(String key) {
        if (!GENERAL_OPTIONS.containsKey(key)) {
            System.out.println(key);
        }
        return GENERAL_OPTIONS.getOrDefault(key, null);
    }

    public static float getFloatValue(String key) {
        if (!FLOAT_OPTIONS.containsKey(key)) {
            System.out.println(key);
        }
        return FLOAT_OPTIONS.getOrDefault(key, null);
    }

    public static float getWeaponAttributes(String key) {
        if (!WEAPON_OPTIONS.containsKey(key)) {
            System.out.println(key);
        }
        return WEAPON_OPTIONS.get(key);
    }

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

        //When changing these increment the runicOptions int

        FLOAT_OPTIONS.put("speed_chance", 15f);
        FLOAT_OPTIONS.put("speed_duration", 300f);


        FLOAT_OPTIONS.put("slowness_chance", 50f);
        FLOAT_OPTIONS.put("slowness_duration", 50f);


        FLOAT_OPTIONS.put("toxin_chance", 15f);
        FLOAT_OPTIONS.put("toxin_duration", 150f);


        FLOAT_OPTIONS.put("freeze_chance", 15f);
        FLOAT_OPTIONS.put("freeze_duration", 120f);


        FLOAT_OPTIONS.put("wildfire_chance", 10f);
        FLOAT_OPTIONS.put("wildfire_duration", 180f);
        FLOAT_OPTIONS.put("wildfire_radius", 10f);

        FLOAT_OPTIONS.put("levitation_chance", 15f);
        FLOAT_OPTIONS.put("levitation_duration", 50f);

        FLOAT_OPTIONS.put("zephyr_chance", 15f);
        FLOAT_OPTIONS.put("zephyr_duration", 180f);

        FLOAT_OPTIONS.put("shielding_chance", 15f);
        FLOAT_OPTIONS.put("shielding_duration", 60f);

        FLOAT_OPTIONS.put("stoneskin_chance", 15f);
        FLOAT_OPTIONS.put("stoneskin_duration", 120f);

        FLOAT_OPTIONS.put("trailblaze_chance", 15f);
        FLOAT_OPTIONS.put("trailblaze_duration", 120f);

        FLOAT_OPTIONS.put("weaken_chance", 15f);
        FLOAT_OPTIONS.put("weaken_duration", 120f);

        FLOAT_OPTIONS.put("unstable_frequency", 140f);
        FLOAT_OPTIONS.put("unstable_duration", 140f);

        FLOAT_OPTIONS.put("active_defence_frequency", 20f);
        FLOAT_OPTIONS.put("active_defence_radius", 5f);

        FLOAT_OPTIONS.put("frostward_frequency", 20f);
        FLOAT_OPTIONS.put("frostward_radius", 5f);
        FLOAT_OPTIONS.put("frostward_slow_duration", 60f);

        FLOAT_OPTIONS.put("momentum_cooldown", 140f);

        FLOAT_OPTIONS.put("imbued_chance", 15f);

        FLOAT_OPTIONS.put("watcher_chance", 5f);
        FLOAT_OPTIONS.put("watcher_restore_amount", 0.5f);
        FLOAT_OPTIONS.put("watcher_radius", 8f);

        FLOAT_OPTIONS.put("omen_chance", 75f);
        FLOAT_OPTIONS.put("omen_absorption_amount", 1f);
        FLOAT_OPTIONS.put("omen_instantkill_threshold", 0.25f);

        FLOAT_OPTIONS.put("steal_chance", 25f);
        FLOAT_OPTIONS.put("steal_duration", 400f);
        FLOAT_OPTIONS.put("steal_invis_duration", 120f);
        FLOAT_OPTIONS.put("steal_blind_duration", 200f);
        FLOAT_OPTIONS.put("steal_radius", 30f);

        FLOAT_OPTIONS.put("gravity_chance", 35f);
        FLOAT_OPTIONS.put("gravity_duration", 250f);
        FLOAT_OPTIONS.put("gravity_radius", 10f);

        FLOAT_OPTIONS.put("soulmeld_chance", 75f);
        FLOAT_OPTIONS.put("soulmeld_duration", 250f);
        FLOAT_OPTIONS.put("soulmeld_radius", 5f);

        FLOAT_OPTIONS.put("soulrend_chance", 85f);
        FLOAT_OPTIONS.put("soulrend_duration", 500f);
        FLOAT_OPTIONS.put("soulrend_rend_damage_multiplier", 3f);
        FLOAT_OPTIONS.put("soulrend_rend_heal_multiplier", 0.5f);
        FLOAT_OPTIONS.put("soulrend_radius", 10f);
        FLOAT_OPTIONS.put("soulrend_max_stacks", 8f);

        FLOAT_OPTIONS.put("ferocity_chance", 75f);
        FLOAT_OPTIONS.put("ferocity_duration", 100f);
        FLOAT_OPTIONS.put("ferocity_max_stacks", 15f);
        FLOAT_OPTIONS.put("ferocity_strength_tier", 2f);

        FLOAT_OPTIONS.put("ember_ire_chance", 30f);
        FLOAT_OPTIONS.put("ember_ire_duration", 150f);

        FLOAT_OPTIONS.put("volcanic_fury_chance", 25f);
        FLOAT_OPTIONS.put("volcanic_fury_radius", 3f);
        FLOAT_OPTIONS.put("volcanic_fury_cooldown", 300f);
        FLOAT_OPTIONS.put("volcanic_fury_damage", 3f);

        FLOAT_OPTIONS.put("storm_chance", 15f);
        FLOAT_OPTIONS.put("storm_radius", 10f);

        FLOAT_OPTIONS.put("plague_chance", 55f);

        FLOAT_OPTIONS.put("brimstone_chance", 15f);

        FLOAT_OPTIONS.put("bramble_chance", 45f);
        FLOAT_OPTIONS.put("bramble_radius", 10f);

        FLOAT_OPTIONS.put("soultether_range", 32f);
        FLOAT_OPTIONS.put("soultether_radius", 8f);
        FLOAT_OPTIONS.put("soultether_duration", 120f);
        FLOAT_OPTIONS.put("soultether_ignite_duration", 120f);
        FLOAT_OPTIONS.put("soultether_resistance_duration", 60f);

        FLOAT_OPTIONS.put("frostfury_cooldown", 380f);
        FLOAT_OPTIONS.put("frostfury_radius", 3f);
        FLOAT_OPTIONS.put("frostfury_damage", 18f);
        FLOAT_OPTIONS.put("frostfury_chance", 15f);
        FLOAT_OPTIONS.put("frostfury_duration", 80f);

        FLOAT_OPTIONS.put("moltenroar_cooldown", 320f);
        FLOAT_OPTIONS.put("moltenroar_radius", 5f);
        FLOAT_OPTIONS.put("moltenroar_knockback_strength", 5f);
        FLOAT_OPTIONS.put("moltenroar_chance", 15f);
        FLOAT_OPTIONS.put("moltenroar_duration", 100f);

        FLOAT_OPTIONS.put("frostshatter_radius", 3f);
        FLOAT_OPTIONS.put("frostshatter_damage", 18f);
        FLOAT_OPTIONS.put("frostshatter_chance", 15f);
        FLOAT_OPTIONS.put("frostshatter_duration", 80f);

        FLOAT_OPTIONS.put("permafrost_radius", 4f);
        FLOAT_OPTIONS.put("permafrost_damage", 1f);
        FLOAT_OPTIONS.put("permafrost_cooldown", 600f);
        FLOAT_OPTIONS.put("permafrost_duration", 200f);

        FLOAT_OPTIONS.put("arcaneassault_radius", 6f);
        FLOAT_OPTIONS.put("arcaneassault_damage", 1f);
        FLOAT_OPTIONS.put("arcaneassault_cooldown", 220f);
        FLOAT_OPTIONS.put("arcaneassault_chance", 25f);
        FLOAT_OPTIONS.put("arcaneassault_duration", 120f);

        FLOAT_OPTIONS.put("thunderblitz_radius", 2f);
        FLOAT_OPTIONS.put("thunderblitz_damage", 3f);
        FLOAT_OPTIONS.put("thunderblitz_cooldown", 250f);
        FLOAT_OPTIONS.put("thunderblitz_chance", 15f);

        FLOAT_OPTIONS.put("stormjolt_cooldown", 100f);
        FLOAT_OPTIONS.put("stormjolt_chance", 15f);

        FLOAT_OPTIONS.put("soulanguish_radius", 3f);
        FLOAT_OPTIONS.put("soulanguish_damage", 4f);
        FLOAT_OPTIONS.put("soulanguish_cooldown", 700f);
        FLOAT_OPTIONS.put("soulanguish_duration", 500f);
        FLOAT_OPTIONS.put("soulanguish_heal", 0.5f);
        FLOAT_OPTIONS.put("soulanguish_range", 22f);

        FLOAT_OPTIONS.put("shockdeflect_block_duration", 35f);
        FLOAT_OPTIONS.put("shockdeflect_damage", 12f);
        FLOAT_OPTIONS.put("shockdeflect_cooldown", 90f);
        FLOAT_OPTIONS.put("shockdeflect_parry_duration", 10f);

        FLOAT_OPTIONS.put("shadowmist_cooldown", 200f);
        FLOAT_OPTIONS.put("shadowmist_chance", 25f);
        FLOAT_OPTIONS.put("shadowmist_damage_multiplier", 0.8f);
        FLOAT_OPTIONS.put("shadowmist_blind_duration", 60f);
        FLOAT_OPTIONS.put("shadowmist_radius", 4f);

        FLOAT_OPTIONS.put("abyssalstandard_cooldown", 700f);
        FLOAT_OPTIONS.put("abyssalstandard_chance", 15f);
        FLOAT_OPTIONS.put("abyssalstandard_damage", 3f);

        FLOAT_OPTIONS.put("righteousstandard_cooldown", 700f);
        FLOAT_OPTIONS.put("righteousstandard_chance", 15f);
        FLOAT_OPTIONS.put("righteousstandard_damage", 3f);

        FLOAT_OPTIONS.put("fatalflicker_cooldown", 300f);
        FLOAT_OPTIONS.put("fatalflicker_chance", 15f);
        FLOAT_OPTIONS.put("fatalflicker_radius", 2f);
        FLOAT_OPTIONS.put("fatalflicker_max_echo_stacks", 99f);
        FLOAT_OPTIONS.put("fatalflicker_dash_velocity", 4f);


        GENERAL_OPTIONS.put("standard_loot_table_weight", 0.01f);
        GENERAL_OPTIONS.put("rare_loot_table_weight", 0.008f);
        GENERAL_OPTIONS.put("runic_loot_table_weight", 0.008f);
        GENERAL_OPTIONS.put("unique_loot_table_weight", 0.002f);
        GENERAL_OPTIONS.put("impact_sound_effect_volume", 0.3f);


        BOOLEAN_OPTIONS.put("display_config_outdated_warning", true);
        BOOLEAN_OPTIONS.put("add_weapons_to_loot_tables", true);
        BOOLEAN_OPTIONS.put("loot_can_be_found_in_villages", false);
        BOOLEAN_OPTIONS.put("enable_weapon_impact_sounds", true);
        BOOLEAN_OPTIONS.put("enable_weapon_footfalls", true);
        BOOLEAN_OPTIONS.put("enable_passive_particles", true);
        BOOLEAN_OPTIONS.put("compat_gobber_end_weapons_unbreakable", true);
        BOOLEAN_OPTIONS.put("enable_unique_gem_sockets", true);

        BOOLEAN_OPTIONS.put("the_watcher", true);
        BOOLEAN_OPTIONS.put("watching_warglaive", true);
        BOOLEAN_OPTIONS.put("longsword_of_the_plague", true);
        BOOLEAN_OPTIONS.put("sword_on_a_stick", true);
        BOOLEAN_OPTIONS.put("bramblethorn", true);
        BOOLEAN_OPTIONS.put("storms_edge", true);
        BOOLEAN_OPTIONS.put("stormbringer", true);
        BOOLEAN_OPTIONS.put("mjolnir", true);
        BOOLEAN_OPTIONS.put("emberblade", true);
        BOOLEAN_OPTIONS.put("hearthflame", true);
        BOOLEAN_OPTIONS.put("twisted_blade", true);
        BOOLEAN_OPTIONS.put("soulrender", true);
        BOOLEAN_OPTIONS.put("soulpyre", true);
        BOOLEAN_OPTIONS.put("soulkeeper", true);
        BOOLEAN_OPTIONS.put("soulstealer", true);
        BOOLEAN_OPTIONS.put("frostfall", true);
        BOOLEAN_OPTIONS.put("molten_edge", true);
        BOOLEAN_OPTIONS.put("livyatan", true);
        BOOLEAN_OPTIONS.put("icewhisper", true);
        BOOLEAN_OPTIONS.put("arcanethyst", true);
        BOOLEAN_OPTIONS.put("thunderbrand", true);
        BOOLEAN_OPTIONS.put("brimstone_claymore", true);
        BOOLEAN_OPTIONS.put("slumbering_lichblade", true);
        BOOLEAN_OPTIONS.put("shadowsting", true);
        BOOLEAN_OPTIONS.put("dormant_relic", true);
        BOOLEAN_OPTIONS.put("whisperwind", true);

        BOOLEAN_OPTIONS.put("active_defence", true);
        BOOLEAN_OPTIONS.put("float", true);
        BOOLEAN_OPTIONS.put("greater_float", true);
        BOOLEAN_OPTIONS.put("freeze", true);
        BOOLEAN_OPTIONS.put("shielding", true);
        BOOLEAN_OPTIONS.put("greater_shielding", true);
        BOOLEAN_OPTIONS.put("slow", true);
        BOOLEAN_OPTIONS.put("greater_slow", true);
        BOOLEAN_OPTIONS.put("stoneskin", true);
        BOOLEAN_OPTIONS.put("greater_stoneskin", true);
        BOOLEAN_OPTIONS.put("swiftness", true);
        BOOLEAN_OPTIONS.put("greater_swiftness", true);
        BOOLEAN_OPTIONS.put("trailblaze", true);
        BOOLEAN_OPTIONS.put("greater_trailblaze", true);
        BOOLEAN_OPTIONS.put("weaken", true);
        BOOLEAN_OPTIONS.put("greater_weaken", true);
        BOOLEAN_OPTIONS.put("zephyr", true);
        BOOLEAN_OPTIONS.put("greater_zephyr", true);
        BOOLEAN_OPTIONS.put("frost_ward", true);
        BOOLEAN_OPTIONS.put("wildfire", true);
        BOOLEAN_OPTIONS.put("unstable", true);
        BOOLEAN_OPTIONS.put("momentum", true);
        BOOLEAN_OPTIONS.put("greater_momentum", true);
        BOOLEAN_OPTIONS.put("imbued", true);
        BOOLEAN_OPTIONS.put("greater_imbued", true);
        BOOLEAN_OPTIONS.put("pincushion", true);
        BOOLEAN_OPTIONS.put("greater_pincushion", true);
        BOOLEAN_OPTIONS.put("ward", true);
        BOOLEAN_OPTIONS.put("immolation", true);


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

        WEAPON_OPTIONS.put("longsword_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("twinblade_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("rapier_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("katana_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("sai_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("spear_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("glaive_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("warglaive_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("cutlass_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("claymore_positive_damage_modifier", 2f);
        WEAPON_OPTIONS.put("greataxe_positive_damage_modifier", 3f);
        WEAPON_OPTIONS.put("greathammer_positive_damage_modifier", 4f);
        WEAPON_OPTIONS.put("chakram_positive_damage_modifier", 0f);
        WEAPON_OPTIONS.put("scythe_positive_damage_modifier", 1f);
        WEAPON_OPTIONS.put("halberd_positive_damage_modifier", 3f);

        WEAPON_OPTIONS.put("longsword_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("twinblade_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("rapier_negative_damage_modifier", 1f);
        WEAPON_OPTIONS.put("katana_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("sai_negative_damage_modifier", 3f);
        WEAPON_OPTIONS.put("spear_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("glaive_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("warglaive_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("cutlass_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("claymore_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("greataxe_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("greathammer_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("chakram_negative_damage_modifier", 1f);
        WEAPON_OPTIONS.put("scythe_negative_damage_modifier", 0f);
        WEAPON_OPTIONS.put("halberd_negative_damage_modifier", 0f);

        WEAPON_OPTIONS.put("iron_damage_modifier", 3f);
        WEAPON_OPTIONS.put("gold_damage_modifier", 3f);
        WEAPON_OPTIONS.put("diamond_damage_modifier", 3f);
        WEAPON_OPTIONS.put("netherite_damage_modifier", 3f);
        WEAPON_OPTIONS.put("runic_damage_modifier", 3f);

        WEAPON_OPTIONS.put("adamantite_damage_modifier", 3f);
        WEAPON_OPTIONS.put("aquarium_damage_modifier", 3f);
        WEAPON_OPTIONS.put("banglum_damage_modifier", 3f);
        WEAPON_OPTIONS.put("carmot_damage_modifier", 3f);
        WEAPON_OPTIONS.put("kyber_damage_modifier", 3f);
        WEAPON_OPTIONS.put("mythril_damage_modifier", 3f);
        WEAPON_OPTIONS.put("orichalcum_damage_modifier", 3f);
        WEAPON_OPTIONS.put("durasteel_damage_modifier", 3f);
        WEAPON_OPTIONS.put("osmium_damage_modifier", 3f);
        WEAPON_OPTIONS.put("prometheum_damage_modifier", 3f);
        WEAPON_OPTIONS.put("quadrillum_damage_modifier", 3f);
        WEAPON_OPTIONS.put("runite_damage_modifier", 3f);
        WEAPON_OPTIONS.put("star_platinum_damage_modifier", 3f);
        WEAPON_OPTIONS.put("bronze_damage_modifier", 3f);
        WEAPON_OPTIONS.put("copper_damage_modifier", 3f);
        WEAPON_OPTIONS.put("steel_damage_modifier", 3f);
        WEAPON_OPTIONS.put("palladium_damage_modifier", 3f);
        WEAPON_OPTIONS.put("stormyx_damage_modifier", 3f);
        WEAPON_OPTIONS.put("celestium_damage_modifier", 3f);
        WEAPON_OPTIONS.put("metallurgium_damage_modifier", 3f);

        WEAPON_OPTIONS.put("gobber_damage_modifier", 1f);
        WEAPON_OPTIONS.put("gobber_nether_damage_modifier", 3f);
        WEAPON_OPTIONS.put("gobber_end_damage_modifier", 6f);

        WEAPON_OPTIONS.put("longsword_attackspeed", -2.4f);
        WEAPON_OPTIONS.put("twinblade_attackspeed", -2f);
        WEAPON_OPTIONS.put("rapier_attackspeed", -1.8f);
        WEAPON_OPTIONS.put("katana_attackspeed", -2f);
        WEAPON_OPTIONS.put("sai_attackspeed", -1.5f);
        WEAPON_OPTIONS.put("spear_attackspeed", -2.7f);
        WEAPON_OPTIONS.put("glaive_attackspeed", -2.6f);
        WEAPON_OPTIONS.put("warglaive_attackspeed", -2.2f);
        WEAPON_OPTIONS.put("cutlass_attackspeed", -2f);
        WEAPON_OPTIONS.put("claymore_attackspeed", -2.8f);
        WEAPON_OPTIONS.put("greataxe_attackspeed", -3.1f);
        WEAPON_OPTIONS.put("greathammer_attackspeed", -3.2f);
        WEAPON_OPTIONS.put("chakram_attackspeed", -3.0f);
        WEAPON_OPTIONS.put("scythe_attackspeed", -2.7f);
        WEAPON_OPTIONS.put("halberd_attackspeed", -2.8f);

        WEAPON_OPTIONS.put("brimstone_damage_modifier", 6f);
        WEAPON_OPTIONS.put("thewatcher_damage_modifier", 6f);
        WEAPON_OPTIONS.put("stormsedge_damage_modifier", 3f);
        WEAPON_OPTIONS.put("stormbringer_damage_modifier", 3f);
        WEAPON_OPTIONS.put("swordonastick_damage_modifier", 5f);
        WEAPON_OPTIONS.put("bramblethorn_damage_modifier", 6f);
        WEAPON_OPTIONS.put("watchingwarglaive_damage_modifier", 3f);
        WEAPON_OPTIONS.put("longswordofplague_damage_modifier", 3f);
        WEAPON_OPTIONS.put("emberblade_damage_modifier", 3f);
        WEAPON_OPTIONS.put("hearthflame_damage_modifier", 8f);
        WEAPON_OPTIONS.put("soulkeeper_damage_modifier", 8f);
        WEAPON_OPTIONS.put("twistedblade_damage_modifier", 4f);
        WEAPON_OPTIONS.put("soulstealer_damage_modifier", 0f);
        WEAPON_OPTIONS.put("soulrender_damage_modifier", 4f);
        WEAPON_OPTIONS.put("mjolnir_damage_modifier", 3f);
        WEAPON_OPTIONS.put("thedispatcher_damage_modifier", 4f);
        WEAPON_OPTIONS.put("soulpyre_damage_modifier", 7f);
        WEAPON_OPTIONS.put("frostfall_damage_modifier", 5f);
        WEAPON_OPTIONS.put("moltenedge_damage_modifier", 4f);
        WEAPON_OPTIONS.put("livyatan_damage_modifier", 4f);
        WEAPON_OPTIONS.put("icewhisper_damage_modifier", 7f);
        WEAPON_OPTIONS.put("arcanethyst_damage_modifier", 7f);
        WEAPON_OPTIONS.put("thunderbrand_damage_modifier", 7f);
        WEAPON_OPTIONS.put("lichblade_damage_modifier", 7f);
        WEAPON_OPTIONS.put("shadowsting_damage_modifier", -2f);
        WEAPON_OPTIONS.put("sunfire_damage_modifier", 3f);
        WEAPON_OPTIONS.put("harbinger_damage_modifier", 3f);
        WEAPON_OPTIONS.put("whisperwind_damage_modifier", 3f);

        WEAPON_OPTIONS.put("brimstone_attackspeed", -2.8f);
        WEAPON_OPTIONS.put("thewatcher_attackspeed", -2.8f);
        WEAPON_OPTIONS.put("stormsedge_attackspeed", -2f);
        WEAPON_OPTIONS.put("stormbringer_attackspeed", -2.4f);
        WEAPON_OPTIONS.put("swordonastick_attackspeed", -2.6f);
        WEAPON_OPTIONS.put("bramblethorn_attackspeed", -1.8f);
        WEAPON_OPTIONS.put("watchingwarglaive_attackspeed", -2.2f);
        WEAPON_OPTIONS.put("longswordofplague_attackspeed", -2.4f);
        WEAPON_OPTIONS.put("emberblade_attackspeed", -2.4f);
        WEAPON_OPTIONS.put("hearthflame_attackspeed", -3.2f);
        WEAPON_OPTIONS.put("soulkeeper_attackspeed", -2.9f);
        WEAPON_OPTIONS.put("twistedblade_attackspeed", -2.6f);
        WEAPON_OPTIONS.put("soulstealer_attackspeed", -1.5f);
        WEAPON_OPTIONS.put("soulrender_attackspeed", -2.4f);
        WEAPON_OPTIONS.put("mjolnir_attackspeed", -3.0f);
        WEAPON_OPTIONS.put("thedispatcher_attackspeed", -2.0f);
        WEAPON_OPTIONS.put("soulpyre_attackspeed", -3.0f);
        WEAPON_OPTIONS.put("frostfall_attackspeed", -2.5f);
        WEAPON_OPTIONS.put("moltenedge_attackspeed", -2.1f);
        WEAPON_OPTIONS.put("livyatan_attackspeed", -2.1f);
        WEAPON_OPTIONS.put("icewhisper_attackspeed", -2.7f);
        WEAPON_OPTIONS.put("arcanethyst_attackspeed", -2.7f);
        WEAPON_OPTIONS.put("thunderbrand_attackspeed", -2.7f);
        WEAPON_OPTIONS.put("lichblade_attackspeed", -3.1f);
        WEAPON_OPTIONS.put("shadowsting_attackspeed", -1.7f);
        WEAPON_OPTIONS.put("sunfire_attackspeed", -2.4f);
        WEAPON_OPTIONS.put("harbinger_attackspeed", -2.4f);
        WEAPON_OPTIONS.put("whisperwind_attackspeed", -2.0f);

    }

    public static void loadConfig() {
        //System.out.println("Loading common Simply Swords config");
        JsonObject json;
        json = Config.getJsonObject(Config.readFile(new File("config/simplyswords/booleans.json5")));
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            BOOLEAN_OPTIONS.put(entry.getKey(), entry.getValue().getAsBoolean());
        }

        json = Config.getJsonObject(Config.readFile(new File("config/simplyswords/loot_config.json5")));
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            LOOT_OPTIONS.put(entry.getKey(), entry.getValue().getAsFloat());
        }

        json = Config.getJsonObject(Config.readFile(new File("config/simplyswords/general_config.json5")));
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            GENERAL_OPTIONS.put(entry.getKey(), entry.getValue().getAsFloat());
        }

        json = Config.getJsonObject(Config.readFile(new File("config/simplyswords/effects_config.json5")));
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            FLOAT_OPTIONS.put(entry.getKey(), entry.getValue().getAsFloat());
        }

        json = Config.getJsonObject(Config.readFile(new File("config/simplyswords/weapon_attributes.json5")));
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            WEAPON_OPTIONS.put(entry.getKey(), entry.getValue().getAsFloat());
        }

    }


    public static void generateConfigs(boolean overwrite) {
        //System.out.println("Generating common Simply Swords config");
        StringBuilder config = new StringBuilder("{\n");
        int i = 0;
        for (String key : BOOLEAN_OPTIONS.keySet()) {
            if (i == 8) {
                config.append("\n");
                config.append("// --------------------------------------------- LOOT BLACKLISTING ----------------------------------------------------------------- \n");
                config.append("// Setting any of the below values to false will prevent that item from generating in loot. \n");
                config.append("// The item will still exist in the creative menu, but will be unobtainable through ordinary survival gameplay.  \n");
                config.append("// --------------------------------------------------------------------------------------------------------------------------------- \n");
                config.append("\n");
            }
            if (i == 33) {
                config.append("\n");
                config.append("// --------------------------------------------- RUNIC BLACKLISTING ----------------------------------------------------------------- \n");
                config.append("// Setting any of the below values to false will prevent that runic power from being obtainable. \n");
                config.append("// The power will be completely removed from the pool of Runic powers, but will still be visible in the RUNIC GRIMOIRE if Patchouli is installed.  \n");
                config.append("// ---------------------------------------------------------------------------------------------------------------------------------- \n");
                config.append("\n");
            }
            config.append("  \"").append(key).append("\": ").append(BOOLEAN_OPTIONS.get(key));
            ++i;
            if (i < BOOLEAN_OPTIONS.size()) {
                config.append(",");
            }
            config.append("\n");
        }
        config.append("}");
        Config.createFile("config/simplyswords/booleans.json5", config.toString(), overwrite);

        config = new StringBuilder("{\n");
        i = 0;
        for (String key : LOOT_OPTIONS.keySet()) {
            if (i == 0) {
                config.append("// --------------------------------------------- LOOT CONFIGURATION ----------------------------------------------------------------- \n");
                config.append("// This config allows for the tweaking of loot injection for UNIQUE weapons. \n");
                config.append("// Standard iron > diamond gear, and Runic Tablets, are controlled by the loot modifiers in the general_config, not here.  \n");
                config.append("// ---------------------------------------------------------------------------------------------------------------------------------- \n");
                config.append("// --------------------------------------------- How does it work? ------------------------------------------------------------------ \n");
                config.append("// If 'add_weapons_to_loot_tables' is enabled in the boolean config, Simply Swords will attempt to inject its loot into    \n");
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
        Config.createFile("config/simplyswords/loot_config.json5", config.toString(), overwrite);

        config = new StringBuilder("{\n");
        i = 0;
        for (String item : GENERAL_OPTIONS.keySet()) {
            if (i == 0) {
                config.append("// -- GENERAL CONFIGURATION -- \n");
                config.append("\n");
                config.append("// -- Loot Table Weights -- \n");
                config.append("// The chances of loot appearing in chests. \n");
                config.append("// 1 = 100% chance \n");
                config.append("// Values are very sensitive - recommend keeping changes small \n");
                config.append("// Simply Swords loot has a chance to appear in any chest vanilla or modded, except villager chests \n");
                config.append("// ------------------------ \n");
                config.append("\n");
                config.append("// Standard Loot: Iron > Gold Weapons. Default: 0.01 \n");
            }
            if (i == 1) {
                config.append("\n");
                config.append("// Rare Loot: Diamond Weapons. Default: 0.008 \n");
            }
            if (i == 2) {
                config.append("\n");
                config.append("// Runic Loot: Runic Tablets. Default: 0.008 \n");
            }
            if (i == 3) {
                config.append("\n");
                config.append("// Unique Loot: Unique Weapons. Default: 0.002 \n");
            }
            if (i == 4) {
                config.append("\n");
                config.append("// -- General Options -- \n");
            }
            config.append("  \"").append(item).append("\": ").append(GENERAL_OPTIONS.get(item));
            ++i;
            if (i < GENERAL_OPTIONS.size()) {
                config.append(",");
            }
            config.append("\n");
        }
        config.append("}");
        Config.createFile("config/simplyswords/general_config.json5", config.toString(), overwrite);

        config = new StringBuilder("{\n");
        i = 0;
        for (String item : FLOAT_OPTIONS.keySet()) {
            if (i == 0) {
                config.append("// -- EFFECTS CONFIGURATION -- \n");
                config.append("\n");
                config.append("//Chance range 0-100, where 100 = 100% chance to occur\n");
                config.append("//Radius is measured in blocks\n");
                config.append("//Duration in ticks, where 20 is equivalent to one second \n");
                config.append("\n");
                config.append("// ---------------------------- \n");
                config.append("// -- Runic Power: Swiftness -- \n");
                config.append("// ---------------------------- \n");
            }
            if (i == 2) {
                config.append("\n");
                config.append("// ----------------------- \n");
                config.append("// -- Runic Power: Slow -- \n");
                config.append("// ----------------------- \n");
            }
            if (i == 4) {
                config.append("\n");
                config.append("// ------------------------- \n");
                config.append("// -- Runic Power: Poison -- \n");
                config.append("// ------------------------- \n");
            }
            if (i == 6) {
                config.append("\n");
                config.append("// ------------------------- \n");
                config.append("// -- Runic Power: Freeze -- \n");
                config.append("// ------------------------- \n");
            }
            if (i == 8) {
                config.append("\n");
                config.append("// --------------------------- \n");
                config.append("// -- Runic Power: Wildfire -- \n");
                config.append("// --------------------------- \n");
            }
            if (i == 11) {
                config.append("\n");
                config.append("// ------------------------ \n");
                config.append("// -- Runic Power: Float -- \n");
                config.append("// ------------------------ \n");
            }
            if (i == 13) {
                config.append("\n");
                config.append("// ------------------------- \n");
                config.append("// -- Runic Power: Zephyr -- \n");
                config.append("// ------------------------- \n");
            }
            if (i == 15) {
                config.append("\n");
                config.append("// ---------------------------- \n");
                config.append("// -- Runic Power: Shielding -- \n");
                config.append("// ---------------------------- \n");
            }
            if (i == 17) {
                config.append("\n");
                config.append("// ---------------------------- \n");
                config.append("// -- Runic Power: Stoneskin -- \n");
                config.append("// ---------------------------- \n");
            }
            if (i == 19) {
                config.append("\n");
                config.append("// ----------------------------- \n");
                config.append("// -- Runic Power: Trailblaze -- \n");
                config.append("// ----------------------------- \n");
            }
            if (i == 21) {
                config.append("\n");
                config.append("// ------------------------- \n");
                config.append("// -- Runic Power: Weaken -- \n");
                config.append("// ------------------------- \n");
            }
            if (i == 23) {
                config.append("\n");
                config.append("// --------------------------- \n");
                config.append("// -- Runic Power: Unstable -- \n");
                config.append("// --------------------------- \n");
            }
            if (i == 25) {
                config.append("\n");
                config.append("// --------------------------------- \n");
                config.append("// -- Runic Power: Active Defence -- \n");
                config.append("// --------------------------------- \n");
            }
            if (i == 27) {
                config.append("\n");
                config.append("// ----------------------------- \n");
                config.append("// -- Runic Power: Frost Ward -- \n");
                config.append("// ----------------------------- \n");
            }
            if (i == 30) {
                config.append("\n");
                config.append("// --------------------------- \n");
                config.append("// -- Runic Power: Momentum -- \n");
                config.append("// --------------------------- \n");
            }
            if (i == 31) {
                config.append("\n");
                config.append("// ------------------------- \n");
                config.append("// -- Runic Power: Imbued -- \n");
                config.append("// ------------------------- \n");
            }
            if (i == 32) {
                config.append("\n");
                config.append("// ---------------------------- \n");
                config.append("// -- Unique Effect: Watcher -- \n");
                config.append("// -- Restore amount refers to health gained from each enemy in range -- \n");
                config.append("// ---------------------------- \n");
            }
            if (i == 35) {
                config.append("\n");
                config.append("// ------------------------- \n");
                config.append("// -- Unique Effect: Omen -- \n");
                config.append("// -- Absorption amount refers to the tier of regeneration gained on proc -- \n");
                config.append("// -- Instantkill Threshold refers to the % of maxhealth an enemy must be UNDER in order for the effect to proc. Where 1 = 100% -- \n");
                config.append("// ------------------------- \n");
            }
            if (i == 36) {
                config.append("\n");
                config.append("// ------------------------------- \n");
                config.append("// -- Unique Effect: Soul Steal -- \n");
                config.append("// Duration refers to the haste, slow, and glow effects \n");
                config.append("// ------------------------------- \n");
            }
            if (i == 43) {
                config.append("\n");
                config.append("// ---------------------------- \n");
                config.append("// -- Unique Effect: Gravity -- \n");
                config.append("// NOT YET IMPLEMENTED \n");
                config.append("// ---------------------------- \n");
            }
            if (i == 46) {
                config.append("\n");
                config.append("// ------------------------------ \n");
                config.append("// -- Unique Effect: Soul Meld -- \n");
                config.append("// ------------------------------ \n");
            }
            if (i == 49) {
                config.append("\n");
                config.append("// ------------------------------ \n");
                config.append("// -- Unique Effect: Soul Rend -- \n");
                config.append("// ------------------------------ \n");
            }
            if (i == 55) {
                config.append("\n");
                config.append("// ----------------------------- \n");
                config.append("// -- Unique Effect: Ferocity -- \n");
                config.append("// ----------------------------- \n");
            }
            if (i == 59) {
                config.append("\n");
                config.append("// ------------------------------ \n");
                config.append("// -- Unique Effect: Ember Ire -- \n");
                config.append("// ------------------------------ \n");
            }
            if (i == 61) {
                config.append("\n");
                config.append("// ---------------------------------- \n");
                config.append("// -- Unique Effect: Volcanic Fury -- \n");
                config.append("// ---------------------------------- \n");
            }
            if (i == 65) {
                config.append("\n");
                config.append("// -------------------------- \n");
                config.append("// -- Unique Effect: Storm -- \n");
                config.append("// -------------------------- \n");
            }
            if (i == 67) {
                config.append("\n");
                config.append("// --------------------------- \n");
                config.append("// -- Unique Effect: Plague -- \n");
                config.append("// --------------------------- \n");
            }
            if (i == 68) {
                config.append("\n");
                config.append("// ------------------------------ \n");
                config.append("// -- Unique Effect: Brimstone -- \n");
                config.append("// ------------------------------ \n");
            }
            if (i == 69) {
                config.append("\n");
                config.append("// ---------------------------- \n");
                config.append("// -- Unique Effect: Bramble -- \n");
                config.append("// ---------------------------- \n");
            }
            if (i == 71) {
                config.append("\n");
                config.append("// -------------------------------- \n");
                config.append("// -- Unique Effect: Soul Tether -- \n");
                config.append("// -------------------------------- \n");
            }
            if (i == 76) {
                config.append("\n");
                config.append("// ------------------------------- \n");
                config.append("// -- Unique Effect: Frost Fury -- \n");
                config.append("// ------------------------------- \n");
            }
            if (i == 81) {
                config.append("\n");
                config.append("// -------------------------------- \n");
                config.append("// -- Unique Effect: Molten Roar -- \n");
                config.append("// -------------------------------- \n");
            }
            if (i == 86) {
                config.append("\n");
                config.append("// ---------------------------------- \n");
                config.append("// -- Unique Effect: Frost Shatter -- \n");
                config.append("// ---------------------------------- \n");
            }
            if (i == 90) {
                config.append("\n");
                config.append("// ------------------------------- \n");
                config.append("// -- Unique Effect: Permafrost -- \n");
                config.append("// ------------------------------- \n");
            }
            if (i == 94) {
                config.append("\n");
                config.append("// ----------------------------------- \n");
                config.append("// -- Unique Effect: Arcane Assault -- \n");
                config.append("// ----------------------------------- \n");
            }
            if (i == 99) {
                config.append("\n");
                config.append("// ---------------------------------- \n");
                config.append("// -- Unique Effect: Thunder Blitz -- \n");
                config.append("// ---------------------------------- \n");
            }
            if (i == 103) {
                config.append("\n");
                config.append("// ------------------------------- \n");
                config.append("// -- Unique Effect: Storm Jolt -- \n");
                config.append("// ------------------------------- \n");
            }
            if (i == 105) {
                config.append("\n");
                config.append("// --------------------------------- \n");
                config.append("// -- Unique Effect: Soul Anguish -- \n");
                config.append("// --------------------------------- \n");
            }
            if (i == 111) {
                config.append("\n");
                config.append("// ---------------------------------- \n");
                config.append("// -- Unique Effect: Shock Deflect -- \n");
                config.append("// ---------------------------------- \n");
            }
            if (i == 115) {
                config.append("\n");
                config.append("// ------------------------------- \n");
                config.append("// -- Unique Effect: Shadowmist -- \n");
                config.append("// ------------------------------- \n");
            }
            if (i == 120) {
                config.append("\n");
                config.append("// ------------------------------------- \n");
                config.append("// -- Unique Effect: Abyssal Standard -- \n");
                config.append("// ------------------------------------- \n");
            }
            if (i == 123) {
                config.append("\n");
                config.append("// --------------------------------------- \n");
                config.append("// -- Unique Effect: Righteous Standard -- \n");
                config.append("// --------------------------------------- \n");
            }
            if (i == 126) {
                config.append("\n");
                config.append("// ---------------------------------- \n");
                config.append("// -- Unique Effect: Fatal Flicker -- \n");
                config.append("// ---------------------------------- \n");
            }
            config.append("  \"").append(item).append("\": ").append(FLOAT_OPTIONS.get(item));
            ++i;
            if (i < FLOAT_OPTIONS.size()) {
                config.append(",");
            }
            config.append("\n");
        }
        config.append("}");
        Config.createFile("config/simplyswords/effects_config.json5", config.toString(), overwrite);

        config = new StringBuilder("{\n");
        i = 0;
        for (String item : WEAPON_OPTIONS.keySet()) {
            if (i == 0) {
                config.append("// -- WEAPON ATTRIBUTES CONFIGURATION -- \n");
                config.append("// These values should be THE SAME ON BOTH CLIENT AND SERVER, otherwise damage tooltips will display incorrect on the client \n");
                config.append("// The damage values of weapons can be modified by adjusting their weights \n");
                config.append("// This is not the outputted damage value you see in game, but it affects it directly \n");
                config.append("// Calculation: vanilla tool material damage + base_modifier + positive_modifier - negative_modifier = actual modifier \n");
                config.append("\n");
                config.append("// -- Positive Damage Modifiers -- \n");
                config.append("// Example use-case: Adding 3 to a value below will INCREASE the in-game damage of that weapon type by 3 \n");
                config.append("// ------------------------------- \n");
            }
            if (i == 15) {
                config.append("\n");
                config.append("// -- Negative Damage Modifiers -- \n");
                config.append("// Example use-case: Adding 3 to a value below will DECREASE the in-game damage of that weapon type by 3 \n");
                config.append("// ------------------------------- \n");
            }
            if (i == 30) {
                config.append("\n");
                config.append("// -- Base Damage Modifiers -- \n");
                config.append("// Positive & Negative damage modifiers scale off these base values \n");
                config.append("// --------------------------- \n");
            }
            if (i == 58) {
                config.append("\n");
                config.append("// -- Attack Speed Modifiers -- \n");
                config.append("// Recommended range: -1.0 to -3.7, with -1.0 being fast and -3.7 being slow \n");
                config.append("// ---------------------------- \n");
            }
            if (i == 73) {
                config.append("\n");
                config.append("\n");
                config.append("// -- UNIQUE ATTRIBUTES CONFIGURATION -- \n");
                config.append("// Uniques scale a little bit differently and only require one modifier \n");
                config.append("// This is not the outputted damage value you see in game, but it affects it directly \n");
                config.append("// Calculation: Vanilla netherite tool damage + damage_modifier = actual modifier \n");
                config.append("\n");
                config.append("// -- Damage Modifiers -- \n");
                config.append("// Example use-case: Adding 3 to a value below will INCREASE the in-game damage of that weapon by 3 \n");
                config.append("// ---------------------- \n");
            }
            if (i ==101) {
                config.append("\n");
                config.append("// -- Attack Speed Modifiers -- \n");
                config.append("// Recommended range: -1.0 to -3.7, with -1.0 being fast and -3.7 being slow \n");
                config.append("// ---------------------------- \n");
            }
            config.append("  \"").append(item).append("\": ").append(WEAPON_OPTIONS.get(item));
            ++i;
            if (i < WEAPON_OPTIONS.size()) {
                config.append(",");
            }
            config.append("\n");
        }
        config.append("}");
        Config.createFile("config/simplyswords/weapon_attributes.json5", config.toString(), overwrite);

    }
}