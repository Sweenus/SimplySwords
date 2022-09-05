package net.sweenus.simplyswords.item;

import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;

public class MythicMetalsCompat {

    //Compat for Mythic Metals
    static int rapier_modifier = 1;
    static int sai_modifier = 3;
    static int claymore_modifier = 2;
    static int adamantite_modifier = 3;
    static int aquarium_modifier = 3;
    static int banglum_modifier = 3;
    static int carmot_modifier = 3;
    static int kyber_modifier = 3;
    static int mythril_modifier = 3;
    static int orichalcum_modifier = 3;
    static int osmium_modifier = 3;
    static int prometheum_modifier = 3;
    static int quadrillum_modifier = 3;
    static int runite_modifier = 3;
    static int star_platinum_modifier = 3;
    static int bronze_modifier = 3;


    //ADAMANTITE
    public static final Item ADAMANTITE_LONGSWORD = registerItem( "mythicmetals_compat/adamantite/adamantite_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier, -2.4f,
                    "mythicmetals:adamantite_ingot"));

    public static final Item ADAMANTITE_TWINBLADE = registerItem( "mythicmetals_compat/adamantite/adamantite_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier, -1.7f,
                    "mythicmetals:adamantite_ingot"));

    public static final Item ADAMANTITE_RAPIER = registerItem( "mythicmetals_compat/adamantite/adamantite_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:adamantite_ingot"));

    public static final Item ADAMANTITE_KATANA = registerItem( "mythicmetals_compat/adamantite/adamantite_katana",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier, -2f,
                    "mythicmetals:adamantite_ingot"));

    public static final Item ADAMANTITE_SAI = registerItem( "mythicmetals_compat/adamantite/adamantite_sai",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier - sai_modifier, -1.1f,
                    "mythicmetals:adamantite_ingot"));

    public static final Item ADAMANTITE_SPEAR = registerItem( "mythicmetals_compat/adamantite/adamantite_spear",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier, -2.7f,
                    "mythicmetals:adamantite_ingot"));

    public static final Item ADAMANTITE_GLAIVE = registerItem( "mythicmetals_compat/adamantite/adamantite_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier, -2.6f,
                    "mythicmetals:adamantite_ingot"));

    public static final Item ADAMANTITE_WARGLAIVE = registerItem( "mythicmetals_compat/adamantite/adamantite_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier, -2.2f,
                    "mythicmetals:adamantite_ingot"));

    public static final Item ADAMANTITE_CUTLASS = registerItem( "mythicmetals_compat/adamantite/adamantite_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier, -2f,
                    "mythicmetals:adamantite_ingot"));

    public static final Item ADAMANTITE_CLAYMORE = registerItem( "mythicmetals_compat/adamantite/adamantite_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.ADAMANTITE, adamantite_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:adamantite_ingot"));


    //AQUARIUM
    public static final Item AQUARIUM_LONGSWORD = registerItem( "mythicmetals_compat/aquarium/aquarium_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier, -2.4f,
                    "mythicmetals:aquarium_ingot"));

    public static final Item AQUARIUM_TWINBLADE = registerItem( "mythicmetals_compat/aquarium/aquarium_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier, -1.7f,
                    "mythicmetals:aquarium_ingot"));
    public static final Item AQUARIUM_RAPIER = registerItem( "mythicmetals_compat/aquarium/aquarium_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:aquarium_ingot"));

    public static final Item AQUARIUM_KATANA = registerItem( "mythicmetals_compat/aquarium/aquarium_katana",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier, -2f,
                    "mythicmetals:aquarium_ingot"));

    public static final Item AQUARIUM_SAI = registerItem( "mythicmetals_compat/aquarium/aquarium_sai",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier - sai_modifier, -1.1f,
                    "mythicmetals:aquarium_ingot"));

    public static final Item AQUARIUM_SPEAR = registerItem( "mythicmetals_compat/aquarium/aquarium_spear",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier, -2.7f,
                    "mythicmetals:aquarium_ingot"));

    public static final Item AQUARIUM_GLAIVE = registerItem( "mythicmetals_compat/aquarium/aquarium_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier, -2.6f,
                    "mythicmetals:aquarium_ingot"));

    public static final Item AQUARIUM_WARGLAIVE = registerItem( "mythicmetals_compat/aquarium/aquarium_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier, -2.2f,
                    "mythicmetals:aquarium_ingot"));

    public static final Item AQUARIUM_CUTLASS = registerItem( "mythicmetals_compat/aquarium/aquarium_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier, -2f,
                    "mythicmetals:aquarium_ingot"));

    public static final Item AQUARIUM_CLAYMORE = registerItem( "mythicmetals_compat/aquarium/aquarium_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.AQUARIUM, aquarium_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:aquarium_ingot"));


    //BANGLUM
    public static final Item BANGLUM_LONGSWORD = registerItem( "mythicmetals_compat/banglum/banglum_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier, -2.4f,
                    "mythicmetals:banglum_ingot"));

    public static final Item BANGLUM_TWINBLADE = registerItem( "mythicmetals_compat/banglum/banglum_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier, -1.7f,
                    "mythicmetals:banglum_ingot"));

    public static final Item BANGLUM_RAPIER = registerItem( "mythicmetals_compat/banglum/banglum_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:banglum_ingot"));

    public static final Item BANGLUM_KATANA = registerItem( "mythicmetals_compat/banglum/banglum_katana",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier, -2f,
                    "mythicmetals:banglum_ingot"));

    public static final Item BANGLUM_SAI = registerItem( "mythicmetals_compat/banglum/banglum_sai",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier - sai_modifier, -1.1f,
                    "mythicmetals:banglum_ingot"));

    public static final Item BANGLUM_SPEAR = registerItem( "mythicmetals_compat/banglum/banglum_spear",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier, -2.7f,
                    "mythicmetals:banglum_ingot"));

    public static final Item BANGLUM_GLAIVE = registerItem( "mythicmetals_compat/banglum/banglum_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier, -2.6f,
                    "mythicmetals:banglum_ingot"));

    public static final Item BANGLUM_WARGLAIVE = registerItem( "mythicmetals_compat/banglum/banglum_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier, -2.2f,
                    "mythicmetals:banglum_ingot"));

    public static final Item BANGLUM_CUTLASS = registerItem( "mythicmetals_compat/banglum/banglum_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier, -2f,
                    "mythicmetals:banglum_ingot"));

    public static final Item BANGLUM_CLAYMORE = registerItem( "mythicmetals_compat/banglum/banglum_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.BANGLUM, banglum_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:banglum_ingot"));


    //CARMOT
    public static final Item CARMOT_LONGSWORD = registerItem( "mythicmetals_compat/carmot/carmot_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier, -2.4f,
                    "mythicmetals:carmot_ingot"));

    public static final Item CARMOT_TWINBLADE = registerItem( "mythicmetals_compat/carmot/carmot_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier, -1.7f,
                    "mythicmetals:carmot_ingot"));

    public static final Item CARMOT_RAPIER = registerItem( "mythicmetals_compat/carmot/carmot_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:carmot_ingot"));

    public static final Item CARMOT_KATANA = registerItem( "mythicmetals_compat/carmot/carmot_katana",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier, -2f,
                    "mythicmetals:carmot_ingot"));

    public static final Item CARMOT_SAI = registerItem( "mythicmetals_compat/carmot/carmot_sai",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier - sai_modifier, -1.1f,
                    "mythicmetals:carmot_ingot"));

    public static final Item CARMOT_SPEAR = registerItem( "mythicmetals_compat/carmot/carmot_spear",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier, -2.7f,
                    "mythicmetals:carmot_ingot"));

    public static final Item CARMOT_GLAIVE = registerItem( "mythicmetals_compat/carmot/carmot_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier, -2.6f,
                    "mythicmetals:carmot_ingot"));

    public static final Item CARMOT_WARGLAIVE = registerItem( "mythicmetals_compat/carmot/carmot_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier, -2.2f,
                    "mythicmetals:carmot_ingot"));

    public static final Item CARMOT_CUTLASS = registerItem( "mythicmetals_compat/carmot/carmot_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier, -2f,
                    "mythicmetals:carmot_ingot"));

    public static final Item CARMOT_CLAYMORE = registerItem( "mythicmetals_compat/carmot/carmot_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.CARMOT, carmot_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:carmot_ingot"));

    //KYBER
    public static final Item KYBER_LONGSWORD = registerItem( "mythicmetals_compat/kyber/kyber_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier, -2.4f,
                    "mythicmetals:kyber_ingot"));

    public static final Item KYBER_TWINBLADE = registerItem( "mythicmetals_compat/kyber/kyber_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier, -1.7f,
                    "mythicmetals:kyber_ingot"));

    public static final Item KYBER_RAPIER = registerItem( "mythicmetals_compat/kyber/kyber_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:kyber_ingot"));

    public static final Item KYBER_KATANA = registerItem( "mythicmetals_compat/kyber/kyber_katana",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier, -2f,
                    "mythicmetals:kyber_ingot"));

    public static final Item KYBER_SAI = registerItem( "mythicmetals_compat/kyber/kyber_sai",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier - sai_modifier, -1.1f,
                    "mythicmetals:kyber_ingot"));

    public static final Item KYBER_SPEAR = registerItem( "mythicmetals_compat/kyber/kyber_spear",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier, -2.7f,
                    "mythicmetals:kyber_ingot"));

    public static final Item KYBER_GLAIVE = registerItem( "mythicmetals_compat/kyber/kyber_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier, -2.6f,
                    "mythicmetals:kyber_ingot"));

    public static final Item KYBER_WARGLAIVE = registerItem( "mythicmetals_compat/kyber/kyber_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier, -2.2f,
                    "mythicmetals:kyber_ingot"));

    public static final Item KYBER_CUTLASS = registerItem( "mythicmetals_compat/kyber/kyber_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier, -2f,
                    "mythicmetals:kyber_ingot"));

    public static final Item KYBER_CLAYMORE = registerItem( "mythicmetals_compat/kyber/kyber_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.KYBER, kyber_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:kyber_ingot"));


    //MYTHRIL
    public static final Item MYTHRIL_LONGSWORD = registerItem( "mythicmetals_compat/mythril/mythril_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier, -2.4f,
                    "mythicmetals:mythril_ingot"));

    public static final Item MYTHRIL_TWINBLADE = registerItem( "mythicmetals_compat/mythril/mythril_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier, -1.7f,
                    "mythicmetals:mythril_ingot"));

    public static final Item MYTHRIL_RAPIER = registerItem( "mythicmetals_compat/mythril/mythril_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:mythril_ingot"));

    public static final Item MYTHRIL_KATANA = registerItem( "mythicmetals_compat/mythril/mythril_katana",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier, -2f,
                    "mythicmetals:mythril_ingot"));

    public static final Item MYTHRIL_SAI = registerItem( "mythicmetals_compat/mythril/mythril_sai",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier - sai_modifier, -1.1f,
                    "mythicmetals:mythril_ingot"));

    public static final Item MYTHRIL_SPEAR = registerItem( "mythicmetals_compat/mythril/mythril_spear",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier, -2.7f,
                    "mythicmetals:mythril_ingot"));

    public static final Item MYTHRIL_GLAIVE = registerItem( "mythicmetals_compat/mythril/mythril_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier, -2.6f,
                    "mythicmetals:mythril_ingot"));

    public static final Item MYTHRIL_WARGLAIVE = registerItem( "mythicmetals_compat/mythril/mythril_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier, -2.2f,
                    "mythicmetals:mythril_ingot"));

    public static final Item MYTHRIL_CUTLASS = registerItem( "mythicmetals_compat/mythril/mythril_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier, -2f,
                    "mythicmetals:mythril_ingot"));

    public static final Item MYTHRIL_CLAYMORE = registerItem( "mythicmetals_compat/mythril/mythril_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.MYTHRIL, mythril_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:mythril_ingot"));


    //ORICHALCUM
    public static final Item ORICHALCUM_LONGSWORD = registerItem( "mythicmetals_compat/orichalcum/orichalcum_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier, -2.4f,
                    "mythicmetals:orichalcum_ingot"));

    public static final Item ORICHALCUM_TWINBLADE = registerItem( "mythicmetals_compat/orichalcum/orichalcum_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier, -1.7f,
                    "mythicmetals:orichalcum_ingot"));

    public static final Item ORICHALCUM_RAPIER = registerItem( "mythicmetals_compat/orichalcum/orichalcum_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:orichalcum_ingot"));

    public static final Item ORICHALCUM_KATANA = registerItem( "mythicmetals_compat/orichalcum/orichalcum_katana",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier, -2f,
                    "mythicmetals:orichalcum_ingot"));

    public static final Item ORICHALCUM_SAI = registerItem( "mythicmetals_compat/orichalcum/orichalcum_sai",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier - sai_modifier, -1.1f,
                    "mythicmetals:orichalcum_ingot"));

    public static final Item ORICHALCUM_SPEAR = registerItem( "mythicmetals_compat/orichalcum/orichalcum_spear",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier, -2.7f,
                    "mythicmetals:orichalcum_ingot"));

    public static final Item ORICHALCUM_GLAIVE = registerItem( "mythicmetals_compat/orichalcum/orichalcum_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier, -2.6f,
                    "mythicmetals:orichalcum_ingot"));

    public static final Item ORICHALCUM_WARGLAIVE = registerItem( "mythicmetals_compat/orichalcum/orichalcum_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier, -2.2f,
                    "mythicmetals:orichalcum_ingot"));

    public static final Item ORICHALCUM_CUTLASS = registerItem( "mythicmetals_compat/orichalcum/orichalcum_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier, -2f,
                    "mythicmetals:orichalcum_ingot"));

    public static final Item ORICHALCUM_CLAYMORE = registerItem( "mythicmetals_compat/orichalcum/orichalcum_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.ORICHALCUM, orichalcum_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:orichalcum_ingot"));


    //OSMIUM
    public static final Item OSMIUM_LONGSWORD = registerItem( "mythicmetals_compat/osmium/osmium_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier, -2.4f,
                    "mythicmetals:osmium_ingot"));

    public static final Item OSMIUM_TWINBLADE = registerItem( "mythicmetals_compat/osmium/osmium_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier, -1.7f,
                    "mythicmetals:osmium_ingot"));

    public static final Item OSMIUM_RAPIER = registerItem( "mythicmetals_compat/osmium/osmium_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:osmium_ingot"));

    public static final Item OSMIUM_KATANA = registerItem( "mythicmetals_compat/osmium/osmium_katana",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier, -2f,
                    "mythicmetals:osmium_ingot"));

    public static final Item OSMIUM_SAI = registerItem( "mythicmetals_compat/osmium/osmium_sai",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier - sai_modifier, -1.1f,
                    "mythicmetals:osmium_ingot"));

    public static final Item OSMIUM_SPEAR = registerItem( "mythicmetals_compat/osmium/osmium_spear",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier, -2.7f,
                    "mythicmetals:osmium_ingot"));

    public static final Item OSMIUM_GLAIVE = registerItem( "mythicmetals_compat/osmium/osmium_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier, -2.6f,
                    "mythicmetals:osmium_ingot"));

    public static final Item OSMIUM_WARGLAIVE = registerItem( "mythicmetals_compat/osmium/osmium_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier, -2.2f,
                    "mythicmetals:osmium_ingot"));

    public static final Item OSMIUM_CUTLASS = registerItem( "mythicmetals_compat/osmium/osmium_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier, -2f,
                    "mythicmetals:osmium_ingot"));

    public static final Item OSMIUM_CLAYMORE = registerItem( "mythicmetals_compat/osmium/osmium_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.OSMIUM, osmium_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:osmium_ingot"));


    //PROMETHEUM
    public static final Item PROMETHEUM_LONGSWORD = registerItem( "mythicmetals_compat/prometheum/prometheum_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier, -2.4f,
                    "mythicmetals:prometheum_ingot"));

    public static final Item PROMETHEUM_TWINBLADE = registerItem( "mythicmetals_compat/prometheum/prometheum_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier, -1.7f,
                    "mythicmetals:prometheum_ingot"));

    public static final Item PROMETHEUM_RAPIER = registerItem( "mythicmetals_compat/prometheum/prometheum_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:prometheum_ingot"));

    public static final Item PROMETHEUM_KATANA = registerItem( "mythicmetals_compat/prometheum/prometheum_katana",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier, -2f,
                    "mythicmetals:prometheum_ingot"));

    public static final Item PROMETHEUM_SAI = registerItem( "mythicmetals_compat/prometheum/prometheum_sai",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier - sai_modifier, -1.1f,
                    "mythicmetals:prometheum_ingot"));

    public static final Item PROMETHEUM_SPEAR = registerItem( "mythicmetals_compat/prometheum/prometheum_spear",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier, -2.7f,
                    "mythicmetals:prometheum_ingot"));

    public static final Item PROMETHEUM_GLAIVE = registerItem( "mythicmetals_compat/prometheum/prometheum_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier, -2.6f,
                    "mythicmetals:prometheum_ingot"));

    public static final Item PROMETHEUM_WARGLAIVE = registerItem( "mythicmetals_compat/prometheum/prometheum_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier, -2.2f,
                    "mythicmetals:prometheum_ingot"));

    public static final Item PROMETHEUM_CUTLASS = registerItem( "mythicmetals_compat/prometheum/prometheum_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier, -2f,
                    "mythicmetals:prometheum_ingot"));

    public static final Item PROMETHEUM_CLAYMORE = registerItem( "mythicmetals_compat/prometheum/prometheum_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.PROMETHEUM, prometheum_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:prometheum_ingot"));


    //QUADRILLUM
    public static final Item QUADRILLUM_LONGSWORD = registerItem( "mythicmetals_compat/quadrillum/quadrillum_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier, -2.4f,
                    "mythicmetals:quadrillum_ingot"));

    public static final Item QUADRILLUM_TWINBLADE = registerItem( "mythicmetals_compat/quadrillum/quadrillum_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier, -1.7f,
                    "mythicmetals:quadrillum_ingot"));

    public static final Item QUADRILLUM_RAPIER = registerItem( "mythicmetals_compat/quadrillum/quadrillum_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:quadrillum_ingot"));

    public static final Item QUADRILLUM_KATANA = registerItem( "mythicmetals_compat/quadrillum/quadrillum_katana",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier, -2f,
                    "mythicmetals:quadrillum_ingot"));

    public static final Item QUADRILLUM_SAI = registerItem( "mythicmetals_compat/quadrillum/quadrillum_sai",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier - sai_modifier, -1.1f,
                    "mythicmetals:quadrillum_ingot"));

    public static final Item QUADRILLUM_SPEAR = registerItem( "mythicmetals_compat/quadrillum/quadrillum_spear",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier, -2.7f,
                    "mythicmetals:quadrillum_ingot"));

    public static final Item QUADRILLUM_GLAIVE = registerItem( "mythicmetals_compat/quadrillum/quadrillum_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier, -2.6f,
                    "mythicmetals:quadrillum_ingot"));

    public static final Item QUADRILLUM_WARGLAIVE = registerItem( "mythicmetals_compat/quadrillum/quadrillum_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier, -2.2f,
                    "mythicmetals:quadrillum_ingot"));

    public static final Item QUADRILLUM_CUTLASS = registerItem( "mythicmetals_compat/quadrillum/quadrillum_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier, -2f,
                    "mythicmetals:quadrillum_ingot"));

    public static final Item QUADRILLUM_CLAYMORE = registerItem( "mythicmetals_compat/quadrillum/quadrillum_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.QUADRILLUM, quadrillum_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:quadrillum_ingot"));


    //RUNITE
    public static final Item RUNITE_LONGSWORD = registerItem( "mythicmetals_compat/runite/runite_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier, -2.4f,
                    "mythicmetals:runite_ingot"));

    public static final Item RUNITE_TWINBLADE = registerItem( "mythicmetals_compat/runite/runite_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier, -1.7f,
                    "mythicmetals:runite_ingot"));

    public static final Item RUNITE_RAPIER = registerItem( "mythicmetals_compat/runite/runite_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:runite_ingot"));

    public static final Item RUNITE_KATANA = registerItem( "mythicmetals_compat/runite/runite_katana",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier, -2f,
                    "mythicmetals:runite_ingot"));

    public static final Item RUNITE_SAI = registerItem( "mythicmetals_compat/runite/runite_sai",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier - sai_modifier, -1.1f,
                    "mythicmetals:runite_ingot"));

    public static final Item RUNITE_SPEAR = registerItem( "mythicmetals_compat/runite/runite_spear",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier, -2.7f,
                    "mythicmetals:runite_ingot"));

    public static final Item RUNITE_GLAIVE = registerItem( "mythicmetals_compat/runite/runite_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier, -2.6f,
                    "mythicmetals:runite_ingot"));

    public static final Item RUNITE_WARGLAIVE = registerItem( "mythicmetals_compat/runite/runite_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier, -2.2f,
                    "mythicmetals:runite_ingot"));

    public static final Item RUNITE_CUTLASS = registerItem( "mythicmetals_compat/runite/runite_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier, -2f,
                    "mythicmetals:runite_ingot"));

    public static final Item RUNITE_CLAYMORE = registerItem( "mythicmetals_compat/runite/runite_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.RUNITE, runite_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:runite_ingot"));


    //STAR_PLATINUM
    public static final Item STAR_PLATINUM_LONGSWORD = registerItem( "mythicmetals_compat/star_platinum/star_platinum_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier, -2.4f,
                    "mythicmetals:star_platinum_ingot"));

    public static final Item STAR_PLATINUM_TWINBLADE = registerItem( "mythicmetals_compat/star_platinum/star_platinum_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier, -1.7f,
                    "mythicmetals:star_platinum_ingot"));

    public static final Item STAR_PLATINUM_RAPIER = registerItem( "mythicmetals_compat/star_platinum/star_platinum_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:star_platinum_ingot"));

    public static final Item STAR_PLATINUM_KATANA = registerItem( "mythicmetals_compat/star_platinum/star_platinum_katana",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier, -2f,
                    "mythicmetals:star_platinum_ingot"));

    public static final Item STAR_PLATINUM_SAI = registerItem( "mythicmetals_compat/star_platinum/star_platinum_sai",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier - sai_modifier, -1.1f,
                    "mythicmetals:star_platinum_ingot"));

    public static final Item STAR_PLATINUM_SPEAR = registerItem( "mythicmetals_compat/star_platinum/star_platinum_spear",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier, -2.7f,
                    "mythicmetals:star_platinum_ingot"));

    public static final Item STAR_PLATINUM_GLAIVE = registerItem( "mythicmetals_compat/star_platinum/star_platinum_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier, -2.6f,
                    "mythicmetals:star_platinum_ingot"));

    public static final Item STAR_PLATINUM_WARGLAIVE = registerItem( "mythicmetals_compat/star_platinum/star_platinum_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier, -2.2f,
                    "mythicmetals:star_platinum_ingot"));

    public static final Item STAR_PLATINUM_CUTLASS = registerItem( "mythicmetals_compat/star_platinum/star_platinum_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier, -2f,
                    "mythicmetals:star_platinum_ingot"));

    public static final Item STAR_PLATINUM_CLAYMORE = registerItem( "mythicmetals_compat/star_platinum/star_platinum_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.STAR_PLATINUM, star_platinum_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:star_platinum_ingot"));


    //BRONZE
    public static final Item BRONZE_LONGSWORD = registerItem( "mythicmetals_compat/bronze/bronze_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier, -2.4f,
                    "mythicmetals:bronze_ingot"));

    public static final Item BRONZE_TWINBLADE = registerItem( "mythicmetals_compat/bronze/bronze_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier, -1.7f,
                    "mythicmetals:bronze_ingot"));

    public static final Item BRONZE_RAPIER = registerItem( "mythicmetals_compat/bronze/bronze_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier - rapier_modifier, -1.6f,
                    "mythicmetals:bronze_ingot"));

    public static final Item BRONZE_KATANA = registerItem( "mythicmetals_compat/bronze/bronze_katana",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier, -2f,
                    "mythicmetals:bronze_ingot"));

    public static final Item BRONZE_SAI = registerItem( "mythicmetals_compat/bronze/bronze_sai",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier - sai_modifier, -1.1f,
                    "mythicmetals:bronze_ingot"));

    public static final Item BRONZE_SPEAR = registerItem( "mythicmetals_compat/bronze/bronze_spear",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier, -2.7f,
                    "mythicmetals:bronze_ingot"));

    public static final Item BRONZE_GLAIVE = registerItem( "mythicmetals_compat/bronze/bronze_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier, -2.6f,
                    "mythicmetals:bronze_ingot"));

    public static final Item BRONZE_WARGLAIVE = registerItem( "mythicmetals_compat/bronze/bronze_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier, -2.2f,
                    "mythicmetals:bronze_ingot"));

    public static final Item BRONZE_CUTLASS = registerItem( "mythicmetals_compat/bronze/bronze_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier, -2f,
                    "mythicmetals:bronze_ingot"));

    public static final Item BRONZE_CLAYMORE = registerItem( "mythicmetals_compat/bronze/bronze_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.BRONZE, bronze_modifier + claymore_modifier, -2.8f,
                    "mythicmetals:bronze_ingot"));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mythic Metals compat Items for " + SimplySwords.MOD_ID);
    }

}
