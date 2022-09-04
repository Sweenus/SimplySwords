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

public class MythicMetalsCompat {

    //Compat for Mythic Metals


    //ADAMANTITE
    public static final Item ADAMANTITE_LONGSWORD = registerItem( "mythicmetals_compat/adamantite_longsword",
            new SwordItem(ToolMaterials.IRON, 3, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_TWINBLADE = registerItem( "mythicmetals_compat/adamantite_twinblade",
            new SwordItem(ToolMaterials.IRON, 3, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item ADAMANTITE_RAPIER = registerItem( "mythicmetals_compat/adamantite_rapier",
            new SwordItem(ToolMaterials.IRON, 2, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_KATANA = registerItem( "mythicmetals_compat/adamantite_katana",
            new SwordItem(ToolMaterials.IRON, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_SAI = registerItem( "mythicmetals_compat/adamantite_sai",
            new SwordItem(ToolMaterials.IRON, 0, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_SPEAR = registerItem( "mythicmetals_compat/adamantite_spear",
            new SwordItem(ToolMaterials.IRON, 3, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_GLAIVE = registerItem( "mythicmetals_compat/adamantite_glaive",
            new SwordItem(ToolMaterials.IRON, 3, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_WARGLAIVE = registerItem( "mythicmetals_compat/adamantite_warglaive",
            new SwordItem(ToolMaterials.IRON, 3, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_CUTLASS = registerItem( "mythicmetals_compat/adamantite_cutlass",
            new SwordItem(ToolMaterials.IRON, 3, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item ADAMANTITE_CLAYMORE = registerItem( "mythicmetals_compat/adamantite_claymore",
            new SwordItem(ToolMaterials.IRON, 5, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mythic Metals compat Items for " + SimplySwords.MOD_ID);
    }

}
