package net.sweenus.simplyswords.compat;

import dev.architectury.platform.Platform;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;
import net.sweenus.simplyswords.api.AwakeningFormRegistry;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.config.Config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.HashSet;
import java.util.Set;

/**
 * Supplies optional Spell Power modifiers through the item attribute pipeline.
 *
 * The modifiers deliberately remain transient rather than being written to the
 * stack's component patch. A world can therefore be reopened after Spell Power is
 * removed without saved Simply Swords stacks retaining references to missing
 * attributes.
 */
public final class SpellPowerWeaponAttributes {
    private static final List<SpellScalingProfile> FIRE = List.of(SpellScalingProfile.FIRE);
    private static final List<SpellScalingProfile> FROST = List.of(SpellScalingProfile.FROST);
    private static final List<SpellScalingProfile> LIGHTNING = List.of(SpellScalingProfile.LIGHTNING);
    private static final List<SpellScalingProfile> ARCANE = List.of(SpellScalingProfile.ARCANE);
    private static final List<SpellScalingProfile> SOUL = List.of(SpellScalingProfile.SOUL);
    private static final List<SpellScalingProfile> HEALING = List.of(SpellScalingProfile.HEALING);
    private static final List<SpellScalingProfile> NATURE = List.of(SpellScalingProfile.NATURE);
    private static final List<SpellScalingProfile> EVOCATION = List.of(SpellScalingProfile.EVOCATION);
    private static final List<SpellScalingProfile> ELDRITCH = List.of(SpellScalingProfile.ELDRITCH);
    private static final List<SpellScalingProfile> FROST_FIRE = List.of(
            SpellScalingProfile.FROST, SpellScalingProfile.FIRE);
    private static final List<SpellScalingProfile> HEALING_FIRE = List.of(
            SpellScalingProfile.HEALING, SpellScalingProfile.FIRE);
    private static final List<SpellScalingProfile> FROST_LIGHTNING = List.of(
            SpellScalingProfile.FROST, SpellScalingProfile.LIGHTNING);

    private static final Map<Identifier, WeaponDefinition> DEFINITIONS = createDefinitions();

    private SpellPowerWeaponAttributes() {
    }

    public static Map<Identifier, Double> defaultBonuses() {
        Map<Identifier, Double> defaults = new LinkedHashMap<>();
        DEFINITIONS.forEach((id, definition) -> defaults.put(id, definition.defaultBonus()));
        return Collections.unmodifiableMap(defaults);
    }

    public static void applyTooltipModifiers(
            ItemStack stack,
            AttributeModifierSlot slot,
            BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> consumer
    ) {
        if (slot == AttributeModifierSlot.HAND) {
            apply(stack, "hand", consumer);
        }
    }

    public static void applyEquipmentModifiers(
            ItemStack stack,
            EquipmentSlot slot,
            BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> consumer
    ) {
        if (slot == EquipmentSlot.MAINHAND) {
            apply(stack, "mainhand", consumer);
        } else if (slot == EquipmentSlot.OFFHAND) {
            apply(stack, "offhand", consumer);
        }
    }

    static List<SpellScalingProfile> profiles(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (itemId.equals(id("dormant_relic"))) {
            return AwakeningFormRegistry.getRoute(stack)
                    .map(route -> {
                        if (AwakeningFormRegistry.SUN_ROUTE.equals(route)) return HEALING_FIRE;
                        if (AwakeningFormRegistry.HARBINGER_ROUTE.equals(route)) return SOUL;
                        return List.<SpellScalingProfile>of();
                    })
                    .orElseGet(List::of);
        }

        return profiles(itemId);
    }

    static List<SpellScalingProfile> profiles(Identifier itemId) {
        WeaponDefinition definition = DEFINITIONS.get(itemId);
        return definition == null ? List.of() : definition.profiles();
    }

    private static void apply(
            ItemStack stack,
            String slotName,
            BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> consumer
    ) {
        if (stack == null || stack.isEmpty() || consumer == null
                || !Platform.isModLoaded("spell_power")
                || !SimplySwords.passVersionCheck("spell_power", SimplySwords.minimumSpellPowerVersion)
                || !Config.compatibility.spellPowerApi.get().enableWeaponAttributes.get()) {
            return;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        Double configured = Config.compatibility.spellPowerApi.get().weaponSpellPowerBonuses.get(itemId);
        double amount = configured == null ? 0.0D : Math.max(0.0D, configured);
        if (amount <= 0.0D) {
            return;
        }

        Set<RegistryEntry<EntityAttribute>> appliedAttributes = new HashSet<>();
        for (SpellScalingComponents.Definition component : SpellScalingComponents.weaponComponents(stack)) {
            RegistryEntry<EntityAttribute> attribute =
                    SimplySwordsExpectPlatform.getSpellPowerAttribute(component.id());
            if (attribute != null && appliedAttributes.add(attribute)) {
                consumer.accept(
                        attribute,
                        new EntityAttributeModifier(
                                modifierId(itemId, component.id(), slotName),
                                amount,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        )
                );
            }
        }
    }

    private static Identifier modifierId(Identifier itemId, Identifier componentId, String slotName) {
        return Identifier.of(
                SimplySwords.MOD_ID,
                "spell_power/" + itemId.getPath() + "/" + componentId.getPath() + "/" + slotName
        );
    }

    private static Map<Identifier, WeaponDefinition> createDefinitions() {
        Map<Identifier, WeaponDefinition> definitions = new LinkedHashMap<>();

        add(definitions, "brimstone_claymore", FIRE, true);
        add(definitions, "watcher_claymore", SOUL, true);
        add(definitions, "the_devourer", SOUL, true);
        add(definitions, "storms_edge", LIGHTNING, false);
        add(definitions, "stormscale", LIGHTNING, false);
        add(definitions, "ionbound_stormscale", LIGHTNING, false);
        add(definitions, "stormbringer", LIGHTNING, false);
        add(definitions, "bramblethorn", NATURE, false);
        add(definitions, "watching_warglaive", SOUL, false);
        add(definitions, "toxic_longsword", SOUL, false);
        add(definitions, "emberblade", FIRE, false);
        add(definitions, "hearthflame", FIRE, true);
        add(definitions, "soulkeeper", SOUL, true);
        add(definitions, "twisted_blade", SOUL, true);
        add(definitions, "soulstealer", SOUL, false);
        add(definitions, "soulrender", SOUL, true);
        add(definitions, "soulstalker", SOUL, true);
        add(definitions, "soulpyre", SOUL, true);
        add(definitions, "frostfall", FROST, false);
        add(definitions, "molten_edge", FIRE, false);
        add(definitions, "livyatan", FROST_LIGHTNING, false);
        add(definitions, "icewhisper", FROST, true);
        add(definitions, "arcanethyst", ARCANE, true);
        add(definitions, "thunderbrand", LIGHTNING, true);
        add(definitions, "mjolnir", LIGHTNING, false);
        add(definitions, "slumbering_lichblade", SOUL, true);
        add(definitions, "waking_lichblade", SOUL, true);
        add(definitions, "awakened_lichblade", SOUL, true);
        add(definitions, "shadowsting", SOUL, false);
        add(definitions, "dormant_relic", List.of(), false);
        add(definitions, "righteous_relic", HEALING_FIRE, false);
        add(definitions, "tainted_relic", SOUL, false);
        add(definitions, "sunfire", HEALING_FIRE, false);
        add(definitions, "harbinger", SOUL, false);
        add(definitions, "whisperwind", EVOCATION, true);
        add(definitions, "dreadwhisper", SOUL, true);
        add(definitions, "emberlash", FIRE, false);
        add(definitions, "waxweaver", FIRE, false);
        add(definitions, "hiveheart", NATURE, false);
        add(definitions, "stars_edge", ARCANE, false);
        add(definitions, "wickpiercer", FIRE, false);
        add(definitions, "gloampiercer", SOUL, false);
        add(definitions, "tempest", FROST_FIRE, false);
        add(definitions, "flamewind", FIRE, false);
        add(definitions, "magiscythe", ARCANE, false);
        add(definitions, "enigma", EVOCATION, false);
        add(definitions, "magispear", ARCANE, false);
        add(definitions, "magiblade", ARCANE, false);
        add(definitions, "caelestis", ELDRITCH, false);
        add(definitions, "dawnquiver", HEALING, true);
        add(definitions, "riftmane", ARCANE, true);
        add(definitions, "wraithfang", SOUL, false);
        add(definitions, "wraithmaw", SOUL, false);
        add(definitions, "bloodwake", SOUL, false);
        add(definitions, "chompolotl", NATURE, false);

        return Collections.unmodifiableMap(definitions);
    }

    private static void add(
            Map<Identifier, WeaponDefinition> definitions,
            String itemPath,
            List<SpellScalingProfile> profiles,
            boolean twoHanded
    ) {
        definitions.put(id(itemPath), new WeaponDefinition(profiles, twoHanded ? 4.0D : 2.0D));
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }

    private record WeaponDefinition(List<SpellScalingProfile> profiles, double defaultBonus) {
    }
}
