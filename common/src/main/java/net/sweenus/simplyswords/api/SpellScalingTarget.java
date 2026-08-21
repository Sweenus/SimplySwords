package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;

import java.util.Objects;

public record SpellScalingTarget(Identifier schoolId, String displayTranslationKey) {
    public SpellScalingTarget {
        Objects.requireNonNull(schoolId, "schoolId");
        displayTranslationKey = displayTranslationKey == null ? "" : displayTranslationKey;
    }
}
