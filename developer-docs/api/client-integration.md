# Client integration

Client API calls belong in the addon's client initializer. Do not reference
client classes from common registration or server gameplay code.

## Awakening form model property

Expose a form stage's `modelValue` to item model overrides:

```java
public static final Identifier FORM_PROPERTY =
        new Identifier("exampleaddon", "awakening_form");

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
        world,
        tooltip,
        context,
        "exampleaddon",
        "oracle_index:books/exampleaddon/weapon-types",
        "oracle_index:books/exampleaddon/unique-weapons",
        "oracle_index:books/exampleaddon/runic-powers",
        new Identifier("exampleaddon", "unique_effects.riftbrand")
);
```

The paths are Oracle Index document locations. The config identifier may be
null when the addon does not expose a compatible config path.

`UniqueWeaponItem` calls this through its protected
`generateDynamicTooltip` method. Override that method to supply addon paths, or
leave it empty if the addon does not use Oracle Index navigation.

Add custom lines through `appendUniqueWeaponTooltip(...)`, then call
`appendSharedUniqueWeaponTooltip(...)`. Do not override the mapped public
`appendTooltip` method from a separately compiled addon.

For a standard spell-school scaling line:

```java
SimplySwordsClientAPI.appendSpellScaleTooltip(tooltip, "arcane");
```

## Simply Tooltips

Register the addon namespace during client initialization to let Simply Swords'
built-in provider render addon `UniqueWeaponItem` instances:

```java
SimplySwordsClientAPI.registerUniqueTooltipNamespace("exampleaddon");
```

This supplies the standard lore and stat layout, unique rarity, awakening
progress frame, sealed-ability presentation, and level-aware reveal animation.
It claims only `UniqueWeaponItem` instances in that namespace.

Override `getTooltipWeaponType` when the provider should show a weapon-type
badge that cannot be inferred from the item path:

```java
@Override
public String getTooltipWeaponType(ItemStack stack) {
    return "stave";
}
```

Themes can still be assigned through Simply Tooltips'
`assets/simplytooltips/item_themes/*.json` data files.

For compatible items that do not extend `UniqueWeaponItem`, use the data tag:

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

The compatibility provider parses ordinary lore and stats but does not add the
awakening progress presentation. Addons only need a custom `TooltipProvider`
when deliberately replacing the built-in unique layout.

## Shared weapon HUD placement

Use the shared transform when an addon HUD should follow Simply Swords' scale
and offset configuration:

```java
SimplySwordsClientAPI.pushWeaponHudTransform(context);
try {
    // Draw around local origin.
} finally {
    context.getMatrices().pop();
}
```

The helper pushes the matrix stack, centres the origin above the hotbar, and
applies the configured scale. The caller must pop it.

## Visual effects

Gameplay state should be created on the server. Use vanilla tracked entities,
particles, sounds, or explicit addon packets to present that state to clients.

When a renderer needs the full state of a status effect applied to another
player or mob, use the opt-in
[observer status-effect API](observer-status-effects.md). It avoids requiring an
addon-specific packet and does not apply gameplay effects to remote entities.

Keep renderer classes and `MinecraftClient` references in client source sets.
Test a dedicated server to catch accidental client classloading.

For reusable parchment entities, glyph atlases, and white-marble terrain
fields, see [Parchment visuals](parchment-visuals.md).
