# Awakening

Awakening gives a unique weapon eight upgrade levels. The Runic Forge adds or
removes one Runic Tablet per level.

## Opting in

Every `UniqueWeaponItem`, including `UniqueSwordItem`, automatically receives
`AwakeningProfile.DEFAULT`.
Other item classes must register a profile:

```java
SimplySwordsAPI.registerAwakeningProfile(
        MY_WEAPON.get(),
        new AwakeningProfile(
                0.50F, // attack damage at level 0
                0.75F, // attack speed at level 0
                4      // ability unlock level
        )
);
```

The multipliers interpolate linearly to 1.0 at level 8. Constructor values are
clamped to valid ranges. A two-argument constructor is available when only the
dormant damage multiplier and unlock level need changing.

Register profiles during common initialization after items are available.

## Reading and changing progress

Useful `AwakeningApi` methods include:

```java
boolean globallyEnabled = AwakeningApi.isAwakeningSystemEnabled();
boolean usingStoredLevel = AwakeningApi.usesAwakeningProgression(stack);
int level = AwakeningApi.getLevel(stack);
boolean unlocked = AwakeningApi.isAbilityUnlocked(stack);
int unlockLevel = AwakeningApi.getAbilityUnlockLevel(stack);
float strength = AwakeningApi.getEffectMultiplier(stack);
float damageStrength = AwakeningApi.getAttributeMultiplier(stack);
float speedStrength = AwakeningApi.getAttackSpeedMultiplier(stack);
```

Use `AwakeningApi.setLevel` for an explicit level change. It clamps the value and
rebuilds attack attributes.

`AwakeningApi.rebuildAttributes(stack)` reapplies the current level to the
stack's attack attributes without changing that level. Normal profile
registration, initialization, and forge operations already do this.

`AwakeningProfileRegistry.get(itemOrStack)` and `isAwakenable(stack)` provide
lower-level profile lookup. Prefer `AwakeningApi` for effective gameplay state,
especially when global compatibility mode is enabled.

Use the initialization helpers according to how the item was obtained:

```java
AwakeningApi.initializeNaturalDrop(stack);   // level 0
AwakeningApi.initializeFullyAwakened(stack); // level 8
```

Pity loot and Contained Remnant transformations already use natural-drop
initialization.

An awakenable stack with no stored awakening component is treated as a full
level-8 stack unless it is explicitly initialized as a natural drop. This keeps
commands, recipes, and intentional direct rewards backward compatible. Always
call the appropriate initializer in an addon's own acquisition path.

`ensureInitialized` is normally called by `UniqueWeaponItem.inventoryTick`.
Custom base items must either register a profile and call it themselves or make
sure the stack passes through another supported initialization path.

## Scaling ability effects

Scale full-strength values with:

```java
float damage = AwakeningApi.scaleEffect(stack, fullDamage);
int chance = AwakeningApi.scaleChance(stack, fullChance);
```

The multiplier is zero below the profile's ability unlock level and then follows
`level / 8`.

`SimplySwordsAPI.scaleAbilityDamage(...)` already chooses the greater of attack
damage and spell-power scaling and then applies awakening scaling. Do not call
`scaleEffect` on its result a second time.

Use `SimplySwordsAPI.scaleAbilityValue(...)` for a fixed full-strength value
that should compete with spell power. It also applies `scaleEffect` internally.

Gem powers use their own named helpers:

```java
AwakeningApi.areGemPowersActive(stack);
AwakeningApi.getGemPowerMultiplier(stack);
AwakeningApi.scaleGemPower(stack, fullValue);
AwakeningApi.scaleGemPowerDuration(stack, fullTicks);
```

Non-awakenable weapons, including runic weapons, behave at full gem-power
strength.

Damage-bearing gem powers should normally use
`HelperMethods.gemPowerScaledDamage(...)` or `gemPowerScaledValue(...)`. These
helpers select the greater physical/spell result and apply `scaleGemPower`
exactly once.

## Global compatibility mode

When ordinary unique awakening is disabled in Simply Swords' general config:

- ordinary awakenable weapons behave as level 8;
- their stored level is retained;
- they cannot be modified in the Runic Forge;
- form families marked as persistent continue using stored progression.

Use `AwakeningApi.usesAwakeningProgression(stack)` when code needs to know
whether the stack is actively following stored levels. Use `getLevel` for
effective gameplay strength.

## Attribute requirements

Awakening scales the attack damage and attack speed modifiers stored in the
item's default `ATTRIBUTE_MODIFIERS` component. Register the complete level-8
attributes on the item itself. Do not pre-scale the default item settings.

At level 8, the component should match the item's original full attributes
exactly.
