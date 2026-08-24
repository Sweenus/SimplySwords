package net.sweenus.simplyswords.api;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.item.component.AwakeningComponent;

import java.util.Objects;
import java.util.Optional;

public record AwakeningFormStage(
        Identifier id,
        int minimumLevel,
        Item item,
        String translationKey,
        AwakeningFormRarity rarity,
        float modelValue
) {
    public AwakeningFormStage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(rarity, "rarity");
        if (minimumLevel < 0 || minimumLevel > AwakeningComponent.MAX_LEVEL) {
            throw new IllegalArgumentException("Awakening form level must be between 0 and "
                    + AwakeningComponent.MAX_LEVEL + ": " + minimumLevel);
        }
        if (!Float.isFinite(modelValue) || modelValue < 0.0F || modelValue > 1.0F) {
            throw new IllegalArgumentException("Awakening form model value must be between 0 and 1: "
                    + modelValue);
        }
    }

    public AwakeningFormStage(Identifier id, int minimumLevel, Item item, float modelValue) {
        this(id, minimumLevel, item, null, AwakeningFormRarity.UNCHANGED, modelValue);
    }

    public Optional<String> displayTranslationKey() {
        return Optional.ofNullable(translationKey);
    }
}
