package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Rarity;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.*;
import net.sweenus.simplyswords.item.custom.*;

public class ItemsRegistry {

    static float iron_modifier = SimplySwords.weaponAttributesConfig.iron_damageModifier;
    static float gold_modifier = SimplySwords.weaponAttributesConfig.gold_damageModifier;
    static float diamond_modifier = SimplySwords.weaponAttributesConfig.diamond_damageModifier;
    static float netherite_modifier = SimplySwords.weaponAttributesConfig.netherite_damageModifier;
    static float runic_modifier = SimplySwords.weaponAttributesConfig.runic_damageModifier;


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

    static float brimstone_attackspeed = SimplySwords.weaponAttributesConfig.brimstone_attackSpeed;
    static float thewatcher_attackspeed = SimplySwords.weaponAttributesConfig.thewatcher_attackSpeed;
    static float stormsedge_attackspeed = SimplySwords.weaponAttributesConfig.stormsedge_attackSpeed;
    static float stormbringer_attackspeed = SimplySwords.weaponAttributesConfig.stormbringer_attackSpeed;
    static float swordonastick_attackspeed = SimplySwords.weaponAttributesConfig.swordonastick_attackSpeed;
    static float bramblethorn_attackspeed = SimplySwords.weaponAttributesConfig.bramblethorn_attackSpeed;
    static float watchingwarglaive_attackspeed = SimplySwords.weaponAttributesConfig.watchingwarglaive_attackSpeed;
    static float longswordofplague_attackspeed = SimplySwords.weaponAttributesConfig.longswordofplague_attackSpeed;
    static float emberblade_attackspeed = SimplySwords.weaponAttributesConfig.emberblade_attackSpeed;
    static float hearthflame_attackspeed = SimplySwords.weaponAttributesConfig.hearthflame_attackSpeed;
    static float soulkeeper_attackspeed = SimplySwords.weaponAttributesConfig.soulkeeper_attackSpeed;
    static float twistedblade_attackspeed = SimplySwords.weaponAttributesConfig.twistedblade_attackSpeed;
    static float soulstealer_attackspeed = SimplySwords.weaponAttributesConfig.soulstealer_attackSpeed;
    static float soulrender_attackspeed = SimplySwords.weaponAttributesConfig.soulrender_attackSpeed;
    static float mjolnir_attackspeed = SimplySwords.weaponAttributesConfig.mjolnir_attackSpeed;
    static float soulpyre_attackspeed = SimplySwords.weaponAttributesConfig.soulpyre_attackSpeed;
    static float frostfall_attackspeed = SimplySwords.weaponAttributesConfig.frostfall_attackSpeed;
    static float moltenedge_attackspeed = SimplySwords.weaponAttributesConfig.moltenedge_attackSpeed;
    static float livyatan_attackspeed = SimplySwords.weaponAttributesConfig.livyatan_attackSpeed;
    static float icewhisper_attackspeed = SimplySwords.weaponAttributesConfig.icewhisper_attackSpeed;
    static float arcanethyst_attackspeed = SimplySwords.weaponAttributesConfig.arcanethyst_attackSpeed;
    static float thunderbrand_attackspeed = SimplySwords.weaponAttributesConfig.thunderbrand_attackSpeed;
    static float lichblade_attackspeed = SimplySwords.weaponAttributesConfig.lichblade_attackSpeed;
    static float shadowsting_attackspeed = SimplySwords.weaponAttributesConfig.shadowsting_attackSpeed;
    static float sunfire_attackspeed = SimplySwords.weaponAttributesConfig.sunfire_attackSpeed;
    static float harbinger_attackspeed = SimplySwords.weaponAttributesConfig.harbinger_attackSpeed;
    static float whisperwind_attackspeed = SimplySwords.weaponAttributesConfig.whisperwind_attackSpeed;



    static float brimstone_damage_modifier = SimplySwords.weaponAttributesConfig.brimstone_damageModifier;
    static float thewatcher_damage_modifier = SimplySwords.weaponAttributesConfig.thewatcher_damageModifier;
    static float stormsedge_damage_modifier = SimplySwords.weaponAttributesConfig.stormsedge_damageModifier;
    static float stormbringer_damage_modifier = SimplySwords.weaponAttributesConfig.stormbringer_damageModifier;
    static float swordonastick_damage_modifier = SimplySwords.weaponAttributesConfig.swordonastick_damageModifier;
    static float bramblethorn_damage_modifier = SimplySwords.weaponAttributesConfig.bramblethorn_damageModifier;
    static float watchingwarglaive_damage_modifier = SimplySwords.weaponAttributesConfig.watchingwarglaive_damageModifier;
    static float longswordofplague_damage_modifier = SimplySwords.weaponAttributesConfig.longswordofplague_damageModifier;
    static float emberblade_damage_modifier = SimplySwords.weaponAttributesConfig.emberblade_damageModifier;
    static float hearthflame_damage_modifier = SimplySwords.weaponAttributesConfig.hearthflame_damageModifier;
    static float soulkeeper_damage_modifier = SimplySwords.weaponAttributesConfig.soulkeeper_damageModifier;
    static float twistedblade_damage_modifier = SimplySwords.weaponAttributesConfig.twistedblade_damageModifier;
    static float soulstealer_damage_modifier = SimplySwords.weaponAttributesConfig.soulstealer_damageModifier;
    static float soulrender_damage_modifier = SimplySwords.weaponAttributesConfig.soulrender_damageModifier;
    static float mjolnir_damage_modifier = SimplySwords.weaponAttributesConfig.mjolnir_damageModifier;
    static float soulpyre_damage_modifier = SimplySwords.weaponAttributesConfig.soulpyre_damageModifier;
    static float frostfall_damage_modifier = SimplySwords.weaponAttributesConfig.frostfall_damageModifier;
    static float moltenedge_damage_modifier = SimplySwords.weaponAttributesConfig.moltenedge_damageModifier;
    static float livyatan_damage_modifier = SimplySwords.weaponAttributesConfig.livyatan_damageModifier;
    static float icewhisper_damage_modifier = SimplySwords.weaponAttributesConfig.icewhisper_damageModifier;
    static float arcanethyst_damage_modifier = SimplySwords.weaponAttributesConfig.arcanethyst_damageModifier;
    static float thunderbrand_damage_modifier = SimplySwords.weaponAttributesConfig.thunderbrand_damageModifier;
    static float lichblade_damage_modifier = SimplySwords.weaponAttributesConfig.lichblade_damageModifier;
    static float shadowsting_damage_modifier = SimplySwords.weaponAttributesConfig.shadowsting_damageModifier;
    static float sunfire_damage_modifier = SimplySwords.weaponAttributesConfig.sunfire_damageModifier;
    static float harbinger_damage_modifier = SimplySwords.weaponAttributesConfig.harbinger_damageModifier;
    static float whisperwind_damage_modifier = SimplySwords.weaponAttributesConfig.whisperwind_damageModifier;


    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.ITEM);


    public static final RegistrySupplier<RunicTabletItem> RUNIC_TABLET = ITEM.register("runic_tablet", RunicTabletItem::new);
    public static final RegistrySupplier<RunefusedGemItem> RUNEFUSED_GEM = ITEM.register("runefused_gem", RunefusedGemItem::new);
    public static final RegistrySupplier<NetherfusedGemItem> NETHERFUSED_GEM = ITEM.register("netherfused_gem", NetherfusedGemItem::new);
    public static final RegistrySupplier<EmpoweredRemnantItem> EMPOWERED_REMNANT = ITEM.register("empowered_remnant", EmpoweredRemnantItem::new);

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

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_HALBERD = ITEM.register( "iron_halberd", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + halberd_positive_modifier - halberd_negative_modifier),
                    halberd_attackspeed,
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

    public static final RegistrySupplier<SimplySwordsSwordItem> GOLD_HALBERD = ITEM.register( "gold_halberd", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.GOLD,
                    (int) (gold_modifier + halberd_positive_modifier - halberd_negative_modifier),
                    halberd_attackspeed,
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

    public static final RegistrySupplier<SimplySwordsSwordItem> DIAMOND_HALBERD = ITEM.register( "diamond_halberd", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.DIAMOND,
                    (int) (diamond_modifier + halberd_positive_modifier - halberd_negative_modifier),
                    halberd_attackspeed,
                    "minecraft:diamond_ingot"));

    //NETHERITE

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_LONGSWORD = ITEM.register( "netherite_longsword", () ->
            new SimplySwordsNetheriteSwordItem(ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_TWINBLADE = ITEM.register( "netherite_twinblade", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_RAPIER = ITEM.register( "netherite_rapier", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (iron_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    "minecraft:netherite_ingot"));
    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_KATANA = ITEM.register( "netherite_katana", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    "minecraft:netherite_ingot"));
    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_SAI = ITEM.register( "netherite_sai", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_SPEAR = ITEM.register( "netherite_spear", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_GLAIVE = ITEM.register( "netherite_glaive", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_WARGLAIVE = ITEM.register( "netherite_warglaive", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_CUTLASS = ITEM.register( "netherite_cutlass", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_CLAYMORE = ITEM.register( "netherite_claymore", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_GREATHAMMER = ITEM.register( "netherite_greathammer", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_GREATAXE = ITEM.register( "netherite_greataxe", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_CHAKRAM = ITEM.register( "netherite_chakram", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_SCYTHE = ITEM.register( "netherite_scythe", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    "minecraft:netherite_ingot"));

    public static final RegistrySupplier<SimplySwordsNetheriteSwordItem> NETHERITE_HALBERD = ITEM.register( "netherite_halberd", () ->
            new SimplySwordsNetheriteSwordItem(
                    ToolMaterials.NETHERITE,
                    (int) (netherite_modifier + halberd_positive_modifier - halberd_negative_modifier),
                    halberd_attackspeed,
                    "minecraft:netherite_ingot"));


    //RUNIC
    public static final RegistrySupplier<RunicSwordItem> RUNIC_LONGSWORD = ITEM.register( "runic_longsword", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_TWINBLADE = ITEM.register( "runic_twinblade", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + twinblade_positive_modifier - twinblade_negative_modifier),
                    twinblade_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_RAPIER = ITEM.register( "runic_rapier", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + rapier_positive_modifier - rapier_negative_modifier),
                    rapier_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_KATANA = ITEM.register( "runic_katana", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + katana_positive_modifier - katana_negative_modifier),
                    katana_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_SAI = ITEM.register( "runic_sai", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + sai_positive_modifier - sai_negative_modifier),
                    sai_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_SPEAR = ITEM.register( "runic_spear", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + spear_positive_modifier - spear_negative_modifier),
                    spear_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_GLAIVE = ITEM.register( "runic_glaive", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + glaive_positive_modifier - glaive_negative_modifier),
                    glaive_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_CUTLASS = ITEM.register( "runic_cutlass", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + cutlass_positive_modifier - cutlass_negative_modifier),
                    cutlass_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));
    public static final RegistrySupplier<RunicSwordItem> RUNIC_CLAYMORE = ITEM.register( "runic_claymore", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + claymore_positive_modifier - claymore_negative_modifier),
                    claymore_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_CHAKRAM = ITEM.register( "runic_chakram", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + chakram_positive_modifier - chakram_negative_modifier),
                    chakram_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_GREATAXE = ITEM.register( "runic_greataxe", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + greataxe_positive_modifier - greataxe_negative_modifier),
                    greataxe_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_GREATHAMMER = ITEM.register( "runic_greathammer", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + greathammer_positive_modifier - greathammer_negative_modifier),
                    greathammer_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));
    public static final RegistrySupplier<RunicSwordItem> RUNIC_WARGLAIVE = ITEM.register( "runic_warglaive", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + warglaive_positive_modifier - warglaive_negative_modifier),
                    warglaive_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_SCYTHE = ITEM.register( "runic_scythe", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + scythe_positive_modifier - scythe_negative_modifier),
                    scythe_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    public static final RegistrySupplier<RunicSwordItem> RUNIC_HALBERD = ITEM.register( "runic_halberd", () ->
            new RunicSwordItem(ModToolMaterial.RUNIC,
                    (int) (runic_modifier + halberd_positive_modifier - halberd_negative_modifier),
                    halberd_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.RARE)));

    //SPECIAL
    public static final RegistrySupplier<FireSwordItem> BRIMSTONE_CLAYMORE = ITEM.register( "brimstone_claymore", () ->
            new FireSwordItem(ModToolMaterial.UNIQUE,
                    (int) (brimstone_damage_modifier),
                    brimstone_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<WatcherSwordItem> WATCHER_CLAYMORE = ITEM.register( "watcher_claymore", () ->
            new WatcherSwordItem(ModToolMaterial.UNIQUE,
                    (int) (thewatcher_damage_modifier),
                    thewatcher_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<StormsEdgeSwordItem> STORMS_EDGE = ITEM.register( "storms_edge", () ->
            new StormsEdgeSwordItem(ModToolMaterial.UNIQUE,
                    (int) (stormsedge_damage_modifier),
                    stormsedge_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<StormbringerSwordItem> STORMBRINGER = ITEM.register( "stormbringer", () ->
            new StormbringerSwordItem(ModToolMaterial.UNIQUE,
                    (int) (stormbringer_damage_modifier),
                    stormbringer_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<SwordItem> SWORD_ON_A_STICK = ITEM.register( "sword_on_a_stick", () ->
            new SwordItem(ToolMaterials.WOOD,
                    (int) (swordonastick_damage_modifier),
                    swordonastick_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<BrambleSwordItem> BRAMBLETHORN = ITEM.register( "bramblethorn", () ->
            new BrambleSwordItem(ToolMaterials.WOOD,
                    (int) (bramblethorn_damage_modifier),
                    bramblethorn_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<WatcherSwordItem> WATCHING_WARGLAIVE = ITEM.register( "watching_warglaive", () ->
            new WatcherSwordItem(ModToolMaterial.UNIQUE,
                    (int) (watchingwarglaive_damage_modifier),
                    watchingwarglaive_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));
    public static final RegistrySupplier<PlagueSwordItem> TOXIC_LONGSWORD = ITEM.register( "toxic_longsword", () ->
            new PlagueSwordItem(ModToolMaterial.UNIQUE,
                    (int) (longswordofplague_damage_modifier),
                    longswordofplague_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<EmberIreSwordItem> EMBERBLADE = ITEM.register( "emberblade", () ->
            new EmberIreSwordItem(ModToolMaterial.UNIQUE,
                    (int) (emberblade_damage_modifier),
                    emberblade_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<VolcanicFurySwordItem> HEARTHFLAME = ITEM.register( "hearthflame", () ->
            new VolcanicFurySwordItem(ModToolMaterial.UNIQUE,
                    (int) (hearthflame_damage_modifier),
                    hearthflame_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<SoulSwordItem> SOULKEEPER = ITEM.register( "soulkeeper", () ->
            new SoulSwordItem(ModToolMaterial.UNIQUE,
                    (int) (soulkeeper_damage_modifier),
                    soulkeeper_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<HasteSwordItem> TWISTED_BLADE = ITEM.register( "twisted_blade", () ->
            new HasteSwordItem(ModToolMaterial.UNIQUE,
                    (int) (twistedblade_damage_modifier),
                    twistedblade_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<StealSwordItem> SOULSTEALER = ITEM.register("soulstealer", () ->
            new StealSwordItem(ModToolMaterial.UNIQUE,
                    (int) (soulstealer_damage_modifier),
                    soulstealer_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<RendSwordItem> SOULRENDER = ITEM.register( "soulrender", () ->
            new RendSwordItem(ModToolMaterial.UNIQUE,
                    (int) (soulrender_damage_modifier),
                    soulrender_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<SoulPyreSwordItem> SOULPYRE = ITEM.register( "soulpyre", () ->
            new SoulPyreSwordItem(ModToolMaterial.UNIQUE,
                    (int) (soulpyre_damage_modifier),
                    soulpyre_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<FrostfallSwordItem> FROSTFALL = ITEM.register( "frostfall", () ->
            new FrostfallSwordItem(ModToolMaterial.UNIQUE,
                    (int) (frostfall_damage_modifier),
                    frostfall_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<MoltenEdgeSwordItem> MOLTEN_EDGE = ITEM.register( "molten_edge", () ->
            new MoltenEdgeSwordItem(ModToolMaterial.UNIQUE,
                    (int) (moltenedge_damage_modifier),
                    moltenedge_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<LivyatanSwordItem> LIVYATAN = ITEM.register( "livyatan", () ->
            new LivyatanSwordItem(ModToolMaterial.UNIQUE,
                    (int) (livyatan_damage_modifier),
                    livyatan_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<IcewhisperSwordItem> ICEWHISPER = ITEM.register( "icewhisper", () ->
            new IcewhisperSwordItem(ModToolMaterial.UNIQUE,
                    (int) (icewhisper_damage_modifier),
                    icewhisper_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));
    public static final RegistrySupplier<ArcanethystSwordItem> ARCANETHYST = ITEM.register( "arcanethyst", () ->
            new ArcanethystSwordItem(ModToolMaterial.UNIQUE,
                    (int) (arcanethyst_damage_modifier),
                    arcanethyst_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<ThunderbrandSwordItem> THUNDERBRAND = ITEM.register( "thunderbrand", () ->
            new ThunderbrandSwordItem(ModToolMaterial.UNIQUE,
                    (int) (thunderbrand_damage_modifier),
                    thunderbrand_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    /*
    public static final RegistrySupplier<RendSwordItem> THE_DISPATCHER = ITEM.register( "the_dispatcher", () ->
            new RendSwordItem(ModToolMaterial.UNIQUE,
                    (int) (thedispatcher_damage_modifier),
                    thedispatcher_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

     */

    public static final RegistrySupplier<StormSwordItem> MJOLNIR = ITEM.register( "mjolnir", () ->
            new StormSwordItem(ModToolMaterial.UNIQUE,
                    (int) (mjolnir_damage_modifier),
                    mjolnir_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<LichbladeSwordItem> SLUMBERING_LICHBLADE = ITEM.register( "slumbering_lichblade", () ->
            new LichbladeSwordItem(ModToolMaterial.UNIQUE,
                    (int) (lichblade_damage_modifier),
                    lichblade_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<LichbladeSwordItem> WAKING_LICHBLADE = ITEM.register( "waking_lichblade", () ->
            new LichbladeSwordItem(ModToolMaterial.UNIQUE,
                    (int) (lichblade_damage_modifier),
                    lichblade_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<LichbladeSwordItem> AWAKENED_LICHBLADE = ITEM.register( "awakened_lichblade", () ->
            new LichbladeSwordItem(ModToolMaterial.UNIQUE,
                    (int) (lichblade_damage_modifier),
                    lichblade_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<ShadowstingSwordItem> SHADOWSTING = ITEM.register( "shadowsting", () ->
            new ShadowstingSwordItem(ModToolMaterial.UNIQUE,
                    (int) (shadowsting_damage_modifier),
                    shadowsting_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<DormantRelicSwordItem> DORMANT_RELIC = ITEM.register( "dormant_relic", () ->
            new DormantRelicSwordItem(ModToolMaterial.UNIQUE,
                    (int) (sunfire_damage_modifier),
                    sunfire_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<RighteousRelicSwordItem> RIGHTEOUS_RELIC = ITEM.register( "righteous_relic", () ->
            new RighteousRelicSwordItem(ModToolMaterial.UNIQUE,
                    (int) (sunfire_damage_modifier),
                    sunfire_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<TaintedRelicSwordItem> TAINTED_RELIC = ITEM.register( "tainted_relic", () ->
            new TaintedRelicSwordItem(ModToolMaterial.UNIQUE,
                    (int) (harbinger_damage_modifier),
                    harbinger_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<SunfireSwordItem> SUNFIRE = ITEM.register( "sunfire", () ->
            new SunfireSwordItem(ModToolMaterial.UNIQUE,
                    (int) (sunfire_damage_modifier),
                    sunfire_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<HarbingerSwordItem> HARBINGER = ITEM.register( "harbinger", () ->
            new HarbingerSwordItem(ModToolMaterial.UNIQUE,
                    (int) (harbinger_damage_modifier),
                    harbinger_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

    public static final RegistrySupplier<WhisperwindSwordItem> WHISPERWIND = ITEM.register( "whisperwind", () ->
            new WhisperwindSwordItem(ModToolMaterial.UNIQUE,
                    (int) (whisperwind_damage_modifier),
                    whisperwind_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

/*
    public static final RegistrySupplier<TaintedRelicSwordItem> STORMSCALE = ITEM.register( "stormscale", () ->
            new TaintedRelicSwordItem(ModToolMaterial.UNIQUE,
                    (int) (harbinger_damage_modifier),
                    harbinger_attackspeed,
                    new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof()));

*/

}
