package net.sweenus.simplyswords.config;

import com.google.common.collect.ImmutableMap;
import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.SpellPowerWeaponAttributes;

import static net.sweenus.simplyswords.SimplySwords.minimumSpellPowerVersion;
import static net.sweenus.simplyswords.SimplySwords.minimumSpellbookVersion;

@Version(version = 1)
public class CompatibilityConfig extends Config {
    static final float DEFAULT_DIMINISHING_RETURNS_START = 1.40F;
    static final float DEFAULT_DIMINISHING_RETURNS_STRENGTH = 10.0F;
    static final float DEFAULT_SPELL_POWER_API_SCALING_MULTIPLIER = 0.50F;
    static final float DEFAULT_IRONS_BASE_POWER = 2.17F;

    public CompatibilityConfig() {
        super(Identifier.of(SimplySwords.MOD_ID, "compatibility"));
    }

    public ValidatedCondition<Boolean> enableSpellPowerScaling = new ValidatedBoolean(true)
            .toCondition(
                    () -> SimplySwords.passVersionCheck("spell_power", minimumSpellPowerVersion)
                            || SimplySwords.passVersionCheck("irons_spellbooks", minimumSpellbookVersion),
                    Text.translatable("simplyswords.compatibility.enableSpellPowerScaling.condition"),
                    () -> false
            ).withFailTitle(Text.translatable("simplyswords.compatibility.condition.failTitle"));

    public ValidatedFloat spellScalingDiminishingReturnsStart =
            new ValidatedFloat(DEFAULT_DIMINISHING_RETURNS_START, Float.MAX_VALUE, 1.0F);
    public ValidatedFloat spellScalingDiminishingReturnsStrength =
            new ValidatedFloat(DEFAULT_DIMINISHING_RETURNS_STRENGTH, Float.MAX_VALUE, 0.0F);

    public ValidatedCondition<SpellPowerApiSettings> spellPowerApi =
            new ValidatedAny<>(new SpellPowerApiSettings())
                    .toCondition(
                            () -> SimplySwords.passVersionCheck("spell_power", minimumSpellPowerVersion),
                            Text.translatable("simplyswords.compatibility.spellPowerApi.condition"),
                            SpellPowerApiSettings::new
                    ).withFailTitle(Text.translatable("simplyswords.compatibility.condition.failTitle"));

    public ValidatedCondition<IronsSpellsSettings> ironsSpells =
            new ValidatedAny<>(new IronsSpellsSettings())
                    .toCondition(
                            () -> SimplySwords.passVersionCheck("irons_spellbooks", minimumSpellbookVersion),
                            Text.translatable("simplyswords.compatibility.ironsSpells.condition"),
                            IronsSpellsSettings::new
                    ).withFailTitle(Text.translatable("simplyswords.compatibility.condition.failTitle"));

    public static final class SpellPowerApiSettings extends ConfigSection {
        public ValidatedBoolean enableWeaponAttributes = new ValidatedBoolean(true);
        public ValidatedFloat scalingMultiplier =
                new ValidatedFloat(DEFAULT_SPELL_POWER_API_SCALING_MULTIPLIER, 1.0F, 0.0F);
        public ValidatedIdentifierMap<Double> weaponSpellPowerBonuses =
                new ValidatedIdentifierMap.Builder<Double>()
                        .keyHandler(ValidatedIdentifier.ofRegistry(
                                Identifier.of(SimplySwords.MOD_ID, "emberblade"), Registries.ITEM))
                        .valueHandler(new ValidatedDouble(2.0D, 10000.0D, 0.0D))
                        .defaults(SpellPowerWeaponAttributes.defaultBonuses())
                        .build();
    }

    public static final class IronsSpellsSettings extends ConfigSection {
        public ValidatedBoolean enableCooldownReduction = new ValidatedBoolean(true);
        public ValidatedFloat basePower =
                new ValidatedFloat(DEFAULT_IRONS_BASE_POWER, Float.MAX_VALUE, 0.0F);
        public ValidatedIdentifierMap<Integer> weaponManaCosts =
                new ValidatedIdentifierMap.Builder<Integer>()
                        .keyHandler(ValidatedIdentifier.ofRegistry(
                                Identifier.of(SimplySwords.MOD_ID, "emberblade"), Registries.ITEM))
                        .valueHandler(new ValidatedInt(0, 10000, 0))
                        .defaults(defaultWeaponManaCosts())
                        .build();
    }

    static ImmutableMap<Identifier, Integer> defaultWeaponManaCosts() {
        return ImmutableMap.<Identifier, Integer>builder()
                .put(id("arcanethyst"), 45)
                .put(id("awakened_lichblade"), 55)
                .put(id("bloodwake"), 15)
                .put(id("bramblethorn"), 45)
                .put(id("brimstone_claymore"), 45)
                .put(id("caelestis"), 150)
                .put(id("chompolotl"), 43)
                .put(id("dawnquiver"), 45)
                .put(id("dreadwhisper"), 45)
                .put(id("emberblade"), 15)
                .put(id("emberlash"), 20)
                .put(id("enigma"), 100)
                .put(id("flamewind"), 30)
                .put(id("frostfall"), 17)
                .put(id("gloampiercer"), 45)
                .put(id("harbinger"), 67)
                .put(id("hearthflame"), 25)
                .put(id("hiveheart"), 80)
                .put(id("icewhisper"), 45)
                .put(id("ionbound_stormscale"), 25)
                .put(id("livyatan"), 10)
                .put(id("magiblade"), 15)
                .put(id("magiscythe"), 77)
                .put(id("magispear"), 35)
                .put(id("mjolnir"), 55)
                .put(id("molten_edge"), 10)
                .put(id("ribboncleaver"), 10)
                .put(id("riftmane"), 60)
                .put(id("shadowsting"), 70)
                .put(id("soulkeeper"), 40)
                .put(id("soulpyre"), 115)
                .put(id("soulrender"), 10)
                .put(id("soulstalker"), 100)
                .put(id("soulstealer"), 60)
                .put(id("stars_edge"), 52)
                .put(id("stormbringer"), 10)
                .put(id("storms_edge"), 30)
                .put(id("stormscale"), 25)
                .put(id("sunfire"), 67)
                .put(id("tempest"), 20)
                .put(id("the_devourer"), 60)
                .put(id("thunderbrand"), 45)
                .put(id("toxic_longsword"), 0)
                .put(id("twisted_blade"), 20)
                .put(id("waxweaver"), 40)
                .put(id("whisperwind"), 40)
                .put(id("wickpiercer"), 3)
                .put(id("wraithfang"), 40)
                .put(id("wraithmaw"), 47)
                .build();
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }
}
