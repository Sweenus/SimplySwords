package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class UniqueAbilityDefinition {
    private final Identifier id;
    private final UniqueAbilityKind kind;
    private final Map<Identifier, UniqueAbilityKey<?>> keys;
    private final Set<Identifier> events;
    private final UniqueAbilityKey<Integer> cooldownKey;

    private UniqueAbilityDefinition(Builder builder) {
        this.id = builder.id;
        this.kind = builder.kind;
        this.keys = Collections.unmodifiableMap(new LinkedHashMap<>(builder.keys));
        this.events = Collections.unmodifiableSet(new LinkedHashSet<>(builder.events));
        this.cooldownKey = builder.cooldownKey;
    }

    public static Builder builder(Identifier id, UniqueAbilityKind kind) {
        return new Builder(id, kind);
    }

    public Identifier id() {
        return id;
    }

    public UniqueAbilityKind kind() {
        return kind;
    }

    public Collection<UniqueAbilityKey<?>> keys() {
        return keys.values();
    }

    public Set<Identifier> events() {
        return events;
    }

    public Optional<UniqueAbilityKey<Integer>> cooldownKey() {
        return Optional.ofNullable(cooldownKey);
    }

    public boolean supports(UniqueAbilityKey<?> key) {
        return key != null && keys.get(key.id()) == key;
    }

    public boolean supportsEvent(Identifier eventId) {
        return events.contains(eventId);
    }

    public static final class Builder {
        private final Identifier id;
        private final UniqueAbilityKind kind;
        private final Map<Identifier, UniqueAbilityKey<?>> keys = new LinkedHashMap<>();
        private final Set<Identifier> events = new LinkedHashSet<>();
        private UniqueAbilityKey<Integer> cooldownKey;

        private Builder(Identifier id, UniqueAbilityKind kind) {
            this.id = Objects.requireNonNull(id);
            this.kind = Objects.requireNonNull(kind);
        }

        public Builder key(UniqueAbilityKey<?> key) {
            Objects.requireNonNull(key);
            if (keys.putIfAbsent(key.id(), key) != null) {
                throw new IllegalArgumentException("duplicate key " + key.id());
            }
            return this;
        }

        public Builder cooldownKey(UniqueAbilityKey<Integer> key) {
            key(key);
            cooldownKey = key;
            return this;
        }

        public Builder event(Identifier eventId) {
            if (!events.add(Objects.requireNonNull(eventId))) {
                throw new IllegalArgumentException("duplicate event " + eventId);
            }
            return this;
        }

        public UniqueAbilityDefinition build() {
            return new UniqueAbilityDefinition(this);
        }
    }
}
