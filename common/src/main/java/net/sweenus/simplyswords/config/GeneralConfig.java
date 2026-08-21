package net.sweenus.simplyswords.config;

import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedSet;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

@Version(version = 1)
public class GeneralConfig extends Config {

    public GeneralConfig() {
        super(Identifier.of(SimplySwords.MOD_ID, "general"));
    }

    public boolean enableWeaponImpactSounds = true;
    @ValidatedFloat.Restrict(min = 0f, max = 1f)
    public float weaponImpactSoundsVolume = 0.3f;
    public boolean enableWeaponFootfalls = true;
    public boolean enablePassiveParticles = true;
    public boolean enableModernFieldEffects = true;
    @ValidatedInt.Restrict(min = 0, max = 2)
    public int stormEffectDetail = 2;
    public boolean enableWeaponImplicits = true;
    public boolean enableNonPlayerWeaponAbilityUse = true;
    public boolean enableAbilityDamageEnchantScaling = true;
    @ValidatedFloat.Restrict(min = 0f)
    public float nonPlayerWeaponAbilityDamageModifier = 0.5f;
    @ValidatedFloat.Restrict(min = 0f)
    public float weaponAbilityDamageToPlayersModifier = 0.5f;
    @ValidatedFloat.Restrict(min = 0f)
    public float nonPlayerWeaponHitDamageModifier = 0.3f;
    @ValidatedInt.Restrict(min = 1)
    public int nonPlayerWeaponAbilityCheckInterval = 80;
    @ValidatedInt.Restrict(min = 0, max = 100)
    public int nonPlayerWeaponAbilityChance = 50;
    public ValidatedSet<String> abilityIgnoredEntities =
            new ValidatedString("", "(?:#?[a-z0-9_.-]+:[a-z0-9_./-]+|[a-z_]+)?")
                    .toSet("passive", "minecraft:armor_stand", "minecraft:villager");
    public boolean enableUniqueWeaponAwakening = true;
    public boolean enableUniqueGemSockets = true;
    public ValidatedSet<String> additionalGemSocketItems =
            new ValidatedString("", "(?:#?[a-z0-9_.-]+:[a-z0-9_./-]+)?").toSet();
    public boolean enableTooltipInfoButtons = true;
    public boolean tooltipInfoButtonsRequireInventoryScreen = true;
    @ValidatedInt.Restrict(min = 1200, max = 72000)
    public int containedRemnantTransformFrequency = 18000;

}
