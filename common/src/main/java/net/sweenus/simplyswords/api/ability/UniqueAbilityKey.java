package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class UniqueAbilityKey<T> {
    private final Identifier id;
    private final Class<T> type;
    private final T defaultValue;
    private final UnaryOperator<T> validator;

    private UniqueAbilityKey(Identifier id, Class<T> type, T defaultValue, UnaryOperator<T> validator) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.validator = Objects.requireNonNull(validator);
    }

    public static UniqueAbilityKey<Integer> integer(Identifier id, int defaultValue, int minimum, int maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("minimum exceeds maximum");
        }
        return new UniqueAbilityKey<>(id, Integer.class, Math.clamp(defaultValue, minimum, maximum),
                value -> Math.clamp(value, minimum, maximum));
    }

    public static UniqueAbilityKey<Double> decimal(Identifier id, double defaultValue, double minimum, double maximum) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("invalid range");
        }
        return new UniqueAbilityKey<>(id, Double.class, Math.clamp(defaultValue, minimum, maximum),
                value -> Math.clamp(Double.isFinite(value) ? value : defaultValue, minimum, maximum));
    }

    public static UniqueAbilityKey<Boolean> flag(Identifier id, boolean defaultValue) {
        return new UniqueAbilityKey<>(id, Boolean.class, defaultValue, UnaryOperator.identity());
    }

    public static <T> UniqueAbilityKey<T> value(Identifier id, Class<T> type, T defaultValue) {
        return new UniqueAbilityKey<>(id, type, defaultValue, UnaryOperator.identity());
    }

    public Identifier id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }

    T validate(Object value) {
        return validator.apply(type.cast(value));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UniqueAbilityKey<?> key && id.equals(key.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
