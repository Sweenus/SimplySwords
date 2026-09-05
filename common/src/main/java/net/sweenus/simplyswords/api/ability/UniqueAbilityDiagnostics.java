package net.sweenus.simplyswords.api.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import java.util.Map;

public interface UniqueAbilityDiagnostics {
    UniqueAbilityDiagnostics NONE = new UniqueAbilityDiagnostics() {
    };

    default boolean isActive(LivingEntity actor) {
        return false;
    }

    default void onComposed(UniqueAbilityExecution execution, Map<UniqueAbilityKey<?>, Object> base) {
    }

    default void onRoll(LivingEntity actor, Identifier abilityId, String label,
                        double chance, double roll, boolean passed) {
    }
}
