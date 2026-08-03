package net.sweenus.simplyswords.compat;

import nourl.mythicmetals.item.tools.MythicTools;
import nourl.mythicmetals.item.tools.ToolSet;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.mythicmetals.PalladiumSwordItem;
import net.sweenus.simplyswords.compat.mythicmetals.PrometheumSwordItem;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.LegacyWeaponAttributes;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
    static float bronze_modifier = Config.weaponAttribute.materialDamageModifier.bronze_damageModifier.get();
    static float carmot_modifier = Config.weaponAttribute.materialDamageModifier.carmot_damageModifier.get();
    static float celestium_modifier = Config.weaponAttribute.materialDamageModifier.celestium_damageModifier.get();
    static float copper_modifier = Config.weaponAttribute.materialDamageModifier.copper_damageModifier.get();
    static float durasteel_modifier = Config.weaponAttribute.materialDamageModifier.durasteel_damageModifier.get();
    static float kyber_modifier = Config.weaponAttribute.materialDamageModifier.kyber_damageModifier.get();
    static float metallurgium_modifier = Config.weaponAttribute.materialDamageModifier.metallurgium_damageModifier.get();
    static float mythril_modifier = Config.weaponAttribute.materialDamageModifier.mythril_damageModifier.get();
    static float orichalcum_modifier = Config.weaponAttribute.materialDamageModifier.orichalcum_damageModifier.get();
    static float osmium_modifier = Config.weaponAttribute.materialDamageModifier.osmium_damageModifier.get();
    static float palladium_modifier = Config.weaponAttribute.materialDamageModifier.palladium_damageModifier.get();
    static float prometheum_modifier = Config.weaponAttribute.materialDamageModifier.prometheum_damageModifier.get();
    static float quadrillum_modifier = Config.weaponAttribute.materialDamageModifier.quadrillum_damageModifier.get();
    static float runite_modifier = Config.weaponAttribute.materialDamageModifier.runite_damageModifier.get();
    static float star_platinum_modifier = Config.weaponAttribute.materialDamageModifier.starPlatinum_damageModifier.get();
    static float steel_modifier = Config.weaponAttribute.materialDamageModifier.steel_damageModifier.get();
    static float stormyx_modifier = Config.weaponAttribute.materialDamageModifier.stormyx_damageModifier.get();

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.ITEM);

    private static void registerItems(Map<String, Float> supportedItems) {
        MythicTools.TOOL_MAP.forEach((name, toolSet) -> {
            if (supportedItems.containsKey(name.toLowerCase(Locale.ROOT))) {
                registerSwords(toolSet, name, supportedItems.get(name));
            }
        });
    }

    private static void registerSwords(ToolSet toolSet, String name, float modifier) {
        final String normalizedName = name.toLowerCase(Locale.ROOT);
        var weaponPath = "mythicmetals_compat/" + normalizedName + "/" + normalizedName + "_";
        var material = toolSet.getSword().getMaterial();
        var repairStacks = material.getRepairIngredient().getMatchingStacks();
        // Edge case to handle Prometheum Auto Repair items
        var settings = defaultSettings();
        // Handle Palladium separately
        if (normalizedName.equals("palladium")) {
            registerPalladiumTools(material, weaponPath, modifier);
            return;
        }

        ITEM.register(weaponPath + "longsword", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + longsword_modifier), longsword_attackspeed)));
        ITEM.register(weaponPath + "twinblade", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + twinblade_modifier), twinblade_attackspeed)));
        ITEM.register(weaponPath + "rapier", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + rapier_modifier), rapier_attackspeed)));
        ITEM.register(weaponPath + "katana", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + katana_modifier), katana_attackspeed)));
        ITEM.register(weaponPath + "sai", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + sai_modifier), sai_attackspeed)));
        ITEM.register(weaponPath + "spear", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + spear_modifier), spear_attackspeed)));
        ITEM.register(weaponPath + "glaive", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + glaive_modifier), glaive_attackspeed)));
        ITEM.register(weaponPath + "warglaive", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + warglaive_modifier), warglaive_attackspeed)));
        ITEM.register(weaponPath + "cutlass", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + cutlass_modifier), cutlass_attackspeed)));
        ITEM.register(weaponPath + "claymore", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + claymore_modifier), claymore_attackspeed)));
        ITEM.register(weaponPath + "greathammer", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + greathammer_modifier), greathammer_attackspeed)));
        ITEM.register(weaponPath + "greataxe", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + greataxe_modifier), greataxe_attackspeed)));
        ITEM.register(weaponPath + "chakram", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + chakram_modifier), chakram_attackspeed)));
        ITEM.register(weaponPath + "scythe", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + scythe_modifier), scythe_attackspeed)));
        ITEM.register(weaponPath + "halberd", () -> createSword(normalizedName, material, LegacyWeaponAttributes.configure(settings, (int) (modifier + halberd_modifier), halberd_attackspeed)));
    }

    private static void registerPalladiumTools(ToolMaterial material, String weaponPath, float modifier) {
        ITEM.register(weaponPath + "longsword", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + longsword_modifier), longsword_attackspeed)));
        ITEM.register(weaponPath + "twinblade", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + twinblade_modifier), twinblade_attackspeed)));
        ITEM.register(weaponPath + "rapier", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + rapier_modifier), rapier_attackspeed)));
        ITEM.register(weaponPath + "katana", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + katana_modifier), katana_attackspeed)));
        ITEM.register(weaponPath + "sai", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + sai_modifier), sai_attackspeed)));
        ITEM.register(weaponPath + "spear", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + spear_modifier), spear_attackspeed)));
        ITEM.register(weaponPath + "glaive", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + glaive_modifier), glaive_attackspeed)));
        ITEM.register(weaponPath + "warglaive", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + warglaive_modifier), warglaive_attackspeed)));
        ITEM.register(weaponPath + "cutlass", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + cutlass_modifier), cutlass_attackspeed)));
        ITEM.register(weaponPath + "claymore", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + claymore_modifier), claymore_attackspeed)));
        ITEM.register(weaponPath + "greathammer", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + greathammer_modifier), greathammer_attackspeed)));
        ITEM.register(weaponPath + "greataxe", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + greataxe_modifier), greataxe_attackspeed)));
        ITEM.register(weaponPath + "chakram", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + chakram_modifier), chakram_attackspeed)));
        ITEM.register(weaponPath + "scythe", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + scythe_modifier), scythe_attackspeed)));
        ITEM.register(weaponPath + "halberd", () -> new PalladiumSwordItem(material, LegacyWeaponAttributes.configure(defaultSettings(), (int) (modifier + halberd_modifier), halberd_attackspeed)));
    }

    private static Item.Settings defaultSettings() {
        return new Item.Settings().arch$tab(SimplySwords.SIMPLYSWORDS);
    }

    private static SimplySwordsSwordItem createSword(String materialName, ToolMaterial material, Item.Settings settings) {
        return materialName.equals("prometheum")
                ? new PrometheumSwordItem(material, settings)
                : new SimplySwordsSwordItem(material, settings);
    }

    public static void init() {
        var supportedItems = new HashMap<String, Float>();
        supportedItems.put("adamantite", adamantite_modifier);
        supportedItems.put("aquarium", aquarium_modifier);
        supportedItems.put("banglum", banglum_modifier);
        supportedItems.put("bronze", bronze_modifier);
        supportedItems.put("carmot", carmot_modifier);
        supportedItems.put("celestium", celestium_modifier);
        // These do not have textures
        // supportedItems.put("copper", copper_modifier);
        // supportedItems.put("durasteel", durasteel_modifier);
        supportedItems.put("kyber", kyber_modifier);
        supportedItems.put("metallurgium", metallurgium_modifier);
        supportedItems.put("mythril", mythril_modifier);
        supportedItems.put("orichalcum", orichalcum_modifier);
        supportedItems.put("osmium", osmium_modifier);
        supportedItems.put("palladium", palladium_modifier);
        supportedItems.put("prometheum", prometheum_modifier);
        supportedItems.put("quadrillum", quadrillum_modifier);
        supportedItems.put("runite", runite_modifier);
        supportedItems.put("star_platinum", star_platinum_modifier);
        supportedItems.put("steel", steel_modifier);
        supportedItems.put("stormyx", stormyx_modifier);

        registerItems(supportedItems);
        ITEM.register();
    }


}
