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

    static int dragon_scale_modifier = Config.weaponAttribute.materialDamageModifier.dragon_scale_damageModifier.get();

    /* 1.21
    
    //Dragon Scale
    public static final Item DRAGON_SCALE_LONGSWORD = registerItem( "dragon_loot_compat/dragon_longsword",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + longsword_modifier), longsword_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_TWINBLADE = registerItem( "dragon_loot_compat/dragon_twinblade",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + twinblade_modifier), twinblade_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_RAPIER = registerItem( "dragon_loot_compat/dragon_rapier",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + rapier_modifier), rapier_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_KATANA = registerItem( "dragon_loot_compat/dragon_katana",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + katana_modifier), katana_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_SAI = registerItem( "dragon_loot_compat/dragon_sai",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + sai_modifier), sai_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_SPEAR = registerItem( "dragon_loot_compat/dragon_spear",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + spear_modifier), spear_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_GLAIVE = registerItem( "dragon_loot_compat/dragon_glaive",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + glaive_modifier), glaive_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_WARGLAIVE = registerItem( "dragon_loot_compat/dragon_warglaive",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + warglaive_modifier), warglaive_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_CUTLASS = registerItem( "dragon_loot_compat/dragon_cutlass",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + cutlass_modifier), cutlass_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_CLAYMORE = registerItem( "dragon_loot_compat/dragon_claymore",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + claymore_modifier), claymore_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_GREATHAMMER = registerItem( "dragon_loot_compat/dragon_greathammer",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + greathammer_modifier), greathammer_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_GREATAXE = registerItem( "dragon_loot_compat/dragon_greataxe",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + greataxe_modifier), greataxe_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_CHAKRAM = registerItem( "dragon_loot_compat/dragon_chakram",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + chakram_modifier), chakram_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_SCYTHE = registerItem( "dragon_loot_compat/dragon_scythe",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + scythe_modifier), scythe_attackspeed,
                    "dragonloot:dragon_scale"));

    public static final Item DRAGON_SCALE_HALBERD = registerItem( "dragon_loot_compat/dragon_halberd",
            new SimplySwordsNetheriteSwordItem(ModToolMaterial.DRAGON_SCALE, (int)(dragon_scale_modifier + halberd_modifier), halberd_attackspeed,
                    "dragonloot:dragon_scale"));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }

    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Dragon Loot compat Items for " + SimplySwords.MOD_ID);
    }

     */
}