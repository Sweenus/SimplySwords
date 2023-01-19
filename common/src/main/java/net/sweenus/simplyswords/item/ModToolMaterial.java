package net.sweenus.simplyswords.item;

import net.fabricmc.yarn.constants.MiningLevels;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.Lazy;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.function.Supplier;

public enum ModToolMaterial implements ToolMaterial {
    RUNIC(MiningLevels.NETHERITE, 2031, 9.0f, 5.0f, 25, () -> Ingredient.ofItems(Items.NETHERITE_INGOT)),
    UNIQUE(MiningLevels.NETHERITE, 3270, 15.0f, 5.0f, 30, () -> Ingredient.ofItems(ItemsRegistry.RUNIC_TABLET.get())),

    //MYTHIC METALS
    ADAMANTITE(4, 1024, 7.0F, 5F, 16, () -> Ingredient.ofItems(Items.DIAMOND)),
    AEGIS_RED(4, 2170, 8.0F, 6.0F, 25, () -> Ingredient.ofItems(Items.DIAMOND)),
    AEGIS_WHITE(4, 2070, 10.0F, 5.0F, 25, () -> Ingredient.ofItems(Items.DIAMOND)),
    AQUARIUM(2, 455, 6.5F, 2.0F, 12, () -> Ingredient.ofItems(Items.DIAMOND)),
    BANGLUM(2, 260, 11.0F, 2.0F, 1, () -> Ingredient.ofItems(Items.DIAMOND)),
    BRONZE(2, 354, 5.5F, 2.5F, 14, () -> Ingredient.ofItems(Items.DIAMOND)),
    CARMOT(3, 1130, 11.5F, 3.0F, 42, () -> Ingredient.ofItems(Items.DIAMOND)),
    CELESTIUM(5, 2270, 16.9F, 6.0F, 26, () -> Ingredient.ofItems(Items.DIAMOND)),
    COPPER(1, 125, 4.5F, 1.0F, 8, () -> Ingredient.ofItems(Items.COPPER_INGOT)),
    DURASTEEL(3, 800, 7.1F, 3.5F, 12, () -> Ingredient.ofItems(Items.DIAMOND)),
    GILDED_MIDAS_GOLD(3, 999, 13.0F, 4.0F, 30, () -> Ingredient.ofItems(Items.DIAMOND)),
    HALLOWED(4, 1984, 12.0F, 5.0F, 20, () -> Ingredient.ofItems(Items.DIAMOND)),
    KYBER(3, 889, 7.0F, 2.5F, 20, () -> Ingredient.ofItems(Items.DIAMOND)),
    LEGENDARY_BANGLUM(3, 1040, 12.0F, 4.0F, 2, () -> Ingredient.ofItems(Items.DIAMOND)),
    METALLURGIUM(5, 3000, 15.0F, 8.0F, 30, () -> Ingredient.ofItems(Items.DIAMOND)),
    MIDAS_GOLD(3, 300, 13.0F, 3.0F, 30, () -> Ingredient.ofItems(Items.DIAMOND)),
    MYTHRIL(4, 1564, 13.0F, 3.0F, 22, () -> Ingredient.ofItems(Items.DIAMOND)),
    ORICHALCUM(4, 2048, 6.0F, 4.0F, 16, () -> Ingredient.ofItems(Items.DIAMOND)),
    OSMIUM(2, 584, 7.0F, 2.0F, 13, () -> Ingredient.ofItems(Items.DIAMOND)),
    PALLADIUM(4, 1234, 8.0F, 3.5F, 16, () -> Ingredient.ofItems(Items.DIAMOND)),
    PROMETHEUM(3, 1472, 6.0F, 4.0F, 15, () -> Ingredient.ofItems(Items.DIAMOND)),
    QUADRILLUM(2, 321, 5.0F, 2.5F, 8, () -> Ingredient.ofItems(Items.DIAMOND)),
    RUNITE(3, 1337, 8.9F, 3.3F, 17, () -> Ingredient.ofItems(Items.DIAMOND)),
    STAR_PLATINUM(4, 1300, 9.0F, 4.0F, 18, () -> Ingredient.ofItems(Items.DIAMOND)),
    STEEL(2, 600, 6.5F, 2.5F, 12, () -> Ingredient.ofItems(Items.DIAMOND)),
    STORMYX(3, 1305, 8.0F, 3.5F, 20, () -> Ingredient.ofItems(Items.DIAMOND)),


    //GOBBER
    GOBBER(5, 3800, 9.0F, 9.0F, 20, () -> Ingredient.ofItems(Items.DIAMOND)),
    GOBBER_NETHER(6, 5200, 12.0F, 9.0F, 25, () -> Ingredient.ofItems(Items.DIAMOND)),
    GOBBER_END(7, 8000, 14.0F, 9.0F, 30, () -> Ingredient.ofItems(Items.DIAMOND));

    private final int miningLevel;
    private final int itemDurability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final Lazy<Ingredient> repairIngredient;

    ModToolMaterial(int miningLevel, int itemDurability, float miningSpeed, float attackDamage, int enchantability, Supplier<Ingredient> repairIngredient) {
        this.miningLevel = miningLevel;
        this.itemDurability = itemDurability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        this.repairIngredient = new Lazy<>(repairIngredient);
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
    public int getMiningLevel() {
        return this.miningLevel;
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
