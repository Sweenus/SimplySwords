package net.sweenus.simplyswords.api;

import java.util.Locale;

//
// Loader-neutral spell schools used by Simply Swords ability scaling.
//
// The platform modules map these profiles to the closest school supplied by
// Spell Power Attributes or Iron's Spells 'n Spellbooks.
//
public enum SpellScalingProfile {
    FIRE("fire"),
    FROST("frost"),
    LIGHTNING("lightning"),
    ARCANE("arcane"),
    SOUL("soul"),
    HEALING("healing"),
    NATURE("nature"),
    EVOCATION("evocation"),
    ELDRITCH("eldritch");

    private final String id;

    SpellScalingProfile(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    //
    // Resolves legacy string school names without breaking existing addons.
    //
    public static SpellScalingProfile fromLegacyName(String school) {
        String value = school == null ? "" : school.toLowerCase(Locale.ROOT);
        if (value.contains("lightning")) return LIGHTNING;
        if (value.contains("fire")) return FIRE;
        if (value.contains("frost") || value.contains("ice")) return FROST;
        if (value.contains("nature") || value.contains("earth")) return NATURE;
        if (value.contains("evocation") || value.contains("wind")) return EVOCATION;
        if (value.contains("eldritch")) return ELDRITCH;
        if (value.contains("arcane")) return ARCANE;
        if (value.contains("soul")) return SOUL;
        if (value.contains("healing") || value.contains("holy")) return HEALING;
        return ARCANE;
    }
}
