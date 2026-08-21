package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class SpellScalingRegistry {
    private static final Map<Identifier, SpellScalingDefinition> DEFINITIONS = new LinkedHashMap<>();

    static {
        registerBuiltIn(SpellScalingProfile.FIRE, "fire", "scaleFire", "fire", "scaleFire");
        registerBuiltIn(SpellScalingProfile.FROST, "frost", "scaleFrost", "ice", "scaleIce");
        registerBuiltIn(SpellScalingProfile.LIGHTNING, "lightning", "scaleLightning", "lightning", "scaleLightning");
        registerBuiltIn(SpellScalingProfile.ARCANE, "arcane", "scaleArcane", "ender", "scaleEnder");
        registerBuiltIn(SpellScalingProfile.SOUL, "soul", "scaleSoul", "blood", "scaleBlood");
        registerBuiltIn(SpellScalingProfile.HEALING, "healing", "scaleHealing", "holy", "scaleHoly");
        registerBuiltIn(SpellScalingProfile.NATURE, "healing", "scaleHealing", "nature", "scaleNature");
        registerBuiltIn(SpellScalingProfile.EVOCATION, "arcane", "scaleArcane", "evocation", "scaleEvocation");
        registerBuiltIn(SpellScalingProfile.ELDRITCH, "soul", "scaleSoul", "eldritch", "scaleEldritch");
    }

    private SpellScalingRegistry() {
    }

    static synchronized void register(SpellScalingDefinition definition) {
        SpellScalingDefinition existing = DEFINITIONS.putIfAbsent(definition.id(), definition);
        if (existing != null && !existing.equals(definition)) {
            throw new IllegalStateException("Spell scaling profile " + definition.id() + " is already registered");
        }
    }

    static synchronized Optional<SpellScalingDefinition> get(Identifier id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    private static void registerBuiltIn(SpellScalingProfile profile, String spellPowerSchool, String spellPowerLabel,
                                        String ironsSchool, String ironsLabel) {
        register(new SpellScalingDefinition(
                profile.registryId(),
                new SpellScalingTarget(new Identifier("spell_power", spellPowerSchool), displayKey(spellPowerLabel)),
                new SpellScalingTarget(new Identifier("irons_spellbooks", ironsSchool), displayKey(ironsLabel))
        ));
    }

    private static String displayKey(String suffix) {
        return "item.simplyswords.compat." + suffix;
    }
}
