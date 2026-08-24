# Parchment visuals

Simply Swords exposes its parchment and white-marble renderers for addon
entities. The addon owns entity registration, tracked state, spawning, and
lifetime; Simply Swords owns the client renderer and shared textures.

## Entity contracts

Implement the matching common-side interface on the addon's entity, then
register the renderer from the addon client initializer.

| Interface | Shared renderer | State supplied by the entity |
| --- | --- | --- |
| `ParchmentBoltRenderData` | `ParchmentBoltEntityRenderer` | spin seed and flight yaw/pitch |
| `ParchmentBookRenderData` | `AmanuensisBookRenderer` | glyph data, channel progress, recitation pulse |
| `ParchmentChannelRenderData` | `SpellChannelVisualEntityRenderer` | glyph data, owner entity ID, progress |
| `ParchmentOrbitRenderData` | `ParchmentOrbitVisualEntityRenderer` | mode, pages, radius, height, owner entity ID |
| `ParchmentRibbonRenderData` | `ParchmentRibbonVisualEntityRenderer` | style, end offset, seed, width, life progress |
| `TerrainFieldRenderData` | `ConsecrationFieldEntityRenderer` | field radius and terrain style ID |

Example registration:

```java
EntityRendererRegistry.register(
        MY_CHANNEL_ENTITY,
        SpellChannelVisualEntityRenderer::new
);
```

The interface values used during rendering must be available on the client.
Use tracked entity data for values that can change or differ between instances.
Keep renderer registration and all `net.minecraft.client` references out of
common and dedicated-server initialization.

`ParchmentOrbitRenderData` and `ParchmentRibbonRenderData` define the supported
mode and style constants. Use those constants rather than copying their numeric
values.

## Glyph selection and styles

`ParchmentGlyphRenderData` selects a style ID, the first atlas cell, and the
number of consecutive glyphs to reveal. `ParchmentGlyphRef` groups those values
for common spell definitions. Atlas cells are read left-to-right, then
top-to-bottom.

The default `simplyswords:parchment_gold` style and its textures can be used
without addon resources. To register another atlas, call this during client
initialization:

```java
SimplySwordsClientAPI.registerParchmentGlyphStyle(
        Identifier.of("exampleaddon", "rift_blue"),
        new ParchmentGlyphStyle(
                Identifier.of("exampleaddon", "textures/entity/rift_glyphs.png"),
                Identifier.of("exampleaddon", "textures/entity/rift_glyphs_glow.png"),
                8,
                8,
                0xFFB8E8FF,
                0xFF7CCEFF,
                1.7F,
                0.55F
        )
);
```

Colors are ARGB. `haloScale` cannot be below `1.0`, and `haloAlpha` is clamped
to `0.0` through `1.0`. A style ID may be registered repeatedly only with the
same value; conflicting duplicate registrations fail fast. Unknown IDs log a
warning and use the default style.

## White-marble terrain fields

An entity implementing `TerrainFieldRenderData` defaults to
`ParchmentVisualIds.WHITE_MARBLE`. The shared field renderer draws fully opaque
calcite, quartz, polished diorite, and related block sprites across terrain
inside the radius. It follows Simply Swords' modern field-effects setting.

White marble is the only terrain palette currently exposed. Other terrain
style IDs are reserved for future API expansion and presently render with the
white-marble palette.
