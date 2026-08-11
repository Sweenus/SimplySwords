package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.render.ObserverStatusVisualStyle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ObserverStatusVisualRegistry {

    private static final Map<Identifier, ObserverStatusVisualStyle> STYLES = new LinkedHashMap<>();

    private ObserverStatusVisualRegistry() {
    }

    public static synchronized void register(Identifier effectId, ObserverStatusVisualStyle style) {
        if (effectId == null || style == null) {
            throw new IllegalArgumentException("Observer status visual ID and style cannot be null");
        }
        STYLES.put(effectId, style);
        ObserverStatusEffectSyncRegistry.register(effectId);
    }

    public static synchronized Map<Identifier, ObserverStatusVisualStyle> registeredStyles() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(STYLES));
    }
}
