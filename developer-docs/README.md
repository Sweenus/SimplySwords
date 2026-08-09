# Simply Swords addon developer documentation

These documents explain how an addon can build weapons that behave like Simply
Swords' built-in unique weapons. They target Minecraft 1.21.1 and Java 21.

Start here:

1. [Set up the dependency](getting-started.md).
2. Follow the [complete unique weapon walkthrough](unique-weapon-walkthrough.md).
3. Use the [API reference](api/README.md) when adding more advanced behavior.
4. Finish with the [testing checklist](testing-checklist.md).

## What counts as a supported extension point?

The primary API lives in:

- `net.sweenus.simplyswords.api`
- `net.sweenus.simplyswords.client.api`

The following classes are also intended for addon use:

- `UniqueWeaponItem`
- `UniqueSwordItem`
- `UniqueWeaponActiveAbility`
- `GemPower`, `RunicGemPower`, `RunefusedGemPower`, and `NetherGemPower`
- `GemPowerRegistry`
- the parchment render-data interfaces and their matching client renderers

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

- [Getting started](getting-started.md)
- [Unique weapon walkthrough](unique-weapon-walkthrough.md)
- [Testing checklist](testing-checklist.md)
- [API index](api/README.md)
- [Active abilities](api/active-abilities.md)
- [Awakening](api/awakening.md)
- [Awakening forms](api/awakening-forms.md)
- [Gem sockets and powers](api/gem-sockets-and-powers.md)
- [Unique loot and pity](api/unique-loot.md)
- [Weapon implicits](api/weapon-implicits.md)
- [Combat and damage](api/combat-and-damage.md)
- [Contained Remnants](api/contained-remnants.md)
- [Battle Standards](api/battle-standards.md)
- [Observer-synchronized status effects](api/observer-status-effects.md)
- [Client integration](api/client-integration.md)
- [Parchment visuals](api/parchment-visuals.md)
