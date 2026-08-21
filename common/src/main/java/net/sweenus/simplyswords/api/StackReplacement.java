package net.sweenus.simplyswords.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class StackReplacement {
    private StackReplacement() {
    }

    public static ItemStack copyTo(ItemStack source, Item targetItem) {
        return copyTo(source, targetItem, source == null ? 0 : source.getCount());
    }

    public static ItemStack copyTo(ItemStack source, Item targetItem, int count) {
        if (source == null || source.isEmpty() || targetItem == null || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(targetItem, count);
        if (source.hasNbt()) {
            result.setNbt(source.getNbt().copy());
        }
        return result;
    }
}
