package net.sweenus.simplyswords.api.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class UniqueAbilityApi {
    private static final AtomicLong NEXT_EXECUTION_ID = new AtomicLong();
    private static final Map<Identifier, UniqueAbilityDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<Identifier, ModifierEntry> MODIFIERS = new LinkedHashMap<>();
    private static final ThreadLocal<UniqueAbilityExecution> STARTED_EXECUTION = new ThreadLocal<>();
    private static volatile UniqueAbilityDiagnostics diagnostics = UniqueAbilityDiagnostics.NONE;

    private UniqueAbilityApi() {
    }

    public static void setDiagnostics(@Nullable UniqueAbilityDiagnostics sink) {
        diagnostics = sink == null ? UniqueAbilityDiagnostics.NONE : sink;
    }

    public static UniqueAbilityDiagnostics diagnostics() {
        return diagnostics;
    }

    public static synchronized void registerDefinition(UniqueAbilityDefinition definition) {
        Objects.requireNonNull(definition);
        UniqueAbilityDefinition existing = DEFINITIONS.putIfAbsent(definition.id(), definition);
        if (existing != null && existing != definition) {
            throw new IllegalStateException("Ability definition already registered: " + definition.id());
        }
    }

    public static synchronized void registerModifier(Identifier ownerId, int priority,
                                                     UniqueAbilityModifier modifier) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(modifier);
        if (MODIFIERS.putIfAbsent(ownerId, new ModifierEntry(ownerId, priority, modifier)) != null) {
            throw new IllegalStateException("Ability modifier already registered: " + ownerId);
        }
    }

    public static synchronized boolean isDefinitionRegistered(Identifier id) {
        return DEFINITIONS.containsKey(id);
    }

    public static synchronized Optional<UniqueAbilityDefinition> definition(Identifier id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static synchronized List<UniqueAbilityDefinition> definitions() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static UniqueAbilityExecution begin(UniqueAbilityDefinition definition, UniqueAbilityContext context,
                                               Consumer<UniqueAbilityTuning.Builder> baseTuning) {
        Objects.requireNonNull(definition);
        Objects.requireNonNull(context);
        Objects.requireNonNull(baseTuning);
        UniqueAbilityTuning.Builder tuning = new UniqueAbilityTuning.Builder(definition);
        baseTuning.accept(tuning);
        UniqueAbilityDiagnostics sink = diagnostics;
        Map<UniqueAbilityKey<?>, Object> base = null;
        if (sink != UniqueAbilityDiagnostics.NONE && isActive(sink, context.actor())) {
            base = new LinkedHashMap<>();
            for (UniqueAbilityKey<?> key : definition.keys()) {
                base.put(key, tuning.get(key));
            }
        }
        List<UniqueAbilityObserver> observers = new ArrayList<>();
        for (ModifierEntry entry : modifierSnapshot()) {
            try {
                UniqueAbilityObserver observer = entry.modifier.prepare(context, definition, tuning);
                if (observer != null && observer != UniqueAbilityObserver.NONE) {
                    observers.add(observer);
                }
            } catch (RuntimeException exception) {
                SimplySwords.LOGGER.error("Unique ability modifier {} failed for {}", entry.ownerId, definition.id(), exception);
            }
        }
        UniqueAbilityExecution execution = new UniqueAbilityExecution(
                NEXT_EXECUTION_ID.incrementAndGet(), definition, context, tuning.build(), observers);
        STARTED_EXECUTION.set(execution);
        if (base != null) {
            try {
                sink.onComposed(execution, Collections.unmodifiableMap(base));
            } catch (RuntimeException exception) {
                SimplySwords.LOGGER.error("Unique ability diagnostics failed for {}", definition.id(), exception);
            }
        }
        emit(execution, UniqueAbilityPhase.ATTEMPT, definition.id(), null, 0, 0.0);
        return execution;
    }

    public static void start(UniqueAbilityExecution execution) {
        if (execution == null || execution.isStarted() || execution.isTerminal()) {
            return;
        }
        execution.markStarted();
        emit(execution, UniqueAbilityPhase.START, execution.definition().id(), null, 0, 0.0);
    }

    public static UniqueAbilityExecution preparePassive(UniqueAbilityDefinition definition, UniqueAbilityContext context,
                                                        Consumer<UniqueAbilityTuning.Builder> baseTuning) {
        UniqueAbilityExecution outer = takeStartedExecution();
        try {
            return begin(definition, context, baseTuning);
        } finally {
            publishStartedExecution(outer);
        }
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, UniqueAbilityContext context,
                                                      Consumer<UniqueAbilityTuning.Builder> baseTuning) {
        UniqueAbilityExecution outer = takeStartedExecution();
        try {
            UniqueAbilityExecution execution = begin(definition, context, baseTuning);
            takeStartedExecution();
            start(execution);
            return execution;
        } finally {
            publishStartedExecution(outer);
        }
    }

    public static void reportRoll(UniqueAbilityExecution execution, UniqueAbilityKey<?> key,
                                  double chance, double roll, boolean passed) {
        if (execution == null || key == null) {
            return;
        }
        reportRoll(execution.context().actor(), execution.definition().id(), key.id().getPath(),
                chance, roll, passed);
    }

    public static void reportRoll(@Nullable LivingEntity actor, Identifier abilityId, String label,
                                  double chance, double roll, boolean passed) {
        UniqueAbilityDiagnostics sink = diagnostics;
        if (actor == null || abilityId == null || sink == UniqueAbilityDiagnostics.NONE
                || !isActive(sink, actor)) {
            return;
        }
        try {
            sink.onRoll(actor, abilityId, label, chance, roll, passed);
        } catch (RuntimeException exception) {
            SimplySwords.LOGGER.error("Unique ability diagnostics failed for {}", abilityId, exception);
        }
    }

    private static boolean isActive(UniqueAbilityDiagnostics sink, @Nullable LivingEntity actor) {
        if (actor == null) {
            return false;
        }
        try {
            return sink.isActive(actor);
        } catch (RuntimeException exception) {
            SimplySwords.LOGGER.error("Unique ability diagnostics failed", exception);
            return false;
        }
    }

    public static void emit(UniqueAbilityExecution execution, UniqueAbilityPhase phase, Identifier eventId,
                            @Nullable LivingEntity target, int affectedTargets, double magnitude) {
        Objects.requireNonNull(execution);
        Objects.requireNonNull(phase);
        Objects.requireNonNull(eventId);
        if (execution.isTerminal()) {
            return;
        }
        if (eventId != execution.definition().id() && !eventId.equals(execution.definition().id())
                && !execution.definition().supportsEvent(eventId)) {
            throw new IllegalArgumentException("unsupported event " + eventId);
        }
        UniqueAbilityEvent event = new UniqueAbilityEvent(execution, phase, eventId, target,
                Math.max(0, affectedTargets), magnitude);
        UniqueAbilityExecution outer = STARTED_EXECUTION.get();
        try {
            for (UniqueAbilityObserver observer : execution.observers()) {
                try {
                    observer.onEvent(event);
                } catch (RuntimeException exception) {
                    SimplySwords.LOGGER.error("Unique ability observer failed for {}", execution.definition().id(), exception);
                } finally {
                    publishStartedExecution(outer);
                }
            }
        } finally {
            publishStartedExecution(outer);
        }
    }

    public static void finish(UniqueAbilityExecution execution, Identifier eventId, int affectedTargets) {
        terminate(execution, UniqueAbilityPhase.FINISH, eventId, affectedTargets);
    }

    public static void cancel(UniqueAbilityExecution execution) {
        terminate(execution, UniqueAbilityPhase.CANCEL, execution.definition().id(), 0);
    }

    public static void clearStartedExecution() {
        STARTED_EXECUTION.remove();
    }

    public static void publishStartedExecution(UniqueAbilityExecution execution) {
        if (execution == null) {
            STARTED_EXECUTION.remove();
            return;
        }
        STARTED_EXECUTION.set(execution);
    }

    public static @Nullable UniqueAbilityExecution takeStartedExecution() {
        UniqueAbilityExecution execution = STARTED_EXECUTION.get();
        STARTED_EXECUTION.remove();
        return execution;
    }

    private static void terminate(UniqueAbilityExecution execution, UniqueAbilityPhase phase,
                                  Identifier eventId, int affectedTargets) {
        if (execution == null || execution.isTerminal()) {
            return;
        }
        emit(execution, phase, eventId, null, affectedTargets, 0.0);
        execution.markTerminal();
    }

    private static synchronized List<ModifierEntry> modifierSnapshot() {
        return MODIFIERS.values().stream()
                .sorted(Comparator.comparingInt(ModifierEntry::priority).thenComparing(entry -> entry.ownerId.toString()))
                .toList();
    }

    private record ModifierEntry(Identifier ownerId, int priority, UniqueAbilityModifier modifier) {
    }
}
