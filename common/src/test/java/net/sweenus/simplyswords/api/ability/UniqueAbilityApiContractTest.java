package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponSecondaryAction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UniqueAbilityApiContractTest {

    @Test
    void secondaryActionsAreSeparateAndOptIn() throws NoSuchMethodException {
        assertTrue(Modifier.isPublic(UniqueWeaponSecondaryAction.class.getModifiers()));
        assertTrue(!UniqueWeaponSecondaryAction.class.isAssignableFrom(UniqueWeaponActiveAbility.class));
        assertEquals(net.minecraft.util.TypedActionResult.class,
                UniqueWeaponSecondaryAction.class.getMethod("startPlayerSecondaryAbility",
                        net.minecraft.world.World.class, net.minecraft.entity.player.PlayerEntity.class,
                        net.minecraft.util.Hand.class).getReturnType());
    }

    @Test
    void defeatedOriginChainTargetingIsAdditive() throws NoSuchMethodException {
        assertEquals(List.class, net.sweenus.simplyswords.api.SimplySwordsAPI.class.getMethod(
                "findAbilityChainTargetsFromPosition", net.minecraft.server.world.ServerWorld.class,
                net.minecraft.entity.LivingEntity.class, net.minecraft.util.math.Vec3d.class,
                int.class, double.class).getReturnType());
        assertEquals(List.class, net.sweenus.simplyswords.api.SimplySwordsAPI.class.getMethod(
                "findAbilityChainTargets", net.minecraft.server.world.ServerWorld.class,
                net.minecraft.entity.LivingEntity.class, net.minecraft.entity.LivingEntity.class,
                int.class, double.class).getReturnType());
    }

    @Test
    void startedExecutionHandoffIsAdditiveAndClearsAfterOneTake() {
        UniqueAbilityDefinition definition = UniqueAbilityDefinition.builder(id("handoff"),
                UniqueAbilityKind.ACTIVE).build();
        UniqueAbilityExecution execution = new UniqueAbilityExecution(1, definition, null,
                new UniqueAbilityTuning.Builder(definition).build(), List.of());
        UniqueAbilityApi.clearStartedExecution();

        assertNull(UniqueAbilityApi.takeStartedExecution());

        UniqueAbilityApi.publishStartedExecution(execution);

        assertEquals(execution, UniqueAbilityApi.takeStartedExecution());
        assertNull(UniqueAbilityApi.takeStartedExecution());

        UniqueAbilityApi.publishStartedExecution(execution);
        UniqueAbilityApi.publishStartedExecution(null);

        assertNull(UniqueAbilityApi.takeStartedExecution());
    }

    @Test
    void abyssalSpectralSettingAdditionsAreDisabledByDefaultAndDoNotDisturbExistingOnes() {
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryTuning.EMPTY;

        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.REPEAT_WINDOW_TICKS, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.INCOMING_DREAD_THRESHOLD, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.LOW_HEALTH_PERCENT, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.EXECUTE_DREAD_THRESHOLD, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.CLAIM_BONUS_CAP, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.CHAIN_RANGE, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.CHAIN_DELAY_TICKS, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.GLOAM_MOVE_RANGE, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.GLOAM_VULNERABILITY_BONUS, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.HAUNT_RANGE, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.BURIAL_RANGE, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.RECALL_RANGE, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.GRAVEWALK_RANGE, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.BURST_RANGE, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.BURST_WINDOW_TICKS, 0));
        assertEquals(1.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.LAUNCH_COUNT, 1));
        assertEquals(2.0, tuning.with(AbyssalSpectralMasteryTuning.Setting.HAUNT_INTERVAL_TICKS, 40)
                .get(AbyssalSpectralMasteryTuning.Setting.INTERVAL_TICKS, 2));
        assertEquals(30.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.REPEAT_WINDOW_TICKS, 30));
        assertEquals(100.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.LOW_HEALTH_PERCENT, 140));
        assertEquals(4.0, tuning.with(AbyssalSpectralMasteryTuning.Setting.THRESHOLD, 4)
                .get(AbyssalSpectralMasteryTuning.Setting.THRESHOLD, 0));
        assertEquals(0.0, tuning.with(AbyssalSpectralMasteryTuning.Setting.REPEAT_WINDOW_TICKS, 30)
                .get(AbyssalSpectralMasteryTuning.Setting.THRESHOLD, 0));
    }

    @Test
    void stormSoulSettingAdditionsAreDisabledByDefaultAndDoNotDisturbExistingOnes() {
        net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning tuning =
                net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning.EMPTY;
        var setting = net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning.Setting.class;

        assertEquals(0.0, tuning.get(Enum.valueOf(setting, "RADIUS_CAP"), 0));
        assertEquals(0.0, tuning.get(Enum.valueOf(setting, "GROWTH_CAP_LIMIT"), 0));
        assertEquals(0.0, tuning.get(Enum.valueOf(setting, "COOLDOWN_BASE_TICKS"), 0));
        assertEquals(0.0, tuning.get(Enum.valueOf(setting, "PLANT_DAMAGE_MULTIPLIER"), 0));
        assertEquals(0.0, tuning.get(Enum.valueOf(setting, "CHAIN_DAMAGE_MULTIPLIER"), 0));
        assertEquals(0.0, tuning.get(Enum.valueOf(setting, "WARD_LOCKOUT_TICKS"), 0));
        assertFalse(tuning.has(Enum.valueOf(setting, "RADIUS_CAP")));
        assertEquals(3.5, tuning.with(Enum.valueOf(setting, "RADIUS_CAP"), 4.5)
                .get(Enum.valueOf(setting, "RADIUS"), 3.5));
        assertEquals(0.0, tuning.with(Enum.valueOf(setting, "CONDUCTIVE_TARGET_CAP"), 12)
                .get(Enum.valueOf(setting, "TARGET_CAP"), 0));
    }

    @Test
    void tuningKeysClampAndRejectForeignDefinitions() {
        UniqueAbilityKey<Integer> chance = UniqueAbilityKey.integer(id("chance"), 10, 0, 100);
        UniqueAbilityKey<Integer> foreign = UniqueAbilityKey.integer(id("foreign"), 0, 0, 10);
        UniqueAbilityDefinition definition = UniqueAbilityDefinition.builder(id("test"), UniqueAbilityKind.PASSIVE)
                .key(chance).build();
        UniqueAbilityTuning.Builder tuning = new UniqueAbilityTuning.Builder(definition);

        tuning.set(chance, 140);

        assertEquals(100, tuning.get(chance));
        assertThrows(IllegalArgumentException.class, () -> tuning.set(foreign, 1));
    }

    @Test
    void lifecycleIsOrderedAndTerminalEventsAreIdempotent() {
        Identifier hit = id("hit");
        UniqueAbilityDefinition definition = UniqueAbilityDefinition.builder(id("test"), UniqueAbilityKind.ACTIVE)
                .event(hit).build();
        List<UniqueAbilityPhase> phases = new ArrayList<>();
        UniqueAbilityExecution execution = new UniqueAbilityExecution(1, definition, null,
                new UniqueAbilityTuning.Builder(definition).build(), List.of(event -> phases.add(event.phase())));

        UniqueAbilityApi.start(execution);
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, hit, null, 1, 2.0);
        UniqueAbilityApi.finish(execution, definition.id(), 1);
        UniqueAbilityApi.cancel(execution);

        assertEquals(List.of(UniqueAbilityPhase.START, UniqueAbilityPhase.HIT, UniqueAbilityPhase.FINISH), phases);
        assertTrue(execution.isTerminal());
    }

    @Test
    void definitionsRejectDuplicateKeysAndEvents() {
        UniqueAbilityKey<Boolean> flag = UniqueAbilityKey.flag(id("flag"), false);
        assertThrows(IllegalArgumentException.class, () -> UniqueAbilityDefinition.builder(
                id("duplicate_key"), UniqueAbilityKind.ACTIVE).key(flag).key(flag));
        assertThrows(IllegalArgumentException.class, () -> UniqueAbilityDefinition.builder(
                id("duplicate_event"), UniqueAbilityKind.ACTIVE).event(id("hit")).event(id("hit")));
    }

    @Test
    void stormsEdgeAdditionsAreDisabledByDefaultAndAdditive() {
        UniqueAbilityTuning tuning = new UniqueAbilityTuning.Builder(BuiltinUniqueAbilities.STORMBREAK).build();

        assertEquals(BuiltinUniqueAbilities.CORRIDOR_FORCE_OUTWARD,
                tuning.get(BuiltinUniqueAbilities.CORRIDOR_FORCE_MODE));
        assertEquals(0, tuning.get(BuiltinUniqueAbilities.AFTERIMAGE_DURATION_TICKS));
        assertEquals(0, tuning.get(BuiltinUniqueAbilities.AFTERSHOCK_DELAY_TICKS));
        assertEquals(0, tuning.get(BuiltinUniqueAbilities.SUPERCELL_DURATION_TICKS));
        assertTrue(BuiltinUniqueAbilities.STORMBREAK.supportsEvent(BuiltinUniqueAbilities.DASH_END));
        assertTrue(BuiltinUniqueAbilities.STORMBREAK.supportsEvent(BuiltinUniqueAbilities.SUPERCELL_HIT));
        assertTrue(BuiltinUniqueAbilities.STORMS_EDGE_MELEE.supportsEvent(BuiltinUniqueAbilities.MELEE_HIT));
    }

    @Test
    void brimstoneDefinitionsPreserveBaselineAndDisableAdditions() {
        UniqueAbilityTuning eruption = new UniqueAbilityTuning.Builder(
                BuiltinUniqueAbilities.BRIMSTONE_ERUPTION).build();
        UniqueAbilityTuning rite = new UniqueAbilityTuning.Builder(BuiltinUniqueAbilities.BRIMSTONE_RITE).build();

        assertEquals(15, eruption.get(BuiltinUniqueAbilities.BRIMSTONE_PROC_CHANCE));
        assertEquals(3.0, eruption.get(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_RADIUS));
        assertEquals(0, eruption.get(BuiltinUniqueAbilities.BRIMSTONE_CINDER_COUNT));
        assertEquals(0, eruption.get(BuiltinUniqueAbilities.BRIMSTONE_CHAIN_MAX_DETONATIONS));
        assertEquals(120, rite.get(BuiltinUniqueAbilities.BRIMSTONE_RITE_DURATION_TICKS));
        assertEquals(20, rite.get(BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_INTERVAL_TICKS));
        assertEquals(0, rite.get(BuiltinUniqueAbilities.BRIMSTONE_WAKE_DURATION_TICKS));
        assertTrue(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION.supportsEvent(
                BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_HIT));
        assertTrue(BuiltinUniqueAbilities.BRIMSTONE_RITE.supportsEvent(
                BuiltinUniqueAbilities.BRIMSTONE_EMERGENCY_PLUNGE));
    }

    @Test
    void abyssalSpectralDefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = List.of(
                AbyssalSpectralMasteryAbilities.WATCHER_DREAD, AbyssalSpectralMasteryAbilities.WATCHER_OMEN,
                AbyssalSpectralMasteryAbilities.DEVOURER_MASS, AbyssalSpectralMasteryAbilities.DEVOURER_REPRISAL,
                AbyssalSpectralMasteryAbilities.WICKPIERCER_THROW, AbyssalSpectralMasteryAbilities.WICKPIERCER_REVIVE,
                AbyssalSpectralMasteryAbilities.GLOAMPIERCER_AMBUSH, AbyssalSpectralMasteryAbilities.GLOAMPIERCER_BARRAGE,
                AbyssalSpectralMasteryAbilities.WRAITHFANG_THROW, AbyssalSpectralMasteryAbilities.WRAITHMAW_MUSTER);
        assertEquals(10, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(AbyssalSpectralMasteryAbilities.TUNING));
            assertTrue(definition.supportsEvent(AbyssalSpectralMasteryAbilities.HIT));
            assertTrue(definition.supportsEvent(AbyssalSpectralMasteryAbilities.COLLAPSE));
        }
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryTuning.EMPTY
                .with(AbyssalSpectralMasteryTuning.Setting.EXECUTE_THRESHOLD, 5)
                .with(AbyssalSpectralMasteryTuning.Setting.TARGET_CAP, 500)
                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED, Double.NaN);
        assertEquals(1.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.EXECUTE_THRESHOLD, 0));
        assertEquals(64, tuning.integer(AbyssalSpectralMasteryTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED, 1));
    }

    @Test
    void stormSoulDefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = List.of(
                StormSoulMasteryAbilities.STORMSCALE_ROD, StormSoulMasteryAbilities.IONBOUND_CRUSHER,
                StormSoulMasteryAbilities.IONBOUND_BEAM, StormSoulMasteryAbilities.IONBOUND_SHIELD,
                StormSoulMasteryAbilities.SOULRENDER_MARK, StormSoulMasteryAbilities.SOULRENDER_REAP,
                StormSoulMasteryAbilities.SOULSTALKER_TENDRIL, StormSoulMasteryAbilities.SOULSTALKER_STRIDE,
                StormSoulMasteryAbilities.WHISPERWIND_DASH, StormSoulMasteryAbilities.WHISPERWIND_RESET,
                StormSoulMasteryAbilities.DREADWHISPER_REAVE, StormSoulMasteryAbilities.DREADWHISPER_WOUND);
        assertEquals(12, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(StormSoulMasteryAbilities.TUNING));
            assertTrue(definition.supportsEvent(StormSoulMasteryAbilities.HIT));
            assertTrue(definition.supportsEvent(StormSoulMasteryAbilities.FINISH));
        }
        StormSoulMasteryTuning tuning = StormSoulMasteryTuning.EMPTY
                .with(StormSoulMasteryTuning.Setting.CHANCE, 500)
                .with(StormSoulMasteryTuning.Setting.TARGET_CAP, 500)
                .with(StormSoulMasteryTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(StormSoulMasteryTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(StormSoulMasteryTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(StormSoulMasteryTuning.Setting.SPEED, 1));
    }

    @Test
    void longPathFinalFormsDefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = List.of(
                LongPathFinalFormsMasteryAbilities.LICHBLADE_AURA, LongPathFinalFormsMasteryAbilities.LICHBLADE_CHANNEL,
                LongPathFinalFormsMasteryAbilities.SUNFIRE_STANDARD, LongPathFinalFormsMasteryAbilities.SUNFIRE_REGEN,
                LongPathFinalFormsMasteryAbilities.HARBINGER_STANDARD, LongPathFinalFormsMasteryAbilities.HARBINGER_OMEN);
        assertEquals(6, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(LongPathFinalFormsMasteryAbilities.TUNING));
            assertTrue(definition.supportsEvent(LongPathFinalFormsMasteryAbilities.HIT));
            assertTrue(definition.supportsEvent(LongPathFinalFormsMasteryAbilities.SUPPORT));
            assertTrue(definition.supportsEvent(LongPathFinalFormsMasteryAbilities.FINISH));
        }
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryTuning.EMPTY
                .with(LongPathFinalFormsMasteryTuning.Setting.CHANCE, 500)
                .with(LongPathFinalFormsMasteryTuning.Setting.TARGET_CAP, 500)
                .with(LongPathFinalFormsMasteryTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(LongPathFinalFormsMasteryTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(LongPathFinalFormsMasteryTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(LongPathFinalFormsMasteryTuning.Setting.SPEED, 1));
    }

    @Test
    void fireForgeDefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = FireForgeMasteryAbilities.definitions();
        assertEquals(11, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(FireForgeMasteryAbilities.TUNING));
            assertTrue(definition.supportsEvent(FireForgeMasteryAbilities.HIT));
            assertTrue(definition.supportsEvent(FireForgeMasteryAbilities.PULSE));
            assertTrue(definition.supportsEvent(FireForgeMasteryAbilities.FINISH));
        }
        FireForgeMasteryTuning tuning = FireForgeMasteryTuning.EMPTY
                .with(FireForgeMasteryTuning.Setting.CHANCE, 500)
                .with(FireForgeMasteryTuning.Setting.TARGET_CAP, 500)
                .with(FireForgeMasteryTuning.Setting.HEAT_FLOOR, 75)
                .with(FireForgeMasteryTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(FireForgeMasteryTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(FireForgeMasteryTuning.Setting.TARGET_CAP, 0));
        assertEquals(75, tuning.integer(FireForgeMasteryTuning.Setting.HEAT_FLOOR, 0));
        assertEquals(0.0, tuning.get(FireForgeMasteryTuning.Setting.SPEED, 1));
    }

    @Test
    void stormFrostWaterDefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = StormFrostWaterMasteryAbilities.definitions();
        assertEquals(14, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(StormFrostWaterMasteryAbilities.TUNING));
            assertTrue(definition.supportsEvent(StormFrostWaterMasteryAbilities.HIT));
            assertTrue(definition.supportsEvent(StormFrostWaterMasteryAbilities.PULSE));
            assertTrue(definition.supportsEvent(StormFrostWaterMasteryAbilities.RETURN_HIT));
            assertTrue(definition.supportsEvent(StormFrostWaterMasteryAbilities.CATCH));
            assertTrue(definition.supportsEvent(StormFrostWaterMasteryAbilities.RECALL));
            assertTrue(definition.supportsEvent(StormFrostWaterMasteryAbilities.FINISH));
        }
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(StormFrostWaterMasteryTuning.Setting.CHANCE, 500)
                .with(StormFrostWaterMasteryTuning.Setting.TARGET_CAP, 500)
                .with(StormFrostWaterMasteryTuning.Setting.FREEZE_CAP_TICKS, 100)
                .with(StormFrostWaterMasteryTuning.Setting.FROSTFALL_DEADFALL_ANGLE_DEGREES, 120)
                .with(StormFrostWaterMasteryTuning.Setting.FROSTFALL_PULSE_TARGET_CAP, 500)
                .with(StormFrostWaterMasteryTuning.Setting.ICEWHISPER_LAST_SNOW_HEALTH_PERCENT, 500)
                .with(StormFrostWaterMasteryTuning.Setting.ICEWHISPER_AURA_TARGET_CAP, 500)
                .with(StormFrostWaterMasteryTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(StormFrostWaterMasteryTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(StormFrostWaterMasteryTuning.Setting.TARGET_CAP, 0));
        assertEquals(100, tuning.integer(StormFrostWaterMasteryTuning.Setting.FREEZE_CAP_TICKS, 0));
        assertEquals(90, tuning.integer(StormFrostWaterMasteryTuning.Setting.FROSTFALL_DEADFALL_ANGLE_DEGREES, 0));
        assertEquals(64, tuning.integer(StormFrostWaterMasteryTuning.Setting.FROSTFALL_PULSE_TARGET_CAP, 0));
        assertEquals(100, tuning.integer(StormFrostWaterMasteryTuning.Setting.ICEWHISPER_LAST_SNOW_HEALTH_PERCENT, 0));
        assertEquals(64, tuning.integer(StormFrostWaterMasteryTuning.Setting.ICEWHISPER_AURA_TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(StormFrostWaterMasteryTuning.Setting.SPEED, 1));
    }

    @Test
    void natureSwarmDefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = NatureSwarmMasteryAbilities.definitions();
        assertEquals(10, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(NatureSwarmMasteryAbilities.TUNING));
            assertTrue(definition.supportsEvent(NatureSwarmMasteryAbilities.HIT));
            assertTrue(definition.supportsEvent(NatureSwarmMasteryAbilities.KILL));
            assertTrue(definition.supportsEvent(NatureSwarmMasteryAbilities.FINISH));
        }
        NatureSwarmMasteryTuning tuning = NatureSwarmMasteryTuning.EMPTY
                .with(NatureSwarmMasteryTuning.Setting.CHANCE, 500)
                .with(NatureSwarmMasteryTuning.Setting.TARGET_CAP, 500)
                .with(NatureSwarmMasteryTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(NatureSwarmMasteryTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(NatureSwarmMasteryTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(NatureSwarmMasteryTuning.Setting.SPEED, 1));
    }

    @Test
    void deathShadowBloodDefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = DeathShadowBloodMasteryAbilities.definitions();
        assertEquals(18, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(DeathShadowBloodMasteryAbilities.TUNING));
            assertTrue(definition.supportsEvent(DeathShadowBloodMasteryAbilities.HIT));
            assertTrue(definition.supportsEvent(DeathShadowBloodMasteryAbilities.KILL));
            assertTrue(definition.supportsEvent(DeathShadowBloodMasteryAbilities.FINISH));
        }
        DeathShadowBloodMasteryTuning tuning = DeathShadowBloodMasteryTuning.EMPTY
                .with(DeathShadowBloodMasteryTuning.Setting.CHANCE, 500)
                .with(DeathShadowBloodMasteryTuning.Setting.TARGET_CAP, 500)
                .with(DeathShadowBloodMasteryTuning.Setting.DURATION_CAP_TICKS, 120)
                .with(DeathShadowBloodMasteryTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(DeathShadowBloodMasteryTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(DeathShadowBloodMasteryTuning.Setting.TARGET_CAP, 0));
        assertEquals(120, tuning.integer(DeathShadowBloodMasteryTuning.Setting.DURATION_CAP_TICKS, 0));
        assertEquals(0.0, tuning.get(DeathShadowBloodMasteryTuning.Setting.SPEED, 1));
    }

    @Test
    void arcaneCosmicDefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = ArcaneCosmicMasteryAbilities.definitions();
        assertEquals(21, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(ArcaneCosmicMasteryAbilities.TUNING));
            assertTrue(definition.supportsEvent(ArcaneCosmicMasteryAbilities.HIT));
            assertTrue(definition.supportsEvent(ArcaneCosmicMasteryAbilities.KILL));
            assertTrue(definition.supportsEvent(ArcaneCosmicMasteryAbilities.FINISH));
        }
        ArcaneCosmicMasteryTuning tuning = ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.CHANCE, 500)
                .with(ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, 500)
                .with(ArcaneCosmicMasteryTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(ArcaneCosmicMasteryTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(ArcaneCosmicMasteryTuning.Setting.SPEED, 1));
    }

    @Test
    void martialCommandEldritchDefinitionsAreAdditiveTypedAndBounded() {
        List<UniqueAbilityDefinition> definitions = MartialCommandEldritchMasteryAbilities.definitions();
        assertEquals(15, definitions.stream().map(UniqueAbilityDefinition::id).distinct().count());
        for (UniqueAbilityDefinition definition : definitions) {
            assertTrue(definition.supports(MartialCommandEldritchMasteryAbilities.TUNING));
            assertTrue(definition.supportsEvent(MartialCommandEldritchMasteryAbilities.HIT));
            assertTrue(definition.supportsEvent(MartialCommandEldritchMasteryAbilities.KILL));
            assertTrue(definition.supportsEvent(MartialCommandEldritchMasteryAbilities.FINISH));
        }
        MartialCommandEldritchMasteryTuning tuning = MartialCommandEldritchMasteryTuning.EMPTY
                .with(MartialCommandEldritchMasteryTuning.Setting.CHANCE, 500)
                .with(MartialCommandEldritchMasteryTuning.Setting.TARGET_CAP, 500)
                .with(MartialCommandEldritchMasteryTuning.Setting.SPEED, Double.NaN);
        assertEquals(100, tuning.integer(MartialCommandEldritchMasteryTuning.Setting.CHANCE, 0));
        assertEquals(64, tuning.integer(MartialCommandEldritchMasteryTuning.Setting.TARGET_CAP, 0));
        assertEquals(0.0, tuning.get(MartialCommandEldritchMasteryTuning.Setting.SPEED, 1));
    }

    @Test
    void masteryAbilityDefinitionsAreGloballyUnique() {
        List<UniqueAbilityDefinition> definitions = new ArrayList<>();
        definitions.addAll(List.of(
                AbyssalSpectralMasteryAbilities.WATCHER_DREAD, AbyssalSpectralMasteryAbilities.WATCHER_OMEN,
                AbyssalSpectralMasteryAbilities.DEVOURER_MASS, AbyssalSpectralMasteryAbilities.DEVOURER_REPRISAL,
                AbyssalSpectralMasteryAbilities.WICKPIERCER_THROW, AbyssalSpectralMasteryAbilities.WICKPIERCER_REVIVE,
                AbyssalSpectralMasteryAbilities.GLOAMPIERCER_AMBUSH, AbyssalSpectralMasteryAbilities.GLOAMPIERCER_BARRAGE,
                AbyssalSpectralMasteryAbilities.WRAITHFANG_THROW, AbyssalSpectralMasteryAbilities.WRAITHMAW_MUSTER,
                StormSoulMasteryAbilities.STORMSCALE_ROD, StormSoulMasteryAbilities.IONBOUND_CRUSHER,
                StormSoulMasteryAbilities.IONBOUND_BEAM, StormSoulMasteryAbilities.IONBOUND_SHIELD,
                StormSoulMasteryAbilities.SOULRENDER_MARK, StormSoulMasteryAbilities.SOULRENDER_REAP,
                StormSoulMasteryAbilities.SOULSTALKER_TENDRIL, StormSoulMasteryAbilities.SOULSTALKER_STRIDE,
                StormSoulMasteryAbilities.WHISPERWIND_DASH, StormSoulMasteryAbilities.WHISPERWIND_RESET,
                StormSoulMasteryAbilities.DREADWHISPER_REAVE, StormSoulMasteryAbilities.DREADWHISPER_WOUND,
                LongPathFinalFormsMasteryAbilities.LICHBLADE_AURA, LongPathFinalFormsMasteryAbilities.LICHBLADE_CHANNEL,
                LongPathFinalFormsMasteryAbilities.SUNFIRE_STANDARD, LongPathFinalFormsMasteryAbilities.SUNFIRE_REGEN,
                LongPathFinalFormsMasteryAbilities.HARBINGER_STANDARD, LongPathFinalFormsMasteryAbilities.HARBINGER_OMEN));
        definitions.addAll(FireForgeMasteryAbilities.definitions());
        definitions.addAll(StormFrostWaterMasteryAbilities.definitions());
        definitions.addAll(NatureSwarmMasteryAbilities.definitions());
        definitions.addAll(DeathShadowBloodMasteryAbilities.definitions());
        definitions.addAll(ArcaneCosmicMasteryAbilities.definitions());
        definitions.addAll(MartialCommandEldritchMasteryAbilities.definitions());

        assertEquals(117, definitions.size());
        assertEquals(definitions.size(), definitions.stream().map(UniqueAbilityDefinition::id)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new)).size());
    }

    @Test
    void masteryCohortKeysUseStableSemanticPaths() {
        List<UniqueAbilityKey<?>> keys = List.of(
                AbyssalSpectralMasteryAbilities.TUNING,
                StormSoulMasteryAbilities.TUNING,
                LongPathFinalFormsMasteryAbilities.TUNING,
                FireForgeMasteryAbilities.TUNING,
                StormFrostWaterMasteryAbilities.TUNING,
                NatureSwarmMasteryAbilities.TUNING,
                DeathShadowBloodMasteryAbilities.TUNING,
                ArcaneCosmicMasteryAbilities.TUNING,
                MartialCommandEldritchMasteryAbilities.TUNING);
        assertEquals(9, keys.stream().map(UniqueAbilityKey::id).distinct().count());
        assertTrue(keys.stream().allMatch(key -> key.id().getPath().startsWith("mastery/")));
        assertTrue(keys.stream().noneMatch(key -> key.id().getPath().matches("phase\\d+/.*")));
    }

    private static Identifier id(String path) {
        return Identifier.of("simplyswords_test", path);
    }
}
