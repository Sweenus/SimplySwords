package net.sweenus.simplyswords.compat;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.SpellScalingTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stable config-facing identifiers for built-in spell-scaled effects.
 *
 * A weapon with a single school uses its effective awakening form id. Composite
 * weapons use code form/effect ids so each independently-scaled effect can be
 * configured without changing the public spell-scaling profile API.
 */
public final class SpellScalingComponents {
    public enum Kind {
        WEAPON,
        GEM_POWER
    }

    public record Definition(Identifier id, Identifier ownerId, Kind kind,
                             SpellScalingProfile defaultProfile, Identifier defaultIronsSchool,
                             String effectTranslationKey) {
    }

    private static final Map<Identifier, Definition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<Identifier, List<Definition>> WEAPONS = new LinkedHashMap<>();
    private static final Map<Identifier, Definition> POWERS = new LinkedHashMap<>();

    static {
        weapon("brimstone_claymore", SpellScalingProfile.FIRE, "fire");
        weapon("watcher_claymore", SpellScalingProfile.SOUL, "blood");
        weapon("the_devourer", SpellScalingProfile.SOUL, "eldritch");
        weapon("storms_edge", SpellScalingProfile.LIGHTNING, "lightning");
        weapon("stormscale", SpellScalingProfile.LIGHTNING, "lightning");
        weapon("awakened_stormscale", SpellScalingProfile.LIGHTNING, "lightning");
        weapon("ionbound_stormscale", SpellScalingProfile.LIGHTNING, "lightning");
        weapon("stormbringer", SpellScalingProfile.LIGHTNING, "lightning");
        weapon("bramblethorn", SpellScalingProfile.NATURE, "nature");
        weapon("watching_warglaive", SpellScalingProfile.SOUL, "blood");
        weapon("toxic_longsword", SpellScalingProfile.SOUL, "nature");
        weapon("emberblade", SpellScalingProfile.FIRE, "fire");
        weapon("hearthflame", SpellScalingProfile.FIRE, "fire");
        weapon("soulkeeper", SpellScalingProfile.SOUL, "blood");
        weapon("twisted_blade", SpellScalingProfile.SOUL, "blood");
        weapon("soulstealer", SpellScalingProfile.SOUL, "blood");
        weapon("soulrender", SpellScalingProfile.SOUL, "blood");
        weapon("soulstalker", SpellScalingProfile.SOUL, "eldritch");
        weapon("soulpyre", SpellScalingProfile.SOUL, "blood");
        weapon("frostfall", SpellScalingProfile.FROST, "ice");
        weapon("molten_edge", SpellScalingProfile.FIRE, "fire");
        compositeWeapon("livyatan", "frost", SpellScalingProfile.FROST, "ice");
        compositeWeapon("livyatan", "lightning", SpellScalingProfile.LIGHTNING, "lightning");
        weapon("icewhisper", SpellScalingProfile.FROST, "ice");
        weapon("arcanethyst", SpellScalingProfile.ARCANE, "eldritch");
        weapon("thunderbrand", SpellScalingProfile.LIGHTNING, "lightning");
        weapon("mjolnir", SpellScalingProfile.LIGHTNING, "lightning");
        weapon("slumbering_lichblade", SpellScalingProfile.SOUL, "blood");
        weapon("waking_lichblade", SpellScalingProfile.SOUL, "blood");
        weapon("awakened_lichblade", SpellScalingProfile.SOUL, "blood");
        weapon("shadowsting", SpellScalingProfile.SOUL, "ender");
        compositeWeapon("righteous_relic", "damage", SpellScalingProfile.FIRE, "fire");
        compositeWeapon("righteous_relic", "healing", SpellScalingProfile.HEALING, "holy");
        compositeWeapon("sunfire", "damage", SpellScalingProfile.FIRE, "fire");
        compositeWeapon("sunfire", "healing", SpellScalingProfile.HEALING, "holy");
        weapon("tainted_relic", SpellScalingProfile.SOUL, "eldritch");
        weapon("harbinger", SpellScalingProfile.SOUL, "eldritch");
        weapon("whisperwind", SpellScalingProfile.EVOCATION, "evocation");
        weapon("dreadwhisper", SpellScalingProfile.SOUL, "eldritch");
        weapon("emberlash", SpellScalingProfile.FIRE, "fire");
        weapon("waxweaver", SpellScalingProfile.FIRE, "fire");
        weapon("hiveheart", SpellScalingProfile.NATURE, "nature");
        weapon("stars_edge", SpellScalingProfile.ARCANE, "ender");
        weapon("wickpiercer", SpellScalingProfile.FIRE, "fire");
        weapon("gloampiercer", SpellScalingProfile.SOUL, "eldritch");
        compositeWeapon("tempest", "fire", SpellScalingProfile.FIRE, "fire");
        compositeWeapon("tempest", "frost", SpellScalingProfile.FROST, "ice");
        weapon("flamewind", SpellScalingProfile.FIRE, "fire");
        weapon("magiscythe", SpellScalingProfile.ARCANE, "ender");
        weapon("enigma", SpellScalingProfile.EVOCATION, "evocation");
        weapon("magispear", SpellScalingProfile.ARCANE, "evocation");
        weapon("magiblade", SpellScalingProfile.ARCANE, "eldritch");
        weapon("caelestis", SpellScalingProfile.ELDRITCH, "eldritch");
        weapon("dawnquiver", SpellScalingProfile.HEALING, "holy");
        weapon("riftmane", SpellScalingProfile.ARCANE, "evocation");
        weapon("wraithfang", SpellScalingProfile.SOUL, "ender");
        weapon("wraithmaw", SpellScalingProfile.SOUL, "ender");
        weapon("bloodwake", SpellScalingProfile.SOUL, "blood");
        weapon("chompolotl", SpellScalingProfile.NATURE, "nature");
        // Kept for the optional Eldritch End weapon if its registration is enabled.
        weapon("dreadtide", SpellScalingProfile.ELDRITCH, "eldritch");

        power("banehead_swarm", SpellScalingProfile.ELDRITCH, "evocation");
        power("dancing_blades", SpellScalingProfile.EVOCATION, "ender");
        power("dragon_maw", SpellScalingProfile.FIRE, "fire");
        power("evocation", SpellScalingProfile.EVOCATION, "evocation");
        power("faultline", SpellScalingProfile.NATURE, "nature");
        power("goat_stampede", SpellScalingProfile.NATURE, "nature");
        power("imbued", SpellScalingProfile.ARCANE, "ender");
        power("immolation", SpellScalingProfile.FIRE, "fire");
        power("necromantic_arsenal", SpellScalingProfile.SOUL, "blood");
        power("radiance", SpellScalingProfile.FIRE, "fire");
        power("runic_slash", SpellScalingProfile.ARCANE, "ender");
        power("sniffer_slam", SpellScalingProfile.NATURE, "nature");
        power("stormlash", SpellScalingProfile.LIGHTNING, "lightning");
        power("verdant_trail", SpellScalingProfile.NATURE, "nature");
        power("wing_buffet", SpellScalingProfile.EVOCATION, "evocation");
        power("wolf_pack", SpellScalingProfile.NATURE, "nature");
    }

    private SpellScalingComponents() {
    }

    public static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }

    public static Identifier weaponComponent(ItemStack stack, SpellScalingProfile requestedProfile) {
        SpellScalingProfile fallback = requestedProfile == null ? SpellScalingProfile.ARCANE : requestedProfile;
        return weaponComponent(stack, fallback.registryId());
    }

    public static Identifier weaponComponent(ItemStack stack, Identifier requestedProfileId) {
        Identifier fallback = requestedProfileId == null
                ? SpellScalingProfile.ARCANE.registryId()
                : requestedProfileId;
        if (stack == null || stack.isEmpty()) {
            return fallback;
        }
        Identifier owner = AwakeningApi.getFormId(stack)
                .orElseGet(() -> Registries.ITEM.getId(stack.getItem()));
        List<Definition> components = WEAPONS.get(owner);
        if (components == null || components.isEmpty()) {
            return fallback;
        }
        for (Definition component : components) {
            if (component.defaultProfile().registryId().equals(fallback)) {
                return component.id();
            }
        }
        return fallback;
    }

    public static Identifier component(String ownerPath, String effectPath) {
        return id(ownerPath + "/" + effectPath);
    }

    public static Identifier power(String powerPath) {
        Definition definition = POWERS.get(id(powerPath));
        return definition == null ? id(powerPath) : definition.id();
    }

    public static Optional<Definition> get(Identifier componentId) {
        return Optional.ofNullable(DEFINITIONS.get(componentId));
    }

    public static List<Definition> weaponComponents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        Identifier owner = AwakeningApi.getFormId(stack)
                .orElseGet(() -> Registries.ITEM.getId(stack.getItem()));
        return WEAPONS.getOrDefault(owner, List.of());
    }

    public static List<Identifier> componentIds(Kind kind) {
        return DEFINITIONS.values().stream()
                .filter(definition -> definition.kind() == kind)
                .map(Definition::id)
                .toList();
    }

    public static Map<Identifier, Identifier> defaultSpellPowerSchools(Kind kind) {
        return defaultSchools(kind, true);
    }

    public static Map<Identifier, Identifier> defaultIronsSchools(Kind kind) {
        return defaultSchools(kind, false);
    }

    public static Optional<Identifier> defaultSpellPowerSchool(Identifier componentId) {
        return defaultSchool(componentId, true);
    }

    public static Optional<Identifier> defaultIronsSchool(Identifier componentId) {
        return get(componentId).map(Definition::defaultIronsSchool);
    }

    private static Map<Identifier, Identifier> defaultSchools(Kind kind, boolean spellPower) {
        Map<Identifier, Identifier> defaults = new LinkedHashMap<>();
        DEFINITIONS.values().stream()
                .filter(definition -> definition.kind() == kind)
                .forEach(definition -> defaultSchool(definition.id(), spellPower)
                        .ifPresent(school -> defaults.put(definition.id(), school)));
        return Collections.unmodifiableMap(defaults);
    }

    private static Optional<Identifier> defaultSchool(Identifier componentId, boolean spellPower) {
        if (!spellPower) {
            return defaultIronsSchool(componentId);
        }
        return get(componentId)
                .flatMap(definition -> SimplySwordsAPI.getSpellScalingDefinition(
                        definition.defaultProfile().registryId()))
                .map(definition -> definition.spellPowerTarget())
                .map(SpellScalingTarget::schoolId);
    }

    private static void weapon(String ownerPath, SpellScalingProfile profile, String ironsSchool) {
        register(new Definition(
                id(ownerPath), id(ownerPath), Kind.WEAPON, profile, ironsSchool(ironsSchool), ""));
    }

    private static void compositeWeapon(String ownerPath, String effectPath, SpellScalingProfile profile,
                                        String ironsSchool) {
        register(new Definition(
                component(ownerPath, effectPath),
                id(ownerPath),
                Kind.WEAPON,
                profile,
                ironsSchool(ironsSchool),
                "item.simplyswords.compat.component." + ownerPath + "." + effectPath
        ));
    }

    private static void power(String powerPath, SpellScalingProfile profile, String ironsSchool) {
        Definition definition = new Definition(
                id(powerPath), id(powerPath), Kind.GEM_POWER, profile, ironsSchool(ironsSchool), "");
        register(definition);
        POWERS.put(definition.ownerId(), definition);
    }

    private static Identifier ironsSchool(String path) {
        return Identifier.of("irons_spellbooks", path);
    }

    private static void register(Definition definition) {
        if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate spell scaling component " + definition.id());
        }
        if (definition.kind() == Kind.WEAPON) {
            WEAPONS.computeIfAbsent(definition.ownerId(), ignored -> new ArrayList<>()).add(definition);
        }
    }
}
