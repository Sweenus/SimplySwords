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

Definitions can be registered and inspected through `UniqueAbilityApi`.
`BuiltinUniqueAbilities`.

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
