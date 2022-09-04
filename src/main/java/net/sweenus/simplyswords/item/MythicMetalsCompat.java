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
    static int mythril_modifier = 3;
    static int orichalcum_modifier = 4;
    static int osmium_modifier = 2;
    static int prometheum_modifier = 4;
    static int quadrillum_modifier = 2;
    static int runite_modifier = 3;
    static int star_platinum_modifier = 4;


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


    //MYTHRIL
    public static final Item MYTHRIL_LONGSWORD = registerItem( "mythicmetals_compat/mythril/mythril_longsword",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item MYTHRIL_TWINBLADE = registerItem( "mythicmetals_compat/mythril/mythril_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item MYTHRIL_RAPIER = registerItem( "mythicmetals_compat/mythril/mythril_rapier",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item MYTHRIL_KATANA = registerItem( "mythicmetals_compat/mythril/mythril_katana",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item MYTHRIL_SAI = registerItem( "mythicmetals_compat/mythril/mythril_sai",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item MYTHRIL_SPEAR = registerItem( "mythicmetals_compat/mythril/mythril_spear",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item MYTHRIL_GLAIVE = registerItem( "mythicmetals_compat/mythril/mythril_glaive",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item MYTHRIL_WARGLAIVE = registerItem( "mythicmetals_compat/mythril/mythril_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item MYTHRIL_CUTLASS = registerItem( "mythicmetals_compat/mythril/mythril_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item MYTHRIL_CLAYMORE = registerItem( "mythicmetals_compat/mythril/mythril_claymore",
            new SwordItem(ToolMaterials.DIAMOND, mythril_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    //ORICHALCUM
    public static final Item ORICHALCUM_LONGSWORD = registerItem( "mythicmetals_compat/orichalcum/orichalcum_longsword",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ORICHALCUM_TWINBLADE = registerItem( "mythicmetals_compat/orichalcum/orichalcum_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item ORICHALCUM_RAPIER = registerItem( "mythicmetals_compat/orichalcum/orichalcum_rapier",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ORICHALCUM_KATANA = registerItem( "mythicmetals_compat/orichalcum/orichalcum_katana",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ORICHALCUM_SAI = registerItem( "mythicmetals_compat/orichalcum/orichalcum_sai",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ORICHALCUM_SPEAR = registerItem( "mythicmetals_compat/orichalcum/orichalcum_spear",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ORICHALCUM_GLAIVE = registerItem( "mythicmetals_compat/orichalcum/orichalcum_glaive",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ORICHALCUM_WARGLAIVE = registerItem( "mythicmetals_compat/orichalcum/orichalcum_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ORICHALCUM_CUTLASS = registerItem( "mythicmetals_compat/orichalcum/orichalcum_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item ORICHALCUM_CLAYMORE = registerItem( "mythicmetals_compat/orichalcum/orichalcum_claymore",
            new SwordItem(ToolMaterials.DIAMOND, orichalcum_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    //OSMIUM
    public static final Item OSMIUM_LONGSWORD = registerItem( "mythicmetals_compat/osmium/osmium_longsword",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item OSMIUM_TWINBLADE = registerItem( "mythicmetals_compat/osmium/osmium_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item OSMIUM_RAPIER = registerItem( "mythicmetals_compat/osmium/osmium_rapier",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item OSMIUM_KATANA = registerItem( "mythicmetals_compat/osmium/osmium_katana",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item OSMIUM_SAI = registerItem( "mythicmetals_compat/osmium/osmium_sai",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item OSMIUM_SPEAR = registerItem( "mythicmetals_compat/osmium/osmium_spear",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item OSMIUM_GLAIVE = registerItem( "mythicmetals_compat/osmium/osmium_glaive",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item OSMIUM_WARGLAIVE = registerItem( "mythicmetals_compat/osmium/osmium_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item OSMIUM_CUTLASS = registerItem( "mythicmetals_compat/osmium/osmium_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item OSMIUM_CLAYMORE = registerItem( "mythicmetals_compat/osmium/osmium_claymore",
            new SwordItem(ToolMaterials.DIAMOND, osmium_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    //PROMETHEUM
    public static final Item PROMETHEUM_LONGSWORD = registerItem( "mythicmetals_compat/prometheum/prometheum_longsword",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item PROMETHEUM_TWINBLADE = registerItem( "mythicmetals_compat/prometheum/prometheum_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item PROMETHEUM_RAPIER = registerItem( "mythicmetals_compat/prometheum/prometheum_rapier",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item PROMETHEUM_KATANA = registerItem( "mythicmetals_compat/prometheum/prometheum_katana",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item PROMETHEUM_SAI = registerItem( "mythicmetals_compat/prometheum/prometheum_sai",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item PROMETHEUM_SPEAR = registerItem( "mythicmetals_compat/prometheum/prometheum_spear",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item PROMETHEUM_GLAIVE = registerItem( "mythicmetals_compat/prometheum/prometheum_glaive",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item PROMETHEUM_WARGLAIVE = registerItem( "mythicmetals_compat/prometheum/prometheum_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item PROMETHEUM_CUTLASS = registerItem( "mythicmetals_compat/prometheum/prometheum_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item PROMETHEUM_CLAYMORE = registerItem( "mythicmetals_compat/prometheum/prometheum_claymore",
            new SwordItem(ToolMaterials.DIAMOND, prometheum_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    //QUADRILLUM
    public static final Item QUADRILLUM_LONGSWORD = registerItem( "mythicmetals_compat/quadrillum/quadrillum_longsword",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item QUADRILLUM_TWINBLADE = registerItem( "mythicmetals_compat/quadrillum/quadrillum_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item QUADRILLUM_RAPIER = registerItem( "mythicmetals_compat/quadrillum/quadrillum_rapier",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item QUADRILLUM_KATANA = registerItem( "mythicmetals_compat/quadrillum/quadrillum_katana",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item QUADRILLUM_SAI = registerItem( "mythicmetals_compat/quadrillum/quadrillum_sai",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item QUADRILLUM_SPEAR = registerItem( "mythicmetals_compat/quadrillum/quadrillum_spear",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item QUADRILLUM_GLAIVE = registerItem( "mythicmetals_compat/quadrillum/quadrillum_glaive",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item QUADRILLUM_WARGLAIVE = registerItem( "mythicmetals_compat/quadrillum/quadrillum_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item QUADRILLUM_CUTLASS = registerItem( "mythicmetals_compat/quadrillum/quadrillum_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item QUADRILLUM_CLAYMORE = registerItem( "mythicmetals_compat/quadrillum/quadrillum_claymore",
            new SwordItem(ToolMaterials.DIAMOND, quadrillum_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    //RUNITE
    public static final Item RUNITE_LONGSWORD = registerItem( "mythicmetals_compat/runite/runite_longsword",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNITE_TWINBLADE = registerItem( "mythicmetals_compat/runite/runite_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item RUNITE_RAPIER = registerItem( "mythicmetals_compat/runite/runite_rapier",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNITE_KATANA = registerItem( "mythicmetals_compat/runite/runite_katana",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNITE_SAI = registerItem( "mythicmetals_compat/runite/runite_sai",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNITE_SPEAR = registerItem( "mythicmetals_compat/runite/runite_spear",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNITE_GLAIVE = registerItem( "mythicmetals_compat/runite/runite_glaive",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNITE_WARGLAIVE = registerItem( "mythicmetals_compat/runite/runite_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNITE_CUTLASS = registerItem( "mythicmetals_compat/runite/runite_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item RUNITE_CLAYMORE = registerItem( "mythicmetals_compat/runite/runite_claymore",
            new SwordItem(ToolMaterials.DIAMOND, runite_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    //STAR_PLATINUM
    public static final Item STAR_PLATINUM_LONGSWORD = registerItem( "mythicmetals_compat/star_platinum/star_platinum_longsword",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier, -2.4f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item STAR_PLATINUM_TWINBLADE = registerItem( "mythicmetals_compat/star_platinum/star_platinum_twinblade",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier, -1.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));
    public static final Item STAR_PLATINUM_RAPIER = registerItem( "mythicmetals_compat/star_platinum/star_platinum_rapier",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier - rapier_modifier, -1.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item STAR_PLATINUM_KATANA = registerItem( "mythicmetals_compat/star_platinum/star_platinum_katana",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item STAR_PLATINUM_SAI = registerItem( "mythicmetals_compat/star_platinum/star_platinum_sai",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier - sai_modifier, -1.1f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item STAR_PLATINUM_SPEAR = registerItem( "mythicmetals_compat/star_platinum/star_platinum_spear",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier, -2.7f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item STAR_PLATINUM_GLAIVE = registerItem( "mythicmetals_compat/star_platinum/star_platinum_glaive",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier, -2.6f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item STAR_PLATINUM_WARGLAIVE = registerItem( "mythicmetals_compat/star_platinum/star_platinum_warglaive",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier, -2.2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item STAR_PLATINUM_CUTLASS = registerItem( "mythicmetals_compat/star_platinum/star_platinum_cutlass",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier, -2f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));

    public static final Item STAR_PLATINUM_CLAYMORE = registerItem( "mythicmetals_compat/star_platinum/star_platinum_claymore",
            new SwordItem(ToolMaterials.DIAMOND, star_platinum_modifier + claymore_modifier, -2.8f,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS)));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mythic Metals compat Items for " + SimplySwords.MOD_ID);
    }

}
