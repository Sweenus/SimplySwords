package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.UUID;

public class DawnquiverArrowEntity extends Entity {

    private static final TrackedData<Integer> OWNER_ID =
            DataTracker.registerData(DawnquiverArrowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SCALE =
            DataTracker.registerData(DawnquiverArrowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(DawnquiverArrowEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final DustColorTransitionParticleEffect DAWN_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(1.0F, 0.85F, 0.35F),
                    new Vector3f(1.0F, 0.99F, 0.86F), 1.2F);

    private static final int MAX_AGE = 120;

    private UUID ownerUuid;
    private UUID targetUuid;
    private ItemStack sourceStack = ItemStack.EMPTY;
    private float damage;
    private double speed = 1.2;
    private double turnRateDegrees = 9.0;
    private double impactRadius = 2.0;
    private double maxDistance = 64.0;
    private double traveled;

    public DawnquiverArrowEntity(EntityType<? extends DawnquiverArrowEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public DawnquiverArrowEntity(ServerWorld world, LivingEntity owner, ItemStack stack, Vec3d origin,
                                 Vec3d direction, @Nullable LivingEntity target, float damage,
                                 double speed, double turnRateDegrees, double scale,
                                 double impactRadius, double maxDistance) {
        this(EntityRegistry.DAWNQUIVER_ARROW.get(), world);
        this.ownerUuid = owner.getUuid();
        this.sourceStack = stack.copy();
        this.damage = Math.max(0.0F, damage);
        this.speed = Math.max(0.1, speed);
        this.turnRateDegrees = Math.max(0.0, turnRateDegrees);
        this.impactRadius = Math.max(0.0, impactRadius);
        this.maxDistance = Math.max(4.0, maxDistance);
        this.targetUuid = target == null ? null : target.getUuid();
        this.dataTracker.set(OWNER_ID, owner.getId());
        this.dataTracker.set(SCALE, (float) Math.max(0.2, scale));
        this.dataTracker.set(SEED, owner.getRandom().nextInt(4096));
        this.setPosition(origin.x, origin.y, origin.z);
        this.setVelocity(direction.normalize().multiply(this.speed));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ID, -1);
        builder.add(SCALE, 1.0F);
        builder.add(SEED, 0);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (this.getWorld().isClient()) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }

        LivingEntity owner = resolveOwner(world);
        if (owner == null || this.age > MAX_AGE || this.traveled > this.maxDistance) {
            dissipate(world);
            return;
        }

        Vec3d current = this.getPos();
        Vec3d velocity = this.getVelocity();
        LivingEntity target = resolveTarget(world, owner);
        if (target != null && velocity.lengthSquared() > 1.0E-6) {
            Vec3d desired = aimPoint(target).subtract(current);
            if (desired.lengthSquared() > 1.0E-6) {
                Vec3d steered = turnToward(velocity.normalize(), desired.normalize(),
                        Math.toRadians(this.turnRateDegrees));
                velocity = steered.multiply(this.speed);
                this.setVelocity(velocity);
            }
        }

        Vec3d next = current.add(velocity);
        BlockHitResult blockHit = world.raycast(new RaycastContext(current, next,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        Vec3d segmentEnd = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getPos();

        LivingEntity struck = findCollisionTarget(world, owner, current, segmentEnd);
        if (struck != null) {
            this.setPosition(segmentEnd.x, segmentEnd.y, segmentEnd.z);
            impact(world, owner, struck);
            return;
        }

        this.setPosition(segmentEnd.x, segmentEnd.y, segmentEnd.z);
        this.traveled += current.distanceTo(segmentEnd);
        spawnTrail(world, current, segmentEnd);

        if (blockHit.getType() != HitResult.Type.MISS) {
            impact(world, owner, null);
        }
    }

    private void impact(ServerWorld world, LivingEntity owner, @Nullable LivingEntity struck) {
        float scale = getScale();
        if (struck != null) {
            SimplySwordsAPI.applyEntityWeaponHit(this.sourceStack, struck, owner, this.damage);
        }
        if (this.impactRadius > 0.0) {
            Box splash = this.getBoundingBox().expand(this.impactRadius);
            for (LivingEntity nearby : world.getEntitiesByClass(LivingEntity.class, splash,
                    entity -> entity != struck && isValidTarget(entity, owner))) {
                SimplySwordsAPI.applyEntityWeaponHit(this.sourceStack, nearby, owner, this.damage * 0.4F);
            }
        }

        world.spawnParticles(DAWN_DUST, this.getX(), this.getY(), this.getZ(),
                Math.round(40 * scale), 0.55 * scale, 0.55 * scale, 0.55 * scale, 0.09);
        world.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(),
                Math.round(18 * scale), 0.4 * scale, 0.4 * scale, 0.4 * scale, 0.12);
        world.spawnParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnEntity(new DawnquiverImpactVisualEntity(world,
                this.getX(), this.getY(), this.getZ(), scale, this.getSeed()));
        world.playSound(null, this.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_02.get(),
                SoundCategory.PLAYERS, 0.6F * scale, 1.0F + world.random.nextFloat() * 0.15F);
        this.discard();
    }

    private void dissipate(ServerWorld world) {
        world.spawnParticles(DAWN_DUST, this.getX(), this.getY(), this.getZ(),
                10, 0.3, 0.3, 0.3, 0.03);
        this.discard();
    }

    private void spawnTrail(ServerWorld world, Vec3d from, Vec3d to) {
        float scale = getScale();
        world.spawnParticles(DAWN_DUST, to.x, to.y, to.z,
                Math.round(3 * scale), 0.08, 0.08, 0.08, 0.01);
        if (this.age % 2 == 0) {
            world.spawnParticles(ParticleTypes.END_ROD, from.x, from.y, from.z,
                    1, 0.05, 0.05, 0.05, 0.005);
        }
    }

    private LivingEntity findCollisionTarget(ServerWorld world, LivingEntity owner, Vec3d start, Vec3d end) {
        double grace = 0.35 * getScale();
        Box search = new Box(start, end).expand(grace + 0.55);
        return world.getEntitiesByClass(LivingEntity.class, search,
                        entity -> isValidTarget(entity, owner)
                                && entity.getBoundingBox().expand(entity.getTargetingMargin() + grace)
                                .raycast(start, end).isPresent())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(start)))
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity entity, LivingEntity owner) {
        return entity != owner && entity.isAlive() && !entity.isRemoved()
                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                && HelperMethods.checkAbilityTarget(entity, owner);
    }

    private static Vec3d aimPoint(LivingEntity target) {
        return target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
    }

    private static Vec3d turnToward(Vec3d current, Vec3d desired, double maximumAngle) {
        double dot = MathHelper.clamp(current.dotProduct(desired), -1.0, 1.0);
        double angle = Math.acos(dot);
        if (angle <= maximumAngle || angle < 1.0E-5) {
            return desired;
        }
        double progress = maximumAngle / angle;
        return current.multiply(1.0 - progress).add(desired.multiply(progress)).normalize();
    }

    @Nullable
    private LivingEntity resolveTarget(ServerWorld world, LivingEntity owner) {
        if (this.targetUuid == null) {
            return null;
        }
        Entity entity = world.getEntity(this.targetUuid);
        return entity instanceof LivingEntity living && isValidTarget(living, owner) ? living : null;
    }

    @Nullable
    private LivingEntity resolveOwner(ServerWorld world) {
        if (this.ownerUuid == null) {
            return null;
        }
        Entity entity = world.getEntity(this.ownerUuid);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
    }
}
