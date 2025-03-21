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
    static float longsword_positive_modifier = Config.getFloat("longsword_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.longsword_positiveDamageModifier);
    static float twinblade_positive_modifier = Config.getFloat("twinblade_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.twinblade_positiveDamageModifier);
    static float rapier_positive_modifier = Config.getFloat("rapier_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.rapier_positiveDamageModifier);
    static float katana_positive_modifier = Config.getFloat("katana_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.katana_positiveDamageModifier);
    static float sai_positive_modifier = Config.getFloat("sai_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.sai_positiveDamageModifier);
    static float spear_positive_modifier = Config.getFloat("spear_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.spear_positiveDamageModifier);
    static float glaive_positive_modifier = Config.getFloat("glaive_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.glaive_positiveDamageModifier);
    static float warglaive_positive_modifier = Config.getFloat("warglaive_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.warglaive_positiveDamageModifier);
    static float cutlass_positive_modifier = Config.getFloat("cutlass_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.cutlass_positiveDamageModifier);
    static float claymore_positive_modifier = Config.getFloat("claymore_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.claymore_positiveDamageModifier);
    static float greataxe_positive_modifier = Config.getFloat("greataxe_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.greataxe_positiveDamageModifier);
    static float greathammer_positive_modifier = Config.getFloat("greathammer_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.greathammer_positiveDamageModifier);
    static float chakram_positive_modifier = Config.getFloat("chakram_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.chakram_positiveDamageModifier);
    static float scythe_positive_modifier = Config.getFloat("scythe_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.scythe_positiveDamageModifier);
    static float halberd_positive_modifier = Config.getFloat("halberd_positiveDamageModifier", "WeaponAttributes", ConfigDefaultValues.halberd_positiveDamageModifier);

    static float longsword_negative_modifier = Config.getFloat("longsword_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.longsword_negativeDamageModifier);
    static float twinblade_negative_modifier = Config.getFloat("twinblade_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.twinblade_negativeDamageModifier);
    static float rapier_negative_modifier = Config.getFloat("rapier_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.rapier_negativeDamageModifier);
    static float sai_negative_modifier = Config.getFloat("sai_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.sai_negativeDamageModifier);
    static float spear_negative_modifier = Config.getFloat("spear_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.spear_negativeDamageModifier);
    static float katana_negative_modifier = Config.getFloat("katana_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.katana_negativeDamageModifier);
    static float glaive_negative_modifier = Config.getFloat("glaive_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.glaive_negativeDamageModifier);
    static float warglaive_negative_modifier = Config.getFloat("warglaive_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.warglaive_negativeDamageModifier);
    static float cutlass_negative_modifier = Config.getFloat("cutlass_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.cutlass_negativeDamageModifier);
    static float claymore_negative_modifier = Config.getFloat("claymore_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.claymore_negativeDamageModifier);
    static float greataxe_negative_modifier = Config.getFloat("greataxe_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.greataxe_negativeDamageModifier);
    static float greathammer_negative_modifier = Config.getFloat("greathammer_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.greathammer_negativeDamageModifier);
    static float chakram_negative_modifier = Config.getFloat("chakram_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.chakram_negativeDamageModifier);
    static float scythe_negative_modifier = Config.getFloat("scythe_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.scythe_negativeDamageModifier);
    static float halberd_negative_modifier = Config.getFloat("halberd_negativeDamageModifier", "WeaponAttributes", ConfigDefaultValues.halberd_negativeDamageModifier);

    static float longsword_attackspeed = Config.getFloat("longsword_attackSpeed", "WeaponAttributes", ConfigDefaultValues.longsword_attackSpeed);
    static float twinblade_attackspeed = Config.getFloat("twinblade_attackSpeed", "WeaponAttributes", ConfigDefaultValues.twinblade_attackSpeed);
    static float rapier_attackspeed = Config.getFloat("rapier_attackSpeed", "WeaponAttributes", ConfigDefaultValues.rapier_attackSpeed);
    static float sai_attackspeed = Config.getFloat("sai_attackSpeed", "WeaponAttributes", ConfigDefaultValues.sai_attackSpeed);
    static float spear_attackspeed = Config.getFloat("spear_attackSpeed", "WeaponAttributes", ConfigDefaultValues.spear_attackSpeed);
    static float katana_attackspeed = Config.getFloat("katana_attackSpeed", "WeaponAttributes", ConfigDefaultValues.katana_attackSpeed);
    static float glaive_attackspeed = Config.getFloat("glaive_attackSpeed", "WeaponAttributes", ConfigDefaultValues.glaive_attackSpeed);
    static float warglaive_attackspeed = Config.getFloat("warglaive_attackSpeed", "WeaponAttributes", ConfigDefaultValues.warglaive_attackSpeed);
    static float cutlass_attackspeed = Config.getFloat("cutlass_attackSpeed", "WeaponAttributes", ConfigDefaultValues.cutlass_attackSpeed);
    static float claymore_attackspeed = Config.getFloat("claymore_attackSpeed", "WeaponAttributes", ConfigDefaultValues.claymore_attackSpeed);
    static float greataxe_attackspeed = Config.getFloat("greataxe_attackSpeed", "WeaponAttributes", ConfigDefaultValues.greataxe_attackSpeed);
    static float greathammer_attackspeed = Config.getFloat("greathammer_attackSpeed", "WeaponAttributes", ConfigDefaultValues.greathammer_attackSpeed);
    static float chakram_attackspeed = Config.getFloat("chakram_attackSpeed", "WeaponAttributes", ConfigDefaultValues.chakram_attackSpeed);
    static float scythe_attackspeed = Config.getFloat("scythe_attackSpeed", "WeaponAttributes", ConfigDefaultValues.scythe_attackSpeed);
    static float halberd_attackspeed = Config.getFloat("halberd_attackSpeed", "WeaponAttributes", ConfigDefaultValues.halberd_attackSpeed);

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

    static int netherite_iron_modifier = (int) Config.getFloat("netherite_iron_damageModifier", "WeaponAttributes", ConfigDefaultValues.netheriteIron_damageModifier);
    static int netherite_gold_modifier = (int) Config.getFloat("netherite_gold_damageModifier", "WeaponAttributes", ConfigDefaultValues.netheriteGold_damageModifier);
    static int netherite_emerald_modifier = (int) Config.getFloat("netherite_emerald_damageModifier", "WeaponAttributes", ConfigDefaultValues.netheriteEmerald_damageModifier);
    static int netherite_diamond_modifier = (int) Config.getFloat("netherite_diamond_damageModifier", "WeaponAttributes", ConfigDefaultValues.netheriteDiamond_damageModifier);


    //Netherite-Iron
    public static final Item NETHERITE_IRON_LONGSWORD = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_longsword",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + longsword_modifier, longsword_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_TWINBLADE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_twinblade",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + twinblade_modifier, twinblade_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_RAPIER = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_rapier",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + rapier_modifier, rapier_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_KATANA = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_katana",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + katana_modifier, katana_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_SAI = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_sai",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + sai_modifier, sai_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_SPEAR = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_spear",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + spear_modifier, spear_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_GLAIVE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_glaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + glaive_modifier, glaive_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_WARGLAIVE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_warglaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + warglaive_modifier, warglaive_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_CUTLASS = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_cutlass",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + cutlass_modifier, cutlass_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_CLAYMORE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_claymore",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + claymore_modifier, claymore_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_GREATHAMMER = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_greathammer",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + greathammer_modifier, greathammer_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_GREATAXE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_greataxe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + greataxe_modifier, greataxe_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_CHAKRAM = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_chakram",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + chakram_modifier, chakram_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_SCYTHE = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_scythe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + scythe_modifier, scythe_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));

    public static final Item NETHERITE_IRON_HALBERD = registerItem( "advanced_netherite_compat/netherite_iron/netherite_iron_halberd",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_IRON, netherite_iron_modifier + halberd_modifier, halberd_attackspeed,
                    "advancednetherite:netherite_iron_ingot"));


    //Netherite-Gold
    public static final Item NETHERITE_GOLD_LONGSWORD = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_longsword",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + longsword_modifier, longsword_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_TWINBLADE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_twinblade",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + twinblade_modifier, twinblade_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_RAPIER = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_rapier",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + rapier_modifier, rapier_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_KATANA = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_katana",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + katana_modifier, katana_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_SAI = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_sai",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + sai_modifier, sai_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_SPEAR = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_spear",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + spear_modifier, spear_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_GLAIVE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_glaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + glaive_modifier, glaive_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_WARGLAIVE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_warglaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + warglaive_modifier, warglaive_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_CUTLASS = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_cutlass",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + cutlass_modifier, cutlass_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_CLAYMORE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_claymore",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + claymore_modifier, claymore_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_GREATHAMMER = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_greathammer",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + greathammer_modifier, greathammer_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_GREATAXE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_greataxe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + greataxe_modifier, greataxe_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_CHAKRAM = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_chakram",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + chakram_modifier, chakram_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_SCYTHE = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_scythe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + scythe_modifier, scythe_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));

    public static final Item NETHERITE_GOLD_HALBERD = registerItem( "advanced_netherite_compat/netherite_gold/netherite_gold_halberd",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_GOLD, netherite_gold_modifier + halberd_modifier, halberd_attackspeed,
                    "advancednetherite:netherite_gold_ingot"));


    //Netherite-Emerald
    public static final Item NETHERITE_EMERALD_LONGSWORD = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_longsword",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + longsword_modifier, longsword_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_TWINBLADE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_twinblade",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + twinblade_modifier, twinblade_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_RAPIER = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_rapier",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + rapier_modifier, rapier_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_KATANA = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_katana",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + katana_modifier, katana_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_SAI = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_sai",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + sai_modifier, sai_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_SPEAR = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_spear",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + spear_modifier, spear_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_GLAIVE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_glaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + glaive_modifier, glaive_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_WARGLAIVE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_warglaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + warglaive_modifier, warglaive_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_CUTLASS = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_cutlass",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + cutlass_modifier, cutlass_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_CLAYMORE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_claymore",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + claymore_modifier, claymore_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_GREATHAMMER = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_greathammer",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + greathammer_modifier, greathammer_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_GREATAXE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_greataxe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + greataxe_modifier, greataxe_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_CHAKRAM = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_chakram",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + chakram_modifier, chakram_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_SCYTHE = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_scythe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + scythe_modifier, scythe_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));

    public static final Item NETHERITE_EMERALD_HALBERD = registerItem( "advanced_netherite_compat/netherite_emerald/netherite_emerald_halberd",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_EMERALD, netherite_emerald_modifier + halberd_modifier, halberd_attackspeed,
                    "advancednetherite:netherite_emerald_ingot"));


    //Netherite-Diamond
    public static final Item NETHERITE_DIAMOND_LONGSWORD = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_longsword",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + longsword_modifier, longsword_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_TWINBLADE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_twinblade",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + twinblade_modifier, twinblade_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_RAPIER = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_rapier",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + rapier_modifier, rapier_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_KATANA = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_katana",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + katana_modifier, katana_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_SAI = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_sai",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + sai_modifier, sai_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_SPEAR = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_spear",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + spear_modifier, spear_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_GLAIVE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_glaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + glaive_modifier, glaive_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_WARGLAIVE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_warglaive",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + warglaive_modifier, warglaive_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_CUTLASS = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_cutlass",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + cutlass_modifier, cutlass_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_CLAYMORE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_claymore",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + claymore_modifier, claymore_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_GREATHAMMER = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_greathammer",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + greathammer_modifier, greathammer_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_GREATAXE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_greataxe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + greataxe_modifier, greataxe_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_CHAKRAM = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_chakram",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + chakram_modifier, chakram_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_SCYTHE = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_scythe",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + scythe_modifier, scythe_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));

    public static final Item NETHERITE_DIAMOND_HALBERD = registerItem( "advanced_netherite_compat/netherite_diamond/netherite_diamond_halberd",
            new AdvancedNetheriteSwordItem(ModToolMaterial.NETHERITE_DIAMOND, netherite_diamond_modifier + halberd_modifier, halberd_attackspeed,
                    "advancednetherite:netherite_diamond_ingot"));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Advanced Netherite compat Items for " + SimplySwords.MOD_ID);
    }

}
