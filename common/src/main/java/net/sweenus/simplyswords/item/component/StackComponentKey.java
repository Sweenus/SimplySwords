package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * 1.20.1-compatible storage key for Simply Swords' typed stack state.
 *
 * <p>Minecraft's data-component system does not exist until 1.20.5. Keeping the
 * component records and codecs behind this adapter lets the backport retain the
 * same API-level value types while storing their encoded form in stack NBT.</p>
 */
public final class StackComponentKey<T> {
    private static final String ROOT_KEY = "SimplySwordsComponents";

    private final Identifier id;
    private final Codec<T> codec;

    public StackComponentKey(Identifier id, Codec<T> codec) {
        this.id = id;
        this.codec = codec;
    }

    public Identifier id() {
        return id;
    }

    public T get(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasNbt()) return null;
        NbtCompound root = stack.getNbt().getCompound(ROOT_KEY);
        String key = id.toString();
        if (!root.contains(key)) return null;
        DataResult<T> decoded = codec.parse(NbtOps.INSTANCE, root.get(key));
        Optional<T> value = decoded.result();
        return value.orElse(null);
    }

    public T getOrDefault(ItemStack stack, T fallback) {
        T value = get(stack);
        return value == null ? fallback : value;
    }

    public boolean contains(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasNbt()) return false;
        return stack.getNbt().getCompound(ROOT_KEY).contains(id.toString());
    }

    public void set(ItemStack stack, T value) {
        if (stack == null || stack.isEmpty()) return;
        if (value == null) {
            remove(stack);
            return;
        }
        DataResult<NbtElement> encoded = codec.encodeStart(NbtOps.INSTANCE, value);
        encoded.result().ifPresent(element -> {
            NbtCompound stackNbt = stack.getOrCreateNbt();
            NbtCompound root = stackNbt.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)
                    ? stackNbt.getCompound(ROOT_KEY)
                    : new NbtCompound();
            root.put(id.toString(), element);
            stackNbt.put(ROOT_KEY, root);
        });
    }

    public void remove(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasNbt()) return;
        NbtCompound stackNbt = stack.getNbt();
        if (!stackNbt.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) return;
        NbtCompound root = stackNbt.getCompound(ROOT_KEY);
        root.remove(id.toString());
        if (root.isEmpty()) stackNbt.remove(ROOT_KEY);
        else stackNbt.put(ROOT_KEY, root);
    }

    public T apply(ItemStack stack, T fallback, UnaryOperator<T> updater) {
        T updated = updater.apply(getOrDefault(stack, fallback));
        set(stack, updated);
        return updated;
    }
}
