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

    private static boolean isLoaded = false;
    private static final HashMap<String, JsonObject> JSONS = new HashMap<>();

    public static boolean getBoolean(String key, String parent, boolean defaultValue) {
        var e = getElement(parent, key);
        if (e == null) return defaultValue;

        try {
            return e.getAsBoolean();
        }catch (Exception ignored) {
            System.out.println("Failed to fetch config value for " + key + ". Loading default value.\nIt is recommended that you restart your game.");
            return defaultValue;
        }
    }

    public static float getFloat(String key, String parent, float defaultValue) {
        var e = getElement(parent, key);
        if (e == null) return defaultValue;

        try {
            return e.getAsFloat();
        }catch (Exception ignored) {
            System.out.println("Failed to fetch config value for " + key + ". Loading default value.\nIt is recommended that you restart your game.");
            return defaultValue;
        }
    }

    public static double getDouble(String key, String parent, double defaultValue) {
        var e = getElement(parent, key);
        if (e == null) return defaultValue;

        try {
            return e.getAsDouble();
        }catch (Exception ignored) {
            System.out.println("Failed to fetch config value for " + key + ". Loading default value.\nIt is recommended that you restart your game.");
            return defaultValue;
        }
    }

    public static int getInt(String key, String parent, int defaultValue) {
        var e = getElement(parent, key);
        if (e == null) return defaultValue;

        try {
            return e.getAsInt();
        }catch (Exception ignored) {
            System.out.println("Failed to fetch config value for " + key + ". Loading default value.\nIt is recommended that you restart your game.");
            return defaultValue;
        }
    }

    private static JsonObject getJson(String key) {
        if (!isLoaded) {
            Path path = Paths.get("config/simplyswords_main/");
            if (!Files.exists(path)) return null;

            JSONS.put("GemEffects", Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/gem_effects.json5"))));
            JSONS.put("General", Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/general.json5"))));
            JSONS.put("Loot", Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/loot.json5"))));
            JSONS.put("RunicEffects", Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/runic_effects.json5"))));
            JSONS.put("StatusEffects", Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/status_effects.json5"))));
            JSONS.put("UniqueEffects", Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/unique_effects.json5"))));
            JSONS.put("WeaponAttributes", Config.getJsonObject(Config.readFile(new File("config/simplyswords_main/weapon_attributes.json5"))));
            isLoaded = true;

        }
        return JSONS.get(key);
    }

    private static JsonElement getElement(String parent, String key) {
        var json = getJson(parent);
        if (json == null) return null;
        return json.get(key);
    }
}
