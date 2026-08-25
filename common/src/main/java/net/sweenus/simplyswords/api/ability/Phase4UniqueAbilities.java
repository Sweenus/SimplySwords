package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

public final class Phase4UniqueAbilities {
    public static final Identifier LICHBLADE_AURA_ID = id("awakened_lichblade/soul_anguish_aura");
    public static final Identifier LICHBLADE_CHANNEL_ID = id("awakened_lichblade/soul_anguish_channel");
    public static final Identifier SUNFIRE_STANDARD_ID = id("sunfire/radiant_standard");
    public static final Identifier SUNFIRE_REGEN_ID = id("sunfire/regeneration");
    public static final Identifier HARBINGER_STANDARD_ID = id("harbinger/gravity_standard");
    public static final Identifier HARBINGER_OMEN_ID = id("harbinger/weakness_omen");
    public static final Identifier HIT = id("phase4/hit");
    public static final Identifier PULSE = id("phase4/pulse");
    public static final Identifier SUPPORT = id("phase4/support");
    public static final Identifier KILL = id("phase4/kill");
    public static final Identifier FINISH = id("phase4/finish");
    public static final UniqueAbilityKey<Phase4AbilityTuning> TUNING = UniqueAbilityKey.value(
            id("phase4/tuning"), Phase4AbilityTuning.class, Phase4AbilityTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("phase4/cooldown_ticks"), 0, 0, 72000);

    public static final UniqueAbilityDefinition LICHBLADE_AURA = passive(LICHBLADE_AURA_ID);
    public static final UniqueAbilityDefinition LICHBLADE_CHANNEL = active(LICHBLADE_CHANNEL_ID);
    public static final UniqueAbilityDefinition SUNFIRE_STANDARD = active(SUNFIRE_STANDARD_ID);
    public static final UniqueAbilityDefinition SUNFIRE_REGEN = passive(SUNFIRE_REGEN_ID);
    public static final UniqueAbilityDefinition HARBINGER_STANDARD = active(HARBINGER_STANDARD_ID);
    public static final UniqueAbilityDefinition HARBINGER_OMEN = passive(HARBINGER_OMEN_ID);

    private Phase4UniqueAbilities() {
    }

    public static void register() {
        UniqueAbilityApi.registerDefinition(LICHBLADE_AURA);
        UniqueAbilityApi.registerDefinition(LICHBLADE_CHANNEL);
        UniqueAbilityApi.registerDefinition(SUNFIRE_STANDARD);
        UniqueAbilityApi.registerDefinition(SUNFIRE_REGEN);
        UniqueAbilityApi.registerDefinition(HARBINGER_STANDARD);
        UniqueAbilityApi.registerDefinition(HARBINGER_OMEN);
    }

    public static Phase4AbilityTuning tuning(UniqueAbilityExecution execution) {
        return execution.tuning().get(TUNING);
    }

    private static UniqueAbilityDefinition passive(Identifier id) {
        return base(id, UniqueAbilityKind.PASSIVE).build();
    }

    private static UniqueAbilityDefinition active(Identifier id) {
        return base(id, UniqueAbilityKind.ACTIVE).cooldownKey(COOLDOWN_TICKS).build();
    }

    private static UniqueAbilityDefinition.Builder base(Identifier id, UniqueAbilityKind kind) {
        return UniqueAbilityDefinition.builder(id, kind).key(TUNING).event(HIT).event(PULSE)
                .event(SUPPORT).event(KILL).event(FINISH);
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }
}
