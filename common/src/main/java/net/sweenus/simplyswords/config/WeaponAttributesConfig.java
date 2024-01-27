package net.sweenus.simplyswords.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "weapon_attributes")
public class WeaponAttributesConfig implements ConfigData {

    @ConfigEntry.Gui.PrefixText
    public float longsword_positiveDamageModifier = 0.0f;
    public float twinblade_positiveDamageModifier = 0.0f;
    public float rapier_positiveDamageModifier = 0.0f;
    public float katana_positiveDamageModifier = 0.0f;
    public float sai_positiveDamageModifier = 0.0f;
    public float spear_positiveDamageModifier = 0.0f;
    public float glaive_positiveDamageModifier = 0.0f;
    public float warglaive_positiveDamageModifier = 0.0f;
    public float cutlass_positiveDamageModifier = 0.0f;
    public float claymore_positiveDamageModifier = 2.0f;
    public float greataxe_positiveDamageModifier = 3.0f;
    public float greathammer_positiveDamageModifier = 4.0f;
    public float chakram_positiveDamageModifier = 0.0f;
    public float scythe_positiveDamageModifier = 1.0f;
    public float halberd_positiveDamageModifier = 3.0f;

    @ConfigEntry.Gui.PrefixText
    public float longsword_negativeDamageModifier = 0.0f;
    public float twinblade_negativeDamageModifier = 0.0f;
    public float rapier_negativeDamageModifier = 1.0f;
    public float katana_negativeDamageModifier = 0.0f;
    public float sai_negativeDamageModifier = 3.0f;
    public float spear_negativeDamageModifier = 0.0f;
    public float glaive_negativeDamageModifier = 0.0f;
    public float warglaive_negativeDamageModifier = 0.0f;
    public float cutlass_negativeDamageModifier = 0.0f;
    public float claymore_negativeDamageModifier = 0.0f;
    public float greataxe_negativeDamageModifier = 0.0f;
    public float greathammer_negativeDamageModifier = 0.0f;
    public float chakram_negativeDamageModifier = 1.0f;
    public float scythe_negativeDamageModifier = 0.0f;
    public float halberd_negativeDamageModifier = 0.0f;

    @ConfigEntry.Gui.PrefixText
    public float iron_damageModifier = 3.0f;
    public float gold_damageModifier = 3.0f;
    public float diamond_damageModifier = 3.0f;
    public float netherite_damageModifier = 3.0f;
    public float runic_damageModifier = 3.0f;
    public float adamantite_damageModifier = 3.0f;
    public float aquarium_damageModifier = 3.0f;
    public float banglum_damageModifier = 3.0f;
    public float carmot_damageModifier = 3.0f;
    public float kyber_damageModifier = 3.0f;
    public float mythril_damageModifier = 3.0f;
    public float orichalcum_damageModifier = 3.0f;
    public float durasteel_damageModifier = 3.0f;
    public float osmium_damageModifier = 3.0f;
    public float prometheum_damageModifier = 3.0f;
    public float quadrillum_damageModifier = 3.0f;
    public float runite_damageModifier = 3.0f;
    public float starPlatinum_damageModifier = 3.0f;
    public float bronze_damageModifier = 3.0f;
    public float copper_damageModifier = 3.0f;
    public float steel_damageModifier = 3.0f;
    public float palladium_damageModifier = 3.0f;
    public float stormyx_damageModifier = 3.0f;
    public float celestium_damageModifier = 3.0f;
    public float metallurgium_damageModifier = 3.0f;
    public float gobber_damageModifier = 1.0f;
    public float gobberNether_damageModifier = 3.0f;
    public float gobberEnd_damageModifier = 6.0f;

    @ConfigEntry.Gui.PrefixText
    public float longsword_attackSpeed = -2.4f;
    public float twinblade_attackSpeed = -2.0f;
    public float rapier_attackSpeed = -1.8f;
    public float katana_attackSpeed = -2.0f;
    public float sai_attackSpeed = -1.5f;
    public float spear_attackSpeed = -2.7f;
    public float glaive_attackSpeed = -2.6f;
    public float warglaive_attackSpeed = -2.2f;
    public float cutlass_attackSpeed = -2.0f;
    public float claymore_attackSpeed = -2.8f;
    public float greataxe_attackSpeed = -3.1f;
    public float greathammer_attackSpeed = -3.2f;
    public float chakram_attackSpeed = -3.0f;
    public float scythe_attackSpeed = -2.7f;
    public float halberd_attackSpeed = -2.8f;

    @ConfigEntry.Gui.PrefixText
    public float brimstone_damageModifier = 6.0f;
    public float thewatcher_damageModifier = 6.0f;
    public float stormsedge_damageModifier = 3.0f;
    public float stormbringer_damageModifier = 3.0f;
    public float swordonastick_damageModifier = 5.0f;
    public float bramblethorn_damageModifier = 6.0f;
    public float watchingwarglaive_damageModifier = 3.0f;
    public float longswordofplague_damageModifier = 3.0f;
    public float emberblade_damageModifier = 3.0f;
    public float hearthflame_damageModifier = 8.0f;
    public float soulkeeper_damageModifier = 8.0f;
    public float twistedblade_damageModifier = 4.0f;
    public float soulstealer_damageModifier = 0.0f;
    public float soulrender_damageModifier = 4.0f;
    public float mjolnir_damageModifier = 3.0f;
    public float soulpyre_damageModifier = 7.0f;
    public float frostfall_damageModifier = 5.0f;
    public float moltenedge_damageModifier = 4.0f;
    public float livyatan_damageModifier = 4.0f;
    public float icewhisper_damageModifier = 7.0f;
    public float arcanethyst_damageModifier = 7.0f;
    public float thunderbrand_damageModifier = 7.0f;
    public float lichblade_damageModifier = 7.0f;
    public float shadowsting_damageModifier = -2.0f;
    public float sunfire_damageModifier = 3.0f;
    public float harbinger_damageModifier = 3.0f;
    public float whisperwind_damageModifier = 3.0f;
    public float emberlash_damageModifier = 0.0f;
    public float waxweaver_damageModifier = 6.0f;

    @ConfigEntry.Gui.PrefixText
    public float brimstone_attackSpeed = -2.8f;
    public float thewatcher_attackSpeed = -2.8f;
    public float stormsedge_attackSpeed = -2.0f;
    public float stormbringer_attackSpeed = -2.4f;
    public float swordonastick_attackSpeed = -2.6f;
    public float bramblethorn_attackSpeed = -1.8f;
    public float watchingwarglaive_attackSpeed = -2.2f;
    public float longswordofplague_attackSpeed = -2.4f;
    public float emberblade_attackSpeed = -2.4f;
    public float hearthflame_attackSpeed = -3.2f;
    public float soulkeeper_attackSpeed = -2.9f;
    public float twistedblade_attackSpeed = -2.6f;
    public float soulstealer_attackSpeed = -1.5f;
    public float soulrender_attackSpeed = -2.4f;
    public float mjolnir_attackSpeed = -3.0f;
    public float soulpyre_attackSpeed = -3.0f;
    public float frostfall_attackSpeed = -2.5f;
    public float moltenedge_attackSpeed = -2.1f;
    public float livyatan_attackSpeed = -2.1f;
    public float icewhisper_attackSpeed = -2.7f;
    public float arcanethyst_attackSpeed = -2.7f;
    public float thunderbrand_attackSpeed = -2.7f;
    public float lichblade_attackSpeed = -3.1f;
    public float shadowsting_attackSpeed = -1.7f;
    public float sunfire_attackSpeed = -2.4f;
    public float harbinger_attackSpeed = -2.4f;
    public float whisperwind_attackSpeed = -2.0f;
    public float emberlash_attackSpeed = -1.5f;
    public float waxweaver_attackSpeed = -2.9f;

}