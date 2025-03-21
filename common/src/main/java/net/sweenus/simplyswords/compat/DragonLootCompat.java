package net.sweenus.simplyswords.compat;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.*;

public class DragonLootCompat {

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

    static int dragon_scale_modifier = (int) Config.getFloat("dragon_scale_damageModifier", "WeaponAttributes", ConfigDefaultValues.dragonScale_damageModifier);


    //Dragon Scale
    public static final Item DRAGON_SCALE_LONGSWORD = registerItem( "dragon_loot_compat/dragon_longsword",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + longsword_modifier, longsword_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_TWINBLADE = registerItem( "dragon_loot_compat/dragon_twinblade",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + twinblade_modifier, twinblade_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_RAPIER = registerItem( "dragon_loot_compat/dragon_rapier",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + rapier_modifier, rapier_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_KATANA = registerItem( "dragon_loot_compat/dragon_katana",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + katana_modifier, katana_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_SAI = registerItem( "dragon_loot_compat/dragon_sai",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + sai_modifier, sai_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_SPEAR = registerItem( "dragon_loot_compat/dragon_spear",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + spear_modifier, spear_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_GLAIVE = registerItem( "dragon_loot_compat/dragon_glaive",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + glaive_modifier, glaive_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_WARGLAIVE = registerItem( "dragon_loot_compat/dragon_warglaive",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + warglaive_modifier, warglaive_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_CUTLASS = registerItem( "dragon_loot_compat/dragon_cutlass",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + cutlass_modifier, cutlass_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_CLAYMORE = registerItem( "dragon_loot_compat/dragon_claymore",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + claymore_modifier, claymore_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_GREATHAMMER = registerItem( "dragon_loot_compat/dragon_greathammer",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + greathammer_modifier, greathammer_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_GREATAXE = registerItem( "dragon_loot_compat/dragon_greataxe",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + greataxe_modifier, greataxe_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_CHAKRAM = registerItem( "dragon_loot_compat/dragon_chakram",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + chakram_modifier, chakram_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_SCYTHE = registerItem( "dragon_loot_compat/dragon_scythe",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + scythe_modifier, scythe_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_HALBERD = registerItem( "dragon_loot_compat/dragon_halberd",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, dragon_scale_modifier + halberd_modifier, halberd_attackspeed,
                    "dragonloot:dragon_scale"));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }



    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Dragon Loot compat Items for " + SimplySwords.MOD_ID);
    }

}
