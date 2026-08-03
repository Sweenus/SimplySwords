package net.sweenus.simplyswords.forge.compat;

import net.sweenus.simplyswords.config.Config;

public class GobberCompat {

    //Compat for Gobber
    static float longsword_modifier = Config.weaponAttribute.typeDamageModifier.longsword_damageModifier;
    static float twinblade_modifier = Config.weaponAttribute.typeDamageModifier.twinblade_damageModifier;
    static float rapier_modifier = Config.weaponAttribute.typeDamageModifier.rapier_damageModifier;
    static float katana_modifier = Config.weaponAttribute.typeDamageModifier.katana_damageModifier;
    static float sai_modifier = Config.weaponAttribute.typeDamageModifier.sai_damageModifier;
    static float spear_modifier = Config.weaponAttribute.typeDamageModifier.spear_damageModifier;
    static float glaive_modifier = Config.weaponAttribute.typeDamageModifier.glaive_damageModifier;
    static float warglaive_modifier = Config.weaponAttribute.typeDamageModifier.warglaive_damageModifier;
    static float cutlass_modifier = Config.weaponAttribute.typeDamageModifier.cutlass_damageModifier;
    static float claymore_modifier = Config.weaponAttribute.typeDamageModifier.claymore_damageModifier;
    static float greataxe_modifier = Config.weaponAttribute.typeDamageModifier.greataxe_damageModifier;
    static float greathammer_modifier = Config.weaponAttribute.typeDamageModifier.greathammer_damageModifier;
    static float chakram_modifier = Config.weaponAttribute.typeDamageModifier.chakram_damageModifier;
    static float scythe_modifier = Config.weaponAttribute.typeDamageModifier.scythe_damageModifier;
    static float halberd_modifier = Config.weaponAttribute.typeDamageModifier.halberd_damageModifier;

    static float longsword_attackspeed = Config.weaponAttribute.typeAttackSpeed.longsword_attackSpeed;
    static float twinblade_attackspeed = Config.weaponAttribute.typeAttackSpeed.twinblade_attackSpeed;
    static float rapier_attackspeed = Config.weaponAttribute.typeAttackSpeed.rapier_attackSpeed;
    static float sai_attackspeed = Config.weaponAttribute.typeAttackSpeed.sai_attackSpeed;
    static float spear_attackspeed = Config.weaponAttribute.typeAttackSpeed.spear_attackSpeed;
    static float katana_attackspeed = Config.weaponAttribute.typeAttackSpeed.katana_attackSpeed;
    static float glaive_attackspeed = Config.weaponAttribute.typeAttackSpeed.glaive_attackSpeed;
    static float warglaive_attackspeed = Config.weaponAttribute.typeAttackSpeed.warglaive_attackSpeed;
    static float cutlass_attackspeed = Config.weaponAttribute.typeAttackSpeed.cutlass_attackSpeed;
    static float claymore_attackspeed = Config.weaponAttribute.typeAttackSpeed.claymore_attackSpeed;
    static float greataxe_attackspeed = Config.weaponAttribute.typeAttackSpeed.greataxe_attackSpeed;
    static float greathammer_attackspeed = Config.weaponAttribute.typeAttackSpeed.greathammer_attackSpeed;
    static float chakram_attackspeed = Config.weaponAttribute.typeAttackSpeed.chakram_attackSpeed;
    static float scythe_attackspeed = Config.weaponAttribute.typeAttackSpeed.scythe_attackSpeed;
    static float halberd_attackspeed = Config.weaponAttribute.typeAttackSpeed.halberd_attackSpeed;

    static float gobber_modifier = Config.weaponAttribute.materialDamageModifier.gobber_damageModifier.get();
    static float gobber_nether_modifier = Config.weaponAttribute.materialDamageModifier.gobberNether_damageModifier.get();
    static float gobber_end_modifier = Config.weaponAttribute.materialDamageModifier.gobberEnd_damageModifier.get();

    //GOBBER
    /*

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

     */

}