# Creating a unique weapon

This walkthrough creates `exampleaddon:riftbrand`. It uses one item, an instant
area ability, awakening, both gem sockets, a longsword implicit, and pity loot.

Imports are omitted where the class name makes them clear.

## 1. Register the item

Create an item class that extends `UniqueSwordItem` and implements
`UniqueWeaponActiveAbility`:

```java
public final class RiftbrandItem extends UniqueSwordItem
        implements UniqueWeaponActiveAbility {

    public RiftbrandItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }
}
```

`UniqueSwordItem` is the semantic base for unique swords. It extends
`UniqueWeaponItem`, which is the shared addon base for swords, staves, and other
unique weapons. Both bases make the weapon fireproof and supply:

- awakening initialization and Runic Forge support;
- runefused and netherfused sockets;
- socket ticking, inventory socketing, and post-hit gem powers;
- weapon implicit initialization and tooltip lines;
- form-aware names and rarity.

Extend `UniqueWeaponItem` directly for a unique that should not be described as
a sword. Its constructor and attribute requirements are the same. External
addons should use the protected `UniqueWeaponItem` hooks documented below
instead of overriding mapped Minecraft item methods directly; those mapped
names are not stable across a separately remapped addon jar.

Register it using the addon's normal item registry. This Architectury example
uses Netherite as a placeholder material:

```java
public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create("exampleaddon", RegistryKeys.ITEM);

public static final RegistrySupplier<RiftbrandItem> RIFTBRAND =
        ITEMS.register("riftbrand", () -> new RiftbrandItem(
                ToolMaterials.NETHERITE,
                LegacyWeaponAttributes.configure(
                        new Item.Settings(),
                        7,
                        -2.6F
                )
        ));
```

`LegacyWeaponAttributes.configure` bridges the settings-based 1.21 API to
Minecraft 1.20.1's constructor-based sword attributes. The damage argument is
added to the material's attack damage, and speed modifies the player's base
attack speed. Use the addon's own configuration values in a real weapon.

Call `ITEMS.register()` from common initialization.

## 2. Add the ability

Right-click fallback is optional but recommended. The same ability is then
available through Simply Swords' configurable main-hand and offhand hotkeys:

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

The default `canActivate` requires a target. Riftbrand is an area burst, so it
must explicitly allow targetless player use:

```java
@Override
public boolean canActivate(WeaponAbilityContext context) {
    return context != null
            && context.world() != null
            && context.actor() != null
            && context.actor().isAlive()
            && context.stack() != null
            && !context.stack().isEmpty()
            && context.stack().getDamage() < context.stack().getMaxDamage() - 1;
}
```

Activation runs on the server for players and living entities:

```java
@Override
public boolean activate(WeaponAbilityContext context) {
    LivingEntity actor = context.actor();
    ItemStack stack = context.stack();
    ServerWorld world = context.world();
    DamageSource source = SimplySwordsAPI.getWeaponDamageSource(actor);

    float damage = SimplySwordsAPI.scaleAbilityDamage(
            SpellScalingProfile.ARCANE, actor, stack, 0.8F, 1.6F
    );

    List<LivingEntity> targets = world.getEntitiesByClass(
            LivingEntity.class,
            actor.getBoundingBox().expand(4.0),
            target -> target.isAlive()
                    && SimplySwordsAPI.isValidAbilityTarget(target, actor)
    );

    for (LivingEntity target : targets) {
        float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                world, stack, target, source, damage
        );
        SimplySwordsAPI.applyEntityWeaponHit(stack, target, actor, finalDamage);
    }

    world.spawnParticles(
            ParticleTypes.REVERSE_PORTAL,
            actor.getX(), actor.getBodyY(0.5), actor.getZ(),
            40, 1.5, 0.5, 1.5, 0.05
    );
    world.playSound(
            null, actor.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE,
            actor.getSoundCategory(), 0.8F, 1.2F
    );
    return true;
}

@Override
public int getActivationCooldownTicks(
        ItemStack stack,
        WeaponAbilityContext context
) {
    return 12 * 20;
}
```

`tryActivateWeaponAbility` handles awakening locks and the correct player or
non-player cooldown. The synthetic weapon hit also runs enchantment post-hit,
weapon implicit, and item post-hit behavior while bypassing target iframes. See
[Combat and damage](api/combat-and-damage.md) before choosing that behavior.

For a delayed or channeled ability, store server-side state by actor UUID and
tick it from the addon's server tick callback. Do not make the main effect
depend on a player-only channel callback.

## 3. Register awakening and integrations

Because Riftbrand extends `UniqueSwordItem`, and therefore `UniqueWeaponItem`,
it automatically receives the
default awakening profile: 50% dormant attack damage, 75% dormant attack speed,
and ability unlock at level 4.

Register an explicit profile only when different values are wanted:

```java
SimplySwordsAPI.registerAwakeningProfile(
        RIFTBRAND.get(),
        new AwakeningProfile(0.60F, 0.85F, 3)
);
```

The sample ability uses `scaleAbilityDamage`, which already scales its result
with awakening. Do not pass that result through `AwakeningApi.scaleEffect`
again.

Choose a `SpellScalingProfile` that matches the ability. The platform bridge
maps it to Spell Power Attributes on Fabric and Iron's Spells on NeoForge. As a
starting point, built-in abilities generally use a spell multiplier around
twice their attack multiplier so dedicated spell investment can compete with a
weapon build. Tune both values in the weapon's config.

On NeoForge, that spell multiplier is combined with the single shared
`general.ironsSpellBasePower` setting and the actor's generic and mapped-school
Iron's Spells attributes. Addons do not need a separate Iron's base-power field
for each ability. Fabric continues to interpret the same multiplier through
Spell Power Attributes.

Add it to the pity-controlled unique pool and give it the built-in longsword
implicit:

```java
SimplySwordsAPI.registerUniqueLoot(RIFTBRAND.get(), 1);
SimplySwordsAPI.registerWeaponType(
        RIFTBRAND.get(),
        new Identifier("simplyswords", "longsword")
);
```

Perform these calls during common initialization after the item has been
registered.

## 4. Add resources

At minimum, add:

```text
assets/exampleaddon/lang/en_us.json
assets/exampleaddon/models/item/riftbrand.json
assets/exampleaddon/textures/item/riftbrand.png
data/simplytooltips/tags/items/simply_swords_compat.json
```

Language example:

```json
{
  "item.exampleaddon.riftbrand": "Riftbrand",
  "item.exampleaddon.riftbrand.ability": "Rift Pulse",
  "item.exampleaddon.riftbrand.description": "Release a burst of rift energy."
}
```

The Simply Tooltips compatibility tag gives an addon item the standard lore and
stats layout:

```json
{
  "replace": false,
  "values": [
    "exampleaddon:riftbrand"
  ]
}
```

For the built-in unique layout and awakening frame, register the addon namespace
with `SimplySwordsClientAPI.registerUniqueTooltipNamespace` during client
initialization. See [Client integration](api/client-integration.md).

Override the remap-safe tooltip hook to add the ability description, then call
`appendSharedUniqueWeaponTooltip(...)` so implicit and Simply Swords
integrations remain:

```java
@Override
protected void appendUniqueWeaponTooltip(
        ItemStack stack,
        World world,
        List<Text> tooltip,
        TooltipContext context
) {
    tooltip.add(Text.empty());
    tooltip.add(Text.translatable("item.exampleaddon.riftbrand.ability"));
    tooltip.add(Text.translatable("item.exampleaddon.riftbrand.description"));
    appendSharedUniqueWeaponTooltip(stack, world, tooltip, context);
}
```

`UniqueWeaponItem`'s default dynamic documentation links point at Simply Swords.
Override its protected `generateDynamicTooltip` method if the addon publishes
its own Oracle Index pages or config navigation. It is also valid to leave that
method empty while retaining the ordinary tooltip lines.

## 5. Parity checklist

A built-in-equivalent unique should normally have:

- a fireproof, damageable item with configured attributes;
- `UniqueWeaponItem` behavior or equivalent calls to each subsystem;
- player hotkey and optional right-click activation;
- server-authoritative behavior that works for players and mobs;
- awakening-scaled attributes, ability values, and gem powers;
- runefused and netherfused sockets;
- a weapon type and implicit;
- pity-pool registration if it should appear in chests;
- tooltip, model, texture, translations, sounds, and particles;
- Fabric, NeoForge, and dedicated-server testing.

Branching forms, custom gem powers, and Contained Remnant transformations are
optional. Their APIs are documented separately.
