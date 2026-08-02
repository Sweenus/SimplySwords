# API reference

Use `SimplySwordsAPI` as the preferred common-side facade and
`SimplySwordsClientAPI` for client-only hooks.

## Systems

| System | Main types | Guide |
| --- | --- | --- |
| Active abilities | `UniqueWeaponActiveAbility`, `WeaponAbilityContext`, `WeaponAbilityActivationSource` | [Active abilities](active-abilities.md) |
| Awakening | `AwakeningApi`, `AwakeningProfile`, `AwakeningProfileRegistry` | [Awakening](awakening.md) |
| Branching forms | `AwakeningFormFamily`, `AwakeningFormStage`, `AwakeningFormRoute`, `AwakeningFormHandler`, `AwakeningFormRegistry` | [Awakening forms](awakening-forms.md) |
| Gem sockets | `AdditionalGemSocketApi`, `GemPowerComponent` | [Gem sockets and powers](gem-sockets-and-powers.md) |
| Gem powers | `GemPower`, its typed subclasses, `GemPowerRegistry` | [Gem sockets and powers](gem-sockets-and-powers.md) |
| Unique loot | `UniqueLootRegistry`, `SimplySwordsAPI.registerUniqueLoot` | [Unique loot](unique-loot.md) |
| Weapon implicits | `WeaponImplicitDefinition`, `WeaponImplicitRegistry` | [Weapon implicits](weapon-implicits.md) |
| Weapon hits | `DelegatedWeaponHitContext`, hit helpers on `SimplySwordsAPI` | [Combat and damage](combat-and-damage.md) |
| Remnant transformations | `SimplySwordsAPI.registerTransformation` | [Contained Remnants](contained-remnants.md) |
| Battle Standards | `SimplySwordsAPI.spawnBattleStandard` | [Battle Standards](battle-standards.md) |
| Observer status effects | `SimplySwordsAPI.registerObserverSyncedStatusEffect`, `ObserverStatusEffectClientApi` | [Observer status effects](observer-status-effects.md) |
| Client integration | `SimplySwordsClientAPI` | [Client integration](client-integration.md) |

## Preferred facade methods

`SimplySwordsAPI` exposes:

- ability validation, activation, and cooldown dispatch;
- awakening profile and form-family registration;
- unique pity-loot registration;
- gem socket hooks;
- weapon type and implicit registration;
- synthetic and delegated weapon hits;
- Contained Remnant transformations;
- observer status-effect registration for remote client presentation.

Registry classes expose extra lookup operations needed by tooltips or advanced
integrations. Prefer the facade when it offers the same operation.

## Lower-level and legacy methods

- `spawnBattleStandard` is a legacy public gameplay helper rather than a
  registry. Its behavior is documented separately.
- Deprecated `canActivateFromEntity`, `activateFromEntity`, and
  `getEntityActivationCooldownTicks` methods remain for older abilities. New
  code should use `WeaponAbilityContext`.
- `applyEntityWeaponPostHit` runs post-hit integrations without dealing damage.
  Most abilities should use a normal damage call or one of the complete
  synthetic hit methods instead.
- Classes outside the API and extension packages may change as built-in
  abilities evolve.
