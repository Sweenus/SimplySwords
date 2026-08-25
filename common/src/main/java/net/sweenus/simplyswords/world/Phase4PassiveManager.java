package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.ability.Phase4AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase4UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.item.custom.HarbingerSwordItem;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Phase4PassiveManager {
    private static final Map<UUID, PassiveState> STATES = new HashMap<>();
    private static final Map<UUID, AllyCharge> ALLY_CHARGES = new HashMap<>();
    private static final Map<UUID, OwnerBonus> OWNER_BONUSES = new HashMap<>();

    private Phase4PassiveManager() {
    }

    public static void sunfireMelee(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;
        UniqueAbilityExecution execution = begin(Phase4UniqueAbilities.SUNFIRE_REGEN, world, stack, attacker, target);
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(execution);
        int chance = tuning.integer(s("CHANCE"), Config.uniqueEffects.sunfire.chance);
        boolean exact = tuning.has(s("CHANCE"));
        boolean proc = chance >= 100 || chance > 0 && (exact
                ? attacker.getRandom().nextInt(100) < chance
                : attacker.getRandom().nextInt(100) <= chance);
        PassiveState state = state(attacker, world.getTime());
        state.sunfireTuning = tuning;
        if (tuning.flag(131072)) {
            if (world.getTime() > state.comboDeadline) state.combo = 0;
            state.combo++;
            state.comboDeadline = world.getTime() + 80;
            if (state.combo >= 3 && world.getTime() >= state.flareReadyAt) {
                state.combo = 0;
                state.flareReadyAt = world.getTime() + tuning.integer(s("LOCKOUT_TICKS"), 40);
                flare(world, stack, attacker, target.getPos(), tuning);
                attacker.heal((float) tuning.get(s("HEAL_AMOUNT"), 1));
                UniqueAbilityApi.start(execution);
                UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.PULSE,
                        target, 1, tuning.get(s("FLARE_DAMAGE_MULTIPLIER"), .7));
            }
        } else if (proc) {
            world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), .3F, 1.7F);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,
                    tuning.integer(s("STATUS_DURATION_TICKS"), 40), 1), attacker);
            state.regenUntil = world.getTime() + tuning.integer(s("STATUS_DURATION_TICKS"), 40);
            if (attacker.getHealth() >= attacker.getMaxHealth()) state.reserve = Math.min(4, state.reserve + 1);
            if (tuning.flag(2048)) attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 80, 0), attacker);
            UniqueAbilityApi.start(execution);
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.HIT,
                    target, 1, chance);
        }
        if (execution.isStarted()) UniqueAbilityApi.finish(execution, Phase4UniqueAbilities.FINISH, 1);
        else UniqueAbilityApi.cancel(execution);
    }

    public static void harbingerMelee(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;
        UniqueAbilityExecution execution = begin(Phase4UniqueAbilities.HARBINGER_OMEN, world, stack, attacker, target);
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(execution);
        PassiveState state = state(attacker, world.getTime());
        int chance = tuning.integer(s("CHANCE"), Config.uniqueEffects.harbinger.chance);
        boolean exact = tuning.has(s("CHANCE"));
        boolean proc = chance >= 100 || chance > 0 && (exact
                ? attacker.getRandom().nextInt(100) < chance
                : attacker.getRandom().nextInt(100) <= chance);
        if (proc) {
            world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), .3F, 1.6F);
            if (tuning.flag(131072) && state.omenTarget != null && !state.omenTarget.equals(target.getUuid())) {
                if (world.getEntity(state.omenTarget) instanceof LivingEntity previous) {
                    previous.removeStatusEffect(StatusEffects.WEAKNESS);
                }
            }
            state.omenTarget = tuning.flag(131072) ? target.getUuid() : null;
            int amplifier = 0;
            if (tuning.flag(4096)) {
                if (world.getTime() > state.omenCounterDeadline || !target.getUuid().equals(state.counterTarget)) {
                    state.omenCounter = 0;
                }
                state.counterTarget = target.getUuid();
                state.omenCounter++;
                state.omenCounterDeadline = world.getTime() + 200;
                if (state.omenCounter % 3 == 0) amplifier = 1;
            }
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,
                    amplifier == 1 ? 80 : tuning.integer(s("STATUS_DURATION_TICKS"), 160), amplifier), attacker);
            UniqueAbilityApi.start(execution);
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.HIT,
                    target, 1, chance);
        }
        if (tuning.flag(8192)) {
            Vec3d standard = Phase4StandardManager.standardPosition(attacker.getUuid(), false);
            if (standard != null && target.getPos().squaredDistanceTo(standard) <= 100
                    && world.getTime() >= state.pullReadyAt) {
                state.pullReadyAt = world.getTime() + tuning.integer(s("LOCKOUT_TICKS"), 20);
                pull(target, standard, tuning.get(s("PULL_STRENGTH"), .75));
            }
        }
        if (target.isDead() && target.hasStatusEffect(StatusEffects.WEAKNESS) && tuning.flag(16384)) {
            Phase4StandardManager.reduceCooldown(attacker.getUuid(), tuning.integer(s("REFUND_TICKS"), 20));
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.KILL, target, 1, 0);
        }
        if (execution.isStarted()) UniqueAbilityApi.finish(execution, Phase4UniqueAbilities.FINISH, 1);
        else UniqueAbilityApi.cancel(execution);
    }

    public static void tickHeld(ItemStack stack, LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) return;
        PassiveState state = state(owner, world.getTime());
        if (state.reserve > 0 && owner.getHealth() / owner.getMaxHealth() < .5F) {
            owner.setAbsorptionAmount(Math.max(owner.getAbsorptionAmount(), state.reserve));
            state.reserve = 0;
        }
        UniqueAbilityExecution execution = begin(Phase4UniqueAbilities.SUNFIRE_REGEN, world, stack, owner, null);
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(execution);
        state.sunfireTuning = tuning;
        if (tuning.flag(32768) && owner.getHealth() / owner.getMaxHealth() < .3F
                && world.getTime() >= state.rekindleReadyAt) {
            state.rekindleReadyAt = world.getTime() + tuning.integer(s("LOCKOUT_TICKS"), 600);
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1), owner);
            owner.setAbsorptionAmount(Math.max(owner.getAbsorptionAmount(), 4));
            UniqueAbilityApi.start(execution);
        }
        if (execution.isStarted()) UniqueAbilityApi.finish(execution, Phase4UniqueAbilities.FINISH, 1);
        else UniqueAbilityApi.cancel(execution);
    }

    public static float modifyOutgoingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(source.getAttacker() instanceof LivingEntity attacker)) return amount;
        long tick = attacker.getWorld().getTime();
        ItemStack weapon = source.getWeaponStack();
        if (weapon != null && !weapon.isEmpty() && weapon.getItem() instanceof HarbingerSwordItem
                && target.hasStatusEffect(StatusEffects.WEAKNESS)
                && attacker.getWorld() instanceof ServerWorld world) {
            UniqueAbilityExecution execution = begin(Phase4UniqueAbilities.HARBINGER_OMEN,
                    world, weapon, attacker, target);
            Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(execution);
            double multiplier = tuning.get(s("MELEE_DAMAGE_MULTIPLIER"), 1);
            if (tuning.flag(65536)) multiplier *= tuning.get(s("WEAKENED_DAMAGE_MULTIPLIER"), .8);
            if (tuning.flag(131072) && isExecutionOmen(attacker, target)) {
                multiplier *= tuning.get(s("WEAKENED_DAMAGE_MULTIPLIER"), 1.4);
            }
            amount *= (float) multiplier;
            UniqueAbilityApi.cancel(execution);
        }
        OwnerBonus bonus = OWNER_BONUSES.get(attacker.getUuid());
        if (bonus != null) {
            if (bonus.expiresAt <= tick) OWNER_BONUSES.remove(attacker.getUuid());
            else amount *= 1 + bonus.bonus;
        }
        AllyCharge charge = ALLY_CHARGES.get(attacker.getUuid());
        if (charge != null) {
            if (charge.expiresAt <= tick) ALLY_CHARGES.remove(attacker.getUuid());
            else if (source.getSource() == attacker) {
                if (charge.weakness) {
                    charge.pendingWeakness = true;
                }
                if (charge.fireTicks > 0) charge.pendingFireTicks = charge.fireTicks;
                amount *= 1 + charge.damageBonus;
            }
        }
        return amount;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        PassiveState state = STATES.get(target.getUuid());
        if (state == null || state.sunfireTuning == null || !(target.getWorld() instanceof ServerWorld world)) {
            return amount;
        }
        if (state.sunfireTuning.flag(8192) && world.getTime() < state.regenUntil
                && source.getAttacker() instanceof LivingEntity attacker
                && source.getSource() == attacker
                && state.attackerLocks.getOrDefault(attacker.getUuid(), 0L) <= world.getTime()) {
            attacker.setOnFireFor(Math.max(1, state.sunfireTuning.integer(s("FIRE_TICKS"), 40) / 20));
            state.attackerLocks.put(attacker.getUuid(), world.getTime()
                    + state.sunfireTuning.integer(s("LOCKOUT_TICKS"), 40));
            trim(state.attackerLocks, 8);
        }
        return amount;
    }

    public static void onDamageApplied(LivingEntity target, DamageSource source) {
        if (!(source.getAttacker() instanceof LivingEntity attacker)) return;
        AllyCharge charge = ALLY_CHARGES.get(attacker.getUuid());
        if (charge != null && charge.pendingWeakness) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0), attacker);
            charge.pendingWeakness = false;
        }
        if (charge != null && charge.pendingFireTicks > 0) {
            target.setOnFireFor(Math.max(1, charge.pendingFireTicks / 20));
            charge.pendingFireTicks = 0;
        }
        if (charge != null && charge.singleUse) ALLY_CHARGES.remove(attacker.getUuid());
    }

    public static boolean isExecutionOmen(LivingEntity owner, LivingEntity target) {
        PassiveState state = STATES.get(owner.getUuid());
        return state != null && target.getUuid().equals(state.omenTarget);
    }

    public static void supportHarbingerAlly(LivingEntity owner, LivingEntity ally,
                                            Phase4AbilityTuning tuning, long tick) {
        boolean weakness = tuning.flag(128);
        float bonus = tuning.flag(1024) && ally != owner
                ? (float) tuning.get(s("SUPPORT_DAMAGE_BONUS"), .15)
                : tuning.flag(512) ? (float) tuning.get(s("SUPPORT_DAMAGE_BONUS"), .1) : 0;
        if (weakness || bonus > 0) {
            ALLY_CHARGES.put(ally.getUuid(), new AllyCharge(tick + 80, weakness, bonus, weakness, 0));
            trim(ALLY_CHARGES, 32);
        }
    }

    public static void supportSunfireAlly(LivingEntity ally, int duration, int fireTicks, long tick) {
        ALLY_CHARGES.put(ally.getUuid(), new AllyCharge(tick + duration, false, 0, true, fireTicks));
        trim(ALLY_CHARGES, 32);
    }

    public static void setHarbingerOwnerBonus(LivingEntity owner, float bonus, long expiresAt) {
        if (bonus <= 0) return;
        OWNER_BONUSES.put(owner.getUuid(), new OwnerBonus(expiresAt, bonus));
        trim(OWNER_BONUSES, 32);
    }

    private static UniqueAbilityExecution begin(UniqueAbilityDefinition definition, ServerWorld world,
                                                ItemStack stack, LivingEntity actor, LivingEntity target) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null), tuning -> {
                    tuning.set(Phase4UniqueAbilities.TUNING, Phase4AbilityTuning.EMPTY);
                    if (definition.cooldownKey().isPresent()) tuning.set(Phase4UniqueAbilities.COOLDOWN_TICKS,
                            definition == Phase4UniqueAbilities.SUNFIRE_STANDARD
                                    ? Config.uniqueEffects.sunfire.cooldown : Config.uniqueEffects.harbinger.cooldown);
                });
        UniqueAbilityApi.takeStartedExecution();
        return execution;
    }

    private static void flare(ServerWorld world, ItemStack stack, LivingEntity owner,
                              Vec3d center, Phase4AbilityTuning tuning) {
        double radius = tuning.get(s("RADIUS"), 3);
        int cap = tuning.integer(s("TARGET_CAP"), 8);
        Box box = new Box(center.x + radius, center.y + radius, center.z + radius,
                center.x - radius, center.y - radius, center.z - radius);
        float base = (float) owner.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                * (float) tuning.get(s("FLARE_DAMAGE_MULTIPLIER"), .7);
        world.getOtherEntities(owner, box).stream().filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast).filter(target -> HelperMethods.checkAbilityTarget(target, owner))
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                        .thenComparing(target -> target.getUuid().toString()))
                .limit(Math.clamp(cap, 0, 64)).forEach(target -> {
                    DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
                    target.damage(source, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, base));
                });
    }

    private static void dealBonus(LivingEntity attacker, LivingEntity target, ItemStack stack, float ratio) {
        if (!(attacker.getWorld() instanceof ServerWorld world) || ratio <= 0) return;
        float amount = (float) attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * ratio;
        DamageSource source = attacker.getDamageSources().indirectMagic(attacker, attacker);
        target.damage(source, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, amount));
    }

    private static void pull(LivingEntity target, Vec3d center, double strength) {
        Vec3d delta = center.subtract(target.getPos());
        if (delta.lengthSquared() < .001) return;
        Vec3d velocity = delta.normalize().multiply(strength);
        target.setVelocity(velocity.x, Math.clamp(velocity.y, -.3, .3), velocity.z);
        target.velocityModified = true;
    }

    private static PassiveState state(LivingEntity owner, long tick) {
        STATES.entrySet().removeIf(entry -> entry.getValue().expiresAt <= tick);
        PassiveState state = STATES.computeIfAbsent(owner.getUuid(), ignored -> new PassiveState());
        state.expiresAt = tick + 2400;
        trim(STATES, 32);
        return state;
    }

    private static <T> void trim(Map<UUID, T> map, int cap) {
        if (map.size() <= cap) return;
        map.keySet().stream().sorted(Comparator.comparing(UUID::toString)).limit(map.size() - cap)
                .toList().forEach(map::remove);
    }

    private static Phase4AbilityTuning.Setting s(String name) {
        return Phase4AbilityTuning.Setting.valueOf(name);
    }

    private static final class PassiveState {
        private long expiresAt;
        private long regenUntil;
        private long comboDeadline;
        private long flareReadyAt;
        private long rekindleReadyAt;
        private long omenCounterDeadline;
        private long pullReadyAt;
        private int reserve;
        private int combo;
        private int omenCounter;
        private UUID counterTarget;
        private UUID omenTarget;
        private Phase4AbilityTuning sunfireTuning;
        private final Map<UUID, Long> attackerLocks = new HashMap<>();
    }

    private static final class AllyCharge {
        private final long expiresAt;
        private final boolean weakness;
        private final float damageBonus;
        private final boolean singleUse;
        private final int fireTicks;
        private boolean pendingWeakness;
        private int pendingFireTicks;

        private AllyCharge(long expiresAt, boolean weakness, float damageBonus, boolean singleUse, int fireTicks) {
            this.expiresAt = expiresAt;
            this.weakness = weakness;
            this.damageBonus = damageBonus;
            this.singleUse = singleUse;
            this.fireTicks = fireTicks;
        }
    }

    private record OwnerBonus(long expiresAt, float bonus) {
    }
}
