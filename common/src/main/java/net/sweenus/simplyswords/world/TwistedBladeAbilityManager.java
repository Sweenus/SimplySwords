package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.TwistedBladeCrescendoVisualEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class TwistedBladeAbilityManager {

    private static final int VISUAL_LIFETIME = 8;
    private static final Map<ServerWorld, Map<UUID, WielderState>> WIELDER_STATES = new HashMap<>();

    private TwistedBladeAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, WielderState> states = WIELDER_STATES.get(world);
        return states != null && !states.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, WielderState> states = WIELDER_STATES.get(world);
        if (states == null || states.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<UUID, WielderState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, WielderState> entry = iterator.next();
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity actor) || !actor.isAlive() || actor.isRemoved()) {
                iterator.remove();
                continue;
            }

            WielderState state = entry.getValue();
            if (state.armed != null && now >= state.armed.expiresAt) {
                state.armed = null;
                actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
                state.hitCounter = 0;
            }
            if (getFerocityStacks(actor) <= 0) {
                state.hitCounter = 0;
            }
            if (state.armed == null && state.hitCounter <= 0 && getFerocityStacks(actor) <= 0) {
                iterator.remove();
            }
        }

        if (states.isEmpty()) {
            WIELDER_STATES.remove(world);
        }
    }

    public static int getFerocityStacks(LivingEntity actor) {
        if (actor == null) {
            return 0;
        }
        StatusEffectInstance ferocity =
                actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
        return ferocity == null ? 0 : Math.max(0, ferocity.getAmplifier() + 1);
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.TWISTED_BLADE.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || getFerocityStacks(context.actor()) <= 0
                || isArmed(context.world(), context.actor())) {
            return false;
        }

        if (context.actor() instanceof PlayerEntity) {
            return true;
        }
        LivingEntity target = context.target();
        return target != null
                && target.isAlive()
                && target != context.actor()
                && HelperMethods.checkAbilityTarget(target, context.actor())
                && (context.sourcePlayer() == null
                || target != context.sourcePlayer()
                && HelperMethods.checkAbilityTarget(target, context.sourcePlayer()));
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        LivingEntity actor = context.actor();
        int consumedStacks = getFerocityStacks(actor);
        if (consumedStacks <= 0) {
            return false;
        }

        WielderState state = state(context.world(), actor);
        state.hitCounter = 0;
        StatusEffectInstance currentFerocity =
                actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
        int empoweredWindow = Math.max(1, Config.uniqueEffects.twisted_blade.empoweredWindow);
        actor.addStatusEffect(
                new StatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.FEROCITY),
                        Math.max(empoweredWindow, currentFerocity == null ? 0 : currentFerocity.getDuration()),
                        consumedStacks - 1,
                        false,
                        false,
                        true
                ),
                actor
        );
        state.armed = new ArmedCrescendo(
                consumedStacks,
                context.world().getTime() + empoweredWindow
        );
        spawnActivationCue(context.world(), actor, consumedStacks);
        return true;
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack,
                                  LivingEntity reportedAttacker, LivingEntity target) {
        if (world == null
                || stack == null
                || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.TWISTED_BLADE.get())
                || reportedAttacker == null
                || target == null) {
            return;
        }

        DelegatedWeaponHitContext delegated = SimplySwordsAPI.getDelegatedWeaponHitContext();
        LivingEntity actor = delegated == null ? reportedAttacker : delegated.actor();
        LivingEntity sourceOwner = delegated == null ? null : delegated.owner();
        if (actor == null || !actor.isAlive() || actor.getWorld() != world || target.getWorld() != world) {
            return;
        }

        WielderState existingState = getState(world, actor);
        ArmedCrescendo armed = existingState == null ? null : existingState.armed;
        boolean empowered = armed != null && world.getTime() < armed.expiresAt;
        if (empowered) {
            existingState.armed = null;
            triggerCrescendo(
                    world,
                    stack,
                    actor,
                    sourceOwner,
                    target,
                    true,
                    armed.consumedStacks,
                    existingState
            );
            actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
            existingState.hitCounter = 0;
        }

        int stacks = tryGainFerocity(world, actor);
        if (empowered || stacks <= 0) {
            return;
        }

        WielderState state = existingState == null ? state(world, actor) : existingState;
        state.hitCounter++;
        int interval = getCrescendoInterval(stacks);
        if (state.hitCounter < interval) {
            return;
        }

        state.hitCounter = 0;
        triggerCrescendo(world, stack, actor, sourceOwner, target, false, stacks, state);
    }

    private static int tryGainFerocity(ServerWorld world, LivingEntity actor) {
        int chance = Math.clamp(Config.uniqueEffects.twisted_blade.chance, 0, 100);
        int currentStacks = getFerocityStacks(actor);
        if (chance <= 0 || actor.getRandom().nextInt(100) >= chance) {
            return currentStacks;
        }

        int maximumStacks = Math.max(1, Config.uniqueEffects.twisted_blade.maxStacks);
        int newStacks = Math.min(maximumStacks, currentStacks + 1);
        actor.addStatusEffect(
                new StatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.FEROCITY),
                        Math.max(1, Config.uniqueEffects.twisted_blade.duration),
                        newStacks - 1,
                        false,
                        false,
                        true
                ),
                actor
        );
        spawnStackGainCue(world, actor, newStacks, maximumStacks);
        return newStacks;
    }

    private static int getCrescendoInterval(int stacks) {
        int maximumStacks = Math.max(1, Config.uniqueEffects.twisted_blade.maxStacks);
        int tier = stacks >= maximumStacks
                ? 3
                : Math.min(3, Math.max(0, (Math.max(1, stacks) - 1) * 4 / maximumStacks));
        int baseInterval = Math.max(1, Config.uniqueEffects.twisted_blade.crescendoBaseInterval);
        int minimumInterval = Math.max(1, Math.min(
                baseInterval,
                Config.uniqueEffects.twisted_blade.crescendoMinimumInterval
        ));
        return Math.max(minimumInterval, Math.round(MathHelper.lerp(tier / 3.0F, baseInterval, minimumInterval)));
    }

    private static void triggerCrescendo(ServerWorld world, ItemStack stack, LivingEntity actor,
                                         LivingEntity sourceOwner, LivingEntity impactTarget,
                                         boolean empowered, int stacks, WielderState state) {
        int maximumStacks = Math.max(1, Config.uniqueEffects.twisted_blade.maxStacks);
        float stackFraction = MathHelper.clamp(stacks / (float) maximumStacks, 0.0F, 1.0F);
        float damageScaling = empowered
                ? MathHelper.lerp(
                stackFraction,
                Math.max(0.0F, Config.uniqueEffects.twisted_blade.empoweredMinimumDamageScaling),
                Math.max(0.0F, Config.uniqueEffects.twisted_blade.empoweredMaximumDamageScaling)
        )
                : Math.max(0.0F, Config.uniqueEffects.twisted_blade.crescendoDamageScaling);
        float spellScaling = empowered
                ? MathHelper.lerp(
                stackFraction,
                Math.max(0.0F, Config.uniqueEffects.twisted_blade.empoweredMinimumSpellScaling),
                Math.max(0.0F, Config.uniqueEffects.twisted_blade.empoweredMaximumSpellScaling)
        )
                : Math.max(0.0F, Config.uniqueEffects.twisted_blade.crescendoSpellScaling);
        double radius = empowered
                ? MathHelper.lerp(
                stackFraction,
                Math.max(0.1, Config.uniqueEffects.twisted_blade.empoweredMinimumRadius),
                Math.max(0.1, Config.uniqueEffects.twisted_blade.empoweredMaximumRadius)
        )
                : Math.max(0.1, Config.uniqueEffects.twisted_blade.crescendoRadius);
        double knockback = empowered
                ? MathHelper.lerp(
                stackFraction,
                Math.max(0.0, Config.uniqueEffects.twisted_blade.empoweredMinimumKnockback),
                Math.max(0.0, Config.uniqueEffects.twisted_blade.empoweredMaximumKnockback)
        )
                : Math.max(0.0, Config.uniqueEffects.twisted_blade.crescendoKnockback);

        Vec3d center = impactTarget.getPos().add(
                0.0,
                Math.max(0.35, impactTarget.getHeight() * 0.5),
                0.0
        );
        Vec3d facing = horizontalDirection(impactTarget.getPos().subtract(actor.getPos()), actor);
        float damage = HelperMethods.abilityScaledDamage("soul", actor, stack, damageScaling, spellScaling);
        Box area = new Box(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        );
        DamageSource source = SimplySwordsAPI.getWeaponDamageSource(actor);

        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                area,
                candidate -> isValidTarget(world, actor, sourceOwner, candidate)
        )) {
            float enchantedDamage = HelperMethods.applyAbilityDamageEnchantments(
                    world,
                    stack,
                    candidate,
                    source,
                    damage
            );
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(
                    () -> damaged[0] = HelperMethods.damageThroughIframes(candidate, source, enchantedDamage)
            );
            if (damaged[0]) {
                knockAway(candidate, center, facing, knockback, empowered ? 0.16 : 0.08);
            }
        }

        boolean mirrored = state.nextMirrored;
        state.nextMirrored = !state.nextMirrored;
        spawnCrescendoEffects(
                world,
                actor,
                center,
                (float) Math.max(0.0, center.y - impactTarget.getY()),
                facing,
                radius,
                empowered,
                mirrored
        );
    }

    private static boolean isValidTarget(ServerWorld world, LivingEntity actor,
                                         LivingEntity sourceOwner, LivingEntity target) {
        return target != null
                && target.isAlive()
                && target != actor
                && target != sourceOwner
                && target.getWorld() == world
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, actor)
                && (sourceOwner == null || HelperMethods.checkAbilityTarget(target, sourceOwner));
    }

    private static void knockAway(LivingEntity target, Vec3d center, Vec3d fallbackDirection,
                                  double strength, double lift) {
        Vec3d outward = target.getPos().subtract(center).multiply(1.0, 0.0, 1.0);
        if (outward.horizontalLengthSquared() < 0.0001) {
            outward = fallbackDirection;
        }
        if (strength > 0.0 && outward.horizontalLengthSquared() > 0.0001) {
            Vec3d direction = outward.normalize();
            target.takeKnockback(strength, -direction.x, -direction.z);
        }

        double resistance = Math.clamp(
                target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE),
                0.0,
                1.0
        );
        if (lift > 0.0 && resistance < 1.0) {
            target.addVelocity(0.0, lift * (1.0 - resistance), 0.0);
            target.velocityModified = true;
        }
    }

    private static void spawnStackGainCue(ServerWorld world, LivingEntity actor,
                                          int stacks, int maximumStacks) {
        float progress = MathHelper.clamp(stacks / (float) maximumStacks, 0.0F, 1.0F);
        Vec3d pos = actor.getPos().add(0.0, Math.max(0.45, actor.getHeight() * 0.62), 0.0);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z,
                2 + Math.round(progress * 3.0F), 0.22, 0.22, 0.22, 0.035);
        if (progress >= 0.5F) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z,
                    1 + Math.round(progress * 2.0F), 0.2, 0.25, 0.2, 0.02);
        }
        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_02.get(),
                actor.getSoundCategory(),
                0.28F,
                0.85F + progress * 0.65F
        );
    }

    private static void spawnActivationCue(ServerWorld world, LivingEntity actor, int stacks) {
        int maximumStacks = Math.max(1, Config.uniqueEffects.twisted_blade.maxStacks);
        float progress = MathHelper.clamp(stacks / (float) maximumStacks, 0.0F, 1.0F);
        Vec3d pos = actor.getPos().add(0.0, Math.max(0.4, actor.getHeight() * 0.5), 0.0);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z,
                12 + Math.round(progress * 14.0F), 0.42, 0.55, 0.42, 0.045);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z,
                8 + Math.round(progress * 8.0F), 0.38, 0.48, 0.38, 0.035);
        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                actor.getSoundCategory(),
                0.65F,
                0.78F + progress * 0.42F
        );
    }

    private static void spawnCrescendoEffects(ServerWorld world, LivingEntity actor,
                                              Vec3d center, float groundOffset, Vec3d facing, double radius,
                                              boolean empowered, boolean mirrored) {
        int multiplier = empowered ? 2 : 1;
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z,
                12 * multiplier, radius * 0.22, radius * 0.18, radius * 0.22, 0.07);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                8 * multiplier, radius * 0.25, radius * 0.2, radius * 0.25, 0.055);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z,
                empowered ? 3 : 1, radius * 0.12, radius * 0.08, radius * 0.12, 0.0);
        if (empowered) {
            world.spawnParticles(ParticleTypes.POOF, center.x, center.y, center.z,
                    16, radius * 0.3, radius * 0.12, radius * 0.3, 0.04);
        }

        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                empowered
                        ? SoundRegistry.MAGIC_SWORD_ATTACK_04.get()
                        : SoundRegistry.MAGIC_SWORD_WHOOSH_04.get(),
                actor.getSoundCategory(),
                empowered ? 1.0F : 0.72F,
                empowered ? 0.72F : 1.08F + world.random.nextFloat() * 0.12F
        );
        if (!empowered) {
            world.playSound(
                    null,
                    center.x,
                    center.y,
                    center.z,
                    SoundRegistry.MAGIC_SWORD_ATTACK_02.get(),
                    actor.getSoundCategory(),
                    0.82F,
                    1.02F + world.random.nextFloat() * 0.10F
            );
        }

        if (!Config.general.enableModernFieldEffects) {
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-facing.x, facing.z));
        TwistedBladeCrescendoVisualEntity visual = new TwistedBladeCrescendoVisualEntity(
                world,
                center.x,
                center.y,
                center.z,
                yaw,
                (float) (radius * 0.82),
                groundOffset,
                empowered,
                mirrored,
                VISUAL_LIFETIME
        );
        world.spawnEntity(visual);
    }

    private static Vec3d horizontalDirection(Vec3d direction, LivingEntity actor) {
        Vec3d horizontal = direction.multiply(1.0, 0.0, 1.0);
        if (horizontal.horizontalLengthSquared() < 0.0001) {
            horizontal = Vec3d.fromPolar(0.0F, actor.getYaw()).multiply(1.0, 0.0, 1.0);
        }
        return horizontal.horizontalLengthSquared() < 0.0001
                ? new Vec3d(0.0, 0.0, 1.0)
                : horizontal.normalize();
    }

    private static boolean isArmed(ServerWorld world, LivingEntity actor) {
        WielderState state = getState(world, actor);
        if (state == null || state.armed == null) {
            return false;
        }
        if (world.getTime() >= state.armed.expiresAt) {
            state.armed = null;
            return false;
        }
        return true;
    }

    private static WielderState getState(ServerWorld world, LivingEntity actor) {
        Map<UUID, WielderState> states = WIELDER_STATES.get(world);
        return states == null ? null : states.get(actor.getUuid());
    }

    private static WielderState state(ServerWorld world, LivingEntity actor) {
        return WIELDER_STATES
                .computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(actor.getUuid(), ignored -> new WielderState());
    }

    private static final class WielderState {
        private int hitCounter;
        private boolean nextMirrored;
        private ArmedCrescendo armed;
    }

    private record ArmedCrescendo(int consumedStacks, long expiresAt) {
    }
}
