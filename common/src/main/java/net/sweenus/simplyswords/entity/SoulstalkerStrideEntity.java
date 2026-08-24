package net.sweenus.simplyswords.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.network.SoulstalkerLeapLaunchPacket;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.registry.SoulstalkerVoice;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.GloamStainManager;
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
    private static final int REATTACH_GRACE = 14;
    private static final int LEAP_RELEASE_GRACE = 14;
    private static final int LEAP_ARC_TICKS = 10;
    private static final int IDLE_INITIAL_DELAY_MIN = 20;
    private static final int IDLE_INITIAL_DELAY_VARIANCE = 61;
    private static final int IDLE_GAP_MIN = 60;
    private static final int IDLE_GAP_VARIANCE = 81;
    private static final double WALL_LATCH_ALIGNMENT = 0.55;
    private static final double CEILING_LATCH_ALIGNMENT = 0.45;
    private static final int WALL_COYOTE = 8;
    private static final int CORNER_GRACE = 4;
    private static final int CEILING_SUPPORT_GRACE = 5;
    private static final double CEILING_CLEARANCE = 0.045;
    private static final int STANDOFF_STALL_TICKS = 12;
    private static final double STANDOFF_HOLD_REACH = 2.6;
    private static final double MAX_STANDOFF = 1.65;
    private static final double WALL_ENTRY_REACH = 1.25;
    private static final double WALL_HOLD_REACH = 1.7;
    private static final double GROUND_CLING_DISTANCE = 3.0;
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
    private static final TrackedData<Float> SURFACE_FRAME_YAW =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> STEP_SEQUENCE =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Byte> STEP_LEG =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Integer> LEAP_SEQUENCE =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> LEAPING =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> VOICE_INDEX =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VOICE_SEQUENCE =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> CEILING_FORWARD_LOCKED =
            DataTracker.registerData(SoulstalkerStrideEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final DustColorTransitionParticleEffect GLOAM_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.015F, 0.004F, 0.035F),
                    new Vector3f(0.24F, 0.035F, 0.38F), 1.1F);

    private final List<PendingFootfall> pendingFootfalls = new ArrayList<>();
    private final Map<UUID, Long> footfallImmunity = new HashMap<>();
    private boolean jumpQueued;
    private float jumpScale = 0.6F;
    private byte queuedLeapSurfaceMode = Byte.MIN_VALUE;
    private Vec3d queuedLeapSurfaceNormal = Vec3d.ZERO;
    private Vec3d queuedLeapLook = Vec3d.ZERO;
    private int passengerlessTicks;
    private int reattachTicks;
    private int leapGraceTicks;
    private int leapArcTicks;
    private Vec3d leapDirection = Vec3d.ZERO;
    private Vec3d leapOrigin = Vec3d.ZERO;
    private Vec3d leapLastObservedPosition;
    private Vec3d leapLastObservedMotion = Vec3d.ZERO;
    private Vec3d serverLeapSyncVelocity = Vec3d.ZERO;
    private Vec3d leapSourceNormal = Vec3d.ZERO;
    private BlockPos leapSourceBlock;
    private double leapTravelDistance;
    private boolean leapDepartureConfirmed;
    private int leapTicks;
    private long nextIdleVoiceTick = Long.MIN_VALUE;
    private long activeVoiceEndTick = Long.MIN_VALUE;
    private int lastIdleVoiceIndex = -1;
    private boolean voiceLeapWasActive;
    private int wallLostTicks;
    private int cornerTicks;
    private int lateralSign = 1;
    private Vec3d lastWallNormal;
    private double standoffTargetY = Double.NEGATIVE_INFINITY;
    private Vec3d standoffOrigin;
    private double standoffDistance;
    private int standoffStallTicks;
    private double standoffStallY;
    private boolean overhangBlocked;
    private boolean nearGround;
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
    private double ceilingTargetY = Double.NaN;
    private int ceilingSupportTicks;

    public SoulstalkerStrideEntity(EntityType<? extends SoulstalkerStrideEntity> type, World world) {
        super(type, world);
        setInvulnerable(true);
        experiencePoints = 0;
        moveControl = new MoveControl(this) {
            @Override
            public void tick() {
            }
        };
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
        builder.add(SURFACE_FRAME_YAW, 0.0F);
        builder.add(STEP_SEQUENCE, 0);
        builder.add(STEP_LEG, (byte) 0);
        builder.add(LEAP_SEQUENCE, 0);
        builder.add(LEAPING, false);
        builder.add(VOICE_INDEX, -1);
        builder.add(VOICE_SEQUENCE, 0);
        builder.add(CEILING_FORWARD_LOCKED, false);
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
        if (isAdheredSurface() || (isLeapInProgress() && (!isOnGround() || getVelocity().y > 0.0))) {
            return Vec3d.ZERO;
        }
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
        if (leapGraceTicks > 0) {
            leapGraceTicks--;
        }
        nearGround = controller != null && hasGroundWithin(controller, GROUND_CLING_DISTANCE);
        setNoGravity(isAdheredSurface());
        Vec3d leapObservationStart = leapLastObservedPosition == null
                ? getPos() : leapLastObservedPosition;
        super.tickMovement();
        restoreServerLeapVelocity(controller);
        if (controller == null) {
            if (!getWorld().isClient()) {
                serverLeapSyncVelocity = Vec3d.ZERO;
                setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
            }
            return;
        }
        if (!getWorld().isClient() && getWorld() instanceof ServerWorld world) {
            resolveLeapContact(world, controller, leapObservationStart,
                    getPos().subtract(leapObservationStart));
            if (isLeapInProgress()) {
                leapLastObservedPosition = getPos();
            }
        }
        if (leapGraceTicks > 0 && isOnGround() && getVelocity().y <= 0.0) {
            leapGraceTicks = 0;
        }
        if (!getWorld().isClient() && getWorld() instanceof ServerWorld world) {
            if (getSurfaceMode() == SURFACE_MANTLE) {
                tickMantle(world);
            } else {
                updateSurfaceState(controller);
            }
        }
        if (getSurfaceMode() != SURFACE_MANTLE && leapGraceTicks <= 0) {
            applySurfaceMovement(controller);
        }
        if (jumpQueued) {
            performLeap(controller);
        }
        tickLeapArc();
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
            if (isOnGround() && getVelocity().y <= 0.0) {
                reattachTicks = 0;
            } else {
                reattachTicks--;
                setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
                return;
            }
        }
        byte mode = getSurfaceMode();
        Vec3d travel = travelDirection(controller);
        BlockHitResult wall = mode == SURFACE_WALL
                ? resolveWallFace(controller) : findWall(getPos(), travel, WALL_ENTRY_REACH);
        BlockHitResult ceiling = findCeiling();
        if (mode == SURFACE_CEILING) {
            CeilingSupport support = refreshCeilingSupport(controller, true);
            if (support != null || ceilingSupportTicks > 0) {
                Vec3d normal = support == null ? new Vec3d(0.0, -1.0, 0.0) : support.normal();
                setSurface(SURFACE_CEILING, normal);
            } else if (!tryBeginCeilingMantle(controller)) {
                setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
            }
            return;
        }
        if (mode == SURFACE_WALL) {
            Vec3d motion = wallMotion(controller,
                    horizontalNormal(getSurfaceNormal(), horizontalFacing(getYaw()).multiply(-1.0)));
            boolean driving = Math.abs(controller.forwardSpeed) > 0.05F
                    || Math.abs(controller.sidewaysSpeed) > 0.05F;
            boolean climbing = motion.y > 0.05;
            if (isOnGround() && !climbing) {
                wallLostTicks = 0;
                setSurface(SURFACE_GROUND, new Vec3d(0.0, 1.0, 0.0));
                return;
            }
            if (ceiling.getType() != HitResult.Type.MISS && climbing
                    && (overhangBlocked || overhangBypassDistance(getSurfaceNormal()) < 0.0)) {
                wallLostTicks = 0;
                overhangBlocked = false;
                enterCeiling(controller, Vec3d.of(ceiling.getSide().getVector()), getSurfaceNormal());
                return;
            }
            if (wall.getType() != HitResult.Type.MISS) {
                wallLostTicks = 0;
                setSurface(SURFACE_WALL, Vec3d.of(wall.getSide().getVector()));
                return;
            }
            if (climbing && tryBeginWallMantle()) {
                wallLostTicks = 0;
                return;
            }
            if (driving && ++wallLostTicks <= WALL_COYOTE) {
                return;
            }
            wallLostTicks = 0;
            setSurface(isOnGround() ? SURFACE_GROUND : SURFACE_AIRBORNE,
                    new Vec3d(0.0, 1.0, 0.0));
            return;
        }
        wallLostTicks = 0;
        boolean rising = getVelocity().y > 0.08;
        if (controller.forwardSpeed > 0.05F && wall.getType() != HitResult.Type.MISS
                && !canStepOver(travel) && isMovingInto(wall)) {
            setSurface(SURFACE_WALL, Vec3d.of(wall.getSide().getVector()));
        } else if (isOnGround() || !rising && nearGround) {
            setSurface(SURFACE_GROUND, new Vec3d(0.0, 1.0, 0.0));
        } else if (ceiling.getType() != HitResult.Type.MISS && getVelocity().y > 0.0) {
            enterCeiling(controller, Vec3d.of(ceiling.getSide().getVector()), travel);
        } else {
            setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
        }
    }

    private boolean isMovingInto(BlockHitResult wall) {
        Vec3d intoWall = Vec3d.of(wall.getSide().getVector()).multiply(-1.0);
        Vec3d velocity = new Vec3d(getVelocity().x, 0.0, getVelocity().z);
        return velocity.lengthSquared() < 1.0E-4 || velocity.dotProduct(intoWall) > 0.0;
    }

    private boolean hasGroundWithin(LivingEntity controller, double distance) {
        Vec3d travel = travelDirection(controller);
        double halfWidth = getDimensions(EntityPose.STANDING).width() * 0.5;
        Vec3d[] samples = {
                getPos(),
                getPos().add(travel.multiply(halfWidth)),
                getPos().subtract(travel.multiply(halfWidth))
        };
        for (Vec3d sample : samples) {
            Vec3d start = sample.add(0.0, 0.05, 0.0);
            BlockHitResult hit = getWorld().raycast(new RaycastContext(
                    start, start.add(0.0, -distance, 0.0),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            if (hit.getType() != HitResult.Type.MISS && hit.getSide().getVector().getY() > 0) {
                return true;
            }
        }
        return false;
    }

    private void tickLeapArc() {
        if (leapArcTicks <= 0) {
            return;
        }
        if (isOnGround() || isAdheredSurface() || getVelocity().y <= 0.0) {
            leapArcTicks = 0;
            return;
        }
        leapArcTicks--;
        if (isLogicalSideForUpdatingMovement()) {
            setVelocity(getVelocity().add(0.0, 0.032, 0.0));
            velocityModified = true;
        }
    }

    private Vec3d travelDirection(LivingEntity controller) {
        Vec3d travel = horizontalMovementDirection(controller);
        return travel.lengthSquared() < 1.0E-6 ? horizontalFacing(controller.getYaw()) : travel;
    }

    private boolean canStepOver(Vec3d travel) {
        double stepHeight = Math.max(0.5, getStepHeight());
        double halfWidth = getDimensions(EntityPose.STANDING).width() * 0.5;
        Vec3d ahead = getPos().add(travel.multiply(halfWidth + 0.45));
        Box probe = new Box(ahead.x - halfWidth, getY() + 0.05, ahead.z - halfWidth,
                ahead.x + halfWidth, getY() + stepHeight + 0.05, ahead.z + halfWidth);
        double top = Double.NEGATIVE_INFINITY;
        for (VoxelShape shape : getWorld().getBlockCollisions(this, probe)) {
            if (!shape.isEmpty()) {
                top = Math.max(top, shape.getBoundingBox().maxY);
            }
        }
        if (top == Double.NEGATIVE_INFINITY || top > getY() + stepHeight + 1.0E-4) {
            return false;
        }
        return canOccupy(new Vec3d(ahead.x, top + 0.02, ahead.z));
    }

    private void applySurfaceMovement(LivingEntity controller) {
        byte mode = getSurfaceMode();
        if (mode == SURFACE_WALL) {
            if (cornerTicks > 0) {
                cornerTicks--;
            }
            Vec3d normal = horizontalNormal(getSurfaceNormal(), horizontalFacing(controller.getYaw()).multiply(-1.0));
            if (lastWallNormal != null && lastWallNormal.dotProduct(normal) < 0.9) {
                cornerTicks = CORNER_GRACE;
            }
            lastWallNormal = normal;
            Vec3d motion = wallMotion(controller, normal);
            double speed = MathHelper.clamp(Config.uniqueEffects.soulstalker.climbSpeed, 0.05, 1.0);
            Vec3d velocity = motion.multiply(speed);
            boolean climbing = motion.y > 0.05;
            if (tickStandoff(normal, climbing)) {
                velocity = new Vec3d(motion.x * speed, speed * 0.95, motion.z * speed);
                if (standoffTravel(normal) < standoffDistance) {
                    velocity = velocity.add(normal.multiply(0.22));
                }
            } else {
                double adhesion = wallAdhesion(normal) * (cornerTicks > 0 ? 0.5 : 1.0);
                velocity = velocity.add(normal.multiply(-adhesion));
            }
            setVelocity(velocity);
            velocityModified = true;
        } else if (mode == SURFACE_CEILING) {
            if (getWorld().isClient()) {
                refreshCeilingSupport(controller, false);
            }
            if (dataTracker.get(CEILING_FORWARD_LOCKED) && controller.forwardSpeed <= 0.05F) {
                dataTracker.set(CEILING_FORWARD_LOCKED, false);
                dataTracker.set(SURFACE_FRAME_YAW, 0.0F);
            }
            float frameYaw = controller.getYaw() + getFrameYaw();
            Vec3d forward = horizontalFacing(frameYaw);
            Vec3d right = rightFacing(frameYaw);
            double speed = MathHelper.clamp(Config.uniqueEffects.soulstalker.movementSpeed, 0.05, 1.0) * 0.9;
            double correction = ceilingCorrection();
            Vec3d horizontal = forward.multiply(controller.forwardSpeed * speed)
                    .add(right.multiply(controller.sidewaysSpeed * speed * 0.72));
            if (!Double.isNaN(ceilingTargetY) && ceilingTargetY < getY() - 0.08) {
                double remainingDrop = getY() - ceilingTargetY;
                double travelScale = MathHelper.clamp(1.0 - (remainingDrop - 0.08) / 0.45, 0.08, 1.0);
                horizontal = horizontal.multiply(travelScale);
            }
            setVelocity(horizontal
                    .add(0.0, correction, 0.0));
            velocityModified = true;
            lastWallNormal = null;
            clearStandoff();
        } else {
            lastWallNormal = null;
            clearStandoff();
        }
    }

    private boolean tickStandoff(Vec3d normal, boolean climbing) {
        if (isStandingOff()) {
            if (!climbing || getY() >= standoffTargetY) {
                clearStandoff();
                return false;
            }
            if (getY() > standoffStallY + 0.05) {
                standoffStallY = getY();
                standoffStallTicks = 0;
            } else if (++standoffStallTicks > STANDOFF_STALL_TICKS) {
                double retry = overhangBypassDistance(normal);
                double extended = standoffTravel(normal) + retry;
                if (retry > 0.0 && extended > standoffDistance + 0.1 && extended <= MAX_STANDOFF) {
                    standoffDistance = extended;
                    standoffStallTicks = 0;
                } else {
                    clearStandoff();
                    overhangBlocked = true;
                    return false;
                }
            }
            return true;
        }
        if (!climbing || !climbBlocked()) {
            overhangBlocked = false;
            return false;
        }
        double bypass = overhangBypassDistance(normal);
        if (bypass <= 0.0 || bypass > MAX_STANDOFF) {
            return false;
        }
        standoffOrigin = getPos();
        standoffDistance = bypass;
        standoffTargetY = overhangTopY(normal) + 0.1;
        standoffStallY = getY();
        standoffStallTicks = 0;
        overhangBlocked = false;
        return true;
    }

    private boolean isStandingOff() {
        return standoffTargetY != Double.NEGATIVE_INFINITY && standoffOrigin != null;
    }

    private double standoffTravel(Vec3d normal) {
        return standoffOrigin == null ? 0.0 : getPos().subtract(standoffOrigin).dotProduct(normal);
    }

    private double wallHoldReach() {
        return isStandingOff() ? STANDOFF_HOLD_REACH : WALL_HOLD_REACH;
    }

    private void clearStandoff() {
        standoffTargetY = Double.NEGATIVE_INFINITY;
        standoffOrigin = null;
        standoffDistance = 0.0;
        standoffStallTicks = 0;
        standoffStallY = 0.0;
    }

    private boolean climbBlocked() {
        return !canOccupy(getPos().add(0.0, 0.5, 0.0));
    }

    private double overhangTopY(Vec3d normal) {
        double halfWidth = getDimensions(EntityPose.STANDING).width() * 0.45;
        double assemblyTop = getY() + getAssemblyHeight();
        Vec3d center = getPos().add(normal.multiply(0.1));
        Box probe = new Box(center.x - halfWidth, assemblyTop - 0.1, center.z - halfWidth,
                center.x + halfWidth, assemblyTop + 4.5, center.z + halfWidth);
        double top = Double.NEGATIVE_INFINITY;
        for (VoxelShape shape : getWorld().getBlockCollisions(this, probe)) {
            if (!shape.isEmpty()) {
                top = Math.max(top, shape.getBoundingBox().maxY);
            }
        }
        return top == Double.NEGATIVE_INFINITY ? assemblyTop + 1.0 : top;
    }

    private double wallFaceDistance(Vec3d normal) {
        BlockHitResult wall = findWall(getPos(), normal.multiply(-1.0), wallHoldReach());
        if (wall.getType() == HitResult.Type.MISS) {
            return Double.MAX_VALUE;
        }
        Vec3d delta = wall.getPos().subtract(getPos());
        return Math.sqrt(delta.x * delta.x + delta.z * delta.z);
    }

    private double wallAdhesion(Vec3d normal) {
        double distance = wallFaceDistance(normal);
        if (distance == Double.MAX_VALUE) {
            return 0.32;
        }
        return MathHelper.clamp(0.055 + (distance - 0.8) * 0.42, 0.055, 0.32);
    }

    private double ceilingCorrection() {
        double targetY = ceilingTargetY;
        if (Double.isNaN(targetY)) {
            BlockHitResult ceiling = findCeiling();
            if (ceiling.getType() == HitResult.Type.MISS) {
                return 0.0;
            }
            targetY = ceiling.getPos().y - getAssemblyHeight() - CEILING_CLEARANCE;
        }
        return MathHelper.clamp((targetY - getY()) * 0.5, -0.26, 0.26);
    }

    private CeilingSupport refreshCeilingSupport(LivingEntity controller, boolean useGrace) {
        double stepHeight = Math.max(0.5, getStepHeight());
        CeilingSupport local = findCeilingSupport(getPos(), stepHeight);
        CeilingSupport selected = local;
        Vec3d movement = ceilingMovement(controller);
        if (movement.lengthSquared() > 1.0E-6) {
            Vec3d direction = movement.normalize();
            double halfWidth = getDimensions(EntityPose.STANDING).width() * 0.5;
            for (double distance : new double[]{halfWidth + 0.22, halfWidth + 0.78}) {
                CeilingSupport ahead = findCeilingSupport(getPos().add(direction.multiply(distance)), stepHeight);
                if (ahead == null) {
                    continue;
                }
                if (selected == null || Math.abs(ahead.targetY() - selected.targetY()) > 0.08) {
                    selected = ahead;
                    break;
                }
            }
        }
        if (selected != null) {
            ceilingTargetY = selected.targetY();
            if (useGrace) {
                ceilingSupportTicks = CEILING_SUPPORT_GRACE;
            }
            return selected;
        }
        if (useGrace && ceilingSupportTicks > 0) {
            ceilingSupportTicks--;
        }
        return null;
    }

    private CeilingSupport findCeilingSupport(Vec3d sample, double stepHeight) {
        double assemblyHeight = getAssemblyHeight();
        double assemblyTop = getY() + assemblyHeight;
        Vec3d start = new Vec3d(sample.x, assemblyTop - stepHeight - 0.28, sample.z);
        Vec3d end = new Vec3d(sample.x, assemblyTop + stepHeight + 0.78, sample.z);
        BlockHitResult hit = getWorld().raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (hit.getType() == HitResult.Type.MISS || hit.getSide().getVector().getY() >= 0) {
            return null;
        }
        double targetY = hit.getPos().y - assemblyHeight - CEILING_CLEARANCE;
        if (Math.abs(targetY - getY()) > stepHeight + 0.12) {
            return null;
        }
        Vec3d target = new Vec3d(sample.x, targetY, sample.z);
        return canOccupy(target) ? new CeilingSupport(targetY, Vec3d.of(hit.getSide().getVector())) : null;
    }

    private Vec3d ceilingMovement(LivingEntity controller) {
        float frameYaw = controller.getYaw() + getFrameYaw();
        return horizontalFacing(frameYaw).multiply(controller.forwardSpeed)
                .add(rightFacing(frameYaw).multiply(controller.sidewaysSpeed * 0.72));
    }

    private BlockHitResult findWall(Vec3d origin, Vec3d direction, double reach) {
        Vec3d normalized = new Vec3d(direction.x, 0.0, direction.z);
        if (normalized.lengthSquared() < 1.0E-6) {
            normalized = horizontalFacing(getYaw());
        } else {
            normalized = normalized.normalize();
        }
        double assemblyHeight = getAssemblyHeight();
        double[] heights = {0.35, MathHelper.clamp(Config.uniqueEffects.soulstalker.riderHeight, 0.5, 4.0) * 0.72,
                assemblyHeight * 0.62, assemblyHeight - 0.55};
        BlockHitResult closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (double height : heights) {
            Vec3d start = origin.add(0.0, height, 0.0);
            BlockHitResult hit = getWorld().raycast(new RaycastContext(
                    start, start.add(normalized.multiply(reach)),
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
        return closest == null ? BlockHitResult.createMissed(origin.add(normalized),
                net.minecraft.util.math.Direction.UP, getBlockPos()) : closest;
    }

    private BlockHitResult findLeapWallContact(Vec3d movementStart, Vec3d incomingVelocity) {
        Vec3d horizontal = new Vec3d(incomingVelocity.x, 0.0, incomingVelocity.z);
        if (horizontal.lengthSquared() < 1.0E-6) {
            return null;
        }
        horizontal = horizontal.normalize();
        double halfWidth = getDimensions(EntityPose.STANDING).width() * 0.5;
        Vec3d side = new Vec3d(-horizontal.z, 0.0, horizontal.x).multiply(halfWidth * 0.72);
        Vec3d sweep = incomingVelocity.add(horizontal.multiply(halfWidth * 0.9 + 0.08));
        double assemblyHeight = getAssemblyHeight();
        double[] heights = {0.3, Math.min(1.45, assemblyHeight * 0.38),
                assemblyHeight * 0.66, assemblyHeight - 0.35};
        Vec3d[] offsets = {Vec3d.ZERO, side, side.multiply(-1.0)};
        BlockHitResult closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Vec3d offset : offsets) {
            for (double height : heights) {
                Vec3d start = movementStart.add(offset).add(0.0, height, 0.0);
                BlockHitResult hit = getWorld().raycast(new RaycastContext(
                        start, start.add(sweep), RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE, this));
                if (hit.getType() == HitResult.Type.MISS || hit.getSide().getVector().getY() != 0
                        || shouldIgnoreLeapSource(hit)) {
                    continue;
                }
                double distance = start.squaredDistanceTo(hit.getPos());
                if (distance < closestDistance) {
                    closest = hit;
                    closestDistance = distance;
                }
            }
        }
        if (closest == null && horizontalCollision) {
            BlockHitResult fallback = findWall(getPos(), horizontal,
                    halfWidth + Math.max(0.45, incomingVelocity.horizontalLength()));
            if (fallback.getType() != HitResult.Type.MISS && !shouldIgnoreLeapSource(fallback)) {
                closest = fallback;
            }
        }
        return closest;
    }

    private BlockHitResult findLeapCeilingContact(Vec3d movementStart, Vec3d incomingVelocity) {
        double halfWidth = getDimensions(EntityPose.STANDING).width() * 0.5;
        Vec3d horizontal = new Vec3d(incomingVelocity.x, 0.0, incomingVelocity.z);
        Vec3d forward = horizontal.lengthSquared() < 1.0E-6
                ? horizontalFacing(getYaw()) : horizontal.normalize();
        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);
        Vec3d[] offsets = {
                Vec3d.ZERO,
                forward.multiply(halfWidth * 0.7),
                side.multiply(halfWidth * 0.62),
                side.multiply(-halfWidth * 0.62)
        };
        Vec3d sweep = incomingVelocity.add(0.0, 0.38, 0.0);
        BlockHitResult closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Vec3d offset : offsets) {
            Vec3d start = movementStart.add(offset).add(0.0, getAssemblyHeight() - 0.24, 0.0);
            BlockHitResult hit = getWorld().raycast(new RaycastContext(
                    start, start.add(sweep), RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, this));
            if (hit.getType() == HitResult.Type.MISS || hit.getSide().getVector().getY() >= 0) {
                continue;
            }
            double distance = start.squaredDistanceTo(hit.getPos());
            if (distance < closestDistance) {
                closest = hit;
                closestDistance = distance;
            }
        }
        if (closest == null && verticalCollision) {
            BlockHitResult fallback = findCeiling();
            if (fallback.getType() != HitResult.Type.MISS && fallback.getSide().getVector().getY() < 0) {
                closest = fallback;
            }
        }
        return closest;
    }

    private BlockHitResult findLeapGroundContact() {
        double halfWidth = getDimensions(EntityPose.STANDING).width() * 0.5;
        double offset = Math.min(0.38, halfWidth * 0.68);
        Vec3d[] samples = {
                getPos(),
                getPos().add(offset, 0.0, 0.0),
                getPos().add(-offset, 0.0, 0.0),
                getPos().add(0.0, 0.0, offset),
                getPos().add(0.0, 0.0, -offset)
        };
        BlockHitResult closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Vec3d sample : samples) {
            Vec3d start = sample.add(0.0, 0.16, 0.0);
            BlockHitResult hit = getWorld().raycast(new RaycastContext(
                    start, sample.add(0.0, -0.28, 0.0),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            if (hit.getType() == HitResult.Type.MISS || hit.getSide().getVector().getY() <= 0) {
                continue;
            }
            double distance = start.squaredDistanceTo(hit.getPos());
            if (distance < closestDistance) {
                closest = hit;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private boolean shouldIgnoreLeapSource(BlockHitResult hit) {
        if (leapTravelDistance >= 0.9 || leapSourceNormal.lengthSquared() < 1.0E-6) {
            return false;
        }
        if (leapSourceBlock != null && leapSourceBlock.equals(hit.getBlockPos())) {
            return true;
        }
        Vec3d normal = Vec3d.of(hit.getSide().getVector());
        return normal.dotProduct(leapSourceNormal) > 0.95;
    }

    private BlockHitResult resolveWallFace(LivingEntity controller) {
        Vec3d normal = horizontalNormal(getSurfaceNormal(), horizontalFacing(getYaw()).multiply(-1.0));
        BlockHitResult current = findWall(getPos(), normal.multiply(-1.0), wallHoldReach());
        if (current.getType() != HitResult.Type.MISS) {
            return current;
        }
        Vec3d travel = wallTravelDirection(controller, normal);
        if (travel.lengthSquared() < 1.0E-6) {
            return current;
        }
        BlockHitResult inner = findWall(getPos(), travel, 0.95);
        if (inner.getType() != HitResult.Type.MISS) {
            return inner;
        }
        Vec3d aroundCorner = getPos().add(travel.multiply(0.85)).subtract(normal.multiply(0.85));
        BlockHitResult wrapped = findWall(aroundCorner, travel.multiply(-1.0), 1.2);
        return wrapped.getType() != HitResult.Type.MISS ? wrapped : current;
    }

    private Vec3d wallSideAxis(LivingEntity controller, Vec3d normal) {
        Vec3d side = normal.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 1.0E-6) {
            return Vec3d.ZERO;
        }
        side = side.normalize();
        double alignment = side.dotProduct(rightFacing(controller.getYaw()));
        if (Math.abs(alignment) > 0.15) {
            lateralSign = alignment < 0.0 ? -1 : 1;
        }
        return lateralSign < 0 ? side.multiply(-1.0) : side;
    }

    private Vec3d wallMotion(LivingEntity controller, Vec3d normal) {
        Vec3d side = wallSideAxis(controller, normal);
        Vec3d look = controller.getRotationVec(1.0F);
        double drive = Math.max(Math.abs(controller.forwardSpeed), Math.abs(controller.sidewaysSpeed));
        if (drive < 1.0E-4) {
            return Vec3d.ZERO;
        }
        double vertical = Math.abs(controller.forwardSpeed) > 0.05F
                ? controller.forwardSpeed : look.y * drive;
        double lateral = Math.abs(controller.sidewaysSpeed) > 0.05F
                ? controller.sidewaysSpeed : look.dotProduct(side) * drive;
        Vec3d motion = side.multiply(lateral).add(0.0, vertical, 0.0);
        double length = motion.length();
        return length > drive ? motion.multiply(drive / length) : motion;
    }

    private Vec3d wallTravelDirection(LivingEntity controller, Vec3d normal) {
        Vec3d motion = wallMotion(controller, normal);
        Vec3d travel = new Vec3d(motion.x, 0.0, motion.z);
        return travel.lengthSquared() < 1.0E-6 ? Vec3d.ZERO : travel.normalize();
    }

    private double overhangBypassDistance(Vec3d surfaceNormal) {
        Vec3d outward = horizontalNormal(surfaceNormal, horizontalFacing(getYaw()));
        for (double distance : new double[]{0.6, 1.05, 1.4, 1.65}) {
            if (canOccupy(getPos().add(outward.multiply(distance)).add(0.0, 0.8, 0.0))) {
                return distance;
            }
        }
        return -1.0;
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
            beginMantle(control, target, 5);
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
            beginMantle(control, target, 8);
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
        jumpQueued = false;
        byte mode = queuedLeapSurfaceMode == Byte.MIN_VALUE
                ? getSurfaceMode() : queuedLeapSurfaceMode;
        Vec3d sourceNormal = queuedLeapSurfaceNormal.lengthSquared() < 1.0E-6
                ? getSurfaceNormal() : queuedLeapSurfaceNormal;
        Vec3d look = queuedLeapLook.lengthSquared() < 1.0E-6
                ? controller.getRotationVec(1.0F) : queuedLeapLook;
        queuedLeapSurfaceMode = Byte.MIN_VALUE;
        queuedLeapSurfaceNormal = Vec3d.ZERO;
        queuedLeapLook = Vec3d.ZERO;
        if (!isAnchoredSurface(mode) && !nearGround) {
            jumpScale = 0.6F;
            return;
        }
        BlockHitResult sourceContact = mode == SURFACE_WALL
                ? findWall(getPos(), sourceNormal.multiply(-1.0), wallHoldReach())
                : mode == SURFACE_CEILING ? findCeiling() : null;
        Vec3d horizontal = new Vec3d(look.x, 0.0, look.z);
        if (horizontal.lengthSquared() < 1.0E-6) {
            horizontal = horizontalFacing(controller.getYaw());
        } else {
            horizontal = horizontal.normalize();
        }
        double pitchRetention = Math.max(0.65, Math.sqrt(Math.max(0.0, 1.0 - look.y * look.y)));
        double horizontalSpeed = Math.max(0.1, Config.uniqueEffects.soulstalker.leapHorizontalStrength)
                * jumpScale * pitchRetention;
        double verticalBase = Math.max(0.0, Config.uniqueEffects.soulstalker.leapVerticalStrength) * jumpScale;
        double verticalSpeed = mode == SURFACE_CEILING
                ? -Math.max(0.18, verticalBase * 0.48) + MathHelper.clamp(look.y * 0.18, -0.08, 0.08)
                : verticalBase + MathHelper.clamp(look.y * 0.45, -0.15, 0.35);
        Vec3d launch = horizontal.multiply(horizontalSpeed).add(0.0, verticalSpeed, 0.0);
        double releaseStrength = Math.max(0.0, Config.uniqueEffects.soulstalker.leapSurfaceReleaseStrength);
        if (mode == SURFACE_WALL) {
            double outwardSpeed = launch.dotProduct(sourceNormal);
            if (outwardSpeed < releaseStrength) {
                launch = launch.add(sourceNormal.multiply(releaseStrength - outwardSpeed));
            }
        } else if (mode == SURFACE_CEILING) {
            launch = launch.add(sourceNormal.multiply(releaseStrength));
        }
        if (controller instanceof ServerPlayerEntity) {
            serverLeapSyncVelocity = launch;
            setVelocity(launch);
            velocityModified = false;
            velocityDirty = false;
        } else {
            serverLeapSyncVelocity = Vec3d.ZERO;
            setVelocity(launch);
            velocityModified = true;
            velocityDirty = true;
        }
        leapGraceTicks = LEAP_RELEASE_GRACE;
        leapArcTicks = mode == SURFACE_CEILING ? 0 : LEAP_ARC_TICKS;
        dataTracker.set(LEAPING, true);
        leapDirection = launch.lengthSquared() < 1.0E-6 ? horizontal : launch.normalize();
        leapOrigin = getPos();
        leapLastObservedPosition = getPos();
        leapLastObservedMotion = Vec3d.ZERO;
        leapSourceNormal = mode == SURFACE_WALL || mode == SURFACE_CEILING ? sourceNormal : Vec3d.ZERO;
        leapSourceBlock = sourceContact != null && sourceContact.getType() != HitResult.Type.MISS
                ? sourceContact.getBlockPos().toImmutable() : null;
        leapTravelDistance = 0.0;
        leapDepartureConfirmed = false;
        leapTicks = 0;
        jumpScale = 0.6F;
        wallLostTicks = 0;
        pendingFootfalls.clear();
        forcedStepsRemaining = 0;
        clearMantle();
        clearStandoff();
        overhangBlocked = false;
        if (!getWorld().isClient() && getWorld() instanceof ServerWorld world) {
            reattachTicks = REATTACH_GRACE;
            setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
            dataTracker.set(LEAP_SEQUENCE, dataTracker.get(LEAP_SEQUENCE) + 1);
            if (controller instanceof ServerPlayerEntity player) {
                new SoulstalkerLeapLaunchPacket(getId(), mode, launch).sendTo(player);
            }
            world.spawnParticles(GLOAM_DUST, getX(), getBodyY(0.34), getZ(),
                    28, 0.78, 0.48, 0.78, 0.075);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, getX(), getBodyY(0.34), getZ(),
                    18, 0.62, 0.4, 0.62, 0.12);
            world.playSound(null, getBlockPos(), SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(),
                    SoundCategory.PLAYERS, 0.8F, 0.72F);
            world.playSound(null, getBlockPos(), SoundRegistry.ELEMENTAL_BOW_WIND_SHOOT_FLYBY_01.get(),
                    SoundCategory.PLAYERS, 0.38F, 1.28F);
        }
    }

    private void restoreServerLeapVelocity(LivingEntity controller) {
        if (getWorld().isClient() || !(controller instanceof ServerPlayerEntity)
                || !isLeapInProgress() || serverLeapSyncVelocity.lengthSquared() < 1.0E-6) {
            return;
        }
        setVelocity(serverLeapSyncVelocity);
        velocityModified = false;
        velocityDirty = false;
    }

    private void resolveLeapContact(ServerWorld world, LivingEntity owner,
                                    Vec3d movementStart, Vec3d observedMovement) {
        if (!isLeapInProgress()) {
            return;
        }
        leapTicks++;
        leapTravelDistance += observedMovement.length();
        if (observedMovement.lengthSquared() > 0.0025) {
            leapLastObservedMotion = observedMovement;
        }
        if (!leapDepartureConfirmed && (observedMovement.y > 0.01
                || getPos().squaredDistanceTo(leapOrigin) > 0.0225
                || leapTicks > 1 && findLeapGroundContact() == null)) {
            leapDepartureConfirmed = true;
        }
        Vec3d motion = observedMovement.lengthSquared() > 0.0025
                ? observedMovement : leapLastObservedMotion;
        if (motion.horizontalLengthSquared() > 0.01) {
            BlockHitResult wall = findLeapWallContact(movementStart, motion);
            if (wall != null) {
                Vec3d normal = Vec3d.of(wall.getSide().getVector());
                Vec3d approach = motion.normalize();
                if (approach.dotProduct(normal.multiply(-1.0)) >= WALL_LATCH_ALIGNMENT
                        && tryLatchSurface(owner, SURFACE_WALL, normal, motion, wall.getPos())) {
                    resolveLeapImpact(world, owner, wall.getPos(), normal, false);
                    return;
                }
            }
        }
        if (motion.y > 0.08) {
            BlockHitResult ceiling = findLeapCeilingContact(movementStart, motion);
            if (ceiling != null) {
                Vec3d normal = Vec3d.of(ceiling.getSide().getVector());
                Vec3d approach = motion.normalize();
                if (approach.dotProduct(normal.multiply(-1.0)) >= CEILING_LATCH_ALIGNMENT
                        && tryLatchSurface(owner, SURFACE_CEILING, normal, motion, ceiling.getPos())) {
                    resolveLeapImpact(world, owner, ceiling.getPos(), normal, false);
                    return;
                }
            }
        }
        BlockHitResult ground = findLeapGroundContact();
        if (leapDepartureConfirmed && ground != null && observedMovement.y <= 0.02) {
            double groundY = ground.getPos().y;
            Vec3d impact = new Vec3d(getX(), groundY, getZ());
            endLeapOnSurface(SURFACE_GROUND, new Vec3d(0.0, 1.0, 0.0), owner, motion);
            resolveLeapImpact(world, owner, impact, new Vec3d(0.0, 1.0, 0.0), true);
        }
    }

    private boolean tryLatchSurface(LivingEntity owner, byte mode, Vec3d normal,
                                    Vec3d incomingVelocity, Vec3d contact) {
        double halfWidth = getDimensions(EntityPose.STANDING).width() * 0.5;
        Vec3d position = mode == SURFACE_CEILING
                ? new Vec3d(getX(), contact.y - getAssemblyHeight() - 0.045, getZ())
                : new Vec3d(contact.x + normal.x * (halfWidth + 0.045), getY(),
                contact.z + normal.z * (halfWidth + 0.045));
        Vec3d resolved = canOccupy(position) ? position : null;
        if (resolved == null) {
            for (double distance : new double[]{0.08, 0.16, 0.26, 0.36}) {
                Vec3d candidate = position.add(normal.multiply(distance));
                if (canOccupy(candidate)) {
                    resolved = candidate;
                    break;
                }
            }
        }
        if (resolved == null) {
            return false;
        }
        setPosition(resolved);
        endLeapOnSurface(mode, normal, owner, incomingVelocity);
        if (mode == SURFACE_CEILING) {
            enterCeiling(owner, normal, incomingVelocity);
        }
        return true;
    }

    private void endLeapOnSurface(byte mode, Vec3d normal, LivingEntity owner, Vec3d incomingVelocity) {
        dataTracker.set(LEAPING, false);
        leapOrigin = Vec3d.ZERO;
        leapLastObservedPosition = null;
        leapLastObservedMotion = Vec3d.ZERO;
        serverLeapSyncVelocity = Vec3d.ZERO;
        leapSourceNormal = Vec3d.ZERO;
        leapSourceBlock = null;
        leapTravelDistance = 0.0;
        leapDepartureConfirmed = false;
        leapTicks = 0;
        reattachTicks = 0;
        leapGraceTicks = 0;
        leapArcTicks = 0;
        setVelocity(Vec3d.ZERO);
        velocityModified = true;
        velocityDirty = true;
        if (mode != SURFACE_CEILING) {
            setSurface(mode, normal);
        }
        pendingFootfalls.clear();
        forcedStepsRemaining = 0;
        clearMantle();
        clearStandoff();
        overhangBlocked = false;
        leapDirection = incomingVelocity.lengthSquared() < 1.0E-6
                ? owner.getRotationVec(1.0F) : incomingVelocity.normalize();
    }

    private void resolveLeapImpact(ServerWorld world, LivingEntity owner, Vec3d impact,
                                   Vec3d surfaceNormal, boolean createStain) {
        double radius = Math.max(0.25, Config.uniqueEffects.soulstalker.leapImpactRadius);
        float damage = Math.max(0.0F, (float) (HelperMethods.getEntityAttackDamage(owner)
                * Math.max(0.0, Config.uniqueEffects.soulstalker.leapImpactDamageScaling)));
        double knockback = Math.max(0.0, Config.uniqueEffects.soulstalker.leapImpactKnockback);
        double lift = Math.max(0.0, Config.uniqueEffects.soulstalker.leapImpactLift);
        Box search = Box.of(impact, radius * 2.0, radius * 2.0, radius * 2.0);
        DamageSource source = world.getDamageSources().indirectMagic(owner, owner);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkAbilityTarget(entity, owner)
                        && squaredDistanceToBox(impact, entity.getBoundingBox()) <= radius * radius)) {
            float resolvedDamage = HelperMethods.applyWeaponAbilityDamageToPlayersModifier(target, damage);
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(source, resolvedDamage));
            if (!damaged[0]) {
                continue;
            }
            Vec3d outward = target.getPos().subtract(impact).multiply(1.0, 0.0, 1.0);
            if (outward.horizontalLengthSquared() < 1.0E-4) {
                outward = new Vec3d(leapDirection.x, 0.0, leapDirection.z);
            }
            if (outward.horizontalLengthSquared() < 1.0E-4) {
                outward = horizontalFacing(owner.getYaw());
            }
            outward = outward.normalize();
            if (knockback > 0.0) {
                target.takeKnockback(knockback, -outward.x, -outward.z);
            }
            double resistance = MathHelper.clamp(
                    target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
            if (lift > 0.0 && resistance < 1.0) {
                target.addVelocity(0.0, lift * (1.0 - resistance), 0.0);
                target.velocityModified = true;
            }
        }
        cueVoice(SoulstalkerVoice.LEAP_ATTACK_LAND);
        if (createStain) {
            GloamStainManager.createPatch(world, owner.getUuid(), impact,
                    Math.max(0.25, Config.uniqueEffects.soulstalker.leapImpactStainRadius),
                    Math.max(20, Config.uniqueEffects.soulstalker.stainDuration),
                    Math.max(1, Config.uniqueEffects.soulstalker.stainFadeDuration),
                    Math.clamp(Config.uniqueEffects.soulstalker.stainSlowAmplifier, 0, 4));
        }
        double particleRadius = Math.max(0.45, radius * 0.42);
        Vec3d center = impact.add(surfaceNormal.multiply(0.12));
        world.spawnParticles(GLOAM_DUST, center.x, center.y, center.z,
                34, particleRadius, particleRadius * 0.72, particleRadius, 0.085);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                22, particleRadius * 0.82, particleRadius * 0.52, particleRadius * 0.82, 0.11);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z,
                9, particleRadius * 0.55, particleRadius * 0.42, particleRadius * 0.55, 0.045);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_WET_SPONGE_STEP,
                SoundCategory.PLAYERS, 1.0F, 0.52F + world.random.nextFloat() * 0.08F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_SLIME_SQUISH,
                SoundCategory.PLAYERS, 0.88F, 0.58F + world.random.nextFloat() * 0.08F);
        world.playSound(null, center.x, center.y, center.z, SoundRegistry.DARK_SWORD_ATTACK_03.get(),
                SoundCategory.PLAYERS, 0.42F, 0.62F + world.random.nextFloat() * 0.08F);
    }

    private static double squaredDistanceToBox(Vec3d point, Box box) {
        double x = MathHelper.clamp(point.x, box.minX, box.maxX) - point.x;
        double y = MathHelper.clamp(point.y, box.minY, box.maxY) - point.y;
        double z = MathHelper.clamp(point.z, box.minZ, box.maxZ) - point.z;
        return x * x + y * y + z * z;
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
                forcedStepDelay = 0;
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
                world, this, getPos(), rigYaw(controller),
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

    private static float rigYaw(LivingEntity controller) {
        return controller instanceof PlayerEntity ? controller.getYaw() : controller.getBodyYaw();
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
        if (mode != SURFACE_CEILING && dataTracker.get(SURFACE_FRAME_YAW) != 0.0F) {
            dataTracker.set(SURFACE_FRAME_YAW, 0.0F);
        }
        if (mode != SURFACE_CEILING) {
            dataTracker.set(CEILING_FORWARD_LOCKED, false);
            ceilingTargetY = Double.NaN;
            ceilingSupportTicks = 0;
        }
        dataTracker.set(SURFACE_MODE, mode);
        Vec3d resolved = normal.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 1.0, 0.0) : normal.normalize();
        dataTracker.set(SURFACE_NORMAL, resolved.toVector3f());
        setNoGravity(mode == SURFACE_WALL || mode == SURFACE_CEILING || mode == SURFACE_MANTLE);
    }

    private void enterCeiling(LivingEntity controller, Vec3d ceilingNormal, Vec3d continuation) {
        if (getSurfaceMode() != SURFACE_CEILING) {
            Vec3d heading = new Vec3d(continuation.x, 0.0, continuation.z);
            if (heading.lengthSquared() < 1.0E-6) {
                heading = horizontalFacing(controller.getYaw());
            }
            float headingYaw = (float) (MathHelper.atan2(-heading.x, heading.z) * MathHelper.DEGREES_PER_RADIAN);
            setSurface(SURFACE_CEILING, ceilingNormal);
            boolean forwardLocked = controller.forwardSpeed > 0.05F;
            dataTracker.set(CEILING_FORWARD_LOCKED, forwardLocked);
            dataTracker.set(SURFACE_FRAME_YAW, forwardLocked
                    ? MathHelper.wrapDegrees(headingYaw - controller.getYaw()) : 0.0F);
            return;
        }
        setSurface(SURFACE_CEILING, ceilingNormal);
    }

    private float getFrameYaw() {
        return dataTracker.get(SURFACE_FRAME_YAW);
    }

    private boolean isAdheredSurface() {
        if (leapGraceTicks > 0) {
            return false;
        }
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
        if (!getWorld().isClient() && getWorld() instanceof ServerWorld world) {
            tickVoiceSchedule(world);
        }
    }

    private void tickVoiceSchedule(ServerWorld world) {
        LivingEntity controller = getControllingPassenger();
        if (controller == null || !controller.isAlive()) {
            nextIdleVoiceTick = Long.MIN_VALUE;
            activeVoiceEndTick = Long.MIN_VALUE;
            lastIdleVoiceIndex = -1;
            voiceLeapWasActive = false;
            return;
        }

        long now = world.getTime();
        if (nextIdleVoiceTick == Long.MIN_VALUE) {
            nextIdleVoiceTick = now + idleInitialDelay(world);
        }
        if (isLeapInProgress()) {
            voiceLeapWasActive = true;
            return;
        }
        if (voiceLeapWasActive) {
            voiceLeapWasActive = false;
            nextIdleVoiceTick = Math.max(now, activeVoiceEndTick) + idleInitialDelay(world);
            return;
        }
        if (now < activeVoiceEndTick || now < nextIdleVoiceTick) {
            return;
        }

        SoulstalkerVoice voice = SoulstalkerVoice.randomIdleDifferent(world.random, lastIdleVoiceIndex);
        lastIdleVoiceIndex = voice.ordinal();
        cueVoice(voice);
        nextIdleVoiceTick = activeVoiceEndTick + idleGap(world);
    }

    private void cueVoice(SoulstalkerVoice voice) {
        dataTracker.set(VOICE_INDEX, voice.ordinal());
        dataTracker.set(VOICE_SEQUENCE, getVoiceSequence() + 1);
        activeVoiceEndTick = getWorld().getTime() + voice.getDurationTicks();
    }

    private static int idleInitialDelay(ServerWorld world) {
        return IDLE_INITIAL_DELAY_MIN + world.random.nextInt(IDLE_INITIAL_DELAY_VARIANCE);
    }

    private static int idleGap(ServerWorld world) {
        return IDLE_GAP_MIN + world.random.nextInt(IDLE_GAP_VARIANCE);
    }

    @Override
    public boolean isClimbing() {
        if (leapGraceTicks > 0) {
            return false;
        }
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

    public boolean isLeapInProgress() {
        return dataTracker.get(LEAPING);
    }

    public int getVoiceIndex() {
        return dataTracker.get(VOICE_INDEX);
    }

    public int getVoiceSequence() {
        return dataTracker.get(VOICE_SEQUENCE);
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
    }

    @Override
    public boolean canJump() {
        return isAnchoredSurface(getSurfaceMode()) || nearGround;
    }

    @Override
    public void startJumping(int height) {
        if (!getWorld().isClient()) {
            queueLeap(height);
        }
    }

    public void applyNetworkLeap(byte sourceMode, Vec3d launch) {
        if (!getWorld().isClient() || !Double.isFinite(launch.x)
                || !Double.isFinite(launch.y) || !Double.isFinite(launch.z)) {
            return;
        }
        setSurface(SURFACE_AIRBORNE, new Vec3d(0.0, 1.0, 0.0));
        leapGraceTicks = LEAP_RELEASE_GRACE;
        leapArcTicks = sourceMode == SURFACE_CEILING ? 0 : LEAP_ARC_TICKS;
        dataTracker.set(LEAPING, true);
        setVelocity(launch);
    }

    private void queueLeap(int strength) {
        jumpScale = resolveJumpScale(strength);
        queuedLeapSurfaceMode = getSurfaceMode();
        queuedLeapSurfaceNormal = getSurfaceNormal();
        LivingEntity controller = getControllingPassenger();
        queuedLeapLook = controller == null ? Vec3d.ZERO : controller.getRotationVec(1.0F);
        jumpQueued = true;
    }

    private static float resolveJumpScale(int strength) {
        return 0.6F + 0.4F * MathHelper.clamp(strength, 0, 100) / 100.0F;
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

    private record CeilingSupport(double targetY, Vec3d normal) {
    }
}
