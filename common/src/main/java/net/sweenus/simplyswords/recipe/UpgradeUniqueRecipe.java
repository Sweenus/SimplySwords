package net.sweenus.simplyswords.recipe;

import com.google.gson.JsonObject;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.RecipeTypeRegistry;

public class UpgradeUniqueRecipe extends ShapedRecipe {
    private final RawUpgradableRecipe raw;
    private final ItemStack result;

    public UpgradeUniqueRecipe(Identifier id, String group, CraftingRecipeCategory category,
                               RawUpgradableRecipe raw, ItemStack result, boolean showNotification) {
        super(id, group, category, raw.getWidth(), raw.getHeight(), raw.getIngredients(), result, showNotification);
        this.raw = raw;
        this.result = result;
    }

    @Override public boolean matches(RecipeInputInventory input, World world) { return raw.matches(input); }

    @Override
    public ItemStack craft(RecipeInputInventory input, DynamicRegistryManager registryManager) {
        int slot = raw.getUpgradableItemSlot();
        if (raw.isMirrored()) {
            int x = slot % raw.getWidth();
            int y = slot / raw.getWidth();
            slot = y * raw.getWidth() + raw.getWidth() - 1 - x;
        }
        ItemStack output = result.copy();
        ItemStack source = input.getStack(slot);
        if (source.hasNbt()) output.setNbt(source.getNbt().copy());
        return output;
    }

    @Override public RecipeSerializer<?> getSerializer() { return RecipeTypeRegistry.UNIQUE_UPGRADE.get(); }

    public static final class Serializer implements RecipeSerializer<UpgradeUniqueRecipe> {
        @Override
        public UpgradeUniqueRecipe read(Identifier id, JsonObject json) {
            String group = JsonHelper.getString(json, "group", "");
            CraftingRecipeCategory category = CraftingRecipeCategory.CODEC
                    .parse(com.mojang.serialization.JsonOps.INSTANCE,
                            json.has("category") ? json.get("category") : new com.google.gson.JsonPrimitive("misc"))
                    .result().orElse(CraftingRecipeCategory.MISC);
            RawUpgradableRecipe raw = RawUpgradableRecipe.fromJson(JsonHelper.getObject(json, "raw"));
            JsonObject resultJson = JsonHelper.getObject(json, "result");
            String itemId = JsonHelper.getString(resultJson, resultJson.has("id") ? "id" : "item");
            ItemStack result = new ItemStack(Registries.ITEM.get(new Identifier(itemId)),
                    JsonHelper.getInt(resultJson, "count", 1));
            return new UpgradeUniqueRecipe(id, group, category, raw, result,
                    JsonHelper.getBoolean(json, "show_notification", true));
        }

        @Override
        public UpgradeUniqueRecipe read(Identifier id, PacketByteBuf buf) {
            String group = buf.readString();
            CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
            RawUpgradableRecipe raw = RawUpgradableRecipe.read(buf);
            ItemStack result = buf.readItemStack();
            return new UpgradeUniqueRecipe(id, group, category, raw, result, buf.readBoolean());
        }

        @Override
        public void write(PacketByteBuf buf, UpgradeUniqueRecipe recipe) {
            buf.writeString(recipe.getGroup());
            buf.writeEnumConstant(recipe.getCategory());
            recipe.raw.write(buf);
            buf.writeItemStack(recipe.result);
            buf.writeBoolean(recipe.showNotification());
        }
    }
}
