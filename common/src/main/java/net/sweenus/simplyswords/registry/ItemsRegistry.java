package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.SimplySwordsSwordItem;
import net.sweenus.simplyswords.item.custom.StealSwordItem;

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

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(SimplySwords.MOD_ID, Registry.ITEM_KEY);

    public static final RegistrySupplier<SimplySwordsSwordItem> IRON_LONGSWORD = ITEM.register("iron_longsword", () ->
            new SimplySwordsSwordItem(
                    ToolMaterials.IRON,
                    (int) (iron_modifier + longsword_positive_modifier - longsword_negative_modifier),
                    longsword_attackspeed,
                    "minecraft:iron_ingot"));

    public static final RegistrySupplier<StealSwordItem> SOULSTEALER = ITEM.register("soulstealer", () ->
            new StealSwordItem(ToolMaterials.NETHERITE,
                    (int) (soulstealer_damage_modifier),
                    soulstealer_attackspeed,
                    new Item.Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC)));

}
