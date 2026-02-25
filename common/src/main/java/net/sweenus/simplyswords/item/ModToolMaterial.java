package net.sweenus.simplyswords.item;

import com.google.common.base.Suppliers;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.function.Supplier;

public enum ModToolMaterial implements ToolMaterial {
    RUNIC(4, 2031, 9.0f, 5.0f, 25, Items.NETHERITE_INGOT),
    UNIQUE(4, 3270, 15.0f, 5.0f, 30, ItemsRegistry.RUNIC_TABLET.get()),

    //GOBBER
    GOBBER(5, 3800, 9.0F, 9.0F, 20, Items.DIAMOND),
    GOBBER_NETHER(6, 5200, 12.0F, 9.0F, 25, Items.DIAMOND),
    GOBBER_END(7, 8000, 14.0F, 9.0F, 30, Items.DIAMOND);

    private final int miningLevel;
    private final int itemDurability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final Supplier<Ingredient> repairIngredient;

    ModToolMaterial(int miningLevel, int itemDurability, float miningSpeed, float attackDamage, int enchantability, Item... repairIngredient) {
        this.miningLevel = miningLevel;
        this.itemDurability = itemDurability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        this.repairIngredient = Suppliers.memoize(() -> Ingredient.ofItems(repairIngredient));
    }

    @Override
    public int getDurability() {
        return this.itemDurability;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return this.miningSpeed;
    }

    @Override
    public float getAttackDamage() {
        return this.attackDamage;
    }

    @Override
    public TagKey<Block> getInverseTag() {
        return TagKey.of(RegistryKeys.BLOCK, Identifier.of("simplyswords", "empty_inverse_tag"));
    }

    @Override
    public int getEnchantability() {
        return this.enchantability;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }
}
