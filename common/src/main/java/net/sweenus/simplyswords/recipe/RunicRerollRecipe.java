package net.sweenus.simplyswords.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.RecipeTypeRegistry;
import net.sweenus.simplyswords.api.StackReplacement;

public class RunicRerollRecipe extends SmithingTransformRecipe {
    final Ingredient template;
    final Ingredient base;
    final Ingredient addition;
    final ItemStack result;

    public RunicRerollRecipe(Ingredient template, Ingredient base, Ingredient addition, ItemStack result) {
        super(template, base, addition, result);

        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    @Override
    public ItemStack craft(SmithingRecipeInput smithingRecipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
        ItemStack itemStack = StackReplacement.copyTo(
                smithingRecipeInput.base(), this.result.getItem(), this.result.getCount());
        itemStack.applyUnvalidatedChanges(this.result.getComponentChanges());
        itemStack.remove(ComponentTypeRegistry.GEM_POWER.get());
        return itemStack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeTypeRegistry.REROLL.get();
    }

    public static class Serializer implements RecipeSerializer<RunicRerollRecipe> {
        private static final MapCodec<RunicRerollRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(Ingredient.ALLOW_EMPTY_CODEC.fieldOf("template").forGetter((recipe) -> recipe.template), Ingredient.ALLOW_EMPTY_CODEC.fieldOf("base").forGetter((recipe) -> recipe.base), Ingredient.ALLOW_EMPTY_CODEC.fieldOf("addition").forGetter((recipe) -> recipe.addition), ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter((recipe) -> recipe.result)).apply(instance, RunicRerollRecipe::new));
        public static final PacketCodec<RegistryByteBuf, RunicRerollRecipe> PACKET_CODEC = PacketCodec.ofStatic(RunicRerollRecipe.Serializer::write, RunicRerollRecipe.Serializer::read);

        public Serializer() {
        }

        public MapCodec<RunicRerollRecipe> codec() {
            return CODEC;
        }

        public PacketCodec<RegistryByteBuf, RunicRerollRecipe> packetCodec() {
            return PACKET_CODEC;
        }

        private static RunicRerollRecipe read(RegistryByteBuf buf) {
            Ingredient ingredient = Ingredient.PACKET_CODEC.decode(buf);
            Ingredient ingredient2 = Ingredient.PACKET_CODEC.decode(buf);
            Ingredient ingredient3 = Ingredient.PACKET_CODEC.decode(buf);
            ItemStack itemStack = ItemStack.PACKET_CODEC.decode(buf);
            return new RunicRerollRecipe(ingredient, ingredient2, ingredient3, itemStack);
        }

        private static void write(RegistryByteBuf buf, RunicRerollRecipe recipe) {
            Ingredient.PACKET_CODEC.encode(buf, recipe.template);
            Ingredient.PACKET_CODEC.encode(buf, recipe.base);
            Ingredient.PACKET_CODEC.encode(buf, recipe.addition);
            ItemStack.PACKET_CODEC.encode(buf, recipe.result);
        }
    }
}
