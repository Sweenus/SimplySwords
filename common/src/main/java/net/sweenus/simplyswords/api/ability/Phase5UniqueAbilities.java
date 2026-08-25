package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

import java.util.List;

public final class Phase5UniqueAbilities {
    public static final Identifier HIT = id("phase5/hit");
    public static final Identifier PULSE = id("phase5/pulse");
    public static final Identifier KILL = id("phase5/kill");
    public static final Identifier FINISH = id("phase5/finish");
    public static final UniqueAbilityKey<Phase5AbilityTuning> TUNING = UniqueAbilityKey.value(
            id("phase5/tuning"), Phase5AbilityTuning.class, Phase5AbilityTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("phase5/cooldown_ticks"), 0, 0, 72000);

    public static final UniqueAbilityDefinition HEARTHFLAME_CHAINS = active("hearthflame/furnace_chains");
    public static final UniqueAbilityDefinition HEARTHFLAME_BRAND = passive("hearthflame/furnace_brand");
    public static final UniqueAbilityDefinition EMBERBLADE_SHRAPNEL = active("emberblade/ember_ire");
    public static final UniqueAbilityDefinition EMBERLASH_SMOULDER = passive("emberlash/smoulder");
    public static final UniqueAbilityDefinition EMBERLASH_CAUTERY = active("emberlash/cauterizing_step");
    public static final UniqueAbilityDefinition FLAMEWIND_SEED = active("flamewind/flame_seed");
    public static final UniqueAbilityDefinition MOLTEN_EDGE_HEAT = passive("molten_edge/heat");
    public static final UniqueAbilityDefinition MOLTEN_EDGE_VENT = active("molten_edge/vent");
    public static final UniqueAbilityDefinition MOLTEN_EDGE_RUPTURE = passive("molten_edge/rupture");
    public static final UniqueAbilityDefinition SOUL_PYRE_TETHER = active("soulpyre/soul_tether");
    public static final UniqueAbilityDefinition SOUL_PYRE_WISP = passive("soulpyre/pyre_wisp");

    private Phase5UniqueAbilities() {
    }

    public static void register() {
        definitions().forEach(UniqueAbilityApi::registerDefinition);
    }

    public static List<UniqueAbilityDefinition> definitions() {
        return List.of(HEARTHFLAME_CHAINS, HEARTHFLAME_BRAND, EMBERBLADE_SHRAPNEL,
                EMBERLASH_SMOULDER, EMBERLASH_CAUTERY, FLAMEWIND_SEED, MOLTEN_EDGE_HEAT,
                MOLTEN_EDGE_VENT, MOLTEN_EDGE_RUPTURE, SOUL_PYRE_TETHER, SOUL_PYRE_WISP);
    }

    public static Phase5AbilityTuning tuning(UniqueAbilityExecution execution) {
        return execution.tuning().get(TUNING);
    }

    private static UniqueAbilityDefinition passive(String path) {
        return base(path, UniqueAbilityKind.PASSIVE).build();
    }

    private static UniqueAbilityDefinition active(String path) {
        return base(path, UniqueAbilityKind.ACTIVE).cooldownKey(COOLDOWN_TICKS).build();
    }

    private static UniqueAbilityDefinition.Builder base(String path, UniqueAbilityKind kind) {
        return UniqueAbilityDefinition.builder(id(path), kind).key(TUNING).event(HIT).event(PULSE)
                .event(KILL).event(FINISH);
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }
}
