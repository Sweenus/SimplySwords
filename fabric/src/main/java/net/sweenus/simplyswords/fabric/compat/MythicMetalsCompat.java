package net.sweenus.simplyswords.fabric.compat;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Rarity;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.fabric.item.PrometheumSwordItem;
import net.sweenus.simplyswords.item.ModToolMaterial;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;
import nourl.mythicmetals.component.MythicDataComponents;
import nourl.mythicmetals.component.PrometheumComponent;

public class MythicMetalsCompat {

    //Compat for Mythic Metals

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


    static float adamantite_modifier = Config.weaponAttribute.materialDamageModifier.adamantite_damageModifier.get();
    static float aquarium_modifier = Config.weaponAttribute.materialDamageModifier.aquarium_damageModifier.get();
    static float banglum_modifier = Config.weaponAttribute.materialDamageModifier.banglum_damageModifier.get();
    static float carmot_modifier = Config.weaponAttribute.materialDamageModifier.carmot_damageModifier.get();
    static float kyber_modifier = Config.weaponAttribute.materialDamageModifier.kyber_damageModifier.get();
    static float mythril_modifier = Config.weaponAttribute.materialDamageModifier.mythril_damageModifier.get();
    static float orichalcum_modifier = Config.weaponAttribute.materialDamageModifier.orichalcum_damageModifier.get();
    static float durasteel_modifier = Config.weaponAttribute.materialDamageModifier.durasteel_damageModifier.get();
    static float osmium_modifier = Config.weaponAttribute.materialDamageModifier.osmium_damageModifier.get();
    static float prometheum_modifier = Config.weaponAttribute.materialDamageModifier.prometheum_damageModifier.get();
    static float quadrillum_modifier = Config.weaponAttribute.materialDamageModifier.quadrillum_damageModifier.get();
    static float runite_modifier = Config.weaponAttribute.materialDamageModifier.runite_damageModifier.get();
    static float star_platinum_modifier = Config.weaponAttribute.materialDamageModifier.starPlatinum_damageModifier.get();
    static float bronze_modifier = Config.weaponAttribute.materialDamageModifier.bronze_damageModifier.get();
    static float copper_modifier = Config.weaponAttribute.materialDamageModifier.copper_damageModifier.get();
    static float steel_modifier = Config.weaponAttribute.materialDamageModifier.steel_damageModifier.get();
    static float palladium_modifier = Config.weaponAttribute.materialDamageModifier.palladium_damageModifier.get();
    static float stormyx_modifier = Config.weaponAttribute.materialDamageModifier.stormyx_damageModifier.get();
    static float celestium_modifier = Config.weaponAttribute.materialDamageModifier.celestium_damageModifier.get();
    static float metallurgium_modifier = Config.weaponAttribute.materialDamageModifier.metallurgium_damageModifier.get();

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.ITEM);

    //ADAMANTITE

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_LONGSWORD = ITEM.register("iron_longsword", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    new Item.Settings().rarity(Rarity.COMMON)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_TWINBLADE = ITEM.register("mythicmetals_compat/adamantite/adamantite_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings().rarity(Rarity.COMMON)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_RAPIER = ITEM.register("mythicmetals_compat/adamantite/adamantite_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_KATANA = ITEM.register("mythicmetals_compat/adamantite/adamantite_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_SAI = ITEM.register("mythicmetals_compat/adamantite/adamantite_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_SPEAR = ITEM.register("mythicmetals_compat/adamantite/adamantite_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_GLAIVE = ITEM.register("mythicmetals_compat/adamantite/adamantite_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_WARGLAIVE = ITEM.register("mythicmetals_compat/adamantite/adamantite_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_CUTLASS = ITEM.register("mythicmetals_compat/adamantite/adamantite_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_CLAYMORE = ITEM.register("mythicmetals_compat/adamantite/adamantite_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_GREATHAMMER = ITEM.register("mythicmetals_compat/adamantite/adamantite_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_GREATAXE = ITEM.register("mythicmetals_compat/adamantite/adamantite_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_CHAKRAM = ITEM.register("mythicmetals_compat/adamantite/adamantite_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_SCYTHE = ITEM.register("mythicmetals_compat/adamantite/adamantite_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:adamantite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ADAMANTITE_HALBERD = ITEM.register("mythicmetals_compat/adamantite/adamantite_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ADAMANTITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ADAMANTITE,
                                    (int) (adamantite_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:adamantite_ingot"));


    //AQUARIUM
    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_LONGSWORD = ITEM.register("mythicmetals_compat/aquarium/aquarium_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_TWINBLADE = ITEM.register("mythicmetals_compat/aquarium/aquarium_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_RAPIER = ITEM.register("mythicmetals_compat/aquarium/aquarium_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_KATANA = ITEM.register("mythicmetals_compat/aquarium/aquarium_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_SAI = ITEM.register("mythicmetals_compat/aquarium/aquarium_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_SPEAR = ITEM.register("mythicmetals_compat/aquarium/aquarium_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_GLAIVE = ITEM.register("mythicmetals_compat/aquarium/aquarium_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_WARGLAIVE = ITEM.register("mythicmetals_compat/aquarium/aquarium_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_CUTLASS = ITEM.register("mythicmetals_compat/aquarium/aquarium_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_CLAYMORE = ITEM.register("mythicmetals_compat/aquarium/aquarium_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_GREATHAMMER = ITEM.register("mythicmetals_compat/aquarium/aquarium_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_GREATAXE = ITEM.register("mythicmetals_compat/aquarium/aquarium_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_CHAKRAM = ITEM.register("mythicmetals_compat/aquarium/aquarium_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_SCYTHE = ITEM.register("mythicmetals_compat/aquarium/aquarium_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:aquarium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> AQUARIUM_HALBERD = ITEM.register("mythicmetals_compat/aquarium/aquarium_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.AQUARIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.AQUARIUM,
                                    (int) (aquarium_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:aquarium_ingot"));


    //BANGLUM
    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_LONGSWORD = ITEM.register("mythicmetals_compat/banglum/banglum_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_TWINBLADE = ITEM.register("mythicmetals_compat/banglum/banglum_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_RAPIER = ITEM.register("mythicmetals_compat/banglum/banglum_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_KATANA = ITEM.register("mythicmetals_compat/banglum/banglum_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_SAI = ITEM.register("mythicmetals_compat/banglum/banglum_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_SPEAR = ITEM.register("mythicmetals_compat/banglum/banglum_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_GLAIVE = ITEM.register("mythicmetals_compat/banglum/banglum_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_WARGLAIVE = ITEM.register("mythicmetals_compat/banglum/banglum_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_CUTLASS = ITEM.register("mythicmetals_compat/banglum/banglum_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_CLAYMORE = ITEM.register("mythicmetals_compat/banglum/banglum_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_GREATHAMMER = ITEM.register("mythicmetals_compat/banglum/banglum_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_GREATAXE = ITEM.register("mythicmetals_compat/banglum/banglum_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_CHAKRAM = ITEM.register("mythicmetals_compat/banglum/banglum_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_SCYTHE = ITEM.register("mythicmetals_compat/banglum/banglum_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:banglum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BANGLUM_HALBERD = ITEM.register("mythicmetals_compat/banglum/banglum_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BANGLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BANGLUM,
                                    (int) (banglum_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:banglum_ingot"));


    //CARMOT
    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_LONGSWORD = ITEM.register("mythicmetals_compat/carmot/carmot_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_TWINBLADE = ITEM.register("mythicmetals_compat/carmot/carmot_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_RAPIER = ITEM.register("mythicmetals_compat/carmot/carmot_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_KATANA = ITEM.register("mythicmetals_compat/carmot/carmot_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_SAI = ITEM.register("mythicmetals_compat/carmot/carmot_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_SPEAR = ITEM.register("mythicmetals_compat/carmot/carmot_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_GLAIVE = ITEM.register("mythicmetals_compat/carmot/carmot_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_WARGLAIVE = ITEM.register("mythicmetals_compat/carmot/carmot_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_CUTLASS = ITEM.register("mythicmetals_compat/carmot/carmot_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_CLAYMORE = ITEM.register("mythicmetals_compat/carmot/carmot_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_GREATHAMMER = ITEM.register("mythicmetals_compat/carmot/carmot_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_GREATAXE = ITEM.register("mythicmetals_compat/carmot/carmot_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_CHAKRAM = ITEM.register("mythicmetals_compat/carmot/carmot_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_SCYTHE = ITEM.register("mythicmetals_compat/carmot/carmot_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CARMOT_HALBERD = ITEM.register("mythicmetals_compat/carmot/carmot_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CARMOT,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CARMOT,
                                    (int) (carmot_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:carmot_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_LONGSWORD = ITEM.register("mythicmetals_compat/kyber/kyber_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_TWINBLADE = ITEM.register("mythicmetals_compat/kyber/kyber_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_RAPIER = ITEM.register("mythicmetals_compat/kyber/kyber_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_KATANA = ITEM.register("mythicmetals_compat/kyber/kyber_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_SAI = ITEM.register("mythicmetals_compat/kyber/kyber_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_SPEAR = ITEM.register("mythicmetals_compat/kyber/kyber_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_GLAIVE = ITEM.register("mythicmetals_compat/kyber/kyber_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_WARGLAIVE = ITEM.register("mythicmetals_compat/kyber/kyber_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_CUTLASS = ITEM.register("mythicmetals_compat/kyber/kyber_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_CLAYMORE = ITEM.register("mythicmetals_compat/kyber/kyber_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_GREATHAMMER = ITEM.register("mythicmetals_compat/kyber/kyber_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_GREATAXE = ITEM.register("mythicmetals_compat/kyber/kyber_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_CHAKRAM = ITEM.register("mythicmetals_compat/kyber/kyber_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_SCYTHE = ITEM.register("mythicmetals_compat/kyber/kyber_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:kyber_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> KYBER_HALBERD = ITEM.register("mythicmetals_compat/kyber/kyber_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.KYBER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.KYBER,
                                    (int) (kyber_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:kyber_ingot"));


    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_LONGSWORD = ITEM.register("mythicmetals_compat/mythril/mythril_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_TWINBLADE = ITEM.register("mythicmetals_compat/mythril/mythril_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_RAPIER = ITEM.register("mythicmetals_compat/mythril/mythril_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_KATANA = ITEM.register("mythicmetals_compat/mythril/mythril_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_SAI = ITEM.register("mythicmetals_compat/mythril/mythril_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_SPEAR = ITEM.register("mythicmetals_compat/mythril/mythril_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_GLAIVE = ITEM.register("mythicmetals_compat/mythril/mythril_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_WARGLAIVE = ITEM.register("mythicmetals_compat/mythril/mythril_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_CUTLASS = ITEM.register("mythicmetals_compat/mythril/mythril_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_CLAYMORE = ITEM.register("mythicmetals_compat/mythril/mythril_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_GREATHAMMER = ITEM.register("mythicmetals_compat/mythril/mythril_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_GREATAXE = ITEM.register("mythicmetals_compat/mythril/mythril_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_CHAKRAM = ITEM.register("mythicmetals_compat/mythril/mythril_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_SCYTHE = ITEM.register("mythicmetals_compat/mythril/mythril_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:mythril_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> MYTHRIL_HALBERD = ITEM.register("mythicmetals_compat/mythril/mythril_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.MYTHRIL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.MYTHRIL,
                                    (int) (mythril_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:mythril_ingot"));


    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_LONGSWORD = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_TWINBLADE = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_RAPIER = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_KATANA = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_SAI = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_SPEAR = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_GLAIVE = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_WARGLAIVE = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_CUTLASS = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_CLAYMORE = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_GREATHAMMER = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_GREATAXE = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_CHAKRAM = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_SCYTHE = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:orichalcum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> ORICHALCUM_HALBERD = ITEM.register("mythicmetals_compat/orichalcum/orichalcum_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.ORICHALCUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.ORICHALCUM,
                                    (int) (orichalcum_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:orichalcum_ingot"));


    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_LONGSWORD = ITEM.register("mythicmetals_compat/osmium/osmium_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_TWINBLADE = ITEM.register("mythicmetals_compat/osmium/osmium_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_RAPIER = ITEM.register("mythicmetals_compat/osmium/osmium_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_KATANA = ITEM.register("mythicmetals_compat/osmium/osmium_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_SAI = ITEM.register("mythicmetals_compat/osmium/osmium_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_SPEAR = ITEM.register("mythicmetals_compat/osmium/osmium_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_GLAIVE = ITEM.register("mythicmetals_compat/osmium/osmium_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_WARGLAIVE = ITEM.register("mythicmetals_compat/osmium/osmium_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_CUTLASS = ITEM.register("mythicmetals_compat/osmium/osmium_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_CLAYMORE = ITEM.register("mythicmetals_compat/osmium/osmium_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_GREATHAMMER = ITEM.register("mythicmetals_compat/osmium/osmium_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_GREATAXE = ITEM.register("mythicmetals_compat/osmium/osmium_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_CHAKRAM = ITEM.register("mythicmetals_compat/osmium/osmium_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_SCYTHE = ITEM.register("mythicmetals_compat/osmium/osmium_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:osmium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> OSMIUM_HALBERD = ITEM.register("mythicmetals_compat/osmium/osmium_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.OSMIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.OSMIUM,
                                    (int) (osmium_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:osmium_ingot"));


    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_LONGSWORD = ITEM.register("mythicmetals_compat/prometheum/prometheum_longsword", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_TWINBLADE = ITEM.register("mythicmetals_compat/prometheum/prometheum_twinblade", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_RAPIER = ITEM.register("mythicmetals_compat/prometheum/prometheum_rapier", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_KATANA = ITEM.register("mythicmetals_compat/prometheum/prometheum_katana", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_SAI = ITEM.register("mythicmetals_compat/prometheum/prometheum_sai", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_SPEAR = ITEM.register("mythicmetals_compat/prometheum/prometheum_spear", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_GLAIVE = ITEM.register("mythicmetals_compat/prometheum/prometheum_glaive", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_WARGLAIVE = ITEM.register("mythicmetals_compat/prometheum/prometheum_warglaive", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_CUTLASS = ITEM.register("mythicmetals_compat/prometheum/prometheum_cutlass", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_CLAYMORE = ITEM.register("mythicmetals_compat/prometheum/prometheum_claymore", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_GREATHAMMER = ITEM.register("mythicmetals_compat/prometheum/prometheum_greathammer", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_GREATAXE = ITEM.register("mythicmetals_compat/prometheum/prometheum_greataxe", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_CHAKRAM = ITEM.register("mythicmetals_compat/prometheum/prometheum_chakram", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_SCYTHE = ITEM.register("mythicmetals_compat/prometheum/prometheum_scythe", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:prometheum_ingot"));

    public static final RegistrySupplier<PrometheumSwordItem> PROMETHEUM_HALBERD = ITEM.register("mythicmetals_compat/prometheum/prometheum_halberd", () ->
            new PrometheumSwordItem(
                    ModToolMaterial.PROMETHEUM,
                    new Item.Settings()
                            .component(MythicDataComponents.PROMETHEUM, PrometheumComponent.DEFAULT)
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PROMETHEUM,
                                    (int) (prometheum_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:prometheum_ingot"));


    //QUADRILLUM
    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_LONGSWORD = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_TWINBLADE = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_RAPIER = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_KATANA = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_SAI = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_SPEAR = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_GLAIVE = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_WARGLAIVE = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_CUTLASS = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_CLAYMORE = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_GREATHAMMER = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_GREATAXE = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_CHAKRAM = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_SCYTHE = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:quadrillum_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> QUADRILLUM_HALBERD = ITEM.register("mythicmetals_compat/quadrillum/quadrillum_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.QUADRILLUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.QUADRILLUM,
                                    (int) (quadrillum_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:quadrillum_ingot"));


    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_LONGSWORD = ITEM.register("mythicmetals_compat/runite/runite_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_TWINBLADE = ITEM.register("mythicmetals_compat/runite/runite_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_RAPIER = ITEM.register("mythicmetals_compat/runite/runite_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_KATANA = ITEM.register("mythicmetals_compat/runite/runite_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_SAI = ITEM.register("mythicmetals_compat/runite/runite_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_SPEAR = ITEM.register("mythicmetals_compat/runite/runite_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_GLAIVE = ITEM.register("mythicmetals_compat/runite/runite_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_WARGLAIVE = ITEM.register("mythicmetals_compat/runite/runite_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_CUTLASS = ITEM.register("mythicmetals_compat/runite/runite_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_CLAYMORE = ITEM.register("mythicmetals_compat/runite/runite_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_GREATHAMMER = ITEM.register("mythicmetals_compat/runite/runite_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_GREATAXE = ITEM.register("mythicmetals_compat/runite/runite_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_CHAKRAM = ITEM.register("mythicmetals_compat/runite/runite_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_SCYTHE = ITEM.register("mythicmetals_compat/runite/runite_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:runite_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> RUNITE_HALBERD = ITEM.register("mythicmetals_compat/runite/runite_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.RUNITE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.RUNITE,
                                    (int) (runite_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:runite_ingot"));


    //STAR_PLATINUM
    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_LONGSWORD = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_TWINBLADE = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_RAPIER = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_KATANA = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_SAI = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_SPEAR = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_GLAIVE = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_WARGLAIVE = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_CUTLASS = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_CLAYMORE = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_GREATHAMMER = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_GREATAXE = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_CHAKRAM = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_SCYTHE = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:star_platinum"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STAR_PLATINUM_HALBERD = ITEM.register("mythicmetals_compat/star_platinum/star_platinum_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STAR_PLATINUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STAR_PLATINUM,
                                    (int) (star_platinum_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:star_platinum"));


    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_LONGSWORD = ITEM.register("mythicmetals_compat/bronze/bronze_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_TWINBLADE = ITEM.register("mythicmetals_compat/bronze/bronze_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_RAPIER = ITEM.register("mythicmetals_compat/bronze/bronze_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_KATANA = ITEM.register("mythicmetals_compat/bronze/bronze_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_SAI = ITEM.register("mythicmetals_compat/bronze/bronze_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_SPEAR = ITEM.register("mythicmetals_compat/bronze/bronze_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_GLAIVE = ITEM.register("mythicmetals_compat/bronze/bronze_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_WARGLAIVE = ITEM.register("mythicmetals_compat/bronze/bronze_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_CUTLASS = ITEM.register("mythicmetals_compat/bronze/bronze_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_CLAYMORE = ITEM.register("mythicmetals_compat/bronze/bronze_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_GREATHAMMER = ITEM.register("mythicmetals_compat/bronze/bronze_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_GREATAXE = ITEM.register("mythicmetals_compat/bronze/bronze_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_CHAKRAM = ITEM.register("mythicmetals_compat/bronze/bronze_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_SCYTHE = ITEM.register("mythicmetals_compat/bronze/bronze_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:bronze_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> BRONZE_HALBERD = ITEM.register("mythicmetals_compat/bronze/bronze_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.BRONZE,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.BRONZE,
                                    (int) (bronze_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:bronze_ingot"));


    //STEEL
    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_LONGSWORD = ITEM.register("mythicmetals_compat/steel/steel_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_TWINBLADE = ITEM.register("mythicmetals_compat/steel/steel_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_RAPIER = ITEM.register("mythicmetals_compat/steel/steel_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_KATANA = ITEM.register("mythicmetals_compat/steel/steel_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_SAI = ITEM.register("mythicmetals_compat/steel/steel_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_SPEAR = ITEM.register("mythicmetals_compat/steel/steel_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_GLAIVE = ITEM.register("mythicmetals_compat/steel/steel_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_WARGLAIVE = ITEM.register("mythicmetals_compat/steel/steel_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_CUTLASS = ITEM.register("mythicmetals_compat/steel/steel_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_CLAYMORE = ITEM.register("mythicmetals_compat/steel/steel_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_GREATHAMMER = ITEM.register("mythicmetals_compat/steel/steel_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_GREATAXE = ITEM.register("mythicmetals_compat/steel/steel_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_CHAKRAM = ITEM.register("mythicmetals_compat/steel/steel_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_SCYTHE = ITEM.register("mythicmetals_compat/steel/steel_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:steel_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STEEL_HALBERD = ITEM.register("mythicmetals_compat/steel/steel_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STEEL,
                                    (int) (steel_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:steel_ingot"));


    //STORMYX
    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_LONGSWORD = ITEM.register("mythicmetals_compat/stormyx/stormyx_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_TWINBLADE = ITEM.register("mythicmetals_compat/stormyx/stormyx_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_RAPIER = ITEM.register("mythicmetals_compat/stormyx/stormyx_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_KATANA = ITEM.register("mythicmetals_compat/stormyx/stormyx_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_SAI = ITEM.register("mythicmetals_compat/stormyx/stormyx_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_SPEAR = ITEM.register("mythicmetals_compat/stormyx/stormyx_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_GLAIVE = ITEM.register("mythicmetals_compat/stormyx/stormyx_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_WARGLAIVE = ITEM.register("mythicmetals_compat/stormyx/stormyx_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_CUTLASS = ITEM.register("mythicmetals_compat/stormyx/stormyx_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_CLAYMORE = ITEM.register("mythicmetals_compat/stormyx/stormyx_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_GREATHAMMER = ITEM.register("mythicmetals_compat/stormyx/stormyx_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_GREATAXE = ITEM.register("mythicmetals_compat/stormyx/stormyx_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_CHAKRAM = ITEM.register("mythicmetals_compat/stormyx/stormyx_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_SCYTHE = ITEM.register("mythicmetals_compat/stormyx/stormyx_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:stormyx_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> STORMYX_HALBERD = ITEM.register("mythicmetals_compat/stormyx/stormyx_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.STORMYX,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.STORMYX,
                                    (int) (stormyx_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:stormyx_ingot"));


    //PALLADIUM
    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_LONGSWORD = ITEM.register("mythicmetals_compat/palladium/palladium_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_TWINBLADE = ITEM.register("mythicmetals_compat/palladium/palladium_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_RAPIER = ITEM.register("mythicmetals_compat/palladium/palladium_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_KATANA = ITEM.register("mythicmetals_compat/palladium/palladium_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_SAI = ITEM.register("mythicmetals_compat/palladium/palladium_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_SPEAR = ITEM.register("mythicmetals_compat/palladium/palladium_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_GLAIVE = ITEM.register("mythicmetals_compat/palladium/palladium_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_WARGLAIVE = ITEM.register("mythicmetals_compat/palladium/palladium_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_CUTLASS = ITEM.register("mythicmetals_compat/palladium/palladium_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_CLAYMORE = ITEM.register("mythicmetals_compat/palladium/palladium_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_GREATHAMMER = ITEM.register("mythicmetals_compat/palladium/palladium_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_GREATAXE = ITEM.register("mythicmetals_compat/palladium/palladium_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_CHAKRAM = ITEM.register("mythicmetals_compat/palladium/palladium_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_SCYTHE = ITEM.register("mythicmetals_compat/palladium/palladium_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:palladium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> PALLADIUM_HALBERD = ITEM.register("mythicmetals_compat/palladium/palladium_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.PALLADIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.PALLADIUM,
                                    (int) (palladium_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:palladium_ingot"));


    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_LONGSWORD = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_TWINBLADE = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_RAPIER = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_KATANA = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_SAI = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_SPEAR = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_GLAIVE = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_WARGLAIVE = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_CUTLASS = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_CLAYMORE = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_GREATHAMMER = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_GREATAXE = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_CHAKRAM = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_SCYTHE = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:metallurgium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> METALLURGIUM_HALBERD = ITEM.register("mythicmetals_compat/metallurgium/metallurgium_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.METALLURGIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.METALLURGIUM,
                                    (int) (metallurgium_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:metallurgium_ingot"));


    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_LONGSWORD = ITEM.register("mythicmetals_compat/celestium/celestium_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_TWINBLADE = ITEM.register("mythicmetals_compat/celestium/celestium_twinblade", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + twinblade_modifier), twinblade_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_RAPIER = ITEM.register("mythicmetals_compat/celestium/celestium_rapier", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + rapier_modifier), rapier_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_KATANA = ITEM.register("mythicmetals_compat/celestium/celestium_katana", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + katana_modifier), katana_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_SAI = ITEM.register("mythicmetals_compat/celestium/celestium_sai", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + sai_modifier), sai_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_SPEAR = ITEM.register("mythicmetals_compat/celestium/celestium_spear", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + spear_modifier), spear_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_GLAIVE = ITEM.register("mythicmetals_compat/celestium/celestium_glaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + glaive_modifier), glaive_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_WARGLAIVE = ITEM.register("mythicmetals_compat/celestium/celestium_warglaive", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + warglaive_modifier), warglaive_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_CUTLASS = ITEM.register("mythicmetals_compat/celestium/celestium_cutlass", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + cutlass_modifier), cutlass_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_CLAYMORE = ITEM.register("mythicmetals_compat/celestium/celestium_claymore", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + claymore_modifier), claymore_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_GREATHAMMER = ITEM.register("mythicmetals_compat/celestium/celestium_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_GREATAXE = ITEM.register("mythicmetals_compat/celestium/celestium_greataxe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + greataxe_modifier), greataxe_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_CHAKRAM = ITEM.register("mythicmetals_compat/celestium/celestium_chakram", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + chakram_modifier), chakram_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_SCYTHE = ITEM.register("mythicmetals_compat/celestium/celestium_scythe", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + scythe_modifier), scythe_attackspeed)), "mythicmetals:celestium_ingot"));

    public static final RegistrySupplier<SimplySwordsSwordItem> CELESTIUM_HALBERD = ITEM.register("mythicmetals_compat/celestium/celestium_halberd", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.CELESTIUM,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.CELESTIUM,
                                    (int) (celestium_modifier + halberd_modifier), halberd_attackspeed)), "mythicmetals:celestium_ingot"));

    // COPPER
    public static final RegistrySupplier<SimplySwordsSwordItem> COPPER_LONGSWORD = ITEM.register("mythicmetals_compat/copper/copper_longsword", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.COPPER,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.COPPER,
                                    (int) (copper_modifier + longsword_modifier), longsword_attackspeed)), "mythicmetals:copper_ingot"));

    // DURASTEEL
    public static final RegistrySupplier<SimplySwordsSwordItem> DURASTEEL_GREATHAMMER = ITEM.register("mythicmetals_compat/durasteel/durasteel_greathammer", () ->
            new SimplySwordsSwordItem(
                    ModToolMaterial.DURASTEEL,
                    new Item.Settings()
                            .attributeModifiers(SwordItem.createAttributeModifiers(ModToolMaterial.DURASTEEL,
                                    (int) (durasteel_modifier + greathammer_modifier), greathammer_attackspeed)), "mythicmetals:durasteel_ingot"));




    public static void assignTab() {
        registerModItems();
        var simplySwordsKey = ((RegistryEntry<ItemGroup>) SimplySwords.SIMPLYSWORDS).getKey();
        if (simplySwordsKey == null) {
            throw new IllegalStateException("SimplySwords key is null!");
        }

        ItemGroupEvents.modifyEntriesEvent(simplySwordsKey.get()).register(entries -> {
            if (ITEM != null) {
                ITEM.forEach(item -> {
                    var resolvedItem = item.get();
                    if (resolvedItem != null) {
                        entries.add(resolvedItem);
                    } else {
                        System.err.println("Warning: Null item encountered in ITEM collection!");
                    }
                });
            } else {
                System.err.println("Warning: ITEM collection is null.");
            }
        });
    }


    public static void registerModItems() {
        SimplySwords.LOGGER.info("Registering Mythic Metals compat Items for " + SimplySwords.MOD_ID);
    }
}