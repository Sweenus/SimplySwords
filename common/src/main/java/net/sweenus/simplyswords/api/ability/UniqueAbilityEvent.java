package net.sweenus.simplyswords.api.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record UniqueAbilityEvent(
        UniqueAbilityExecution execution,
        UniqueAbilityPhase phase,
        Identifier eventId,
        @Nullable LivingEntity target,
        int affectedTargets,
        double magnitude
) {
}
