package net.sweenus.simplyswords.api;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.item.component.AwakeningComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//
// Addon-facing definition of a base weapon and all forms reachable through
// Runic Forge awakening. A route is selected at #selectionLevel(),
// persisted on the stack, and cleared only when a lower level is committed.
// Stages may point back to the base item or to distinct registered form items.
//
public final class AwakeningFormFamily {
    private final Item baseItem;
    private final AwakeningProfile profile;
    private final AwakeningFormStage baseStage;
    private final int selectionLevel;
    private final boolean persistentProgression;
    private final Map<Identifier, AwakeningFormRoute> routes;
    private final AwakeningFormHandler handler;
    private final Set<Item> members;

    private AwakeningFormFamily(Builder builder) {
        baseItem = builder.baseItem;
        profile = builder.profile;
        baseStage = builder.baseStage;
        selectionLevel = builder.selectionLevel;
        persistentProgression = builder.persistentProgression;
        routes = Map.copyOf(builder.routes);
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("Awakening form family must contain at least one route");
        }
        if (builder.handler == null) {
            if (routes.size() != 1) {
                throw new IllegalArgumentException("Branching awakening families require a route resolver");
            }
            Identifier fixedRoute = routes.keySet().iterator().next();
            handler = context -> fixedRoute;
        } else {
            handler = builder.handler;
        }

        Set<Item> familyMembers = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Identifier> formIds = new HashSet<>();
        formIds.add(baseStage.id());
        familyMembers.add(baseItem);
        familyMembers.addAll(builder.aliases);
        for (AwakeningFormRoute route : routes.values()) {
            for (AwakeningFormStage stage : route.stages()) {
                if (!formIds.add(stage.id())) {
                    throw new IllegalArgumentException("Duplicate awakening form id: " + stage.id());
                }
                familyMembers.add(stage.item());
            }
        }
        members = Set.copyOf(familyMembers);
    }

    public static Builder builder(Item baseItem, AwakeningProfile profile, Identifier baseFormId) {
        return new Builder(baseItem, profile, baseFormId);
    }

    public Item baseItem() {
        return baseItem;
    }

    public AwakeningProfile profile() {
        return profile;
    }

    public AwakeningFormStage baseStage() {
        return baseStage;
    }

    public int selectionLevel() {
        return selectionLevel;
    }

    public boolean persistentProgression() {
        return persistentProgression;
    }

    public Map<Identifier, AwakeningFormRoute> routes() {
        return routes;
    }

    public AwakeningFormHandler handler() {
        return handler;
    }

    public Set<Item> members() {
        return members;
    }

    public boolean containsRoute(Identifier route) {
        return routes.containsKey(route);
    }

    public AwakeningFormStage resolveStage(Identifier route, int level) {
        AwakeningFormRoute definition = routes.get(route);
        return definition == null
                ? baseStage
                : definition.stageForLevel(level).orElse(baseStage);
    }

    public static final class Builder {
        private final Item baseItem;
        private final AwakeningProfile profile;
        private AwakeningFormStage baseStage;
        private int selectionLevel;
        private boolean persistentProgression;
        private final Map<Identifier, AwakeningFormRoute> routes = new LinkedHashMap<>();
        private final List<Item> aliases = new ArrayList<>();
        private AwakeningFormHandler handler;

        private Builder(Item baseItem, AwakeningProfile profile, Identifier baseFormId) {
            this.baseItem = Objects.requireNonNull(baseItem, "baseItem");
            this.profile = Objects.requireNonNull(profile, "profile");
            this.baseStage = new AwakeningFormStage(
                    Objects.requireNonNull(baseFormId, "baseFormId"),
                    0,
                    baseItem,
                    0.0F
            );
        }

        public Builder basePresentation(String translationKey, AwakeningFormRarity rarity, float modelValue) {
            baseStage = new AwakeningFormStage(
                    baseStage.id(),
                    0,
                    baseItem,
                    translationKey,
                    rarity,
                    modelValue
            );
            return this;
        }

        public Builder selectionLevel(int selectionLevel) {
            if (selectionLevel < 0 || selectionLevel > AwakeningComponent.MAX_LEVEL) {
                throw new IllegalArgumentException("Awakening route selection level must be between 0 and "
                        + AwakeningComponent.MAX_LEVEL + ": " + selectionLevel);
            }
            this.selectionLevel = selectionLevel;
            return this;
        }

        public Builder persistentProgression(boolean persistentProgression) {
            this.persistentProgression = persistentProgression;
            return this;
        }

        public Builder route(Identifier route, AwakeningFormStage... stages) {
            Objects.requireNonNull(route, "route");
            if (routes.containsKey(route)) {
                throw new IllegalArgumentException("Duplicate awakening route: " + route);
            }
            routes.put(route, new AwakeningFormRoute(route, List.of(stages)));
            return this;
        }

        public Builder alias(Item item) {
            aliases.add(Objects.requireNonNull(item, "item"));
            return this;
        }

        public Builder routeHandler(AwakeningFormHandler handler) {
            this.handler = Objects.requireNonNull(handler, "handler");
            return this;
        }

        public AwakeningFormFamily build() {
            return new AwakeningFormFamily(this);
        }
    }
}
