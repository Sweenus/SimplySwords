package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

import java.util.List;

public final class StormFrostWaterMasteryAbilities {
    public static final Identifier HIT = id("mastery/storm_frost_water/hit");
    public static final Identifier PULSE = id("mastery/storm_frost_water/pulse");
    public static final Identifier RETURN_HIT = id("mastery/storm_frost_water/return_hit");
    public static final Identifier CATCH = id("mastery/storm_frost_water/catch");
    public static final Identifier RECALL = id("mastery/storm_frost_water/recall");
    public static final Identifier FINISH = id("mastery/storm_frost_water/finish");
    public static final UniqueAbilityKey<StormFrostWaterMasteryTuning> TUNING = UniqueAbilityKey.value(
            id("mastery/storm_frost_water/tuning"), StormFrostWaterMasteryTuning.class, StormFrostWaterMasteryTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("mastery/storm_frost_water/cooldown_ticks"), 0, 0, 72000);

    public static final UniqueAbilityDefinition STORMBRINGER_GUARD = active("stormbringer/shock_deflect");
    public static final UniqueAbilityDefinition STORMBRINGER_CHAIN = passive("stormbringer/chain_tempest");
    public static final UniqueAbilityDefinition MJOLNIR_STORM = active("mjolnir/storm");
    public static final UniqueAbilityDefinition THUNDERBRAND_REFRESH = passive("thunderbrand/refresh");
    public static final UniqueAbilityDefinition THUNDERBRAND_BLITZ = active("thunderbrand/thunder_blitz");
    public static final UniqueAbilityDefinition TEMPEST_MARK = passive("tempest/elemental_cadence");
    public static final UniqueAbilityDefinition TEMPEST_VORTEX = active("tempest/elemental_vortex");
    public static final UniqueAbilityDefinition FROSTFALL_THROW = active("frostfall/frost_fury");
    public static final UniqueAbilityDefinition FROSTFALL_FIELD = passive("frostfall/glacial_impact");
    public static final UniqueAbilityDefinition ICEWHISPER_AURA = passive("icewhisper/permafrost");
    public static final UniqueAbilityDefinition ICEWHISPER_COMETS = active("icewhisper/comet_storm");
    public static final UniqueAbilityDefinition LIVYATAN_THROW = active("livyatan/tempest_current");
    public static final UniqueAbilityDefinition LIVYATAN_RETURN = passive("livyatan/tidal_return");
    public static final UniqueAbilityDefinition LIVYATAN_WAVE = passive("livyatan/leviathans_wake");

    private StormFrostWaterMasteryAbilities() {
    }

    public static void register() {
        definitions().forEach(UniqueAbilityApi::registerDefinition);
    }

    public static List<UniqueAbilityDefinition> definitions() {
        return List.of(STORMBRINGER_GUARD, STORMBRINGER_CHAIN, MJOLNIR_STORM, THUNDERBRAND_REFRESH,
                THUNDERBRAND_BLITZ,
                TEMPEST_MARK, TEMPEST_VORTEX, FROSTFALL_THROW, FROSTFALL_FIELD, ICEWHISPER_AURA,
                ICEWHISPER_COMETS, LIVYATAN_THROW, LIVYATAN_RETURN, LIVYATAN_WAVE);
    }

    public static StormFrostWaterMasteryTuning tuning(UniqueAbilityExecution execution) {
        return execution.tuning().get(TUNING);
    }

    private static UniqueAbilityDefinition active(String path) {
        return base(path, UniqueAbilityKind.ACTIVE).cooldownKey(COOLDOWN_TICKS).build();
    }

    private static UniqueAbilityDefinition passive(String path) {
        return base(path, UniqueAbilityKind.PASSIVE).build();
    }

    private static UniqueAbilityDefinition.Builder base(String path, UniqueAbilityKind kind) {
        return UniqueAbilityDefinition.builder(id(path), kind).key(TUNING).event(HIT).event(PULSE)
                .event(RETURN_HIT).event(CATCH).event(RECALL).event(FINISH);
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }
}
