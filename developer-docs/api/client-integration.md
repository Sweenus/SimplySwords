# Client integration

Client API calls belong in the addon's client initializer. Do not reference
client classes from common registration or server gameplay code.

## Awakening form model property

Expose a form stage's `modelValue` to item model overrides:

```java
public static final Identifier FORM_PROPERTY =
        Identifier.of("exampleaddon", "awakening_form");

SimplySwordsClientAPI.registerAwakeningFormModelProperty(
        RIFTBRAND.get(),
        FORM_PROPERTY
);
```

Register the property for every physical item used by the form family.

An item model can then select overrides at the stage values:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "exampleaddon:item/riftbrand"
  },
  "overrides": [
    {
      "predicate": {
        "exampleaddon:awakening_form": 0.5
      },
      "model": "exampleaddon:item/riftbrand_stirring"
    },
    {
      "predicate": {
        "exampleaddon:awakening_form": 1.0
      },
      "model": "exampleaddon:item/riftbrand_awakened"
    }
  ]
}
```

List overrides from lower to higher thresholds.

## Dynamic tooltip navigation

`SimplySwordsClientAPI.generateDynamicTooltip` adds Simply Swords-style
information, search, and configuration navigation:

```java
SimplySwordsClientAPI.generateDynamicTooltip(
        stack,
        context,
        tooltip,
        type,
        "exampleaddon",
        "oracle_index:books/exampleaddon/weapon-types",
        "oracle_index:books/exampleaddon/unique-weapons",
        "oracle_index:books/exampleaddon/runic-powers",
        Identifier.of("exampleaddon", "unique_effects.riftbrand")
);
```

The paths are Oracle Index document locations. The config identifier may be
null when the addon does not expose a compatible config path.

`UniqueSwordItem` calls this through its protected
`generateDynamicTooltip` method. Override that method to supply addon paths, or
leave it empty if the addon does not use Oracle Index navigation.

Always call `super.appendTooltip(...)` when overriding the public tooltip method
unless the addon intentionally replaces implicit and integration lines.

## Simply Tooltips

Simply Swords' built-in tooltip provider deliberately supports only items in the
`simplyswords` namespace. Addon weapons therefore have two integration levels.

For the standard Simply Swords lore and stats layout, tag the item:

```text
data/simplytooltips/tags/items/simply_swords_compat.json
```

```json
{
  "replace": false,
  "values": [
    "exampleaddon:riftbrand",
    "#exampleaddon:unique_weapons"
  ]
}
```

This parses ability sections, action labels, implicit lines, attribute stats,
and dynamic-tooltip hints. Themes and badges can be assigned through Simply
Tooltips' `assets/simplytooltips/item_themes/*.json` data files.

The compatibility tag does not add awakening progress. For full parity,
register an addon `TooltipProvider` from client initialization at a priority
above the compatibility provider:

```java
TooltipProviderRegistry.register(new RiftbrandTooltipProvider(), 100);
```

The provider should:

1. Support only the addon's applicable unique items.
2. Build its lore and stats from the raw tooltip lines.
3. Read the effective level and unlock state from `AwakeningApi`.
4. Supply this progress metadata in its `ModernTooltipModel`:

```java
int level = AwakeningApi.getLevel(stack);

ItemFrameProgress progress = AwakeningApi.usesAwakeningProgression(stack)
        ? new ItemFrameProgress(
                level,
                8,
                0xFF74E7FF,
                0xFFFFFFFF,
                Text.literal(Integer.toString(level))
        )
        : null;
```

Use an animation key containing the level, such as
`"|awakening:" + level`, so a level change creates a fresh reveal animation.
When ALT is held, replace ordinary badges with an
`AWAKENING LEVEL <level>` badge. When
`AwakeningApi.isAbilityUnlocked(stack)` is false, replace or grey the ability
body and show `AwakeningApi.getAbilityUnlockLevel(stack)`.

Pass the `ItemFrameProgress` as the final field of `ModernTooltipModel`.
Simply Tooltips handles the frame shape, reveal animation, and travelling
highlight; the addon only supplies progress data.

Compiling a custom provider requires the Simply Tooltips API on the client
compile classpath. If the addon does not want that compile-time integration, use
the data tag and accept that awakening progress remains in ordinary tooltip
text or another addon-provided presentation.

## Visual effects

Gameplay state should be created on the server. Use vanilla tracked entities,
particles, sounds, or explicit addon packets to present that state to clients.

When a renderer needs the full state of a status effect applied to another
player or mob, use the opt-in
[observer status-effect API](observer-status-effects.md). It avoids requiring an
addon-specific packet and does not apply gameplay effects to remote entities.

Keep renderer classes and `MinecraftClient` references in client source sets.
Test a dedicated server to catch accidental client classloading.
