package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class LocalStormStatusEffectRegistry {

    private static final Set<Identifier> EFFECTS = new LinkedHashSet<>();

    private LocalStormStatusEffectRegistry() {
    }

    public static synchronized void register(Identifier effectId) {
        if (effectId == null) throw new IllegalArgumentException("Local storm status effect ID cannot be null");
        EFFECTS.add(effectId);
        ObserverStatusEffectSyncRegistry.register(effectId);
    }

    public static synchronized Set<Identifier> registeredEffects() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(EFFECTS));
    }
}
