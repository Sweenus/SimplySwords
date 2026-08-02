package net.sweenus.simplyswords.api;

//
// Read-only client presentation state for an observer-synchronized status effect.
// This does not represent authoritative gameplay state and is not inserted into
// the observed entity's vanilla status-effect map.
//
public record ObserverStatusEffectSnapshot(int amplifier,
                                           int remainingDuration,
                                           boolean infinite,
                                           boolean ambient,
                                           boolean showParticles,
                                           boolean showIcon) {
}
