package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

public final class StormSoulMasteryAbilities {
    public static final Identifier STORMSCALE_ROD_ID = id("stormscale/lightning_rod");
    public static final Identifier IONBOUND_CRUSHER_ID = id("ionbound_stormscale/ion_crusher");
    public static final Identifier IONBOUND_BEAM_ID = id("ionbound_stormscale/paralysis_beam");
    public static final Identifier IONBOUND_SHIELD_ID = id("ionbound_stormscale/ion_reserve");
    public static final Identifier SOULRENDER_MARK_ID = id("soulrender/rendmark");
    public static final Identifier SOULRENDER_REAP_ID = id("soulrender/reaping");
    public static final Identifier SOULRENDER_GRAVE_ID = id("soulrender/gravebound");
    public static final Identifier SOULSTALKER_TENDRIL_ID = id("soulstalker/hunting_tendrils");
    public static final Identifier SOULSTALKER_STRIDE_ID = id("soulstalker/gloam_stride");
    public static final Identifier WHISPERWIND_DASH_ID = id("whisperwind/petal_step");
    public static final Identifier WHISPERWIND_RESET_ID = id("whisperwind/zephyr_rhythm");
    public static final Identifier WHISPERWIND_STILL_WIND_ID = id("whisperwind/still_wind");
    public static final Identifier DREADWHISPER_REAVE_ID = id("dreadwhisper/reaving_front");
    public static final Identifier DREADWHISPER_WOUND_ID = id("dreadwhisper/corrupted_wound");
    public static final Identifier HIT = id("mastery/storm_soul/hit");
    public static final Identifier PULSE = id("mastery/storm_soul/pulse");
    public static final Identifier KILL = id("mastery/storm_soul/kill");
    public static final Identifier FINISH = id("mastery/storm_soul/finish");
    public static final UniqueAbilityKey<StormSoulMasteryTuning> TUNING = UniqueAbilityKey.value(
            id("mastery/storm_soul/tuning"), StormSoulMasteryTuning.class, StormSoulMasteryTuning.EMPTY);
    public static final UniqueAbilityKey<Integer> COOLDOWN_TICKS = UniqueAbilityKey.integer(
            id("mastery/storm_soul/cooldown_ticks"), 0, 0, 72000);

    public static final UniqueAbilityDefinition STORMSCALE_ROD = active(STORMSCALE_ROD_ID);
    public static final UniqueAbilityDefinition IONBOUND_CRUSHER = active(IONBOUND_CRUSHER_ID);
    public static final UniqueAbilityDefinition IONBOUND_BEAM = active(IONBOUND_BEAM_ID);
    public static final UniqueAbilityDefinition IONBOUND_SHIELD = passive(IONBOUND_SHIELD_ID);
    public static final UniqueAbilityDefinition SOULRENDER_MARK = passive(SOULRENDER_MARK_ID);
    public static final UniqueAbilityDefinition SOULRENDER_REAP = active(SOULRENDER_REAP_ID);
    public static final UniqueAbilityDefinition SOULRENDER_GRAVE = passive(SOULRENDER_GRAVE_ID);
    public static final UniqueAbilityDefinition SOULSTALKER_TENDRIL = passive(SOULSTALKER_TENDRIL_ID);
    public static final UniqueAbilityDefinition SOULSTALKER_STRIDE = active(SOULSTALKER_STRIDE_ID);
    public static final UniqueAbilityDefinition WHISPERWIND_DASH = active(WHISPERWIND_DASH_ID);
    public static final UniqueAbilityDefinition WHISPERWIND_RESET = passive(WHISPERWIND_RESET_ID);
    public static final UniqueAbilityDefinition WHISPERWIND_STILL_WIND = passive(WHISPERWIND_STILL_WIND_ID);
    public static final UniqueAbilityDefinition DREADWHISPER_REAVE = active(DREADWHISPER_REAVE_ID);
    public static final UniqueAbilityDefinition DREADWHISPER_WOUND = passive(DREADWHISPER_WOUND_ID);

    private StormSoulMasteryAbilities() {
    }

    public static void register() {
        UniqueAbilityApi.registerDefinition(STORMSCALE_ROD);
        UniqueAbilityApi.registerDefinition(IONBOUND_CRUSHER);
        UniqueAbilityApi.registerDefinition(IONBOUND_BEAM);
        UniqueAbilityApi.registerDefinition(IONBOUND_SHIELD);
        UniqueAbilityApi.registerDefinition(SOULRENDER_MARK);
        UniqueAbilityApi.registerDefinition(SOULRENDER_REAP);
        UniqueAbilityApi.registerDefinition(SOULRENDER_GRAVE);
        UniqueAbilityApi.registerDefinition(SOULSTALKER_TENDRIL);
        UniqueAbilityApi.registerDefinition(SOULSTALKER_STRIDE);
        UniqueAbilityApi.registerDefinition(WHISPERWIND_DASH);
        UniqueAbilityApi.registerDefinition(WHISPERWIND_RESET);
        UniqueAbilityApi.registerDefinition(WHISPERWIND_STILL_WIND);
        UniqueAbilityApi.registerDefinition(DREADWHISPER_REAVE);
        UniqueAbilityApi.registerDefinition(DREADWHISPER_WOUND);
    }

    public static StormSoulMasteryTuning tuning(UniqueAbilityExecution execution) {
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
                .event(HIT).event(PULSE).event(KILL).event(FINISH);
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }
}
