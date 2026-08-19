package net.sweenus.simplyswords.gametest;

import net.sweenus.simplyswords.api.SpellScalingProfile;

import java.util.List;

final class AbilityBalanceCatalog {

    private static final String NO_SCALING =
            "; no spell scaling wired - uses attack damage directly, so it can never respond to caster gear";

    private AbilityBalanceCatalog() {
    }

    static List<AbilityBalanceSpec> all() {
        return List.of(
                spec("arcanethyst", "Arcane Assault", SpellScalingProfile.ARCANE, ActivationMode.ACTIVE_ABILITY, 160, 3, BalanceCategory.AOE_CONTROL, ScenarioSetup.NONE, false, "Final slam damage is the balance event"),
                spec("brimstone_claymore", "Brimstone", SpellScalingProfile.FIRE, ActivationMode.ACTIVE_ABILITY, 220, 3, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, false, "Includes passive and final burst damage"),
                spec("chompolotl", "Axolotl summon", SpellScalingProfile.NATURE, ActivationMode.SUMMON, 560, 3, BalanceCategory.SUMMON, ScenarioSetup.NONE, true, "Runs through repeated summon attacks"),
                spec("dreadtide", "Dreadtide", SpellScalingProfile.ELDRITCH, ActivationMode.ACTIVE_ABILITY, 200, 3, BalanceCategory.LONG_COOLDOWN_BURST, ScenarioSetup.NONE, false, "Optional Eldritch End weapon"),
                spec("emberlash", "Smoulder", SpellScalingProfile.FIRE, ActivationMode.PASSIVE_ON_HIT, 160, 3, BalanceCategory.FREQUENT_PASSIVE, ScenarioSetup.NONE, false, true, "Includes sustained smoulder contribution; on-hit only, judge on rotation ability DPS"),
                spec("enigma", "Elemental Vortex", SpellScalingProfile.EVOCATION, ActivationMode.AURA, 240, 3, BalanceCategory.AOE_CONTROL, ScenarioSetup.NONE, false, "Persistent crowd-control field"),
                spec("flamewind", "Flamewind", SpellScalingProfile.FIRE, ActivationMode.ACTIVE_ABILITY, 160, 3, BalanceCategory.AOE_CONTROL, ScenarioSetup.NONE, false, "Includes spread and detonation"),
                spec("frostfall", "Frostfall", SpellScalingProfile.FROST, ActivationMode.ACTIVE_ABILITY, 200, 3, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, false, "Includes initial hit and pulses"),
                spec("harbinger", "Harbinger", SpellScalingProfile.SOUL, ActivationMode.AURA, 260, 3, BalanceCategory.PERSISTENT_AURA, ScenarioSetup.NONE, false, "Runs through the persistent area effect"),
                spec("icewhisper", "Icewhisper", SpellScalingProfile.FROST, ActivationMode.ACTIVE_ABILITY, 200, 3, BalanceCategory.HYBRID, ScenarioSetup.NONE, false, "Includes passive aura and comet"),
                spec("awakened_lichblade", "Soul Anguish", SpellScalingProfile.SOUL, ActivationMode.CHANNEL, 260, 3, BalanceCategory.HYBRID, ScenarioSetup.NONE, false, "Includes passive aura and active damage"),
                spec("livyatan", "Livyatan", SpellScalingProfile.FROST, ActivationMode.PROJECTILE, 240, 4, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, true, "Includes wave, return, and return lightning"),
                spec("magiblade", "Magiblade summon", SpellScalingProfile.ARCANE, ActivationMode.SUMMON, 320, 3, BalanceCategory.SUMMON, ScenarioSetup.NONE, true, "Runs through the summon lifetime"),
                spec("magiscythe", "Magiscythe", SpellScalingProfile.ARCANE, ActivationMode.CHANNEL, 460, 3, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, false, "Runs through the full active duration"),
                spec("magispear", "Magispear", SpellScalingProfile.ARCANE, ActivationMode.THROW, 260, 4, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, true, "Includes main, secondary, and throw damage"),
                spec("riftmane", "Riftmane", SpellScalingProfile.ARCANE, ActivationMode.SUMMON, 260, 3, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, true, "Includes charger and passive contribution"),
                spec("shadowsting", "Shadow Dance", SpellScalingProfile.SOUL, ActivationMode.ACTIVE_ABILITY, 220, 3, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, false, "Includes clone strikes and chained hits"),
                spec("soulpyre", "Soulpyre", SpellScalingProfile.SOUL, ActivationMode.ACTIVE_ABILITY, 680, 3, BalanceCategory.LONG_COOLDOWN_BURST, ScenarioSetup.NONE, false, "Runs through all ability stages"),
                spec("soulkeeper", "Lanterns", SpellScalingProfile.SOUL, ActivationMode.AURA, 300, 3, BalanceCategory.PERSISTENT_AURA, ScenarioSetup.NONE, true, "Uses real lantern intersections"),
                spec("soulrender", "Soulrender", SpellScalingProfile.SOUL, ActivationMode.ACTIVE_ABILITY, 560, 3, BalanceCategory.HYBRID, ScenarioSetup.PRIME_WITH_MELEE, false, "Includes mark setup before payoff"),
                spec("stars_edge", "Stars Edge", SpellScalingProfile.ARCANE, ActivationMode.ACTIVE_ABILITY, 220, 3, BalanceCategory.HYBRID, ScenarioSetup.NONE, false, "Includes passive and constellation damage"),
                spec("mjolnir", "Mjolnir storm", SpellScalingProfile.LIGHTNING, ActivationMode.THROW, 300, 4, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, true, "Includes storm, conductive bursts, and thunderclap"),
                spec("stormbringer", "Stormbringer", SpellScalingProfile.LIGHTNING, ActivationMode.HOLD_RELEASE, 220, 3, BalanceCategory.LONG_COOLDOWN_BURST, ScenarioSetup.NONE, true, "Includes charge, main hit, and chain damage"),
                spec("storms_edge", "Stormbreak", SpellScalingProfile.LIGHTNING, ActivationMode.ACTIVE_ABILITY, 160, 3, BalanceCategory.SHORT_COOLDOWN_ACTIVE, ScenarioSetup.NONE, false, "Includes dash and thunderclap"),
                spec("sunfire", "Sunfire", SpellScalingProfile.HEALING, ActivationMode.AURA, 240, 3, BalanceCategory.HYBRID, ScenarioSetup.NONE, false, "Damage is included; healing is excluded from DPS status"),
                spec("tempest", "Tempest", SpellScalingProfile.FIRE, ActivationMode.AURA, 1320, 3, BalanceCategory.PERSISTENT_AURA, ScenarioSetup.NONE, false, "Uses float-accurate stored vortex damage; coefficient remains provisional"),
                spec("thunderbrand", "Thunderbrand", SpellScalingProfile.LIGHTNING, ActivationMode.PROJECTILE, 220, 4, BalanceCategory.AOE_CONTROL, ScenarioSetup.NONE, true, "Includes chained targets"),
                spec("wraithfang", "Wraithfang", SpellScalingProfile.SOUL, ActivationMode.THROW, 160, 4, BalanceCategory.SHORT_COOLDOWN_ACTIVE, ScenarioSetup.NONE, true, "Uses damage-specific value scaling with diminishing returns"),
                spec("the_devourer", "Devourer", SpellScalingProfile.SOUL, ActivationMode.CHANNEL, 900, 3, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, false, "Coefficient remains test-derived and provisional"),
                spec("bloodwake", "Bloodwake", SpellScalingProfile.SOUL, ActivationMode.ACTIVE_ABILITY, 160, 3, BalanceCategory.SHORT_COOLDOWN_ACTIVE, ScenarioSetup.NONE, false, "Very short cooldown, expect high uptime"),
                spec("bramblethorn", "Bramblethorn", SpellScalingProfile.NATURE, ActivationMode.ACTIVE_ABILITY, 200, 3, BalanceCategory.SHORT_COOLDOWN_ACTIVE, ScenarioSetup.NONE, false, "Also carries an on-hit component"),
                spec("caelestis", "Caelestis", SpellScalingProfile.ELDRITCH, ActivationMode.ACTIVE_ABILITY, 1000, 3, BalanceCategory.LONG_COOLDOWN_BURST, ScenarioSetup.NONE, false, "Ninety second cooldown; no eldritch upgrade orb exists in Iron's"),
                spec("dawnquiver", "Dawnquiver", SpellScalingProfile.HEALING, ActivationMode.HOLD_RELEASE, 200, 4, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, true, "Charge and release; healing school scaling"),
                spec("dreadwhisper", "Dreadwhisper", SpellScalingProfile.SOUL, ActivationMode.ACTIVE_ABILITY, 200, 3, BalanceCategory.SHORT_COOLDOWN_ACTIVE, ScenarioSetup.NONE, false, "Uses attackScaledDamage only" + NO_SCALING),
                spec("emberblade", "Ember Ire", SpellScalingProfile.FIRE, ActivationMode.HOLD_RELEASE, 200, 3, BalanceCategory.SHORT_COOLDOWN_ACTIVE, ScenarioSetup.NONE, false, "Charge and release with an on-hit component"),
                spec("gloampiercer", "Gloampiercer", SpellScalingProfile.SOUL, ActivationMode.ACTIVE_ABILITY, 220, 3, BalanceCategory.LONG_COOLDOWN_BURST, ScenarioSetup.NONE, false, "Uses raw entity attack damage" + NO_SCALING),
                spec("hearthflame", "Hearthflame", SpellScalingProfile.FIRE, ActivationMode.AURA, 260, 3, BalanceCategory.PERSISTENT_AURA, ScenarioSetup.NONE, false, "Persistent hearth field"),
                spec("hiveheart", "Hiveheart", SpellScalingProfile.NATURE, ActivationMode.SUMMON, 300, 3, BalanceCategory.SUMMON, ScenarioSetup.NONE, true, "Runs through the swarm lifetime"),
                spec("ionbound_stormscale", "Ionbound Stormscale", SpellScalingProfile.LIGHTNING, ActivationMode.ACTIVE_ABILITY, 160, 3, BalanceCategory.SHORT_COOLDOWN_ACTIVE, ScenarioSetup.NONE, false, "Upgraded stormscale variant"),
                spec("molten_edge", "Molten Edge", SpellScalingProfile.FIRE, ActivationMode.ACTIVE_ABILITY, 160, 3, BalanceCategory.FREQUENT_PASSIVE, ScenarioSetup.NONE, false, "Two second cooldown, near-permanent uptime"),
                spec("ribboncleaver", "Ribboncleaver", SpellScalingProfile.ARCANE, ActivationMode.ACTIVE_ABILITY, 160, 3, BalanceCategory.SHORT_COOLDOWN_ACTIVE, ScenarioSetup.NONE, false, "No scaling helper used at all" + NO_SCALING),
                spec("soulstalker", "Soulstalker", SpellScalingProfile.SOUL, ActivationMode.ACTIVE_ABILITY, 900, 3, BalanceCategory.LONG_COOLDOWN_BURST, ScenarioSetup.NONE, false, "Leap with a long cooldown" + NO_SCALING),
                spec("soulstealer", "Soulstealer", SpellScalingProfile.SOUL, ActivationMode.PASSIVE_ON_HIT, 160, 3, BalanceCategory.FREQUENT_PASSIVE, ScenarioSetup.NONE, false, true, "One second cooldown on-hit steal"),
                spec("stormscale", "Stormscale", SpellScalingProfile.LIGHTNING, ActivationMode.ACTIVE_ABILITY, 900, 3, BalanceCategory.LONG_COOLDOWN_BURST, ScenarioSetup.NONE, false, "Fifty second cooldown with a long active window"),
                spec("toxic_longsword", "Plague", SpellScalingProfile.SOUL, ActivationMode.PASSIVE_ON_HIT, 200, 3, BalanceCategory.FREQUENT_PASSIVE, ScenarioSetup.PRIME_WITH_MELEE, false, true, "Longsword of the Plague; pure on-hit passive with no active ability"),
                spec("twisted_blade", "Twisted Blade", SpellScalingProfile.SOUL, ActivationMode.ACTIVE_ABILITY, 200, 3, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, false, "Uses attackScaledDamage only" + NO_SCALING),
                spec("waxweaver", "Waxweaver", SpellScalingProfile.FIRE, ActivationMode.ACTIVE_ABILITY, 300, 3, BalanceCategory.LONG_COOLDOWN_BURST, ScenarioSetup.NONE, false, "Sixty second cooldown"),
                spec("whisperwind", "Whisperwind", SpellScalingProfile.EVOCATION, ActivationMode.ACTIVE_ABILITY, 200, 3, BalanceCategory.SHORT_COOLDOWN_ACTIVE, ScenarioSetup.NONE, false, "Evocation school scaling"),
                spec("wickpiercer", "Wickpiercer", SpellScalingProfile.FIRE, ActivationMode.ACTIVE_ABILITY, 200, 3, BalanceCategory.MULTI_HIT_ACTIVE, ScenarioSetup.NONE, false, "Short active window with an on-hit component"),
                spec("wraithmaw", "Wraithmaw", SpellScalingProfile.SOUL, ActivationMode.ACTIVE_ABILITY, 260, 3, BalanceCategory.LONG_COOLDOWN_BURST, ScenarioSetup.NONE, false, "Uses raw entity attack damage" + NO_SCALING)
        );
    }

    private static AbilityBalanceSpec spec(String weaponId, String ability, SpellScalingProfile profile,
                                           ActivationMode activationMode, int durationTicks, int targetDistance,
                                           BalanceCategory category, ScenarioSetup setup,
                                           boolean reliabilitySensitive, String notes) {
        return spec(weaponId, ability, profile, activationMode, durationTicks, targetDistance, category, setup,
                reliabilitySensitive, false, notes);
    }

    private static AbilityBalanceSpec spec(String weaponId, String ability, SpellScalingProfile profile,
                                           ActivationMode activationMode, int durationTicks, int targetDistance,
                                           BalanceCategory category, ScenarioSetup setup,
                                           boolean reliabilitySensitive, boolean requiresMeleeTrigger, String notes) {
        return new AbilityBalanceSpec(weaponId, ability, profile, activationMode, durationTicks,
                targetDistance, category, setup, reliabilitySensitive, requiresMeleeTrigger, notes);
    }
}
