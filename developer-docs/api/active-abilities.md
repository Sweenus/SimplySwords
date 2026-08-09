# Active abilities

An item participates in the casting system by implementing
`UniqueWeaponActiveAbility`. No separate ability registry or addon network
packet is required.

## Required methods

Most abilities override:

```java
boolean canActivate(WeaponAbilityContext context);
boolean activate(WeaponAbilityContext context);
int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context);
```

Return `true` from `activate` only after the ability has actually started.
Simply Swords applies the cooldown after a successful return.

The interface's default `canActivate` requires a living target. Override it for
self-buffs, movement, area effects, or other targetless abilities. Always check
that the actor is alive, the stack is usable, and any chosen target passes
friendly-fire rules.

## Player input

Implementing the interface automatically makes the item recognizable by the
configurable main-hand and offhand ability hotkeys.

To support ordinary right-click as a fallback:

```java
@Override
protected TypedActionResult<ItemStack> useUniqueWeapon(
        World world,
        PlayerEntity user,
        Hand hand
) {
    return useFromDefaultInput(world, user, hand);
}
```

This hook is available on `UniqueWeaponItem` and `UniqueSwordItem`. Addon
subclasses should not override Minecraft's mapped `use` method directly: an
addon and its Simply Swords dependency are remapped separately, so that method
may no longer override the runtime name in a production jar.

If a player binds the matching ability hotkey, Simply Swords suppresses this
default-input path to prevent two casts from one action.

The hotkeys begin unbound. Players choose them in Minecraft's controls screen.

## Understanding the context

`WeaponAbilityContext` contains:

- `world()` — authoritative `ServerWorld`;
- `stack()` — the weapon being used;
- `actor()` — the entity physically casting the ability;
- `sourcePlayer()` — an owning player for delegated casts, otherwise null;
- `target()` — optional target;
- `origin()` and `facing()` — captured cast geometry;
- `hand()` — nullable source hand;
- `activationSource()` — `PLAYER`, `MOB`, `MINION`, or `DELEGATED_EFFECT`.

A direct player cast has the player in `actor()` and **null** in
`sourcePlayer()`. Do not use `sourcePlayer()` as the general caster.

For a player-owned minion, `actor()` is the minion and `sourcePlayer()` may be
the owner. Use both when checking friendly fire.

## Living entities

Ordinary `MobEntity` instances automatically try their main-hand ability against
their AI target. The check interval and chance are controlled by Simply Swords'
non-player ability configuration.

The automatic path:

- requires the feature to be enabled;
- uses the main hand;
- requires a live, valid AI target;
- uses the same awakening check and cooldown dispatcher as players.

A custom `LivingEntity` that does not extend `MobEntity` should call the API
itself:

```java
WeaponAbilityContext context = WeaponAbilityContext.of(
        world,
        weapon,
        caster,
        owner instanceof ServerPlayerEntity player ? player : null,
        target,
        Hand.MAIN_HAND,
        WeaponAbilityActivationSource.MINION
);

if (SimplySwordsAPI.tryActivateWeaponAbility(context)) {
    caster.swingHand(Hand.MAIN_HAND);
}
```

Call `tryActivateWeaponAbility`, not `activate` directly. The API enforces the
awakening lock and applies either a player item cooldown or the non-player
cooldown manager.

For UI or AI preview checks:

```java
boolean ready = SimplySwordsAPI.canActivateWeaponAbility(context);
int cooldownTicks =
        SimplySwordsAPI.getWeaponAbilityCooldownTicks(context);
```

These methods query readiness and the configured duration without starting the
ability. The duration method does not report how many ticks remain on an
existing cooldown.

## Timed and channeled abilities

For an effect that continues over several ticks:

1. Validate and create an ability state in `activate`.
2. Key state by the actor UUID and world.
3. Store a copy of the ability stack plus origin, facing, owner, and target IDs
   needed later.
4. Tick the state from the addon's server tick callback.
5. Remove it on completion, actor death, world mismatch, or another explicit
   cancellation condition.

This model works for players and other living entities. The
`PlayerWeaponAbilityChannelManager` only models held player input and should not
be the sole driver of shared gameplay.

Use the remap-safe `UniqueWeaponItem` hooks when held-input behavior is part of
the design:

```java
protected int getUniqueWeaponMaxUseTime(ItemStack stack);
protected void tickUniqueWeaponUse(
        World world, LivingEntity user, ItemStack stack, int remainingTicks
);
protected ItemStack finishUniqueWeaponUse(
        ItemStack stack, World world, LivingEntity user
);
protected void stopUniqueWeaponUse(
        ItemStack stack, World world, LivingEntity user, int remainingTicks
);
```

The default maximum use time is zero. Override only the hooks needed by the
weapon, and keep authoritative channel state on the server.

## Cooldowns and refreshes

Use `SimplySwordsAPI.tryActivateWeaponAbility` for normal casts. It uses the
player's `ItemCooldownManager` for a `ServerPlayerEntity` and
`WeaponAbilityCooldownManager` for other actors.

If an addon intentionally clears or changes a non-player cooldown, it currently
needs the lower-level manager. Keep that dependency isolated because the
manager is not part of the primary API package.

## Compatibility rules

- Perform gameplay on the server.
- Send or spawn visuals from authoritative state.
- Avoid casting `actor()` to `PlayerEntity`.
- Use the context stack rather than assuming the main hand.
- Use UUIDs rather than keeping live entity references in long-lived state.
- Check both actor and delegated owner when selecting hostile targets.
