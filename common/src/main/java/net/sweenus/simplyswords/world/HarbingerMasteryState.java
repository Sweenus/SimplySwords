package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.ability.LongPathFinalFormsMasteryTuning;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HarbingerMasteryState {
    private static final Identifier GUARD = Identifier.of("simplyswords", "harbinger_formation_guard");
    private static final Map<LivingEntity, Map<UUID, Support>> SUPPORT = new HashMap<>();
    private static final Map<LivingEntity, LivingEntity> MARKS = new HashMap<>();
    private static final Map<LivingEntity, Weakness> WEAKNESS = new HashMap<>();
    private static boolean updatingWeakness;

    private HarbingerMasteryState() {
    }

    public static void support(BattleStandardDarkEntity banner, LivingEntity ally, LongPathFinalFormsMasteryTuning tuning) {
        long now = ally.getWorld().getTime();
        float damage = tuning.flag(1024) ? .15F : tuning.flag(512) ? .1F : 0;
        double guard = tuning.flag(512) ? tuning.get(s("KNOCKBACK_RESISTANCE"), .15) : 0;
        if (damage == 0 && guard == 0 && !tuning.flag(128)) return;
        SUPPORT.computeIfAbsent(ally, ignored -> new HashMap<>()).put(banner.getUuid(), new Support(
                banner, now + (tuning.flag(1024) ? 60 : 80), damage, now + 80, guard,
                tuning.flag(128) ? now + tuning.integer(s("ALLY_CHARGE_TICKS"), 80) : 0,
                tuning.integer(s("ALLY_WEAKNESS_TICKS"), 60), tuning.flag(512) ? now + 80 : 0));
        updateGuard(ally);
    }

    public static float damageBonus(LivingEntity attacker) {
        long now = attacker.getWorld().getTime();
        double support = SUPPORT.getOrDefault(attacker, Map.of()).values().stream()
                .filter(value -> valid(value.banner, attacker))
                .mapToDouble(value -> Math.max(value.damageUntil > now ? value.damage : 0,
                        value.formationUntil > now ? .1 : 0)).max().orElse(0);
        return (float) support + BattleStandardMasteryManager.solitaryBonus(attacker);
    }

    public static void onDamageApplied(LivingEntity target, DamageSource source) {
        if (!(source.getAttacker() instanceof LivingEntity attacker)
                || !LongPathFinalFormsMasteryCombatManager.melee(source)
                || !HelperMethods.checkAbilityTarget(target, attacker)) return;
        long now = attacker.getWorld().getTime();
        int duration = 0;
        for (Support value : SUPPORT.getOrDefault(attacker, Map.of()).values()) {
            if (valid(value.banner, attacker) && value.chargeUntil > now) {
                duration = Math.max(duration, value.weaknessTicks);
                value.chargeUntil = 0;
            }
        }
        if (duration > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0), attacker);
    }

    public static boolean isMarked(LivingEntity owner, LivingEntity target) {
        return MARKS.get(owner) == target && owner.isAlive() && !owner.isRemoved()
                && target.isAlive() && !target.isRemoved() && owner.getWorld() == target.getWorld()
                && target.hasStatusEffect(StatusEffects.WEAKNESS) && HelperMethods.checkAbilityTarget(target, owner);
    }

    public static void applyWeakness(LivingEntity owner, LivingEntity target, int duration, int amplifier,
                                     boolean execution, boolean melee) {
        if (!target.isAlive() || !HelperMethods.checkAbilityTarget(target, owner)
                || !target.canHaveStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, amplifier))) return;
        if (execution) {
            if (!melee && MARKS.get(owner) != target) return;
            if (melee) {
                LivingEntity previous = MARKS.put(owner, target);
                if (previous != null && previous != target) removeOmen(owner, previous);
            }
        }
        Weakness state = WEAKNESS.computeIfAbsent(target, ignored -> {
            Weakness created = new Weakness((ServerWorld) target.getWorld());
            StatusEffectInstance current = target.getStatusEffect(StatusEffects.WEAKNESS);
            if (current != null) created.external = new Effect(current, target.getWorld().getTime());
            return created;
        });
        long now = target.getWorld().getTime();
        Effect previous = state.omens.get(owner);
        StatusEffectInstance merged = previous == null ? null : previous.remaining(now);
        StatusEffectInstance incoming = new StatusEffectInstance(StatusEffects.WEAKNESS, duration, amplifier);
        if (merged == null) merged = incoming;
        else merged.upgrade(incoming);
        state.omens.put(owner, new Effect(merged, now));
        refresh(target, state);
    }

    public static void externalWeakness(LivingEntity target, StatusEffectInstance effect) {
        if (updatingWeakness || target.getWorld().isClient() || !effect.getEffectType().equals(StatusEffects.WEAKNESS)
                || !target.canHaveStatusEffect(effect)) return;
        Weakness state = WEAKNESS.get(target);
        if (state == null) return;
        long now = target.getWorld().getTime();
        StatusEffectInstance current = state.external == null ? null : state.external.remaining(now);
        if (current == null) current = remainingEffect(effect, 0);
        else current.upgrade(effect);
        state.external = new Effect(current, now);
    }

    public static void weaknessRemoved(LivingEntity target, StatusEffectInstance effect) {
        if (updatingWeakness || !effect.getEffectType().equals(StatusEffects.WEAKNESS)) return;
        WEAKNESS.remove(target);
        MARKS.entrySet().removeIf(entry -> entry.getValue() == target);
    }

    private static void removeOmen(LivingEntity owner, LivingEntity target) {
        Weakness state = WEAKNESS.get(target);
        if (state == null) return;
        state.omens.remove(owner);
        refresh(target, state);
    }

    private static void refresh(LivingEntity target, Weakness state) {
        long now = state.world.getTime();
        StatusEffectInstance combined = state.external == null ? null : state.external.remaining(now);
        for (Effect effect : state.omens.values()) {
            StatusEffectInstance remaining = effect.remaining(now);
            if (remaining == null) continue;
            if (combined == null) combined = remaining;
            else combined.upgrade(remaining);
        }
        updatingWeakness = true;
        try {
            target.removeStatusEffect(StatusEffects.WEAKNESS);
            if (combined != null) target.addStatusEffect(combined);
        } finally {
            updatingWeakness = false;
        }
        if (state.omens.isEmpty()) WEAKNESS.remove(target);
    }

    public static void tick(ServerWorld world) {
        for (LivingEntity ally : java.util.List.copyOf(SUPPORT.keySet())) {
            Map<UUID, Support> values = SUPPORT.get(ally);
            values.values().removeIf(value -> !valid(value.banner, ally)
                    || value.damageUntil <= ally.getWorld().getTime() && value.guardUntil <= ally.getWorld().getTime()
                    && value.chargeUntil <= ally.getWorld().getTime() && value.formationUntil <= ally.getWorld().getTime());
            updateGuard(ally);
            if (values.isEmpty()) SUPPORT.remove(ally);
        }
        for (LivingEntity target : java.util.List.copyOf(WEAKNESS.keySet())) {
            Weakness state = WEAKNESS.get(target);
            if (!target.isAlive() || target.isRemoved()) {
                WEAKNESS.remove(target);
                continue;
            }
            if (state.world != target.getWorld()) {
                state.omens.clear();
                refresh(target, state);
                continue;
            }
            boolean changed = state.omens.entrySet().removeIf(entry -> !entry.getKey().isAlive()
                    || entry.getKey().isRemoved() || entry.getKey().getWorld() != target.getWorld()
                    || entry.getValue().remaining(target.getWorld().getTime()) == null);
            if (changed) refresh(target, state);
        }
        MARKS.entrySet().removeIf(entry -> !isMarked(entry.getKey(), entry.getValue()));
    }

    public static void clearOwner(LivingEntity owner) {
        MARKS.remove(owner);
        for (LivingEntity target : java.util.List.copyOf(WEAKNESS.keySet())) removeOmen(owner, target);
        SUPPORT.remove(owner);
        updateGuard(owner);
        for (Map<UUID, Support> values : SUPPORT.values()) values.values().removeIf(value -> value.banner.ownerEntity == owner);
        SUPPORT.keySet().forEach(HarbingerMasteryState::updateGuard);
    }

    public static void removeBanner(UUID banner) {
        SUPPORT.forEach((ally, values) -> {
            values.remove(banner);
            updateGuard(ally);
        });
    }

    public static void clear(ServerWorld world) {
        for (LivingEntity target : java.util.List.copyOf(WEAKNESS.keySet())) {
            if (target.getWorld() == world) {
                Weakness state = WEAKNESS.get(target);
                state.omens.clear();
                refresh(target, state);
            }
        }
        SUPPORT.keySet().removeIf(ally -> {
            if (ally.getWorld() != world) return false;
            var attribute = ally.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
            if (attribute != null) attribute.removeModifier(GUARD);
            return true;
        });
        MARKS.entrySet().removeIf(entry -> entry.getKey().getWorld() == world || entry.getValue().getWorld() == world);
    }

    public static void clearAll() {
        SUPPORT.keySet().stream().map(ally -> (ServerWorld) ally.getWorld()).distinct().toList().forEach(HarbingerMasteryState::clear);
        WEAKNESS.keySet().stream().map(target -> (ServerWorld) target.getWorld()).distinct().toList().forEach(HarbingerMasteryState::clear);
        MARKS.clear();
    }

    private static boolean valid(BattleStandardDarkEntity banner, LivingEntity ally) {
        return ally.isAlive() && !ally.isRemoved() && banner.isAlive() && !banner.isRemoved()
                && banner.getWorld() == ally.getWorld() && banner.ownerEntity != null
                && banner.ownerEntity.isAlive() && !banner.ownerEntity.isRemoved()
                && banner.ownerEntity.getWorld() == banner.getWorld();
    }

    private static void updateGuard(LivingEntity ally) {
        var attribute = ally.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        double value = SUPPORT.getOrDefault(ally, Map.of()).values().stream()
                .filter(support -> valid(support.banner, ally) && support.guardUntil > ally.getWorld().getTime())
                .mapToDouble(support -> support.guard).max().orElse(0);
        attribute.removeModifier(GUARD);
        if (value > 0) attribute.addTemporaryModifier(new EntityAttributeModifier(GUARD, value,
                EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private static LongPathFinalFormsMasteryTuning.Setting s(String name) {
        return LongPathFinalFormsMasteryTuning.Setting.valueOf(name);
    }

    private static final class Weakness {
        private final ServerWorld world;
        private Weakness(ServerWorld world) { this.world = world; }
        private Effect external;
        private final Map<LivingEntity, Effect> omens = new HashMap<>();
    }

    private static StatusEffectInstance remainingEffect(StatusEffectInstance effect, long elapsed) {
        if (effect == null) return null;
        StatusEffectInstance hidden = remainingEffect(((net.sweenus.simplyswords.mixin.StatusEffectInstanceAccessor)
                effect).simplyswords$getHiddenEffect(), elapsed);
        if (!effect.isInfinite() && elapsed >= effect.getDuration()) return hidden;
        return new StatusEffectInstance(effect.getEffectType(), effect.isInfinite() ? -1 : (int) (effect.getDuration() - elapsed),
                effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon(), hidden);
    }

    private record Effect(StatusEffectInstance effect, long appliedAt) {
        private Effect {
            effect = remainingEffect(effect, 0);
        }
        private StatusEffectInstance remaining(long now) {
            return remainingEffect(effect, Math.max(0, now - appliedAt));
        }
    }

    private static final class Support {
        private final BattleStandardDarkEntity banner;
        private final long damageUntil;
        private final float damage;
        private final long guardUntil;
        private final double guard;
        private long chargeUntil;
        private final int weaknessTicks;
        private final long formationUntil;

        private Support(BattleStandardDarkEntity banner, long damageUntil, float damage, long guardUntil,
                        double guard, long chargeUntil, int weaknessTicks, long formationUntil) {
            this.formationUntil = formationUntil;
            this.banner = banner;
            this.damageUntil = damageUntil;
            this.damage = damage;
            this.guardUntil = guardUntil;
            this.guard = guard;
            this.chargeUntil = chargeUntil;
            this.weaknessTicks = weaknessTicks;
        }
    }
}
