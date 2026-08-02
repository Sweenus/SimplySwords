# Unique loot and pity

Registering an addon weapon here adds it to Simply Swords' pity-controlled
unique chest pool:

```java
SimplySwordsAPI.registerUniqueLoot(MY_UNIQUE.get(), 1);
```

Call this during common initialization after the item is registered.

## Weights

The weight controls selection relative to every other entry in the unique pool:

```java
SimplySwordsAPI.registerUniqueLoot(COMMONER_UNIQUE.get(), 3);
SimplySwordsAPI.registerUniqueLoot(RARER_UNIQUE.get(), 1);
```

Weights below one are clamped to one. Weight does not change the chance for a
chest to produce any unique; that chance and the soft/hard pity rules come from
Simply Swords' loot configuration. Weight only affects which weapon is selected
after a unique roll succeeds.

Do not also inject the same item independently into the same chest unless two
separate acquisition rolls are intentional.

## Awakening state

Items selected by the pity system are created with:

```java
AwakeningApi.initializeNaturalDrop(new ItemStack(item));
```

This starts awakenable weapons at level 0. Non-awakenable items are unaffected.

For an addon's own loot path, use `initializeNaturalDrop` before inserting the
stack if it should follow the same progression. Use
`initializeFullyAwakened` only for an intentional max-level reward.

## Disabled weapons

Simply Swords' disabled unique weapon configuration is checked before an addon
entry is selected. Users can therefore remove an addon unique from this pool
without the addon implementing another blacklist.

## Direct registry access

`UniqueLootRegistry.register(item)` is equivalent to weight one, and
`entries()` returns a read-only view. Prefer
`SimplySwordsAPI.registerUniqueLoot` for registration.

## Testing

Use `/simplyswords loot_test <loot_table> <rolls>` to run simulated loot rolls.
The command reports every registered unique, including addon namespaces.

Use `/simplyswords loot_test_chest <loot_table> [count]` to receive real
loot-table-backed chests. It gives 16 by default. Place and open a fresh chest
for every roll; an opened vanilla loot container cannot generate twice.

Operators can inspect and control live player pity with:

```text
/simplyswords pity status [player]
/simplyswords pity ignore_regions <true|false> [player]
/simplyswords pity set <unique|tablet> <misses> [player]
/simplyswords pity reset <unique|tablet|all> [player]
```

Region bypass lets newly placed chests at the same coordinates grant pity
progress repeatedly. It lasts only for the player's current login session;
the counters themselves remain persistent until reset or satisfied.
