package net.sweenus.simplyswords.compat;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingTarget;
import net.sweenus.simplyswords.config.Config;

import java.util.Optional;

public final class SpellScalingAssignments {
    private SpellScalingAssignments() {
    }

    public static Optional<Identifier> spellPowerSchool(Identifier scalingId) {
        return school(scalingId, true, false);
    }

    public static Optional<Identifier> ironsSchool(Identifier scalingId) {
        return school(scalingId, false, false);
    }

    public static Optional<Identifier> defaultSpellPowerSchool(Identifier scalingId) {
        return school(scalingId, true, true);
    }

    public static Optional<Identifier> defaultIronsSchool(Identifier scalingId) {
        return school(scalingId, false, true);
    }

    private static Optional<Identifier> school(Identifier scalingId, boolean spellPower, boolean defaultsOnly) {
        if (scalingId == null) {
            return Optional.empty();
        }
        Optional<SpellScalingComponents.Definition> component = SpellScalingComponents.get(scalingId);
        if (component.isPresent()) {
            SpellScalingComponents.Definition definition = component.get();
            if (!defaultsOnly) {
                Identifier configured = configuredSchool(definition, spellPower);
                if (configured != null) {
                    return Optional.of(configured);
                }
            }
            return spellPower
                    ? SpellScalingComponents.defaultSpellPowerSchool(scalingId)
                    : SpellScalingComponents.defaultIronsSchool(scalingId);
        }
        return SimplySwordsAPI.getSpellScalingDefinition(scalingId)
                .map(definition -> spellPower
                        ? definition.spellPowerTarget()
                        : definition.ironsTarget())
                .map(SpellScalingTarget::schoolId);
    }

    private static Identifier configuredSchool(SpellScalingComponents.Definition definition, boolean spellPower) {
        if (spellPower) {
            return definition.kind() == SpellScalingComponents.Kind.WEAPON
                    ? Config.compatibility.spellPowerApi.get().weaponAbilitySchools.get(definition.id())
                    : Config.compatibility.spellPowerApi.get().gemPowerSchools.get(definition.id());
        }
        return definition.kind() == SpellScalingComponents.Kind.WEAPON
                ? Config.compatibility.ironsSpells.get().weaponAbilitySchools.get(definition.id())
                : Config.compatibility.ironsSpells.get().gemPowerSchools.get(definition.id());
    }
}
