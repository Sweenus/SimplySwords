# Simply Swords addon developer documentation

These documents explain how an addon can build weapons that behave like Simply
Swords' built-in unique weapons. They target Minecraft 1.21.1 and Java 21.

Start here:

1. [Set up the dependency](developer-docs/getting-started.md).
2. Follow the [complete unique weapon walkthrough](developer-docs/unique-weapon-walkthrough.md).
3. Use the [API reference](developer-docs/api/README.md) when adding more advanced behavior.
4. Finish with the [testing checklist](developer-docs/testing-checklist.md).

## What counts as a supported extension point?

The primary API lives in:

- `net.sweenus.simplyswords.api`
- `net.sweenus.simplyswords.client.api`

The following classes are also intended for addon use:

- `UniqueSwordItem`
- `UniqueWeaponActiveAbility`
- `GemPower`, `RunicGemPower`, `RunefusedGemPower`, and `NetherGemPower`
- `GemPowerRegistry`

Classes under `world`, `mixin`, and most of `util` are implementation details.
The reference occasionally mentions a utility when it is the only practical way
to match built-in behavior. Treat those utilities as less stable than the API
package.

## API registration rule

Register addon items normally, then register their Simply Swords integrations
during common initialization. Client model properties and other rendering hooks
belong in the addon's client initializer.

Simply Swords must be a required dependency so its registries and components
exist before addon content is used.

## Documents

- [Getting started](developer-docs/getting-started.md)
- [Unique weapon walkthrough](developer-docs/unique-weapon-walkthrough.md)
- [Testing checklist](developer-docs/testing-checklist.md)
- [API index](developer-docs/api/README.md)
- [Active abilities](developer-docs/api/active-abilities.md)
- [Awakening](developer-docs/api/awakening.md)
- [Awakening forms](developer-docs/api/awakening-forms.md)
- [Gem sockets and powers](developer-docs/api/gem-sockets-and-powers.md)
- [Unique loot and pity](developer-docs/api/unique-loot.md)
- [Weapon implicits](developer-docs/api/weapon-implicits.md)
- [Combat and damage](developer-docs/api/combat-and-damage.md)
- [Contained Remnants](developer-docs/api/contained-remnants.md)
- [Battle Standards](developer-docs/api/battle-standards.md)
- [Client integration](developer-docs/api/client-integration.md)
