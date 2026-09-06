package net.sweenus.simplyswords.world;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TempestAbilityManager {
    public enum Element { FIRE, FROST }

    public record MarkApplication(Element primary, int fireApplied, int frostApplied) {
    }

    private static final Identifier FROST_SLOW = Identifier.of(SimplySwords.MOD_ID, "tempest_frost_slow");
    private static final Map<ServerWorld, WorldState> STATES = new HashMap<>();
    private static boolean initialized;

    private TempestAbilityManager() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        TickEvent.SERVER_LEVEL_POST.register(world -> {
            if (world instanceof ServerWorld serverWorld) tick(serverWorld);
        });
    }

    public static MarkApplication applyMark(ServerWorld world, LivingEntity owner, LivingEntity target,
                                            StormFrostWaterMasteryTuning tuning, float fireDamage, float frostDamage,
                                            int configuredDuration, int configuredCap) {
        WorldState state = STATES.computeIfAbsent(world, ignored -> new WorldState());
        long now = world.getTime();
        pruneMarks(world, state, now);
        Cadence cadence = state.cadences.computeIfAbsent(owner.getUuid(), ignored -> new Cadence());
        Element primary = choosePrimary(owner, cadence, tuning, now);
        cadence.attacks++;
        cadence.alternating = cadence.lastPrimary != null && cadence.lastPrimary != primary
                ? cadence.alternating + 1 : 1;
        cadence.lastPrimary = primary;

        int cap = resolveMarkCap(tuning, configuredCap);
        int duration = resolveMarkDuration(tuning, configuredDuration);
        double potency = tuning.get(s("TEMPEST_SPECIALIST_POTENCY_MULTIPLIER"), 1);
        MarkKey key = new MarkKey(owner.getUuid(), target.getUuid());
        Marks marks = state.marks.computeIfAbsent(key, ignored -> new Marks());
        marks.tuning = tuning;
        marks.fireDamage = fireDamage;
        marks.frostDamage = frostDamage;
        int fireApplied = add(marks, primary, cap, duration, potency, now);
        int frostApplied = primary == Element.FROST ? fireApplied : 0;
        if (primary == Element.FIRE) frostApplied = 0;
        int appliedFire = primary == Element.FIRE ? fireApplied : 0;

        boolean specialist = tuning.has(s("TEMPEST_SPECIALIST_LOCK_TICKS"));
        Element opposite = opposite(primary);
        if (!specialist) {
            int shifting = tuning.integer(s("TEMPEST_SHIFTING_ATTACK_COUNT"), 0);
            if (shifting > 0 && cadence.attacks % shifting == 0) {
                int applied = add(marks, opposite, cap, duration, 1, now);
                if (opposite == Element.FIRE) appliedFire += applied;
                else frostApplied += applied;
            }
            int balanced = tuning.integer(s("TEMPEST_BALANCED_ATTACK_COUNT"), 0);
            if (balanced > 0 && cadence.alternating > 0 && cadence.alternating % balanced == 0) {
                int applied = add(marks, opposite, cap, duration, 1, now);
                if (opposite == Element.FIRE) appliedFire += applied;
                else frostApplied += applied;
            }
        }

        applyPerfectSequence(owner, marks, tuning, now);
        syncMarkEffects(world, target.getUuid(), state, now);
        return new MarkApplication(primary, appliedFire, frostApplied);
    }

    public static boolean hasConsumableMarks(ServerWorld world, LivingEntity owner, double range) {
        WorldState state = STATES.get(world);
        if (state == null) return false;
        pruneMarks(world, state, world.getTime());
        double rangeSquared = range * range;
        for (Map.Entry<MarkKey, Marks> entry : state.marks.entrySet()) {
            if (!entry.getKey().owner.equals(owner.getUuid()) || !entry.getValue().hasBoth()) continue;
            Entity target = world.getEntity(entry.getKey().target);
            if (target instanceof LivingEntity living && living.isAlive()
                    && owner.squaredDistanceTo(living) <= rangeSquared
                    && HelperMethods.checkAbilityTarget(living, owner)) return true;
        }
        return false;
    }

    public static boolean startVortex(WeaponAbilityContext context, StormFrostWaterMasteryTuning tuning,
                                      UniqueAbilityExecution execution, int configuredDuration,
                                      int configuredMaximumSize) {
        ServerWorld world = context.world();
        LivingEntity owner = context.actor();
        WorldState worldState = STATES.computeIfAbsent(world, ignored -> new WorldState());
        long now = world.getTime();
        pruneMarks(world, worldState, now);

        List<Map.Entry<MarkKey, Marks>> available = worldState.marks.entrySet().stream()
                .filter(entry -> entry.getKey().owner.equals(owner.getUuid()) && entry.getValue().hasBoth())
                .filter(entry -> world.getEntity(entry.getKey().target) instanceof LivingEntity target
                        && target.isAlive() && owner.squaredDistanceTo(target) <= 225
                        && HelperMethods.checkAbilityTarget(target, owner))
                .sorted(Comparator.comparingDouble(entry -> {
                    Entity target = world.getEntity(entry.getKey().target);
                    return target == null ? Double.MAX_VALUE : owner.squaredDistanceTo(target);
                }))
                .limit(64)
                .toList();
        if (available.isEmpty()) return false;

        int retain = tuning.integer(s("TEMPEST_RECALL_RETAIN_COUNT"), 0);
        int fire = 0;
        int frost = 0;
        double weighted = 0;
        for (Map.Entry<MarkKey, Marks> entry : available) {
            Marks marks = entry.getValue();
            int consumedFire = consumedCount(marks.fire, retain);
            int consumedFrost = consumedCount(marks.frost, retain);
            fire += consumedFire;
            frost += consumedFrost;
            weighted += consumedFire * marks.firePotency + consumedFrost * marks.frostPotency;
            marks.fire -= consumedFire;
            marks.frost -= consumedFrost;
            marks.fireWeight = marks.fire <= 0 ? 0 : marks.fire * marks.firePotency;
            marks.frostWeight = marks.frost <= 0 ? 0 : marks.frost * marks.frostPotency;
            syncMarkEffects(world, entry.getKey().target, worldState, now);
        }
        worldState.marks.entrySet().removeIf(entry -> entry.getValue().empty());
        if (fire <= 0 || frost <= 0) return false;

        Vortex previous = worldState.vortices.remove(owner.getUuid());
        if (previous != null) finish(world, owner, previous, false);
        int duration = resolveVortexDuration(tuning, configuredDuration);
        int maximumSize = resolveMaximumSize(tuning, configuredMaximumSize);
        Vortex vortex = new Vortex(execution, tuning, owner.getPos(), now, now + duration,
                fire, frost, Math.max(1, weighted), maximumSize,
                copy(owner.getStatusEffect(StatusEffects.SPEED)),
                copy(owner.getStatusEffect(StatusEffects.RESISTANCE)),
                copy(owner.getStatusEffect(EffectRegistry.getReference(EffectRegistry.ELEMENTAL_VORTEX))));
        worldState.vortices.put(owner.getUuid(), vortex);

        int shellThreshold = integer(tuning, "TEMPEST_SHELL_STACK_THRESHOLD", "COUNT", 0);
        if (shellThreshold > 0 && fire + frost >= shellThreshold) {
            float absorption = (float) value(tuning, "TEMPEST_SHELL_ABSORPTION", "ABSORPTION", 0);
            MasteryAbsorptionTracker.grant(owner, absorption,
                    integer(tuning, "TEMPEST_SHELL_DURATION_TICKS", "STATUS_DURATION_TICKS", 80), absorption);
        }
        if (tuning.has(s("TEMPEST_SPEED_AMPLIFIER"))) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration,
                    tuning.integer(s("TEMPEST_SPEED_AMPLIFIER"), 0), false, false, true), owner);
        }
        if (tuning.has(s("TEMPEST_RESISTANCE_AMPLIFIER"))) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration,
                    tuning.integer(s("TEMPEST_RESISTANCE_AMPLIFIER"), 0), false, false, true), owner);
        }
        owner.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.ELEMENTAL_VORTEX),
                duration, Math.max(0, Math.min(255, fire + frost - 1)), false, false, true), owner);
        thermalShock(world, owner, vortex);
        createWaves(owner, vortex);
        return true;
    }

    public static boolean hasManagedMark(LivingEntity target, Element element) {
        if (!(target.getWorld() instanceof ServerWorld world)) return false;
        WorldState state = STATES.get(world);
        if (state == null) return false;
        return state.marks.entrySet().stream().anyMatch(entry -> entry.getKey().target.equals(target.getUuid())
                && (element == Element.FIRE ? entry.getValue().fire : entry.getValue().frost) > 0);
    }

    public static boolean hasManagedVortex(LivingEntity owner) {
        return owner.getWorld() instanceof ServerWorld world && STATES.containsKey(world)
                && STATES.get(world).vortices.containsKey(owner.getUuid());
    }

    public static Vec3d managedVortexCenter(LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) return null;
        WorldState state = STATES.get(world);
        Vortex vortex = state == null ? null : state.vortices.get(owner.getUuid());
        return vortex == null ? null : vortex.center(owner);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        WorldState state = STATES.get(world);
        if (state == null) return;
        Vortex vortex = state.vortices.remove(actor.getUuid());
        if (vortex != null) finish(world, actor, vortex, false);
        state.cadences.remove(actor.getUuid());
        Set<UUID> affected = new HashSet<>();
        state.marks.entrySet().removeIf(entry -> {
            boolean remove = entry.getKey().owner.equals(actor.getUuid()) || entry.getKey().target.equals(actor.getUuid());
            if (remove) affected.add(entry.getKey().target);
            return remove;
        });
        affected.forEach(target -> syncMarkEffects(world, target, state, world.getTime()));
        removeFrostSlow(actor);
        if (state.empty()) STATES.remove(world);
    }

    public static void clear(ServerWorld world) {
        WorldState state = STATES.remove(world);
        if (state == null) return;
        state.vortices.forEach((id, vortex) -> {
            if (world.getEntity(id) instanceof LivingEntity owner) finish(world, owner, vortex, false);
            else UniqueAbilityApi.cancel(vortex.execution);
        });
        state.marks.keySet().stream().map(MarkKey::target).distinct().forEach(id -> {
            if (world.getEntity(id) instanceof LivingEntity target) removeFrostSlow(target);
        });
    }

    public static void clearAll() {
        new ArrayList<>(STATES.keySet()).forEach(TempestAbilityManager::clear);
        STATES.clear();
    }

    public static int resolveMarkDuration(StormFrostWaterMasteryTuning tuning, int configured) {
        int base = tuning.integer(s("DURATION_TICKS"), configured);
        return Math.max(1, base + tuning.integer(s("TEMPEST_MARK_DURATION_BONUS_TICKS"), 0));
    }

    public static int resolveMarkCap(StormFrostWaterMasteryTuning tuning, int configured) {
        if (tuning.has(s("TEMPEST_PRISMATIC_STACK_CAP")))
            return tuning.integer(s("TEMPEST_PRISMATIC_STACK_CAP"), configured);
        if (tuning.has(s("TEMPEST_MARK_STACK_CAP")))
            return tuning.integer(s("TEMPEST_MARK_STACK_CAP"), configured);
        return tuning.integer(s("STACK_CAP"), configured);
    }

    public static int resolveVortexDuration(StormFrostWaterMasteryTuning tuning, int configured) {
        double duration = tuning.get(s("DURATION_TICKS"), configured)
                + tuning.get(s("TEMPEST_DURATION_BONUS_TICKS"), 0);
        return Math.max(1, (int) Math.round(duration
                * tuning.get(s("TEMPEST_SINGULARITY_DURATION_MULTIPLIER"), 1)));
    }

    public static int resolveMaximumSize(StormFrostWaterMasteryTuning tuning, int configured) {
        return Math.max(1, (int) Math.round(tuning.get(s("STACK_CAP"), configured)
                * tuning.get(s("TEMPEST_MAX_SIZE_MULTIPLIER"), 1)));
    }

    public static int consumedCount(int available, int retain) {
        if (available <= 0) return 0;
        return retain > 0 ? Math.max(1, available - retain) : available;
    }

    public static double resolveRadius(StormFrostWaterMasteryTuning tuning, int effectiveStacks, int maximumSize) {
        if (tuning.has(s("TEMPEST_SINGULARITY_RADIUS")))
            return tuning.get(s("TEMPEST_SINGULARITY_RADIUS"), 4);
        int stacks = Math.min(Math.max(0, effectiveStacks), Math.max(1, maximumSize));
        double growing = 1 + stacks / 6.0 * tuning.get(s("TEMPEST_RADIUS_PER_STACK_MULTIPLIER"), 1);
        return tuning.get(s("RADIUS"), growing) + tuning.get(s("TEMPEST_START_RADIUS_BONUS"), 0);
    }

    public static int resolvePulseInterval(StormFrostWaterMasteryTuning tuning, int effectiveStacks, int maximumSize) {
        if (effectiveStacks >= maximumSize && tuning.has(s("TEMPEST_MAX_CADENCE_INTERVAL_TICKS")))
            return tuning.integer(s("TEMPEST_MAX_CADENCE_INTERVAL_TICKS"), 8);
        return tuning.integer(s("INTERVAL_TICKS"), 10);
    }

    public static double resolveDamageMultiplier(StormFrostWaterMasteryTuning tuning, int consumedStacks,
                                                 boolean consumedBoth) {
        double multiplier = tuning.get(s("DAMAGE_MULTIPLIER"), 1)
                * tuning.get(s("TEMPEST_VORTEX_DAMAGE_MULTIPLIER"), 1)
                * tuning.get(s("TEMPEST_SINGULARITY_DAMAGE_MULTIPLIER"), 1)
                * (1 + consumedStacks * tuning.get(s("TEMPEST_CONSUMED_DAMAGE_PER_STACK"), 0));
        if (consumedBoth) multiplier *= tuning.get(s("TEMPEST_PRISMATIC_DAMAGE_MULTIPLIER"), 1);
        return multiplier;
    }

    private static void tick(ServerWorld world) {
        WorldState state = STATES.get(world);
        if (state == null) return;
        long now = world.getTime();
        pruneMarks(world, state, now);
        tickMarks(world, state, now);
        tickVortices(world, state, now);
        if (state.empty()) STATES.remove(world);
    }

    private static void tickMarks(ServerWorld world, WorldState state, long now) {
        Set<UUID> targets = new HashSet<>();
        for (Map.Entry<MarkKey, Marks> entry : state.marks.entrySet()) {
            if (!(world.getEntity(entry.getKey().owner) instanceof LivingEntity owner)
                    || !(world.getEntity(entry.getKey().target) instanceof LivingEntity target)) continue;
            Marks marks = entry.getValue();
            targets.add(target.getUuid());
            if (marks.fire > 0 && now >= marks.nextFirePulse) {
                damageMark(target, owner, (float) (marks.fireDamage
                        * marks.tuning.get(s("DAMAGE_MULTIPLIER"), 1)
                        * marks.tuning.get(s("TEMPEST_FIRE_DAMAGE_MULTIPLIER"), 1)), marks.fire, marks.firePotency);
                marks.nextFirePulse = now + Math.max(1, 16 - marks.fire);
            }
            if (marks.frost > 0 && now >= marks.nextFrostPulse) {
                damageMark(target, owner, (float) (marks.frostDamage
                        * marks.tuning.get(s("DAMAGE_MULTIPLIER"), 1)), marks.frost, marks.frostPotency);
                marks.nextFrostPulse = now + Math.max(1, 16 - marks.frost);
            }
        }
        targets.forEach(id -> {
            if (world.getEntity(id) instanceof LivingEntity target) {
                applyFrostSlow(target, state);
                syncMarkEffects(world, id, state, now);
            }
        });
    }

    private static void tickVortices(ServerWorld world, WorldState state, long now) {
        Iterator<Map.Entry<UUID, Vortex>> iterator = state.vortices.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Vortex> entry = iterator.next();
            if (!(world.getEntity(entry.getKey()) instanceof LivingEntity owner) || !owner.isAlive()) {
                UniqueAbilityApi.cancel(entry.getValue().execution);
                iterator.remove();
                continue;
            }
            Vortex vortex = entry.getValue();
            MasteryAbsorptionTracker.tick(owner);
            if (now >= vortex.expiresAt) {
                finalConvergence(world, owner, vortex);
                finish(world, owner, vortex, true);
                iterator.remove();
                continue;
            }
            double fixedRange = vortex.tuning.get(s("TEMPEST_FIXED_RANGE"), 0);
            if (fixedRange > 0 && owner.squaredDistanceTo(vortex.origin) > fixedRange * fixedRange) {
                finish(world, owner, vortex, false);
                iterator.remove();
                continue;
            }
            tickWaves(world, owner, vortex);
            int effective = effectiveStacks(vortex, now);
            int interval = resolvePulseInterval(vortex.tuning, effective, vortex.maximumSize);
            if (now < vortex.nextPulse) continue;
            Vec3d center = fixedRange > 0 ? vortex.origin : owner.getPos();
            pulse(world, owner, vortex, center, resolveRadius(vortex.tuning, effective, vortex.maximumSize),
                    vortex.pulseDamage(), integer(vortex.tuning,
                            "TEMPEST_PULSE_TARGET_CAP", "SEARCH_CAP", 24), true);
            vortex.pulses++;
            int steamEvery = vortex.tuning.integer(s("TEMPEST_STEAM_PULSE_COUNT"), 0);
            if (steamEvery > 0 && vortex.consumedBoth() && vortex.pulses % steamEvery == 0) {
                secondaryBurst(world, owner, center,
                        vortex.tuning.get(s("TEMPEST_STEAM_RADIUS"), 3),
                        vortex.pulseDamage() * vortex.tuning.get(s("TEMPEST_STEAM_DAMAGE_MULTIPLIER"), .3),
                        vortex.tuning.integer(s("TEMPEST_STEAM_TARGET_CAP"), 8));
            }
            vortex.nextPulse = now + interval;
        }
    }

    private static void pulse(ServerWorld world, LivingEntity owner, Vortex vortex, Vec3d center,
                              double radius, double damage, int cap, boolean countRime) {
        List<LivingEntity> targets = targets(world, owner, center, radius, cap);
        int affected = 0;
        for (LivingEntity target : targets) {
            boolean damaged = damage(target, owner, damage);
            if (damaged) affected++;
            double pull = value(vortex.tuning, "TEMPEST_VORTEX_PULL_STRENGTH", "PULL_STRENGTH", 0)
                    * vortex.tuning.get(s("TEMPEST_SINGULARITY_PULL_MULTIPLIER"), 1);
            if (pull > 0) {
                Vec3d direction = center.subtract(target.getPos()).multiply(1, 0, 1);
                if (direction.lengthSquared() > .0001) {
                    target.addVelocity(direction.normalize().multiply(.08 * pull));
                    target.velocityModified = true;
                }
            }
            if (damaged && countRime && qualifiesRime(vortex)) {
                int hits = vortex.hits.merge(target.getUuid(), 1, Integer::sum);
                int required = vortex.tuning.integer(s("TEMPEST_RIME_HIT_COUNT"), 3);
                if (hits == required) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                        vortex.tuning.integer(s("TEMPEST_RIME_ROOT_TICKS"), 20), 255,
                        false, false, true), owner);
            }
        }
        UniqueAbilityApi.emit(vortex.execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                StormFrostWaterMasteryAbilities.PULSE, null, affected, damage);
    }

    private static void thermalShock(ServerWorld world, LivingEntity owner, Vortex vortex) {
        if (!vortex.consumedBoth() || !vortex.tuning.has(s("TEMPEST_THERMAL_DAMAGE_MULTIPLIER"))) return;
        secondaryBurst(world, owner, owner.getPos(), vortex.tuning.get(s("TEMPEST_THERMAL_RADIUS"), 3),
                vortex.pulseDamage() * vortex.tuning.get(s("TEMPEST_THERMAL_DAMAGE_MULTIPLIER"), .4),
                vortex.tuning.integer(s("TEMPEST_THERMAL_TARGET_CAP"), 10));
    }

    private static void finalConvergence(ServerWorld world, LivingEntity owner, Vortex vortex) {
        double perStack = vortex.tuning.get(s("TEMPEST_FINAL_DAMAGE_PER_STACK"), 0);
        if (perStack <= 0) return;
        int capped = Math.min(vortex.fire + vortex.frost,
                vortex.tuning.integer(s("TEMPEST_FINAL_STACK_CAP"), 20));
        secondaryBurst(world, owner, vortex.center(owner), vortex.tuning.get(s("TEMPEST_FINAL_RADIUS"), 4),
                vortex.pulseDamage() * perStack * capped,
                vortex.tuning.integer(s("TEMPEST_FINAL_TARGET_CAP"), 16));
    }

    private static void secondaryBurst(ServerWorld world, LivingEntity owner, Vec3d center,
                                       double radius, double damage, int cap) {
        for (LivingEntity target : targets(world, owner, center, radius, cap)) damage(target, owner, damage);
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + .5, center.z,
                16, radius / 2, .4, radius / 2, .04);
    }

    private static void createWaves(LivingEntity owner, Vortex vortex) {
        int count = vortex.tuning.integer(s("TEMPEST_WAVE_COUNT"), 0);
        if (count <= 0) return;
        Vec3d facing = owner.getRotationVec(1).multiply(1, 0, 1);
        if (facing.lengthSquared() < .0001) facing = new Vec3d(0, 0, 1);
        facing = facing.normalize();
        for (int index = 0; index < count; index++) {
            float angle = count == 1 ? 0 : (float) ((index - (count - 1) / 2.0) * .36);
            Wave wave = new Wave(owner.getPos(), facing.rotateY(angle), index % 2 == 0);
            vortex.waves.add(wave);
            if (owner.getWorld() instanceof ServerWorld world) spawnWaveLaunch(world, wave);
        }
    }

    private static void tickWaves(ServerWorld world, LivingEntity owner, Vortex vortex) {
        Iterator<Wave> iterator = vortex.waves.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            wave.center = wave.center.add(wave.direction.multiply(.8));
            wave.travelled += .8;
            double width = vortex.tuning.get(s("TEMPEST_WAVE_WIDTH"), 3) / 2;
            int cap = vortex.tuning.integer(s("TEMPEST_WAVE_TARGET_CAP"), 16);
            for (LivingEntity target : targets(world, owner, wave.center, width, cap)) {
                if (!wave.hit.add(target.getUuid())) continue;
                damage(target, owner, vortex.pulseDamage()
                        * vortex.tuning.get(s("TEMPEST_WAVE_DAMAGE_MULTIPLIER"), .75));
                if (wave.fire) target.setOnFireForTicks(40);
                else target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0), owner);
                spawnWaveHit(world, target, wave.fire);
            }
            spawnWaveCrest(world, wave, width);
            if (wave.travelled >= 12 || wave.hit.size() >= cap) {
                spawnWaveDissipation(world, wave, width);
                iterator.remove();
            }
        }
    }

    private static void spawnWaveLaunch(ServerWorld world, Wave wave) {
        Vec3d center = wave.center.add(wave.direction.multiply(.35)).add(0, .55, 0);
        int count = Config.general.enableModernFieldEffects ? 10 : 6;
        world.spawnParticles(wave.fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
                center.x, center.y, center.z, count, .22, .28, .22, .035);
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(wave.fire ? ParticleTypes.SMALL_FLAME : ParticleTypes.ITEM_SNOWBALL,
                    center.x, center.y, center.z, 5, .16, .2, .16, .025);
        }
    }

    private static void spawnWaveCrest(ServerWorld world, Wave wave, double halfWidth) {
        Vec3d lateral = new Vec3d(-wave.direction.z, 0, wave.direction.x);
        int points = Config.general.enableModernFieldEffects ? 9 : 5;
        for (int index = 0; index < points; index++) {
            double offset = points == 1 ? 0 : index * 2.0 / (points - 1) - 1;
            double height = .18 + (1 - Math.abs(offset)) * .65;
            Vec3d crest = wave.center.add(lateral.multiply(offset * halfWidth)).add(0, height, 0);
            world.spawnParticles(wave.fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
                    crest.x, crest.y, crest.z, 1, .025, .035, .025, .006);
            if (Config.general.enableModernFieldEffects && index % 2 == 0) {
                Vec3d trail = crest.subtract(wave.direction.multiply(.4)).add(0, -.08, 0);
                world.spawnParticles(wave.fire ? ParticleTypes.SMALL_FLAME : ParticleTypes.ITEM_SNOWBALL,
                        trail.x, trail.y, trail.z, 1, .035, .035, .035, .008);
            }
        }
        if (Config.general.enableModernFieldEffects) {
            Vec3d wake = wave.center.subtract(wave.direction.multiply(.25)).add(0, .38, 0);
            world.spawnParticles(wave.fire ? ParticleTypes.SMOKE : ParticleTypes.CLOUD,
                    wake.x, wake.y, wake.z, 3, halfWidth * .45, .16, halfWidth * .45, .012);
        }
    }

    private static void spawnWaveHit(ServerWorld world, LivingEntity target, boolean fire) {
        int count = Config.general.enableModernFieldEffects ? 9 : 5;
        world.spawnParticles(fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
                target.getX(), target.getBodyY(.5), target.getZ(), count, .28, .35, .28, .045);
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(fire ? ParticleTypes.SMOKE : ParticleTypes.ITEM_SNOWBALL,
                    target.getX(), target.getBodyY(.45), target.getZ(), 4, .2, .25, .2, .025);
        }
    }

    private static void spawnWaveDissipation(ServerWorld world, Wave wave, double halfWidth) {
        Vec3d center = wave.center.add(0, .45, 0);
        int count = Config.general.enableModernFieldEffects ? 12 : 7;
        world.spawnParticles(wave.fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
                center.x, center.y, center.z, count, halfWidth * .55, .28, halfWidth * .55, .035);
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(wave.fire ? ParticleTypes.SMOKE : ParticleTypes.CLOUD,
                    center.x, center.y, center.z, 6, halfWidth * .4, .2, halfWidth * .4, .02);
        }
    }

    private static List<LivingEntity> targets(ServerWorld world, LivingEntity owner, Vec3d center,
                                              double radius, int cap) {
        Box box = new Box(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        return world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(target -> HelperMethods.checkAbilityTarget(target, owner))
                .sorted(Comparator.comparingDouble(target -> target.squaredDistanceTo(center)))
                .limit(Math.max(1, cap))
                .toList();
    }

    private static void damageMark(LivingEntity target, LivingEntity owner, float base, int stacks, double potency) {
        float amount = HelperMethods.applyNonPlayerAbilityDamageModifier(owner,
                (float) (base * potency + Math.max(0, stacks - 1) / 4.0));
        target.timeUntilRegen = 0;
        target.damage(target.getDamageSources().indirectMagic(target, owner), amount);
    }

    private static boolean damage(LivingEntity target, LivingEntity owner, double amount) {
        target.timeUntilRegen = 0;
        return target.damage(target.getDamageSources().indirectMagic(target, owner),
                HelperMethods.applyNonPlayerAbilityDamageModifier(owner, (float) Math.max(0, amount)));
    }

    private static void pruneMarks(ServerWorld world, WorldState state, long now) {
        Set<UUID> affected = new HashSet<>();
        Iterator<Map.Entry<MarkKey, Marks>> iterator = state.marks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MarkKey, Marks> entry = iterator.next();
            Marks marks = entry.getValue();
            Entity owner = world.getEntity(entry.getKey().owner);
            Entity target = world.getEntity(entry.getKey().target);
            if (marks.fire > 0 && (now >= marks.fireExpires || target instanceof LivingEntity living
                    && !living.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FIRE_VORTEX)))) {
                marks.fire = 0;
                marks.fireWeight = 0;
                marks.firePotency = 1;
            }
            if (marks.frost > 0 && (now >= marks.frostExpires || target instanceof LivingEntity living
                    && !living.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FROST_VORTEX)))) {
                marks.frost = 0;
                marks.frostWeight = 0;
                marks.frostPotency = 1;
            }
            if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()
                    || !(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()
                    || marks.empty()) {
                affected.add(entry.getKey().target);
                iterator.remove();
            }
        }
        affected.forEach(id -> syncMarkEffects(world, id, state, now));
    }

    private static int add(Marks marks, Element element, int cap, int duration, double potency, long now) {
        if (element == Element.FIRE) {
            int before = marks.fire;
            marks.fire = Math.min(cap, marks.fire + 1);
            marks.fireExpires = Math.max(marks.fireExpires, now + duration);
            marks.fireAppliedAt = now;
            marks.fireWeight += marks.fire > before ? potency : 0;
            marks.firePotency = marks.fire == 0 ? 1 : marks.fireWeight / marks.fire;
            return marks.fire > before ? 1 : 0;
        }
        int before = marks.frost;
        marks.frost = Math.min(cap, marks.frost + 1);
        marks.frostExpires = Math.max(marks.frostExpires, now + duration);
        marks.frostAppliedAt = now;
        marks.frostWeight += marks.frost > before ? potency : 0;
        marks.frostPotency = marks.frost == 0 ? 1 : marks.frostWeight / marks.frost;
        return marks.frost > before ? 1 : 0;
    }

    private static Element choosePrimary(LivingEntity owner, Cadence cadence,
                                         StormFrostWaterMasteryTuning tuning, long now) {
        if (tuning.has(s("TEMPEST_SPECIALIST_LOCK_TICKS"))) {
            if (cadence.locked == null || now >= cadence.lockExpires) {
                cadence.locked = owner.getRandom().nextBoolean() ? Element.FIRE : Element.FROST;
                cadence.lockExpires = now + tuning.integer(s("TEMPEST_SPECIALIST_LOCK_TICKS"), 200);
            }
            return cadence.locked;
        }
        if (tuning.has(s("TEMPEST_PRISMATIC_STACK_CAP"))) {
            return cadence.lastPrimary == Element.FIRE ? Element.FROST : Element.FIRE;
        }
        return owner.getRandom().nextBoolean() ? Element.FIRE : Element.FROST;
    }

    private static void applyPerfectSequence(LivingEntity owner, Marks marks,
                                             StormFrostWaterMasteryTuning tuning, long now) {
        int window = tuning.integer(s("TEMPEST_SEQUENCE_WINDOW_TICKS"), 0);
        if (window <= 0 || marks.fire <= 0 || marks.frost <= 0
                || Math.abs(marks.fireAppliedAt - marks.frostAppliedAt) > window) return;
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,
                tuning.integer(s("TEMPEST_SEQUENCE_HASTE_TICKS"), 100),
                tuning.integer(s("TEMPEST_SEQUENCE_HASTE_AMPLIFIER"), 0), false, false, true), owner);
    }

    private static void syncMarkEffects(ServerWorld world, UUID targetId, WorldState state, long now) {
        if (!(world.getEntity(targetId) instanceof LivingEntity target)) return;
        int fire = 0;
        int frost = 0;
        long fireExpiry = 0;
        long frostExpiry = 0;
        for (Map.Entry<MarkKey, Marks> entry : state.marks.entrySet()) {
            if (!entry.getKey().target.equals(targetId)) continue;
            Marks marks = entry.getValue();
            fire = Math.max(fire, marks.fire);
            frost = Math.max(frost, marks.frost);
            fireExpiry = Math.max(fireExpiry, marks.fireExpires);
            frostExpiry = Math.max(frostExpiry, marks.frostExpires);
        }
        syncEffect(target, EffectRegistry.getReference(EffectRegistry.FIRE_VORTEX), fire, fireExpiry, now);
        syncEffect(target, EffectRegistry.getReference(EffectRegistry.FROST_VORTEX), frost, frostExpiry, now);
        if (frost <= 0) removeFrostSlow(target);
    }

    private static void syncEffect(LivingEntity target,
                                   net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect,
                                   int stacks, long expires, long now) {
        if (stacks <= 0 || expires <= now) {
            target.removeStatusEffect(effect);
            return;
        }
        int duration = (int) Math.min(72000, Math.max(1, expires - now));
        StatusEffectInstance current = target.getStatusEffect(effect);
        int amplifier = Math.max(0, Math.min(255, stacks - 1));
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < duration - 1) {
            target.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, false, false, true));
        }
    }

    private static void applyFrostSlow(LivingEntity target, WorldState state) {
        double strongest = 0;
        for (Map.Entry<MarkKey, Marks> entry : state.marks.entrySet()) {
            if (!entry.getKey().target.equals(target.getUuid()) || entry.getValue().frost <= 0) continue;
            Marks marks = entry.getValue();
            double base = Math.min(.4, marks.frost * .05);
            strongest = Math.max(strongest, Math.min(
                    marks.tuning.get(s("TEMPEST_FROST_SLOW_CAP"), .4),
                    base + marks.tuning.get(s("TEMPEST_FROST_SLOW_BONUS"), 0)));
        }
        var speed = target.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(FROST_SLOW);
        if (strongest > 0) speed.addTemporaryModifier(new EntityAttributeModifier(FROST_SLOW,
                -strongest, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeFrostSlow(LivingEntity target) {
        var speed = target.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(FROST_SLOW);
    }

    private static int effectiveStacks(Vortex vortex, long now) {
        int shrinkInterval = Math.max(1, (int) Math.round(40
                * vortex.tuning.get(s("TEMPEST_SHRINK_INTERVAL_MULTIPLIER"), 1)));
        int lost = (int) Math.max(0, (now - vortex.startedAt) / shrinkInterval);
        return Math.max(0, Math.min(vortex.maximumSize, vortex.fire + vortex.frost) - lost);
    }

    private static boolean qualifiesRime(Vortex vortex) {
        int threshold = vortex.tuning.integer(s("TEMPEST_RIME_STACK_THRESHOLD"), 0);
        return threshold > 0 && vortex.fire >= threshold && vortex.frost >= threshold;
    }

    private static void finish(ServerWorld world, LivingEntity owner, Vortex vortex, boolean completed) {
        int elapsed = (int) Math.max(0, world.getTime() - vortex.startedAt);
        restore(owner, EffectRegistry.getReference(EffectRegistry.ELEMENTAL_VORTEX), vortex.previousVortex,
                Math.max(0, Math.min(255, vortex.fire + vortex.frost - 1)),
                (int) Math.max(1, vortex.expiresAt - vortex.startedAt), elapsed);
        if (vortex.tuning.has(s("TEMPEST_SPEED_AMPLIFIER"))) {
            restore(owner, StatusEffects.SPEED, vortex.previousSpeed,
                    vortex.tuning.integer(s("TEMPEST_SPEED_AMPLIFIER"), 0),
                    (int) Math.max(1, vortex.expiresAt - vortex.startedAt), elapsed);
        }
        if (vortex.tuning.has(s("TEMPEST_RESISTANCE_AMPLIFIER"))) {
            restore(owner, StatusEffects.RESISTANCE, vortex.previousResistance,
                    vortex.tuning.integer(s("TEMPEST_RESISTANCE_AMPLIFIER"), 0),
                    (int) Math.max(1, vortex.expiresAt - vortex.startedAt), elapsed);
        }
        if (completed) UniqueAbilityApi.finish(vortex.execution, StormFrostWaterMasteryAbilities.FINISH, 0);
        else UniqueAbilityApi.cancel(vortex.execution);
    }

    private static StatusEffectInstance copy(StatusEffectInstance effect) {
        return effect == null ? null : new StatusEffectInstance(effect);
    }

    private static void restore(LivingEntity owner,
                                net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> type,
                                StatusEffectInstance previous, int amplifier, int maximumDuration, int elapsed) {
        StatusEffectInstance current = owner.getStatusEffect(type);
        if (current != null && (current.getAmplifier() != amplifier || current.getDuration() > maximumDuration + 2)) return;
        if (current != null) owner.removeStatusEffect(type);
        if (previous != null && previous.getDuration() > elapsed) {
            owner.addStatusEffect(new StatusEffectInstance(previous.getEffectType(),
                    previous.getDuration() - elapsed, previous.getAmplifier(), previous.isAmbient(),
                    previous.shouldShowParticles(), previous.shouldShowIcon()), owner);
        }
    }

    private static Element opposite(Element element) {
        return element == Element.FIRE ? Element.FROST : Element.FIRE;
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }

    private static double value(StormFrostWaterMasteryTuning tuning, String scoped, String shared, double fallback) {
        StormFrostWaterMasteryTuning.Setting scopedSetting = s(scoped);
        return tuning.has(scopedSetting) ? tuning.get(scopedSetting, fallback) : tuning.get(s(shared), fallback);
    }

    private static int integer(StormFrostWaterMasteryTuning tuning, String scoped, String shared, int fallback) {
        return (int) Math.round(value(tuning, scoped, shared, fallback));
    }

    private record MarkKey(UUID owner, UUID target) {
    }

    private static final class Marks {
        private int fire;
        private int frost;
        private long fireExpires;
        private long frostExpires;
        private long fireAppliedAt;
        private long frostAppliedAt;
        private long nextFirePulse;
        private long nextFrostPulse;
        private float fireDamage;
        private float frostDamage;
        private double fireWeight;
        private double frostWeight;
        private double firePotency = 1;
        private double frostPotency = 1;
        private StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY;

        private boolean hasBoth() {
            return fire > 0 && frost > 0;
        }

        private boolean empty() {
            return fire <= 0 && frost <= 0;
        }
    }

    private static final class Cadence {
        private int attacks;
        private int alternating;
        private Element lastPrimary;
        private Element locked;
        private long lockExpires;
    }

    private static final class Vortex {
        private final UniqueAbilityExecution execution;
        private final StormFrostWaterMasteryTuning tuning;
        private final Vec3d origin;
        private final long startedAt;
        private final long expiresAt;
        private final int fire;
        private final int frost;
        private final double baseDamage;
        private final int maximumSize;
        private final StatusEffectInstance previousSpeed;
        private final StatusEffectInstance previousResistance;
        private final StatusEffectInstance previousVortex;
        private final Map<UUID, Integer> hits = new HashMap<>();
        private final List<Wave> waves = new ArrayList<>();
        private long nextPulse;
        private int pulses;

        private Vortex(UniqueAbilityExecution execution, StormFrostWaterMasteryTuning tuning, Vec3d origin,
                       long startedAt, long expiresAt, int fire, int frost,
                       double baseDamage, int maximumSize, StatusEffectInstance previousSpeed,
                       StatusEffectInstance previousResistance, StatusEffectInstance previousVortex) {
            this.execution = execution;
            this.tuning = tuning;
            this.origin = origin;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
            this.fire = fire;
            this.frost = frost;
            this.baseDamage = baseDamage;
            this.maximumSize = maximumSize;
            this.previousSpeed = previousSpeed;
            this.previousResistance = previousResistance;
            this.previousVortex = previousVortex;
            this.nextPulse = startedAt;
        }

        private boolean consumedBoth() {
            return fire > 0 && frost > 0;
        }

        private double pulseDamage() {
            return baseDamage * resolveDamageMultiplier(tuning, fire + frost, consumedBoth());
        }

        private Vec3d center(LivingEntity owner) {
            return tuning.get(s("TEMPEST_FIXED_RANGE"), 0) > 0 ? origin : owner.getPos();
        }
    }

    private static final class Wave {
        private Vec3d center;
        private final Vec3d direction;
        private final boolean fire;
        private final Set<UUID> hit = new HashSet<>();
        private double travelled;

        private Wave(Vec3d center, Vec3d direction, boolean fire) {
            this.center = center;
            this.direction = direction;
            this.fire = fire;
        }
    }

    private static final class WorldState {
        private final Map<MarkKey, Marks> marks = new HashMap<>();
        private final Map<UUID, Cadence> cadences = new HashMap<>();
        private final Map<UUID, Vortex> vortices = new HashMap<>();

        private boolean empty() {
            return marks.isEmpty() && vortices.isEmpty();
        }
    }
}
