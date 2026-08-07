package net.sweenus.simplyswords.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

//
// Config-driven target exclusion. Entities selected by the abilityIgnoredEntities
// list are skipped by all weapon and gem ability targeting and damage.
//
public final class IgnoredEntities {

    private IgnoredEntities() {
    }

    //
    // Class-based categories, for the groupings vanilla has no entity type tag for.
    //
    private enum Category {
        PASSIVE("passive", PassiveEntity.class),
        ANIMAL("animal", AnimalEntity.class),
        VILLAGER("villager", MerchantEntity.class),
        HOSTILE("hostile", Monster.class),
        WATER("water", WaterCreatureEntity.class),
        AMBIENT("ambient", AmbientEntity.class),
        GOLEM("golem", GolemEntity.class),
        PLAYER("player", PlayerEntity.class),
        TAMEABLE("tameable", Tameable.class);

        private final String keyword;
        private final Class<?> matched;

        Category(String keyword, Class<?> matched) {
            this.keyword = keyword;
            this.matched = matched;
        }

        boolean matches(Entity entity) {
            return matched.isInstance(entity);
        }

        static Category fromKeyword(String keyword) {
            for (Category category : values()) {
                if (category.keyword.equals(keyword)) {
                    return category;
                }
            }
            return null;
        }
    }

    //
    // A parsed view of the config list. isIgnored runs for every entity of every
    // ability scan, so selectors are resolved once here rather than per call.
    //
    private static final class Snapshot {
        private final Set<String> raw;
        private final Set<EntityType<?>> types;
        private final TagKey<EntityType<?>>[] tags;
        private final Category[] categories;

        private Snapshot(Set<String> raw, Set<EntityType<?>> types,
                         TagKey<EntityType<?>>[] tags, Category[] categories) {
            this.raw = raw;
            this.types = types;
            this.tags = tags;
            this.categories = categories;
        }

        private boolean isEmpty() {
            return types.isEmpty() && tags.length == 0 && categories.length == 0;
        }
    }

    private static volatile Snapshot snapshot = null;

    public static boolean isIgnored(Entity entity) {
        if (entity == null) {
            return false;
        }
        Snapshot current = current();
        if (current.isEmpty()) {
            return false;
        }

        EntityType<?> type = entity.getType();
        if (current.types.contains(type)) {
            return true;
        }
        for (Category category : current.categories) {
            if (category.matches(entity)) {
                return true;
            }
        }
        for (TagKey<EntityType<?>> tag : current.tags) {
            // Queried live so datapack reloads need no snapshot invalidation.
            if (type.isIn(tag)) {
                return true;
            }
        }
        return false;
    }

    //
    // Rebuilds only when the config list itself changed, so edits made through the
    // config screen apply without a restart.
    //
    private static Snapshot current() {
        Snapshot current = snapshot;
        Set<String> raw = Config.general.abilityIgnoredEntities;
        if (current != null && current.raw.equals(raw)) {
            return current;
        }
        Snapshot rebuilt = build(raw);
        snapshot = rebuilt;
        return rebuilt;
    }

    @SuppressWarnings("unchecked")
    private static Snapshot build(Set<String> raw) {
        Set<EntityType<?>> types = new HashSet<>();
        List<TagKey<EntityType<?>>> tags = new ArrayList<>();
        EnumSet<Category> categories = EnumSet.noneOf(Category.class);
        List<String> unrecognised = new ArrayList<>();

        for (String rawSelector : raw) {
            if (rawSelector == null || rawSelector.isBlank()) {
                continue;
            }

            if (rawSelector.charAt(0) == '#') {
                Identifier tagId = Identifier.tryParse(rawSelector.substring(1));
                if (tagId == null) {
                    unrecognised.add(rawSelector);
                    continue;
                }
                tags.add(TagKey.of(RegistryKeys.ENTITY_TYPE, tagId));
                continue;
            }

            if (rawSelector.indexOf(':') >= 0) {
                Identifier typeId = Identifier.tryParse(rawSelector);
                // getOrEmpty, not get: an unknown id resolves to the registry's default
                // entry (the pig), which would silently ignore pigs on any typo.
                Optional<EntityType<?>> type = typeId == null
                        ? Optional.empty()
                        : Registries.ENTITY_TYPE.getOrEmpty(typeId);
                if (type.isEmpty()) {
                    unrecognised.add(rawSelector);
                    continue;
                }
                types.add(type.get());
                continue;
            }

            Category category = Category.fromKeyword(rawSelector);
            if (category == null) {
                unrecognised.add(rawSelector);
                continue;
            }
            categories.add(category);
        }

        if (!unrecognised.isEmpty()) {
            SimplySwords.LOGGER.warn("Ignoring unrecognised abilityIgnoredEntities entries: {}", unrecognised);
        }

        return new Snapshot(
                new HashSet<>(raw),
                types,
                tags.toArray(new TagKey[0]),
                categories.toArray(new Category[0]));
    }
}
