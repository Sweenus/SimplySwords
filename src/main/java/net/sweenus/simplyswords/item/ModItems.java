package net.sweenus.simplyswords.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Rarity;
import net.sweenus.simplyswords.SimplySwords;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.item.custom.*;

public class ModItems {
   // public static final Item ITEMNAMEHERE = registerItem( "item_name",
        //new Item(new FabricItemSettings().group(ItemGroup.MISC)));


    //IRON
    public static final Item IRON_LONGSWORD = registerItem( "iron_longsword",
            new SwordItem(ToolMaterials.IRON, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item IRON_TWINBLADE = registerItem( "iron_twinblade",
            new SwordItem(ToolMaterials.IRON, 3, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item IRON_RAPIER = registerItem( "iron_rapier",
            new SwordItem(ToolMaterials.IRON, 2, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item IRON_KATANA = registerItem( "iron_katana",
            new SwordItem(ToolMaterials.IRON, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item IRON_SAI = registerItem( "iron_sai",
            new SwordItem(ToolMaterials.IRON, 1, -0.5f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item IRON_CUTLASS = registerItem( "iron_cutlass",
            new SwordItem(ToolMaterials.IRON, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item IRON_CLAYMORE = registerItem( "iron_claymore",
            new SwordItem(ToolMaterials.IRON, 5, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    //GOLD
    public static final Item GOLD_LONGSWORD = registerItem( "gold_longsword",
            new SwordItem(ToolMaterials.GOLD, 2, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item GOLD_TWINBLADE = registerItem( "gold_twinblade",
            new SwordItem(ToolMaterials.GOLD, 2, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item GOLD_RAPIER = registerItem( "gold_rapier",
            new SwordItem(ToolMaterials.GOLD, 1, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item GOLD_KATANA = registerItem( "gold_katana",
            new SwordItem(ToolMaterials.GOLD, 2, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item GOLD_SAI = registerItem( "gold_sai",
            new SwordItem(ToolMaterials.GOLD, 0, -0.5f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item GOLD_CUTLASS = registerItem( "gold_cutlass",
            new SwordItem(ToolMaterials.GOLD, 2, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item GOLD_CLAYMORE = registerItem( "gold_claymore",
            new SwordItem(ToolMaterials.GOLD, 4, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    //DIAMOND
    public static final Item DIAMOND_LONGSWORD = registerItem( "diamond_longsword",
            new SwordItem(ToolMaterials.DIAMOND, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item DIAMOND_TWINBLADE = registerItem( "diamond_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, 3, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item DIAMOND_RAPIER = registerItem( "diamond_rapier",
            new SwordItem(ToolMaterials.DIAMOND, 2, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item DIAMOND_KATANA = registerItem( "diamond_katana",
            new SwordItem(ToolMaterials.DIAMOND, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item DIAMOND_SAI = registerItem( "diamond_sai",
            new SwordItem(ToolMaterials.DIAMOND, 1, -0.5f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item DIAMOND_CUTLASS = registerItem( "diamond_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item DIAMOND_CLAYMORE = registerItem( "diamond_claymore",
            new SwordItem(ToolMaterials.DIAMOND, 5, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    //NETHERITE

    public static final Item NETHERITE_LONGSWORD = registerItem( "netherite_longsword",
            new SwordItem(ToolMaterials.NETHERITE, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item NETHERITE_TWINBLADE = registerItem( "netherite_twinblade",
            new SwordItem(ToolMaterials.NETHERITE, 3, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item NETHERITE_RAPIER = registerItem( "netherite_rapier",
            new SwordItem(ToolMaterials.NETHERITE, 2, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item NETHERITE_KATANA = registerItem( "netherite_katana",
            new SwordItem(ToolMaterials.NETHERITE, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item NETHERITE_SAI = registerItem( "netherite_sai",
            new SwordItem(ToolMaterials.NETHERITE, 1, -0.5f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item NETHERITE_CUTLASS = registerItem( "netherite_cutlass",
            new SwordItem(ToolMaterials.NETHERITE, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    public static final Item NETHERITE_CLAYMORE = registerItem( "netherite_claymore",
            new SwordItem(ToolMaterials.NETHERITE, 6, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    //RUNIC
    public static final Item RUNIC_LONGSWORD = registerItem( "runic_longsword",
            new PoisonSwordItem(ToolMaterials.NETHERITE, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_TWINBLADE = registerItem( "runic_twinblade",
            new WildfireSwordItem(ToolMaterials.NETHERITE, 3, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_KATANA = registerItem( "runic_katana",
            new WildfireSwordItem(ToolMaterials.NETHERITE, 3, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_GLAIVE = registerItem( "runic_glaive",
            new WildfireSwordItem(ToolMaterials.NETHERITE, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_WARGLAIVE = registerItem( "runic_warglaive",
            new SpeedSwordItem(ToolMaterials.NETHERITE, 3, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_RAPIER = registerItem( "runic_rapier",
            new SpeedSwordItem(ToolMaterials.NETHERITE, 2, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));
    public static final Item RUNIC_CUTLASS = registerItem( "runic_cutlass",
            new LevitationSwordItem(ToolMaterials.NETHERITE, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item RUNIC_CLAYMORE = registerItem( "runic_claymore",
            new FreezeSwordItem(ToolMaterials.NETHERITE, 6, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

//SPECIAL
    public static final Item BRIMSTONE_CLAYMORE = registerItem( "brimstone_claymore",
            new FireSwordItem(ToolMaterials.NETHERITE, 6, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item WATCHER_CLAYMORE = registerItem( "watcher_claymore",
            new WatcherSwordItem(ToolMaterials.NETHERITE, 6, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item STORMS_EDGE = registerItem( "storms_edge",
            new StormSwordItem(ToolMaterials.NETHERITE, 3, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item SWORD_ON_A_STICK = registerItem( "sword_on_a_stick",
            new SwordItem(ToolMaterials.WOOD, 5, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item BRAMBLETHORN = registerItem( "bramblethorn",
            new BrambleSwordItem(ToolMaterials.WOOD, 6, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item TOXIC_LONGSWORD = registerItem( "toxic_longsword",
            new PlagueSwordItem(ToolMaterials.NETHERITE, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }

    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mod Items for " + SimplySwords.MOD_ID);
    }

}
