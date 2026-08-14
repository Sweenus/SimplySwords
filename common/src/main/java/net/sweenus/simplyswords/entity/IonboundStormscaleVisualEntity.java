package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

import java.util.Optional;
import java.util.UUID;

public final class IonboundStormscaleVisualEntity extends Entity {
    public static final int ORBIT = 0;
    public static final int SHIELD = 1;
    public static final int CORRIDOR = 2;
    public static final int BEAM = 3;
    private static final int ORBIT_LIFETIME = 1800;
    private static final long ORBIT_LEASE_TICKS = 100L;

    private static final TrackedData<Integer> KIND = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> CUBES = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> LENGTH = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> WIDTH = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEIGHT = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> MATERIALIZE = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> HOLD = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CLOSE = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(IonboundStormscaleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private long orbitLeaseExpiresAt = Long.MIN_VALUE;

    public IonboundStormscaleVisualEntity(EntityType<? extends IonboundStormscaleVisualEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
    }

    public static IonboundStormscaleVisualEntity orbit(World world, Entity owner, int cubes) {
        IonboundStormscaleVisualEntity visual = create(world, owner.getPos(), ORBIT, ORBIT_LIFETIME);
        visual.dataTracker.set(SEED, owner.getUuid().hashCode());
        visual.refreshOrbitLease(owner, cubes);
        return visual;
    }

    public static IonboundStormscaleVisualEntity shield(World world, Entity owner, int lifetime) {
        IonboundStormscaleVisualEntity visual = create(world, owner.getPos(), SHIELD, lifetime);
        visual.setOwner(owner);
        visual.dataTracker.set(WIDTH, owner.getWidth() + 0.9F);
        visual.dataTracker.set(HEIGHT, owner.getHeight() + 0.7F);
        return visual;
    }

    public static IonboundStormscaleVisualEntity corridor(World world, Vec3d origin, float yaw,
                                                           float length, float width, float height,
                                                           int materialize, int hold, int close) {
        IonboundStormscaleVisualEntity visual = create(world, origin, CORRIDOR,
                materialize + hold + close + 8);
        visual.setYaw(yaw);
        visual.dataTracker.set(LENGTH, length);
        visual.dataTracker.set(WIDTH, width);
        visual.dataTracker.set(HEIGHT, height);
        visual.dataTracker.set(MATERIALIZE, materialize);
        visual.dataTracker.set(HOLD, hold);
        visual.dataTracker.set(CLOSE, close);
        return visual;
    }

    public static IonboundStormscaleVisualEntity beam(World world, Entity owner,
                                                       float length, float width, int lifetime) {
        IonboundStormscaleVisualEntity visual = create(world, owner.getPos(), BEAM, lifetime);
        visual.setOwner(owner);
        visual.setYaw(owner.getYaw());
        visual.setPitch(owner.getPitch());
        visual.dataTracker.set(LENGTH, length);
        visual.dataTracker.set(WIDTH, width);
        visual.dataTracker.set(HEIGHT, owner.getHeight());
        return visual;
    }

    private static IonboundStormscaleVisualEntity create(World world, Vec3d position, int kind, int lifetime) {
        IonboundStormscaleVisualEntity visual = new IonboundStormscaleVisualEntity(EntityRegistry.IONBOUND_STORMSCALE_VISUAL.get(), world);
        visual.setPosition(position);
        visual.dataTracker.set(KIND, kind);
        visual.dataTracker.set(LIFETIME, lifetime);
        visual.dataTracker.set(SEED, world.random.nextInt());
        return visual;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(KIND, ORBIT);
        this.dataTracker.startTracking(OWNER_ID, -1);
        this.dataTracker.startTracking(OWNER_UUID, Optional.empty());
        this.dataTracker.startTracking(CUBES, 0);
        this.dataTracker.startTracking(LIFETIME, 20);
        this.dataTracker.startTracking(LENGTH, 1.0F);
        this.dataTracker.startTracking(WIDTH, 1.0F);
        this.dataTracker.startTracking(HEIGHT, 1.0F);
        this.dataTracker.startTracking(MATERIALIZE, 1);
        this.dataTracker.startTracking(HOLD, 1);
        this.dataTracker.startTracking(CLOSE, 1);
        this.dataTracker.startTracking(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) return;
        if (getKind() == ORBIT && getWorld() instanceof ServerWorld serverWorld
                && (age >= getLifetime() || serverWorld.getTime() > orbitLeaseExpiresAt)) {
            discard();
            return;
        }
        if (getKind() == ORBIT || getKind() == SHIELD || getKind() == BEAM) {
            Entity owner = getOwner();
            if (owner == null || !owner.isAlive()) {
                discard();
                return;
            }
            setPosition(owner.getPos());
        }
        if (age >= getLifetime()) discard();
    }

    public void setCubes(int cubes) { dataTracker.set(CUBES, MathHelper.clamp(cubes, 0, 3)); }
    public void refreshOrbitLease(Entity owner, int cubes) {
        if (getKind() != ORBIT || owner == null) return;
        setOwner(owner);
        setCubes(cubes);
        if (getWorld() instanceof ServerWorld serverWorld) {
            orbitLeaseExpiresAt = serverWorld.getTime() + ORBIT_LEASE_TICKS;
        }
    }
    public boolean isOwnedBy(Entity owner) {
        return owner != null && dataTracker.get(OWNER_UUID)
                .map(owner.getUuid()::equals).orElse(false);
    }
    public boolean needsOrbitRenewal() {
        return getKind() != ORBIT || age >= ORBIT_LIFETIME - 1;
    }
    public int getKind() { return dataTracker.get(KIND); }
    public int getOwnerId() { return dataTracker.get(OWNER_ID); }
    public int getCubes() { return dataTracker.get(CUBES); }
    public int getLifetime() { return dataTracker.get(LIFETIME); }
    public float getLength() { return dataTracker.get(LENGTH); }
    public float getVisualWidth() { return dataTracker.get(WIDTH); }
    public float getVisualHeight() { return dataTracker.get(HEIGHT); }
    public int getMaterializeTicks() { return dataTracker.get(MATERIALIZE); }
    public int getHoldTicks() { return dataTracker.get(HOLD); }
    public int getCloseTicks() { return dataTracker.get(CLOSE); }
    public int getSeed() { return dataTracker.get(SEED); }
    public Entity getOwner() {
        Optional<UUID> expected = dataTracker.get(OWNER_UUID);
        if (expected.isEmpty()) return null;
        Entity byId = getOwnerId() < 0 ? null : getWorld().getEntityById(getOwnerId());
        if (byId != null && expected.get().equals(byId.getUuid())) return byId;
        if (!(getWorld() instanceof ServerWorld serverWorld)) return null;
        Entity byUuid = serverWorld.getEntity(expected.get());
        if (byUuid != null) dataTracker.set(OWNER_ID, byUuid.getId());
        return byUuid;
    }

    private void setOwner(Entity owner) {
        dataTracker.set(OWNER_ID, owner.getId());
        dataTracker.set(OWNER_UUID, Optional.of(owner.getUuid()));
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public Box getVisibilityBoundingBox() {
        double extent = Math.max(4.0, Math.max(getLength(), getVisualWidth()) + 3.0);
        return getBoundingBox().expand(extent, Math.max(3.0, getVisualHeight() + 2.0), extent);
    }

    @Override public boolean isAttackable() { return false; }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        dataTracker.set(OWNER_UUID, nbt.containsUuid("owner_uuid")
                ? Optional.of(nbt.getUuid("owner_uuid")) : Optional.empty());
        dataTracker.set(KIND, nbt.getInt("kind"));
        dataTracker.set(OWNER_ID, nbt.getInt("owner"));
        dataTracker.set(CUBES, nbt.getInt("cubes"));
        dataTracker.set(LIFETIME, nbt.getInt("lifetime"));
        dataTracker.set(LENGTH, nbt.getFloat("length"));
        dataTracker.set(WIDTH, nbt.getFloat("width"));
        dataTracker.set(HEIGHT, nbt.getFloat("height"));
        dataTracker.set(MATERIALIZE, nbt.getInt("materialize"));
        dataTracker.set(HOLD, nbt.getInt("hold"));
        dataTracker.set(CLOSE, nbt.getInt("close"));
        dataTracker.set(SEED, nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        dataTracker.get(OWNER_UUID).ifPresent(uuid -> nbt.putUuid("owner_uuid", uuid));
        nbt.putInt("kind", getKind());
        nbt.putInt("owner", getOwnerId());
        nbt.putInt("cubes", getCubes());
        nbt.putInt("lifetime", getLifetime());
        nbt.putFloat("length", getLength());
        nbt.putFloat("width", getVisualWidth());
        nbt.putFloat("height", getVisualHeight());
        nbt.putInt("materialize", getMaterializeTicks());
        nbt.putInt("hold", getHoldTicks());
        nbt.putInt("close", getCloseTicks());
        nbt.putInt("seed", getSeed());
    }
}
