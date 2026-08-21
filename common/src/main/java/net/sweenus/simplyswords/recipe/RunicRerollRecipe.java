package net.sweenus.simplyswords.recipe;

import com.google.gson.JsonObject;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.RecipeTypeRegistry;
import net.sweenus.simplyswords.api.StackReplacement;

public class RunicRerollRecipe extends SmithingTransformRecipe {
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;

    public RunicRerollRecipe(Identifier id, Ingredient template, Ingredient base,
                             Ingredient addition, ItemStack result) {
        super(id, template, base, addition, result);
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    @Override
    public ItemStack craft(Inventory inventory, DynamicRegistryManager registryManager) {
        ItemStack source = inventory.getStack(1);
        ItemStack output = StackReplacement.copyTo(source, result.getItem(), result.getCount());
        ComponentTypeRegistry.GEM_POWER.remove(output);
        return output;
    }

    @Override public RecipeSerializer<?> getSerializer() { return RecipeTypeRegistry.REROLL.get(); }

    public static final class Serializer implements RecipeSerializer<RunicRerollRecipe> {
        @Override
        public RunicRerollRecipe read(Identifier id, JsonObject json) {
            Ingredient template = Ingredient.fromJson(JsonHelper.getElement(json, "template"));
            Ingredient base = Ingredient.fromJson(JsonHelper.getElement(json, "base"));
            Ingredient addition = Ingredient.fromJson(JsonHelper.getElement(json, "addition"));
            JsonObject resultJson = JsonHelper.getObject(json, "result");
            String itemId = JsonHelper.getString(resultJson, resultJson.has("id") ? "id" : "item");
            ItemStack result = new ItemStack(Registries.ITEM.get(new Identifier(itemId)),
                    JsonHelper.getInt(resultJson, "count", 1));
            return new RunicRerollRecipe(id, template, base, addition, result);
        }

        @Override
        public RunicRerollRecipe read(Identifier id, PacketByteBuf buf) {
            return new RunicRerollRecipe(id, Ingredient.fromPacket(buf), Ingredient.fromPacket(buf),
                    Ingredient.fromPacket(buf), buf.readItemStack());
        }

        @Override
        public void write(PacketByteBuf buf, RunicRerollRecipe recipe) {
            recipe.template.write(buf);
            recipe.base.write(buf);
            recipe.addition.write(buf);
            buf.writeItemStack(recipe.result);
        }
    }
}
