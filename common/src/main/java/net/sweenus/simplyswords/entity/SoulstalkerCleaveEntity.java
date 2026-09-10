package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.SoulstalkerAbilityManager;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SoulstalkerCleaveEntity extends Entity {
    private static final TrackedData<Integer> OWNER_ID =
            DataTracker.registerData(SoulstalkerCleaveEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<UUID>> OWNER_UUID =
            DataTracker.registerData(SoulstalkerCleaveEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<ItemStack> WEAPON_STACK =
            DataTracker.registerData(SoulstalkerCleaveEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Vector3f> DIRECTION =
            DataTracker.registerData(SoulstalkerCleaveEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Float> INITIAL_WIDTH =
            DataTracker.registerData(SoulstalkerCleaveEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> FINAL_WIDTH =
            DataTracker.registerData(SoulstalkerCleaveEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> PROGRESS =
            DataTracker.registerData(SoulstalkerCleaveEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(SoulstalkerCleaveEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final double CRESCENT_LATERAL_SCALE = 0.71;
    private static final double CRESCENT_VERTICAL_SCALE = 0.22;
    private static final double CRESCENT_MINIMUM_VERTICAL = 0.75;
    private static final DustColorTransitionParticleEffect GLOAM_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.025F, 0.008F, 0.07F),
                    new Vector3f(0.48F, 0.08F, 0.82F), 1.25F);

    private final Set<UUID> attemptedTargets = new HashSet<>();
    private double maxDistance = 10.0;
    private double traveled;
    private float weaponDamage;
    private int targetCap = Integer.MAX_VALUE;
    private int victims;
    private java.util.UUID strideId;
    private java.util.UUID trackedOwnerId;
    private boolean released;

    public SoulstalkerCleaveEntity(EntityType<? extends SoulstalkerCleaveEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
    }

    public SoulstalkerCleaveEntity(ServerWorld world, LivingEntity owner, ItemStack stack,
                                   Vec3d origin, Vec3d direction, double distance, double speed,
                                   float damage, float initialWidth, float finalWidth,
                                   int targetCap, java.util.UUID strideId) {
        this(EntityRegistry.SOULSTALKER_CLEAVE.get(), world);
        Vec3d normalized = direction.lengthSquared() < 1.0E-6
                ? Vec3d.fromPolar(owner.getPitch(), owner.getYaw()) : direction.normalize();
        dataTracker.set(OWNER_ID, owner.getId());
        dataTracker.set(OWNER_UUID, Optional.of(owner.getUuid()));
        dataTracker.set(WEAPON_STACK, stack.copy());
        CombatProvenanceApi.attach(this, stack);
        dataTracker.set(DIRECTION, normalized.toVector3f());
        dataTracker.set(INITIAL_WIDTH, Math.max(0.25F, initialWidth));
        dataTracker.set(FINAL_WIDTH, Math.max(0.25F, finalWidth));
        dataTracker.set(SEED, world.random.nextInt());
        maxDistance = Math.max(1.0, distance);
        weaponDamage = Math.max(0.0F, damage);
        this.targetCap = Math.max(1, targetCap);
        this.strideId = strideId;
        this.trackedOwnerId = owner.getUuid();
        SoulstalkerAbilityManager.retainStride(world, trackedOwnerId);
        setPosition(origin);
        setVelocity(normalized.multiply(Math.max(0.05, speed)));
        setYaw(owner.getYaw());
        setPitch(owner.getPitch());
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ID, -1);
        builder.add(OWNER_UUID, Optional.empty());
        builder.add(WEAPON_STACK, ItemStack.EMPTY);
        builder.add(DIRECTION, new Vector3f(0.0F, 0.0F, 1.0F));
        builder.add(INITIAL_WIDTH, 1.1F);
        builder.add(FINAL_WIDTH, 3.2F);
        builder.add(PROGRESS, 0.0F);
        builder.add(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient() || !(getWorld() instanceof ServerWorld world)) {
            return;
        }
        LivingEntity owner = resolveOwner(world);
        Vec3d velocity = getVelocity();
        if (owner == null || velocity.lengthSquared() < 1.0E-6 || age > 80) {
            discard();
            return;
        }
        double remaining = maxDistance - traveled;
        if (remaining <= 0.0) {
            discard();
            return;
        }
        Vec3d current = getPos();
        double stepLength = velocity.length();
        Vec3d next = current.add(stepLength > remaining
                ? velocity.multiply(remaining / stepLength) : velocity);
        BlockHitResult blockHit = world.raycast(new RaycastContext(current, next,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        Vec3d segmentEnd = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getPos();
        double nextTraveled = traveled + current.distanceTo(segmentEnd);
        float progress = MathHelper.clamp((float) (nextTraveled / maxDistance), 0.0F, 1.0F);
        dataTracker.set(PROGRESS, progress);
        damageTargets(world, owner, current, segmentEnd, getCurrentWidth());
        setPosition(segmentEnd);
        traveled = nextTraveled;
        if ((age & 1) == 0) {
            Vec3d middle = current.lerp(segmentEnd, 0.5);
            world.spawnParticles(GLOAM_DUST, middle.x, middle.y, middle.z,
                    3, getCurrentWidth() * 0.12, getCurrentWidth() * 0.08,
                    getCurrentWidth() * 0.12, 0.01);
        }
        if (blockHit.getType() != HitResult.Type.MISS || traveled >= maxDistance) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, segmentEnd.x, segmentEnd.y, segmentEnd.z,
                    8, 0.2, 0.2, 0.2, 0.03);
            discard();
        }
    }

    private void damageTargets(ServerWorld world, LivingEntity owner, Vec3d start, Vec3d end, float width) {
        double lateral = Math.max(0.2, width * CRESCENT_LATERAL_SCALE);
        double vertical = Math.max(CRESCENT_MINIMUM_VERTICAL, width * CRESCENT_VERTICAL_SCALE);
        Box search = new Box(start, end).expand(Math.max(lateral, vertical));
        ItemStack stack = dataTracker.get(WEAPON_STACK);
        if (stack.isEmpty() || victims >= targetCap) {
            return;
        }
        List<LivingEntity> candidates = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkAbilityTarget(entity, owner)
                        && !attemptedTargets.contains(entity.getUuid())
                        && withinSweep(start, end, entity.getBoundingBox(), lateral, vertical)));
        candidates.sort(Comparator.comparingDouble(
                entity -> travelOrder(start, end, entity.getBoundingBox())));
        for (LivingEntity target : candidates) {
            if (victims >= targetCap) {
                break;
            }
            if (!attemptedTargets.add(target.getUuid())) {
                continue;
            }
            if (SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, weaponDamage)) {
                victims++;
                SoulstalkerAbilityManager.reportStrideHit(world, owner, target, weaponDamage);
                if (strideId != null
                        && world.getEntity(strideId) instanceof SoulstalkerStrideEntity stride) {
                    stride.addCleaveHitBonus();
                }
                world.spawnParticles(GLOAM_DUST, target.getX(), target.getBodyY(0.58), target.getZ(),
                        12, 0.32, 0.26, 0.32, 0.04);
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getBodyY(0.58), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                world.playSound(null, target.getBlockPos(), SoundRegistry.DARK_SWORD_ATTACK_03.get(),
                        SoundCategory.PLAYERS, 0.48F, 0.82F + world.random.nextFloat() * 0.12F);
            }
        }
        dataTracker.set(WEAPON_STACK, stack);
    }

    private static double travelOrder(Vec3d start, Vec3d end, Box box) {
        Vec3d delta = end.subtract(start);
        double lengthSquared = delta.lengthSquared();
        if (lengthSquared < 1.0E-9) {
            return 0.0;
        }
        return MathHelper.clamp(box.getCenter().subtract(start).dotProduct(delta) / lengthSquared, 0.0, 1.0);
    }

    private static boolean withinSweep(Vec3d start, Vec3d end, Box box,
                                       double lateral, double vertical) {
        Vec3d closest = start.add(end.subtract(start).multiply(travelOrder(start, end, box)));
        double x = MathHelper.clamp(closest.x, box.minX, box.maxX) - closest.x;
        double y = MathHelper.clamp(closest.y, box.minY, box.maxY) - closest.y;
        double z = MathHelper.clamp(closest.z, box.minZ, box.maxZ) - closest.z;
        return (x * x + z * z) / (lateral * lateral) + y * y / (vertical * vertical) <= 1.0;
    }

    public void releaseTracking() {
        if (released || trackedOwnerId == null || getWorld().isClient()
                || !(getWorld() instanceof ServerWorld world)) {
            return;
        }
        released = true;
        SoulstalkerAbilityManager.releaseStride(world, trackedOwnerId);
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseTracking();
        super.remove(reason);
    }

    private LivingEntity resolveOwner(ServerWorld world) {
        UUID ownerUuid = getOwnerUuid();
        if (ownerUuid == null) {
            return null;
        }
        Entity entity = getOwnerId() < 0 ? null : world.getEntityById(getOwnerId());
        if (entity == null || !ownerUuid.equals(entity.getUuid())) {
            entity = world.getEntity(ownerUuid);
        }
        if (entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            dataTracker.set(OWNER_ID, living.getId());
            return living;
        }
        return null;
    }

    public float getCurrentWidth() {
        float progress = MathHelper.clamp(getProgress(), 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        return MathHelper.lerp(eased, dataTracker.get(INITIAL_WIDTH), dataTracker.get(FINAL_WIDTH));
    }

    public float getProgress() {
        return dataTracker.get(PROGRESS);
    }

    public Vec3d getDirection() {
        Vector3f direction = dataTracker.get(DIRECTION);
        return new Vec3d(direction.x, direction.y, direction.z);
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
    public Box getVisibilityBoundingBox() {
        double reach = Math.max(2.0, Math.max(dataTracker.get(INITIAL_WIDTH), dataTracker.get(FINAL_WIDTH)) + 1.0);
        return getBoundingBox().expand(reach);
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
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
