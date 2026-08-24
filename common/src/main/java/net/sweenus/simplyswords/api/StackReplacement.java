package net.sweenus.simplyswords.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class StackReplacement {
    private StackReplacement() {
    }

    public static ItemStack copyTo(ItemStack source, Item targetItem) {
        if (source == null || source.isEmpty() || targetItem == null) {
            return ItemStack.EMPTY;
        }
        return source.copyComponentsToNewStack(targetItem, source.getCount());
    }

    public static ItemStack copyTo(ItemStack source, Item targetItem, int count) {
        if (source == null || source.isEmpty() || targetItem == null || count <= 0) {
            return ItemStack.EMPTY;
        }
        return source.copyComponentsToNewStack(targetItem, count);
    }
}
