package net.sweenus.simplyswords.world;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class GloamMechanicsManager {
    private static final int CONTACT_INTERVAL = 5;
    private static final int CONTACT_GRACE = CONTACT_INTERVAL + 1;
    private static final Map<ServerWorld, Map<UUID, ExposureState>> ACTIVE = new HashMap<>();
    private static final DustParticleEffect GLOAM_DUST =
            new DustParticleEffect(new Vector3f(0.35F, 0.08F, 0.52F), 1.0F);
    private static boolean initialized;

    private GloamMechanicsManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        EntityEvent.ADD.register((entity, world) -> {
            if (entity instanceof LivingEntity living && !world.isClient()) {
                clearEffects(living);
            }
            return EventResult.pass();
        });
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ExposureState> states = ACTIVE.get(world);
        return states != null && !states.isEmpty();
    }

    public static void recordContact(ServerWorld world, LivingEntity owner,
                                     LivingEntity target, int baseSlowAmplifier) {
        if (world == null || owner == null || target == null || !owner.isAlive()
                || !target.isAlive() || target.isRemoved()) {
            return;
        }
        long now = world.getTime();
        Map<UUID, ExposureState> states = ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>());
        ExposureState state = states.computeIfAbsent(target.getUuid(), ignored ->
                new ExposureState(owner.getUuid(), now));
        if (owner.getUuid().equals(state.ownerId)) {
            state.ownerLastContactTick = now;
        } else if (now - state.ownerLastContactTick > CONTACT_GRACE
                || resolveLiving(world, state.ownerId) == null) {
            state.ownerId = owner.getUuid();
            state.ownerLastContactTick = now;
        }
        if (state.contactTick != now) {
            state.contactTick = now;
            state.baseSlowAmplifier = Math.clamp(baseSlowAmplifier, 0, 4);
        } else {
            state.baseSlowAmplifier = Math.max(
                    state.baseSlowAmplifier, Math.clamp(baseSlowAmplifier, 0, 4));
        }
        state.lastContactTick = now;
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ExposureState> states = ACTIVE.get(world);
        if (states == null || states.isEmpty()) {
            return;
        }
        long now = world.getTime();
        int buildTicks = Math.max(10, Config.uniqueEffects.gloam.exposureBuildTicks);
        int decayTicks = Math.max(1, Config.uniqueEffects.gloam.exposureDecayTicks);
        Iterator<Map.Entry<UUID, ExposureState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ExposureState> entry = iterator.next();
            ExposureState state = entry.getValue();
            LivingEntity target = resolveLiving(world, entry.getKey());
            LivingEntity owner = resolveLiving(world, state.ownerId);
            if (target == null || owner == null) {
                clearEffects(target);
                iterator.remove();
                continue;
            }
            boolean touching = now - state.lastContactTick <= CONTACT_GRACE;
            if (now < state.immunityEndTick) {
                state.exposure = 0.0F;
                updateExposureEffect(target, 0, owner);
                if (touching && now % CONTACT_INTERVAL == 0L) {
                    applySlow(target, owner, state.baseSlowAmplifier);
                }
                continue;
            }
            state.exposure = touching
                    ? Math.min(1.0F, state.exposure + 1.0F / buildTicks)
                    : Math.max(0.0F, state.exposure - 1.0F / decayTicks);
            if (state.exposure >= 1.0F) {
                triggerGrasp(world, target, owner, state, now);
                continue;
            }
            int visualStage = Math.clamp(MathHelper.floor(state.exposure * 4.0F), 0, 3);
            updateExposureEffect(target, visualStage, owner);
            if (touching && now % CONTACT_INTERVAL == 0L) {
                int maximumBonus = Math.clamp(Config.uniqueEffects.gloam.maximumSlowBonus, 0, 4);
                applySlow(target, owner,
                        Math.clamp(state.baseSlowAmplifier + Math.min(visualStage, maximumBonus), 0, 4));
            }
            if (!touching && state.exposure <= 0.0F) {
                updateExposureEffect(target, 0, owner);
                iterator.remove();
            }
        }
        if (states.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    public static void onTargetDeath(LivingEntity target) {
        if (!(target.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Map<UUID, ExposureState> states = ACTIVE.get(world);
        ExposureState state = states == null ? null : states.remove(target.getUuid());
        if (state == null || world.getTime() - state.lastContactTick > CONTACT_GRACE) {
            return;
        }
        LivingEntity owner = resolveLiving(world, state.ownerId);
        if (owner != null) {
            GloamStainManager.createGrowthPatch(
                    world, owner.getUuid(), target.getPos(), state.baseSlowAmplifier);
        }
        if (states.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    public static boolean isGrasped(LivingEntity entity) {
        return entity != null && entity.hasStatusEffect(
                EffectRegistry.getReference(EffectRegistry.GLOAM_GRASP));
    }

    private static void triggerGrasp(ServerWorld world, LivingEntity target,
                                     LivingEntity owner, ExposureState state, long now) {
        int duration = Math.max(1, Config.uniqueEffects.gloam.graspDuration);
        int immunity = Math.max(0, Config.uniqueEffects.gloam.graspImmunityDuration);
        state.exposure = 0.0F;
        state.immunityEndTick = now + duration + immunity;
        updateExposureEffect(target, 0, owner);
        target.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.GLOAM_GRASP),
                duration, 0, false, false, false), owner);
        Vec3d center = target.getBoundingBox().getCenter();
        world.spawnParticles(GLOAM_DUST, center.x, target.getY() + 0.12, center.z,
                18, target.getWidth() * 0.55, 0.12, target.getWidth() * 0.55, 0.035);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, target.getY() + 0.18, center.z,
                10, target.getWidth() * 0.45, 0.18, target.getWidth() * 0.45, 0.025);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ENTITY_SLIME_SQUISH, target.getSoundCategory(),
                0.25F, 0.66F + world.random.nextFloat() * 0.08F);
    }

    private static void updateExposureEffect(LivingEntity target, int stage, LivingEntity owner) {
        var effect = EffectRegistry.getReference(EffectRegistry.GLOAM_EXPOSURE);
        StatusEffectInstance current = target.getStatusEffect(effect);
        if (stage <= 0) {
            if (current != null) {
                target.removeStatusEffect(effect);
            }
            return;
        }
        int amplifier = stage - 1;
        if (current == null || current.getAmplifier() != amplifier) {
            target.addStatusEffect(new StatusEffectInstance(
                    effect, 200, amplifier, false, false, false), owner);
        }
    }

    private static void applySlow(LivingEntity target, LivingEntity owner, int amplifier) {
        target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS, CONTACT_INTERVAL * 2 + 1,
                Math.clamp(amplifier, 0, 4), false, false, true), owner);
    }

    private static void clearEffects(LivingEntity target) {
        if (target == null) {
            return;
        }
        target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.GLOAM_EXPOSURE));
        target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.GLOAM_GRASP));
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                ? living : null;
    }

    private static final class ExposureState {
        private UUID ownerId;
        private long lastContactTick;
        private long ownerLastContactTick;
        private long contactTick = Long.MIN_VALUE;
        private long immunityEndTick;
        private float exposure;
        private int baseSlowAmplifier;

        private ExposureState(UUID ownerId, long lastContactTick) {
            this.ownerId = ownerId;
            this.lastContactTick = lastContactTick;
            this.ownerLastContactTick = lastContactTick;
        }
    }
}
