package net.sweenus.simplyswords.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.custom.*;

public class ModItems {
   // public static final Item ITEMNAMEHERE = registerItem( "item_name",
        //new Item(new FabricItemSettings().group(ItemGroup.MISC)));

    static float iron_modifier = (int)SimplySwordsConfig.getWeaponAttributes("iron_damage_modifier");
    static float gold_modifier = (int)SimplySwordsConfig.getWeaponAttributes("gold_damage_modifier");
    static float diamond_modifier = (int)SimplySwordsConfig.getWeaponAttributes("diamond_damage_modifier");
    static float netherite_modifier = (int)SimplySwordsConfig.getWeaponAttributes("netherite_damage_modifier");
    static float runic_modifier = (int)SimplySwordsConfig.getWeaponAttributes("runic_damage_modifier");


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

    static float brimstone_attackspeed = SimplySwordsConfig.getWeaponAttributes("brimstone_attackspeed");
    static float thewatcher_attackspeed = SimplySwordsConfig.getWeaponAttributes("thewatcher_attackspeed");
    static float stormsedge_attackspeed = SimplySwordsConfig.getWeaponAttributes("stormsedge_attackspeed");
    static float stormbringer_attackspeed = SimplySwordsConfig.getWeaponAttributes("stormbringer_attackspeed");
    static float swordonastick_attackspeed = SimplySwordsConfig.getWeaponAttributes("swordonastick_attackspeed");
    static float bramblethorn_attackspeed = SimplySwordsConfig.getWeaponAttributes("bramblethorn_attackspeed");
    static float watchingwarglaive_attackspeed = SimplySwordsConfig.getWeaponAttributes("watchingwarglaive_attackspeed");
    static float longswordofplague_attackspeed = SimplySwordsConfig.getWeaponAttributes("longswordofplague_attackspeed");
    static float emberblade_attackspeed = SimplySwordsConfig.getWeaponAttributes("emberblade_attackspeed");
    static float hearthflame_attackspeed = SimplySwordsConfig.getWeaponAttributes("hearthflame_attackspeed");
    static float soulkeeper_attackspeed = SimplySwordsConfig.getWeaponAttributes("soulkeeper_attackspeed");
    static float twistedblade_attackspeed = SimplySwordsConfig.getWeaponAttributes("twistedblade_attackspeed");
    static float soulstealer_attackspeed = SimplySwordsConfig.getWeaponAttributes("soulstealer_attackspeed");
    static float soulrender_attackspeed = SimplySwordsConfig.getWeaponAttributes("soulrender_attackspeed");
    static float mjolnir_attackspeed = SimplySwordsConfig.getWeaponAttributes("mjolnir_attackspeed");

    static float brimstone_damage_modifier = SimplySwordsConfig.getWeaponAttributes("brimstone_damage_modifier");
    static float thewatcher_damage_modifier = SimplySwordsConfig.getWeaponAttributes("thewatcher_damage_modifier");
    static float stormsedge_damage_modifier = SimplySwordsConfig.getWeaponAttributes("stormsedge_damage_modifier");
    static float stormbringer_damage_modifier = SimplySwordsConfig.getWeaponAttributes("stormbringer_damage_modifier");
    static float swordonastick_damage_modifier = SimplySwordsConfig.getWeaponAttributes("swordonastick_damage_modifier");
    static float bramblethorn_damage_modifier = SimplySwordsConfig.getWeaponAttributes("bramblethorn_damage_modifier");
    static float watchingwarglaive_damage_modifier = SimplySwordsConfig.getWeaponAttributes("watchingwarglaive_damage_modifier");
    static float longswordofplague_damage_modifier = SimplySwordsConfig.getWeaponAttributes("longswordofplague_damage_modifier");
    static float emberblade_damage_modifier = SimplySwordsConfig.getWeaponAttributes("emberblade_damage_modifier");
    static float hearthflame_damage_modifier = SimplySwordsConfig.getWeaponAttributes("hearthflame_damage_modifier");
    static float soulkeeper_damage_modifier = SimplySwordsConfig.getWeaponAttributes("soulkeeper_damage_modifier");
    static float twistedblade_damage_modifier = SimplySwordsConfig.getWeaponAttributes("twistedblade_damage_modifier");
    static float soulstealer_damage_modifier = SimplySwordsConfig.getWeaponAttributes("soulstealer_damage_modifier");
    static float soulrender_damage_modifier = SimplySwordsConfig.getWeaponAttributes("soulrender_damage_modifier");
    static float mjolnir_damage_modifier = SimplySwordsConfig.getWeaponAttributes("mjolnir_damage_modifier");


    //IRON
    public static final Item IRON_LONGSWORD = registerItem( "iron_longsword",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_TWINBLADE = registerItem( "iron_twinblade",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    "minecraft:iron_ingot"));
    public static final Item IRON_RAPIER = registerItem( "iron_rapier",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_KATANA = registerItem( "iron_katana",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_SAI = registerItem( "iron_sai",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_SPEAR = registerItem( "iron_spear",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_GLAIVE = registerItem( "iron_glaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_WARGLAIVE = registerItem( "iron_warglaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_CUTLASS = registerItem( "iron_cutlass",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    "minecraft:iron_ingot"));
    public static final Item IRON_CLAYMORE = registerItem( "iron_claymore",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_GREATHAMMER = registerItem( "iron_greathammer",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_GREATAXE = registerItem( "iron_greataxe",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_CHAKRAM = registerItem( "iron_chakram",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    "minecraft:iron_ingot"));

    public static final Item IRON_SCYTHE = registerItem( "iron_scythe",
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    "minecraft:iron_ingot"));

    //GOLD
    public static final Item GOLD_LONGSWORD = registerItem( "gold_longsword",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_TWINBLADE = registerItem( "gold_twinblade",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_RAPIER = registerItem( "gold_rapier",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_KATANA = registerItem( "gold_katana",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_SAI = registerItem( "gold_sai",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_SPEAR = registerItem( "gold_spear",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_GLAIVE = registerItem( "gold_glaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_WARGLAIVE = registerItem( "gold_warglaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_CUTLASS = registerItem( "gold_cutlass",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    "minecraft:gold_ingot"));
    public static final Item GOLD_CLAYMORE = registerItem( "gold_claymore",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_GREATHAMMER = registerItem( "gold_greathammer",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_GREATAXE = registerItem( "gold_greataxe",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_CHAKRAM = registerItem( "gold_chakram",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    "minecraft:gold_ingot"));

    public static final Item GOLD_SCYTHE = registerItem( "gold_scythe",
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    "minecraft:gold_ingot"));

    //DIAMOND
    public static final Item DIAMOND_LONGSWORD = registerItem( "diamond_longsword",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:diamond"));

    public static final Item DIAMOND_TWINBLADE = registerItem( "diamond_twinblade",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    "minecraft:diamond"));
    public static final Item DIAMOND_RAPIER = registerItem( "diamond_rapier",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    "minecraft:diamond"));
    public static final Item DIAMOND_KATANA = registerItem( "diamond_katana",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    "minecraft:diamond"));
    public static final Item DIAMOND_SAI = registerItem( "diamond_sai",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    "minecraft:diamond"));

    public static final Item DIAMOND_SPEAR = registerItem( "diamond_spear",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    "minecraft:diamond"));

    public static final Item DIAMOND_GLAIVE = registerItem( "diamond_glaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    "minecraft:diamond"));

    public static final Item DIAMOND_WARGLAIVE = registerItem( "diamond_warglaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    "minecraft:diamond"));
    public static final Item DIAMOND_CUTLASS = registerItem( "diamond_cutlass",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    "minecraft:diamond"));
    public static final Item DIAMOND_CLAYMORE = registerItem( "diamond_claymore",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    "minecraft:diamond"));

    public static final Item DIAMOND_GREATHAMMER = registerItem( "diamond_greathammer",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    "minecraft:diamond"));

    public static final Item DIAMOND_GREATAXE = registerItem( "diamond_greataxe",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    "minecraft:diamond"));

    public static final Item DIAMOND_CHAKRAM = registerItem( "diamond_chakram",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    "minecraft:diamond_ingot"));

    public static final Item DIAMOND_SCYTHE = registerItem( "diamond_scythe",
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    "minecraft:diamond_ingot"));

    //NETHERITE

    public static final Item NETHERITE_LONGSWORD = registerItem( "netherite_longsword",
            new SimplySwordsSwordItem(ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_TWINBLADE = registerItem( "netherite_twinblade",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_RAPIER = registerItem( "netherite_rapier",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (iron_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    "minecraft:netherite_ingot"));
    public static final Item NETHERITE_KATANA = registerItem( "netherite_katana",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    "minecraft:netherite_ingot"));
    public static final Item NETHERITE_SAI = registerItem( "netherite_sai",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_SPEAR = registerItem( "netherite_spear",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_GLAIVE = registerItem( "netherite_glaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_WARGLAIVE = registerItem( "netherite_warglaive",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_CUTLASS = registerItem( "netherite_cutlass",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_CLAYMORE = registerItem( "netherite_claymore",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_GREATHAMMER = registerItem( "netherite_greathammer",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_GREATAXE = registerItem( "netherite_greataxe",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_CHAKRAM = registerItem( "netherite_chakram",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final Item NETHERITE_SCYTHE = registerItem( "netherite_scythe",
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    "minecraft:netherite_ingot"));


    //RUNIC
    public static final Item RUNIC_LONGSWORD = registerItem( "runic_longsword",
            new FreezeSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_TWINBLADE = registerItem( "runic_twinblade",
            new WildfireSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_RAPIER = registerItem( "runic_rapier",
            new SpeedSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_KATANA = registerItem( "runic_katana",
            new WildfireSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_SAI = registerItem( "runic_sai",
            new SlownessSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_SPEAR = registerItem( "runic_spear",
            new FreezeSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_GLAIVE = registerItem( "runic_glaive",
            new WildfireSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_CUTLASS = registerItem( "runic_cutlass",
            new LevitationSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));
    public static final Item RUNIC_CLAYMORE = registerItem( "runic_claymore",
            new FreezeSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_CHAKRAM = registerItem( "runic_chakram",
            new SpeedSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_GREATAXE = registerItem( "runic_greataxe",
            new FreezeSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_GREATHAMMER = registerItem( "runic_greathammer",
            new WildfireSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final Item RUNIC_SCYTHE = registerItem( "runic_scythe",
            new LevitationSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.RARE)));

//SPECIAL
    public static final Item BRIMSTONE_CLAYMORE = registerItem( "brimstone_claymore",
            new FireSwordItem(ToolMaterials.NETHERITE,
                    (int) (brimstone_damage_modifier),
                    brimstone_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item WATCHER_CLAYMORE = registerItem( "watcher_claymore",
            new WatcherSwordItem(ToolMaterials.NETHERITE,
                    (int) (thewatcher_damage_modifier),
                    thewatcher_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item STORMS_EDGE = registerItem( "storms_edge",
            new StormSwordItem(ToolMaterials.NETHERITE,
                    (int) (stormsedge_damage_modifier),
                    stormsedge_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item STORMBRINGER = registerItem( "stormbringer",
            new StormSwordItem(ToolMaterials.NETHERITE,
                    (int) (stormbringer_damage_modifier),
                    stormbringer_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item SWORD_ON_A_STICK = registerItem( "sword_on_a_stick",
            new SwordItem(ToolMaterials.WOOD,
                    (int) (swordonastick_damage_modifier),
                    swordonastick_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item BRAMBLETHORN = registerItem( "bramblethorn",
            new BrambleSwordItem(ToolMaterials.WOOD,
                    (int) (bramblethorn_damage_modifier),
                    bramblethorn_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item WATCHING_WARGLAIVE = registerItem( "watching_warglaive",
            new WatcherSwordItem(ToolMaterials.NETHERITE,
                    (int) (watchingwarglaive_damage_modifier),
                    watchingwarglaive_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));
    public static final Item TOXIC_LONGSWORD = registerItem( "toxic_longsword",
            new PlagueSwordItem(ToolMaterials.NETHERITE,
                    (int) (longswordofplague_damage_modifier),
                    longswordofplague_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item EMBERBLADE = registerItem( "emberblade",
            new EmberIreSwordItem(ToolMaterials.NETHERITE,
                    (int) (emberblade_damage_modifier),
                    emberblade_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item HEARTHFLAME = registerItem( "hearthflame",
            new VolcanicFurySwordItem(ToolMaterials.NETHERITE,
                    (int) (hearthflame_damage_modifier),
                    hearthflame_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item SOULKEEPER = registerItem( "soulkeeper",
            new SoulSwordItem(ToolMaterials.NETHERITE,
                    (int) (soulkeeper_damage_modifier),
                    soulkeeper_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item TWISTED_BLADE = registerItem( "twisted_blade",
            new HasteSwordItem(ToolMaterials.NETHERITE,
                    (int) (twistedblade_damage_modifier),
                    twistedblade_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    //public static final Item TWILIGHT = registerItem( "twilight",
            //new GravSwordItem(ToolMaterials.NETHERITE, 4, -3.7f,
                    //new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item SOULSTEALER = registerItem( "soulstealer",
            new StealSwordItem(ToolMaterials.NETHERITE,
                    (int) (soulstealer_damage_modifier),
                    soulstealer_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item SOULRENDER = registerItem( "soulrender",
            new RendSwordItem(ToolMaterials.NETHERITE,
                    (int) (soulrender_damage_modifier),
                    soulrender_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));



    //public static final Item MAGIC_ESTOC = registerItem( "magic_estoc",
            //new SwordItem(ToolMaterials.NETHERITE, 3, -2.4f,
                    //new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final Item MJOLNIR = registerItem( "mjolnir",
            new StormSwordItem(ToolMaterials.NETHERITE,
                    (int) (mjolnir_damage_modifier),
                    mjolnir_attackspeed,
                    new FabricItemSettings().group(ModItemGroup.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SimplySwords.MOD_ID, name), item);
    }


    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mod Items for " + SimplySwords.MOD_ID);
    }

}
