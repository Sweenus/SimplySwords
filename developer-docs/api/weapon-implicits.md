# Weapon implicits

An implicit is a rolled property associated with a weapon type. For example,
longswords can deflect incoming damage and rapiers can pierce armor.

`UniqueWeaponItem`, including `UniqueSwordItem`, initializes its implicit on the
server and appends its tooltip automatically.

## Assigning a built-in weapon type

Register one item directly:

```java
SimplySwordsAPI.registerWeaponType(
        RIFTBRAND.get(),
        Identifier.of("simplyswords", "longsword")
);
```

Or register an item tag:

```java
TagKey<Item> tag = TagKey.of(
        RegistryKeys.ITEM,
        Identifier.of("exampleaddon", "rift_weapons")
);

SimplySwordsAPI.registerWeaponType(
        tag,
        Identifier.of("simplyswords", "longsword")
);
```

Built-in weapon type paths are:

```text
rapier, cutlass, sai, dagger, claymore, longsword, greathammer,
hammer, katana, spear, glaive, halberd, warglaive, chakram,
scythe, greataxe, twinblade
```

For data-driven assignment, place the addon item in a Simply Swords implicit
tag, for example:

```text
data/simplyswords/tags/items/implicit/longsword.json
```

Use `"replace": false` and list the addon's item ID.

## Defining a custom implicit

A definition connects one weapon type to a value range and optional handlers:

```java
Identifier type = Identifier.of("exampleaddon", "riftblade");

WeaponImplicitDefinition definition = new WeaponImplicitDefinition(
        Identifier.of("exampleaddon", "rift_surge"),
        type,
        5,
        15,
        (stack, component, target, source, amount) ->
                amount * (1.0F + component.value() / 100.0F),
        null,
        null,
        component -> Text.translatable(
                "implicit.exampleaddon.rift_surge",
                component.value()
        )
);

SimplySwordsAPI.registerWeaponImplicit(definition);
SimplySwordsAPI.registerWeaponType(RIFTBRAND.get(), type);
```

The optional handlers are:

- `DamageHandler` — modifies outgoing damage;
- `HitHandler` — runs after a successful hit;
- `IncomingDamageHandler` — may cancel damage while the weapon is held;
- `TooltipFormatter` — renders the rolled value.

Only one definition is resolved for a weapon type. Use stable IDs and register
definitions before stacks of that weapon initialize.

## Values

Ordinary weapons roll across the complete inclusive range. A stack whose item
extends `UniqueWeaponItem` rolls in the top 10% of the range.

The roll is stored in the weapon's typed Simply Swords stack state and survives save,
reload, socket changes, and awakening.

## Manual integration

Custom item bases can use:

```java
SimplySwordsAPI.getOrCreateWeaponImplicit(stack);
SimplySwordsAPI.appendWeaponImplicitTooltip(stack, tooltip);
SimplySwordsAPI.appendWeaponImplicitTooltip(stack, tooltip, includeRange);
SimplySwordsAPI.applyWeaponImplicitDamage(stack, target, source, amount);
SimplySwordsAPI.applyWeaponImplicitOnHit(stack, target, attacker, damage);
```

Do not apply a handler manually if the normal Simply Swords hit path already
does so, or it will execute twice.
