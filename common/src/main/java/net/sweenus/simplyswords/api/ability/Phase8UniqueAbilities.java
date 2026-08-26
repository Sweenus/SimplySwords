package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

import java.util.List;

public final class Phase8UniqueAbilities {
    public static final Identifier HIT = id("phase8/hit");
    public static final Identifier PULSE = id("phase8/pulse");
    public static final Identifier KILL = id("phase8/kill");
    public static final Identifier FINISH = id("phase8/finish");
    public static final UniqueAbilityKey<Phase8AbilityTuning> TUNING = UniqueAbilityKey.value(
            id("phase8/tuning"), Phase8AbilityTuning.class, Phase8AbilityTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("phase8/cooldown_ticks"), 0, 0, 72000);

    public static final UniqueAbilityDefinition PLAGUE_PESTILENCE = passive("longsword_of_the_plague/pestilence");
    public static final UniqueAbilityDefinition PLAGUE_DEATH_KNELL = passive("longsword_of_the_plague/death_knell");
    public static final UniqueAbilityDefinition PLAGUE_OUTBREAK = passive("longsword_of_the_plague/outbreak");
    public static final UniqueAbilityDefinition SOULKEEPER_LANTERNS = passive("soulkeeper/soul_lanterns");
    public static final UniqueAbilityDefinition SOULKEEPER_VELOCITY = passive("soulkeeper/soul_velocity");
    public static final UniqueAbilityDefinition SOULKEEPER_CONCLAVE = active("soulkeeper/lantern_conclave");
    public static final UniqueAbilityDefinition SOULSTEALER_DEBT = passive("soulstealer/soul_debt");
    public static final UniqueAbilityDefinition SOULSTEALER_APPROACH = active("soulstealer/stygian_approach");
    public static final UniqueAbilityDefinition SOULSTEALER_REAP = active("soulstealer/soul_reap");
    public static final UniqueAbilityDefinition TWISTED_FEROCITY = passive("twisted_blade/ferocity");
    public static final UniqueAbilityDefinition TWISTED_CRESCENDO = passive("twisted_blade/crescendo");
    public static final UniqueAbilityDefinition TWISTED_FINALE = active("twisted_blade/finale");
    public static final UniqueAbilityDefinition SHADOW_ECHO = passive("shadowsting/shadow_echo");
    public static final UniqueAbilityDefinition SHADOW_DANCE = active("shadowsting/shadow_dance");
    public static final UniqueAbilityDefinition SHADOW_VEILSTEP = active("shadowsting/veilstep");
    public static final UniqueAbilityDefinition BLOOD_BURST = passive("bloodwake/crimson_burst");
    public static final UniqueAbilityDefinition BLOOD_RITES = active("bloodwake/blood_rites");
    public static final UniqueAbilityDefinition BLOOD_GROUND = passive("bloodwake/sanguine_ground");

    private Phase8UniqueAbilities() {
    }

    public static void register() {
        definitions().forEach(UniqueAbilityApi::registerDefinition);
    }

    public static List<UniqueAbilityDefinition> definitions() {
        return List.of(PLAGUE_PESTILENCE, PLAGUE_DEATH_KNELL, PLAGUE_OUTBREAK,
                SOULKEEPER_LANTERNS, SOULKEEPER_VELOCITY, SOULKEEPER_CONCLAVE,
                SOULSTEALER_DEBT, SOULSTEALER_APPROACH, SOULSTEALER_REAP,
                TWISTED_FEROCITY, TWISTED_CRESCENDO, TWISTED_FINALE,
                SHADOW_ECHO, SHADOW_DANCE, SHADOW_VEILSTEP,
                BLOOD_BURST, BLOOD_RITES, BLOOD_GROUND);
    }

    public static Phase8AbilityTuning tuning(UniqueAbilityExecution execution) {
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
