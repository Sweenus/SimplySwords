# Contained Remnant transformations

A Contained Remnant can deterministically transform into a unique weapon when
used on a registered block.

Register a block-to-item mapping during common initialization:

```java
SimplySwordsAPI.registerTransformation(
        Blocks.RESPAWN_ANCHOR,
        Identifier.of("exampleaddon", "riftbrand")
);
```

The output identifier must point to a registered item. Choose a block that does
not conflict with another transformation; one block maps to one outcome.

On success, Simply Swords:

- consumes one remnant;
- creates the output as a natural level-0 awakening drop;
- plays the standard transformation effects;
- drops the result for the player;
- respects the disabled unique weapon configuration.

Nearby registered blocks also produce the Contained Remnant's guidance effect.

This integration is optional. A weapon can participate in awakening, sockets,
abilities, and pity loot without a remnant transformation.

Test the mapping after all addon and Simply Swords items have registered. Also
test it with the output disabled in the unique-loot configuration.

