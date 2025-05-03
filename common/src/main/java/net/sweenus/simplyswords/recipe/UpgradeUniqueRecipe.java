
package net.sweenus.simplyswords.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.registry.RecipeTypeRegistry;

public class UpgradeUniqueRecipe extends ShapedRecipe {
    private final int upgradableItemSlot;
    private final ItemStack result;
    private final RawUpgradableRecipe raw;

    public UpgradeUniqueRecipe(String group, CraftingRecipeCategory category, RawUpgradableRecipe raw, ItemStack result, boolean showNotification) {
        super(group, category, null, result, showNotification);
        this.raw = raw;
        this.result = result;
        this.upgradableItemSlot = raw.getUpgradableItemSlot();
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= this.raw.getWidth() && height >= this.raw.getHeight();
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return this.raw.getIngredients();
    }

    @Override
    public int getHeight() {
        return this.raw.getHeight();
    }

    @Override
    public int getWidth() {
        return this.raw.getWidth();
    }

    @Override
    public boolean matches(CraftingRecipeInput craftingRecipeInput, World world) {
        return raw.matches(craftingRecipeInput);
    }

    @Override
    public ItemStack craft(CraftingRecipeInput craftingRecipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
        int slot = upgradableItemSlot;
        if(raw.isMirrored()) {
            int width = getWidth();
            int x = slot % width;
            int y = slot / width;

            slot = (y * width) + (width - 1 - x);
        }

        ItemStack result = this.result.copy();
        result.applyComponentsFrom(craftingRecipeInput.getStackInSlot(slot).getComponents());

        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {return RecipeTypeRegistry.UNIQUE_UPGRADE.get();}

    public static class Serializer implements RecipeSerializer<UpgradeUniqueRecipe> {

        public static final MapCodec<UpgradeUniqueRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapedRecipe::getCategory),
                RawUpgradableRecipe.CODEC.forGetter((recipe) -> recipe.raw),
                ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter((recipe) -> recipe.result),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification)
        ).apply(instance, UpgradeUniqueRecipe::new));

        public static final PacketCodec<RegistryByteBuf, UpgradeUniqueRecipe> PACKET_CODEC = PacketCodec.ofStatic(UpgradeUniqueRecipe.Serializer::write, UpgradeUniqueRecipe.Serializer::read);

        public Serializer() {
        }

        public MapCodec<UpgradeUniqueRecipe> codec() {
            return CODEC;
        }

        public PacketCodec<RegistryByteBuf, UpgradeUniqueRecipe> packetCodec() {
            return PACKET_CODEC;
        }

        private static UpgradeUniqueRecipe read(RegistryByteBuf buf) {
            String string = buf.readString();
            CraftingRecipeCategory craftingRecipeCategory = buf.readEnumConstant(CraftingRecipeCategory.class);
            RawUpgradableRecipe rawShapedRecipe = RawUpgradableRecipe.PACKET_CODEC.decode(buf);
            ItemStack itemStack = ItemStack.PACKET_CODEC.decode(buf);
            boolean bl = buf.readBoolean();
            return new UpgradeUniqueRecipe(string, craftingRecipeCategory, rawShapedRecipe, itemStack, bl);
        }

        private static void write(RegistryByteBuf buf, UpgradeUniqueRecipe recipe) {
            buf.writeString(recipe.getGroup());
            buf.writeEnumConstant(recipe.getCategory());
            RawUpgradableRecipe.PACKET_CODEC.encode(buf, recipe.raw);
            ItemStack.PACKET_CODEC.encode(buf, recipe.result);
            buf.writeBoolean(recipe.showNotification());
        }
    }
}
