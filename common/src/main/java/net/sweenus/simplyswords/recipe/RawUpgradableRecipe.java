package net.sweenus.simplyswords.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The shaped portion of a unique-upgrade recipe, including its source slot. */
public final class RawUpgradableRecipe {
    private final int width;
    private final int height;
    private final int upgradableItemSlot;
    private final DefaultedList<Ingredient> ingredients;
    private boolean mirrored;

    public RawUpgradableRecipe(int width, int height, int upgradableItemSlot,
                               DefaultedList<Ingredient> ingredients) {
        this.width = width;
        this.height = height;
        this.upgradableItemSlot = upgradableItemSlot;
        this.ingredients = ingredients;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getUpgradableItemSlot() { return upgradableItemSlot; }
    public DefaultedList<Ingredient> getIngredients() { return ingredients; }
    public boolean isMirrored() { return mirrored; }

    public boolean matches(RecipeInputInventory input) {
        if (input.getWidth() != width || input.getHeight() != height) return false;
        int expected = 0;
        int actual = 0;
        for (Ingredient ingredient : ingredients) if (!ingredient.isEmpty()) expected++;
        for (int i = 0; i < input.size(); i++) if (!input.getStack(i).isEmpty()) actual++;
        return expected == actual && (matches(input, false) || matches(input, true));
    }

    private boolean matches(RecipeInputInventory input, boolean mirror) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int ingredientX = mirror ? width - x - 1 : x;
                Ingredient ingredient = ingredients.get(ingredientX + y * width);
                ItemStack stack = input.getStack(x + y * input.getWidth());
                if (!ingredient.test(stack)) return false;
            }
        }
        mirrored = mirror;
        return true;
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(width);
        buf.writeVarInt(height);
        for (Ingredient ingredient : ingredients) ingredient.write(buf);
        buf.writeVarInt(upgradableItemSlot);
    }

    public static RawUpgradableRecipe read(PacketByteBuf buf) {
        int width = buf.readVarInt();
        int height = buf.readVarInt();
        DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(width * height, Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) ingredients.set(i, Ingredient.fromPacket(buf));
        return new RawUpgradableRecipe(width, height, buf.readVarInt(), ingredients);
    }

    public static RawUpgradableRecipe fromJson(JsonObject json) {
        JsonArray patternJson = JsonHelper.getArray(json, "pattern");
        List<String> rows = new ArrayList<>();
        for (JsonElement element : patternJson) rows.add(element.getAsString());
        String[] pattern = stripWhitespace(rows);
        int height = pattern.length;
        int width = pattern[0].length();

        JsonObject keyJson = JsonHelper.getObject(json, "key");
        DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(width * height, Ingredient.EMPTY);
        int upgradableSlot = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char symbol = pattern[y].charAt(x);
                if (symbol == ' ') continue;
                JsonElement encoded = keyJson.get(String.valueOf(symbol));
                if (encoded == null) throw new IllegalArgumentException("Undefined recipe symbol: " + symbol);
                boolean upgradable = false;
                JsonElement ingredientJson = encoded;
                if (encoded.isJsonArray()) {
                    JsonArray pair = encoded.getAsJsonArray();
                    ingredientJson = pair.get(0);
                    upgradable = pair.size() > 1 && pair.get(1).getAsBoolean();
                } else if (encoded.isJsonObject() && encoded.getAsJsonObject().has("ingredient")) {
                    JsonObject object = encoded.getAsJsonObject();
                    ingredientJson = object.get("ingredient");
                    upgradable = JsonHelper.getBoolean(object, "upgradable", false);
                } else if (encoded.isJsonObject()) {
                    upgradable = JsonHelper.getBoolean(encoded.getAsJsonObject(), "upgradable", false);
                }
                ingredients.set(x + y * width, Ingredient.fromJson(ingredientJson));
                if (upgradable) {
                    if (upgradableSlot >= 0) throw new IllegalArgumentException("Only one slot may be upgradable");
                    upgradableSlot = x + y * width;
                }
            }
        }
        if (upgradableSlot < 0) throw new IllegalArgumentException("Recipe has no upgradable slot");
        return new RawUpgradableRecipe(width, height, upgradableSlot, ingredients);
    }

    private static String[] stripWhitespace(List<String> rows) {
        int left = Integer.MAX_VALUE;
        int right = -1;
        int top = Integer.MAX_VALUE;
        int bottom = -1;
        for (int y = 0; y < rows.size(); y++) {
            for (int x = 0; x < rows.get(y).length(); x++) {
                if (rows.get(y).charAt(x) == ' ') continue;
                left = Math.min(left, x);
                right = Math.max(right, x);
                top = Math.min(top, y);
                bottom = Math.max(bottom, y);
            }
        }
        if (right < left) throw new IllegalArgumentException("Empty recipe pattern");
        List<String> result = new ArrayList<>();
        for (int y = top; y <= bottom; y++) result.add(rows.get(y).substring(left, right + 1));
        return result.toArray(new String[0]);
    }
}
