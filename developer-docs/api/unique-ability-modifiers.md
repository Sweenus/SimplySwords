# Unique ability modifiers

The `net.sweenus.simplyswords.api.ability` package lets addons tune an opted-in
unique ability and observe its server lifecycle without replacing the weapon
item or changing `UniqueWeaponActiveAbility`.

Register modifiers during common initialization:

```java
UniqueAbilityApi.registerModifier(
        Identifier.of("example", "mastery"),
        0,
        (context, definition, tuning) -> {
            if (definition != BuiltinUniqueAbilities.STORMBREAK) {
                return UniqueAbilityObserver.NONE;
            }
            tuning.add(BuiltinUniqueAbilities.CORRIDOR_WIDTH, 1.0);
            return event -> {
                if (event.phase() == UniqueAbilityPhase.HIT
                        && event.eventId().equals(BuiltinUniqueAbilities.CORRIDOR_HIT)) {
                    applyAddonEffect(event.target());
                }
            };
        }
);
```

Lower priorities run first. Equal priorities are ordered by the modifier owner
identifier. An exception from one modifier or observer is logged and does not
prevent the remaining integrations from running.

`UniqueAbilityContext` identifies the authoritative world, exact weapon stack,
physical actor, optional delegated player, target, hand, and cast geometry. The
actor may be any `LivingEntity`; integrations should not assume a player.

Each execution freezes its tuning and returned observers when it begins. A
long-running ability therefore keeps the mastery or equipment state it started
with even if the actor swaps items before completion.

## Definitions and keys

`UniqueAbilityDefinition` declares an identifier, active or passive kind,
supported typed tuning keys, lifecycle events, and an optional cooldown key.
Keys validate and clamp values when a modifier writes them. A modifier can only
read or write keys declared by that definition.

Definitions can be registered and inspected through `UniqueAbilityApi`; the
original built-ins remain available from `BuiltinUniqueAbilities`.

## Lifecycle

An execution emits `ATTEMPT`, then `START` when accepted. Ability-specific `HIT`
events may follow before one terminal `FINISH` or `CANCEL`. Terminal dispatch is
idempotent. Event payloads include the execution, event identifier, optional
target, affected-target count, and magnitude.

Addon abilities can opt into the same system by registering a definition,
calling `UniqueAbilityApi.begin` with their base tuning, retaining the returned
execution for timed behavior, and emitting lifecycle events as their state
advances. Existing abilities that do not opt in continue to use the unchanged
`UniqueWeaponActiveAbility` contract.

## Passive companion definitions

Some weapons expose a passive definition that exists purely so a modifier can be
resolved outside any running execution. `simplyswords:soulrender/gravebound` is
one: Soulrender's defensive nodes fire on incoming damage, on a target's death,
and on held ticks, none of which sit inside the rendmark or reaping executions.

The pattern is additive and available to addons. Register a passive definition
alongside the weapon's real abilities, `begin` it at the hook, read the frozen
tuning, and terminate it immediately:

```java
UniqueAbilityExecution execution = UniqueAbilityApi.begin(MY_PASSIVE,
        UniqueAbilityContext.passive(world, stack, actor, other, null),
        builder -> builder.set(MY_TUNING_KEY, MyTuning.EMPTY));
UniqueAbilityApi.takeStartedExecution();
UniqueAbilityApi.start(execution);
MyTuning tuning = execution.tuning().get(MY_TUNING_KEY);
UniqueAbilityApi.finish(execution, MY_FINISH_EVENT, 0);
```

Modifiers that do not know the definition ignore it, so adding one never changes
behaviour for existing integrations.

## Reaching spawned entities with tuning

An ability that spawns a long-lived entity must hand that entity the tuning it
was created with. Simply Swords does this by passing the frozen semantic
cohort tuning type, such as `StormSoulMasteryTuning`, into the entity at spawn
time — `SoulstalkerStrideEntity`
is the reference: without it, the entity falls back to config and every modifier
aimed at its behaviour silently does nothing.

Two rules follow:

- Give the entity the tuning in the same call that spawns it, before it ticks.
  A setter invoked later leaves a window where the entity runs on config values.
- If the entity persists (`shouldSave()` returning true), serialise the values
  you need in `writeCustomDataToNbt`. Entities that do not save — the stride
  entity among them — need no snapshot, but they do need the spawn-time hand-off.

## Overridden activation paths

A weapon that overrides `startPlayerAbility` and returns without calling
`SimplySwordsAPI.tryActivateWeaponAbility` never reaches `canActivate` or
`activate`. Whisperwind and Wickpiercer both do this. Any tuning key read only
inside `activate` is then reachable for a delegated mob wielder and unreachable
for a player.

When adding modifier support to such a weapon, read the tuning on **every** path
the ability can start from, not only the one `activate` covers.
