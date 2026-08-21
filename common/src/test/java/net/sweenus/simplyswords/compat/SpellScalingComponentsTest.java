package net.sweenus.simplyswords.compat;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellScalingComponentsTest {

    @Test
    void independentlyScaledWeaponEffectsHaveStableComponentIds() {
        assertComponent("livyatan/frost", "livyatan", SpellScalingProfile.FROST);
        assertComponent("livyatan/lightning", "livyatan", SpellScalingProfile.LIGHTNING);
        assertComponent("righteous_relic/damage", "righteous_relic", SpellScalingProfile.FIRE);
        assertComponent("righteous_relic/healing", "righteous_relic", SpellScalingProfile.HEALING);
        assertComponent("sunfire/damage", "sunfire", SpellScalingProfile.FIRE);
        assertComponent("sunfire/healing", "sunfire", SpellScalingProfile.HEALING);
        assertComponent("tempest/fire", "tempest", SpellScalingProfile.FIRE);
        assertComponent("tempest/frost", "tempest", SpellScalingProfile.FROST);
    }

    @Test
    void progressionFormsAreConfiguredIndependently() {
        List<Identifier> weaponIds = SpellScalingComponents.componentIds(SpellScalingComponents.Kind.WEAPON);

        assertTrue(weaponIds.contains(id("slumbering_lichblade")));
        assertTrue(weaponIds.contains(id("waking_lichblade")));
        assertTrue(weaponIds.contains(id("awakened_lichblade")));
        assertTrue(weaponIds.contains(id("stormscale")));
        assertTrue(weaponIds.contains(id("awakened_stormscale")));
        assertTrue(weaponIds.contains(id("ionbound_stormscale")));
    }

    @Test
    void everySpellScaledGemPowerHasItsOwnAssignment() {
        List<Identifier> powerIds = SpellScalingComponents.componentIds(SpellScalingComponents.Kind.GEM_POWER);

        assertEquals(16, powerIds.size());
        assertEquals(powerIds.size(), powerIds.stream().distinct().count());
        assertTrue(powerIds.contains(id("immolation")));
        assertTrue(powerIds.contains(id("radiance")));
        assertTrue(powerIds.contains(id("stormlash")));
    }

    @Test
    void backendDefaultsUseTheirNativeSchoolIds() {
        Map<Identifier, Identifier> spellPower = SpellScalingComponents.defaultSpellPowerSchools(
                SpellScalingComponents.Kind.WEAPON);
        Map<Identifier, Identifier> irons = SpellScalingComponents.defaultIronsSchools(
                SpellScalingComponents.Kind.WEAPON);

        assertEquals(Identifier.of("spell_power", "frost"), spellPower.get(id("livyatan/frost")));
        assertEquals(Identifier.of("irons_spellbooks", "ice"), irons.get(id("livyatan/frost")));
        assertEquals(Identifier.of("spell_power", "lightning"), spellPower.get(id("livyatan/lightning")));
        assertEquals(Identifier.of("irons_spellbooks", "lightning"), irons.get(id("livyatan/lightning")));
    }

    private static void assertComponent(String componentPath, String ownerPath, SpellScalingProfile profile) {
        SpellScalingComponents.Definition definition = SpellScalingComponents.get(id(componentPath)).orElseThrow();
        assertEquals(id(ownerPath), definition.ownerId());
        assertEquals(profile, definition.defaultProfile());
    }

    private static Identifier id(String path) {
        return Identifier.of("simplyswords", path);
    }
}
