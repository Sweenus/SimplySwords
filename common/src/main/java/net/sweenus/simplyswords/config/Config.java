package net.sweenus.simplyswords.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Config {

    public static File createFile(String path, String contents, boolean overwrite) {
        File file = new File(path);
        if (file.exists() && !overwrite) {
            return file;
        }
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.setReadable(true);
        file.setWritable(true);
        file.setExecutable(true);
        if (contents == null || "".equals(contents)) {
            return file;
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }


    public static String readFile(File file) {
        String output = "";
        try (Scanner scanner = new Scanner(file)) {
            scanner.useDelimiter("\\Z");
            output = scanner.next();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static JsonObject getJsonObject(String json) {
        try {
            return new JsonParser().parse(json).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    // -- Safe Config Fetching --
    // Allows safely fetching config values in a scenario where we do not know if they exist.
    // EG. Addon mod for Simply Swords attempting to load config values before Simply Swords has initialised.

    private static final HashMap<String, Boolean> BOOLEAN = new LinkedHashMap<>();
    private static final HashMap<String, Float> FLOAT = new LinkedHashMap<>();
    private static final HashMap<String, Double> DOUBLE = new LinkedHashMap<>();
    private static final HashMap<String, Integer> INT = new LinkedHashMap<>();

    public static boolean getBoolean(String key, String parent, boolean defaultValue) {
        //System.out.println("Trying to fetch config value for " + key + " from " + parent);
        safeValueFetch("boolean", parent);
        if (!BOOLEAN.isEmpty()) {
            if (BOOLEAN.containsKey(key)) {
                //System.out.println("Successfully fetched value for " + key + " : " + BOOLEAN.get(key));
                return BOOLEAN.get(key);
            }
        }
        System.out.println("Failed to fetch config value for " + key + ". Loading default value.\nIt is recommended that you restart your game.");
        return defaultValue;
    }

    public static float getFloat(String key, String parent, float defaultValue) {
        //System.out.println("Trying to fetch config value for " + key + " from " + parent);
        safeValueFetch("float", parent);
        if (!FLOAT.isEmpty()) {
            if (FLOAT.containsKey(key)) {
                //System.out.println("Successfully fetched value for " + key + " : " + FLOAT.get(key));
                return FLOAT.get(key);
            }
        }
        System.out.println("Failed to fetch config value for " + key + ". Loading default value.\nIt is recommended that you restart your game.");
        //System.out.print(FLOAT);
        return defaultValue;
    }

    public static double getDouble(String key, String parent, double defaultValue) {
        safeValueFetch("double", parent);
        if (!DOUBLE.isEmpty()) {
            if (DOUBLE.containsKey(key))
                return DOUBLE.get(key);
        }
        System.out.println("Failed to fetch config value for " + key + ". Loading default value.\nIt is recommended that you restart your game.");
        return defaultValue;
    }

    public static int getInt(String key, String parent, int defaultValue) {
        safeValueFetch("int", parent);
        if (!INT.isEmpty()) {
            if (INT.containsKey(key))
                return INT.get(key);
        }
        System.out.println("Failed to fetch config value for " + key + ". Loading default value.\nIt is recommended that you restart your game.");
        return defaultValue;
    }

    public static void safeValueFetch(String type, String parent) {
        Path path = Paths.get("config/simplyswords_main/");
        JsonObject json = null;
        if (Files.exists(path)) {
            json = switch (parent) {
                case "GemEffects" -> Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/gem_effects.json5")));
                case "General" -> Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/general.json5")));
                case "Loot" -> Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/loot.json5")));
                case "RunicEffects" -> Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/runic_effects.json5")));
                case "StatusEffects" -> Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/status_effects.json5")));
                case "UniqueEffects" -> Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/unique_effects.json5")));
                case "WeaponAttributes" -> Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/weapon_attributes.json5")));
                default -> null;
            };
        }

        if (json != null) {

            switch (type) {
                case "boolean" -> {
                    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                        try {
                            BOOLEAN.put(entry.getKey(), entry.getValue().getAsBoolean());
                        } catch (Exception e) {
                            //System.out.println(entry.getKey() + ": " + entry.getValue() + " is not a valid value. Skipping this entry.");
                        }
                    }
                }
                case "float" -> {
                    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                        try {
                            FLOAT.put(entry.getKey(), entry.getValue().getAsFloat());
                        } catch (Exception e) {
                            //System.out.println(entry.getKey() + ": " + entry.getValue() + " is not a valid value. Skipping this entry.");
                        }
                    }
                }
                case "double" -> {
                    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                        try {
                            DOUBLE.put(entry.getKey(), entry.getValue().getAsDouble());
                        } catch (Exception e) {
                            //System.out.println(entry.getKey() + ": " + entry.getValue() + " is not a valid value. Skipping this entry.");
                        }
                    }
                }
                case "int" -> {
                    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                        try {
                            INT.put(entry.getKey(), entry.getValue().getAsInt());
                        } catch (Exception e) {
                            //System.out.println(entry.getKey() + ": " + entry.getValue() + " is not a valid value. Skipping this entry.");
                        }
                    }
                }
            }
        }
    }


}
