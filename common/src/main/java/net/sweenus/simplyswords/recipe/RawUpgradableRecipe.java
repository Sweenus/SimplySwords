package net.sweenus.simplyswords.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class RawUpgradableRecipe {
    public static final MapCodec<RawUpgradableRecipe> CODEC;
    public static final PacketCodec<RegistryByteBuf, RawUpgradableRecipe> PACKET_CODEC;
    private final int width;
    private final int height;
    private boolean mirrored;

    public int getUpgradableItemSlot() {
        return upgradableItemSlot;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private final int upgradableItemSlot;
    private final DefaultedList<Ingredient> ingredients;

    public DefaultedList<Ingredient> getIngredients() {
        return ingredients;
    }

    private final Optional<RawUpgradableRecipe.Data> data;
    private final int ingredientCount;
    private final boolean symmetrical;

    public RawUpgradableRecipe(int width, int height, int upgradableItemSlot, DefaultedList<Ingredient> ingredients, Optional<RawUpgradableRecipe.Data> data) {
        this.width = width;
        this.height = height;
        this.upgradableItemSlot = upgradableItemSlot;
        this.ingredients = ingredients;
        this.data = data;
        int i = 0;

        for(Ingredient ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                ++i;
            }
        }

        this.ingredientCount = i;
        this.symmetrical = Util.isSymmetrical(width, height, ingredients);
    }

    private void writeToBuf(RegistryByteBuf buf) {
        buf.writeVarInt(this.width);
        buf.writeVarInt(this.height);

        for(Ingredient ingredient : this.ingredients) {
            Ingredient.PACKET_CODEC.encode(buf, ingredient);
        }

        buf.writeVarInt(this.upgradableItemSlot);
    }

    public boolean matches(CraftingRecipeInput input) {
        if (input.getStackCount() == this.ingredientCount) {
            if (input.getWidth() == this.width && input.getHeight() == this.height) {
                if (!this.symmetrical && this.matches(input, true)) {
                    return true;
                }

                return this.matches(input, false);
            }

        }
        return false;
    }

    private boolean matches(CraftingRecipeInput input, boolean mirrored) {
        for(int i = 0; i < this.height; ++i) {
            for(int j = 0; j < this.width; ++j) {
                Ingredient ingredient;
                if (mirrored) {
                    ingredient = this.ingredients.get(this.width - j - 1 + i * this.width);
                } else {
                    ingredient = this.ingredients.get(j + i * this.width);
                }

                ItemStack itemStack = input.getStackInSlot(j, i);
                if (!ingredient.test(itemStack)) {
                    return false;
                }
            }
        }

        this.mirrored = mirrored;
        return true;
    }

    static {
        CODEC = Data.CODEC.flatXmap(RawUpgradableRecipe::fromData, (recipe) ->
                recipe.data.map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe")));

        PACKET_CODEC = PacketCodec.of(RawUpgradableRecipe::writeToBuf, RawUpgradableRecipe::readFromBuf);
    }

    private static DataResult<? extends RawUpgradableRecipe> fromData(Data data) {
        // Clamp to box
        String[] pattern = stripWhitespace(data.pattern);
        int height = pattern.length;
        int width = pattern[0].length();

        // Create ingredient list
        DefaultedList<Ingredient> defaultedList = DefaultedList.ofSize(height * width, Ingredient.EMPTY);
        CharSet charSet = new CharArraySet(data.key.keySet());
        int upgradableSlot = -1;

        for(int k = 0; k < pattern.length; ++k) {
            String string = pattern[k];

            for(int l = 0; l < string.length(); ++l) {
                int slot = l + (k * width);
                char c = string.charAt(l);

                Ingredient ingredient = c == ' ' ? Ingredient.EMPTY : data.key.get(c).getFirst();
                boolean upgradable = c != ' ' && data.key.get(c).getSecond();
                if (ingredient == null) {
                    return DataResult.error(() -> "Pattern references symbol '" + c + "' but it's not defined in the key");
                }

                if(upgradable) {
                    if(upgradableSlot < 0) {
                        upgradableSlot = slot;
                    } else {
                        int finalSlot = upgradableSlot;
                        return DataResult.error(() -> "Pattern attempted to define slot #" + slot + " as upgradable, but slot #" + finalSlot + " is already upgradable");
                    }
                }

                charSet.remove(c);
                defaultedList.set(slot, ingredient);
            }
        }

        if(upgradableSlot < 0) {
            return DataResult.error(() -> "Pattern does not define a slot as upgradable");
        }

        // Construct
        if (!charSet.isEmpty()) {
            return DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charSet);
        } else {
            return DataResult.success(new RawUpgradableRecipe(width, height, upgradableSlot, defaultedList, Optional.of(data)));
        }

    }

    private static String[] stripWhitespace(List<String> strings) {
        // Find Box Size
        int a = strings.getFirst().length(); // x1
        int b = strings.size(); // y1
        int c = 0; // x2
        int d = 0; // y2

        for(int y = 0; y < strings.size(); y++) {
            for(int x = 0; x < strings.getFirst().length(); x++) {
                if (strings.get(y).charAt(x) == ' ') continue;

                if(x < a) a = x;
                if(x > c) c = x;

                if(y < b) b = y;
                if(y > d) d = y;
            }
        }

        // Reconstruct with excess whitespace stripped
        List<String> strippedStrings = new ArrayList<>();
        for(int y = 0; y < strings.size(); y++) {
            if(y < b || y > d) continue;
            strippedStrings.add(strings.get(y).substring(a, c+1));
        }

        return strippedStrings.toArray(new String[0]);
    }

    private static RawUpgradableRecipe readFromBuf(RegistryByteBuf buf) {
        int i = buf.readVarInt();
        int j = buf.readVarInt();
        DefaultedList<Ingredient> defaultedList = DefaultedList.ofSize(i * j, Ingredient.EMPTY);
        defaultedList.replaceAll((ingredient) -> Ingredient.PACKET_CODEC.decode(buf));
        int slot = buf.readVarInt();
        return new RawUpgradableRecipe(i, j, slot, defaultedList, Optional.empty());
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public record Data(Map<Character, Pair<Ingredient, Boolean>> key, List<String> pattern) {
        private static final Codec<List<String>> PATTERN_CODEC;
        private static final Codec<Pair<Ingredient, Boolean>> INGREDIENT_CODEC;
        private static final Codec<Character> KEY_ENTRY_CODEC;
        public static final MapCodec<RawUpgradableRecipe.Data> CODEC;

        static {
            INGREDIENT_CODEC = Codec.pair(
                    Ingredient.DISALLOW_EMPTY_CODEC,
                    Codec.BOOL.optionalFieldOf("upgradable", false).codec()
            );

            PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap((pattern) -> {
                        if (pattern.size() > 3) {
                            return DataResult.error(() -> "Invalid pattern: too many rows, 3 is maximum");
                        } else if (pattern.isEmpty()) {
                            return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
                        } else {
                            int i = (pattern.getFirst()).length();

                            for(String string : pattern) {
                                if (string.length() > 3) {
                                    return DataResult.error(() -> "Invalid pattern: too many columns, 3 is maximum");
                                }

                                if (i != string.length()) {
                                    return DataResult.error(() -> "Invalid pattern: each row must be the same width");
                                }
                            }

                            return DataResult.success(pattern);
                        }
                    }, Function.identity());

            KEY_ENTRY_CODEC = Codec.STRING.comapFlatMap((keyEntry) -> {
                if (keyEntry.length() != 1) {
                    return DataResult.error(() -> "Invalid key entry: '" + keyEntry + "' is an invalid symbol (must be 1 character only).");
                } else {
                    return " ".equals(keyEntry) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(keyEntry.charAt(0));
                }
            }, String::valueOf);

            CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
                    Codecs.strictUnboundedMap(KEY_ENTRY_CODEC, INGREDIENT_CODEC).fieldOf("key").forGetter((data) -> data.key),
                    PATTERN_CODEC.fieldOf("pattern").forGetter((data) -> data.pattern)
            ).apply(instance, Data::new));
        }
    }

}
