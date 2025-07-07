package net.sweenus.simplyswords.client.util;

import me.shedaniel.rei.api.client.ClientHelper;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ReiUtils {

    public static void openRei(Identifier identifier, String modId, ItemStack itemStack) {
        // Create EntryStack for the item
        EntryStack<?> stack = EntryStacks.of(itemStack);
        // Build the view for the item's usages
        ViewSearchBuilder view = ViewSearchBuilder.builder().addRecipesFor(stack).addUsagesFor(stack);
        // Open the REI view
        ClientHelper.getInstance().openView(view);
    }

}
