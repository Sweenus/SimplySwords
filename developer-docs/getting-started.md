# Getting started

Simply Swords 1.20.1 uses Java 17 and Architectury. An addon may itself be
multiloader or loader-specific.

## Add the dependency

Simply Swords is available through CurseMaven. Add the repository:

```groovy
repositories {
    maven {
        url = "https://www.cursemaven.com"
        content {
            includeGroup "curse.maven"
        }
    }
}
```

Put the correct Fabric or Forge/NeoForge file ID in `gradle.properties`:

```properties
simply_swords_file_id=REPLACE_WITH_FILE_ID
```

Then add the dependency to the relevant module:

```groovy
dependencies {
    modImplementation "curse.maven:simply-swords-659887:${simply_swords_file_id}"
}
```

File IDs are listed on the
[Simply Swords files page](https://www.curseforge.com/minecraft/mc-mods/simply-swords/files).
Choose a Minecraft 1.20.1 file for the same loader as the module being built.
There is no separate NeoForge 1.20.1 build: one jar serves both loaders, and is
named `simplyswords-forge-neoforge-<version>.jar` to say so. NeoForge 1.20.1
reads the same `META-INF/mods.toml` and keeps the `net.minecraftforge` package
namespace, so the Forge build loads on it unchanged.

CurseMaven does not guarantee useful transitive dependency metadata. Keep the
normal Architectury dependency in an Architectury addon and provide Simply
Swords' required runtime dependencies in the development run configuration.
The authoritative dependency list is in Simply Swords' `fabric.mod.json` and
`mods.toml`.

## Declare the mod dependency

Make Simply Swords a required dependency in the addon's loader metadata. This
prevents the addon from loading without the APIs and typed stack state it uses.

Fabric example:

```json
{
  "depends": {
    "simplyswords": ">=1.63.0",
    "minecraft": "1.20.1"
  }
}
```

Forge/NeoForge 1.20.1 example:

```toml
[[dependencies.exampleaddon]]
modId = "simplyswords"
mandatory = true
versionRange = "[1.63.0,)"
ordering = "AFTER"
side = "BOTH"
```

Use a version range appropriate for the API version against which the addon was
tested.

## Common and client initialization

Common initialization should:

1. Register addon items.
2. Register custom gem powers before registries freeze.
3. Schedule awakening profiles or form families.
4. Register unique loot, weapon types, implicits, and optional remnant recipes.

Client initialization should:

1. Register awakening form model properties.
2. Register addon renderers, particles, and client-only networking.

Never load `net.minecraft.client` classes from a common or dedicated-server
entrypoint.
