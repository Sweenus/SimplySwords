package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase10AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase10UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Phase10RepairTest {

    @Test
    void everyPhase10DefinitionCarriesTheEventsItsConsumersEmit() {
        for (UniqueAbilityDefinition definition : Phase10UniqueAbilities.definitions()) {
            assertTrue(definition.supportsEvent(Phase10UniqueAbilities.HIT), definition.id().toString());
            assertTrue(definition.supportsEvent(Phase10UniqueAbilities.KILL), definition.id().toString());
            assertTrue(definition.supportsEvent(Phase10UniqueAbilities.PULSE), definition.id().toString());
            assertTrue(definition.supportsEvent(Phase10UniqueAbilities.FINISH), definition.id().toString());
        }
        for (UniqueAbilityDefinition definition : List.of(Phase10UniqueAbilities.WARG_SANGUINE,
                Phase10UniqueAbilities.RIFTMANE_RIDER)) {
            assertTrue(Phase10UniqueAbilities.definitions().contains(definition));
        }
    }

    @Test
    void additiveSettingsDoNotDisturbTheExistingSurface() {
        assertEquals(Phase10AbilityTuning.Setting.SEARCH_RANGE,
                Phase10AbilityTuning.Setting.valueOf("SEARCH_RANGE"));
        assertEquals(Phase10AbilityTuning.Setting.SEARCH_RADIUS,
                Phase10AbilityTuning.Setting.valueOf("SEARCH_RADIUS"));
        for (String existing : List.of("COOLDOWN_TICKS", "DURATION_TICKS", "RADIUS", "RANGE", "COUNT",
                "TARGET_CAP", "STACK_CAP", "DAMAGE_MULTIPLIER", "MODE")) {
            assertNotEquals(null, Phase10AbilityTuning.Setting.valueOf(existing));
        }
        assertEquals(UniqueAbilityKind.PASSIVE, Phase10UniqueAbilities.RIFTMANE_HARRIER.kind());
        assertEquals(UniqueAbilityKind.ACTIVE, Phase10UniqueAbilities.RIFTMANE_RANK.kind());
    }

    @Test
    void warglaiveBasesPublishTheConfiguredValuesRelativeNodesComposeAgainst() {
        Phase10AbilityTuning mark = WatcherAbilityManager.warglaiveMarkBase(200, 5, 5, 12.0);
        assertEquals(200, mark.integer(s("DURATION_TICKS"), 0));
        assertEquals(5, mark.integer(s("STACK_CAP"), 0));
        assertEquals(12.0, mark.get(s("RANGE"), 0), 1.0E-6);
        assertEquals(240, mark.add(s("DURATION_TICKS"), 40, 160).integer(s("DURATION_TICKS"), 0),
                "Lingering Gaze composes against the configured duration, not its literal fallback");

        Phase10AbilityTuning hunt = WatcherAbilityManager.warglaiveHuntBase(10.0, 5);
        assertEquals(11.5, hunt.add(s("RADIUS"), 1.5, 8).get(s("RADIUS"), 0), 1.0E-6);
    }

    @Test
    void huntDamageMultipliersStackWithoutOverwritingEachOther() {
        Phase10AbilityTuning murderFlight = Phase10AbilityTuning.EMPTY
                .with(s("PER_STACK_MULTIPLIER"), .05).with(s("STACK_CAP"), 5);
        Phase10AbilityTuning dreadDividend = Phase10AbilityTuning.EMPTY
                .with(s("PER_STACK_MULTIPLIER"), .04).with(s("STACK_CAP"), 7);

        assertEquals(1.25F, WatcherAbilityManager.huntStrikeMultiplier(
                murderFlight, Phase10AbilityTuning.EMPTY, 5, 0), 1.0E-5);
        assertEquals(1.28F, WatcherAbilityManager.huntStrikeMultiplier(
                Phase10AbilityTuning.EMPTY, dreadDividend, 0, 7), 1.0E-5);
        assertEquals(1.25F * 1.28F, WatcherAbilityManager.huntStrikeMultiplier(
                murderFlight, dreadDividend, 5, 7), 1.0E-5);
        assertEquals(1.28F, WatcherAbilityManager.huntStrikeMultiplier(
                Phase10AbilityTuning.EMPTY, dreadDividend, 0, 40), 1.0E-5,
                "the Dread term stays capped");
    }

    @Test
    void killingGazeRefundsPerKillAndStopsAtItsCap() {
        Phase10AbilityTuning killingGaze = Phase10AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 24).with(s("REFUND_TICKS"), 15).with(s("LOCKOUT_TICKS"), 60);
        assertEquals(15, WatcherAbilityManager.huntKillRefund(killingGaze, 0));
        assertEquals(15, WatcherAbilityManager.huntKillRefund(killingGaze, 45));
        assertEquals(0, WatcherAbilityManager.huntKillRefund(killingGaze, 60));
        assertEquals(0, WatcherAbilityManager.huntKillRefund(Phase10AbilityTuning.EMPTY, 0),
                "no refund without the node");
    }

    @Test
    void thousandRibbonsSweepsForRealDamage() {
        double base = 0.95;
        Phase10AbilityTuning sweep = Phase10WeaponManager.ribbonPromiseBase(base)
                .with(s("RADIUS"), 5).with(s("TARGET_CAP"), 10)
                .with(s("SECONDARY_DAMAGE_MULTIPLIER"), .7);
        assertEquals(0.0F, Phase10WeaponManager.promiseBonusDamage(sweep, 10.0F, base), 1.0E-5,
                "the capstone adds no bonus of its own");
        assertEquals(13.65F, Phase10WeaponManager.cleaveDamage(sweep, 10.0F, base), 1.0E-4,
                "but the sweep derives from the full empowered hit");

        Phase10AbilityTuning broadCut = Phase10WeaponManager.ribbonPromiseBase(base)
                .with(s("TARGET_CAP"), 3).with(s("RADIUS"), 2.5)
                .with(s("SECONDARY_DAMAGE_MULTIPLIER"), .4);
        assertEquals(2, broadCut.integer(s("TARGET_CAP"), 1) - 1, "Broad Cut cleaves two enemies");
        assertEquals(7.8F, Phase10WeaponManager.cleaveDamage(broadCut, 10.0F, base), 1.0E-4);
    }

    @Test
    void rushTravelWindowComposesAgainstItsSeededBase() {
        Phase10AbilityTuning rush = Phase10WeaponManager.ribbonRushBase();
        assertEquals(8, Phase10WeaponManager.rushTravelTicks(rush));
        assertEquals(16, Phase10WeaponManager.rushTravelTicks(rush.multiply(s("RANGE"), 2, 8)));
        assertEquals(1.7, rush.get(s("SPEED"), 0), 1.0E-6);
        assertEquals(1.87, rush.multiply(s("SPEED"), 1.1, 1.7).get(s("SPEED"), 0), 1.0E-6);
    }

    @Test
    void riftmaneBasesKeepSearchRangeAndChargeDistanceApart() {
        Phase10AbilityTuning charger = RiftmaneAbilityManager.chargerBase(20.0, 1.2, 1.1, 1.0, 16);
        Phase10AbilityTuning harrier = RiftmaneAbilityManager.harrierBase(charger, 70, 60, 16.0, 4.0, 110.0);
        assertEquals(20.0, harrier.get(s("RANGE"), 0), 1.0E-6);
        assertEquals(16.0, harrier.get(s("SEARCH_RANGE"), 0), 1.0E-6);
        assertEquals(18.0, harrier.add(s("SEARCH_RANGE"), 2, 16).get(s("SEARCH_RANGE"), 0), 1.0E-6);
        assertEquals(20.0, harrier.add(s("SEARCH_RANGE"), 2, 16).get(s("RANGE"), 0), 1.0E-6,
                "Far Quarry no longer shortens the charge");
        assertEquals(78, harrier.add(s("CHANCE"), 8, 20).integer(s("CHANCE"), 0));
        assertEquals(1.45, charger.add(s("KNOCKBACK"), .25, 1).get(s("KNOCKBACK"), 0), 1.0E-6,
                "Scattering Line keeps the awakening-scaled base");

        Phase10AbilityTuning rider = RiftmaneAbilityManager.riderBase(charger, 2.0);
        assertEquals(60.0, rider.multiply(s("RANGE"), 3, 1).get(s("RANGE"), 0), 1.0E-6,
                "Ghost Road triples the configured distance instead of a literal one");
        assertEquals(2.4, rider.multiply(s("KNOCKBACK"), 2, 1).get(s("KNOCKBACK"), 0), 1.0E-6);
        assertEquals(2.35, rider.add(s("SECONDARY_RADIUS"), .35, 1).get(s("SECONDARY_RADIUS"), 0), 1.0E-6);

        Phase10AbilityTuning rank = RiftmaneAbilityManager.rankBase(charger, 5, 7.0);
        assertEquals(8.0, rank.add(s("WIDTH"), 1, 6).get(s("WIDTH"), 0), 1.0E-6);
        assertEquals(6, rank.add(s("COUNT"), 1, 5).integer(s("COUNT"), 0));
    }

    @Test
    void dawnquiverBasesComposeAgainstTheConfiguredValues() {
        Phase10AbilityTuning lesser = DawnquiverAbilityManager.lesserBase(100, 80, 24.0, 35.0);
        assertEquals(85, lesser.add(s("INTERVAL_TICKS"), -15, 80).integer(s("INTERVAL_TICKS"), 0),
                "Eager Halo removes exactly 0.75 seconds");
        assertEquals(27.0, lesser.add(s("RANGE"), 3, 20).get(s("RANGE"), 0), 1.0E-6);
        assertEquals(68, lesser.add(s("LOCKOUT_TICKS"), -12, 60).integer(s("LOCKOUT_TICKS"), 0));
        assertEquals(43.0, lesser.add(s("CHANCE"), 8, 20).get(s("CHANCE"), 0), 1.0E-6,
                "Bright Chance now rises above the configured chance instead of below it");

        Phase10AbilityTuning chorus = DawnquiverAbilityManager.chorusBase(3, 35.0);
        assertEquals(4, chorus.add(s("STACK_CAP"), 1, 3).integer(s("STACK_CAP"), 0));
        assertEquals(45.0, chorus.add(s("CHANCE"), 10, 20).get(s("CHANCE"), 0), 1.0E-6);
        assertEquals(15.0, chorus.add(s("PITY_CHANCE"), 15, 0).get(s("PITY_CHANCE"), 0), 1.0E-6,
                "Resonant Impact adds its own 15, not a literal fallback plus 15");

        Phase10AbilityTuning draw = DawnquiverAbilityManager.drawBase(80, .35, 4, .85, 6, 2, 2.0);
        assertEquals(70, draw.add(s("WINDUP_TICKS"), -10, 40).integer(s("WINDUP_TICKS"), 0));
        assertEquals(.30, draw.add(s("HEALTH_THRESHOLD"), -.05, .25).get(s("HEALTH_THRESHOLD"), 0), 1.0E-6);
        assertEquals(5, draw.add(s("TARGET_CAP"), 1, 3).integer(s("TARGET_CAP"), 0),
                "Sunlance actually pierces one more enemy");
        assertEquals(.90, draw.add(s("OUTGOING_MULTIPLIER"), .05, .75).get(s("OUTGOING_MULTIPLIER"), 0), 1.0E-6);
        assertEquals(4, draw.add(s("DELAY_TICKS"), -2, 8).integer(s("DELAY_TICKS"), 0));
        assertEquals(1, draw.add(s("INTERVAL_TICKS"), -1, 3).integer(s("INTERVAL_TICKS"), 0));
        assertEquals(2.5, draw.add(s("RADIUS"), .5, 2.5).get(s("RADIUS"), 0), 1.0E-6);
    }

    @Test
    void dawnquiverCooldownStaysTierRelative() {
        Phase10AbilityTuning plain = DawnquiverAbilityManager.drawBase(80, .35, 4, .85, 6, 2, 2.0);
        assertEquals(50, DawnquiverAbilityManager.releaseCooldown(plain, Phase10AbilityTuning.EMPTY, false, 50));
        assertEquals(150, DawnquiverAbilityManager.releaseCooldown(plain, Phase10AbilityTuning.EMPTY, false, 150));

        Phase10AbilityTuning barrage = plain.with(s("INCOMING_MULTIPLIER"), 1.25);
        assertEquals(63, DawnquiverAbilityManager.releaseCooldown(barrage, Phase10AbilityTuning.EMPTY, false, 50),
                "Angelic Barrage scales the tier cooldown instead of replacing it with a flat value");
        assertEquals(188, DawnquiverAbilityManager.releaseCooldown(barrage, Phase10AbilityTuning.EMPTY, false, 150));

        Phase10AbilityTuning perfect = Phase10AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 15).with(s("INCOMING_MULTIPLIER"), .8);
        assertEquals(120, DawnquiverAbilityManager.releaseCooldown(plain, perfect, true, 150));
        assertEquals(150, DawnquiverAbilityManager.releaseCooldown(plain, perfect, false, 150),
                "and only once maximum Chorus has actually been reached");
    }

    @Test
    void dreadtideBranchesNoLongerShareOneTuning() {
        Phase10AbilityTuning cloak = Phase10WeaponManager.dreadCloakBase();
        assertEquals(.9, cloak.get(s("INCOMING_MULTIPLIER"), 0), 1.0E-6);
        assertEquals(5, cloak.integer(s("STACK_CAP"), 0));

        Phase10AbilityTuning pact = Phase10WeaponManager.dreadPactBase(60, 1200);
        assertEquals(60, pact.integer(s("INTERVAL_TICKS"), 0));
        assertEquals(1200, pact.integer(s("SECONDARY_DURATION_TICKS"), 0));
        assertEquals(70, pact.with(s("INTERVAL_TICKS"), 70).integer(s("INTERVAL_TICKS"), 0));

        Phase10AbilityTuning assault = Phase10WeaponManager.dreadAssaultBase(250, 12);
        assertEquals(280, assault.add(s("DURATION_TICKS"), 30, 250).integer(s("DURATION_TICKS"), 0),
                "Long Call lengthens the configured duration instead of halving it");
        assertEquals(11, assault.add(s("INTERVAL_TICKS"), -1, 12).integer(s("INTERVAL_TICKS"), 0));
        assertEquals(500, assault.multiply(s("DURATION_TICKS"), 2, 250).integer(s("DURATION_TICKS"), 0));

        Phase10AbilityTuning tolerance = pact.with(s("SECONDARY_CORRUPTION"), 60)
                .with(s("INCOMING_MULTIPLIER"), .92);
        Phase10AbilityTuning madness = tolerance.with(s("FINAL_CORRUPTION"), 80)
                .with(s("HEAL_MULTIPLIER"), 1.08).with(s("SPEED"), .7);
        assertEquals(.9, cloak.get(s("INCOMING_MULTIPLIER"), 0), 1.0E-6,
                "the cloak's per-stack reduction is untouched by either Corruption node");
        assertEquals(1.08, madness.get(s("HEAL_MULTIPLIER"), 0), 1.0E-6);
        assertEquals(.92, madness.get(s("INCOMING_MULTIPLIER"), 0), 1.0E-6,
                "and the two Corruption nodes no longer overwrite one another");
    }

    private static Phase10AbilityTuning.Setting s(String name) {
        return Phase10AbilityTuning.Setting.valueOf(name);
    }
}
