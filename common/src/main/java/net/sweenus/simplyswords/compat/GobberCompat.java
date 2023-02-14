package net.sweenus.simplyswords.compat;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.GobberEndSwordItem;
import net.sweenus.simplyswords.item.GobberNetherSwordItem;
import net.sweenus.simplyswords.item.ModToolMaterial;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;

public class GobberCompat {

    //Compat for Gobber
    static float longsword_positive_modifier = SimplySwordsConfig.getWeaponAttributes("longsword_positive_damage_modifier");
    static float twinblade_positive_modifier = SimplySwordsConfig.getWeaponAttributes("twinblade_positive_damage_modifier");
    static float rapier_positive_modifier = SimplySwordsConfig.getWeaponAttributes("rapier_positive_damage_modifier");
    static float katana_positive_modifier = SimplySwordsConfig.getWeaponAttributes("katana_positive_damage_modifier");
    static float sai_positive_modifier = SimplySwordsConfig.getWeaponAttributes("sai_positive_damage_modifier");
    static float spear_positive_modifier = SimplySwordsConfig.getWeaponAttributes("spear_positive_damage_modifier");
    static float glaive_positive_modifier = SimplySwordsConfig.getWeaponAttributes("glaive_positive_damage_modifier");
    static float warglaive_positive_modifier = SimplySwordsConfig.getWeaponAttributes("warglaive_positive_damage_modifier");
    static float cutlass_positive_modifier = SimplySwordsConfig.getWeaponAttributes("cutlass_positive_damage_modifier");
    static float claymore_positive_modifier = SimplySwordsConfig.getWeaponAttributes("claymore_positive_damage_modifier");
    static float greataxe_positive_modifier = SimplySwordsConfig.getWeaponAttributes("greataxe_positive_damage_modifier");
    static float greathammer_positive_modifier = SimplySwordsConfig.getWeaponAttributes("greathammer_positive_damage_modifier");
    static float chakram_positive_modifier = SimplySwordsConfig.getWeaponAttributes("chakram_positive_damage_modifier");
    static float scythe_positive_modifier = SimplySwordsConfig.getWeaponAttributes("scythe_positive_damage_modifier");
    static float halberd_positive_modifier = SimplySwordsConfig.getWeaponAttributes("halberd_positive_damage_modifier");

    static float longsword_negative_modifier = SimplySwordsConfig.getWeaponAttributes("longsword_negative_damage_modifier");
    static float twinblade_negative_modifier = SimplySwordsConfig.getWeaponAttributes("twinblade_negative_damage_modifier");
    static float rapier_negative_modifier = SimplySwordsConfig.getWeaponAttributes("rapier_negative_damage_modifier");
    static float sai_negative_modifier = SimplySwordsConfig.getWeaponAttributes("sai_negative_damage_modifier");
    static float spear_negative_modifier = SimplySwordsConfig.getWeaponAttributes("spear_negative_damage_modifier");
    static float katana_negative_modifier = SimplySwordsConfig.getWeaponAttributes("katana_negative_damage_modifier");
    static float glaive_negative_modifier = SimplySwordsConfig.getWeaponAttributes("glaive_negative_damage_modifier");
    static float warglaive_negative_modifier = SimplySwordsConfig.getWeaponAttributes("warglaive_negative_damage_modifier");
    static float cutlass_negative_modifier = SimplySwordsConfig.getWeaponAttributes("cutlass_negative_damage_modifier");
    static float claymore_negative_modifier = SimplySwordsConfig.getWeaponAttributes("claymore_negative_damage_modifier");
    static float greataxe_negative_modifier = SimplySwordsConfig.getWeaponAttributes("greataxe_negative_damage_modifier");
    static float greathammer_negative_modifier = SimplySwordsConfig.getWeaponAttributes("greathammer_negative_damage_modifier");
    static float chakram_negative_modifier = SimplySwordsConfig.getWeaponAttributes("chakram_negative_damage_modifier");
    static float scythe_negative_modifier = SimplySwordsConfig.getWeaponAttributes("scythe_negative_damage_modifier");
    static float halberd_negative_modifier = SimplySwordsConfig.getWeaponAttributes("halberd_negative_damage_modifier");

    static float longsword_attackspeed = SimplySwordsConfig.getWeaponAttributes("longsword_attackspeed");
    static float twinblade_attackspeed = SimplySwordsConfig.getWeaponAttributes("twinblade_attackspeed");
    static float rapier_attackspeed = SimplySwordsConfig.getWeaponAttributes("rapier_attackspeed");
    static float sai_attackspeed = SimplySwordsConfig.getWeaponAttributes("sai_attackspeed");
    static float spear_attackspeed = SimplySwordsConfig.getWeaponAttributes("spear_attackspeed");
    static float katana_attackspeed = SimplySwordsConfig.getWeaponAttributes("katana_attackspeed");
    static float glaive_attackspeed = SimplySwordsConfig.getWeaponAttributes("glaive_attackspeed");
    static float warglaive_attackspeed = SimplySwordsConfig.getWeaponAttributes("warglaive_attackspeed");
    static float cutlass_attackspeed = SimplySwordsConfig.getWeaponAttributes("cutlass_attackspeed");
    static float claymore_attackspeed = SimplySwordsConfig.getWeaponAttributes("claymore_attackspeed");
    static float greataxe_attackspeed = SimplySwordsConfig.getWeaponAttributes("greataxe_attackspeed");
    static float greathammer_attackspeed = SimplySwordsConfig.getWeaponAttributes("greathammer_attackspeed");
    static float chakram_attackspeed = SimplySwordsConfig.getWeaponAttributes("chakram_attackspeed");
    static float scythe_attackspeed = SimplySwordsConfig.getWeaponAttributes("scythe_attackspeed");
    static float halberd_attackspeed = SimplySwordsConfig.getWeaponAttributes("halberd_attackspeed");

    static int longsword_modifier = (int) (longsword_positive_modifier - longsword_negative_modifier);
    static int twinblade_modifier = (int) (twinblade_positive_modifier - twinblade_negative_modifier);
    static int rapier_modifier = (int) (rapier_positive_modifier - rapier_negative_modifier);
    static int sai_modifier = (int) (sai_positive_modifier - sai_negative_modifier);
    static int spear_modifier = (int) (spear_positive_modifier - spear_negative_modifier);
    static int katana_modifier = (int) (katana_positive_modifier - katana_negative_modifier);
    static int glaive_modifier = (int) (glaive_positive_modifier - glaive_negative_modifier);
    static int warglaive_modifier = (int) (warglaive_positive_modifier - warglaive_negative_modifier);
    static int cutlass_modifier = (int) (cutlass_positive_modifier - cutlass_negative_modifier);
    static int chakram_modifier = (int) (chakram_positive_modifier - chakram_negative_modifier);
    static int scythe_modifier = (int) (scythe_positive_modifier - scythe_negative_modifier);
    static int claymore_modifier = (int) (claymore_positive_modifier - claymore_negative_modifier);
    static int greathammer_modifier = (int) (greathammer_positive_modifier - greathammer_negative_modifier);
    static int greataxe_modifier = (int) (greataxe_positive_modifier - greataxe_negative_modifier);
    static int halberd_modifier = (int) (halberd_positive_modifier - halberd_negative_modifier);
    static int gobber_modifier = (int) SimplySwordsConfig.getWeaponAttributes("gobber_damage_modifier");
    static int gobber_nether_modifier = (int) SimplySwordsConfig.getWeaponAttributes("gobber_nether_damage_modifier");
    static int gobber_end_modifier = (int) SimplySwordsConfig.getWeaponAttributes("gobber_end_damage_modifier");


    //GOBBER
    public static final Item GOBBER_LONGSWORD = registerItem( "gobber_compat/gobber/gobber_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + longsword_modifier, longsword_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_TWINBLADE = registerItem( "gobber_compat/gobber/gobber_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + twinblade_modifier, twinblade_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_RAPIER = registerItem( "gobber_compat/gobber/gobber_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + rapier_modifier, rapier_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_KATANA = registerItem( "gobber_compat/gobber/gobber_katana",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + katana_modifier, katana_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_SAI = registerItem( "gobber_compat/gobber/gobber_sai",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + sai_modifier, sai_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_SPEAR = registerItem( "gobber_compat/gobber/gobber_spear",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + spear_modifier, spear_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_GLAIVE = registerItem( "gobber_compat/gobber/gobber_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + glaive_modifier, glaive_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_WARGLAIVE = registerItem( "gobber_compat/gobber/gobber_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + warglaive_modifier, warglaive_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_CUTLASS = registerItem( "gobber_compat/gobber/gobber_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + cutlass_modifier, cutlass_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_CLAYMORE = registerItem( "gobber_compat/gobber/gobber_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + claymore_modifier, claymore_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_GREATHAMMER = registerItem( "gobber_compat/gobber/gobber_greathammer",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + greathammer_modifier, greathammer_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_GREATAXE = registerItem( "gobber_compat/gobber/gobber_greataxe",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + greataxe_modifier, greataxe_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_CHAKRAM = registerItem( "gobber_compat/gobber/gobber_chakram",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + chakram_modifier, chakram_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_SCYTHE = registerItem( "gobber_compat/gobber/gobber_scythe",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + scythe_modifier, scythe_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_HALBERD = registerItem( "gobber_compat/gobber/gobber_halberd",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + halberd_modifier, halberd_attackspeed,
                    "gobber2:gobber2_ingot"));


    //GOBBER_NETHER
    public static final Item GOBBER_NETHER_LONGSWORD = registerItem( "gobber_compat/gobber_nether/gobber_nether_longsword",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + longsword_modifier, longsword_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_TWINBLADE = registerItem( "gobber_compat/gobber_nether/gobber_nether_twinblade",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + twinblade_modifier, twinblade_attackspeed,
                    "gobber2:gobber2_ingot_nether"));
    public static final Item GOBBER_NETHER_RAPIER = registerItem( "gobber_compat/gobber_nether/gobber_nether_rapier",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + rapier_modifier, rapier_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_KATANA = registerItem( "gobber_compat/gobber_nether/gobber_nether_katana",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + katana_modifier, katana_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_SAI = registerItem( "gobber_compat/gobber_nether/gobber_nether_sai",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + sai_modifier, sai_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_SPEAR = registerItem( "gobber_compat/gobber_nether/gobber_nether_spear",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + spear_modifier, spear_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_GLAIVE = registerItem( "gobber_compat/gobber_nether/gobber_nether_glaive",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + glaive_modifier, glaive_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_WARGLAIVE = registerItem( "gobber_compat/gobber_nether/gobber_nether_warglaive",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + warglaive_modifier, warglaive_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_CUTLASS = registerItem( "gobber_compat/gobber_nether/gobber_nether_cutlass",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + cutlass_modifier, cutlass_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_CLAYMORE = registerItem( "gobber_compat/gobber_nether/gobber_nether_claymore",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + claymore_modifier, claymore_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_GREATHAMMER = registerItem( "gobber_compat/gobber_nether/gobber_nether_greathammer",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + greathammer_modifier, greathammer_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_GREATAXE = registerItem( "gobber_compat/gobber_nether/gobber_nether_greataxe",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + greataxe_modifier, greataxe_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_CHAKRAM = registerItem( "gobber_compat/gobber_nether/gobber_nether_chakram",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + chakram_modifier, chakram_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_SCYTHE = registerItem( "gobber_compat/gobber_nether/gobber_nether_scythe",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + scythe_modifier, scythe_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_HALBERD = registerItem( "gobber_compat/gobber_nether/gobber_nether_halberd",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + halberd_modifier, halberd_attackspeed,
                    "gobber2:gobber2_ingot_nether"));


    //GOBBER_END
    public static final Item GOBBER_END_LONGSWORD = registerItem( "gobber_compat/gobber_end/gobber_end_longsword",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + longsword_modifier, longsword_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_TWINBLADE = registerItem( "gobber_compat/gobber_end/gobber_end_twinblade",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + twinblade_modifier, twinblade_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_RAPIER = registerItem( "gobber_compat/gobber_end/gobber_end_rapier",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + rapier_modifier, rapier_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_KATANA = registerItem( "gobber_compat/gobber_end/gobber_end_katana",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + katana_modifier, katana_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_SAI = registerItem( "gobber_compat/gobber_end/gobber_end_sai",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + sai_modifier, sai_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_SPEAR = registerItem( "gobber_compat/gobber_end/gobber_end_spear",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + spear_modifier, spear_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_GLAIVE = registerItem( "gobber_compat/gobber_end/gobber_end_glaive",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + glaive_modifier, glaive_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_WARGLAIVE = registerItem( "gobber_compat/gobber_end/gobber_end_warglaive",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + warglaive_modifier, warglaive_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_CUTLASS = registerItem( "gobber_compat/gobber_end/gobber_end_cutlass",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + cutlass_modifier, cutlass_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_CLAYMORE = registerItem( "gobber_compat/gobber_end/gobber_end_claymore",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + claymore_modifier, claymore_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_GREATHAMMER = registerItem( "gobber_compat/gobber_end/gobber_end_greathammer",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + greathammer_modifier, greathammer_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_GREATAXE = registerItem( "gobber_compat/gobber_end/gobber_end_greataxe",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + greataxe_modifier, greataxe_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_CHAKRAM = registerItem( "gobber_compat/gobber_end/gobber_end_chakram",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + chakram_modifier, chakram_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_SCYTHE = registerItem( "gobber_compat/gobber_end/gobber_end_scythe",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + scythe_modifier, scythe_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_HALBERD = registerItem( "gobber_compat/gobber_end/gobber_end_halberd",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + halberd_modifier, halberd_attackspeed,
                    "gobber2:gobber2_ingot_end"));
    

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Gobber compat Items for " + SimplySwords.MOD_ID);
    }

}
