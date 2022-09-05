package net.sweenus.simplyswords.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.custom.*;

public class ModItems {
   // public static final Item ITEMNAMEHERE = registerItem( "item_name",
        //new Item(new FabricItemSettings().group(ItemGroup.MISC)));

    //IRON
    public static final Item IRON_LONGSWORD = registerItem( "iron_longsword",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    3,
                    -2.4f,
                    "minecraft:iron_ingot"));

    public static final Item IRON_TWINBLADE = registerItem( "iron_twinblade",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    3,
                    -1.7f,
                    "minecraft:iron_ingot"));
    public static final Item IRON_RAPIER = registerItem( "iron_rapier",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    2,
                    -1.6f,
                    "minecraft:iron_ingot"));

    public static final Item IRON_KATANA = registerItem( "iron_katana",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    3,
                    -2f,
                    "minecraft:iron_ingot"));

    public static final Item IRON_SAI = registerItem( "iron_sai",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    0,
                    -1.1f,
                    "minecraft:iron_ingot"));

    public static final Item IRON_SPEAR = registerItem( "iron_spear",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    3,
                    -2.7f,
                    "minecraft:iron_ingot"));

    public static final Item IRON_GLAIVE = registerItem( "iron_glaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    3,
                    -2.6f,
                    "minecraft:iron_ingot"));

    public static final Item IRON_WARGLAIVE = registerItem( "iron_warglaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    3,
                    -2.2f,
                    "minecraft:iron_ingot"));

    public static final Item IRON_CUTLASS = registerItem( "iron_cutlass",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    3,
                    -2f,
                    "minecraft:iron_ingot"));
    public static final Item IRON_CLAYMORE = registerItem( "iron_claymore",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    5,
                    -2.8f,
                    "minecraft:iron_ingot"));

    public static final Item IRON_GREATHAMMER = registerItem( "iron_greathammer",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    7,
                    -3.3f,
                    "minecraft:iron_ingot"));

    public static final Item IRON_GREATAXE = registerItem( "iron_greataxe",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    6,
                    -3.2f,
                    "minecraft:iron_ingot"));

    //GOLD
    public static final Item GOLD_LONGSWORD = registerItem( "gold_longsword",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    2,
                    -2.4f,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_TWINBLADE = registerItem( "gold_twinblade",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    2,
                    -1.7f,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_RAPIER = registerItem( "gold_rapier",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    1,
                    -1.6f,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_KATANA = registerItem( "gold_katana",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    2,
                    -2f,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_SAI = registerItem( "gold_sai",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    0,
                    -1.1f,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_SPEAR = registerItem( "gold_spear",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    2,
                    -2.7f,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_GLAIVE = registerItem( "gold_glaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    2,
                    -2.6f,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_WARGLAIVE = registerItem( "gold_warglaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    2,
                    -2.2f,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_CUTLASS = registerItem( "gold_cutlass",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    2,
                    -2f,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_CLAYMORE = registerItem( "gold_claymore",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    4,
                    -2.8f,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_GREATHAMMER = registerItem( "gold_greathammer",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    6,
                    -3.3f,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_GREATAXE = registerItem( "gold_greataxe",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    5,
                    -3.2f,
                    "minecraft:gold_ingot"));

    //DIAMOND
    public static final Item DIAMOND_LONGSWORD = registerItem( "diamond_longsword",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    3,
                    -2.4f,
                    "minecraft:diamond"));

    public static final Item DIAMOND_TWINBLADE = registerItem( "diamond_twinblade",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    3,
                    -1.7f,
                    "minecraft:diamond"));
    public static final Item DIAMOND_RAPIER = registerItem( "diamond_rapier",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    2,
                    -1.6f,
                    "minecraft:diamond"));
    public static final Item DIAMOND_KATANA = registerItem( "diamond_katana",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    3,
                    -2f,
                    "minecraft:diamond"));
    public static final Item DIAMOND_SAI = registerItem( "diamond_sai",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    0,
                    -1.1f,
                    "minecraft:diamond"));

    public static final Item DIAMOND_SPEAR = registerItem( "diamond_spear",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    3,
                    -2.7f,
                    "minecraft:diamond"));

    public static final Item DIAMOND_GLAIVE = registerItem( "diamond_glaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    3,
                    -2.6f,
                    "minecraft:diamond"));

    public static final Item DIAMOND_WARGLAIVE = registerItem( "diamond_warglaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    3,
                    -2.2f,
                    "minecraft:diamond"));
    public static final Item DIAMOND_CUTLASS = registerItem( "diamond_cutlass",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    3,
                    -2f,
                    "minecraft:diamond"));
    public static final Item DIAMOND_CLAYMORE = registerItem( "diamond_claymore",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    5,
                    -2.8f,
                    "minecraft:diamond"));

    public static final Item DIAMOND_GREATHAMMER = registerItem( "diamond_greathammer",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    7,
                    -3.3f,
                    "minecraft:diamond"));

    public static final Item DIAMOND_GREATAXE = registerItem( "diamond_greataxe",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    6,
                    -3.2f,
                    "minecraft:diamond"));
    //NETHERITE

    public static final Item NETHERITE_LONGSWORD = registerItem( "netherite_longsword",
            new SimplySwordsSwordItem(ToolMaterials.NETHERITE,
                    3,
                    -2.4f,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_TWINBLADE = registerItem( "netherite_twinblade",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    3,
                    -1.7f,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_RAPIER = registerItem( "netherite_rapier",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    2,
                    -1.6f,
                    "minecraft:netherite_ingot"));
    public static final Item NETHERITE_KATANA = registerItem( "netherite_katana",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    3,
                    -2f,
                    "minecraft:netherite_ingot"));
    public static final Item NETHERITE_SAI = registerItem( "netherite_sai",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    0,
                    -1.1f,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_SPEAR = registerItem( "netherite_spear",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    3,
                    -2.7f,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_GLAIVE = registerItem( "netherite_glaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    3,
                    -2.6f,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_WARGLAIVE = registerItem( "netherite_warglaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    3,
                    -2.2f,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_CUTLASS = registerItem( "netherite_cutlass",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    3,
                    -2f,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_CLAYMORE = registerItem( "netherite_claymore",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    6,
                    -2.8f,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_GREATHAMMER = registerItem( "netherite_greathammer",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    8,
                    -3.3f,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_GREATAXE = registerItem( "netherite_greataxe",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    7,
                    -3.2f,
                    "minecraft:netherite_ingot"));

    //RUNIC
    public static final Item RUNIC_LONGSWORD = registerItem( "runic_longsword",
            new PoisonSwordItem(ToolMaterials.NETHERITE, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_TWINBLADE = registerItem( "runic_twinblade",
            new WildfireSwordItem(ToolMaterials.NETHERITE, 3, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_RAPIER = registerItem( "runic_rapier",
            new SpeedSwordItem(ToolMaterials.NETHERITE, 2, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_KATANA = registerItem( "runic_katana",
            new WildfireSwordItem(ToolMaterials.NETHERITE, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_SAI = registerItem( "runic_sai",
            new SlownessSwordItem(ToolMaterials.NETHERITE, 0, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNIC_SPEAR = registerItem( "runic_spear",
            new FreezeSwordItem(ToolMaterials.NETHERITE, 3, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item RUNIC_GLAIVE = registerItem( "runic_glaive",
            new WildfireSwordItem(ToolMaterials.NETHERITE, 3, -2.6f,
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

    public static final Item STORMBRINGER = registerItem( "stormbringer",
            new StormSwordItem(ToolMaterials.NETHERITE, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item SWORD_ON_A_STICK = registerItem( "sword_on_a_stick",
            new SwordItem(ToolMaterials.WOOD, 5, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item BRAMBLETHORN = registerItem( "bramblethorn",
            new BrambleSwordItem(ToolMaterials.WOOD, 6, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item WATCHING_WARGLAIVE = registerItem( "watching_warglaive",
            new WatcherSwordItem(ToolMaterials.NETHERITE, 3, -2.2f,
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
