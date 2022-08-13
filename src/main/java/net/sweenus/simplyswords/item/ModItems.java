package net.sweenus.simplyswords.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Rarity;
import net.sweenus.simplyswords.SimplySwords;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.item.custom.*;

public class ModItems {
   // public static final Item ITEMNAMEHERE = registerItem( "item_name",
        //new Item(new FabricItemSettings().group(ItemGroup.MISC)));

    public static final Item RUNIC_LONGSWORD = registerItem( "runic_longsword",
            new ElectricSwordItem(ModToolMaterial.ELEMENTITE, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_TWINBLADE = registerItem( "runic_twinblade",
            new WildfireSwordItem(ModToolMaterial.ELEMENTITE, 2, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item TOXIC_LONGSWORD = registerItem( "toxic_longsword",
            new PoisonSwordItem(ModToolMaterial.ELEMENTITE, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item IRON_LONGSWORD = registerItem( "iron_longsword",
            new FreezeSwordItem(ModToolMaterial.ELEMENTITE, 0, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item GOLD_LONGSWORD = registerItem( "gold_longsword",
            new FreezeSwordItem(ModToolMaterial.ELEMENTITE, -1, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item DIAMOND_LONGSWORD = registerItem( "diamond_longsword",
            new FreezeSwordItem(ModToolMaterial.ELEMENTITE, 1, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item NETHERITE_LONGSWORD = registerItem( "netherite_longsword",
            new FreezeSwordItem(ModToolMaterial.ELEMENTITE, 2, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNIC_RAPIER = registerItem( "runic_rapier",
            new SpeedSwordItem(ModToolMaterial.ELEMENTITE, 1, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item NETHERITE_RAPIER = registerItem( "netherite_rapier",
            new SwordItem(ModToolMaterial.ELEMENTITE, 1, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item DIAMOND_RAPIER = registerItem( "diamond_rapier",
            new SwordItem(ModToolMaterial.ELEMENTITE, 0, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item GOLD_RAPIER = registerItem( "gold_rapier",
            new SwordItem(ModToolMaterial.ELEMENTITE, -2, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item IRON_RAPIER = registerItem( "iron_rapier",
            new SwordItem(ModToolMaterial.ELEMENTITE, -1, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item CURVED_SWORD = registerItem( "curved_sword",
            new SwordItem(ModToolMaterial.ELEMENTITE, 0, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item GOLD_CURVED_SWORD = registerItem( "gold_curved_sword",
            new SwordItem(ModToolMaterial.ELEMENTITE, -1, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item DIAMOND_CURVED_SWORD = registerItem( "diamond_curved_sword",
            new SwordItem(ModToolMaterial.ELEMENTITE, 1, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNIC_SWORD = registerItem( "runic_sword",
            new FreezeSwordItem(ModToolMaterial.ELEMENTITE, 0, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item DIAMOND_CLAYMORE = registerItem( "diamond_claymore",
            new SwordItem(ModToolMaterial.ELEMENTITE, 3, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNIC_CLAYMORE = registerItem( "runic_claymore",
            new FreezeSwordItem(ModToolMaterial.ELEMENTITE, 4, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item BRIMSTONE_CLAYMORE = registerItem( "brimstone_claymore",
            new FireSwordItem(ModToolMaterial.ELEMENTITE, 4, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item GOLD_CLAYMORE = registerItem( "gold_claymore",
            new SwordItem(ModToolMaterial.ELEMENTITE, 1, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item IRON_CLAYMORE = registerItem( "iron_claymore",
            new SwordItem(ModToolMaterial.ELEMENTITE, 2, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }

    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mod Items for " + SimplySwords.MOD_ID);
    }

}
