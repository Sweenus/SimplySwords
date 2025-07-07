package net.sweenus.simplyswords.client.util;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class EmiUtils {

    public static void openEmi(Identifier identifier, String modId, ItemStack itemStack) {
        // Open the recipe view for the specified item
        EmiIngredient emiStack = EmiStack.of(itemStack);
        EmiApi.displayRecipes(emiStack);
    }

}
