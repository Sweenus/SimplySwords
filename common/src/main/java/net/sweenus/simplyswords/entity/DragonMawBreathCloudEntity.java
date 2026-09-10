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
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.world.DragonMawManager;

import java.util.UUID;

public class DragonMawBreathCloudEntity extends Entity {

    private static final TrackedData<Integer> OWNER_ENTITY_ID = DataTracker.registerData(DragonMawBreathCloudEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<ItemStack> WEAPON_STACK = DataTracker.registerData(DragonMawBreathCloudEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Float> DAMAGE = DataTracker.registerData(DragonMawBreathCloudEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> RADIUS = DataTracker.registerData(DragonMawBreathCloudEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(DragonMawBreathCloudEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DAMAGE_INTERVAL = DataTracker.registerData(DragonMawBreathCloudEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private UUID ownerUuid;
    private Vec3d origin = Vec3d.ZERO;

    public DragonMawBreathCloudEntity(EntityType<? extends DragonMawBreathCloudEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public DragonMawBreathCloudEntity(ServerWorld world, LivingEntity owner, ItemStack stack, float damage, Vec3d origin, Vec3d position,
                                      double radius, int lifetime, int damageInterval) {
        this(EntityRegistry.DRAGON_MAW_BREATH_CLOUD.get(), world);
        this.ownerUuid = owner.getUuid();
        this.origin = origin;
        this.dataTracker.set(OWNER_ENTITY_ID, owner.getId());
        this.dataTracker.set(WEAPON_STACK, stack.copy());
        CombatProvenanceApi.attach(this, stack);
        this.dataTracker.set(DAMAGE, Math.max(0.0F, damage));
        this.dataTracker.set(RADIUS, (float) Math.max(0.25, radius));
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
        this.dataTracker.set(DAMAGE_INTERVAL, Math.max(1, damageInterval));
        this.setPosition(position.x, position.y + 0.08, position.z);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ENTITY_ID, -1);
        builder.add(WEAPON_STACK, ItemStack.EMPTY);
        builder.add(DAMAGE, 0.0F);
        builder.add(RADIUS, 2.25F);
        builder.add(LIFETIME, 50);
        builder.add(DAMAGE_INTERVAL, 10);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld() instanceof ServerWorld world) {
            spawnParticles(world);
            if (this.age % Math.max(1, this.getDamageInterval()) == 0) {
                damageTargets(world);
            }
            if (this.age > this.getLifetime()) {
                this.discard();
            }
        }
    }

    private void spawnParticles(ServerWorld world) {
        float radius = this.getRadius();
        Vec3d target = this.getPos();
        Vec3d path = target.subtract(this.origin);
        double pathLength = path.length();
        Vec3d direction = pathLength > 1.0E-5 ? path.normalize() : new Vec3d(0.0, 0.0, 1.0);
        Vec3d horizontalRight = new Vec3d(-direction.z, 0.0, direction.x);
        if (horizontalRight.lengthSquared() < 1.0E-5) {
            horizontalRight = new Vec3d(1.0, 0.0, 0.0);
        }

        int streamSteps = Math.max(3, Math.min(10, (int) (pathLength * 1.25)));
        int activeStreamTicks = Math.min(10, Math.max(5, this.getLifetime() / 6));
        if (this.age <= activeStreamTicks) {
            for (int i = 0; i <= streamSteps; i++) {
                double t = i / (double) streamSteps;
                double width = MathHelper.lerp(t, radius * 0.08, radius * 0.36);
                Vec3d center = this.origin.lerp(target, t);
                double turbulence = Math.sin((this.age + i) * 0.85) * width * 0.22;
                Vec3d pos = center.add(horizontalRight.multiply(turbulence));
                double downward = MathHelper.lerp(t, 0.0, -0.28);
                world.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y + downward, pos.z,
                        1, 0.0, 0.0, 0.0, 0.0);
                if (i % 3 == 0) {
                    world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + downward, pos.z,
                            1, width * 0.04, 0.02 + t * 0.02, width * 0.04, 0.012);
                }
                if (i % 4 == 0) {
                    world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + downward, pos.z,
                            1, width * 0.07, 0.025 + t * 0.025, width * 0.07, 0.006);
                }
            }
        }

        int groundSteps = Math.max(5, Math.min(18, (int) (pathLength * 1.8)));
        for (int i = 1; i <= groundSteps; i++) {
            double t = i / (double) groundSteps;
            double width = MathHelper.lerp(t, radius * 0.25, radius);
            Vec3d center = this.origin.lerp(target, t);
            double y = MathHelper.lerp(t, this.origin.y, this.getY()) - 0.05;
            if ((this.age + i) % 3 == 0) {
                world.spawnParticles(ParticleTypes.ASH, center.x, y + 0.02, center.z,
                        1, width * 0.18, 0.035, width * 0.18, 0.006);
            }
            if (t > 0.78 && (this.age + i) % 6 == 0) {
                world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, y + 0.05, center.z,
                        1, width * 0.12, 0.025, width * 0.12, 0.004);
            }
        }

        this.spawnStationaryGroundFlames(world, target, radius);
        world.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.25, this.getZ(),
                8, radius * 0.42, 0.16, radius * 0.42, 0.018);
        if (this.age <= 4) {
            world.spawnParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.12, this.getZ(),
                    10, radius * 0.3, 0.1, radius * 0.3, 0.035);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.22, this.getZ(),
                    8, radius * 0.4, 0.16, radius * 0.4, 0.025);
        }
        if (this.age % 8 == 0) {
            for (int i = 0; i < 12; i++) {
                double angle = MathHelper.TAU * i / 12.0;
                double x = this.getX() + Math.cos(angle) * radius * 0.75;
                double z = this.getZ() + Math.sin(angle) * radius * 0.75;
                Vec3d flamePos = this.getGroundParticlePos(world, x, this.getY() + 1.5, z);
                if (flamePos != null) {
                    world.spawnParticles(ParticleTypes.FLAME, flamePos.x, flamePos.y, flamePos.z,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
                if (i % 3 == 0) {
                    world.spawnParticles(ParticleTypes.LAVA, x, this.getY() + 0.08, z,
                            1, 0.06, 0.035, 0.06, 0.01);
                }
            }
        }
    }

    private void spawnStationaryGroundFlames(ServerWorld world, Vec3d target, float radius) {
        Vec3d center = this.getGroundParticlePos(world, target.x, target.y + 1.5, target.z);
        if (center != null) {
            world.spawnParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
                    1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.08, center.z,
                    2, radius * 0.18, 0.04, radius * 0.18, 0.006);
            if (this.age % 2 == 0) {
                world.spawnParticles(ParticleTypes.SMALL_FLAME, center.x, center.y, center.z,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        int flameCount = Math.max(4, Math.min(10, (int) (radius * 4.0F)));
        for (int i = 0; i < flameCount; i++) {
            double angle = MathHelper.TAU * (i + (this.age % 6) * 0.17) / flameCount;
            double distance = radius * (0.25 + 0.7 * ((i * 37 + this.age * 11) % 100) / 100.0);
            double x = target.x + Math.cos(angle) * distance;
            double z = target.z + Math.sin(angle) * distance;
            Vec3d flamePos = this.getGroundParticlePos(world, x, target.y + 1.5, z);
            if (flamePos != null) {
                world.spawnParticles((i + this.age) % 4 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                        flamePos.x, flamePos.y, flamePos.z,
                        1, 0.0, 0.0, 0.0, 0.0);
                if ((i + this.age) % 3 == 0) {
                    world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, flamePos.x, flamePos.y + 0.06, flamePos.z,
                            1, radius * 0.08, 0.025, radius * 0.08, 0.004);
                }
            }
        }
    }

    private Vec3d getGroundParticlePos(ServerWorld world, double x, double y, double z) {
        Vec3d start = new Vec3d(x, y + 2.0, z);
        Vec3d end = new Vec3d(x, y - 4.0, z);
        BlockHitResult hit = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return hit.getPos().add(0.0, 0.035, 0.0);
    }

    private void damageTargets(ServerWorld world) {
        LivingEntity owner = this.getOwner(world);
        ItemStack stack = this.dataTracker.get(WEAPON_STACK);
        float damage = this.dataTracker.get(DAMAGE);
        if (owner == null || stack.isEmpty() || damage <= 0.0F) {
            return;
        }

        float radius = this.getRadius();
        Vec3d end = this.getPos();
        Box box = new Box(
                Math.min(this.origin.x, end.x), Math.min(this.origin.y, end.y), Math.min(this.origin.z, end.z),
                Math.max(this.origin.x, end.x), Math.max(this.origin.y, end.y), Math.max(this.origin.z, end.z)
        ).expand(radius, 1.0, radius);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, target ->
                DragonMawManager.isValidTarget(owner, target)
                        && this.isInsideBreathArea(target, radius))) {
            SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, damage);
        }

        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.25F, 1.75F);
    }

    private boolean isInsideBreathArea(LivingEntity target, float radius) {
        Vec3d start = this.origin;
        Vec3d end = this.getPos();
        Vec3d path = end.subtract(start);
        Vec3d targetPos = target.getPos().add(0.0, target.getHeight() * 0.35, 0.0);
        double lengthSquared = path.lengthSquared();
        if (lengthSquared < 1.0E-5) {
            return target.squaredDistanceTo(this) <= radius * radius + 1.0;
        }

        double t = MathHelper.clamp(targetPos.subtract(start).dotProduct(path) / lengthSquared, 0.0, 1.0);
        Vec3d closest = start.add(path.multiply(t));
        double allowedRadius = MathHelper.lerp(t, radius * 0.35, radius);
        double horizontalDistanceSquared = new Vec3d(targetPos.x - closest.x, 0.0, targetPos.z - closest.z).lengthSquared();
        double verticalAllowance = target.isOnGround() ? 1.6 : 2.4;
        return horizontalDistanceSquared <= allowedRadius * allowedRadius + 0.35
                && Math.abs(targetPos.y - closest.y) <= verticalAllowance;
    }

    private LivingEntity getOwner(ServerWorld world) {
        int ownerId = this.dataTracker.get(OWNER_ENTITY_ID);
        Entity owner = ownerId >= 0 ? world.getEntityById(ownerId) : null;
        if (owner instanceof LivingEntity livingOwner) {
            return livingOwner;
        }
        if (this.ownerUuid == null) {
            return null;
        }
        Entity entity = world.getEntity(this.ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public int getDamageInterval() {
        return this.dataTracker.get(DAMAGE_INTERVAL);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.ownerUuid = nbt.containsUuid("owner_uuid") ? nbt.getUuid("owner_uuid") : null;
        if (nbt.contains("owner_id")) {
            this.dataTracker.set(OWNER_ENTITY_ID, nbt.getInt("owner_id"));
        }
        if (nbt.contains("origin_x")) {
            this.origin = new Vec3d(nbt.getDouble("origin_x"), nbt.getDouble("origin_y"), nbt.getDouble("origin_z"));
        }
        if (nbt.contains("weapon_stack")) {
            this.dataTracker.set(WEAPON_STACK, ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("weapon_stack")).orElse(ItemStack.EMPTY));
        }
        this.dataTracker.set(DAMAGE, nbt.getFloat("damage"));
        this.dataTracker.set(RADIUS, nbt.getFloat("radius"));
        this.dataTracker.set(LIFETIME, nbt.getInt("lifetime"));
        this.dataTracker.set(DAMAGE_INTERVAL, nbt.getInt("damage_interval"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        nbt.putDouble("origin_x", this.origin.x);
        nbt.putDouble("origin_y", this.origin.y);
        nbt.putDouble("origin_z", this.origin.z);
        nbt.putInt("owner_id", this.dataTracker.get(OWNER_ENTITY_ID));
        ItemStack stack = this.dataTracker.get(WEAPON_STACK);
        if (!stack.isEmpty()) {
            nbt.put("weapon_stack", stack.encode(this.getRegistryManager()));
        }
        nbt.putFloat("damage", this.dataTracker.get(DAMAGE));
        nbt.putFloat("radius", this.dataTracker.get(RADIUS));
        nbt.putInt("lifetime", this.dataTracker.get(LIFETIME));
        nbt.putInt("damage_interval", this.dataTracker.get(DAMAGE_INTERVAL));
    }
}
