package net.sweenus.simplyswords.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.custom.*;

public class MythicMetalsCompat {

    //Compat for Mythic Metals
    static int rapier_modifier = 1;
    static int sai_modifier = 3;
    static int claymore_modifier = 2;
    static int adamantite_modifier = 5;
    static int aquarium_modifier = 2;
    static int banglum_modifier = 2;
    static int carmot_modifier = 3;
    static int kyber_modifier = 2;


    //ADAMANTITE
    public static final Item ADAMANTITE_LONGSWORD = registerItem( "mythicmetals_compat/adamantite/adamantite_longsword",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_TWINBLADE = registerItem( "mythicmetals_compat/adamantite/adamantite_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item ADAMANTITE_RAPIER = registerItem( "mythicmetals_compat/adamantite/adamantite_rapier",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_KATANA = registerItem( "mythicmetals_compat/adamantite/adamantite_katana",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_SAI = registerItem( "mythicmetals_compat/adamantite/adamantite_sai",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_SPEAR = registerItem( "mythicmetals_compat/adamantite/adamantite_spear",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_GLAIVE = registerItem( "mythicmetals_compat/adamantite/adamantite_glaive",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_WARGLAIVE = registerItem( "mythicmetals_compat/adamantite/adamantite_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_CUTLASS = registerItem( "mythicmetals_compat/adamantite/adamantite_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ADAMANTITE_CLAYMORE = registerItem( "mythicmetals_compat/adamantite/adamantite_claymore",
            new SwordItem(ToolMaterials.DIAMOND, adamantite_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    //AQUARIUM
    public static final Item AQUARIUM_LONGSWORD = registerItem( "mythicmetals_compat/aquarium/aquarium_longsword",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item AQUARIUM_TWINBLADE = registerItem( "mythicmetals_compat/aquarium/aquarium_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item AQUARIUM_RAPIER = registerItem( "mythicmetals_compat/aquarium/aquarium_rapier",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item AQUARIUM_KATANA = registerItem( "mythicmetals_compat/aquarium/aquarium_katana",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item AQUARIUM_SAI = registerItem( "mythicmetals_compat/aquarium/aquarium_sai",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item AQUARIUM_SPEAR = registerItem( "mythicmetals_compat/aquarium/aquarium_spear",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item AQUARIUM_GLAIVE = registerItem( "mythicmetals_compat/aquarium/aquarium_glaive",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item AQUARIUM_WARGLAIVE = registerItem( "mythicmetals_compat/aquarium/aquarium_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item AQUARIUM_CUTLASS = registerItem( "mythicmetals_compat/aquarium/aquarium_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item AQUARIUM_CLAYMORE = registerItem( "mythicmetals_compat/aquarium/aquarium_claymore",
            new SwordItem(ToolMaterials.DIAMOND, aquarium_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    //BANGLUM
    public static final Item BANGLUM_LONGSWORD = registerItem( "mythicmetals_compat/banglum/banglum_longsword",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item BANGLUM_TWINBLADE = registerItem( "mythicmetals_compat/banglum/banglum_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item BANGLUM_RAPIER = registerItem( "mythicmetals_compat/banglum/banglum_rapier",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item BANGLUM_KATANA = registerItem( "mythicmetals_compat/banglum/banglum_katana",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item BANGLUM_SAI = registerItem( "mythicmetals_compat/banglum/banglum_sai",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item BANGLUM_SPEAR = registerItem( "mythicmetals_compat/banglum/banglum_spear",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item BANGLUM_GLAIVE = registerItem( "mythicmetals_compat/banglum/banglum_glaive",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item BANGLUM_WARGLAIVE = registerItem( "mythicmetals_compat/banglum/banglum_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item BANGLUM_CUTLASS = registerItem( "mythicmetals_compat/banglum/banglum_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item BANGLUM_CLAYMORE = registerItem( "mythicmetals_compat/banglum/banglum_claymore",
            new SwordItem(ToolMaterials.DIAMOND, banglum_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    //CARMOT
    public static final Item CARMOT_LONGSWORD = registerItem( "mythicmetals_compat/carmot/carmot_longsword",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item CARMOT_TWINBLADE = registerItem( "mythicmetals_compat/carmot/carmot_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item CARMOT_RAPIER = registerItem( "mythicmetals_compat/carmot/carmot_rapier",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item CARMOT_KATANA = registerItem( "mythicmetals_compat/carmot/carmot_katana",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item CARMOT_SAI = registerItem( "mythicmetals_compat/carmot/carmot_sai",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item CARMOT_SPEAR = registerItem( "mythicmetals_compat/carmot/carmot_spear",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item CARMOT_GLAIVE = registerItem( "mythicmetals_compat/carmot/carmot_glaive",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item CARMOT_WARGLAIVE = registerItem( "mythicmetals_compat/carmot/carmot_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item CARMOT_CUTLASS = registerItem( "mythicmetals_compat/carmot/carmot_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item CARMOT_CLAYMORE = registerItem( "mythicmetals_compat/carmot/carmot_claymore",
            new SwordItem(ToolMaterials.DIAMOND, carmot_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    //KYBER
    public static final Item KYBER_LONGSWORD = registerItem( "mythicmetals_compat/kyber/kyber_longsword",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item KYBER_TWINBLADE = registerItem( "mythicmetals_compat/kyber/kyber_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item KYBER_RAPIER = registerItem( "mythicmetals_compat/kyber/kyber_rapier",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item KYBER_KATANA = registerItem( "mythicmetals_compat/kyber/kyber_katana",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item KYBER_SAI = registerItem( "mythicmetals_compat/kyber/kyber_sai",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item KYBER_SPEAR = registerItem( "mythicmetals_compat/kyber/kyber_spear",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item KYBER_GLAIVE = registerItem( "mythicmetals_compat/kyber/kyber_glaive",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item KYBER_WARGLAIVE = registerItem( "mythicmetals_compat/kyber/kyber_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item KYBER_CUTLASS = registerItem( "mythicmetals_compat/kyber/kyber_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item KYBER_CLAYMORE = registerItem( "mythicmetals_compat/kyber/kyber_claymore",
            new SwordItem(ToolMaterials.DIAMOND, kyber_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mythic Metals compat Items for " + SimplySwords.MOD_ID);
    }

}
