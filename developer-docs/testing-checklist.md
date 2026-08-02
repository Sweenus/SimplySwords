# Testing checklist

Use this checklist before releasing an addon unique weapon.

## Startup and registration

- Start a Fabric client.
- Start a NeoForge client.
- Start and join a dedicated server for each supported loader.
- Confirm the item, model, translation, and attributes load without registry or
  classloading errors.
- Confirm the game also starts with optional integrations absent. Keep declared
  hard dependencies, including Simply Tooltips, installed.

## Ability casting

- Test the main-hand hotkey.
- Test the offhand hotkey and dual wielding.
- Test right-click if the item implements the fallback.
- Test with and without a valid target.
- Test cooldown start, expiry, death, dimension change, and weapon swapping.
- Give the weapon to a normal `MobEntity` and verify main-hand automatic use.
- If custom living entities call the API directly, verify their context and
  cooldown behavior.
- Confirm particles and sounds do not execute twice.

## Awakening and Runic Forge

- Test fresh natural loot at levels 0, 4, 7, and 8.
- Confirm attack damage and attack speed rise to their exact full values.
- Confirm the ability is sealed below its configured unlock level.
- Confirm ability and gem-power values use the intended awakening multiplier.
- Add and remove all eight Runic Tablets.
- Add, remove, and replace both gem types.
- Close the forge without taking the preview and check for duplication.
- Disable ordinary awakening and verify legacy/full-awakening behavior.

For a form family, also test every route while upgrading and downgrading across
the selection level. Restart the world and confirm the selected route persists.

## Combat

- Test friendly players, teammates, tamed entities, creative players, and
  spectators.
- Test armor, enchantments, spell scaling, and attack-damage scaling.
- Verify whether each damage instance should respect or bypass iframes.
- Test an actor whose weapon is in the offhand.
- Test delegated effects with both player and non-player owners.

## Loot and data

- Run `/simplyswords loot_test <loot_table> <rolls>` and confirm the addon item
  appears.
- Use `/simplyswords loot_test_chest <loot_table> [count]` to test real chest
  generation and verify the initial awakening level.
- Enable `/simplyswords pity ignore_regions true` when repeatedly placing fresh
  test chests in the same location, then disable it or reconnect afterward.
- Use `/simplyswords pity status`, `set`, and `reset` to verify soft pity, hard
  pity, successful-roll resets, and separate Unique/Tablet progression.
- Confirm disabled-unique configuration excludes the item.
- Verify weapon implicit values and tooltip text survive save/reload.
- Test any Contained Remnant block interaction and disabled-loot handling.
