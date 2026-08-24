# Gem sockets and powers

Simply Swords has runefused and netherfused gem sockets. Unique weapons receive
both socket types; runic weapons use their existing runic handling.

## Native unique sockets

`UniqueWeaponItem.inventoryTick` creates both sockets at a 100% chance when
unique sockets are enabled. It also:

- runs equipped gem-power ticks from either hand;
- accepts inventory-click socketing and replacement;
- returns a displaced gem to the player;
- invokes post-hit gem powers;
- appends socket information through Simply Swords' tooltip integration.

An addon extending `UniqueWeaponItem` or `UniqueSwordItem` should not duplicate
these calls.

## Adding sockets to unrelated items

Users can select additional vanilla or modded items through Simply Swords'
`additionalGemSocketItems` config list. Entries accept an item ID or a tag
prefixed with `#`.

`AdditionalGemSocketApi` provides integration checks:

```java
AdditionalGemSocketApi.isConfigured(stack);
AdditionalGemSocketApi.hasAdditionalSockets(stack);
AdditionalGemSocketApi.isManaged(stack);
AdditionalGemSocketApi.ensureInitialized(stack);
AdditionalGemSocketApi.getTooltipComponent(stack);
```

The persistent additional-socket marker survives later config edits. The system
does not add sockets to stackable items, native unique/runic weapons, or gem
items.

Mixins supplied by Simply Swords handle ordinary configured items. An addon only
needs these methods when it has custom inventory, tooltip, or attack behavior
that bypasses vanilla paths.

For a completely custom native item base, the corresponding facade hooks are:

```java
SimplySwordsAPI.inventoryTickGemSocketLogic(
        stack, world, entity, runicChance, netherChance
);
SimplySwordsAPI.onClickedGemSocketLogic(
        stack, cursorStack, player, cursorReference
);
SimplySwordsAPI.postHitGemSocketLogic(stack, target, attacker);
SimplySwordsAPI.appendTooltipGemSocketLogic(
        stack, tooltipContext, tooltip, tooltipType
);
```

Call each hook from the matching item lifecycle method. Do not add these calls
to a `UniqueWeaponItem` subclass because its base implementation already owns
the lifecycle.

## Creating a custom passive gem power

Choose the base class that matches the gem:

- `RunicGemPower` — powers used by runic gems;
- `RunefusedGemPower` — powers available to runic and runefused gems;
- `NetherGemPower` — netherfused powers;
- `GemPower` — custom combinations using `PowerType`.

Example:

```java
public final class RiftEchoPower extends RunefusedGemPower {
    public RiftEchoPower() {
        super(false);
    }

    @Override
    public void postHit(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker
    ) {
        if (attacker.getWorld().isClient()) {
            return;
        }
        float strength = HelperMethods.gemPowerScaledDamage(
                SpellScalingProfile.ELDRITCH,
                attacker,
                stack,
                0.5F,
                1.0F
        );
        if (strength <= 0.0F) {
            return;
        }
        target.damage(
                SimplySwordsAPI.getWeaponDamageSource(attacker),
                strength
        );
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            List<Text> tooltip,
            TooltipType type,
            boolean isRunic
    ) {
        tooltip.add(Text.translatable("power.exampleaddon.rift_echo"));
    }
}
```

Other public hooks are:

```java
void onSwing(ItemStack stack, ServerWorld world, LivingEntity user, Hand hand);
void inventoryTick(ItemStack stack, World world, LivingEntity user,
                   int slot, boolean selected);
```

Register powers during common initialization before the synchronized custom
registry freezes:

```java
public static final RegistrySupplier<GemPower> RIFT_ECHO =
        GemPowerRegistry.REGISTRY.register(
                Identifier.of("exampleaddon", "rift_echo"),
                RiftEchoPower::new
        );
```

The registry is synchronized to clients. Use stable namespaced IDs and register
the same entries in the same addon version on both sides. A registered power is
automatically included in random gems for every `PowerType` declared by its
base class.

For a custom item path that bypasses Simply Swords' ordinary swing handling,
dispatch socket swing powers with:

```java
SimplySwordsAPI.onWeaponSwing(stack, world, user, hand);
```

`SimplySwordsAPI.getComponent(stack)` returns the stack's current
`GemPowerComponent`. Prefer the higher-level hooks unless custom behavior needs
to inspect it directly.

### Reading a component (breaking change in 1.70.0)

`GemPowerComponent` stores power **IDs**, not registry entries. Before 1.70.0 its
accessors and factories used `RegistryEntry<GemPower>`:

```java
GemPowerComponent c = SimplySwordsAPI.getComponent(stack);

Identifier runicId = c.runicPower();   // was RegistryEntry<GemPower>
GemPower   runic   = c.runic();        // resolves; EMPTY when unset or unknown
boolean    filled  = c.hasRunicSlotFilled();

GemPowerComponent next = GemPowerComponent.create(runicId, netherId);
Identifier rolled = GemPowerRegistry.gemRandomPower(PowerType.RUNEFUSED);
```

`GemPower.EMPTY_ID` is the ID of the empty placeholder, and
`GemPowerRegistry.resolve(id)` performs the lookup behind `runic()` / `nether()`.
To test whether an ID names a real power, use `GemPowerRegistry.REGISTRY.contains(id)`.

Addons built against an earlier version must be recompiled; this cannot be
source- or binary-compatible, because `runicPower()` is a record accessor and
Java cannot overload on return type.

The reason for the change: registry entries reached a component from two
sources that never compare equal — freshly built components held Architectury
`RegistrySupplier` objects, while decoded ones held vanilla
`RegistryEntry.Reference` objects. A component therefore never survived an
encode/decode round trip intact, so storage mods that rebuild a stack from its
component map and match it against storage (Refined Storage, Create, AE2) failed
the match and silently extracted a fuzzy-matched substitute. IDs give correct
value equality, and they also keep `getKey()` off every code path in the
component — that method is ambiguous between Architectury and NeoForge and
throws `IncompatibleClassChangeError` on NeoForge.

For the same reason, do not read powers with `GemPowerRegistry.REGISTRY.getHolder(id)`:
it returns a holder whose `equals` is identity-based, which reintroduces the
problem in your own addon. Store and compare IDs.

## Awakening scaling

Gem powers on awakenable unique weapons use the same unlock threshold and level
curve as unique effects. Check or scale with `AwakeningApi`:

```java
if (!AwakeningApi.areGemPowersActive(stack)) return;

float value = AwakeningApi.scaleGemPower(stack, fullValue);
int ticks = AwakeningApi.scaleGemPowerDuration(stack, fullDuration);
```

Runic and other non-awakenable weapons always receive full values.

Use `gemPowerScaledDamage` for attack-derived damage and
`gemPowerScaledValue` when the non-spell side is a fixed value. They select the
greater result and apply gem awakening once. Do not pass their result through
`scaleGemPower` again. The available loader-neutral profiles and their Fabric
and NeoForge mappings are listed in [Combat and damage](combat-and-damage.md).
