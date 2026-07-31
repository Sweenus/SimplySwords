# Battle Standards

`SimplySwordsAPI.spawnBattleStandard` is a legacy gameplay helper for spawning
the built-in Battle Standard entity:

```java
BattleStandardEntity standard = SimplySwordsAPI.spawnBattleStandard(
        player,
        decayRate,
        standardType,
        height,
        distance,
        positiveEffect,
        positiveEffectSecondary,
        positiveEffectAmplifier,
        negativeEffect,
        negativeEffectSecondary,
        negativeEffectAmplifier,
        dealsDamage,
        doesHealing
);
```

The helper:

- runs only on the server;
- places the standard relative to the player's facing;
- requires air at the destination;
- assigns the player as owner;
- returns the spawned entity, or null when it cannot spawn.

Effect strings may be null when that effect is unused. The standard type and
effect strings are interpreted by the built-in entity, so use values already
supported by Simply Swords rather than treating this as a custom standard
registry.

The method accepts a `PlayerEntity`, not an arbitrary `LivingEntity`. New addon
systems that need different ownership, visuals, or entity behavior should
register their own entity instead of depending on this helper.

