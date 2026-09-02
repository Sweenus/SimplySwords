package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase9AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase9UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Phase9RepairTest {

    @Test
    void everyPhase9DefinitionCarriesTheEventsItsConsumersEmit() {
        for (UniqueAbilityDefinition definition : Phase9UniqueAbilities.definitions()) {
            assertTrue(definition.supportsEvent(Phase9UniqueAbilities.HIT), definition.id().toString());
            assertTrue(definition.supportsEvent(Phase9UniqueAbilities.KILL), definition.id().toString());
            assertTrue(definition.supportsEvent(Phase9UniqueAbilities.FINISH), definition.id().toString());
        }
        assertTrue(Phase9UniqueAbilities.definitions().contains(Phase9UniqueAbilities.ARCANETHYST_IMPACT));
        assertEquals(Phase9AbilityTuning.EMPTY, Phase9UniqueAbilities.tuning(null));
    }

    @Test
    void additiveSettingsDoNotDisturbTheExistingSurface() {
        for (String added : List.of("SECONDARY_STATUS_DURATION_TICKS", "TERTIARY_DURATION_TICKS",
                "TERTIARY_INTERVAL_TICKS", "TERTIARY_RADIUS", "TERTIARY_TARGET_CAP",
                "TERTIARY_DAMAGE_MULTIPLIER")) {
            assertNotEquals(null, Phase9AbilityTuning.Setting.valueOf(added));
        }
        for (String existing : List.of("COOLDOWN_TICKS", "DURATION_TICKS", "RADIUS", "COUNT", "STACK_CAP",
                "REPAIR_AMOUNT", "PULL_STRENGTH", "MODE")) {
            assertNotEquals(null, Phase9AbilityTuning.Setting.valueOf(existing));
        }
    }

    @Test
    void arcanethystSlamAndStasisNoLongerShareOneChannel() {
        Phase9AbilityTuning impact = ArcanethystAssaultManager.impactBase(4.0, 10);
        Phase9AbilityTuning suspension = ArcanethystAssaultManager.suspensionBase(4.0, 18, 14, 6.0, 100);

        assertEquals(4.0, impact.get(s("FINAL_DAMAGE_MULTIPLIER"), 0), 1.0E-6);
        assertEquals(4.6, impact.multiply(s("FINAL_DAMAGE_MULTIPLIER"), 1.15, 4)
                .get(s("FINAL_DAMAGE_MULTIPLIER"), 0), 1.0E-6, "Ruinous Descent composes on the impact tuning");

        Phase9AbilityTuning stasis = suspension.with(s("FINAL_DAMAGE_MULTIPLIER"), 1.4)
                .with(s("DURATION_TICKS"), 50);
        assertEquals(1.4, stasis.get(s("FINAL_DAMAGE_MULTIPLIER"), 0), 1.0E-6);
        assertEquals(4.0, impact.get(s("FINAL_DAMAGE_MULTIPLIER"), 0), 1.0E-6,
                "Stasis Geode no longer collapses the slam multiplier");
    }

    @Test
    void arcanethystRelativeNodesComposeAgainstTheConfiguredBase() {
        Phase9AbilityTuning suspension = ArcanethystAssaultManager.suspensionBase(4.0, 18, 14, 6.0, 100);
        assertEquals(5.0, suspension.add(s("HEIGHT"), 1, 4).get(s("HEIGHT"), 0), 1.0E-6);
        assertEquals(22, suspension.add(s("WINDUP_TICKS"), 4, 18).integer(s("WINDUP_TICKS"), 0));
        assertEquals(22, suspension.add(s("DURATION_TICKS"), 8, 14).integer(s("DURATION_TICKS"), 0));
        assertEquals(7.0, suspension.add(s("RADIUS"), 1, 6).get(s("RADIUS"), 0), 1.0E-6);

        Phase9AbilityTuning spark = ArcanethystAssaultManager.sparkBase(25);
        assertEquals(32, spark.add(s("CHANCE"), 7, 25).integer(s("CHANCE"), 0));
        assertEquals(0.65F, ArcanethystAssaultManager.sparkDamage(
                spark.multiply(s("DAMAGE_MULTIPLIER"), .65, 1), 1.0F), 1.0E-5,
                "Unstable Gem's damage penalty now reaches a consumer");
        assertEquals(1.1F * 1.12F, ArcanethystAssaultManager.sparkDamage(
                spark.multiply(s("DAMAGE_MULTIPLIER"), 1.1, 1).multiply(s("SPELL_MULTIPLIER"), 1.12, 1),
                1.0F), 1.0E-5);
    }

    @Test
    void starsEdgeConstellationKeepsItsChannelsApart() {
        Phase9AbilityTuning base = StarsEdgeAbilityManager.constellationBase(
                120, 100, 5, 10, 2.5, 1.5, 12);
        assertEquals(140, base.add(s("DURATION_TICKS"), 20, 60).integer(s("DURATION_TICKS"), 0),
                "Trailing Sky lengthens the configured recording window");
        assertEquals(2.9, base.add(s("RADIUS"), .4, 2).get(s("RADIUS"), 0), 1.0E-6,
                "Bright Stars extends the configured explosion radius");
        assertEquals(3, base.add(s("INTERVAL_TICKS"), -2, 5).integer(s("INTERVAL_TICKS"), 0));

        Phase9AbilityTuning crowded = base.multiply(s("WIDTH"), .85, 1);
        assertEquals(.85, crowded.get(s("WIDTH"), 0), 1.0E-6);
        assertEquals(1.5, crowded.get(s("SECONDARY_RADIUS"), 0), 1.0E-6,
                "the contact-damage width is no longer collateral of the spacing multiplier");
        assertEquals(12, crowded.integer(s("COUNT"), 0), "and the star count is untouched");
    }

    @Test
    void starsEdgeSupernovaLeavesSolarBrandAlone() {
        Phase9AbilityTuning solar = StarsEdgeAbilityManager.solarBase()
                .with(s("PER_STACK_MULTIPLIER"), .03).with(s("STACK_CAP"), 4).with(s("LOCKOUT_TICKS"), 40)
                .with(s("COUNT"), 5).with(s("FLAT_DAMAGE"), 25);
        assertEquals(4, solar.integer(s("STACK_CAP"), 0), "Solar Brand keeps its four-hit chain cap");
        assertEquals(25, solar.integer(s("FLAT_DAMAGE"), 0), "Supernova Edge owns its own charge threshold");
    }

    @Test
    void magiscytheStormDurationAndRefreshRestoreAreSeparate() {
        Phase9AbilityTuning storm = MagiscytheMasteryManager.stormBase(400, 4.0, 5);
        assertEquals(430, storm.add(s("DURATION_TICKS"), 30, 400).integer(s("DURATION_TICKS"), 0),
                "Gathering Clouds lengthens the configured storm");
        assertEquals(13, storm.add(s("CHANCE"), 8, 5).integer(s("CHANCE"), 0),
                "Stable Front adds eight points to the base refresh chance");
        assertEquals(6.0, storm.add(s("RADIUS"), 2, 4).get(s("RADIUS"), 0), 1.0E-6);

        Phase9AbilityTuning everstorm = storm.with(s("SECONDARY_DURATION_TICKS"), 80).with(s("STACK_CAP"), 8);
        assertEquals(400, everstorm.integer(s("DURATION_TICKS"), 0),
                "Everstorm no longer cuts the initial storm to four seconds");
        assertEquals(80, everstorm.integer(s("SECONDARY_DURATION_TICKS"), 0));
        assertEquals(8, everstorm.integer(s("STACK_CAP"), 0));
    }

    @Test
    void magiscytheStrikeIntervalStaysRefreshRelative() {
        Phase9AbilityTuning storm = MagiscytheMasteryManager.stormBase(400, 4.0, 5);
        assertEquals(10, MagiscytheMasteryManager.strikeInterval(storm, 0));
        assertEquals(6, MagiscytheMasteryManager.strikeInterval(storm, 2));
        assertEquals(4, MagiscytheMasteryManager.strikeInterval(storm, 3));
        assertEquals(4, MagiscytheMasteryManager.strikeInterval(storm, 8), "the base floor still holds");

        Phase9AbilityTuning quickening = storm.with(s("DELAY_TICKS"), 2);
        assertEquals(8, MagiscytheMasteryManager.strikeInterval(quickening, 0),
                "Quickening Storm composes against the formula instead of replacing it");
        assertEquals(4, MagiscytheMasteryManager.strikeInterval(quickening, 2));
        assertEquals(3, MagiscytheMasteryManager.strikeInterval(quickening, 8),
                "and never drops below the hard floor");
    }

    private static Phase9AbilityTuning.Setting s(String name) {
        return Phase9AbilityTuning.Setting.valueOf(name);
    }
}
