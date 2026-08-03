package net.sweenus.simplyswords.compat;

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
    /* 1.21


    //GOBBER
    public static final Item GOBBER_LONGSWORD = registerItem( "gobber_compat/gobber/gobber_longsword",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + longsword_modifier), longsword_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_TWINBLADE = registerItem( "gobber_compat/gobber/gobber_twinblade",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + twinblade_modifier), twinblade_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_RAPIER = registerItem( "gobber_compat/gobber/gobber_rapier",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + rapier_modifier), rapier_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_KATANA = registerItem( "gobber_compat/gobber/gobber_katana",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + katana_modifier), katana_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_SAI = registerItem( "gobber_compat/gobber/gobber_sai",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + sai_modifier), sai_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_SPEAR = registerItem( "gobber_compat/gobber/gobber_spear",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + spear_modifier), spear_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_GLAIVE = registerItem( "gobber_compat/gobber/gobber_glaive",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + glaive_modifier), glaive_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_WARGLAIVE = registerItem( "gobber_compat/gobber/gobber_warglaive",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + warglaive_modifier), warglaive_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_CUTLASS = registerItem( "gobber_compat/gobber/gobber_cutlass",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + cutlass_modifier), cutlass_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_CLAYMORE = registerItem( "gobber_compat/gobber/gobber_claymore",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + claymore_modifier), claymore_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_GREATHAMMER = registerItem( "gobber_compat/gobber/gobber_greathammer",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + greathammer_modifier), greathammer_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_GREATAXE = registerItem( "gobber_compat/gobber/gobber_greataxe",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + greataxe_modifier), greataxe_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_CHAKRAM = registerItem( "gobber_compat/gobber/gobber_chakram",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + chakram_modifier), chakram_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_SCYTHE = registerItem( "gobber_compat/gobber/gobber_scythe",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + scythe_modifier), scythe_attackspeed,
                    "gobber2:gobber2_ingot"));

    public static final Item GOBBER_HALBERD = registerItem( "gobber_compat/gobber/gobber_halberd",
            new SimplySwordsSwordItem(ModToolMaterial.GOBBER, (int)(gobber_modifier + halberd_modifier), halberd_attackspeed,
                    "gobber2:gobber2_ingot"));


    //GOBBER_NETHER
    public static final Item GOBBER_NETHER_LONGSWORD = registerItem( "gobber_compat/gobber_nether/gobber_nether_longsword",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + longsword_modifier), longsword_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_TWINBLADE = registerItem( "gobber_compat/gobber_nether/gobber_nether_twinblade",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + twinblade_modifier), twinblade_attackspeed,
                    "gobber2:gobber2_ingot_nether"));
    public static final Item GOBBER_NETHER_RAPIER = registerItem( "gobber_compat/gobber_nether/gobber_nether_rapier",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + rapier_modifier), rapier_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_KATANA = registerItem( "gobber_compat/gobber_nether/gobber_nether_katana",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + katana_modifier), katana_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_SAI = registerItem( "gobber_compat/gobber_nether/gobber_nether_sai",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + sai_modifier), sai_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_SPEAR = registerItem( "gobber_compat/gobber_nether/gobber_nether_spear",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + spear_modifier), spear_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_GLAIVE = registerItem( "gobber_compat/gobber_nether/gobber_nether_glaive",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + glaive_modifier), glaive_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_WARGLAIVE = registerItem( "gobber_compat/gobber_nether/gobber_nether_warglaive",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + warglaive_modifier), warglaive_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_CUTLASS = registerItem( "gobber_compat/gobber_nether/gobber_nether_cutlass",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + cutlass_modifier), cutlass_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_CLAYMORE = registerItem( "gobber_compat/gobber_nether/gobber_nether_claymore",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + claymore_modifier), claymore_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_GREATHAMMER = registerItem( "gobber_compat/gobber_nether/gobber_nether_greathammer",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + greathammer_modifier), greathammer_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_GREATAXE = registerItem( "gobber_compat/gobber_nether/gobber_nether_greataxe",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + greataxe_modifier), greataxe_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_CHAKRAM = registerItem( "gobber_compat/gobber_nether/gobber_nether_chakram",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + chakram_modifier), chakram_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_SCYTHE = registerItem( "gobber_compat/gobber_nether/gobber_nether_scythe",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + scythe_modifier), scythe_attackspeed,
                    "gobber2:gobber2_ingot_nether"));

    public static final Item GOBBER_NETHER_HALBERD = registerItem( "gobber_compat/gobber_nether/gobber_nether_halberd",
            new GobberNetherSwordItem(ModToolMaterial.GOBBER_NETHER, (int)(gobber_nether_modifier + halberd_modifier), halberd_attackspeed,
                    "gobber2:gobber2_ingot_nether"));


    //GOBBER_END
    public static final Item GOBBER_END_LONGSWORD = registerItem( "gobber_compat/gobber_end/gobber_end_longsword",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + longsword_modifier), longsword_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_TWINBLADE = registerItem( "gobber_compat/gobber_end/gobber_end_twinblade",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + twinblade_modifier), twinblade_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_RAPIER = registerItem( "gobber_compat/gobber_end/gobber_end_rapier",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + rapier_modifier), rapier_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_KATANA = registerItem( "gobber_compat/gobber_end/gobber_end_katana",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + katana_modifier), katana_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_SAI = registerItem( "gobber_compat/gobber_end/gobber_end_sai",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + sai_modifier), sai_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_SPEAR = registerItem( "gobber_compat/gobber_end/gobber_end_spear",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + spear_modifier), spear_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_GLAIVE = registerItem( "gobber_compat/gobber_end/gobber_end_glaive",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + glaive_modifier), glaive_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_WARGLAIVE = registerItem("gobber_compat/gobber_end/gobber_end_warglaive",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + warglaive_modifier), warglaive_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_CUTLASS = registerItem( "gobber_compat/gobber_end/gobber_end_cutlass",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + cutlass_modifier), cutlass_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_CLAYMORE = registerItem( "gobber_compat/gobber_end/gobber_end_claymore",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + claymore_modifier), claymore_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_GREATHAMMER = registerItem( "gobber_compat/gobber_end/gobber_end_greathammer",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + greathammer_modifier), greathammer_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_GREATAXE = registerItem( "gobber_compat/gobber_end/gobber_end_greataxe",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + greataxe_modifier), greataxe_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_CHAKRAM = registerItem( "gobber_compat/gobber_end/gobber_end_chakram",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + chakram_modifier), chakram_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_SCYTHE = registerItem( "gobber_compat/gobber_end/gobber_end_scythe",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + scythe_modifier), scythe_attackspeed,
                    "gobber2:gobber2_ingot_end"));

    public static final Item GOBBER_END_HALBERD = registerItem( "gobber_compat/gobber_end/gobber_end_halberd",
            new GobberEndSwordItem(ModToolMaterial.GOBBER_END, (int)(gobber_end_modifier + halberd_modifier), halberd_attackspeed,
                    "gobber2:gobber2_ingot_end"));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Gobber compat Items for " + SimplySwords.MOD_ID);
    }


     */
}