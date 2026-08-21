package net.sweenus.simplyswords.compat;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Test
    void everyWeaponHasItsThematicIronsSchool() {
        Map<Identifier, Identifier> assignments = SpellScalingComponents.defaultIronsSchools(
                SpellScalingComponents.Kind.WEAPON);
        Set<Identifier> asserted = new HashSet<>();

        assertSchool(assignments, asserted, "fire",
                "brimstone_claymore", "emberblade", "hearthflame", "molten_edge",
                "righteous_relic/damage", "sunfire/damage", "emberlash", "waxweaver",
                "wickpiercer", "tempest/fire", "flamewind");
        assertSchool(assignments, asserted, "ice",
                "frostfall", "livyatan/frost", "icewhisper", "tempest/frost");
        assertSchool(assignments, asserted, "lightning",
                "storms_edge", "stormscale", "awakened_stormscale", "ionbound_stormscale",
                "stormbringer", "livyatan/lightning", "thunderbrand", "mjolnir");
        assertSchool(assignments, asserted, "holy",
                "righteous_relic/healing", "sunfire/healing", "dawnquiver");
        assertSchool(assignments, asserted, "nature",
                "bramblethorn", "toxic_longsword", "hiveheart", "chompolotl");
        assertSchool(assignments, asserted, "evocation",
                "whisperwind", "enigma", "magispear", "riftmane");
        assertSchool(assignments, asserted, "ender",
                "shadowsting", "stars_edge", "magiscythe", "wraithfang", "wraithmaw");
        assertSchool(assignments, asserted, "blood",
                "watcher_claymore", "watching_warglaive", "soulkeeper", "twisted_blade",
                "soulstealer", "soulrender", "soulpyre", "slumbering_lichblade",
                "waking_lichblade", "awakened_lichblade", "bloodwake");
        assertSchool(assignments, asserted, "eldritch",
                "the_devourer", "soulstalker", "arcanethyst", "tainted_relic", "harbinger",
                "dreadwhisper", "gloampiercer", "magiblade", "caelestis", "dreadtide");

        assertEquals(Set.copyOf(SpellScalingComponents.componentIds(SpellScalingComponents.Kind.WEAPON)),
                asserted);
    }

    @Test
    void everyGemPowerHasItsThematicIronsSchool() {
        Map<Identifier, Identifier> assignments = SpellScalingComponents.defaultIronsSchools(
                SpellScalingComponents.Kind.GEM_POWER);
        Set<Identifier> asserted = new HashSet<>();

        assertSchool(assignments, asserted, "fire", "dragon_maw", "immolation", "radiance");
        assertSchool(assignments, asserted, "lightning", "stormlash");
        assertSchool(assignments, asserted, "nature",
                "faultline", "goat_stampede", "sniffer_slam", "verdant_trail", "wolf_pack");
        assertSchool(assignments, asserted, "evocation", "banehead_swarm", "evocation", "wing_buffet");
        assertSchool(assignments, asserted, "ender", "dancing_blades", "imbued", "runic_slash");
        assertSchool(assignments, asserted, "blood", "necromantic_arsenal");

        assertEquals(Set.copyOf(SpellScalingComponents.componentIds(SpellScalingComponents.Kind.GEM_POWER)),
                asserted);
    }

    @Test
    void thematicIronsChangesDoNotAlterSpellPowerDefaults() {
        Map<Identifier, Identifier> weapons = SpellScalingComponents.defaultSpellPowerSchools(
                SpellScalingComponents.Kind.WEAPON);
        Map<Identifier, Identifier> powers = SpellScalingComponents.defaultSpellPowerSchools(
                SpellScalingComponents.Kind.GEM_POWER);
        Set<Identifier> asserted = new HashSet<>();

        assertSpellPowerSchool(weapons, asserted, "soul",
                "the_devourer", "toxic_longsword", "soulstalker", "shadowsting", "tainted_relic",
                "harbinger", "dreadwhisper", "gloampiercer", "wraithfang", "wraithmaw");
        assertSpellPowerSchool(weapons, asserted, "arcane",
                "arcanethyst", "magispear", "magiblade", "riftmane");
        assertSpellPowerSchool(powers, asserted, "soul", "banehead_swarm");
        assertSpellPowerSchool(powers, asserted, "arcane", "dancing_blades");
        assertEquals(16, asserted.size());
        assertEquals(SpellScalingComponents.componentIds(SpellScalingComponents.Kind.WEAPON).size(),
                weapons.size());
        assertEquals(SpellScalingComponents.componentIds(SpellScalingComponents.Kind.GEM_POWER).size(),
                powers.size());
    }

    private static void assertComponent(String componentPath, String ownerPath, SpellScalingProfile profile) {
        SpellScalingComponents.Definition definition = SpellScalingComponents.get(id(componentPath)).orElseThrow();
        assertEquals(id(ownerPath), definition.ownerId());
        assertEquals(profile, definition.defaultProfile());
    }

    private static void assertSchool(Map<Identifier, Identifier> assignments, Set<Identifier> asserted,
                                     String schoolPath, String... componentPaths) {
        assertAssignments(assignments, asserted, "irons_spellbooks", schoolPath, componentPaths);
    }

    private static void assertSpellPowerSchool(Map<Identifier, Identifier> assignments, Set<Identifier> asserted,
                                               String schoolPath, String... componentPaths) {
        assertAssignments(assignments, asserted, "spell_power", schoolPath, componentPaths);
    }

    private static void assertAssignments(Map<Identifier, Identifier> assignments, Set<Identifier> asserted,
                                          String namespace, String schoolPath, String... componentPaths) {
        Identifier expected = Identifier.of(namespace, schoolPath);
        for (String componentPath : componentPaths) {
            Identifier componentId = id(componentPath);
            assertEquals(expected, assignments.get(componentId), componentId.toString());
            asserted.add(componentId);
        }
    }

    private static Identifier id(String path) {
        return Identifier.of("simplyswords", path);
    }
}
