package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UniqueAbilityApiContractTest {

    @Test
    void tuningKeysClampAndRejectForeignDefinitions() {
        UniqueAbilityKey<Integer> chance = UniqueAbilityKey.integer(id("chance"), 10, 0, 100);
        UniqueAbilityKey<Integer> foreign = UniqueAbilityKey.integer(id("foreign"), 0, 0, 10);
        UniqueAbilityDefinition definition = UniqueAbilityDefinition.builder(id("test"), UniqueAbilityKind.PASSIVE)
                .key(chance).build();
        UniqueAbilityTuning.Builder tuning = new UniqueAbilityTuning.Builder(definition);

        tuning.set(chance, 140);

        assertEquals(100, tuning.get(chance));
        assertThrows(IllegalArgumentException.class, () -> tuning.set(foreign, 1));
    }

    @Test
    void lifecycleIsOrderedAndTerminalEventsAreIdempotent() {
        Identifier hit = id("hit");
        UniqueAbilityDefinition definition = UniqueAbilityDefinition.builder(id("test"), UniqueAbilityKind.ACTIVE)
                .event(hit).build();
        List<UniqueAbilityPhase> phases = new ArrayList<>();
        UniqueAbilityExecution execution = new UniqueAbilityExecution(1, definition, null,
                new UniqueAbilityTuning.Builder(definition).build(), List.of(event -> phases.add(event.phase())));

        UniqueAbilityApi.start(execution);
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, hit, null, 1, 2.0);
        UniqueAbilityApi.finish(execution, definition.id(), 1);
        UniqueAbilityApi.cancel(execution);

        assertEquals(List.of(UniqueAbilityPhase.START, UniqueAbilityPhase.HIT, UniqueAbilityPhase.FINISH), phases);
        assertTrue(execution.isTerminal());
    }

    @Test
    void definitionsRejectDuplicateKeysAndEvents() {
        UniqueAbilityKey<Boolean> flag = UniqueAbilityKey.flag(id("flag"), false);
        assertThrows(IllegalArgumentException.class, () -> UniqueAbilityDefinition.builder(
                id("duplicate_key"), UniqueAbilityKind.ACTIVE).key(flag).key(flag));
        assertThrows(IllegalArgumentException.class, () -> UniqueAbilityDefinition.builder(
                id("duplicate_event"), UniqueAbilityKind.ACTIVE).event(id("hit")).event(id("hit")));
    }

    @Test
    void stormsEdgeAdditionsAreDisabledByDefaultAndAdditive() {
        UniqueAbilityTuning tuning = new UniqueAbilityTuning.Builder(BuiltinUniqueAbilities.STORMBREAK).build();

        assertEquals(BuiltinUniqueAbilities.CORRIDOR_FORCE_OUTWARD,
                tuning.get(BuiltinUniqueAbilities.CORRIDOR_FORCE_MODE));
        assertEquals(0, tuning.get(BuiltinUniqueAbilities.AFTERIMAGE_DURATION_TICKS));
        assertEquals(0, tuning.get(BuiltinUniqueAbilities.AFTERSHOCK_DELAY_TICKS));
        assertEquals(0, tuning.get(BuiltinUniqueAbilities.SUPERCELL_DURATION_TICKS));
        assertTrue(BuiltinUniqueAbilities.STORMBREAK.supportsEvent(BuiltinUniqueAbilities.DASH_END));
        assertTrue(BuiltinUniqueAbilities.STORMBREAK.supportsEvent(BuiltinUniqueAbilities.SUPERCELL_HIT));
        assertTrue(BuiltinUniqueAbilities.STORMS_EDGE_MELEE.supportsEvent(BuiltinUniqueAbilities.MELEE_HIT));
    }

    @Test
    void brimstoneDefinitionsPreserveBaselineAndDisableAdditions() {
        UniqueAbilityTuning eruption = new UniqueAbilityTuning.Builder(
                BuiltinUniqueAbilities.BRIMSTONE_ERUPTION).build();
        UniqueAbilityTuning rite = new UniqueAbilityTuning.Builder(BuiltinUniqueAbilities.BRIMSTONE_RITE).build();

        assertEquals(15, eruption.get(BuiltinUniqueAbilities.BRIMSTONE_PROC_CHANCE));
        assertEquals(3.0, eruption.get(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_RADIUS));
        assertEquals(0, eruption.get(BuiltinUniqueAbilities.BRIMSTONE_CINDER_COUNT));
        assertEquals(0, eruption.get(BuiltinUniqueAbilities.BRIMSTONE_CHAIN_MAX_DETONATIONS));
        assertEquals(120, rite.get(BuiltinUniqueAbilities.BRIMSTONE_RITE_DURATION_TICKS));
        assertEquals(20, rite.get(BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_INTERVAL_TICKS));
        assertEquals(0, rite.get(BuiltinUniqueAbilities.BRIMSTONE_WAKE_DURATION_TICKS));
        assertTrue(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION.supportsEvent(
                BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_HIT));
        assertTrue(BuiltinUniqueAbilities.BRIMSTONE_RITE.supportsEvent(
                BuiltinUniqueAbilities.BRIMSTONE_EMERGENCY_PLUNGE));
    }

    @Test
    void phase2DefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = List.of(
                Phase2UniqueAbilities.WATCHER_DREAD, Phase2UniqueAbilities.WATCHER_OMEN,
                Phase2UniqueAbilities.DEVOURER_MASS, Phase2UniqueAbilities.DEVOURER_REPRISAL,
                Phase2UniqueAbilities.WICKPIERCER_THROW, Phase2UniqueAbilities.WICKPIERCER_REVIVE,
                Phase2UniqueAbilities.GLOAMPIERCER_AMBUSH, Phase2UniqueAbilities.GLOAMPIERCER_BARRAGE,
                Phase2UniqueAbilities.WRAITHFANG_THROW, Phase2UniqueAbilities.WRAITHMAW_MUSTER);
        assertEquals(10, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(Phase2UniqueAbilities.TUNING));
            assertTrue(definition.supportsEvent(Phase2UniqueAbilities.HIT));
            assertTrue(definition.supportsEvent(Phase2UniqueAbilities.COLLAPSE));
        }
        Phase2AbilityTuning tuning = Phase2AbilityTuning.EMPTY
                .with(Phase2AbilityTuning.Setting.EXECUTE_THRESHOLD, 5)
                .with(Phase2AbilityTuning.Setting.TARGET_CAP, 500)
                .with(Phase2AbilityTuning.Setting.PROJECTILE_SPEED, Double.NaN);
        assertEquals(1.0, tuning.get(Phase2AbilityTuning.Setting.EXECUTE_THRESHOLD, 0));
        assertEquals(64, tuning.integer(Phase2AbilityTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(Phase2AbilityTuning.Setting.PROJECTILE_SPEED, 1));
    }

    @Test
    void phase3DefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = List.of(
                Phase3UniqueAbilities.STORMSCALE_ROD, Phase3UniqueAbilities.IONBOUND_CRUSHER,
                Phase3UniqueAbilities.IONBOUND_BEAM, Phase3UniqueAbilities.IONBOUND_SHIELD,
                Phase3UniqueAbilities.SOULRENDER_MARK, Phase3UniqueAbilities.SOULRENDER_REAP,
                Phase3UniqueAbilities.SOULSTALKER_TENDRIL, Phase3UniqueAbilities.SOULSTALKER_STRIDE,
                Phase3UniqueAbilities.WHISPERWIND_DASH, Phase3UniqueAbilities.WHISPERWIND_RESET,
                Phase3UniqueAbilities.DREADWHISPER_REAVE, Phase3UniqueAbilities.DREADWHISPER_WOUND);
        assertEquals(12, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(Phase3UniqueAbilities.TUNING));
            assertTrue(definition.supportsEvent(Phase3UniqueAbilities.HIT));
            assertTrue(definition.supportsEvent(Phase3UniqueAbilities.FINISH));
        }
        Phase3AbilityTuning tuning = Phase3AbilityTuning.EMPTY
                .with(Phase3AbilityTuning.Setting.CHANCE, 500)
                .with(Phase3AbilityTuning.Setting.TARGET_CAP, 500)
                .with(Phase3AbilityTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(Phase3AbilityTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(Phase3AbilityTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(Phase3AbilityTuning.Setting.SPEED, 1));
    }

    @Test
    void phase4DefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = List.of(
                Phase4UniqueAbilities.LICHBLADE_AURA, Phase4UniqueAbilities.LICHBLADE_CHANNEL,
                Phase4UniqueAbilities.SUNFIRE_STANDARD, Phase4UniqueAbilities.SUNFIRE_REGEN,
                Phase4UniqueAbilities.HARBINGER_STANDARD, Phase4UniqueAbilities.HARBINGER_OMEN);
        assertEquals(6, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(Phase4UniqueAbilities.TUNING));
            assertTrue(definition.supportsEvent(Phase4UniqueAbilities.HIT));
            assertTrue(definition.supportsEvent(Phase4UniqueAbilities.SUPPORT));
            assertTrue(definition.supportsEvent(Phase4UniqueAbilities.FINISH));
        }
        Phase4AbilityTuning tuning = Phase4AbilityTuning.EMPTY
                .with(Phase4AbilityTuning.Setting.CHANCE, 500)
                .with(Phase4AbilityTuning.Setting.TARGET_CAP, 500)
                .with(Phase4AbilityTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(Phase4AbilityTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(Phase4AbilityTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(Phase4AbilityTuning.Setting.SPEED, 1));
    }

    @Test
    void phase5DefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = Phase5UniqueAbilities.definitions();
        assertEquals(11, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(Phase5UniqueAbilities.TUNING));
            assertTrue(definition.supportsEvent(Phase5UniqueAbilities.HIT));
            assertTrue(definition.supportsEvent(Phase5UniqueAbilities.PULSE));
            assertTrue(definition.supportsEvent(Phase5UniqueAbilities.FINISH));
        }
        Phase5AbilityTuning tuning = Phase5AbilityTuning.EMPTY
                .with(Phase5AbilityTuning.Setting.CHANCE, 500)
                .with(Phase5AbilityTuning.Setting.TARGET_CAP, 500)
                .with(Phase5AbilityTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(Phase5AbilityTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(Phase5AbilityTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(Phase5AbilityTuning.Setting.SPEED, 1));
    }

    @Test
    void phase6DefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = Phase6UniqueAbilities.definitions();
        assertEquals(13, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(Phase6UniqueAbilities.TUNING));
            assertTrue(definition.supportsEvent(Phase6UniqueAbilities.HIT));
            assertTrue(definition.supportsEvent(Phase6UniqueAbilities.PULSE));
            assertTrue(definition.supportsEvent(Phase6UniqueAbilities.FINISH));
        }
        Phase6AbilityTuning tuning = Phase6AbilityTuning.EMPTY
                .with(Phase6AbilityTuning.Setting.CHANCE, 500)
                .with(Phase6AbilityTuning.Setting.TARGET_CAP, 500)
                .with(Phase6AbilityTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(Phase6AbilityTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(Phase6AbilityTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(Phase6AbilityTuning.Setting.SPEED, 1));
    }

    @Test
    void phase7DefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = Phase7UniqueAbilities.definitions();
        assertEquals(10, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(Phase7UniqueAbilities.TUNING));
            assertTrue(definition.supportsEvent(Phase7UniqueAbilities.HIT));
            assertTrue(definition.supportsEvent(Phase7UniqueAbilities.KILL));
            assertTrue(definition.supportsEvent(Phase7UniqueAbilities.FINISH));
        }
        Phase7AbilityTuning tuning = Phase7AbilityTuning.EMPTY
                .with(Phase7AbilityTuning.Setting.CHANCE, 500)
                .with(Phase7AbilityTuning.Setting.TARGET_CAP, 500)
                .with(Phase7AbilityTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(Phase7AbilityTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(Phase7AbilityTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(Phase7AbilityTuning.Setting.SPEED, 1));
    }

    @Test
    void phase8DefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = Phase8UniqueAbilities.definitions();
        assertEquals(18, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(Phase8UniqueAbilities.TUNING));
            assertTrue(definition.supportsEvent(Phase8UniqueAbilities.HIT));
            assertTrue(definition.supportsEvent(Phase8UniqueAbilities.KILL));
            assertTrue(definition.supportsEvent(Phase8UniqueAbilities.FINISH));
        }
        Phase8AbilityTuning tuning = Phase8AbilityTuning.EMPTY
                .with(Phase8AbilityTuning.Setting.CHANCE, 500)
                .with(Phase8AbilityTuning.Setting.TARGET_CAP, 500)
                .with(Phase8AbilityTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(Phase8AbilityTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(Phase8AbilityTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(Phase8AbilityTuning.Setting.SPEED, 1));
    }

    @Test
    void phase9DefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = Phase9UniqueAbilities.definitions();
        assertEquals(21, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(Phase9UniqueAbilities.TUNING));
            assertTrue(definition.supportsEvent(Phase9UniqueAbilities.HIT));
            assertTrue(definition.supportsEvent(Phase9UniqueAbilities.KILL));
            assertTrue(definition.supportsEvent(Phase9UniqueAbilities.FINISH));
        }
        Phase9AbilityTuning tuning = Phase9AbilityTuning.EMPTY
                .with(Phase9AbilityTuning.Setting.CHANCE, 500)
                .with(Phase9AbilityTuning.Setting.TARGET_CAP, 500)
                .with(Phase9AbilityTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(Phase9AbilityTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(Phase9AbilityTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(Phase9AbilityTuning.Setting.SPEED, 1));
    }

    @Test
    void phase10DefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = Phase10UniqueAbilities.definitions();
        assertEquals(15, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(Phase10UniqueAbilities.TUNING));
            assertTrue(definition.supportsEvent(Phase10UniqueAbilities.HIT));
            assertTrue(definition.supportsEvent(Phase10UniqueAbilities.KILL));
            assertTrue(definition.supportsEvent(Phase10UniqueAbilities.FINISH));
        }
        Phase10AbilityTuning tuning = Phase10AbilityTuning.EMPTY
                .with(Phase10AbilityTuning.Setting.CHANCE, 500)
                .with(Phase10AbilityTuning.Setting.TARGET_CAP, 500)
                .with(Phase10AbilityTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(Phase10AbilityTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(Phase10AbilityTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(Phase10AbilityTuning.Setting.SPEED, 1));
    }

    private static Identifier id(String path) {
        return Identifier.of("simplyswords_test", path);
    }
}
