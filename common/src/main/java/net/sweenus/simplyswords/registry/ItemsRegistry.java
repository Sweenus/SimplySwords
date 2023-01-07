package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;
import net.sweenus.simplyswords.item.custom.*;

public class ItemsRegistry {

    static float iron_modifier = (int) SimplySwordsConfig.getWeaponAttributes("iron_damage_modifier");
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
    static float thedispatcher_attackspeed = SimplySwordsConfig.getWeaponAttributes("thedispatcher_attackspeed");
    static float mjolnir_attackspeed = SimplySwordsConfig.getWeaponAttributes("mjolnir_attackspeed");
    static float soulpyre_attackspeed = SimplySwordsConfig.getWeaponAttributes("soulpyre_attackspeed");
    static float frostfall_attackspeed = SimplySwordsConfig.getWeaponAttributes("frostfall_attackspeed");
    static float moltenedge_attackspeed = SimplySwordsConfig.getWeaponAttributes("moltenedge_attackspeed");
    static float livyatan_attackspeed = SimplySwordsConfig.getWeaponAttributes("livyatan_attackspeed");
    static float icewhisper_attackspeed = SimplySwordsConfig.getWeaponAttributes("icewhisper_attackspeed");

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
    static float thedispatcher_damage_modifier = SimplySwordsConfig.getWeaponAttributes("thedispatcher_damage_modifier");
    static float mjolnir_damage_modifier = SimplySwordsConfig.getWeaponAttributes("mjolnir_damage_modifier");
    static float soulpyre_damage_modifier = SimplySwordsConfig.getWeaponAttributes("soulpyre_damage_modifier");
    static float frostfall_damage_modifier = SimplySwordsConfig.getWeaponAttributes("frostfall_damage_modifier");
    static float moltenedge_damage_modifier = SimplySwordsConfig.getWeaponAttributes("moltenedge_damage_modifier");
    static float livyatan_damage_modifier = SimplySwordsConfig.getWeaponAttributes("livyatan_damage_modifier");
    static float icewhisper_damage_modifier = SimplySwordsConfig.getWeaponAttributes("icewhisper_damage_modifier");

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(SimplySwords.MOD_ID, Registry.ITEM_KEY);

    public static final RegistrySupplier<Item> RUNIC_TABLET = ITEM.register("runic_tablet", () ->
            new Item(new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));


    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_LONGSWORD = ITEM.register("iron_longsword", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_TWINBLADE = ITEM.register( "iron_twinblade", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_RAPIER = ITEM.register( "iron_rapier", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_KATANA = ITEM.register( "iron_katana", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_SAI = ITEM.register( "iron_sai", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_SPEAR = ITEM.register( "iron_spear", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_GLAIVE = ITEM.register( "iron_glaive", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_WARGLAIVE = ITEM.register( "iron_warglaive", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_CUTLASS = ITEM.register( "iron_cutlass", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    "minecraft:iron_ingot"));
    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_CLAYMORE = ITEM.register( "iron_claymore", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_GREATHAMMER = ITEM.register( "iron_greathammer", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_GREATAXE = ITEM.register( "iron_greataxe", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_CHAKRAM = ITEM.register( "iron_chakram", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_SCYTHE = ITEM.register( "iron_scythe", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    "minecraft:iron_ingot"));

    //GOLD
    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_LONGSWORD = ITEM.register( "gold_longsword", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:gold_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_TWINBLADE = ITEM.register( "gold_twinblade", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    "minecraft:gold_ingot"));
    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_RAPIER = ITEM.register( "gold_rapier", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    "minecraft:gold_ingot"));
    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_KATANA = ITEM.register( "gold_katana", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    "minecraft:gold_ingot"));
    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_SAI = ITEM.register( "gold_sai", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    "minecraft:gold_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_SPEAR = ITEM.register( "gold_spear", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    "minecraft:gold_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_GLAIVE = ITEM.register( "gold_glaive", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    "minecraft:gold_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_WARGLAIVE = ITEM.register( "gold_warglaive", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    "minecraft:gold_ingot"));
    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_CUTLASS = ITEM.register( "gold_cutlass", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    "minecraft:gold_ingot"));
    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_CLAYMORE = ITEM.register( "gold_claymore", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    "minecraft:gold_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_GREATHAMMER = ITEM.register( "gold_greathammer", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    "minecraft:gold_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_GREATAXE = ITEM.register( "gold_greataxe", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    "minecraft:gold_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_CHAKRAM = ITEM.register( "gold_chakram", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    "minecraft:gold_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_SCYTHE = ITEM.register( "gold_scythe", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    "minecraft:gold_ingot"));

    //DIAMOND
    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_LONGSWORD = ITEM.register( "diamond_longsword", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:diamond"));

    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_TWINBLADE = ITEM.register( "diamond_twinblade", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    "minecraft:diamond"));
    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_RAPIER = ITEM.register( "diamond_rapier", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    "minecraft:diamond"));
    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_KATANA = ITEM.register( "diamond_katana", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    "minecraft:diamond"));
    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_SAI = ITEM.register( "diamond_sai", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    "minecraft:diamond"));

    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_SPEAR = ITEM.register( "diamond_spear", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    "minecraft:diamond"));

    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_GLAIVE = ITEM.register( "diamond_glaive", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    "minecraft:diamond"));

    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_WARGLAIVE = ITEM.register( "diamond_warglaive", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    "minecraft:diamond"));
    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_CUTLASS = ITEM.register( "diamond_cutlass", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    "minecraft:diamond"));
    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_CLAYMORE = ITEM.register( "diamond_claymore", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    "minecraft:diamond"));

    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_GREATHAMMER = ITEM.register( "diamond_greathammer", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    "minecraft:diamond"));

    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_GREATAXE = ITEM.register( "diamond_greataxe", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    "minecraft:diamond"));

    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_CHAKRAM = ITEM.register( "diamond_chakram", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    "minecraft:diamond_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_SCYTHE = ITEM.register( "diamond_scythe", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    "minecraft:diamond_ingot"));

    //NETHERITE

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_LONGSWORD = ITEM.register( "netherite_longsword", () ->
            new SimplySwordsSwordItem(ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_TWINBLADE = ITEM.register( "netherite_twinblade", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_RAPIER = ITEM.register( "netherite_rapier", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (iron_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    "minecraft:netherite_ingot"));
    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_KATANA = ITEM.register( "netherite_katana", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    "minecraft:netherite_ingot"));
    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_SAI = ITEM.register( "netherite_sai", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_SPEAR = ITEM.register( "netherite_spear", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_GLAIVE = ITEM.register( "netherite_glaive", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_WARGLAIVE = ITEM.register( "netherite_warglaive", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_CUTLASS = ITEM.register( "netherite_cutlass", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_CLAYMORE = ITEM.register( "netherite_claymore", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_GREATHAMMER = ITEM.register( "netherite_greathammer", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_GREATAXE = ITEM.register( "netherite_greataxe", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_CHAKRAM = ITEM.register( "netherite_chakram", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> NETHERITE_SCYTHE = ITEM.register( "netherite_scythe", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    "minecraft:netherite_ingot"));


    //RUNIC
    public static final RegistrySupplier<RunicSwordItem> RUNIC_LONGSWORD = ITEM.register( "runic_longsword", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_TWINBLADE = ITEM.register( "runic_twinblade", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_RAPIER = ITEM.register( "runic_rapier", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_KATANA = ITEM.register( "runic_katana", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_SAI = ITEM.register( "runic_sai", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_SPEAR = ITEM.register( "runic_spear", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_GLAIVE = ITEM.register( "runic_glaive", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_CUTLASS = ITEM.register( "runic_cutlass", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));
    public static final RegistrySupplier<RunicSwordItem> RUNIC_CLAYMORE = ITEM.register( "runic_claymore", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_CHAKRAM = ITEM.register( "runic_chakram", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_GREATAXE = ITEM.register( "runic_greataxe", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_GREATHAMMER = ITEM.register( "runic_greathammer", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));
    public static final RegistrySupplier<RunicSwordItem> RUNIC_WARGLAIVE = ITEM.register( "runic_warglaive", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_SCYTHE = ITEM.register( "runic_scythe", () ->
            new RunicSwordItem(ToolMaterials.NETHERITE,
                    (int) (runic_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    //SPECIAL
    public static final RegistrySupplier<FireSwordItem> BRIMSTONE_CLAYMORE = ITEM.register( "brimstone_claymore", () ->
            new FireSwordItem(ToolMaterials.NETHERITE,
                    (int) (brimstone_damage_modifier),
                    brimstone_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<WatcherSwordItem> WATCHER_CLAYMORE = ITEM.register( "watcher_claymore", () ->
            new WatcherSwordItem(ToolMaterials.NETHERITE,
                    (int) (thewatcher_damage_modifier),
                    thewatcher_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<StormSwordItem> STORMS_EDGE = ITEM.register( "storms_edge", () ->
            new StormSwordItem(ToolMaterials.NETHERITE,
                    (int) (stormsedge_damage_modifier),
                    stormsedge_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<StormSwordItem> STORMBRINGER = ITEM.register( "stormbringer", () ->
            new StormSwordItem(ToolMaterials.NETHERITE,
                    (int) (stormbringer_damage_modifier),
                    stormbringer_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<SwordItem> SWORD_ON_A_STICK = ITEM.register( "sword_on_a_stick", () ->
            new SwordItem(ToolMaterials.WOOD,
                    (int) (swordonastick_damage_modifier),
                    swordonastick_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<BrambleSwordItem> BRAMBLETHORN = ITEM.register( "bramblethorn", () ->
            new BrambleSwordItem(ToolMaterials.WOOD,
                    (int) (bramblethorn_damage_modifier),
                    bramblethorn_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<WatcherSwordItem> WATCHING_WARGLAIVE = ITEM.register( "watching_warglaive", () ->
            new WatcherSwordItem(ToolMaterials.NETHERITE,
                    (int) (watchingwarglaive_damage_modifier),
                    watchingwarglaive_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));
    public static final RegistrySupplier<PlagueSwordItem> TOXIC_LONGSWORD = ITEM.register( "toxic_longsword", () ->
            new PlagueSwordItem(ToolMaterials.NETHERITE,
                    (int) (longswordofplague_damage_modifier),
                    longswordofplague_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<EmberIreSwordItem> EMBERBLADE = ITEM.register( "emberblade", () ->
            new EmberIreSwordItem(ToolMaterials.NETHERITE,
                    (int) (emberblade_damage_modifier),
                    emberblade_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<VolcanicFurySwordItem> HEARTHFLAME = ITEM.register( "hearthflame", () ->
            new VolcanicFurySwordItem(ToolMaterials.NETHERITE,
                    (int) (hearthflame_damage_modifier),
                    hearthflame_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<SoulSwordItem> SOULKEEPER = ITEM.register( "soulkeeper", () ->
            new SoulSwordItem(ToolMaterials.NETHERITE,
                    (int) (soulkeeper_damage_modifier),
                    soulkeeper_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<HasteSwordItem> TWISTED_BLADE = ITEM.register( "twisted_blade", () ->
            new HasteSwordItem(ToolMaterials.NETHERITE,
                    (int) (twistedblade_damage_modifier),
                    twistedblade_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<StealSwordItem> SOULSTEALER = ITEM.register("soulstealer", () ->
            new StealSwordItem(ToolMaterials.NETHERITE,
                    (int) (soulstealer_damage_modifier),
                    soulstealer_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<RendSwordItem> SOULRENDER = ITEM.register( "soulrender", () ->
            new RendSwordItem(ToolMaterials.NETHERITE,
                    (int) (soulrender_damage_modifier),
                    soulrender_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<SoulPyreSwordItem> SOULPYRE = ITEM.register( "soulpyre", () ->
            new SoulPyreSwordItem(ToolMaterials.NETHERITE,
                    (int) (soulpyre_damage_modifier),
                    soulpyre_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<FrostfallSwordItem> FROSTFALL = ITEM.register( "frostfall", () ->
            new FrostfallSwordItem(ToolMaterials.NETHERITE,
                    (int) (frostfall_damage_modifier),
                    frostfall_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<MoltenEdgeSwordItem> MOLTEN_EDGE = ITEM.register( "molten_edge", () ->
            new MoltenEdgeSwordItem(ToolMaterials.NETHERITE,
                    (int) (moltenedge_damage_modifier),
                    moltenedge_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<LivyatanSwordItem> LIVYATAN = ITEM.register( "livyatan", () ->
            new LivyatanSwordItem(ToolMaterials.NETHERITE,
                    (int) (livyatan_damage_modifier),
                    livyatan_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<IcewhisperSwordItem> ICEWHISPER = ITEM.register( "icewhisper", () ->
            new IcewhisperSwordItem(ToolMaterials.NETHERITE,
                    (int) (icewhisper_damage_modifier),
                    icewhisper_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));
    public static final RegistrySupplier<ArcanethystSwordItem> ARCANETHYST = ITEM.register( "arcanethyst", () ->
            new ArcanethystSwordItem(ToolMaterials.NETHERITE,
                    (int) (icewhisper_damage_modifier),
                    icewhisper_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));
    /*
    public static final RegistrySupplier<RendSwordItem> THE_DISPATCHER = ITEM.register( "the_dispatcher", () ->
            new RendSwordItem(ToolMaterials.NETHERITE,
                    (int) (thedispatcher_damage_modifier),
                    thedispatcher_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

     */

    public static final RegistrySupplier<StormSwordItem> MJOLNIR = ITEM.register( "mjolnir", () ->
            new StormSwordItem(ToolMaterials.NETHERITE,
                    (int) (mjolnir_damage_modifier),
                    mjolnir_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

}
