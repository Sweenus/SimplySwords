package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record SpellScalingDefinition(Identifier id,
                                     @Nullable SpellScalingTarget spellPowerTarget,
                                     @Nullable SpellScalingTarget ironsTarget) {
    public SpellScalingDefinition {
        Objects.requireNonNull(id, "id");
        if (spellPowerTarget == null && ironsTarget == null) {
            throw new IllegalArgumentException("At least one spell scaling target is required for " + id);
        }
    }
}
