package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

import java.util.List;

public final class Phase9UniqueAbilities {
    public static final Identifier HIT = id("phase9/hit");
    public static final Identifier PULSE = id("phase9/pulse");
    public static final Identifier KILL = id("phase9/kill");
    public static final Identifier FINISH = id("phase9/finish");
    public static final UniqueAbilityKey<Phase9AbilityTuning> TUNING = UniqueAbilityKey.value(
            id("phase9/tuning"), Phase9AbilityTuning.class, Phase9AbilityTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("phase9/cooldown_ticks"), 0, 0, 72000);

    public static final UniqueAbilityDefinition ARCANETHYST_SPARK = passive("arcanethyst/arcane_spark");
    public static final UniqueAbilityDefinition ARCANETHYST_SUSPENSION = active("arcanethyst/arcane_suspension");
    public static final UniqueAbilityDefinition ARCANETHYST_IMPACT = active("arcanethyst/amethyst_impact");
    public static final UniqueAbilityDefinition STARS_SOLAR = passive("stars_edge/solar_reprise");
    public static final UniqueAbilityDefinition STARS_LUNAR = passive("stars_edge/lunar_reprise");
    public static final UniqueAbilityDefinition STARS_CONSTELLATION = active("stars_edge/constellation");
    public static final UniqueAbilityDefinition MAGISCYTHE_STORM = active("magiscythe/storm_core");
    public static final UniqueAbilityDefinition MAGISCYTHE_STRIKES = passive("magiscythe/arc_strikes");
    public static final UniqueAbilityDefinition MAGISCYTHE_MAGEWRIGHT = passive("magiscythe/magewright");
    public static final UniqueAbilityDefinition MAGIBLADE_REPULSION = passive("magiblade/repulsion");
    public static final UniqueAbilityDefinition MAGIBLADE_WARDEN = active("magiblade/warden_head");
    public static final UniqueAbilityDefinition MAGIBLADE_JUDGMENT = active("magiblade/sonic_judgment");
    public static final UniqueAbilityDefinition MAGISPEAR_SPELLPOINT = passive("magispear/spellpoint");
    public static final UniqueAbilityDefinition MAGISPEAR_RAIN = active("magispear/spear_rain");
    public static final UniqueAbilityDefinition MAGISPEAR_SLAM = active("magispear/magislam");
    public static final UniqueAbilityDefinition ENIGMA_STORMCHASER = active("enigma/stormchaser");
    public static final UniqueAbilityDefinition ENIGMA_VORTEX = active("enigma/vortex");
    public static final UniqueAbilityDefinition ENIGMA_TAILWIND = passive("enigma/tailwind");
    public static final UniqueAbilityDefinition CAELESTIS_HOST = active("caelestis/rift_host");
    public static final UniqueAbilityDefinition CAELESTIS_MAW = active("caelestis/breach_maw");
    public static final UniqueAbilityDefinition CAELESTIS_PACT = active("caelestis/unbound_pact");

    private Phase9UniqueAbilities() {
    }

    public static void register() {
        definitions().forEach(UniqueAbilityApi::registerDefinition);
    }

    public static List<UniqueAbilityDefinition> definitions() {
        return List.of(ARCANETHYST_SPARK, ARCANETHYST_SUSPENSION, ARCANETHYST_IMPACT,
                STARS_SOLAR, STARS_LUNAR, STARS_CONSTELLATION,
                MAGISCYTHE_STORM, MAGISCYTHE_STRIKES, MAGISCYTHE_MAGEWRIGHT,
                MAGIBLADE_REPULSION, MAGIBLADE_WARDEN, MAGIBLADE_JUDGMENT,
                MAGISPEAR_SPELLPOINT, MAGISPEAR_RAIN, MAGISPEAR_SLAM,
                ENIGMA_STORMCHASER, ENIGMA_VORTEX, ENIGMA_TAILWIND,
                CAELESTIS_HOST, CAELESTIS_MAW, CAELESTIS_PACT);
    }

    public static Phase9AbilityTuning tuning(UniqueAbilityExecution execution) {
        return execution == null ? Phase9AbilityTuning.EMPTY : execution.tuning().get(TUNING);
    }

    private static UniqueAbilityDefinition active(String path) {
        return base(path, UniqueAbilityKind.ACTIVE).cooldownKey(COOLDOWN_TICKS).build();
    }

    private static UniqueAbilityDefinition passive(String path) {
        return base(path, UniqueAbilityKind.PASSIVE).build();
    }

    private static UniqueAbilityDefinition.Builder base(String path, UniqueAbilityKind kind) {
        return UniqueAbilityDefinition.builder(id(path), kind).key(TUNING)
                .event(HIT).event(PULSE).event(KILL).event(FINISH);
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }
}
