package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.recipe.RunicRerollRecipe;
import net.sweenus.simplyswords.recipe.UpgradeUniqueRecipe;

public class RecipeTypeRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES =
            DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<UpgradeUniqueRecipe>> UNIQUE_UPGRADE =
            RECIPES.register(
                    "unique_upgrade",
                    UpgradeUniqueRecipe.Serializer::new
            );

    public static final RegistrySupplier<RecipeSerializer<RunicRerollRecipe>> REROLL =
            RECIPES.register(
                    "smithing_reroll",
                    RunicRerollRecipe.Serializer::new
            );
}
