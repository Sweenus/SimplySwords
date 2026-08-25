package net.sweenus.simplyswords.api.ability;

import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

public final class BuiltinUniqueAbilities {
    public static final Identifier STORMBREAK_ID = id("stormbreak");
    public static final Identifier STORMS_EDGE_REFRESH_ID = id("storms_edge_refresh");
    public static final Identifier STORMS_EDGE_MELEE_ID = id("storms_edge_melee");
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
    public static final Identifier MODE_NORMAL = id("stormbreak/mode/normal");
    public static final Identifier MODE_FOCUSED = id("stormbreak/mode/focused");
    public static final Identifier MODE_THUNDERHEAD = id("stormbreak/mode/thunderhead");
    public static final Identifier CORRIDOR_FORCE_OUTWARD = id("stormbreak/corridor_force/outward");
    public static final Identifier CORRIDOR_FORCE_INWARD = id("stormbreak/corridor_force/inward");

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

    private BuiltinUniqueAbilities() {
    }

    public static void register() {
        UniqueAbilityApi.registerDefinition(STORMBREAK);
        UniqueAbilityApi.registerDefinition(STORMS_EDGE_REFRESH);
        UniqueAbilityApi.registerDefinition(STORMS_EDGE_MELEE);
    }

    private static Identifier id(String path) {
        return Identifier.of(SimplySwords.MOD_ID, path);
    }
}
