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

    private static Identifier id(String path) {
        return Identifier.of("simplyswords_test", path);
    }
}
