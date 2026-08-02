package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

//
// Opt-in registry for status effects whose presentation state should be
// available to clients observing another living entity.
//
public final class ObserverStatusEffectSyncRegistry {

    private static final Set<Identifier> REGISTERED_EFFECTS = new LinkedHashSet<>();

    private ObserverStatusEffectSyncRegistry() {
    }

    public static synchronized void register(Identifier effectId) {
        if (effectId == null) {
            throw new IllegalArgumentException("Observer-synchronized status effect ID cannot be null");
        }
        REGISTERED_EFFECTS.add(effectId);
    }

    public static synchronized boolean isRegistered(Identifier effectId) {
        return effectId != null && REGISTERED_EFFECTS.contains(effectId);
    }

    public static synchronized Set<Identifier> registeredEffects() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(REGISTERED_EFFECTS));
    }
}
