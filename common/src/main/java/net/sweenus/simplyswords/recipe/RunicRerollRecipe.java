package net.sweenus.simplyswords.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.RecipeTypeRegistry;

import java.util.List;
import java.util.Optional;

public class RunicRerollRecipe implements SmithingRecipe {
    final Ingredient template;
    final Ingredient base;
    final Ingredient addition;
    final ItemStack result;

    public RunicRerollRecipe(Ingredient template, Ingredient base, Ingredient addition, ItemStack result) {
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, World world) {
        return template.test(input.template()) && base.test(input.base()) && addition.test(input.addition());
    }

    @Override
    public ItemStack craft(SmithingRecipeInput smithingRecipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
        ItemStack itemStack = smithingRecipeInput.base().copyComponentsToNewStack(this.result.getItem(), this.result.getCount());
        itemStack.applyUnvalidatedChanges(this.result.getComponentChanges());
        itemStack.remove(ComponentTypeRegistry.GEM_POWER.get());
        return itemStack;
    }

    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return result;
    }

    @Override
    public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
        return RecipeTypeRegistry.REROLL.get();
    }

    @Override
    public RecipeType<SmithingRecipe> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.forMultipleSlots(List.of(template(), Optional.of(base()), addition()));
    }

    @Override
    public Ingredient base() {
        return base;
    }

    @Override
    public Optional<Ingredient> addition() {
        return Optional.of(addition);
    }

    @Override
    public Optional<Ingredient> template() {
        return Optional.of(template);
    }

    public static class Serializer implements RecipeSerializer<RunicRerollRecipe> {
        private static final MapCodec<RunicRerollRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
                Ingredient.CODEC.fieldOf("template").forGetter((recipe) -> recipe.template),
                Ingredient.CODEC.fieldOf("base").forGetter((recipe) -> recipe.base),
                Ingredient.CODEC.fieldOf("addition").forGetter((recipe) -> recipe.addition),
                ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter((recipe) -> recipe.result)
        ).apply(instance, RunicRerollRecipe::new));
        public static final PacketCodec<RegistryByteBuf, RunicRerollRecipe> PACKET_CODEC = PacketCodec.ofStatic(RunicRerollRecipe.Serializer::write, RunicRerollRecipe.Serializer::read);

        @Override
        public MapCodec<RunicRerollRecipe> codec() {
            return CODEC;
        }

        @Override
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
