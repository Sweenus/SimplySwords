package net.sweenus.simplyswords.item;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;

public class GobberCompat {

    //Compat for Mythic Metals
    static int rapier_modifier = 1;
    static int sai_modifier = 3;
    static int chakram_modifier = 1;
    static int scythe_modifier = 1;
    static int claymore_modifier = 2;
    static int greathammer_modifier = 4;
    static int greataxe_modifier = 3;
    static int gobber_modifier = 1;
    static int gobber_nether_modifier = 3;
    static int gobber_end_modifier = 6;


    //GOBBER
    public static final Item GOBBER_LONGSWORD = registerItem( "gobber_compat/gobber/gobber_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier, -2.4f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_TWINBLADE = registerItem( "gobber_compat/gobber/gobber_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier, -1.7f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_RAPIER = registerItem( "gobber_compat/gobber/gobber_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier - rapier_modifier, -1.6f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_KATANA = registerItem( "gobber_compat/gobber/gobber_katana",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier, -2f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_SAI = registerItem( "gobber_compat/gobber/gobber_sai",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier - sai_modifier, -1.1f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_SPEAR = registerItem( "gobber_compat/gobber/gobber_spear",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier, -2.7f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_GLAIVE = registerItem( "gobber_compat/gobber/gobber_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier, -2.6f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_WARGLAIVE = registerItem( "gobber_compat/gobber/gobber_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier, -2.2f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_CUTLASS = registerItem( "gobber_compat/gobber/gobber_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier, -2f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_CLAYMORE = registerItem( "gobber_compat/gobber/gobber_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + claymore_modifier, -2.8f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_GREATHAMMER = registerItem( "gobber_compat/gobber/gobber_greathammer",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + greathammer_modifier, -3.2f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_GREATAXE = registerItem( "gobber_compat/gobber/gobber_greataxe",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + greataxe_modifier, -3.2f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_CHAKRAM = registerItem( "gobber_compat/gobber/gobber_chakram",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier - chakram_modifier, -3f,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_SCYTHE = registerItem( "gobber_compat/gobber/gobber_scythe",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + scythe_modifier, -2.7f,
                    "gobber2:gobber2_ingot"));


    //GOBBER_NETHER
    public static final Item GOBBER_NETHER_LONGSWORD = registerItem( "gobber_compat/gobber_nether/gobber_nether_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier, -2.4f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_TWINBLADE = registerItem( "gobber_compat/gobber_nether/gobber_nether_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier, -1.7f,
                    "gobber2:gobber2_ingot_nether"));
    public static final Item GOBBER_NETHER_RAPIER = registerItem( "gobber_compat/gobber_nether/gobber_nether_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier - rapier_modifier, -1.6f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_KATANA = registerItem( "gobber_compat/gobber_nether/gobber_nether_katana",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier, -2f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_SAI = registerItem( "gobber_compat/gobber_nether/gobber_nether_sai",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier - sai_modifier, -1.1f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_SPEAR = registerItem( "gobber_compat/gobber_nether/gobber_nether_spear",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier, -2.7f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_GLAIVE = registerItem( "gobber_compat/gobber_nether/gobber_nether_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier, -2.6f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_WARGLAIVE = registerItem( "gobber_compat/gobber_nether/gobber_nether_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier, -2.2f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_CUTLASS = registerItem( "gobber_compat/gobber_nether/gobber_nether_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier, -2f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_CLAYMORE = registerItem( "gobber_compat/gobber_nether/gobber_nether_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + claymore_modifier, -2.8f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_GREATHAMMER = registerItem( "gobber_compat/gobber_nether/gobber_nether_greathammer",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + greathammer_modifier, -3.2f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_GREATAXE = registerItem( "gobber_compat/gobber_nether/gobber_nether_greataxe",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + greataxe_modifier, -3.1f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_CHAKRAM = registerItem( "gobber_compat/gobber_nether/gobber_nether_chakram",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier - chakram_modifier, -3f,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_SCYTHE = registerItem( "gobber_compat/gobber_nether/gobber_nether_scythe",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + scythe_modifier, -2.7f,
                    "gobber2:gobber2_ingot_nether"));


    //GOBBER_END
    public static final Item GOBBER_END_LONGSWORD = registerItem( "gobber_compat/gobber_end/gobber_end_longsword",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier, -2.4f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_TWINBLADE = registerItem( "gobber_compat/gobber_end/gobber_end_twinblade",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier, -1.7f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_RAPIER = registerItem( "gobber_compat/gobber_end/gobber_end_rapier",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier - rapier_modifier, -1.6f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_KATANA = registerItem( "gobber_compat/gobber_end/gobber_end_katana",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier, -2f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_SAI = registerItem( "gobber_compat/gobber_end/gobber_end_sai",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier - sai_modifier, -1.1f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_SPEAR = registerItem( "gobber_compat/gobber_end/gobber_end_spear",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier, -2.7f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_GLAIVE = registerItem( "gobber_compat/gobber_end/gobber_end_glaive",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier, -2.6f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_WARGLAIVE = registerItem( "gobber_compat/gobber_end/gobber_end_warglaive",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier, -2.2f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_CUTLASS = registerItem( "gobber_compat/gobber_end/gobber_end_cutlass",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier, -2f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_CLAYMORE = registerItem( "gobber_compat/gobber_end/gobber_end_claymore",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + claymore_modifier, -2.8f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_GREATHAMMER = registerItem( "gobber_compat/gobber_end/gobber_end_greathammer",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + greathammer_modifier, -3.2f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_GREATAXE = registerItem( "gobber_compat/gobber_end/gobber_end_greataxe",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + greataxe_modifier, -3.1f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_CHAKRAM = registerItem( "gobber_compat/gobber_end/gobber_end_chakram",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier - chakram_modifier, -3f,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_SCYTHE = registerItem( "gobber_compat/gobber_end/gobber_end_scythe",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + scythe_modifier, -2.7f,
                    "gobber2:gobber2_ingot_end"));
    

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Gobber compat Items for " + SimplySwords.MOD_ID);
    }

}
