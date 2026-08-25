package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

import java.util.List;

public final class Phase7UniqueAbilities {
    public static final Identifier HIT = id("phase7/hit");
    public static final Identifier PULSE = id("phase7/pulse");
    public static final Identifier KILL = id("phase7/kill");
    public static final Identifier FINISH = id("phase7/finish");
    public static final UniqueAbilityKey<Phase7AbilityTuning> TUNING = UniqueAbilityKey.value(
            id("phase7/tuning"), Phase7AbilityTuning.class, Phase7AbilityTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("phase7/cooldown_ticks"), 0, 0, 72000);

    public static final UniqueAbilityDefinition BRAMBLE_GRASP = active("bramblethorn/wild_grasp");
    public static final UniqueAbilityDefinition BRAMBLE_HUNT = passive("bramblethorn/thorn_dance");
    public static final UniqueAbilityDefinition WAXWEAVER_PRISON = active("waxweaver/waxweave");
    public static final UniqueAbilityDefinition WAXWEAVER_TEMPO = passive("waxweaver/kindled_tempo");
    public static final UniqueAbilityDefinition WAXWEAVER_REVIVAL = passive("waxweaver/chrysalis");
    public static final UniqueAbilityDefinition HIVEHEART_PROC = passive("hiveheart/hivemind");
    public static final UniqueAbilityDefinition HIVEHEART_SWARM = active("hiveheart/swarm");
    public static final UniqueAbilityDefinition CHOMPOLOTL_PROC = passive("chompolotl/chompocalypse");
    public static final UniqueAbilityDefinition CHOMPOLOTL_RALLY = active("chompolotl/rally");
    public static final UniqueAbilityDefinition CHOMPOLOTL_GUARDIAN = passive("chompolotl/blue_guardian");

    private Phase7UniqueAbilities() {
    }

    public static void register() {
        definitions().forEach(UniqueAbilityApi::registerDefinition);
    }

    public static List<UniqueAbilityDefinition> definitions() {
        return List.of(BRAMBLE_GRASP, BRAMBLE_HUNT, WAXWEAVER_PRISON, WAXWEAVER_TEMPO,
                WAXWEAVER_REVIVAL, HIVEHEART_PROC, HIVEHEART_SWARM, CHOMPOLOTL_PROC,
                CHOMPOLOTL_RALLY, CHOMPOLOTL_GUARDIAN);
    }

    public static Phase7AbilityTuning tuning(UniqueAbilityExecution execution) {
        return execution.tuning().get(TUNING);
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
