package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

import java.util.List;

public final class Phase6UniqueAbilities {
    public static final Identifier HIT = id("phase6/hit");
    public static final Identifier PULSE = id("phase6/pulse");
    public static final Identifier RETURN_HIT = id("phase6/return_hit");
    public static final Identifier CATCH = id("phase6/catch");
    public static final Identifier RECALL = id("phase6/recall");
    public static final Identifier FINISH = id("phase6/finish");
    public static final UniqueAbilityKey<Phase6AbilityTuning> TUNING = UniqueAbilityKey.value(
            id("phase6/tuning"), Phase6AbilityTuning.class, Phase6AbilityTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("phase6/cooldown_ticks"), 0, 0, 72000);

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

    private Phase6UniqueAbilities() {
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

    public static Phase6AbilityTuning tuning(UniqueAbilityExecution execution) {
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
