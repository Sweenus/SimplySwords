package net.sweenus.simplyswords.fabric;

import dev.architectury.platform.Platform;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingDefinition;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.SpellScalingTarget;
import net.sweenus.simplyswords.compat.SpellScalingAssignments;
import net.sweenus.simplyswords.compat.SpellSchoolDisplay;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FabricHelperMethods {
    private static final Set<Identifier> WARNED_MISSING_SCHOOLS = ConcurrentHashMap.newKeySet();


    public static String spellSchoolDisplayKey(Identifier scalingProfileId) {
        return target(scalingProfileId)
                .map(SpellScalingTarget::displayTranslationKey)
                .filter(key -> !key.isBlank())
                .orElse("item.simplyswords.compat.scaleArcane");
    }

    public static String spellSchoolDisplayKey(String legacySchool) {
        return spellSchoolDisplayKey(SpellScalingProfile.fromLegacyName(legacySchool).registryId());
    }

    public static float useSpellAttributeScaling(float damageModifier, LivingEntity player, Identifier scalingProfileId) {
        if (Platform.isFabric() && SimplySwords.passVersionCheck("spell_power", SimplySwords.minimumSpellPowerVersion)) {
            if (player != null && !player.getWorld().isClient) {
                SpellSchool school = resolveSchool(scalingProfileId);
                if (school != null) {
                    float scaledValue = (float) (damageModifier
                            * SpellPower.getSpellPower(school, player).randomValue());
                    return HelperMethods.applySpellPowerApiScalingMultiplier(scaledValue);
                }
            }
        }
        return 0;
    }

    public static float useSpellAttributeScaling(float damageModifier, LivingEntity player, String legacySchool) {
        return useSpellAttributeScaling(
                damageModifier, player, SpellScalingProfile.fromLegacyName(legacySchool).registryId());
    }

    public static DamageSource getAbilityMagicDamageSource(ServerWorld world, LivingEntity actor,
                                                            Identifier scalingProfileId) {
        return world.getDamageSources().indirectMagic(actor, actor);
    }

    public static float getAbilityMagicResistanceMultiplier(LivingEntity target, Identifier scalingProfileId) {
        return 1.0F;
    }

    public static int applySpellCooldownReduction(int baseTicks, LivingEntity actor) {
        return baseTicks;
    }

    public static List<Identifier> spellPowerSchoolIds() {
        if (!Platform.isFabric()
                || !SimplySwords.passVersionCheck("spell_power", SimplySwords.minimumSpellPowerVersion)) {
            return List.of();
        }
        return SpellSchools.all().stream()
                .filter(SpellSchool::isMagicArchetype)
                .map(school -> school.id)
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
    }

    public static SpellSchoolDisplay activeSpellSchoolDisplay(Identifier scalingId) {
        SpellSchool school = resolveSchool(scalingId);
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

    public static RegistryEntry<EntityAttribute> spellPowerAttribute(Identifier scalingId) {
        SpellSchool school = resolveSchool(scalingId);
        return school == null ? null : school.getAttributeEntry();
    }

    private static SpellSchool resolveSchool(Identifier scalingProfileId) {
        Identifier configured = SpellScalingAssignments.spellPowerSchool(scalingProfileId).orElse(null);
        SpellSchool school = configured == null ? null : SpellSchools.getSchool(configured.toString());
        if (school != null) {
            return school;
        }
        if (configured != null && WARNED_MISSING_SCHOOLS.add(configured)) {
            SimplySwords.LOGGER.warn("Unknown Spell Power school {}; using the default for {}", configured, scalingProfileId);
        }
        Identifier fallback = SpellScalingAssignments.defaultSpellPowerSchool(scalingProfileId).orElse(null);
        return fallback == null ? null : SpellSchools.getSchool(fallback.toString());
    }

    private static java.util.Optional<SpellScalingTarget> target(Identifier scalingProfileId) {
        return SimplySwordsAPI.getSpellScalingDefinition(scalingProfileId)
                .map(SpellScalingDefinition::spellPowerTarget);
    }

}
