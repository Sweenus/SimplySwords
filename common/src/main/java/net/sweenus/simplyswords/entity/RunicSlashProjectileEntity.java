package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.RunicSlashManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RunicSlashProjectileEntity extends Entity {

    private static final TrackedData<ItemStack> WEAPON_STACK = DataTracker.registerData(RunicSlashProjectileEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(RunicSlashProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> WIDTH = DataTracker.registerData(RunicSlashProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private final Set<UUID> hitTargets = new HashSet<>();
    private UUID ownerUuid;
    private double maxDistance = 8.0;
    private double traveled;
    private float damage;

    public RunicSlashProjectileEntity(EntityType<? extends RunicSlashProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public RunicSlashProjectileEntity(ServerWorld world, LivingEntity owner, ItemStack stack, Vec3d direction, double maxDistance, double speed, float damage, double width) {
        this(EntityRegistry.RUNIC_SLASH_PROJECTILE.get(), world);
        this.ownerUuid = owner.getUuid();
        this.dataTracker.set(OWNER_ID, owner.getId());
        this.dataTracker.set(WEAPON_STACK, stack);
        this.dataTracker.set(WIDTH, (float) Math.max(0.1, width));
        this.maxDistance = maxDistance;
        this.damage = damage;
        this.setVelocity(direction.normalize().multiply(speed));
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(WEAPON_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(OWNER_ID, -1);
        this.dataTracker.startTracking(WIDTH, 1.15F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld world) || !(getOwnerEntity(world) instanceof LivingEntity owner) || !owner.isAlive()) {
            this.discard();
            return;
        }

        Vec3d current = this.getPos();
        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() < 0.0001) {
            this.discard();
            return;
        }

        Vec3d next = current.add(velocity);
        BlockHitResult blockHit = world.raycast(new RaycastContext(current, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            next = blockHit.getPos();
        }

        damageTargets(world, owner, current, next);
        spawnTrail(world, current, next);

        this.traveled += current.distanceTo(next);
        this.setPosition(next);
        if (blockHit.getType() != HitResult.Type.MISS || this.traveled >= this.maxDistance || this.age > 80) {
            this.discard();
        }
    }

    private void damageTargets(ServerWorld world, LivingEntity owner, Vec3d current, Vec3d next) {
        ItemStack stack = this.dataTracker.get(WEAPON_STACK);
        if (stack.isEmpty()) {
            return;
        }

        double halfWidth = Math.max(0.1, this.dataTracker.get(WIDTH) * 0.5F);
        Box searchBox = new Box(current, next).expand(halfWidth);
        for (Entity entity : world.getOtherEntities(this, searchBox, entity -> entity instanceof LivingEntity && entity.isAlive())) {
            if (!(entity instanceof LivingEntity target)
                    || target == owner
                    || this.hitTargets.contains(target.getUuid())
                    || !target.getBoundingBox().expand(target.getTargetingMargin()).expand(halfWidth * 0.35).intersects(searchBox)
                    || !HelperMethods.checkAbilityTarget(target, owner)) {
                continue;
            }

            this.hitTargets.add(target.getUuid());
            boolean[] damaged = {false};
            RunicSlashManager.runSuppressed(() -> damaged[0] = SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, this.damage));
            if (damaged[0]) {
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.ELEMENTAL_SWORD_WIND_ATTACK_02.get(), SoundCategory.PLAYERS, 0.38F, 1.35F + world.random.nextFloat() * 0.15F);
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.62), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getBodyY(0.62), target.getZ(), 8, 0.25, 0.2, 0.25, 0.0);
            }
        }
    }

    private void spawnTrail(ServerWorld world, Vec3d current, Vec3d next) {
        if (this.age % 2 == 0) {
            Vec3d mid = current.add(next).multiply(0.5);
            world.spawnParticles(ParticleTypes.CLOUD, mid.x, mid.y, mid.z, 2, 0.08, 0.04, 0.08, 0.0);
        }
    }

    private Entity getOwnerEntity(ServerWorld world) {
        int ownerId = this.dataTracker.get(OWNER_ID);
        Entity owner = ownerId >= 0 ? world.getEntityById(ownerId) : null;
        if (owner != null || this.ownerUuid == null) {
            return owner;
        }
        return world.getEntity(this.ownerUuid);
    }

    public float getSlashWidth() {
        return this.dataTracker.get(WIDTH);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.ownerUuid = nbt.containsUuid("owner_uuid") ? nbt.getUuid("owner_uuid") : null;
        this.maxDistance = nbt.getDouble("max_distance");
        this.traveled = nbt.getDouble("traveled");
        this.damage = nbt.getFloat("damage");
        if (nbt.contains("weapon_stack")) {
            this.dataTracker.set(WEAPON_STACK, ItemStack.fromNbt(nbt.getCompound("weapon_stack")));
        }
        if (nbt.contains("owner_id")) {
            this.dataTracker.set(OWNER_ID, nbt.getInt("owner_id"));
        }
        if (nbt.contains("width")) {
            this.dataTracker.set(WIDTH, nbt.getFloat("width"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        nbt.putDouble("max_distance", this.maxDistance);
        nbt.putDouble("traveled", this.traveled);
        nbt.putFloat("damage", this.damage);
        ItemStack stack = this.dataTracker.get(WEAPON_STACK);
        if (!stack.isEmpty()) {
            nbt.put("weapon_stack", stack.writeNbt(new NbtCompound()));
        }
        nbt.putInt("owner_id", this.dataTracker.get(OWNER_ID));
        nbt.putFloat("width", this.dataTracker.get(WIDTH));
    }
}
