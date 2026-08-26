package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

import java.util.List;

public final class Phase10UniqueAbilities {
    public static final Identifier HIT = id("phase10/hit");
    public static final Identifier PULSE = id("phase10/pulse");
    public static final Identifier KILL = id("phase10/kill");
    public static final Identifier FINISH = id("phase10/finish");
    public static final UniqueAbilityKey<Phase10AbilityTuning> TUNING = UniqueAbilityKey.value(
            id("phase10/tuning"), Phase10AbilityTuning.class, Phase10AbilityTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("phase10/cooldown_ticks"), 0, 0, 72000);

    public static final UniqueAbilityDefinition WARG_MARK = passive("watching_warglaive/dreadmark");
    public static final UniqueAbilityDefinition WARG_HUNT = active("watching_warglaive/nightwing_hunt");
    public static final UniqueAbilityDefinition WARG_SANGUINE = active("watching_warglaive/sanguine_watch");
    public static final UniqueAbilityDefinition RIBBON_HEAVY = passive("ribboncleaver/heavy_ribbon");
    public static final UniqueAbilityDefinition RIBBON_RUSH = active("ribboncleaver/ribbon_rush");
    public static final UniqueAbilityDefinition RIBBON_PROMISE = passive("ribboncleaver/cleaving_promise");
    public static final UniqueAbilityDefinition RIFTMANE_HARRIER = passive("riftmane/rift_harrier");
    public static final UniqueAbilityDefinition RIFTMANE_RANK = active("riftmane/vanguard_rank");
    public static final UniqueAbilityDefinition RIFTMANE_RIDER = active("riftmane/spectral_rider");
    public static final UniqueAbilityDefinition DAWN_LESSER = passive("dawnquiver/lesser_dawn");
    public static final UniqueAbilityDefinition DAWN_CHORUS = passive("dawnquiver/dawn_chorus");
    public static final UniqueAbilityDefinition DAWN_DRAW = active("dawnquiver/seraphs_draw");
    public static final UniqueAbilityDefinition DREAD_CLOAK = passive("dreadtide/voidcloak");
    public static final UniqueAbilityDefinition DREAD_ASSAULT = active("dreadtide/void_assault");
    public static final UniqueAbilityDefinition DREAD_PACT = passive("dreadtide/corruption_pact");

    private Phase10UniqueAbilities() {
    }

    public static void register() {
        definitions().forEach(UniqueAbilityApi::registerDefinition);
    }

    public static List<UniqueAbilityDefinition> definitions() {
        return List.of(WARG_MARK, WARG_HUNT, WARG_SANGUINE, RIBBON_HEAVY, RIBBON_RUSH,
                RIBBON_PROMISE, RIFTMANE_HARRIER, RIFTMANE_RANK, RIFTMANE_RIDER,
                DAWN_LESSER, DAWN_CHORUS, DAWN_DRAW, DREAD_CLOAK, DREAD_ASSAULT, DREAD_PACT);
    }

    public static Phase10AbilityTuning tuning(UniqueAbilityExecution execution) {
        return execution == null ? Phase10AbilityTuning.EMPTY : execution.tuning().get(TUNING);
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
