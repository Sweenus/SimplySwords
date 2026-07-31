# Combat and damage

Choose a damage path deliberately. The helpers differ in iframe behavior and in
which weapon integrations they invoke.

## Scaling a unique ability

The built-in combined scaler compares attack damage with optional spell power
and uses the greater result:

```java
float baseDamage = HelperMethods.abilityScaledDamage(
        "arcane",
        actor,
        stack,
        0.8F, // attack-damage multiplier
        0.8F  // spell-power multiplier
);
```

This helper also applies awakening effect scaling. Do not scale its result
again.

Known spell-school strings used by built-in content include `arcane`, `fire`,
`frost`, `healing`, `lightning`, and `soul`. If no compatible spell-power
provider is present, attack damage remains the fallback.

To include weapon enchantments and Simply Swords' player/non-player damage
configuration:

```java
DamageSource source = SimplySwordsAPI.getWeaponDamageSource(actor);
float damage = HelperMethods.applyAbilityDamageEnchantments(
        world, stack, target, source, baseDamage
);
```

## Normal damage: respects iframes

Use Minecraft's ordinary damage call when repeated hits should respect the
target's invulnerability frames:

```java
target.damage(source, damage);
```

Apply enchantment scaling first if the ability should benefit from it. This path
does not automatically simulate a complete weapon hit.

## Synthetic weapon hit: bypasses iframes

Use:

```java
SimplySwordsAPI.applyEntityWeaponHit(stack, target, actor, damage);
```

This method clears `timeUntilRegen` before and after damage. It therefore
bypasses normal iframes.

After successful damage it also runs:

- weapon implicit damage and hit behavior;
- enchantment target-damaged behavior;
- sword `postHit`, including gem powers.

Pass the final desired damage only once. Avoid separately calling the same
post-hit hooks.

## Delegated weapon hit

For a minion, projectile-like helper, or summoned entity acting for an owner:

```java
SimplySwordsAPI.applyDelegatedWeaponHit(
        stack,
        target,
        owner,
        actingEntity,
        damage
);
```

The damage source belongs to the owner while
`SimplySwordsAPI.getDelegatedWeaponHitContext()` exposes the physical actor,
origin, and facing during synchronous post-hit callbacks.

The context is thread-local and only exists while the delegated hit is being
processed. Copy values that must survive beyond the callback.

The delegated method also bypasses iframes and runs the complete synthetic
weapon-hit integrations.

## Post-hit without damage

`applyEntityWeaponPostHit` invokes weapon implicit and item post-hit behavior
without dealing damage. It is mainly used after another system has already
confirmed a non-player melee hit.

Do not pair it with `applyEntityWeaponHit` for the same strike.

## Friendly fire

Before selecting or damaging a target, use:

```java
HelperMethods.checkAbilityTarget(target, actor);
```

This accounts for self-targeting, creative and spectator players, teams, player
PVP, configured entity exclusions, tameable ownership, and supported party
integration.

For a delegated player-owned actor, also reject the owner and check:

```java
context.sourcePlayer() == null
        || target != context.sourcePlayer()
        && HelperMethods.checkFriendlyFire(target, context.sourcePlayer());
```

## Ownership rules

- Use `WeaponAbilityContext.actor()` for position, facing, attributes, and the
  held stack.
- Use `sourcePlayer()` only for ownership, attribution, and an additional
  friendly-fire check.
- A directly casting player is the actor and has no `sourcePlayer`.
- Never assume an ability stack is in the main hand.

