package net.sweenus.simplyswords.config;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.sweenus.simplyswords.SimplySwords;

@Config(name = SimplySwords.MOD_ID +"_main")
@Config.Gui.Background("cloth-config2:transparent")
public class ConfigWrapper extends PartitioningSerializer.GlobalData {
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.TransitiveObject
    public GeneralConfig general = new GeneralConfig();
    @ConfigEntry.Category("loot")
    @ConfigEntry.Gui.TransitiveObject
    public LootConfig loot = new LootConfig();
    @ConfigEntry.Category("gem_effects")
    @ConfigEntry.Gui.TransitiveObject
    public GemEffectsConfig gem_effects = new GemEffectsConfig();
    @ConfigEntry.Category("runic_effects")
    @ConfigEntry.Gui.TransitiveObject
    public RunicEffectsConfig runic_effects = new RunicEffectsConfig();
    @ConfigEntry.Category("status_effects")
    @ConfigEntry.Gui.TransitiveObject
    public StatusEffectsConfig status_effects = new StatusEffectsConfig();
    @ConfigEntry.Category("unique_effects")
    @ConfigEntry.Gui.TransitiveObject
    public UniqueEffectsConfig unique_effects = new UniqueEffectsConfig();
    @ConfigEntry.Category("weapon_attributes")
    @ConfigEntry.Gui.TransitiveObject
    public WeaponAttributesConfig weapon_attributes = new WeaponAttributesConfig();
}
