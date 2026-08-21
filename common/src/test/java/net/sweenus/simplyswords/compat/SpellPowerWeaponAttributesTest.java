package net.sweenus.simplyswords.compat;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SpellPowerWeaponAttributesTest {

    @Test
    void defaultsUseWeaponHandedness() {
        Map<Identifier, Double> defaults = SpellPowerWeaponAttributes.defaultBonuses();

        assertEquals(2.0D, defaults.get(id("emberblade")));
        assertEquals(4.0D, defaults.get(id("brimstone_claymore")));
        assertEquals(4.0D, defaults.get(id("dawnquiver")));
        assertEquals(2.0D, defaults.get(id("magiscythe")));
    }

    @Test
    void hybridsGrantTheFullBonusToBothSchools() {
        assertEquals(
                List.of(SpellScalingProfile.FROST, SpellScalingProfile.FIRE),
                SpellPowerWeaponAttributes.profiles(id("tempest"))
        );
        assertEquals(
                List.of(SpellScalingProfile.HEALING, SpellScalingProfile.FIRE),
                SpellPowerWeaponAttributes.profiles(id("sunfire"))
        );
    }

    @Test
    void physicalAndUnattunedWeaponsAreExcluded() {
        Map<Identifier, Double> defaults = SpellPowerWeaponAttributes.defaultBonuses();

        assertFalse(defaults.containsKey(id("ribboncleaver")));
        assertFalse(defaults.containsKey(id("sword_on_a_stick")));
        assertEquals(List.of(), SpellPowerWeaponAttributes.profiles(id("iron_longsword")));
    }

    @Test
    void logicalProfilesRemainCanonical() {
        assertEquals(List.of(SpellScalingProfile.NATURE),
                SpellPowerWeaponAttributes.profiles(id("bramblethorn")));
        assertEquals(List.of(SpellScalingProfile.EVOCATION),
                SpellPowerWeaponAttributes.profiles(id("whisperwind")));
        assertEquals(List.of(SpellScalingProfile.ELDRITCH),
                SpellPowerWeaponAttributes.profiles(id("caelestis")));
    }

    private static Identifier id(String path) {
        return Identifier.of("simplyswords", path);
    }
}
