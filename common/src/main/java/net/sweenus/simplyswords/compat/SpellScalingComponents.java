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
                             SpellScalingProfile defaultProfile, String effectTranslationKey) {
    }

    private static final Map<Identifier, Definition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<Identifier, List<Definition>> WEAPONS = new LinkedHashMap<>();
    private static final Map<Identifier, Definition> POWERS = new LinkedHashMap<>();

    static {
        weapon("brimstone_claymore", SpellScalingProfile.FIRE);
        weapon("watcher_claymore", SpellScalingProfile.SOUL);
        weapon("the_devourer", SpellScalingProfile.SOUL);
        weapon("storms_edge", SpellScalingProfile.LIGHTNING);
        weapon("stormscale", SpellScalingProfile.LIGHTNING);
        weapon("awakened_stormscale", SpellScalingProfile.LIGHTNING);
        weapon("ionbound_stormscale", SpellScalingProfile.LIGHTNING);
        weapon("stormbringer", SpellScalingProfile.LIGHTNING);
        weapon("bramblethorn", SpellScalingProfile.NATURE);
        weapon("watching_warglaive", SpellScalingProfile.SOUL);
        weapon("toxic_longsword", SpellScalingProfile.SOUL);
        weapon("emberblade", SpellScalingProfile.FIRE);
        weapon("hearthflame", SpellScalingProfile.FIRE);
        weapon("soulkeeper", SpellScalingProfile.SOUL);
        weapon("twisted_blade", SpellScalingProfile.SOUL);
        weapon("soulstealer", SpellScalingProfile.SOUL);
        weapon("soulrender", SpellScalingProfile.SOUL);
        weapon("soulstalker", SpellScalingProfile.SOUL);
        weapon("soulpyre", SpellScalingProfile.SOUL);
        weapon("frostfall", SpellScalingProfile.FROST);
        weapon("molten_edge", SpellScalingProfile.FIRE);
        compositeWeapon("livyatan", "frost", SpellScalingProfile.FROST);
        compositeWeapon("livyatan", "lightning", SpellScalingProfile.LIGHTNING);
        weapon("icewhisper", SpellScalingProfile.FROST);
        weapon("arcanethyst", SpellScalingProfile.ARCANE);
        weapon("thunderbrand", SpellScalingProfile.LIGHTNING);
        weapon("mjolnir", SpellScalingProfile.LIGHTNING);
        weapon("slumbering_lichblade", SpellScalingProfile.SOUL);
        weapon("waking_lichblade", SpellScalingProfile.SOUL);
        weapon("awakened_lichblade", SpellScalingProfile.SOUL);
        weapon("shadowsting", SpellScalingProfile.SOUL);
        compositeWeapon("righteous_relic", "damage", SpellScalingProfile.FIRE);
        compositeWeapon("righteous_relic", "healing", SpellScalingProfile.HEALING);
        compositeWeapon("sunfire", "damage", SpellScalingProfile.FIRE);
        compositeWeapon("sunfire", "healing", SpellScalingProfile.HEALING);
        weapon("tainted_relic", SpellScalingProfile.SOUL);
        weapon("harbinger", SpellScalingProfile.SOUL);
        weapon("whisperwind", SpellScalingProfile.EVOCATION);
        weapon("dreadwhisper", SpellScalingProfile.SOUL);
        weapon("emberlash", SpellScalingProfile.FIRE);
        weapon("waxweaver", SpellScalingProfile.FIRE);
        weapon("hiveheart", SpellScalingProfile.NATURE);
        weapon("stars_edge", SpellScalingProfile.ARCANE);
        weapon("wickpiercer", SpellScalingProfile.FIRE);
        weapon("gloampiercer", SpellScalingProfile.SOUL);
        compositeWeapon("tempest", "fire", SpellScalingProfile.FIRE);
        compositeWeapon("tempest", "frost", SpellScalingProfile.FROST);
        weapon("flamewind", SpellScalingProfile.FIRE);
        weapon("magiscythe", SpellScalingProfile.ARCANE);
        weapon("enigma", SpellScalingProfile.EVOCATION);
        weapon("magispear", SpellScalingProfile.ARCANE);
        weapon("magiblade", SpellScalingProfile.ARCANE);
        weapon("caelestis", SpellScalingProfile.ELDRITCH);
        weapon("dawnquiver", SpellScalingProfile.HEALING);
        weapon("riftmane", SpellScalingProfile.ARCANE);
        weapon("wraithfang", SpellScalingProfile.SOUL);
        weapon("wraithmaw", SpellScalingProfile.SOUL);
        weapon("bloodwake", SpellScalingProfile.SOUL);
        weapon("chompolotl", SpellScalingProfile.NATURE);
        // Kept for the optional Eldritch End weapon if its registration is enabled.
        weapon("dreadtide", SpellScalingProfile.ELDRITCH);

        power("banehead_swarm", SpellScalingProfile.ELDRITCH);
        power("dancing_blades", SpellScalingProfile.EVOCATION);
        power("dragon_maw", SpellScalingProfile.FIRE);
        power("evocation", SpellScalingProfile.EVOCATION);
        power("faultline", SpellScalingProfile.NATURE);
        power("goat_stampede", SpellScalingProfile.NATURE);
        power("imbued", SpellScalingProfile.ARCANE);
        power("immolation", SpellScalingProfile.FIRE);
        power("necromantic_arsenal", SpellScalingProfile.SOUL);
        power("radiance", SpellScalingProfile.FIRE);
        power("runic_slash", SpellScalingProfile.ARCANE);
        power("sniffer_slam", SpellScalingProfile.NATURE);
        power("stormlash", SpellScalingProfile.LIGHTNING);
        power("verdant_trail", SpellScalingProfile.NATURE);
        power("wing_buffet", SpellScalingProfile.EVOCATION);
        power("wolf_pack", SpellScalingProfile.NATURE);
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
        return defaultSchool(componentId, false);
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
        return get(componentId)
                .flatMap(definition -> SimplySwordsAPI.getSpellScalingDefinition(
                        definition.defaultProfile().registryId()))
                .map(definition -> spellPower
                        ? definition.spellPowerTarget()
                        : definition.ironsTarget())
                .map(SpellScalingTarget::schoolId);
    }

    private static void weapon(String ownerPath, SpellScalingProfile profile) {
        register(new Definition(id(ownerPath), id(ownerPath), Kind.WEAPON, profile, ""));
    }

    private static void compositeWeapon(String ownerPath, String effectPath, SpellScalingProfile profile) {
        register(new Definition(
                component(ownerPath, effectPath),
                id(ownerPath),
                Kind.WEAPON,
                profile,
                "item.simplyswords.compat.component." + ownerPath + "." + effectPath
        ));
    }

    private static void power(String powerPath, SpellScalingProfile profile) {
        Definition definition = new Definition(id(powerPath), id(powerPath), Kind.GEM_POWER, profile, "");
        register(definition);
        POWERS.put(definition.ownerId(), definition);
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
