package net.sweenus.simplyswords.item;

import com.google.common.base.Suppliers;
import net.fabricmc.yarn.constants.MiningLevels;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.function.Supplier;

public enum ModToolMaterial implements ToolMaterial {
    RUNIC(MiningLevels.NETHERITE, 2031, 9.0f, 5.0f, 25, Items.NETHERITE_INGOT),
    UNIQUE(MiningLevels.NETHERITE, 3270, 15.0f, 5.0f, 30, ItemsRegistry.RUNIC_TABLET.get()),

    //MYTHIC METALS
    ADAMANTITE(4, 1024, 7.0F, 5F, 16, Items.DIAMOND),
    AEGIS_RED(4, 2170, 8.0F, 6.0F, 25, Items.DIAMOND),
    AEGIS_WHITE(4, 2070, 10.0F, 5.0F, 25, Items.DIAMOND),
    AQUARIUM(2, 455, 6.5F, 2.0F, 12, Items.DIAMOND),
    BANGLUM(2, 260, 11.0F, 2.0F, 1, Items.DIAMOND),
    BRONZE(2, 354, 5.5F, 2.5F, 14, Items.DIAMOND),
    CARMOT(3, 1130, 11.5F, 3.0F, 42, Items.DIAMOND),
    CELESTIUM(5, 2270, 16.9F, 6.0F, 26, Items.DIAMOND),
    COPPER(1, 125, 4.5F, 1.0F, 8, Items.COPPER_INGOT),
    DURASTEEL(3, 800, 7.1F, 3.5F, 12, Items.DIAMOND),
    GILDED_MIDAS_GOLD(3, 999, 13.0F, 4.0F, 30, Items.DIAMOND),
    HALLOWED(4, 1984, 12.0F, 5.0F, 20, Items.DIAMOND),
    KYBER(3, 889, 7.0F, 2.5F, 20, Items.DIAMOND),
    LEGENDARY_BANGLUM(3, 1040, 12.0F, 4.0F, 2, Items.DIAMOND),
    METALLURGIUM(5, 3000, 15.0F, 8.0F, 30, Items.DIAMOND),
    MIDAS_GOLD(3, 300, 13.0F, 3.0F, 30, Items.DIAMOND),
    MYTHRIL(4, 1564, 13.0F, 3.0F, 22, Items.DIAMOND),
    ORICHALCUM(4, 2048, 6.0F, 4.0F, 16, Items.DIAMOND),
    OSMIUM(2, 584, 7.0F, 2.0F, 13, Items.DIAMOND),
    PALLADIUM(4, 1234, 8.0F, 3.5F, 16, Items.DIAMOND),
    PROMETHEUM(3, 1472, 6.0F, 4.0F, 15, Items.DIAMOND),
    QUADRILLUM(2, 321, 5.0F, 2.5F, 8, Items.DIAMOND),
    RUNITE(3, 1337, 8.9F, 3.3F, 17, Items.DIAMOND),
    STAR_PLATINUM(4, 1300, 9.0F, 4.0F, 18, Items.DIAMOND),
    STEEL(2, 600, 6.5F, 2.5F, 12, Items.DIAMOND),
    STORMYX(3, 1305, 8.0F, 3.5F, 20, Items.DIAMOND),


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
