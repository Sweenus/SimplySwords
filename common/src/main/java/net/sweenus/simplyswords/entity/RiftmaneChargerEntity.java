package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RiftmaneChargerEntity extends HorseEntity implements SimplySwordsMinion {

    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(RiftmaneChargerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> REAR_TICKS =
            DataTracker.registerData(RiftmaneChargerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(RiftmaneChargerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> WATER_WALK =
            DataTracker.registerData(RiftmaneChargerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final float REAR_RISE_FRACTION = 0.45F;
    private static final float REAR_DROP_TICKS = 4.0F;
    private static final double WALL_HEIGHT = 2.0;
    private static final double PROBE_RADIUS = 0.35;
    private static final int GALLOP_INTERVAL = 16;
    private static final int HIT_SOUND_INTERVAL = 4;
    private static final DustColorTransitionParticleEffect RIFT_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.05F, 0.72F, 0.70F),
                    new Vector3f(0.68F, 0.99F, 0.96F), 1.1F);

    private UUID ownerUuid;
    private boolean audioLead;
    private int lastHitSoundAge = Integer.MIN_VALUE;
    private long expiresAtTick;
    private float damage;
    private double knockbackStrength;
    private double chargeSpeed;
    private double hitRadius = 1.1;
    private Vec3d direction = Vec3d.ZERO;
    private ItemStack sourceStack = ItemStack.EMPTY;
    private final Set<UUID> hitTargets = new HashSet<>();

    public RiftmaneChargerEntity(EntityType<? extends HorseEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setSilent(true);
    }

    public static DefaultAttributeContainer.Builder createChargerAttributes() {
        return AbstractHorseEntity.createBaseHorseAttributes()
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(LIFETIME, 60);
        this.dataTracker.startTracking(REAR_TICKS, 0);
        this.dataTracker.startTracking(SEED, 0);
        this.dataTracker.startTracking(WATER_WALK, false);
    }

    public void setAudioLead(boolean audioLead) {
        this.audioLead = audioLead;
    }

    public boolean getWaterWalk() {
        return this.dataTracker.get(WATER_WALK);
    }

    public void setWaterWalk(boolean waterWalk) {
        this.dataTracker.set(WATER_WALK, waterWalk);
    }

    @Override
    public boolean canWalkOnFluid(FluidState state) {
        return this.getWaterWalk() && state.isIn(FluidTags.WATER);
    }

    @Override
    public boolean shouldDismountUnderwater() {
        return false;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.hasPassengers() && this.ownerUuid != null
                && this.ownerUuid.equals(passenger.getUuid());
    }

    @Override
    public double getMountedHeightOffset() {
        return super.getMountedHeightOffset() + 0.45 * this.getRearProgress(1.0F);
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        float rear = this.getRearProgress(1.0F);
        Vec3d facing = Vec3d.fromPolar(0.0F, this.getYaw());
        double y = this.getY() + this.getMountedHeightOffset() + passenger.getHeightOffset();
        positionUpdater.accept(passenger,
                this.getX() - facing.x * 0.35 * rear, y, this.getZ() - facing.z * 0.35 * rear);
    }

    public void initializeCharge(LivingEntity owner, ItemStack stack, Vec3d direction, int lifetime,
                                 int rearTicks, int seed, float damage, double knockbackStrength,
                                 double chargeSpeed, double hitRadius, double stepHeight) {
        this.ownerUuid = owner.getUuid();
        this.sourceStack = stack.copy();
        this.direction = direction.horizontalLengthSquared() > 1.0E-4
                ? new Vec3d(direction.x, 0.0, direction.z).normalize()
                : Vec3d.fromPolar(0.0F, owner.getYaw());
        this.expiresAtTick = this.getWorld().getTime() + Math.max(1, lifetime);
        this.damage = Math.max(0.0F, damage);
        this.knockbackStrength = Math.max(0.0, knockbackStrength);
        this.chargeSpeed = Math.max(0.0, chargeSpeed);
        this.hitRadius = Math.max(0.25, hitRadius);
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
        this.dataTracker.set(REAR_TICKS, Math.max(0, rearTicks));
        this.dataTracker.set(SEED, seed);
        this.setAiDisabled(true);
        this.setPersistent();
        this.setStepHeight((float) MathHelper.clamp(stepHeight, 0.0, 2.0));

        float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(-this.direction.x, this.direction.z)));
        this.setYaw(yaw);
        this.setBodyYaw(yaw);
        this.setHeadYaw(yaw);
        this.prevYaw = yaw;
        this.prevBodyYaw = yaw;
        this.prevHeadYaw = yaw;
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public int getRearTicks() {
        return this.dataTracker.get(REAR_TICKS);
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    public boolean isRearing(float tickDelta) {
        return this.age + tickDelta < this.getRearTicks();
    }

    public float getRearProgress(float tickDelta) {
        int rear = this.getRearTicks();
        if (rear <= 0) {
            return 0.0F;
        }
        float age = this.age + tickDelta;
        if (age <= rear) {
            return MathHelper.clamp(age / Math.max(1.0F, rear * REAR_RISE_FRACTION), 0.0F, 1.0F);
        }
        return MathHelper.clamp(1.0F - (age - rear) / REAR_DROP_TICKS, 0.0F, 1.0F);
    }

    public float getSlamProgress(float tickDelta) {
        int rear = this.getRearTicks();
        if (rear <= 0) {
            return 1.0F;
        }
        return MathHelper.clamp((this.age + tickDelta - rear) / 12.0F, 0.0F, 1.0F);
    }

    @Override
    public float getAngryAnimationProgress(float tickDelta) {
        return this.getRearProgress(tickDelta);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld world) || world.getTime() > this.expiresAtTick) {
            expire(false);
            return;
        }

        if (this.isOnFire()) {
            this.extinguish();
        }
        this.fallDistance = 0.0F;
        for (Entity passenger : this.getPassengerList()) {
            passenger.fallDistance = 0.0F;
        }

        int rearTicks = this.getRearTicks();
        if (this.age < rearTicks) {
            tickRear(world, rearTicks);
            return;
        }
        if (this.age == rearTicks) {
            slam(world);
        }

        if (blockedByWall(this.direction)) {
            expire(true);
            return;
        }

        Vec3d velocity = new Vec3d(this.direction.x * this.chargeSpeed, this.getVelocity().y,
                this.direction.z * this.chargeSpeed);
        this.setVelocity(velocity);
        this.velocityModified = true;
        this.move(MovementType.SELF, velocity);

        LivingEntity owner = getOwner(world);
        Box searchBox = this.getBoundingBox().expand(this.hitRadius);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox, this::isValidTarget)) {
            if (!this.hitTargets.add(target.getUuid())) {
                continue;
            }
            boolean damaged = owner != null
                    && SimplySwordsAPI.applyDelegatedWeaponHit(this.sourceStack, target, owner, this, this.damage);
            if (damaged) {
                target.takeKnockback(this.knockbackStrength, this.getX() - target.getX(), this.getZ() - target.getZ());
                Vec3d hitPos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
                world.spawnParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 8, 0.24, 0.2, 0.24, 0.06);
                world.spawnParticles(RIFT_DUST, hitPos.x, hitPos.y, hitPos.z, 10, 0.3, 0.25, 0.3, 0.02);
                if (this.age - this.lastHitSoundAge >= HIT_SOUND_INTERVAL) {
                    this.lastHitSoundAge = this.age;
                    world.playSound(null, target.getBlockPos(), SoundRegistry.OBJECT_IMPACT_THUD.get(),
                            SoundCategory.PLAYERS, 0.32F, 1.25F + world.random.nextFloat() * 0.2F);
                }
            }
        }

        spawnTrail(world);
    }

    private void tickRear(ServerWorld world, int rearTicks) {
        Vec3d settle = new Vec3d(0.0, this.getVelocity().y, 0.0);
        this.setVelocity(settle);
        this.velocityModified = true;
        this.move(MovementType.SELF, settle);

        double emergeY = this.getY() + 0.35 + this.age * 0.06;
        world.spawnParticles(RIFT_DUST, this.getX(), emergeY, this.getZ(), 5, 0.45, 0.55, 0.45, 0.03);
        if (this.age % 2 == 0) {
            world.spawnParticles(ParticleTypes.END_ROD, this.getX(), emergeY, this.getZ(),
                    2, 0.4, 0.6, 0.4, 0.02);
        }
        if (!this.audioLead) {
            return;
        }
        if (this.age == Math.max(1, rearTicks / 3)) {
            world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_HORSE_ANGRY,
                    SoundCategory.PLAYERS, 0.55F, 0.72F + world.random.nextFloat() * 0.25F);
        }
        if (this.age == Math.max(2, rearTicks - 6)) {
            world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE,
                    SoundCategory.PLAYERS, 0.35F, 1.5F);
        }
    }

    private void slam(ServerWorld world) {
        this.setVelocity(this.getVelocity().x, -0.45, this.getVelocity().z);
        this.velocityModified = true;

        Vec3d hooves = this.getPos().add(this.direction.multiply(this.getWidth() * 0.5));
        for (int step = 0; step < 18; step++) {
            double angle = MathHelper.TAU * step / 18.0;
            double radius = 0.9;
            world.spawnParticles(RIFT_DUST,
                    hooves.x + Math.cos(angle) * radius, hooves.y + 0.1, hooves.z + Math.sin(angle) * radius,
                    1, 0.06, 0.02, 0.06, 0.02);
        }
        world.spawnParticles(ParticleTypes.CRIT, hooves.x, hooves.y + 0.15, hooves.z,
                14, 0.5, 0.1, 0.5, 0.12);
        world.spawnParticles(ParticleTypes.POOF, hooves.x, hooves.y + 0.1, hooves.z,
                8, 0.5, 0.05, 0.5, 0.02);
        if (!this.audioLead) {
            return;
        }
        world.playSound(null, this.getBlockPos(), SoundRegistry.OBJECT_IMPACT_THUD.get(),
                SoundCategory.PLAYERS, 0.55F, 0.85F + world.random.nextFloat() * 0.15F);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                SoundCategory.PLAYERS, 0.3F, 1.6F);
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.getWorld().isClient()) {
            super.travel(movementInput);
            return;
        }
        Vec3d velocity = this.getVelocity();
        this.setVelocity(velocity.x, this.hasNoGravity() ? velocity.y : (velocity.y - 0.08) * 0.98, velocity.z);
    }

    private boolean blockedByWall(Vec3d travel) {
        double stepHeight = Math.max(0.0, this.getStepHeight());
        double clearance = this.getY() + stepHeight + 0.08;
        double ceiling = this.getY() + WALL_HEIGHT;
        if (clearance >= ceiling) {
            return false;
        }
        double halfWidth = this.getWidth() * 0.5;
        Vec3d ahead = this.getPos().add(travel.multiply(halfWidth + 0.45));
        Box probe = new Box(ahead.x - PROBE_RADIUS, clearance, ahead.z - PROBE_RADIUS,
                ahead.x + PROBE_RADIUS, ceiling, ahead.z + PROBE_RADIUS);
        for (VoxelShape shape : this.getWorld().getBlockCollisions(null, probe)) {
            if (!shape.isEmpty() && shape.getBoundingBox().maxY > clearance) {
                return true;
            }
        }
        return false;
    }

    private void spawnTrail(ServerWorld world) {
        world.spawnParticles(RIFT_DUST, this.getX(), this.getY() + 0.15, this.getZ(),
                3, 0.32, 0.1, 0.32, 0.01);
        if (this.age % 3 == 0) {
            world.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.1, this.getZ(),
                    1, 0.3, 0.05, 0.3, 0.005);
        }
        if (Math.floorMod(this.age + this.getSeed(), GALLOP_INTERVAL) == 0) {
            world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_HORSE_GALLOP,
                    SoundCategory.PLAYERS, 0.18F, 0.7F + world.random.nextFloat() * 0.2F);
        }
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target == this) {
            return false;
        }
        if (target instanceof RiftmaneChargerEntity || this.hasPassenger(target)) {
            return false;
        }
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        LivingEntity owner = getOwner(world);
        if (owner == null || target == owner) {
            return false;
        }
        return HelperMethods.checkFriendlyFire(target, owner);
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Nullable
    private LivingEntity getOwner(ServerWorld world) {
        if (this.ownerUuid == null) {
            return null;
        }
        Entity entity = world.getEntity(this.ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void releaseRiders(ServerWorld world) {
        Vec3d release = this.getPos().add(0.0, 0.1, 0.0);
        for (Entity passenger : this.getPassengerList()) {
            if (!(passenger instanceof LivingEntity rider)) {
                continue;
            }
            rider.stopRiding();
            Box targetBox = rider.getBoundingBox().offset(release.subtract(rider.getPos()));
            if (world.isSpaceEmpty(rider, targetBox)) {
                rider.refreshPositionAfterTeleport(release);
            }
            rider.fallDistance = 0.0F;
            rider.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOW_FALLING, 40, 0, false, false, false), rider);
            world.playSound(null, rider.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                    SoundCategory.PLAYERS, 0.5F, 1.35F);
        }
        this.removeAllPassengers();
    }

    private void expire(boolean impact) {
        if (this.getWorld() instanceof ServerWorld world) {
            releaseRiders(world);
            world.spawnParticles(ParticleTypes.POOF, this.getX(), this.getBodyY(0.5), this.getZ(),
                    impact ? 18 : 10, 0.35, 0.35, 0.35, 0.05);
            world.spawnParticles(RIFT_DUST, this.getX(), this.getBodyY(0.6), this.getZ(),
                    impact ? 24 : 12, 0.4, 0.4, 0.4, 0.03);
            if (impact && this.audioLead) {
                world.playSound(null, this.getBlockPos(), SoundRegistry.OBJECT_IMPACT_THUD.get(),
                        SoundCategory.PLAYERS, 0.5F, 0.8F);
            }
        }
        this.discard();
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
    protected void pushAway(Entity entity) {
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean causedByPlayer) {
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return ActionResult.FAIL;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    @Nullable
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }
}
