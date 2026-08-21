# Combat and damage

Choose a damage path deliberately. The helpers differ in iframe behavior and in
which weapon integrations they invoke.

## Scaling a unique ability

The built-in combined scaler compares attack damage with optional spell power
and uses the greater result:

```java
float baseDamage = SimplySwordsAPI.scaleAbilityDamage(
        SpellScalingProfile.ARCANE,
        actor,
        stack,
        0.8F, // attack-damage multiplier
        0.8F  // spell-power multiplier
);
```

This helper also applies awakening effect scaling. Do not scale its result
again.

Use `SpellScalingProfile` for new code. Legacy string overloads remain for
binary/source compatibility, but unknown strings fall back to Arcane.

| Profile | Spell Power Attributes (Fabric) | Iron's Spells (NeoForge) |
| --- | --- | --- |
| `FIRE` | Fire | Fire |
| `FROST` | Frost | Ice |
| `LIGHTNING` | Lightning | Lightning |
| `ARCANE` | Arcane | Ender |
| `SOUL` | Soul | Blood |
| `HEALING` | Healing | Holy |
| `NATURE` | Healing | Nature |
| `EVOCATION` | Arcane | Evocation |
| `ELDRITCH` | Soul | Eldritch |

The helper accepts any `LivingEntity`, so player and mob casts use the same
path. If the relevant compatibility mod or attribute is unavailable, attack
damage remains the fallback.

### Addon-defined scaling profiles

Addons can register a logical profile without extending `SpellScalingProfile`:

```java
Identifier solar = new Identifier("exampleaddon", "solar");
SimplySwordsAPI.registerSpellScalingDefinition(new SpellScalingDefinition(
        solar,
        new SpellScalingTarget(
                new Identifier("examplemagic", "solar"),
                "school.examplemagic.solar"
        ),
        new SpellScalingTarget(
                new Identifier("ironsaddon", "solar"),
                "school.ironsaddon.solar"
        )
));

float damage = SimplySwordsAPI.scaleAbilityDamage(
        solar, actor, stack, 0.8F, 0.8F
);
```

Register the definition during common initialization. The corresponding Spell
Power or Iron's addon must register each external school before Simply Swords
tries to resolve it. Either target may be `null` when the profile supports only
one backend. Re-registering an identical definition is harmless; registering a
different definition for an existing ID throws instead of silently overriding
another addon.

On NeoForge, Iron's Spells uses one shared configurable base power for all
abilities:

```text
spellScaling * ironsSpellBasePower * genericSpellPower * schoolSpellPower
```

`ironsSpellBasePower` defaults to `2.17`. The per-ability `spellScaling` argument
remains the coefficient that distinguishes small repeated hits from major
impacts. On Fabric, Spell Power Attributes keeps its native coefficient-based
calculation and does not use this shared base value.

For a fixed full-strength value that should compete with spell power, use
`SimplySwordsAPI.scaleAbilityValue(profile, actor, stack, fullValue,
spellScaling)`. It applies unique awakening after choosing the larger value.

## Ability targeting helpers

Use the facade helpers instead of depending on built-in weapon or world-manager
classes:

```java
LivingEntity aimed = SimplySwordsAPI.findLenientAbilityTarget(
        player,
        range,
        target -> SimplySwordsAPI.isValidAbilityTarget(target, player)
);

Optional<LivingEntity> closest = SimplySwordsAPI.findClosestAbilityTarget(
        actor, range, width
);

List<LivingEntity> chain = SimplySwordsAPI.findAbilityChainTargets(
        world, actor, firstTarget, count, jumpRange
);
```

`findLenientAbilityTarget` first uses the direct crosshair target, then tests a
slightly expanded ray against candidates accepted by the predicate.
`findClosestAbilityTarget` searches a forward box but does not apply hostility
or friendly-fire rules; validate its result with `isValidAbilityTarget`.
Chain selection includes the first target and avoids repeats while applying
friendly-fire checks to later targets.

For bolt-like magic damage:

```java
boolean damaged = SimplySwordsAPI.applyAbilityBoltDamage(
        world, actor, stack, target, damage
);
```

This validates the target, applies ability-damage enchantments, bypasses
iframes, and suppresses weapon implicits. It is server-only. For a simple visual
ring of server-spawned particles, use
`spawnAbilityOrbitParticles(world, centre, particle, radius, count)`.

## Scaling gem-power damage

Gem powers must use the gem awakening curve rather than the unique-ability
curve:

```java
float damage = HelperMethods.gemPowerScaledDamage(
        SpellScalingProfile.NATURE,
        actor,
        stack,
        0.8F,
        1.6F
);
```

For fixed values, use `gemPowerScaledValue`. Both helpers choose the greater of
the attack/fixed result and spell result, then call
`AwakeningApi.scaleGemPower`. Runic and other non-awakenable weapons therefore
retain full gem-power values.

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
SimplySwordsAPI.isValidAbilityTarget(target, actor);
```

This accounts for self-targeting, creative and spectator players, teams, player
PVP, configured entity exclusions, tameable ownership, supported party
integration, and Simply Swords minions.

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

## Iron's cooldown reduction

Wielder-owned weapon cooldowns use Iron's Spells 'n Spellbooks cooldown
reduction on Forge when `compatEnableIronsCooldownReduction` is enabled. The
option defaults to enabled. Fabric and installations without Iron's preserve
the configured base cooldown.

All unique weapons opt in by default. A weapon can opt out by overriding
`usesSpellCooldownReduction(ItemStack)` to return `false`.

Use `SimplySwordsAPI.getEffectiveWeaponCooldownTicks` for custom passive or
proc timers and `SimplySwordsAPI.setWeaponCooldown` when committing an item
cooldown. Pass the configured base duration; the shared helper applies
reduction exactly once.

Addon tooltips and HUDs can call
`SimplySwordsClientAPI.getEffectiveWeaponCooldownTicks`; the legacy two-argument
`appendAbilityCooldownTooltip` overload remains only for compatibility and
cannot resolve a player-specific value without a stack.

## Explicit magical ability damage

For damage that is explicitly magical rather than a synthetic weapon hit, use:

```java
boolean damaged = SimplySwordsAPI.applyAbilityMagicDamage(
        world, actor, stack, target, damage, SpellScalingProfile.FROST
);
```

The identifier overload supports addon-defined profiles. On Forge this uses
the mapped Iron's school damage type and applies Iron's generic and matching
school resistance. On Fabric it retains ordinary indirect-magic behavior.
The method respects normal iframes;
`applyAbilityMagicDamageThroughIframes` is the narrowly scoped bypass variant.
Neither method runs weapon implicits, enchantment hit callbacks, or sword
`postHit`.
