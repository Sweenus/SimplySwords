# Observer-synchronized status effects

Minecraft normally sends a player's detailed status-effect instances only to
that player's own client. Register an effect here when another client's
renderer needs to know that the effect is active on a player or mob.

## Registration

Register the effect ID during common initialization:

```java
public static final Identifier PHASED =
        Identifier.of("exampleaddon", "phased");

SimplySwordsAPI.registerObserverSyncedStatusEffect(PHASED);
```

Registration is opt-in. Unregistered effects produce no observer packets. The
effect may be supplied by Minecraft, Simply Swords, or another required addon.
Register it before normal gameplay begins.

## Client queries

Use the client-only API from render or presentation code:

```java
if (ObserverStatusEffectClientApi.isActive(entity, PHASED)) {
    // Render the phased presentation.
}

ObserverStatusEffectClientApi.get(entity, PHASED).ifPresent(snapshot -> {
    int amplifier = snapshot.amplifier();
    int remainingTicks = snapshot.remainingDuration();
    boolean infinite = snapshot.infinite();
});
```

`ObserverStatusEffectSnapshot` exposes the amplifier, predicted remaining
duration, infinite-duration state, ambient flag, particle flag, and icon flag.
An infinite effect reports `-1` for `remainingDuration`.

The system automatically handles effect application, refreshes, upgrades,
removal, entity loading, player joins, respawns, and dimension changes for all
living entities.

## Presentation state only

Observer snapshots are not authoritative gameplay state. They are deliberately
kept separate from the remote entity's vanilla status-effect map, so querying
them does not run addon effect logic, alter attributes, or create particles.

Server gameplay should continue using `LivingEntity.getStatusEffect` and
`hasStatusEffect`. Client renderers should use
`ObserverStatusEffectClientApi` when inspecting another entity.

Simply Swords uses this system for Shadow Dance so every client can suppress
the complete player render, including armor and held items.
