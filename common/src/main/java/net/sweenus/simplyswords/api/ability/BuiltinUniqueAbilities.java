package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

public final class BuiltinUniqueAbilities {
    public static final Identifier STORMBREAK_ID = id("stormbreak");
    public static final Identifier STORMS_EDGE_REFRESH_ID = id("storms_edge_refresh");
    public static final Identifier STORMS_EDGE_MELEE_ID = id("storms_edge_melee");
    public static final Identifier BRIMSTONE_ERUPTION_ID = id("brimstone_eruption");
    public static final Identifier BRIMSTONE_RITE_ID = id("brimstone_rite");
    public static final Identifier CORRIDOR_HIT = id("stormbreak/corridor_hit");
    public static final Identifier THUNDERCLAP_HIT = id("stormbreak/thunderclap_hit");
    public static final Identifier DASH_END = id("stormbreak/dash_end");
    public static final Identifier THUNDERCLAP_FINISH = id("stormbreak/thunderclap_finish");
    public static final Identifier AFTERIMAGE_HIT = id("stormbreak/afterimage_hit");
    public static final Identifier AFTERSHOCK_HIT = id("stormbreak/aftershock_hit");
    public static final Identifier JUDGMENT_HIT = id("stormbreak/judgment_hit");
    public static final Identifier SUPERCELL_HIT = id("stormbreak/supercell_hit");
    public static final Identifier REFRESH_PROC = id("storms_edge_refresh/proc");
    public static final Identifier MELEE_HIT = id("storms_edge_melee/hit");
    public static final Identifier BRIMSTONE_ERUPTION_HIT = id("brimstone_eruption/hit");
    public static final Identifier BRIMSTONE_CINDER_HIT = id("brimstone_eruption/cinder_hit");
    public static final Identifier BRIMSTONE_CHAIN_HIT = id("brimstone_eruption/chain_hit");
    public static final Identifier BRIMSTONE_PULSE_HIT = id("brimstone_rite/pulse_hit");
    public static final Identifier BRIMSTONE_PULSE_FINISH = id("brimstone_rite/pulse_finish");
    public static final Identifier BRIMSTONE_TARGET_JUMP = id("brimstone_rite/target_jump");
    public static final Identifier BRIMSTONE_SNAPBACK_HIT = id("brimstone_rite/snapback_hit");
    public static final Identifier BRIMSTONE_WAKE_HIT = id("brimstone_rite/wake_hit");
    public static final Identifier BRIMSTONE_PLUNGE_START = id("brimstone_rite/plunge_start");
    public static final Identifier BRIMSTONE_PLUNGE_HIT = id("brimstone_rite/plunge_hit");
    public static final Identifier BRIMSTONE_EMERGENCY_PLUNGE = id("brimstone_rite/emergency_plunge");
    public static final Identifier MODE_NORMAL = id("stormbreak/mode/normal");
    public static final Identifier MODE_FOCUSED = id("stormbreak/mode/focused");
    public static final Identifier MODE_THUNDERHEAD = id("stormbreak/mode/thunderhead");
    public static final Identifier CORRIDOR_FORCE_OUTWARD = id("stormbreak/corridor_force/outward");
    public static final Identifier CORRIDOR_FORCE_INWARD = id("stormbreak/corridor_force/inward");
    public static final Identifier BRIMSTONE_FORCE_OUTWARD = id("brimstone_eruption/force/outward");
    public static final Identifier BRIMSTONE_FORCE_BACKDRAFT = id("brimstone_eruption/force/backdraft");
    public static final Identifier BRIMSTONE_ERUPTION_NORMAL = id("brimstone_eruption/mode/normal");
    public static final Identifier BRIMSTONE_ERUPTION_CRUCIBLE = id("brimstone_eruption/mode/crucible");
    public static final Identifier BRIMSTONE_RITE_NORMAL = id("brimstone_rite/duration/normal");
    public static final Identifier BRIMSTONE_RITE_EXECUTIONER = id("brimstone_rite/duration/executioner");
    public static final Identifier BRIMSTONE_RITE_PERPETUAL = id("brimstone_rite/duration/perpetual");
    public static final Identifier BRIMSTONE_GUARD_NORMAL = id("brimstone_rite/guard/normal");
    public static final Identifier BRIMSTONE_GUARD_WALKING = id("brimstone_rite/guard/walking");
    public static final Identifier BRIMSTONE_GUARD_LAST_REPRISAL = id("brimstone_rite/guard/last_reprisal");

    public static final UniqueAbilityKey<Integer> STORMBREAK_COOLDOWN_TICKS =
            UniqueAbilityKey.integer(id("stormbreak/cooldown_ticks"), 100, 0, 72000);
    public static final UniqueAbilityKey<Double> DASH_DISTANCE =
            UniqueAbilityKey.decimal(id("stormbreak/dash_distance"), 10.0, 0.1, 128.0);
    public static final UniqueAbilityKey<Double> DASH_SPEED =
            UniqueAbilityKey.decimal(id("stormbreak/dash_speed"), 2.5, 0.1, 16.0);
    public static final UniqueAbilityKey<Double> CORRIDOR_WIDTH =
            UniqueAbilityKey.decimal(id("stormbreak/corridor_width"), 3.0, 0.1, 64.0);
    public static final UniqueAbilityKey<Double> CORRIDOR_DAMAGE_SCALING =
            UniqueAbilityKey.decimal(id("stormbreak/corridor_damage_scaling"), 0.4, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> CORRIDOR_SPELL_SCALING =
            UniqueAbilityKey.decimal(id("stormbreak/corridor_spell_scaling"), 1.89, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> CORRIDOR_KNOCKBACK =
            UniqueAbilityKey.decimal(id("stormbreak/corridor_knockback"), 0.8, 0.0, 16.0);
    public static final UniqueAbilityKey<Double> CORRIDOR_KNOCK_UP =
            UniqueAbilityKey.decimal(id("stormbreak/corridor_knock_up"), 0.15, 0.0, 16.0);
    public static final UniqueAbilityKey<Double> THUNDERCLAP_RADIUS =
            UniqueAbilityKey.decimal(id("stormbreak/thunderclap_radius"), 3.5, 0.1, 64.0);
    public static final UniqueAbilityKey<Double> THUNDERCLAP_DAMAGE_SCALING =
            UniqueAbilityKey.decimal(id("stormbreak/thunderclap_damage_scaling"), 0.8, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> THUNDERCLAP_SPELL_SCALING =
            UniqueAbilityKey.decimal(id("stormbreak/thunderclap_spell_scaling"), 3.77, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> THUNDERCLAP_KNOCKBACK =
            UniqueAbilityKey.decimal(id("stormbreak/thunderclap_knockback"), 1.2, 0.0, 16.0);
    public static final UniqueAbilityKey<Double> THUNDERCLAP_KNOCK_UP =
            UniqueAbilityKey.decimal(id("stormbreak/thunderclap_knock_up"), 0.3, 0.0, 16.0);
    public static final UniqueAbilityKey<Double> THUNDERCLAP_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("stormbreak/thunderclap_damage_multiplier"), 1.0, 0.0, 100.0);
    public static final UniqueAbilityKey<Identifier> STORMBREAK_MODE =
            UniqueAbilityKey.value(id("stormbreak/mode"), Identifier.class, MODE_NORMAL);
    public static final UniqueAbilityKey<Identifier> CORRIDOR_FORCE_MODE =
            UniqueAbilityKey.value(id("stormbreak/corridor_force"), Identifier.class, CORRIDOR_FORCE_OUTWARD);
    public static final UniqueAbilityKey<Double> THUNDERCLAP_BONUS_PER_CORRIDOR_HIT =
            UniqueAbilityKey.decimal(id("stormbreak/thunderclap_bonus_per_corridor_hit"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> THUNDERCLAP_CORRIDOR_BONUS_CAP =
            UniqueAbilityKey.decimal(id("stormbreak/thunderclap_corridor_bonus_cap"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Integer> AFTERIMAGE_DURATION_TICKS =
            UniqueAbilityKey.integer(id("stormbreak/afterimage_duration_ticks"), 0, 0, 1200);
    public static final UniqueAbilityKey<Double> AFTERIMAGE_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("stormbreak/afterimage_damage_multiplier"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Integer> AFTERSHOCK_DELAY_TICKS =
            UniqueAbilityKey.integer(id("stormbreak/aftershock_delay_ticks"), 0, 0, 1200);
    public static final UniqueAbilityKey<Double> AFTERSHOCK_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("stormbreak/aftershock_damage_multiplier"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> JUDGMENT_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("stormbreak/judgment_damage_multiplier"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Integer> SUPERCELL_DURATION_TICKS =
            UniqueAbilityKey.integer(id("stormbreak/supercell_duration_ticks"), 0, 0, 1200);
    public static final UniqueAbilityKey<Integer> SUPERCELL_INTERVAL_TICKS =
            UniqueAbilityKey.integer(id("stormbreak/supercell_interval_ticks"), 20, 1, 1200);
    public static final UniqueAbilityKey<Double> SUPERCELL_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("stormbreak/supercell_damage_multiplier"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> SUPERCELL_PULL =
            UniqueAbilityKey.decimal(id("stormbreak/supercell_pull"), 0.0, 0.0, 16.0);
    public static final UniqueAbilityKey<Integer> REFRESH_CHANCE =
            UniqueAbilityKey.integer(id("storms_edge_refresh/chance"), 15, 0, 100);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_PROC_CHANCE =
            UniqueAbilityKey.integer(id("brimstone_eruption/proc_chance"), 15, 0, 100);
    public static final UniqueAbilityKey<Double> BRIMSTONE_ERUPTION_RADIUS =
            UniqueAbilityKey.decimal(id("brimstone_eruption/radius"), 3.0, 0.1, 64.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_ERUPTION_DAMAGE_SCALING =
            UniqueAbilityKey.decimal(id("brimstone_eruption/damage_scaling"), 0.8, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_ERUPTION_SPELL_SCALING =
            UniqueAbilityKey.decimal(id("brimstone_eruption/spell_scaling"), 4.19, 0.0, 100.0);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_ERUPTION_FIRE_TICKS =
            UniqueAbilityKey.integer(id("brimstone_eruption/fire_ticks"), 60, 0, 72000);
    public static final UniqueAbilityKey<Double> BRIMSTONE_ERUPTION_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_eruption/damage_multiplier"), 1.0, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_BURNING_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_eruption/burning_damage_multiplier"), 1.0, 0.0, 100.0);
    public static final UniqueAbilityKey<Identifier> BRIMSTONE_FORCE_MODE =
            UniqueAbilityKey.value(id("brimstone_eruption/force"), Identifier.class, BRIMSTONE_FORCE_OUTWARD);
    public static final UniqueAbilityKey<Double> BRIMSTONE_BACKDRAFT_PULL =
            UniqueAbilityKey.decimal(id("brimstone_eruption/backdraft_pull"), 0.0, 0.0, 16.0);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_SLOWNESS_TICKS =
            UniqueAbilityKey.integer(id("brimstone_eruption/slowness_ticks"), 0, 0, 72000);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_CINDER_COUNT =
            UniqueAbilityKey.integer(id("brimstone_eruption/cinder_count"), 0, 0, 16);
    public static final UniqueAbilityKey<Double> BRIMSTONE_CINDER_RANGE =
            UniqueAbilityKey.decimal(id("brimstone_eruption/cinder_range"), 6.0, 0.1, 64.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_CINDER_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_eruption/cinder_damage_multiplier"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_CINDER_FIRE_TICKS =
            UniqueAbilityKey.integer(id("brimstone_eruption/cinder_fire_ticks"), 40, 0, 72000);
    public static final UniqueAbilityKey<Double> BRIMSTONE_CHAIN_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_eruption/chain_damage_multiplier"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_CHAIN_RADIUS =
            UniqueAbilityKey.decimal(id("brimstone_eruption/chain_radius"), 3.0, 0.1, 64.0);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_CHAIN_MAX_DETONATIONS =
            UniqueAbilityKey.integer(id("brimstone_eruption/chain_max_detonations"), 0, 0, 64);
    public static final UniqueAbilityKey<Identifier> BRIMSTONE_ERUPTION_MODE =
            UniqueAbilityKey.value(id("brimstone_eruption/mode"), Identifier.class, BRIMSTONE_ERUPTION_NORMAL);
    public static final UniqueAbilityKey<Double> BRIMSTONE_ERUPTION_RADIUS_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_eruption/radius_multiplier"), 1.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_PRIMARY_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_eruption/primary_damage_multiplier"), 1.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_RITE_COOLDOWN_TICKS =
            UniqueAbilityKey.integer(id("brimstone_rite/cooldown_ticks"), 600, 0, 72000);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_RITE_DURATION_TICKS =
            UniqueAbilityKey.integer(id("brimstone_rite/duration_ticks"), 120, 1, 72000);
    public static final UniqueAbilityKey<Double> BRIMSTONE_RITE_BASE_RADIUS =
            UniqueAbilityKey.decimal(id("brimstone_rite/base_radius"), 2.5, 0.1, 64.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_RITE_MAX_RADIUS =
            UniqueAbilityKey.decimal(id("brimstone_rite/max_radius"), 6.0, 0.1, 64.0);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_RITE_PULSE_INTERVAL_TICKS =
            UniqueAbilityKey.integer(id("brimstone_rite/pulse_interval_ticks"), 20, 1, 72000);
    public static final UniqueAbilityKey<Double> BRIMSTONE_RITE_DAMAGE_SCALING =
            UniqueAbilityKey.decimal(id("brimstone_rite/damage_scaling"), 1.0, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_RITE_SPELL_SCALING =
            UniqueAbilityKey.decimal(id("brimstone_rite/spell_scaling"), 5.3395, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_RITE_PULSE_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_rite/pulse_damage_multiplier"), 0.28, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_RITE_RADIUS_GROWTH_PER_HIT =
            UniqueAbilityKey.decimal(id("brimstone_rite/radius_growth_per_hit"), 0.35, 0.0, 64.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_RITE_FINAL_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_rite/final_damage_multiplier"), 1.0, 0.0, 100.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_RITE_TARGET_JUMP_RANGE =
            UniqueAbilityKey.decimal(id("brimstone_rite/target_jump_range"), 8.0, 0.0, 128.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_RITE_PULSE_PULL =
            UniqueAbilityKey.decimal(id("brimstone_rite/pulse_pull"), 0.0, 0.0, 16.0);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_RITE_SLOWNESS_TICKS =
            UniqueAbilityKey.integer(id("brimstone_rite/slowness_ticks"), 0, 0, 72000);
    public static final UniqueAbilityKey<Double> BRIMSTONE_OVERPRESSURE_PER_PULSE =
            UniqueAbilityKey.decimal(id("brimstone_rite/overpressure_per_pulse"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_OVERPRESSURE_CAP =
            UniqueAbilityKey.decimal(id("brimstone_rite/overpressure_cap"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_SNAPBACK_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_rite/snapback_damage_multiplier"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_SNAPBACK_RADIUS =
            UniqueAbilityKey.decimal(id("brimstone_rite/snapback_radius"), 3.0, 0.1, 64.0);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_WAKE_DURATION_TICKS =
            UniqueAbilityKey.integer(id("brimstone_rite/wake_duration_ticks"), 0, 0, 72000);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_WAKE_INTERVAL_TICKS =
            UniqueAbilityKey.integer(id("brimstone_rite/wake_interval_ticks"), 20, 1, 72000);
    public static final UniqueAbilityKey<Double> BRIMSTONE_WAKE_DAMAGE_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_rite/wake_damage_multiplier"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_WAKE_MIN_MOVE =
            UniqueAbilityKey.decimal(id("brimstone_rite/wake_min_move"), 1.5, 0.1, 64.0);
    public static final UniqueAbilityKey<Identifier> BRIMSTONE_RITE_DURATION_MODE =
            UniqueAbilityKey.value(id("brimstone_rite/duration_mode"), Identifier.class, BRIMSTONE_RITE_NORMAL);
    public static final UniqueAbilityKey<Double> BRIMSTONE_PERPETUAL_START_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_rite/perpetual_start_multiplier"), 1.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_PERPETUAL_PER_PULSE =
            UniqueAbilityKey.decimal(id("brimstone_rite/perpetual_per_pulse"), 0.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_PERPETUAL_CAP =
            UniqueAbilityKey.decimal(id("brimstone_rite/perpetual_cap"), 1.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Identifier> BRIMSTONE_GUARD_MODE =
            UniqueAbilityKey.value(id("brimstone_rite/guard_mode"), Identifier.class, BRIMSTONE_GUARD_NORMAL);
    public static final UniqueAbilityKey<Double> BRIMSTONE_WALKING_RADIUS_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_rite/walking_radius_multiplier"), 1.0, 0.0, 10.0);
    public static final UniqueAbilityKey<Double> BRIMSTONE_WALKING_PULL =
            UniqueAbilityKey.decimal(id("brimstone_rite/walking_pull"), 0.0, 0.0, 16.0);
    public static final UniqueAbilityKey<Integer> BRIMSTONE_EMERGENCY_HEALTH_PERCENT =
            UniqueAbilityKey.integer(id("brimstone_rite/emergency_health_percent"), 0, 0, 100);
    public static final UniqueAbilityKey<Double> BRIMSTONE_EMERGENCY_FINAL_MULTIPLIER =
            UniqueAbilityKey.decimal(id("brimstone_rite/emergency_final_multiplier"), 1.0, 0.0, 10.0);

    public static final UniqueAbilityDefinition STORMBREAK = UniqueAbilityDefinition.builder(
                    STORMBREAK_ID, UniqueAbilityKind.ACTIVE)
            .cooldownKey(STORMBREAK_COOLDOWN_TICKS)
            .key(DASH_DISTANCE)
            .key(DASH_SPEED)
            .key(CORRIDOR_WIDTH)
            .key(CORRIDOR_DAMAGE_SCALING)
            .key(CORRIDOR_SPELL_SCALING)
            .key(CORRIDOR_KNOCKBACK)
            .key(CORRIDOR_KNOCK_UP)
            .key(THUNDERCLAP_RADIUS)
            .key(THUNDERCLAP_DAMAGE_SCALING)
            .key(THUNDERCLAP_SPELL_SCALING)
            .key(THUNDERCLAP_KNOCKBACK)
            .key(THUNDERCLAP_KNOCK_UP)
            .key(THUNDERCLAP_DAMAGE_MULTIPLIER)
            .key(STORMBREAK_MODE)
            .key(CORRIDOR_FORCE_MODE)
            .key(THUNDERCLAP_BONUS_PER_CORRIDOR_HIT)
            .key(THUNDERCLAP_CORRIDOR_BONUS_CAP)
            .key(AFTERIMAGE_DURATION_TICKS)
            .key(AFTERIMAGE_DAMAGE_MULTIPLIER)
            .key(AFTERSHOCK_DELAY_TICKS)
            .key(AFTERSHOCK_DAMAGE_MULTIPLIER)
            .key(JUDGMENT_DAMAGE_MULTIPLIER)
            .key(SUPERCELL_DURATION_TICKS)
            .key(SUPERCELL_INTERVAL_TICKS)
            .key(SUPERCELL_DAMAGE_MULTIPLIER)
            .key(SUPERCELL_PULL)
            .event(CORRIDOR_HIT)
            .event(THUNDERCLAP_HIT)
            .event(DASH_END)
            .event(THUNDERCLAP_FINISH)
            .event(AFTERIMAGE_HIT)
            .event(AFTERSHOCK_HIT)
            .event(JUDGMENT_HIT)
            .event(SUPERCELL_HIT)
            .build();

    public static final UniqueAbilityDefinition STORMS_EDGE_REFRESH = UniqueAbilityDefinition.builder(
            STORMS_EDGE_REFRESH_ID, UniqueAbilityKind.PASSIVE)
            .key(REFRESH_CHANCE)
            .key(CORRIDOR_DAMAGE_SCALING)
            .key(CORRIDOR_SPELL_SCALING)
            .key(THUNDERCLAP_DAMAGE_SCALING)
            .key(THUNDERCLAP_SPELL_SCALING)
            .event(REFRESH_PROC)
            .build();

    public static final UniqueAbilityDefinition STORMS_EDGE_MELEE = UniqueAbilityDefinition.builder(
                    STORMS_EDGE_MELEE_ID, UniqueAbilityKind.PASSIVE)
            .key(CORRIDOR_DAMAGE_SCALING)
            .key(CORRIDOR_SPELL_SCALING)
            .key(THUNDERCLAP_DAMAGE_SCALING)
            .key(THUNDERCLAP_SPELL_SCALING)
            .event(MELEE_HIT)
            .build();

    public static final UniqueAbilityDefinition BRIMSTONE_ERUPTION = UniqueAbilityDefinition.builder(
                    BRIMSTONE_ERUPTION_ID, UniqueAbilityKind.PASSIVE)
            .key(BRIMSTONE_PROC_CHANCE)
            .key(BRIMSTONE_ERUPTION_RADIUS)
            .key(BRIMSTONE_ERUPTION_DAMAGE_SCALING)
            .key(BRIMSTONE_ERUPTION_SPELL_SCALING)
            .key(BRIMSTONE_ERUPTION_FIRE_TICKS)
            .key(BRIMSTONE_ERUPTION_DAMAGE_MULTIPLIER)
            .key(BRIMSTONE_BURNING_DAMAGE_MULTIPLIER)
            .key(BRIMSTONE_FORCE_MODE)
            .key(BRIMSTONE_BACKDRAFT_PULL)
            .key(BRIMSTONE_SLOWNESS_TICKS)
            .key(BRIMSTONE_CINDER_COUNT)
            .key(BRIMSTONE_CINDER_RANGE)
            .key(BRIMSTONE_CINDER_DAMAGE_MULTIPLIER)
            .key(BRIMSTONE_CINDER_FIRE_TICKS)
            .key(BRIMSTONE_CHAIN_DAMAGE_MULTIPLIER)
            .key(BRIMSTONE_CHAIN_RADIUS)
            .key(BRIMSTONE_CHAIN_MAX_DETONATIONS)
            .key(BRIMSTONE_ERUPTION_MODE)
            .key(BRIMSTONE_ERUPTION_RADIUS_MULTIPLIER)
            .key(BRIMSTONE_PRIMARY_DAMAGE_MULTIPLIER)
            .event(BRIMSTONE_ERUPTION_HIT)
            .event(BRIMSTONE_CINDER_HIT)
            .event(BRIMSTONE_CHAIN_HIT)
            .build();

    public static final UniqueAbilityDefinition BRIMSTONE_RITE = UniqueAbilityDefinition.builder(
                    BRIMSTONE_RITE_ID, UniqueAbilityKind.ACTIVE)
            .cooldownKey(BRIMSTONE_RITE_COOLDOWN_TICKS)
            .key(BRIMSTONE_RITE_DURATION_TICKS)
            .key(BRIMSTONE_RITE_BASE_RADIUS)
            .key(BRIMSTONE_RITE_MAX_RADIUS)
            .key(BRIMSTONE_RITE_PULSE_INTERVAL_TICKS)
            .key(BRIMSTONE_RITE_DAMAGE_SCALING)
            .key(BRIMSTONE_RITE_SPELL_SCALING)
            .key(BRIMSTONE_RITE_PULSE_DAMAGE_MULTIPLIER)
            .key(BRIMSTONE_RITE_RADIUS_GROWTH_PER_HIT)
            .key(BRIMSTONE_RITE_FINAL_DAMAGE_MULTIPLIER)
            .key(BRIMSTONE_RITE_TARGET_JUMP_RANGE)
            .key(BRIMSTONE_RITE_PULSE_PULL)
            .key(BRIMSTONE_RITE_SLOWNESS_TICKS)
            .key(BRIMSTONE_OVERPRESSURE_PER_PULSE)
            .key(BRIMSTONE_OVERPRESSURE_CAP)
            .key(BRIMSTONE_SNAPBACK_DAMAGE_MULTIPLIER)
            .key(BRIMSTONE_SNAPBACK_RADIUS)
            .key(BRIMSTONE_WAKE_DURATION_TICKS)
            .key(BRIMSTONE_WAKE_INTERVAL_TICKS)
            .key(BRIMSTONE_WAKE_DAMAGE_MULTIPLIER)
            .key(BRIMSTONE_WAKE_MIN_MOVE)
            .key(BRIMSTONE_RITE_DURATION_MODE)
            .key(BRIMSTONE_PERPETUAL_START_MULTIPLIER)
            .key(BRIMSTONE_PERPETUAL_PER_PULSE)
            .key(BRIMSTONE_PERPETUAL_CAP)
            .key(BRIMSTONE_GUARD_MODE)
            .key(BRIMSTONE_WALKING_RADIUS_MULTIPLIER)
            .key(BRIMSTONE_WALKING_PULL)
            .key(BRIMSTONE_EMERGENCY_HEALTH_PERCENT)
            .key(BRIMSTONE_EMERGENCY_FINAL_MULTIPLIER)
            .event(BRIMSTONE_PULSE_HIT)
            .event(BRIMSTONE_PULSE_FINISH)
            .event(BRIMSTONE_TARGET_JUMP)
            .event(BRIMSTONE_SNAPBACK_HIT)
            .event(BRIMSTONE_WAKE_HIT)
            .event(BRIMSTONE_PLUNGE_START)
            .event(BRIMSTONE_PLUNGE_HIT)
            .event(BRIMSTONE_EMERGENCY_PLUNGE)
            .build();

    private BuiltinUniqueAbilities() {
    }

    public static void register() {
        UniqueAbilityApi.registerDefinition(STORMBREAK);
        UniqueAbilityApi.registerDefinition(STORMS_EDGE_REFRESH);
        UniqueAbilityApi.registerDefinition(STORMS_EDGE_MELEE);
        UniqueAbilityApi.registerDefinition(BRIMSTONE_ERUPTION);
        UniqueAbilityApi.registerDefinition(BRIMSTONE_RITE);
        Phase2UniqueAbilities.register();
        Phase3UniqueAbilities.register();
        Phase4UniqueAbilities.register();
        Phase5UniqueAbilities.register();
        Phase6UniqueAbilities.register();
        Phase7UniqueAbilities.register();
        Phase8UniqueAbilities.register();
        Phase9UniqueAbilities.register();
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }
}
