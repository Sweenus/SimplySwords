package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

public final class AbyssalSpectralMasteryAbilities {
    public static final Identifier WATCHER_DREAD_ID = id("watcher/dread");
    public static final Identifier WATCHER_OMEN_ID = id("watcher/final_omen");
    public static final Identifier DEVOURER_MASS_ID = id("devourer/mass");
    public static final Identifier DEVOURER_REPRISAL_ID = id("devourer/reprisal");
    public static final Identifier WICKPIERCER_THROW_ID = id("wickpiercer/throw");
    public static final Identifier WICKPIERCER_REVIVE_ID = id("wickpiercer/revive");
    public static final Identifier GLOAMPIERCER_AMBUSH_ID = id("gloampiercer/ambush");
    public static final Identifier GLOAMPIERCER_BARRAGE_ID = id("gloampiercer/barrage");
    public static final Identifier WRAITHFANG_THROW_ID = id("wraithfang/throw");
    public static final Identifier WRAITHMAW_MUSTER_ID = id("wraithmaw/muster");
    public static final Identifier HIT = id("mastery/abyssal_spectral/hit");
    public static final Identifier PULSE = id("mastery/abyssal_spectral/pulse");
    public static final Identifier KILL = id("mastery/abyssal_spectral/kill");
    public static final Identifier RETURN = id("mastery/abyssal_spectral/return");
    public static final Identifier RECOVER = id("mastery/abyssal_spectral/recover");
    public static final Identifier COLLAPSE = id("mastery/abyssal_spectral/collapse");
    public static final UniqueAbilityKey<AbyssalSpectralMasteryTuning> TUNING = UniqueAbilityKey.value(
            id("mastery/abyssal_spectral/tuning"), AbyssalSpectralMasteryTuning.class, AbyssalSpectralMasteryTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("mastery/abyssal_spectral/cooldown_ticks"), 0, 0, 72000);

    public static final UniqueAbilityDefinition WATCHER_DREAD = passive(WATCHER_DREAD_ID);
    public static final UniqueAbilityDefinition WATCHER_OMEN = active(WATCHER_OMEN_ID);
    public static final UniqueAbilityDefinition DEVOURER_MASS = active(DEVOURER_MASS_ID);
    public static final UniqueAbilityDefinition DEVOURER_REPRISAL = passive(DEVOURER_REPRISAL_ID);
    public static final UniqueAbilityDefinition WICKPIERCER_THROW = active(WICKPIERCER_THROW_ID);
    public static final UniqueAbilityDefinition WICKPIERCER_REVIVE = passive(WICKPIERCER_REVIVE_ID);
    public static final UniqueAbilityDefinition GLOAMPIERCER_AMBUSH = passive(GLOAMPIERCER_AMBUSH_ID);
    public static final UniqueAbilityDefinition GLOAMPIERCER_BARRAGE = active(GLOAMPIERCER_BARRAGE_ID);
    public static final UniqueAbilityDefinition WRAITHFANG_THROW = active(WRAITHFANG_THROW_ID);
    public static final UniqueAbilityDefinition WRAITHMAW_MUSTER = active(WRAITHMAW_MUSTER_ID);

    private AbyssalSpectralMasteryAbilities() {
    }

    public static void register() {
        UniqueAbilityApi.registerDefinition(WATCHER_DREAD);
        UniqueAbilityApi.registerDefinition(WATCHER_OMEN);
        UniqueAbilityApi.registerDefinition(DEVOURER_MASS);
        UniqueAbilityApi.registerDefinition(DEVOURER_REPRISAL);
        UniqueAbilityApi.registerDefinition(WICKPIERCER_THROW);
        UniqueAbilityApi.registerDefinition(WICKPIERCER_REVIVE);
        UniqueAbilityApi.registerDefinition(GLOAMPIERCER_AMBUSH);
        UniqueAbilityApi.registerDefinition(GLOAMPIERCER_BARRAGE);
        UniqueAbilityApi.registerDefinition(WRAITHFANG_THROW);
        UniqueAbilityApi.registerDefinition(WRAITHMAW_MUSTER);
    }

    public static AbyssalSpectralMasteryTuning tuning(UniqueAbilityExecution execution) {
        return execution.tuning().get(TUNING);
    }

    private static UniqueAbilityDefinition passive(Identifier id) {
        return base(id, UniqueAbilityKind.PASSIVE).build();
    }

    private static UniqueAbilityDefinition active(Identifier id) {
        return base(id, UniqueAbilityKind.ACTIVE).cooldownKey(COOLDOWN_TICKS).build();
    }

    private static UniqueAbilityDefinition.Builder base(Identifier id, UniqueAbilityKind kind) {
        return UniqueAbilityDefinition.builder(id, kind).key(TUNING)
                .event(HIT).event(PULSE).event(KILL).event(RETURN).event(RECOVER).event(COLLAPSE);
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }
}
