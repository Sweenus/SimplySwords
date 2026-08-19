package net.sweenus.simplyswords.gametest;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.util.AbilityScalingProbe;

import java.util.ArrayList;
import java.util.List;

enum ActivationMode {
    PASSIVE_ON_HIT,
    ACTIVE_ABILITY,
    HOLD_RELEASE,
    THROW,
    SUMMON,
    AURA,
    PROJECTILE,
    CHANNEL,
    CUSTOM
}

enum BalanceCategory {
    FREQUENT_PASSIVE,
    SHORT_COOLDOWN_ACTIVE,
    LONG_COOLDOWN_BURST,
    MULTI_HIT_ACTIVE,
    SUMMON,
    PERSISTENT_AURA,
    AOE_CONTROL,
    HYBRID
}

enum BalanceScenarioType {
    ISOLATED_ABILITY("Isolated ability"),
    SINGLE_TARGET_ROTATION("60-second single target"),
    FIVE_TARGET_CLUSTER("60-second five-target cluster"),
    RELIABILITY("Single-cast reliability");

    private final String displayName;

    BalanceScenarioType(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }

    boolean rotation() {
        return this == SINGLE_TARGET_ROTATION || this == FIVE_TARGET_CLUSTER;
    }
}

enum BalanceBuildProfile {
    VANILLA_BASE(false, GearFamily.NONE, GearTier.NONE, false),
    VANILLA_SHARPNESS_V(true, GearFamily.NONE, GearTier.NONE, false),
    IRONS_FULL(false, GearFamily.IRONS, GearTier.FULL, false),
    IRONS_MAX(false, GearFamily.IRONS, GearTier.MAX, false),
    IRONS_MAX_OFFHAND(false, GearFamily.IRONS, GearTier.MAX, true),
    SPELL_POWER_FULL(false, GearFamily.SPELL_POWER, GearTier.FULL, false),
    SPELL_POWER_MAX(false, GearFamily.SPELL_POWER, GearTier.MAX, false),
    HYBRID_SHARPNESS_CASTER(true, GearFamily.ANY_CASTER, GearTier.MAX, true);

    private final boolean sharpness;
    private final GearFamily gearFamily;
    private final GearTier gearTier;
    private final boolean offhand;

    BalanceBuildProfile(boolean sharpness, GearFamily gearFamily, GearTier gearTier, boolean offhand) {
        this.sharpness = sharpness;
        this.gearFamily = gearFamily;
        this.gearTier = gearTier;
        this.offhand = offhand;
    }

    boolean sharpness() {
        return sharpness;
    }

    GearFamily gearFamily() {
        return gearFamily;
    }

    GearTier gearTier() {
        return gearTier;
    }

    boolean offhand() {
        return offhand;
    }
}

enum GearFamily {
    NONE,
    IRONS,
    SPELL_POWER,
    ANY_CASTER
}

enum GearTier {
    NONE,
    FULL,
    MAX
}

enum ScenarioSetup {
    NONE,
    PRIME_WITH_MELEE
}

record AbilityBalanceSpec(
        String weaponId,
        String ability,
        SpellScalingProfile spellProfile,
        ActivationMode activationMode,
        int durationTicks,
        int targetDistance,
        BalanceCategory category,
        ScenarioSetup setup,
        boolean reliabilitySensitive,
        boolean requiresMeleeTrigger,
        String notes
) {
}

record DamageEvent(long tick, String source, float amount, boolean melee) {
}

final class DamageRecorder {
    private final List<DamageEvent> events = new ArrayList<>();
    private long meleeTick = Long.MIN_VALUE;
    private boolean meleeTickClaimed;

    void markMeleeTick(long tick) {
        meleeTick = tick;
        meleeTickClaimed = false;
    }

    void record(long tick, DamageSource source, float amount) {
        // The first direct event in a swing tick is the auto-attack; later ones are on-hit procs.
        boolean melee = !meleeTickClaimed && tick == meleeTick
                && source.isDirect() && source.getSource() == source.getAttacker();
        if (melee) {
            meleeTickClaimed = true;
        }
        events.add(new DamageEvent(tick, source.getName(), amount, melee));
    }

    void clear() {
        events.clear();
        meleeTick = Long.MIN_VALUE;
        meleeTickClaimed = false;
    }

    List<DamageEvent> events() {
        return List.copyOf(events);
    }
}

final class BranchAccumulator {
    private double spellSum;
    private double meleeSum;
    private int samples;
    private int spellWins;
    private int valueSamples;

    void record(AbilityScalingProbe.BranchKind kind, float spellBranch, float meleeBranch, boolean spellWon) {
        if (kind != AbilityScalingProbe.BranchKind.DAMAGE) {
            valueSamples++;
            return;
        }
        spellSum += spellBranch;
        meleeSum += meleeBranch;
        samples++;
        if (spellWon) {
            spellWins++;
        }
    }

    BranchStats snapshot() {
        if (samples == 0) {
            return new BranchStats(Float.NaN, Float.NaN, Float.NaN, 0, valueSamples);
        }
        return new BranchStats((float) (spellSum / samples), (float) (meleeSum / samples),
                spellWins / (float) samples, samples, valueSamples);
    }
}

record BranchStats(float spellBranchAvg, float meleeBranchAvg, float spellWinRate, int samples, int valueSamples) {
    static final BranchStats EMPTY = new BranchStats(Float.NaN, Float.NaN, Float.NaN, 0, 0);
}

record EquipmentResult(boolean available, String notes, float rawSpellPower) {
    static EquipmentResult available(String notes, float rawSpellPower) {
        return new EquipmentResult(true, notes, rawSpellPower);
    }

    static EquipmentResult unavailable(String notes) {
        return new EquipmentResult(false, notes, 0.0F);
    }
}

record BalanceRunResult(
        AbilityBalanceSpec spec,
        BalanceBuildProfile build,
        BalanceScenarioType scenario,
        int targets,
        int durationTicks,
        int casts,
        int castsHit,
        int meleeSwings,
        List<DamageEvent> events,
        float rawSpellMeleeRatio,
        float adjustedSpellMeleeRatio,
        BranchStats branchStats,
        String notes,
        boolean available
) {
    float meleeDamage() {
        return (float) events.stream().filter(DamageEvent::melee).mapToDouble(DamageEvent::amount).sum();
    }

    float abilityDamage() {
        return (float) events.stream().filter(event -> !event.melee()).mapToDouble(DamageEvent::amount).sum();
    }

    float totalDamage() {
        return (float) events.stream().mapToDouble(DamageEvent::amount).sum();
    }

    float maximumHit() {
        return (float) events.stream().mapToDouble(DamageEvent::amount).max().orElse(0.0);
    }

    float totalDps() {
        return durationTicks <= 0 ? 0.0F : totalDamage() / (durationTicks / 20.0F);
    }

    float abilityDps() {
        return durationTicks <= 0 ? 0.0F : abilityDamage() / (durationTicks / 20.0F);
    }
}

record BalanceJob(AbilityBalanceSpec spec, BalanceBuildProfile build, BalanceScenarioType scenario, int repetition) {
}
