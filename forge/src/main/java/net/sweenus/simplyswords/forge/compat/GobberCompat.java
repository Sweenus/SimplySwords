package net.sweenus.simplyswords.forge.compat;

import net.minecraft.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.GobberEndSwordItem;
import net.sweenus.simplyswords.item.GobberNetherSwordItem;
import net.sweenus.simplyswords.item.ModToolMaterial;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;

public class GobberCompat {

    //Compat for Gobber
    static float longsword_positive_modifier = SimplySwords.weaponAttributesConfig.longsword_positiveDamageModifier;
    static float twinblade_positive_modifier = SimplySwords.weaponAttributesConfig.twinblade_positiveDamageModifier;
    static float rapier_positive_modifier = SimplySwords.weaponAttributesConfig.rapier_positiveDamageModifier;
    static float katana_positive_modifier = SimplySwords.weaponAttributesConfig.katana_positiveDamageModifier;
    static float sai_positive_modifier = SimplySwords.weaponAttributesConfig.sai_positiveDamageModifier;
    static float spear_positive_modifier = SimplySwords.weaponAttributesConfig.spear_positiveDamageModifier;
    static float glaive_positive_modifier = SimplySwords.weaponAttributesConfig.glaive_positiveDamageModifier;
    static float warglaive_positive_modifier = SimplySwords.weaponAttributesConfig.warglaive_positiveDamageModifier;
    static float cutlass_positive_modifier = SimplySwords.weaponAttributesConfig.cutlass_positiveDamageModifier;
    static float claymore_positive_modifier = SimplySwords.weaponAttributesConfig.claymore_positiveDamageModifier;
    static float greataxe_positive_modifier = SimplySwords.weaponAttributesConfig.greataxe_positiveDamageModifier;
    static float greathammer_positive_modifier = SimplySwords.weaponAttributesConfig.greathammer_positiveDamageModifier;
    static float chakram_positive_modifier = SimplySwords.weaponAttributesConfig.chakram_positiveDamageModifier;
    static float scythe_positive_modifier = SimplySwords.weaponAttributesConfig.scythe_positiveDamageModifier;
    static float halberd_positive_modifier = SimplySwords.weaponAttributesConfig.halberd_positiveDamageModifier;

    static float longsword_negative_modifier = SimplySwords.weaponAttributesConfig.longsword_negativeDamageModifier;
    static float twinblade_negative_modifier = SimplySwords.weaponAttributesConfig.twinblade_negativeDamageModifier;
    static float rapier_negative_modifier = SimplySwords.weaponAttributesConfig.rapier_negativeDamageModifier;
    static float sai_negative_modifier = SimplySwords.weaponAttributesConfig.sai_negativeDamageModifier;
    static float spear_negative_modifier = SimplySwords.weaponAttributesConfig.spear_negativeDamageModifier;
    static float katana_negative_modifier = SimplySwords.weaponAttributesConfig.katana_negativeDamageModifier;
    static float glaive_negative_modifier = SimplySwords.weaponAttributesConfig.glaive_negativeDamageModifier;
    static float warglaive_negative_modifier = SimplySwords.weaponAttributesConfig.warglaive_negativeDamageModifier;
    static float cutlass_negative_modifier = SimplySwords.weaponAttributesConfig.cutlass_negativeDamageModifier;
    static float claymore_negative_modifier = SimplySwords.weaponAttributesConfig.claymore_negativeDamageModifier;
    static float greataxe_negative_modifier = SimplySwords.weaponAttributesConfig.greataxe_negativeDamageModifier;
    static float greathammer_negative_modifier = SimplySwords.weaponAttributesConfig.greathammer_negativeDamageModifier;
    static float chakram_negative_modifier = SimplySwords.weaponAttributesConfig.chakram_negativeDamageModifier;
    static float scythe_negative_modifier = SimplySwords.weaponAttributesConfig.scythe_negativeDamageModifier;
    static float halberd_negative_modifier = SimplySwords.weaponAttributesConfig.halberd_negativeDamageModifier;

    static float longsword_attackspeed = SimplySwords.weaponAttributesConfig.longsword_attackSpeed;
    static float twinblade_attackspeed = SimplySwords.weaponAttributesConfig.twinblade_attackSpeed;
    static float rapier_attackspeed = SimplySwords.weaponAttributesConfig.rapier_attackSpeed;
    static float sai_attackspeed = SimplySwords.weaponAttributesConfig.sai_attackSpeed;
    static float spear_attackspeed = SimplySwords.weaponAttributesConfig.spear_attackSpeed;
    static float katana_attackspeed = SimplySwords.weaponAttributesConfig.katana_attackSpeed;
    static float glaive_attackspeed = SimplySwords.weaponAttributesConfig.glaive_attackSpeed;
    static float warglaive_attackspeed = SimplySwords.weaponAttributesConfig.warglaive_attackSpeed;
    static float cutlass_attackspeed = SimplySwords.weaponAttributesConfig.cutlass_attackSpeed;
    static float claymore_attackspeed = SimplySwords.weaponAttributesConfig.claymore_attackSpeed;
    static float greataxe_attackspeed = SimplySwords.weaponAttributesConfig.greataxe_attackSpeed;
    static float greathammer_attackspeed = SimplySwords.weaponAttributesConfig.greathammer_attackSpeed;
    static float chakram_attackspeed = SimplySwords.weaponAttributesConfig.chakram_attackSpeed;
    static float scythe_attackspeed = SimplySwords.weaponAttributesConfig.scythe_attackSpeed;
    static float halberd_attackspeed = SimplySwords.weaponAttributesConfig.halberd_attackSpeed;

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
    static int gobber_modifier = (int) SimplySwords.weaponAttributesConfig.gobber_damageModifier;
    static int gobber_nether_modifier = (int) SimplySwords.weaponAttributesConfig.gobberNether_damageModifier;
    static int gobber_end_modifier = (int) SimplySwords.weaponAttributesConfig.gobberEnd_damageModifier;

    //GOBBER

    public static final DeferredRegister<Item> GOBBER_ITEM = DeferredRegister.create(ForgeRegistries.ITEMS, SimplySwords.MOD_ID);
    
    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_LONGSWORD = GOBBER_ITEM.register("gobber_compat/gobber/gobber_longsword", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + longsword_modifier, longsword_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_TWINBLADE = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_twinblade", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + twinblade_modifier, twinblade_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_RAPIER = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_rapier", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + rapier_modifier, rapier_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_KATANA = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_katana", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + katana_modifier, katana_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_SAI = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_sai", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + sai_modifier, sai_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_SPEAR = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_spear", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + spear_modifier, spear_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_GLAIVE = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_glaive", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + glaive_modifier, glaive_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_WARGLAIVE = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_warglaive", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + warglaive_modifier, warglaive_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_CUTLASS = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_cutlass", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + cutlass_modifier, cutlass_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_CLAYMORE = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_claymore", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + claymore_modifier, claymore_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_GREATHAMMER = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_greathammer", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + greathammer_modifier, greathammer_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_GREATAXE = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_greataxe", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + greataxe_modifier, greataxe_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_CHAKRAM = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_chakram", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + chakram_modifier, chakram_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_SCYTHE = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_scythe", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + scythe_modifier, scythe_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final RegistryObject<SimplySwordsSwordItem> GOBBER_HALBERD = GOBBER_ITEM.register( "gobber_compat/gobber/gobber_halberd", () ->
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, gobber_modifier + halberd_modifier, halberd_attackspeed,
                    "gobber2:gobber2_ingot"));


    //GOBBER_NETHER
    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_LONGSWORD = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_longsword", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + longsword_modifier, longsword_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_TWINBLADE = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_twinblade", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + twinblade_modifier, twinblade_attackspeed,
                    "gobber2:gobber2_ingot_nether"));
    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_RAPIER = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_rapier", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + rapier_modifier, rapier_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_KATANA = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_katana", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + katana_modifier, katana_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_SAI = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_sai", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + sai_modifier, sai_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_SPEAR = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_spear", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + spear_modifier, spear_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_GLAIVE = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_glaive", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + glaive_modifier, glaive_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_WARGLAIVE = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_warglaive", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + warglaive_modifier, warglaive_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_CUTLASS = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_cutlass", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + cutlass_modifier, cutlass_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_CLAYMORE = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_claymore", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + claymore_modifier, claymore_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_GREATHAMMER = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_greathammer", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + greathammer_modifier, greathammer_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_GREATAXE = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_greataxe", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + greataxe_modifier, greataxe_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_CHAKRAM = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_chakram", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + chakram_modifier, chakram_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_SCYTHE = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_scythe", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + scythe_modifier, scythe_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final RegistryObject<GobberNetherSwordItem> GOBBER_NETHER_HALBERD = GOBBER_ITEM.register( "gobber_compat/gobber_nether/gobber_nether_halberd", () ->
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, gobber_nether_modifier + halberd_modifier, halberd_attackspeed,
                    "gobber2:gobber2_ingot_nether"));


    //GOBBER_END
    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_LONGSWORD = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_longsword", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + longsword_modifier, longsword_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_TWINBLADE = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_twinblade", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + twinblade_modifier, twinblade_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_RAPIER = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_rapier", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + rapier_modifier, rapier_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_KATANA = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_katana", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + katana_modifier, katana_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_SAI = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_sai", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + sai_modifier, sai_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_SPEAR = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_spear", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + spear_modifier, spear_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_GLAIVE = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_glaive", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + glaive_modifier, glaive_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_WARGLAIVE = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_warglaive", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + warglaive_modifier, warglaive_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_CUTLASS = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_cutlass", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + cutlass_modifier, cutlass_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_CLAYMORE = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_claymore", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + claymore_modifier, claymore_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_GREATHAMMER = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_greathammer", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + greathammer_modifier, greathammer_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_GREATAXE = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_greataxe", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + greataxe_modifier, greataxe_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_CHAKRAM = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_chakram", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + chakram_modifier, chakram_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_SCYTHE = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_scythe", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + scythe_modifier, scythe_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final RegistryObject<GobberEndSwordItem> GOBBER_END_HALBERD = GOBBER_ITEM.register( "gobber_compat/gobber_end/gobber_end_halberd", () ->
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, gobber_end_modifier + halberd_modifier, halberd_attackspeed,
                    "gobber2:gobber2_ingot_end"));


    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Gobber compat Items for " + SimplySwords.MOD_ID);
    }

}
