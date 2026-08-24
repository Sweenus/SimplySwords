package net.sweenus.simplyswords.neoforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.SpellScalingAssignments;
import net.sweenus.simplyswords.compat.SpellSchoolDisplay;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class NeoForgeSpellPowerCompat {
    private static final Set<Identifier> WARNED_MISSING_SCHOOLS = ConcurrentHashMap.newKeySet();

    private NeoForgeSpellPowerCompat() {
    }

    static float scale(float damageModifier, LivingEntity player, Identifier scalingProfileId) {
        SpellSchool school = resolve(scalingProfileId);
        if (school == null) {
            return 0.0F;
        }
        float scaledValue = (float) (damageModifier
                * SpellPower.getSpellPower(school, player).randomValue());
        return HelperMethods.applySpellPowerApiScalingMultiplier(scaledValue);
    }

    static List<Identifier> schoolIds() {
        return SpellSchools.all().stream()
                .filter(SpellSchool::isMagicArchetype)
                .map(school -> school.id)
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
    }

    static SpellSchoolDisplay display(Identifier scalingId) {
        SpellSchool school = resolve(scalingId);
        if (school == null) {
            Identifier fallback = SpellScalingAssignments.defaultSpellPowerSchool(scalingId)
                    .orElse(Identifier.of("spell_power", "arcane"));
            return new SpellSchoolDisplay(fallback, Text.literal(fallback.toString()));
        }
        RegistryEntry<EntityAttribute> attribute = school.getAttributeEntry();
        Text name = attribute == null
                ? Text.literal(school.id.toString())
                : Text.translatable(attribute.value().getTranslationKey());
        return new SpellSchoolDisplay(school.id, name);
    }

    static RegistryEntry<EntityAttribute> attribute(Identifier scalingId) {
        SpellSchool school = resolve(scalingId);
        return school == null ? null : school.getAttributeEntry();
    }

    private static SpellSchool resolve(Identifier scalingId) {
        Identifier configured = SpellScalingAssignments.spellPowerSchool(scalingId).orElse(null);
        SpellSchool school = configured == null ? null : SpellSchools.getSchool(configured.toString());
        if (school != null) {
            return school;
        }
        Identifier fallback = SpellScalingAssignments.defaultSpellPowerSchool(scalingId).orElse(null);
        if (configured != null && !configured.equals(fallback) && WARNED_MISSING_SCHOOLS.add(configured)) {
            SimplySwords.LOGGER.warn("Unknown Spell Power school {}; using the default for {}", configured, scalingId);
        }
        return fallback == null ? null : SpellSchools.getSchool(fallback.toString());
    }
}
