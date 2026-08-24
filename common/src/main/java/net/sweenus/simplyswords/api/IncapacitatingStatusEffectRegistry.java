package net.sweenus.simplyswords.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class IncapacitatingStatusEffectRegistry {

    private static final Set<Identifier> EFFECTS = new LinkedHashSet<>();

    private IncapacitatingStatusEffectRegistry() {
    }

    public static synchronized void register(Identifier effectId) {
        if (effectId == null) throw new IllegalArgumentException("Incapacitating status effect ID cannot be null");
        EFFECTS.add(effectId);
    }

    public static synchronized boolean isIncapacitated(LivingEntity entity) {
        if (entity == null || EFFECTS.isEmpty()) return false;
        return entity.getStatusEffects().stream()
                .map(instance -> Registries.STATUS_EFFECT.getId(instance.getEffectType().value()))
                .anyMatch(EFFECTS::contains);
    }

    public static synchronized Set<Identifier> registeredEffects() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(EFFECTS));
    }
}
