package net.sweenus.simplyswords.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SoulstalkerStrideEntity extends MobEntity implements JumpingMount {
    public static final byte SURFACE_AIRBORNE = 0;
    public static final byte SURFACE_GROUND = 1;
    public static final byte SURFACE_WALL = 2;
    public static final byte SURFACE_CEILING = 3;
    public static final byte SURFACE_MANTLE = 4;
    private static final int[] STEP_ORDER = {0, 3, 1, 4, 2, 5};
    private static final int STEP_INTERVAL = 2;
    private static final int STEP_DURATION = 5;
    private static final int REATTACH_GRACE = 8;
    private static final TrackedData<Integer> OWNER_ID =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<UUID>> OWNER_UUID =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Byte> SURFACE_MODE =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Vector3f> SURFACE_NORMAL =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Integer> STEP_SEQUENCE =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Byte> STEP_LEG =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Integer> LEAP_SEQUENCE =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final DustColorTransitionParticleEffect GLOAM_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.015F, 0.004F, 0.035F),
                    new Vector3f(0.24F, 0.035F, 0.38F), 1.1F);

    private final List<PendingFootfall> pendingFootfalls = new ArrayList<>();
    private final Map<UUID, Long> footfallImmunity = new HashMap<>();
    private boolean jumpQueued;
    private float jumpScale = 0.4F;
    private int passengerlessTicks;
    private int reattachTicks;
    private int stepTicks;
    private int stepCursor;
    private byte footfallSurfaceMode = Byte.MIN_VALUE;
    private int forcedStepCursor;
    private int forcedStepsRemaining;
    private int forcedStepDelay;
    private Vec3d mantleStart;
    private Vec3d mantleControl;
    private Vec3d mantleTarget;
    private int mantleAge;
    private int mantleDuration;

    public SoulstalkerStrideEntity(EntityType<? extends SoulstalkerStrideEntity> type, World world) {
        super(type, world);
        setInvulnerable(true);
        experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder createStrideAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.215)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 2.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_FALL_DAMAGE_MULTIPLIER, 0.0);
    }

    @Override
    protected void initGoals() {
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER_ID, -1);
        builder.add(OWNER_UUID, Optional.empty());
        builder.add(SEED, 0);
        builder.add(SURFACE_MODE, SURFACE_GROUND);
        builder.add(SURFACE_NORMAL, new Vector3f(0.0F, 1.0F, 0.0F));
        builder.add(STEP_SEQUENCE, 0);
        builder.add(STEP_LEG, (byte) 0);
        builder.add(LEAP_SEQUENCE, 0);
    }

    public void setOwner(LivingEntity owner) {
        dataTracker.set(OWNER_ID, owner.getId());
        dataTracker.set(OWNER_UUID, Optional.of(owner.getUuid()));
        dataTracker.set(SEED, owner.getRandom().nextInt());
    }

    public LivingEntity getOwner() {
        UUID expected = getOwnerUuid();
        if (expected == null) {
            return null;
        }
        Entity entity = getOwnerId() < 0 ? null : getWorld().getEntityById(getOwnerId());
        if (entity != null && expected.equals(entity.getUuid()) && entity instanceof LivingEntity living) {
            return living;
        }
        if (!getWorld().isClient() && getWorld() instanceof ServerWorld world) {
            entity = world.getEntity(expected);
            if (entity instanceof LivingEntity living) {
                dataTracker.set(OWNER_ID, living.getId());
                return living;
            }
        }
        return null;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        LivingEntity owner = getOwner();
        return owner != null && hasPassenger(owner) ? owner : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        UUID ownerId = getOwnerUuid();
        return !hasPassengers() && ownerId != null && ownerId.equals(passenger.getUuid());
    }

    @Override
    public Vec3d getPassengerRidingPos(Entity passenger) {
        return getPos().add(0.0, MathHelper.clamp(Config.uniqueEffects.soulstalker.riderHeight, 0.5, 4.0), 0.0);
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity player, Vec3d input) {
        float forward = player.forwardSpeed < 0.0F ? player.forwardSpeed * 0.55F : player.forwardSpeed;
        return new Vec3d(player.sidewaysSpeed * 0.72F, 0.0, forward);
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity player) {
        return (float) MathHelper.clamp(Config.uniqueEffects.soulstalker.movementSpeed, 0.05, 1.0);
    }

    @Override
    protected void tickControlled(PlayerEntity player, Vec3d movementInput) {
        setYaw(player.getYaw());
        prevYaw = getYaw();
        bodyYaw = getYaw();
        headYaw = getYaw();
        setMovementSpeed(getSaddledSpeed(player));
        super.tickControlled(player, movementInput);
    }

    @Override
    public void tickMovement() {
        LivingEntity controller = getControllingPassenger();
        prepareMobController(controller);
        setNoGravity(isAdheredSurface());
        super.tickMovement();
        if (controller == null) {
            if (!getWorld().isClient()) {
                setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
            }
            return;
        }
        if (!getWorld().isClient() && getWorld() instanceof ServerWorld world) {
            if (getSurfaceMode() == SURFACE_MANTLE) {
                tickMantle(world);
            } else {
                updateSurfaceState(controller);
            }
        }
        if (getSurfaceMode() != SURFACE_MANTLE) {
            applySurfaceMovement(controller);
        }
        if (jumpQueued) {
            performLeap(controller);
        }
        if (!getWorld().isClient() && getWorld() instanceof ServerWorld world) {
            tickFootfalls(world, controller);
        }
        fallDistance = 0.0F;
        controller.fallDistance = 0.0F;
    }

    private void prepareMobController(LivingEntity controller) {
        if (controller == null || controller instanceof PlayerEntity) {
            return;
        }
        Vec3d direction = controller.getRotationVec(1.0F);
        if (controller instanceof MobEntity mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
            direction = mob.getTarget().getPos().subtract(getPos());
        }
        if (direction.horizontalLengthSquared() > 1.0E-5) {
            setYaw((float) (MathHelper.atan2(-direction.x, direction.z) * MathHelper.DEGREES_PER_RADIAN));
            bodyYaw = getYaw();
            headYaw = getYaw();
            forwardSpeed = 1.0F;
            sidewaysSpeed = 0.0F;
            setMovementSpeed((float) MathHelper.clamp(Config.uniqueEffects.soulstalker.movementSpeed, 0.05, 1.0));
        }
    }

    private void updateSurfaceState(LivingEntity controller) {
        if (reattachTicks > 0) {
            reattachTicks--;
            setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
            return;
        }
        byte mode = getSurfaceMode();
        Vec3d lookForward = horizontalFacing(controller.getYaw());
        Vec3d wallDirection = mode == SURFACE_WALL
                ? getSurfaceNormal().multiply(-1.0) : lookForward;
        BlockHitResult wall = findWall(wallDirection);
        BlockHitResult ceiling = findCeiling();
        if (mode == SURFACE_CEILING) {
            if (ceiling.getType() != HitResult.Type.MISS) {
                setSurface(SURFACE_CEILING, Vec3d.of(ceiling.getSide().getVector()));
            } else if (!tryBeginCeilingMantle(controller)) {
                setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
            }
            return;
        }
        if (mode == SURFACE_WALL) {
            if (isOnGround() && controller.forwardSpeed <= 0.05F) {
                setSurface(SURFACE_GROUND, new Vec3d(0.0, 1.0, 0.0));
            } else if (ceiling.getType() != HitResult.Type.MISS && controller.forwardSpeed > 0.05F) {
                setSurface(SURFACE_CEILING, Vec3d.of(ceiling.getSide().getVector()));
            } else if (wall.getType() != HitResult.Type.MISS) {
                setSurface(SURFACE_WALL, Vec3d.of(wall.getSide().getVector()));
            } else if (controller.forwardSpeed <= 0.05F || !tryBeginWallMantle()) {
                setSurface(isOnGround() ? SURFACE_GROUND : SURFACE_AIRBORNE,
                        new Vec3d(0.0, 1.0, 0.0));
            }
            return;
        }
        if (controller.forwardSpeed > 0.05F && wall.getType() != HitResult.Type.MISS) {
            setSurface(SURFACE_WALL, Vec3d.of(wall.getSide().getVector()));
        } else if (isOnGround()) {
            setSurface(SURFACE_GROUND, new Vec3d(0.0, 1.0, 0.0));
        } else if (ceiling.getType() != HitResult.Type.MISS && getVelocity().y > 0.0) {
            setSurface(SURFACE_CEILING, Vec3d.of(ceiling.getSide().getVector()));
        } else {
            setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
        }
    }

    private void applySurfaceMovement(LivingEntity controller) {
        byte mode = getSurfaceMode();
        if (mode == SURFACE_WALL) {
            Vec3d normal = horizontalNormal(getSurfaceNormal(), horizontalFacing(controller.getYaw()).multiply(-1.0));
            Vec3d tangent = new Vec3d(-normal.z, 0.0, normal.x);
            Vec3d ownerRight = rightFacing(controller.getYaw());
            if (tangent.dotProduct(ownerRight) < 0.0) {
                tangent = tangent.multiply(-1.0);
            }
            double speed = MathHelper.clamp(Config.uniqueEffects.soulstalker.climbSpeed, 0.05, 1.0);
            Vec3d velocity = tangent.multiply(controller.sidewaysSpeed * speed * 0.72)
                    .add(0.0, controller.forwardSpeed * speed, 0.0)
                    .add(normal.multiply(-0.055));
            setVelocity(velocity);
            velocityModified = true;
        } else if (mode == SURFACE_CEILING) {
            Vec3d forward = horizontalFacing(controller.getYaw());
            Vec3d right = rightFacing(controller.getYaw());
            double speed = MathHelper.clamp(Config.uniqueEffects.soulstalker.movementSpeed, 0.05, 1.0) * 0.9;
            double correction = ceilingCorrection();
            setVelocity(forward.multiply(controller.forwardSpeed * speed)
                    .add(right.multiply(controller.sidewaysSpeed * speed * 0.72))
                    .add(0.0, correction, 0.0));
            velocityModified = true;
        }
    }

    private double ceilingCorrection() {
        BlockHitResult ceiling = findCeiling();
        if (ceiling.getType() == HitResult.Type.MISS) {
            return 0.0;
        }
        double desiredTop = ceiling.getPos().y - 0.045;
        return MathHelper.clamp((desiredTop - (getY() + getAssemblyHeight())) * 0.48, -0.18, 0.18);
    }

    private BlockHitResult findWall(Vec3d direction) {
        Vec3d normalized = new Vec3d(direction.x, 0.0, direction.z);
        if (normalized.lengthSquared() < 1.0E-6) {
            normalized = horizontalFacing(getYaw());
        } else {
            normalized = normalized.normalize();
        }
        double[] heights = {0.35, MathHelper.clamp(Config.uniqueEffects.soulstalker.riderHeight, 0.5, 4.0) * 0.72, 3.45};
        BlockHitResult closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (double height : heights) {
            Vec3d start = getPos().add(0.0, height, 0.0);
            BlockHitResult hit = getWorld().raycast(new RaycastContext(
                    start, start.add(normalized.multiply(1.05)),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            if (hit.getType() == HitResult.Type.MISS || hit.getSide().getVector().getY() != 0) {
                continue;
            }
            double distance = start.squaredDistanceTo(hit.getPos());
            if (distance < closestDistance) {
                closest = hit;
                closestDistance = distance;
            }
        }
        return closest == null ? BlockHitResult.createMissed(getPos().add(normalized),
                net.minecraft.util.math.Direction.UP, getBlockPos()) : closest;
    }

    private BlockHitResult findCeiling() {
        double assemblyTop = getY() + getAssemblyHeight();
        Vec3d start = new Vec3d(getX(), assemblyTop - 0.22, getZ());
        return getWorld().raycast(new RaycastContext(
                start, start.add(0.0, 0.72, 0.0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
    }

    private boolean tryBeginWallMantle() {
        Vec3d normal = horizontalNormal(getSurfaceNormal(), horizontalFacing(getYaw()).multiply(-1.0));
        Vec3d intoWall = normal.multiply(-1.0);
        for (double distance : new double[]{0.72, 1.05, 1.38}) {
            Vec3d sample = getPos().add(intoWall.multiply(distance));
            BlockHitResult hit = getWorld().raycast(new RaycastContext(
                    sample.add(0.0, 2.6, 0.0), sample.add(0.0, -1.1, 0.0),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            if (hit.getType() == HitResult.Type.MISS || hit.getSide().getVector().getY() <= 0) {
                continue;
            }
            Vec3d target = new Vec3d(sample.x, hit.getPos().y + 0.025, sample.z);
            if (target.y < getY() - 0.45 || target.y > getY() + 2.6 || !canOccupy(target)) {
                continue;
            }
            Vec3d control = new Vec3d(getX() + normal.x * 0.78,
                    Math.max(getY(), target.y) + 0.62, getZ() + normal.z * 0.78);
            beginMantle(control, target, 8);
            return true;
        }
        return false;
    }

    private boolean tryBeginCeilingMantle(LivingEntity controller) {
        Vec3d travel = horizontalMovementDirection(controller);
        if (travel.lengthSquared() < 1.0E-6) {
            travel = horizontalFacing(controller.getYaw());
        }
        for (double distance : new double[]{0.55, 0.85, 1.15}) {
            Vec3d sample = getPos().subtract(travel.multiply(distance));
            double assemblyTop = getY() + getAssemblyHeight();
            double scanTop = assemblyTop + 2.8;
            BlockHitResult hit = getWorld().raycast(new RaycastContext(
                    new Vec3d(sample.x, scanTop, sample.z),
                    new Vec3d(sample.x, assemblyTop - 0.4, sample.z),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            if (hit.getType() == HitResult.Type.MISS || hit.getSide().getVector().getY() <= 0) {
                continue;
            }
            Vec3d target = new Vec3d(sample.x, hit.getPos().y + 0.025, sample.z);
            if (target.y > getY() + 5.2 || !canOccupy(target)) {
                continue;
            }
            Vec3d control = new Vec3d(getX() + travel.x * 0.82,
                    target.y + 0.68, getZ() + travel.z * 0.82);
            beginMantle(control, target, 12);
            return true;
        }
        return false;
    }

    private void beginMantle(Vec3d control, Vec3d target, int duration) {
        mantleStart = getPos();
        mantleControl = control;
        mantleTarget = target;
        mantleAge = 0;
        mantleDuration = Math.max(1, duration);
        setSurface(SURFACE_MANTLE, new Vec3d(0.0, 1.0, 0.0));
        setVelocity(Vec3d.ZERO);
        velocityModified = true;
    }

    private void tickMantle(ServerWorld world) {
        if (mantleStart == null || mantleControl == null || mantleTarget == null) {
            setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
            return;
        }
        mantleAge++;
        double progress = MathHelper.clamp(mantleAge / (double) mantleDuration, 0.0, 1.0);
        Vec3d outsideLow = new Vec3d(mantleControl.x, mantleStart.y, mantleControl.z);
        Vec3d next;
        if (progress < 0.3) {
            next = mantleStart.lerp(outsideLow, smooth(progress / 0.3));
        } else if (progress < 0.75) {
            next = outsideLow.lerp(mantleControl, smooth((progress - 0.3) / 0.45));
        } else {
            next = mantleControl.lerp(mantleTarget, smooth((progress - 0.75) / 0.25));
        }
        if (!canOccupy(next)) {
            clearMantle();
            setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
            return;
        }
        setPosition(next);
        setVelocity(Vec3d.ZERO);
        velocityModified = true;
        if (progress >= 1.0) {
            clearMantle();
            setSurface(SURFACE_GROUND, new Vec3d(0.0, 1.0, 0.0));
            world.spawnParticles(GLOAM_DUST, getX(), getY() + 0.12, getZ(),
                    12, 0.45, 0.08, 0.45, 0.025);
        }
    }

    private void clearMantle() {
        mantleStart = null;
        mantleControl = null;
        mantleTarget = null;
        mantleAge = 0;
        mantleDuration = 0;
    }

    private boolean canOccupy(Vec3d position) {
        double halfWidth = getDimensions(EntityPose.STANDING).width() * 0.5;
        Box box = new Box(position.x - halfWidth, position.y, position.z - halfWidth,
                position.x + halfWidth, position.y + getAssemblyHeight(), position.z + halfWidth)
                .contract(0.035);
        return !getWorld().getBlockCollisions(this, box).iterator().hasNext();
    }

    private double getAssemblyHeight() {
        LivingEntity owner = getControllingPassenger();
        double riderTop = MathHelper.clamp(Config.uniqueEffects.soulstalker.riderHeight, 0.5, 4.0)
                + (owner == null ? 1.8 : owner.getHeight());
        return Math.max(getDimensions(EntityPose.STANDING).height(), riderTop);
    }

    private void performLeap(LivingEntity controller) {
        byte mode = getSurfaceMode();
        if (!isAnchoredSurface(mode)) {
            jumpQueued = false;
            jumpScale = 0.4F;
            return;
        }
        Vec3d look = controller.getRotationVec(1.0F);
        Vec3d horizontal = new Vec3d(look.x, 0.0, look.z);
        if (horizontal.lengthSquared() < 1.0E-6) {
            horizontal = horizontalFacing(controller.getYaw());
        } else {
            horizontal = horizontal.normalize();
        }
        double pitchRetention = Math.max(0.35, Math.sqrt(Math.max(0.0, 1.0 - look.y * look.y)));
        double horizontalSpeed = Math.max(0.1, Config.uniqueEffects.soulstalker.leapHorizontalStrength)
                * jumpScale * pitchRetention;
        double verticalBase = Math.max(0.0, Config.uniqueEffects.soulstalker.leapVerticalStrength) * jumpScale;
        double verticalSpeed = mode == SURFACE_CEILING
                ? -Math.max(0.18, verticalBase * 0.48) + MathHelper.clamp(look.y * 0.18, -0.08, 0.08)
                : verticalBase + MathHelper.clamp(look.y * 0.35, -0.12, 0.2);
        Vec3d launch = horizontal.multiply(horizontalSpeed).add(0.0, verticalSpeed, 0.0);
        if (mode == SURFACE_WALL || mode == SURFACE_CEILING) {
            launch = launch.add(getSurfaceNormal().multiply(
                    Math.max(0.0, Config.uniqueEffects.soulstalker.leapSurfaceReleaseStrength)));
        }
        setVelocity(launch);
        velocityModified = true;
        velocityDirty = true;
        setNoGravity(false);
        jumpQueued = false;
        jumpScale = 0.4F;
        pendingFootfalls.clear();
        forcedStepsRemaining = 0;
        clearMantle();
        if (!getWorld().isClient() && getWorld() instanceof ServerWorld world) {
            reattachTicks = REATTACH_GRACE;
            setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
            dataTracker.set(LEAP_SEQUENCE, dataTracker.get(LEAP_SEQUENCE) + 1);
            world.spawnParticles(GLOAM_DUST, getX(), getBodyY(0.34), getZ(),
                    28, 0.78, 0.48, 0.78, 0.075);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, getX(), getBodyY(0.34), getZ(),
                    18, 0.62, 0.4, 0.62, 0.12);
            world.playSound(null, getBlockPos(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                    SoundCategory.PLAYERS, 0.8F, 0.72F);
            world.playSound(null, getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE,
                    SoundCategory.PLAYERS, 0.38F, 1.28F);
        }
    }

    private void tickFootfalls(ServerWorld world, LivingEntity controller) {
        long now = world.getTime();
        Iterator<PendingFootfall> iterator = pendingFootfalls.iterator();
        while (iterator.hasNext()) {
            PendingFootfall footfall = iterator.next();
            if (now < footfall.impactTick) {
                continue;
            }
            resolveFootfall(world, controller, footfall);
            iterator.remove();
        }
        if (now % 100L == 0L) {
            footfallImmunity.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        byte mode = getSurfaceMode();
        if (footfallSurfaceMode != mode) {
            stepTicks = 0;
            if (footfallSurfaceMode != Byte.MIN_VALUE && isAnchoredSurface(mode)) {
                forcedStepCursor = 0;
                forcedStepsRemaining = STEP_ORDER.length;
                forcedStepDelay = 0;
            } else {
                forcedStepsRemaining = 0;
            }
            footfallSurfaceMode = mode;
        }
        if (forcedStepsRemaining > 0) {
            if (forcedStepDelay-- <= 0) {
                queueFootfall(world, controller, mode, STEP_ORDER[forcedStepCursor++]);
                forcedStepsRemaining--;
                forcedStepDelay = STEP_INTERVAL - 1;
            }
            return;
        }
        boolean moving = Math.abs(controller.forwardSpeed) + Math.abs(controller.sidewaysSpeed) > 0.08F
                || mode == SURFACE_GROUND && getVelocity().horizontalLengthSquared() > 0.006;
        if (!isAnchoredSurface(mode) || !moving) {
            stepTicks = 0;
            return;
        }
        if (++stepTicks < STEP_INTERVAL) {
            return;
        }
        stepTicks = 0;
        int leg = STEP_ORDER[stepCursor++ % STEP_ORDER.length];
        queueFootfall(world, controller, mode, leg);
    }

    private void queueFootfall(ServerWorld world, LivingEntity controller, byte mode, int leg) {
        SoulstalkerStrideContactSolver.Contact contact = SoulstalkerStrideContactSolver.find(
                world, this, getPos(), controller.getBodyYaw(),
                MathHelper.clamp(Config.uniqueEffects.soulstalker.riderHeight, 0.5, 4.0),
                mode, getSurfaceNormal(), leg);
        dataTracker.set(STEP_LEG, (byte) leg);
        dataTracker.set(STEP_SEQUENCE, dataTracker.get(STEP_SEQUENCE) + 1);
        if (contact.valid()) {
            pendingFootfalls.add(new PendingFootfall(
                    contact.position(), contact.blockPos(), world.getTime() + STEP_DURATION));
        }
    }

    private void resolveFootfall(ServerWorld world, LivingEntity owner, PendingFootfall footfall) {
        BlockState state = world.getBlockState(footfall.blockPos);
        if (!state.isAir()) {
            BlockSoundGroup group = state.getSoundGroup();
            world.playSound(null, footfall.position.x, footfall.position.y, footfall.position.z,
                    group.getStepSound(), SoundCategory.PLAYERS,
                    group.getVolume() * 0.15F, group.getPitch());
        }
        world.spawnParticles(GLOAM_DUST, footfall.position.x, footfall.position.y, footfall.position.z,
                4, 0.16, 0.08, 0.16, 0.012);
        double radius = Math.max(0.1, Config.uniqueEffects.soulstalker.footfallRadius);
        float damage = Math.max(0.0F, (float) (HelperMethods.getEntityAttackDamage(owner)
                * Math.max(0.0, Config.uniqueEffects.soulstalker.footfallDamageScaling)));
        if (damage <= 0.0F) {
            return;
        }
        Box search = Box.of(footfall.position, radius * 2.0, radius * 2.0, radius * 2.0);
        int immunityTicks = Math.max(1, Config.uniqueEffects.soulstalker.footfallTargetImmunity);
        long now = world.getTime();
        DamageSource source = world.getDamageSources().indirectMagic(owner, owner);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkAbilityTarget(entity, owner))) {
            if (footfallImmunity.getOrDefault(target.getUuid(), Long.MIN_VALUE) > now
                    || !target.getBoundingBox().expand(radius).contains(footfall.position)) {
                continue;
            }
            float resolvedDamage = HelperMethods.applyWeaponAbilityDamageToPlayersModifier(target, damage);
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(source, resolvedDamage));
            if (damaged[0]) {
                footfallImmunity.put(target.getUuid(), now + immunityTicks);
                world.spawnParticles(GLOAM_DUST, target.getX(), target.getBodyY(0.4), target.getZ(),
                        7, 0.24, 0.16, 0.24, 0.025);
            }
        }
    }

    private Vec3d horizontalMovementDirection(LivingEntity controller) {
        Vec3d movement = horizontalFacing(controller.getYaw()).multiply(controller.forwardSpeed)
                .add(rightFacing(controller.getYaw()).multiply(controller.sidewaysSpeed));
        return movement.lengthSquared() < 1.0E-6 ? Vec3d.ZERO : movement.normalize();
    }

    private static Vec3d horizontalFacing(float yawDegrees) {
        float yaw = yawDegrees * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.sin(yaw), 0.0, MathHelper.cos(yaw));
    }

    private static Vec3d rightFacing(float yawDegrees) {
        float yaw = yawDegrees * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(MathHelper.cos(yaw), 0.0, MathHelper.sin(yaw));
    }

    private static Vec3d horizontalNormal(Vec3d value, Vec3d fallback) {
        Vec3d horizontal = new Vec3d(value.x, 0.0, value.z);
        if (horizontal.lengthSquared() < 1.0E-6) {
            horizontal = new Vec3d(fallback.x, 0.0, fallback.z);
        }
        return horizontal.lengthSquared() < 1.0E-6
                ? new Vec3d(0.0, 0.0, -1.0) : horizontal.normalize();
    }

    private static double smooth(double value) {
        double clamped = MathHelper.clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private void setSurface(byte mode, Vec3d normal) {
        dataTracker.set(SURFACE_MODE, mode);
        Vec3d resolved = normal.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 1.0, 0.0) : normal.normalize();
        dataTracker.set(SURFACE_NORMAL, resolved.toVector3f());
        setNoGravity(mode == SURFACE_WALL || mode == SURFACE_CEILING || mode == SURFACE_MANTLE);
    }

    private boolean isAdheredSurface() {
        byte mode = getSurfaceMode();
        return mode == SURFACE_WALL || mode == SURFACE_CEILING || mode == SURFACE_MANTLE;
    }

    private static boolean isAnchoredSurface(byte mode) {
        return mode == SURFACE_GROUND || mode == SURFACE_WALL || mode == SURFACE_CEILING;
    }

    @Override
    public void tick() {
        super.tick();
        if (hasPassengers()) {
            passengerlessTicks = 0;
        } else if (!getWorld().isClient() && ++passengerlessTicks > 10) {
            discard();
        }
    }

    @Override
    public boolean isClimbing() {
        return getSurfaceMode() == SURFACE_WALL || getSurfaceMode() == SURFACE_CEILING;
    }

    public boolean isStrideClimbing() {
        return isClimbing();
    }

    public boolean isGroundStrideSurface() {
        return getSurfaceMode() == SURFACE_GROUND;
    }

    public byte getSurfaceMode() {
        return dataTracker.get(SURFACE_MODE);
    }

    public Vec3d getSurfaceNormal() {
        Vector3f normal = dataTracker.get(SURFACE_NORMAL);
        return new Vec3d(normal.x, normal.y, normal.z);
    }

    public int getStepSequence() {
        return dataTracker.get(STEP_SEQUENCE);
    }

    public int getStepLeg() {
        return Math.clamp(dataTracker.get(STEP_LEG), 0, 5);
    }

    public int getLeapSequence() {
        return dataTracker.get(LEAP_SEQUENCE);
    }

    public int getSeed() {
        return dataTracker.get(SEED);
    }

    public int getOwnerId() {
        return dataTracker.get(OWNER_ID);
    }

    public UUID getOwnerUuid() {
        return dataTracker.get(OWNER_UUID).orElse(null);
    }

    @Override
    public void setJumpStrength(int strength) {
        int clamped = Math.max(0, strength);
        jumpScale = clamped >= 90 ? 1.0F : 0.4F + 0.4F * clamped / 90.0F;
    }

    @Override
    public boolean canJump() {
        return isAnchoredSurface(getSurfaceMode());
    }

    @Override
    public void startJumping(int height) {
        jumpQueued = true;
    }

    @Override
    public void stopJumping() {
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean shouldDismountUnderwater() {
        return false;
    }

    @Override
    public boolean canSprintAsVehicle() {
        return true;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
    }

    private record PendingFootfall(Vec3d position, net.minecraft.util.math.BlockPos blockPos, long impactTick) {
    }
}
