package net.sweenus.simplyswords.api.ability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class UniqueAbilityTuning {
    private final UniqueAbilityDefinition definition;
    private final Map<UniqueAbilityKey<?>, Object> values;

    private UniqueAbilityTuning(UniqueAbilityDefinition definition, Map<UniqueAbilityKey<?>, Object> values) {
        this.definition = definition;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public UniqueAbilityDefinition definition() {
        return definition;
    }

    public static Builder builder(UniqueAbilityDefinition definition) {
        return new Builder(definition);
    }

    public <T> T get(UniqueAbilityKey<T> key) {
        ensureSupported(key);
        Object value = values.get(key);
        return value == null ? key.defaultValue() : key.type().cast(value);
    }

    private void ensureSupported(UniqueAbilityKey<?> key) {
        if (!definition.supports(key)) {
            throw new IllegalArgumentException("unsupported key " + (key == null ? "null" : key.id()));
        }
    }

    public static final class Builder {
        private final UniqueAbilityDefinition definition;
        private final Map<UniqueAbilityKey<?>, Object> values = new LinkedHashMap<>();

        Builder(UniqueAbilityDefinition definition) {
            this.definition = Objects.requireNonNull(definition);
            for (UniqueAbilityKey<?> key : definition.keys()) {
                values.put(key, key.defaultValue());
            }
        }

        public UniqueAbilityDefinition definition() {
            return definition;
        }

        public <T> T get(UniqueAbilityKey<T> key) {
            ensureSupported(key);
            return key.type().cast(values.get(key));
        }

        public <T> Builder set(UniqueAbilityKey<T> key, T value) {
            ensureSupported(key);
            values.put(key, key.validate(Objects.requireNonNull(value)));
            return this;
        }

        public Builder add(UniqueAbilityKey<Double> key, double amount) {
            return set(key, get(key) + amount);
        }

        public Builder add(UniqueAbilityKey<Integer> key, int amount) {
            return set(key, get(key) + amount);
        }

        public Builder multiply(UniqueAbilityKey<Double> key, double factor) {
            return set(key, get(key) * factor);
        }

        private void ensureSupported(UniqueAbilityKey<?> key) {
            if (!definition.supports(key)) {
                throw new IllegalArgumentException("unsupported key " + (key == null ? "null" : key.id()));
            }
        }

        UniqueAbilityTuning build() {
            return new UniqueAbilityTuning(definition, values);
        }
    }
}
