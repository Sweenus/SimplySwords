package net.sweenus.simplyswords.compat;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.*;

public class AdvancedNetheriteCompat {

    //Compat for Advanced Netherite
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

    static float netherite_iron_modifier = Config.weaponAttribute.materialDamageModifier.netherite_iron_damageModifier.get();
    static float netherite_gold_modifier = Config.weaponAttribute.materialDamageModifier.netherite_gold_damageModifier.get();
    static float netherite_emerald_modifier = Config.weaponAttribute.materialDamageModifier.netherite_emerald_damageModifier.get();
    static float netherite_diamond_modifier = Config.weaponAttribute.materialDamageModifier.netherite_diamond_damageModifier.get();
    
    /* 1.21

    //Netherite-Iron
    public static final Item NETHERITE_IRON_LONGSWORD = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_longsword",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + longsword_modifier), longsword_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_TWINBLADE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_twinblade",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + twinblade_modifier), twinblade_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_RAPIER = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_rapier",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + rapier_modifier), rapier_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_KATANA = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_katana",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + katana_modifier), katana_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_SAI = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_sai",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + sai_modifier), sai_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_SPEAR = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_spear",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + spear_modifier), spear_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_GLAIVE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_glaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + glaive_modifier), glaive_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_WARGLAIVE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_warglaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + warglaive_modifier), warglaive_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_CUTLASS = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_cutlass",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + cutlass_modifier), cutlass_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_CLAYMORE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_claymore",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + claymore_modifier), claymore_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_GREATHAMMER = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_greathammer",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + greathammer_modifier), greathammer_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_GREATAXE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_greataxe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + greataxe_modifier), greataxe_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_CHAKRAM = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_chakram",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + chakram_modifier), chakram_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_SCYTHE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_scythe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + scythe_modifier), scythe_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_HALBERD = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_halberd",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, (int)(netherite_iron_modifier + halberd_modifier), halberd_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));


    //Netherite-Gold
    public static final Item NETHERITE_GOLD_LONGSWORD = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_longsword",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + longsword_modifier), longsword_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_TWINBLADE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_twinblade",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + twinblade_modifier), twinblade_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_RAPIER = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_rapier",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + rapier_modifier), rapier_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_KATANA = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_katana",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + katana_modifier), katana_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_SAI = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_sai",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + sai_modifier), sai_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_SPEAR = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_spear",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + spear_modifier), spear_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_GLAIVE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_glaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + glaive_modifier), glaive_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_WARGLAIVE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_warglaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + warglaive_modifier), warglaive_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_CUTLASS = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_cutlass",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + cutlass_modifier), cutlass_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_CLAYMORE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_claymore",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + claymore_modifier), claymore_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_GREATHAMMER = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_greathammer",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + greathammer_modifier), greathammer_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_GREATAXE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_greataxe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + greataxe_modifier), greataxe_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_CHAKRAM = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_chakram",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + chakram_modifier), chakram_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_SCYTHE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_scythe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + scythe_modifier), scythe_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_HALBERD = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_halberd",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, (int)(netherite_gold_modifier + halberd_modifier), halberd_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));


    //Netherite-Emerald
    public static final Item NETHERITE_EMERALD_LONGSWORD = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_longsword",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + longsword_modifier), longsword_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_TWINBLADE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_twinblade",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + twinblade_modifier), twinblade_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_RAPIER = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_rapier",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + rapier_modifier), rapier_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_KATANA = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_katana",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + katana_modifier), katana_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_SAI = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_sai",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + sai_modifier), sai_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_SPEAR = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_spear",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + spear_modifier), spear_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_GLAIVE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_glaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + glaive_modifier), glaive_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_WARGLAIVE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_warglaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + warglaive_modifier), warglaive_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_CUTLASS = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_cutlass",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + cutlass_modifier), cutlass_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_CLAYMORE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_claymore",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + claymore_modifier), claymore_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_GREATHAMMER = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_greathammer",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + greathammer_modifier), greathammer_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_GREATAXE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_greataxe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + greataxe_modifier), greataxe_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_CHAKRAM = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_chakram",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + chakram_modifier), chakram_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_SCYTHE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_scythe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + scythe_modifier), scythe_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_HALBERD = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_halberd",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, (int)(netherite_emerald_modifier + halberd_modifier), halberd_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));


    //Netherite-Diamond
    public static final Item NETHERITE_DIAMOND_LONGSWORD = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_longsword",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + longsword_modifier), longsword_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_TWINBLADE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_twinblade",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + twinblade_modifier), twinblade_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_RAPIER = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_rapier",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + rapier_modifier), rapier_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_KATANA = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_katana",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + katana_modifier), katana_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_SAI = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_sai",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + sai_modifier), sai_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_SPEAR = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_spear",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + spear_modifier), spear_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_GLAIVE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_glaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + glaive_modifier), glaive_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_WARGLAIVE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_warglaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + warglaive_modifier), warglaive_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_CUTLASS = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_cutlass",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + cutlass_modifier), cutlass_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_CLAYMORE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_claymore",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + claymore_modifier), claymore_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_GREATHAMMER = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_greathammer",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + greathammer_modifier), greathammer_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_GREATAXE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_greataxe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + greataxe_modifier), greataxe_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_CHAKRAM = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_chakram",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + chakram_modifier), chakram_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_SCYTHE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_scythe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + scythe_modifier), scythe_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_HALBERD = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_halberd",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, (int)(netherite_diamond_modifier + halberd_modifier), halberd_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier.of(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Advanced Netherite compat Items for " + SimplySwords.MOD_ID);
    }

     */
}