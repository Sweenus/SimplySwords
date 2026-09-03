package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EmberWeaponsMasteryManager {
    private static final Map<UUID, EmberbladeState> EMBERBLADE = new HashMap<>();
    private static final Map<UUID, EmberlashState> EMBERLASH = new HashMap<>();

    private EmberWeaponsMasteryManager() {
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown) {
        return UniqueAbilityApi.begin(definition, UniqueAbilityContext.active(context), tuning -> tuning
                .set(FireForgeMasteryAbilities.TUNING, FireForgeMasteryTuning.EMPTY)
                .set(FireForgeMasteryAbilities.COOLDOWN_TICKS, cooldown));
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null),
                tuning -> tuning.set(FireForgeMasteryAbilities.TUNING, FireForgeMasteryTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        return execution;
    }

    public static boolean releaseEmberblade(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = beginActive(FireForgeMasteryAbilities.EMBERBLADE_SHRAPNEL,
                context, Config.uniqueEffects.emberblade.cooldown);
        return releaseEmberblade(execution, context.world(), context.stack(), context.actor(),
                context.target(), 1, false);
    }

    public static boolean releaseEmberbladeCharge(ServerWorld world, ItemStack stack, LivingEntity actor,
                                                  LivingEntity target, float chargeRatio) {
        UniqueAbilityExecution execution = beginPassive(FireForgeMasteryAbilities.EMBERBLADE_SHRAPNEL,
                world, stack, actor, target);
        boolean result = releaseEmberblade(execution, world, stack, actor, target, chargeRatio, true);
        if (result) {
            int cooldown = FireForgeMasteryAbilities.tuning(execution).integer(s("COOLDOWN_TICKS"),
                    Config.uniqueEffects.emberblade.cooldown);
            SimplySwordsAPI.setWeaponCooldown(actor, stack, cooldown);
        }
        return result;
    }

    private static boolean releaseEmberblade(UniqueAbilityExecution execution, ServerWorld world, ItemStack stack,
                                             LivingEntity actor, LivingEntity target, float rawCharge, boolean charged) {
        if (target == null || !target.isAlive() || !HelperMethods.checkAbilityTarget(target, actor)) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
        UniqueAbilityApi.start(execution);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        EmberbladeState state = EMBERBLADE.computeIfAbsent(actor.getUuid(), ignored -> new EmberbladeState());
        long now = world.getTime();
        if (state.expiresAt <= now) state.reset();
        float charge = Math.clamp(rawCharge + state.bankedCharge, 0, 1);
        state.bankedCharge = 0;
        float initial = HelperMethods.abilityScaledDamage("fire", actor, stack,
                Config.uniqueEffects.emberblade.initialDamageScaling,
                Config.uniqueEffects.emberblade.initialSpellScaling);
        float maximum = HelperMethods.abilityScaledDamage("fire", actor, stack,
                Config.uniqueEffects.emberblade.maxChargeDamageScaling,
                Config.uniqueEffects.emberblade.maxChargeSpellScaling);
        float damage = initial * (float) tuning.get(s("DAMAGE_MULTIPLIER"), 1)
                + maximum * charge * (float) tuning.get(s("FINAL_DAMAGE_MULTIPLIER"), 1);
        if (tuning.flag(1 << 3) && aimAngle(actor, target) <= 2) damage *= 1.2F;
        boolean hit = deal(world, actor, stack, target, damage);
        if (!hit) {
            UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
            return true;
        }
        int fire = tuning.integer(s("FIRE_TICKS"), 0);
        if (fire > 0) target.setOnFireFor(Math.max(1, fire / 20));
        int affected = 1;
        if (tuning.flag(1 << 2)) {
            LivingEntity second = targets(world, actor, target.getPos(), tuning.get(s("RANGE"), 12), 2).stream()
                    .filter(candidate -> candidate != target).findFirst().orElse(null);
            if (second != null && deal(world, actor, stack, second,
                    damage * (float) tuning.get(s("SECONDARY_DAMAGE_MULTIPLIER"), .7))) affected++;
        }
        if (tuning.flag(1 << 5)) {
            affected += splash(world, actor, stack, target.getPos(), tuning.get(s("RADIUS"), 2),
                    tuning.integer(s("TARGET_CAP"), 6), damage * (float) tuning.get(s("SECONDARY_DAMAGE_MULTIPLIER"), .25), target);
        }
        if (charge >= .875F && tuning.flag(1 << 20)) {
            int fragments = tuning.flag(1 << 25) ? 5 : tuning.integer(s("COUNT"), 2);
            float fragmentDamage = damage * (float) tuning.get(s("SECONDARY_DAMAGE_MULTIPLIER"), .35);
            for (LivingEntity candidate : targets(world, actor, target.getPos(), 5, fragments + 1)) {
                if (candidate != target && deal(world, actor, stack, candidate, fragmentDamage)) affected++;
            }
        }
        applyEmberbladeRewards(world, actor, target, tuning, charge, now);
        if (tuning.flag(1 << 16)) {
            Vec3d side = target.getPos().subtract(actor.getPos()).normalize().multiply(-1.2);
            actor.requestTeleport(target.getX() + side.x, target.getY(), target.getZ() + side.z);
        } else {
            double recoil = tuning.flag(1 << 17) ? 4 : 1.1;
            actor.setVelocity(actor.getRotationVec(1).negate().multiply(recoil).multiply(1, 0, 1));
            actor.velocityModified = true;
        }
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, FireForgeMasteryAbilities.HIT,
                target, affected, damage);
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, affected);
        world.spawnParticles(ParticleTypes.LAVA, target.getX(), target.getBodyY(.5), target.getZ(),
                12, .35, .35, .35, .04);
        return true;
    }

    private static void applyEmberbladeRewards(ServerWorld world, LivingEntity actor, LivingEntity target,
                                               FireForgeMasteryTuning tuning, float charge, long now) {
        int chance = Config.uniqueEffects.emberblade.chance;
        boolean proc = tuning.has(s("CHANCE"))
                ? exactRoll(actor, tuning.integer(s("CHANCE"), chance))
                : actor.getRandom().nextInt((int) (250 - charge * 100)) <= chance;
        int duration = tuning.integer(s("STATUS_DURATION_TICKS"), Config.uniqueEffects.emberblade.duration);
        if (proc || tuning.flag(1 << 26) && charge >= .875F) {
            int amplifier = tuning.flag(1 << 26) ? 1 : 0;
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, amplifier), actor);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, amplifier), actor);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1), actor);
            world.playSoundFromEntity(null, actor, SoundRegistry.MAGIC_SWORD_SPELL_01.get(),
                    actor.getSoundCategory(), .5F, 2);
        }
        if (tuning.flag(1 << 9) && charge >= .25F) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0), actor);
        }
        if (tuning.flag(1 << 14) && charge >= .875F) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0), actor);
        }
        if (tuning.flag(1 << 22)) {
            EmberbladeState state = EMBERBLADE.computeIfAbsent(actor.getUuid(), ignored -> new EmberbladeState());
            state.hits++;
            state.expiresAt = now + 100;
            if (state.hits >= 3) {
                splash(world, actor, actor.getMainHandStack(), target.getPos(), 3, 8,
                        HelperMethods.abilityScaledDamage("fire", actor, actor.getMainHandStack(), .4F, 0), target);
                state.hits = 0;
            }
        }
    }

    public static void onEmberlashHit(ServerWorld world, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        UniqueAbilityExecution execution = beginPassive(FireForgeMasteryAbilities.EMBERLASH_SMOULDER,
                world, stack, attacker, target);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        StatusEffectInstance current = target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
        int stacks = current == null ? 0 : current.getAmplifier();
        int cap = tuning.integer(s("STACK_CAP"), Config.uniqueEffects.emberlash.maxStacks);
        if (current != null && !tuning.flag(1 << 25)) {
            float base = HelperMethods.abilityScaledDamage("fire", attacker, stack,
                    Config.uniqueEffects.emberlash.smoulderDamageScaling, Config.uniqueEffects.emberlash.spellScaling);
            float damage = base * stacks * (float) tuning.get(s("PER_STACK_MULTIPLIER"), 1);
            deal(world, attacker, stack, target, damage);
        }
        EmberlashState state = EMBERLASH.computeIfAbsent(attacker.getUuid(), ignored -> new EmberlashState());
        long now = world.getTime();
        if (state.expiresAt <= now) state.reset();
        int added = state.empoweredStacks > 0 ? state.empoweredStacks : 1;
        state.empoweredStacks = 0;
        if (tuning.flag(1 << 5)) {
            state.combo++;
            if (state.combo % 3 == 0) added++;
            state.expiresAt = now + 40;
        }
        if (tuning.flag(1 << 25) && stacks > 0) {
            float base = HelperMethods.abilityScaledDamage("fire", attacker, stack,
                    Config.uniqueEffects.emberlash.smoulderDamageScaling, Config.uniqueEffects.emberlash.spellScaling);
            deal(world, attacker, stack, target, base * stacks * .3F);
            target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
        } else {
            int duration = tuning.integer(s("DURATION_TICKS"), 100);
            HelperMethods.incrementStatusEffect(target, EffectRegistry.getReference(EffectRegistry.SMOULDERING),
                    duration, added, cap + 1);
            if (tuning.flag(1 << 3) && stacks + added >= 3) target.setOnFireFor(3);
            EmberlashSmoulderVisualManager.refresh(world, target);
        }
        if (state.reprisalCharges >= tuning.integer(s("COUNT"), 3) && tuning.flag(1 << 20)) {
            state.reprisalCharges = 0;
            float bonus = HelperMethods.abilityScaledDamage("fire", attacker, stack, .25F, 0);
            deal(world, attacker, stack, target, bonus);
        }
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, FireForgeMasteryAbilities.HIT, target, 1, stacks);
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 1);
    }

    public static boolean activateEmberlash(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = beginActive(FireForgeMasteryAbilities.EMBERLASH_CAUTERY,
                context, Config.uniqueEffects.emberlash.cooldown);
        UniqueAbilityApi.start(execution);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        LivingEntity actor = context.actor();
        LivingEntity target = context.target();
        double distance = 1.5 * tuning.get(s("SPEED"), 1);
        if (target != null && HelperMethods.checkAbilityTarget(target, actor)) {
            LivingEntityAbilityMovementManager.dashAwayFromTarget(context.world(), actor, target, distance, 8);
        } else {
            actor.setVelocity(actor.getRotationVec(1).negate().multiply(distance).multiply(1, 0, 1));
            actor.velocityModified = true;
        }
        if (!tuning.flag(1 << 16)) {
            actor.heal(actor.getMaxHealth() * Config.uniqueEffects.emberlash.heal / 100F
                    * (float) tuning.get(s("HEAL_MULTIPLIER"), 1));
        }
        if (tuning.flag(1 << 11)) actor.setAbsorptionAmount(Math.max(actor.getAbsorptionAmount(), 4));
        if (tuning.flag(1 << 14)) actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1), actor);
        EmberlashState state = EMBERLASH.computeIfAbsent(actor.getUuid(), ignored -> new EmberlashState());
        if (tuning.flag(1 << 13)) state.empoweredStacks = 2;
        state.expiresAt = context.world().getTime() + 80;
        if (tuning.flag(1 << 12)) {
            for (LivingEntity enemy : targets(context.world(), actor, actor.getPos(), 3, 6)) {
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 30, 0), actor);
            }
        }
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
        return true;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world) || amount <= 0) return amount;
        ItemStack stack = held(target, ItemsRegistry.EMBERLASH.get());
        if (stack != null) {
            UniqueAbilityExecution execution = beginPassive(FireForgeMasteryAbilities.EMBERLASH_SMOULDER,
                    world, stack, target, source.getAttacker() instanceof LivingEntity living ? living : null);
            FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
            EmberlashState state = EMBERLASH.get(target.getUuid());
            if (state != null && state.reprisalCharges > 0 && tuning.flag(1 << 26)) {
                amount *= Math.pow(tuning.get(s("INCOMING_MULTIPLIER"), 1.04), state.reprisalCharges);
            }
            if (source.getAttacker() instanceof LivingEntity attacker && tuning.flag(1 << 6)) {
                StatusEffectInstance effect = attacker.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SMOULDERING));
                if (effect != null && effect.getAmplifier() >= tuning.integer(s("STACK_CAP"), 5)) amount *= .88F;
            }
            UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
        }
        return amount;
    }

    public static void onDamageApplied(LivingEntity target, DamageSource source) {
        if (!(target.getWorld() instanceof ServerWorld world)
                || !(source.getAttacker() instanceof LivingEntity attacker)) return;
        ItemStack stack = held(target, ItemsRegistry.EMBERLASH.get());
        if (stack == null) return;
        UniqueAbilityExecution execution = beginPassive(FireForgeMasteryAbilities.EMBERLASH_SMOULDER,
                world, stack, target, attacker);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        EmberlashState state = EMBERLASH.computeIfAbsent(target.getUuid(), ignored -> new EmberlashState());
        long now = world.getTime();
        if (state.expiresAt <= now) state.reset();
        if (tuning.flag(1 << 19)) {
            state.reprisalCharges = Math.min(tuning.integer(s("STACK_CAP"), 3), state.reprisalCharges + 1);
            state.expiresAt = now + tuning.integer(s("DURATION_TICKS"), 100);
        }
        if (tuning.flag(1 << 18) && state.retortAt <= now) {
            HelperMethods.incrementStatusEffect(attacker, EffectRegistry.getReference(EffectRegistry.SMOULDERING),
                    100, 1, Config.uniqueEffects.emberlash.maxStacks + 1);
            state.retortAt = now + 30;
        }
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
    }

    private static boolean deal(ServerWorld world, LivingEntity actor, ItemStack stack,
                                LivingEntity target, float damage) {
        DamageSource source = actor instanceof PlayerEntity player
                ? world.getDamageSources().playerAttack(player) : world.getDamageSources().mobAttack(actor);
        float adjusted = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, damage);
        return HelperMethods.damageThroughIframes(target, source, adjusted);
    }

    private static int splash(ServerWorld world, LivingEntity actor, ItemStack stack, Vec3d center,
                              double radius, int cap, float damage, LivingEntity excluded) {
        int affected = 0;
        for (LivingEntity target : targets(world, actor, center, radius, cap + 1)) {
            if (target != excluded && deal(world, actor, stack, target, damage)) affected++;
            if (affected >= cap) break;
        }
        return affected;
    }

    private static List<LivingEntity> targets(ServerWorld world, LivingEntity actor, Vec3d center,
                                              double radius, int cap) {
        Box box = Box.of(center, radius * 2, radius * 2, radius * 2);
        return world.getEntitiesByClass(LivingEntity.class, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(target -> target != actor && HelperMethods.checkAbilityTarget(target, actor)
                        && target.getPos().squaredDistanceTo(center) <= radius * radius)
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                        .thenComparing(target -> target.getUuid().toString()))
                .limit(Math.min(64, cap)).toList();
    }

    private static double aimAngle(LivingEntity actor, LivingEntity target) {
        Vec3d direction = target.getEyePos().subtract(actor.getEyePos()).normalize();
        return Math.toDegrees(Math.acos(Math.clamp(actor.getRotationVec(1).normalize().dotProduct(direction), -1, 1)));
    }

    private static boolean exactRoll(LivingEntity actor, int chance) {
        return chance >= 100 || chance > 0 && actor.getRandom().nextInt(100) < chance;
    }

    private static ItemStack held(LivingEntity entity, net.minecraft.item.Item item) {
        if (entity.getMainHandStack().isOf(item)) return entity.getMainHandStack();
        if (entity.getOffHandStack().isOf(item)) return entity.getOffHandStack();
        return null;
    }

    private static FireForgeMasteryTuning.Setting s(String name) {
        return FireForgeMasteryTuning.Setting.valueOf(name);
    }

    private static final class EmberbladeState {
        private float bankedCharge;
        private int hits;
        private long expiresAt;

        private void reset() {
            bankedCharge = 0;
            hits = 0;
            expiresAt = 0;
        }
    }

    private static final class EmberlashState {
        private int combo;
        private int empoweredStacks;
        private int reprisalCharges;
        private long expiresAt;
        private long retortAt;

        private void reset() {
            combo = 0;
            empoweredStacks = 0;
            reprisalCharges = 0;
            expiresAt = 0;
        }
    }
}
