# Awakening forms

An awakening form family lets the Runic Forge change a weapon's presentation or
replace it with another registered item as levels change. A family may have one
fixed route or several context-selected routes.

## Fixed form family

This example changes the same item's name, rarity, and model value at levels 4
and 8:

```java
Identifier route = Identifier.of("exampleaddon", "rift");

AwakeningFormFamily family = AwakeningFormFamily.builder(
        RIFTBRAND.get(),
        AwakeningProfile.DEFAULT,
        Identifier.of("exampleaddon", "dormant_riftbrand")
    )
    .basePresentation(
        "item.exampleaddon.riftbrand",
        AwakeningFormRarity.UNIQUE,
        0.0F
    )
    .selectionLevel(0)
    .route(
        route,
        new AwakeningFormStage(
            Identifier.of("exampleaddon", "stirring_riftbrand"),
            4,
            RIFTBRAND.get(),
            "item.exampleaddon.stirring_riftbrand",
            AwakeningFormRarity.UNIQUE,
            0.5F
        ),
        new AwakeningFormStage(
            Identifier.of("exampleaddon", "awakened_riftbrand"),
            8,
            RIFTBRAND.get(),
            "item.exampleaddon.awakened_riftbrand",
            AwakeningFormRarity.LEGENDARY,
            1.0F
        )
    )
    .build();

SimplySwordsAPI.registerAwakeningFormFamily(family);
```

The `modelValue` must be between 0 and 1. Register a client model property to
expose it to item model overrides.

`AwakeningFormRegistry` is the underlying lookup and resolution registry.
Prefer `SimplySwordsAPI.registerAwakeningFormFamily` for addon registration and
the `AwakeningApi` lookup methods for normal weapon logic.

## Registration timing

Form families contain resolved `Item` instances. Register them after item
registries have been populated so deferred item suppliers are safe to read on
NeoForge. Architectury addons can use the common setup lifecycle on both
loaders:

```java
LifecycleEvent.SETUP.register(() -> {
    AwakeningFormFamily family = AwakeningFormFamily.builder(
            RIFTBRAND.get(),
            AwakeningProfile.DEFAULT,
            Identifier.of("exampleaddon", "dormant_riftbrand")
        )
        // Add presentation and routes here.
        .build();

    SimplySwordsAPI.registerAwakeningFormFamily(family);
});
```

Register the lifecycle callback during common initialization, after declaring
the addon's deferred item registrations. Do not call `RegistrySupplier.get()`
from a NeoForge mod constructor while registries are still being assembled.

## Branching by dimension

Multiple routes require an `AwakeningFormHandler`. Its selector runs when the
forge first crosses `selectionLevel`.

```java
Identifier netherRoute = Identifier.of("exampleaddon", "nether");
Identifier endRoute = Identifier.of("exampleaddon", "end");

AwakeningFormHandler handler = context -> {
    RegistryKey<World> dimension = context.world().getRegistryKey();
    return World.END.equals(dimension) ? endRoute : netherRoute;
};
```

Add both routes to the builder and call `.routeHandler(handler)`.

The selection context contains the server world, player, forge position, a copy
of the source stack, original level, and target level.

Route selection may run repeatedly while the screen builds previews. It must:

- be deterministic for the same context;
- be side-effect free;
- return an ID declared by that family;
- avoid consuming items, granting advancements, sending chat, or changing the
  world.

Use `onCommitted` for one-time side effects:

```java
AwakeningFormHandler handler = new AwakeningFormHandler() {
    @Override
    public Identifier selectRoute(AwakeningFormContext context) {
        return World.END.equals(context.world().getRegistryKey())
                ? END_ROUTE
                : NETHER_ROUTE;
    }

    @Override
    public void onCommitted(AwakeningFormCommitContext context) {
        // Award an advancement or play a one-time server effect here.
    }
};
```

## Same item versus replacement items

A stage may reference:

- the base item, changing only presentation and model value; or
- another registered item, replacing the stack while copying components.

Use `.alias(item)` for legacy or alternate items that belong to the family but
do not appear directly as a stage. An item may belong to only one family.

When downgrading below the selection level, the forge clears the route and
returns the family to its base item.

## Persistent progression

`.persistentProgression(true)` keeps the family active even when ordinary
awakening is disabled globally. Use it only when the item's identity depends on
its progression, as with the built-in Lichblade and Dormant Relic.

Ordinary addon uniques should leave it false so the user's legacy-mode setting
is respected.

## Reading a form

```java
AwakeningApi.getFormRoute(stack);      // selected route, if any
AwakeningApi.getFormStage(stack);      // resolved stage metadata
AwakeningApi.getFormId(stack);         // current stage ID
AwakeningApi.getFormModelValue(stack); // current model value
```

Names and rarity are automatically form-aware when the item extends
`UniqueWeaponItem`, including through `UniqueSwordItem`.

Registering a family also registers its awakening profile for the base item,
aliases, and stage items. Do not register those members into another family.
