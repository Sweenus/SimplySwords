# Control and status visuals

Simply Swords exposes common-side registrations for addon status effects that
need server-authoritative control or reusable client presentation. Register the
effect IDs during common initialization.

## Incapacitating effects

Call `SimplySwordsAPI.registerIncapacitatingStatusEffect(effectId)` for an
effect that must prevent a living entity from moving or acting. While a
registered effect is active, Simply Swords suppresses movement and jumping,
mob AI, attacks, item and block interactions, Better Combat requests, and
weapon ability activation. Ongoing item use is stopped. Falling and camera/UI
control are retained.

The addon still owns and registers the `StatusEffect`; this API only supplies
the shared enforcement behavior.

## Observer-driven entity visuals

`SimplySwordsAPI.registerObserverStatusVisual(effectId, style)` both opts the
effect into observer synchronization and registers a client renderer. Use an
`ObserverStatusVisualStyle` with:

- `STATIC_STREAKS` for short blocky lightning crawling around the entity;
- `LIGHTNING_ROD` for a vanilla lightning-rod model with attached arcs.

The renderer reads `ObserverStatusEffectClientApi`, so visuals work for remote
players and mobs even when vanilla does not put their effects in the local
effect map.

## Local storm presentation

`SimplySwordsAPI.registerLocalStormStatusEffect(effectId)` opts an effect into
observer synchronization and treats it as a presentation-only storm marker.
Only a client whose local player has the marker receives full rain and thunder
gradients. The strength fades in and out over ten ticks and is combined with,
rather than replacing, real weather. No server weather, block, fire, or mob
rules are changed; vanilla biome and dimension precipitation behavior remains.

## Movement intent

The client sends change-driven movement-key intent with a periodic heartbeat.
Server ability code can call
`SimplySwordsAPI.getPlayerMovementIntent(serverPlayer)` and receive a
`PlayerMovementIntent` whose `forward` and `strafe` axes are each `-1`, `0`, or
`1`. Positive strafe means right. Stale input resolves to `PlayerMovementIntent.NONE`.
