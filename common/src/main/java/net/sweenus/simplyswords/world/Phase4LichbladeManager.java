package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase4AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase4UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.item.custom.StealSwordItem;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Phase4LichbladeManager {
    private static final Map<UUID, ChannelState> CHANNELS = new HashMap<>();
    private static final Map<UUID, PulseHistory> PULSE_HISTORY = new HashMap<>();

    private Phase4LichbladeManager() {
    }

    public static LivingEntity beginChannel(ServerWorld world, PlayerEntity owner, ItemStack stack) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(Phase4UniqueAbilities.LICHBLADE_CHANNEL,
                UniqueAbilityContext.passive(world, stack, owner, null, null), tuning -> tuning
                        .set(Phase4UniqueAbilities.TUNING, Phase4AbilityTuning.EMPTY)
                        .set(Phase4UniqueAbilities.COOLDOWN_TICKS, Config.uniqueEffects.lichblade.cooldown));
        UniqueAbilityApi.takeStartedExecution();
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(execution);
        LivingEntity target = StealSwordItem.findLenientTarget(owner,
                tuning.get(s("ACQUISITION_RANGE"), Config.uniqueEffects.lichblade.range));
        if (target == null) {
            UniqueAbilityApi.cancel(execution);
            return null;
        }
        if (tuning.isEmpty()) {
            UniqueAbilityApi.cancel(execution);
            return target;
        }
        UniqueAbilityApi.start(execution);
        CHANNELS.put(owner.getUuid(), new ChannelState(execution, stack, target, world.getTime()));
        trimChannels();
        return target;
    }

    public static boolean recall(LivingEntity owner, ItemStack stack) {
        ChannelState state = CHANNELS.get(owner.getUuid());
        if (state == null || !(owner.getWorld() instanceof ServerWorld world)) return false;
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(state.execution);
        if (!tuning.flag(65536)) return false;
        double radius = tuning.get(s("RECALL_RADIUS"), 4);
        float base = HelperMethods.abilityScaledDamage("soul", owner, state.stack,
                Config.uniqueEffects.lichblade.damageScaling, Config.uniqueEffects.lichblade.spellScaling)
                * (float) tuning.get(s("RECALL_DAMAGE_MULTIPLIER"), 1.25);
        Box box = new Box(state.cloud.x + radius, state.cloud.y + radius, state.cloud.z + radius,
                state.cloud.x - radius, state.cloud.y - radius, state.cloud.z - radius);
        int affected = 0;
        for (LivingEntity target : world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(target -> HelperMethods.checkFriendlyFire(target, owner))
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(state.cloud))
                        .thenComparing(target -> target.getUuid().toString())).limit(24).toList()) {
            DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
            if (target.damage(source, HelperMethods.applyAbilityDamageEnchantments(world, state.stack, target, source, base))) {
                affected++;
            }
        }
        state.hits += affected;
        state.recalled = true;
        finish(owner, stack, true);
        owner.stopUsingItem();
        return true;
    }

    public static int maxUseTime(LivingEntity owner) {
        ChannelState state = CHANNELS.get(owner.getUuid());
        if (state == null) return Config.uniqueEffects.lichblade.duration * 2;
        return Phase4UniqueAbilities.tuning(state.execution).integer(s("DURATION_TICKS"),
                Config.uniqueEffects.lichblade.duration) * 2;
    }

    public static boolean tickChannel(ServerWorld world, LivingEntity owner, ItemStack stack) {
        ChannelState state = CHANNELS.get(owner.getUuid());
        if (state == null || state.execution.isTerminal()) return false;
        if (owner.getEquippedStack(EquipmentSlot.MAINHAND) != stack || !owner.isAlive()) {
            finish(owner, stack, false);
            return true;
        }
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(state.execution);
        long elapsed = world.getTime() - state.startedAt;
        int duration = tuning.integer(s("DURATION_TICKS"), Config.uniqueEffects.lichblade.duration);
        LivingEntity target = entity(world, state.targetId);
        if (tuning.flag(32) && !valid(target, owner)) {
            finish(owner, stack, false);
            owner.stopUsingItem();
            return true;
        }
        if (!valid(target, owner)) target = retargetOrReturn(world, owner, state, tuning, elapsed);
        if (target == null) {
            finish(owner, stack, true);
            owner.stopUsingItem();
            return true;
        }
        int moveInterval = tuning.has(s("INTERVAL_TICKS")) && !tuning.flag(8)
                ? tuning.integer(s("INTERVAL_TICKS"), 5) : 5;
        if (elapsed % moveInterval == 0) moveCloud(state, target, tuning);
        int pulseInterval = tuning.flag(8) && elapsed >= 60 ? 4 : 5;
        if (!state.returning && elapsed % pulseInterval == 0) pulse(world, owner, state, tuning, target);
        if (tuning.flag(16)) owner.setVelocity(owner.getVelocity().multiply(.6, 1, .6));
        if (elapsed >= duration) {
            state.returning = true;
            state.targetId = owner.getUuid();
        }
        if (state.returning && state.cloud.squaredDistanceTo(owner.getPos())
                <= Math.pow(tuning.get(s("RADIUS"), Config.uniqueEffects.lichblade.radius), 2)) {
            finish(owner, stack, true);
            owner.stopUsingItem();
        }
        return true;
    }

    public static boolean finish(LivingEntity owner, ItemStack stack, boolean resolve) {
        ChannelState state = CHANNELS.remove(owner.getUuid());
        if (state == null) return false;
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(state.execution);
        if (resolve) resolveAbsorption(owner, state, tuning);
        SimplySwordsAPI.setWeaponCooldown(owner, stack, state.execution.cooldownTicks(Config.uniqueEffects.lichblade.cooldown)
                + (state.recalled ? 100 : 0)
                + (tuning.flag(2048) ? Math.min(200, state.successfulHeals * 10) : 0));
        if (resolve) UniqueAbilityApi.finish(state.execution, Phase4UniqueAbilities.FINISH, state.hits);
        else UniqueAbilityApi.cancel(state.execution);
        return true;
    }

    public static boolean tickPassive(ServerWorld world, LivingEntity owner, ItemStack stack) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(Phase4UniqueAbilities.LICHBLADE_AURA,
                UniqueAbilityContext.passive(world, stack, owner, null, null), tuning ->
                        tuning.set(Phase4UniqueAbilities.TUNING, Phase4AbilityTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(execution);
        if (tuning.isEmpty()) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
        int interval = tuning.integer(s("INTERVAL_TICKS"), 35);
        if (owner.age % interval != 0 || owner.getEquippedStack(EquipmentSlot.MAINHAND) != stack
                || owner.isUsingItem()) {
            UniqueAbilityApi.cancel(execution);
            return true;
        }
        UniqueAbilityApi.start(execution);
        ChannelState state = new ChannelState(execution, stack, owner, world.getTime());
        state.cloud = owner.getPos();
        pulse(world, owner, state, tuning, owner);
        UniqueAbilityApi.finish(execution, Phase4UniqueAbilities.FINISH, state.hits);
        return true;
    }

    public static boolean activateOnce(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(Phase4UniqueAbilities.LICHBLADE_CHANNEL,
                UniqueAbilityContext.active(context), tuning -> tuning
                        .set(Phase4UniqueAbilities.TUNING, Phase4AbilityTuning.EMPTY)
                        .set(Phase4UniqueAbilities.COOLDOWN_TICKS, Config.uniqueEffects.lichblade.cooldown));
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(execution);
        if (tuning.isEmpty()) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
        ChannelState state = new ChannelState(execution, context.stack(), context.target(), context.world().getTime());
        state.cloud = context.target().getPos();
        UniqueAbilityApi.start(execution);
        pulse(context.world(), context.actor(), state, tuning, context.target());
        resolveAbsorption(context.actor(), state, tuning);
        UniqueAbilityApi.finish(execution, Phase4UniqueAbilities.FINISH, state.hits);
        return true;
    }

    public static void onOwnerDamaged(LivingEntity owner) {
        ChannelState state = CHANNELS.get(owner.getUuid());
        if (state == null || state.interruptIgnored) return;
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(state.execution);
        if (tuning.flag(4096)) {
            state.interruptIgnored = true;
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 0), owner);
        }
    }

    private static void pulse(ServerWorld world, LivingEntity owner, ChannelState state,
                              Phase4AbilityTuning tuning, LivingEntity primary) {
        double radius = tuning.get(s("RADIUS"), Config.uniqueEffects.lichblade.radius);
        int cap = tuning.integer(s("TARGET_CAP"), tuning.flag(16) ? 32 : 24);
        List<LivingEntity> targets;
        if (tuning.flag(32)) {
            targets = valid(primary, owner) ? List.of(primary) : List.of();
        } else {
            Box box = new Box(state.cloud.x + radius, state.cloud.y + radius, state.cloud.z + radius,
                    state.cloud.x - radius, state.cloud.y - radius, state.cloud.z - radius);
            targets = world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                    .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                    .filter(target -> HelperMethods.checkFriendlyFire(target, owner))
                    .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(state.cloud))
                            .thenComparing(target -> target.getUuid().toString()))
                    .limit(Math.clamp(cap, 0, 64)).toList();
        }
        float base = HelperMethods.abilityScaledDamage("soul", owner, state.stack,
                Config.uniqueEffects.lichblade.damageScaling, Config.uniqueEffects.lichblade.spellScaling);
        double multiplier = tuning.get(s("DAMAGE_MULTIPLIER"), 1);
        if (tuning.flag(2)) multiplier *= 1 + Math.min(tuning.get(s("BONUS_CAP"), .24),
                Math.max(0, targets.size() - 1) * tuning.get(s("PER_TARGET_BONUS"), .03));
        for (LivingEntity target : targets) {
            if (!valid(target, owner)) continue;
            DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, state.stack, target, source,
                    base * (float) multiplier * (state.retargets == 0 ? 1 : (float) Math.max(.4, 1 - state.retargets * .15)));
            float before = target.getHealth();
            if (!target.damage(source, damage)) continue;
            state.hits++;
            recordPulse(target, owner, world.getTime(), tuning);
            addCharge(state, target, world.getTime(), tuning);
            heal(owner, state, tuning);
            UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.PULSE,
                    target, 1, damage);
            if (before > 0 && target.isDead()) {
                UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.KILL,
                        target, 1, damage);
                if (tuning.flag(4)) deathBurst(world, owner, state, tuning, target.getPos(), damage);
            }
        }
        world.playSoundFromEntity(null, owner, SoundRegistry.DARK_SWORD_BLOCK.get(),
                owner.getSoundCategory(), .1F, .3F);
    }

    private static void recordPulse(LivingEntity target, LivingEntity owner, long tick,
                                    Phase4AbilityTuning tuning) {
        if (!tuning.flag(1)) return;
        PulseHistory history = PULSE_HISTORY.computeIfAbsent(target.getUuid(), ignored -> new PulseHistory());
        if (tick - history.lastTick <= tuning.integer(s("LOCKOUT_TICKS"), 40)
                && owner.getUuid().equals(history.ownerId)) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    tuning.integer(s("STATUS_DURATION_TICKS"), 60), 0), owner);
        }
        history.ownerId = owner.getUuid();
        history.lastTick = tick;
        PULSE_HISTORY.entrySet().removeIf(entry -> tick - entry.getValue().lastTick > 200);
        if (PULSE_HISTORY.size() > 32) PULSE_HISTORY.keySet().stream().sorted(Comparator.comparing(UUID::toString))
                .limit(PULSE_HISTORY.size() - 32).toList().forEach(PULSE_HISTORY::remove);
    }

    private static void addCharge(ChannelState state, LivingEntity target, long tick,
                                  Phase4AbilityTuning tuning) {
        int amount = 1;
        if (tuning.flag(64)) {
            long ready = state.chargeLocks.getOrDefault(target.getUuid(), 0L);
            if (ready > tick) return;
            state.chargeLocks.put(target.getUuid(), tick + tuning.integer(s("CHARGE_LOCKOUT_TICKS"), 10));
            amount = tuning.integer(s("COUNT"), 2);
        }
        state.charge += amount;
        if (state.chargeLocks.size() > 32) state.chargeLocks.keySet().stream()
                .sorted(Comparator.comparing(UUID::toString)).limit(state.chargeLocks.size() - 32)
                .toList().forEach(state.chargeLocks::remove);
    }

    private static void heal(LivingEntity owner, ChannelState state, Phase4AbilityTuning tuning) {
        int chance = tuning.integer(s("CHANCE"), 9);
        boolean exact = tuning.has(s("CHANCE"));
        boolean success = chance >= 100 || chance > 0 && (exact
                ? owner.getRandom().nextInt(100) < chance : owner.getRandom().nextInt(100) <= chance - 1);
        if (!success) return;
        float amount = (float) tuning.get(s("HEAL_AMOUNT"), Config.uniqueEffects.lichblade.heal);
        float missing = owner.getMaxHealth() - owner.getHealth();
        owner.heal(amount);
        if (tuning.flag(256) && amount > missing) {
            float converted = (amount - Math.max(0, missing)) * (float) tuning.get(s("HEAL_MULTIPLIER"), .5);
            owner.setAbsorptionAmount((float) Math.min(owner.getAbsorptionAmount() + converted,
                    tuning.get(s("TEMP_ABSORPTION_CAP"), 4)));
        }
        state.successfulHeals++;
    }

    private static void resolveAbsorption(LivingEntity owner, ChannelState state, Phase4AbilityTuning tuning) {
        if (tuning.flag(2048)) return;
        float cap = (float) tuning.get(s("ABSORPTION_CAP"), Config.uniqueEffects.lichblade.absorptionCap);
        float conversion;
        if (tuning.flag(1024)) conversion = state.charge / (float) tuning.integer(s("COUNT"), 2);
        else {
            double interest = tuning.flag(512) ? Math.min(tuning.get(s("BONUS_CAP"), .4),
                    state.charge / Math.max(1, tuning.integer(s("COUNT"), 10))
                            * tuning.get(s("PER_TARGET_BONUS"), .1)) : 0;
            conversion = state.charge / 2F * (float) (1 + interest);
        }
        owner.setAbsorptionAmount(Math.min(Config.uniqueEffects.abilityAbsorptionCap,
                owner.getAbsorptionAmount() + Math.min(conversion, cap)));
        if (tuning.flag(128) && state.charge >= 4) {
            int duration = Math.min(tuning.integer(s("STATUS_DURATION_TICKS"), 120), state.charge / 4 * 40);
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0), owner);
        }
    }

    private static void deathBurst(ServerWorld world, LivingEntity owner, ChannelState state,
                                   Phase4AbilityTuning tuning, Vec3d center, float pulseDamage) {
        double radius = tuning.get(s("DEATH_BURST_RADIUS"), 3);
        Box box = new Box(center.x + radius, center.y + radius, center.z + radius,
                center.x - radius, center.y - radius, center.z - radius);
        world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(target -> HelperMethods.checkFriendlyFire(target, owner))
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                        .thenComparing(target -> target.getUuid().toString()))
                .limit(tuning.integer(s("DEATH_BURST_TARGET_CAP"), 4)).forEach(target -> {
                    DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
                    target.damage(source, pulseDamage * (float) tuning.get(s("DEATH_BURST_DAMAGE_MULTIPLIER"), .35));
                });
    }

    private static LivingEntity retargetOrReturn(ServerWorld world, LivingEntity owner, ChannelState state,
                                                  Phase4AbilityTuning tuning, long elapsed) {
        int maximum = tuning.flag(32768) ? tuning.integer(s("RETARGET_CAP"), 4)
                : tuning.flag(16384) && elapsed < 80 ? tuning.integer(s("RETARGET_CAP"), 1) : 0;
        if (state.retargets < maximum) {
            double range = tuning.get(s("RETARGET_RANGE"), 8);
            Box box = new Box(state.cloud.x + range, state.cloud.y + range, state.cloud.z + range,
                    state.cloud.x - range, state.cloud.y - range, state.cloud.z - range);
            LivingEntity target = world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                    .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                    .filter(entity -> valid(entity, owner))
                    .sorted(Comparator.comparingDouble((LivingEntity entity) -> entity.getPos().squaredDistanceTo(state.cloud))
                            .thenComparing(entity -> entity.getUuid().toString())).findFirst().orElse(null);
            if (target != null) {
                state.targetId = target.getUuid();
                state.retargets++;
                return target;
            }
        }
        state.returning = true;
        state.targetId = owner.getUuid();
        return owner;
    }

    private static void moveCloud(ChannelState state, LivingEntity target, Phase4AbilityTuning tuning) {
        Vec3d delta = target.getPos().subtract(state.cloud);
        double speed = state.returning && tuning.flag(8192) ? tuning.get(s("SPEED"), 1.5) : 1;
        if (delta.lengthSquared() <= speed * speed) state.cloud = target.getPos();
        else state.cloud = state.cloud.add(delta.normalize().multiply(speed));
    }

    private static LivingEntity entity(ServerWorld world, UUID uuid) {
        Entity entity = world.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static boolean valid(LivingEntity target, LivingEntity owner) {
        return target != null && target.isAlive() && (target == owner || HelperMethods.checkFriendlyFire(target, owner));
    }

    private static void trimChannels() {
        if (CHANNELS.size() <= 32) return;
        CHANNELS.keySet().stream().sorted(Comparator.comparing(UUID::toString)).limit(CHANNELS.size() - 32)
                .toList().forEach(CHANNELS::remove);
    }

    private static Phase4AbilityTuning.Setting s(String name) {
        return Phase4AbilityTuning.Setting.valueOf(name);
    }

    private static final class ChannelState {
        private final UniqueAbilityExecution execution;
        private final ItemStack stack;
        private final long startedAt;
        private final Map<UUID, Long> chargeLocks = new HashMap<>();
        private UUID targetId;
        private Vec3d cloud;
        private boolean returning;
        private boolean interruptIgnored;
        private boolean recalled;
        private int retargets;
        private int charge;
        private int successfulHeals;
        private int hits;

        private ChannelState(UniqueAbilityExecution execution, ItemStack stack,
                             LivingEntity target, long startedAt) {
            this.execution = execution;
            this.stack = stack.copy();
            this.startedAt = startedAt;
            this.targetId = target.getUuid();
            this.cloud = target.getPos();
        }
    }

    private static final class PulseHistory {
        private UUID ownerId;
        private long lastTick;
    }
}
