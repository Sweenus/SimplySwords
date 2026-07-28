package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.world.CaelestisBreachManager;

import java.util.Optional;
import java.util.UUID;

public class CaelestisBreachVisualEntity extends Entity {

    public static final int PHASE_EXPANDING = 0;
    public static final int PHASE_OPEN = 1;
    public static final int PHASE_COLLAPSING = 2;

    private static final TrackedData<Optional<UUID>> BREACH_ID =
            DataTracker.registerData(CaelestisBreachVisualEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Float> RADIUS =
            DataTracker.registerData(CaelestisBreachVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_RADIUS =
            DataTracker.registerData(CaelestisBreachVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> VERTICAL_RANGE =
            DataTracker.registerData(CaelestisBreachVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> PHASE =
            DataTracker.registerData(CaelestisBreachVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(CaelestisBreachVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public CaelestisBreachVisualEntity(EntityType<? extends CaelestisBreachVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public CaelestisBreachVisualEntity(World world, UUID breachId, double x, double y, double z, int seed) {
        this(EntityRegistry.CAELESTIS_BREACH_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setBreachId(breachId);
        this.setSeed(seed);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(BREACH_ID, Optional.empty());
        builder.add(RADIUS, 1.5F);
        builder.add(MAX_RADIUS, 20.0F);
        builder.add(VERTICAL_RANGE, 10.0F);
        builder.add(PHASE, PHASE_EXPANDING);
        builder.add(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (!this.getWorld().isClient() && this.age > 5
                && (this.getBreachId() == null
                || !CaelestisBreachManager.isActive(this.getWorld(), this.getBreachId()))) {
            this.discard();
        }
    }

    public UUID getBreachId() {
        return this.dataTracker.get(BREACH_ID).orElse(null);
    }

    public void setBreachId(UUID breachId) {
        this.dataTracker.set(BREACH_ID, Optional.ofNullable(breachId));
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, Math.max(0.0F, radius));
    }

    public float getMaxRadius() {
        return this.dataTracker.get(MAX_RADIUS);
    }

    public void setMaxRadius(float maxRadius) {
        this.dataTracker.set(MAX_RADIUS, Math.max(1.0F, maxRadius));
    }

    public float getVerticalRange() {
        return this.dataTracker.get(VERTICAL_RANGE);
    }

    public void setVerticalRange(float verticalRange) {
        this.dataTracker.set(VERTICAL_RANGE, Math.max(2.0F, verticalRange));
    }

    public int getPhase() {
        return this.dataTracker.get(PHASE);
    }

    public void setPhase(int phase) {
        this.dataTracker.set(PHASE, Math.clamp(phase, PHASE_EXPANDING, PHASE_COLLAPSING));
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    public void setSeed(int seed) {
        this.dataTracker.set(SEED, seed);
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
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("breach_id")) {
            this.setBreachId(nbt.getUuid("breach_id"));
        }
        this.setRadius(nbt.getFloat("radius"));
        if (nbt.contains("max_radius")) {
            this.setMaxRadius(nbt.getFloat("max_radius"));
        }
        if (nbt.contains("vertical_range")) {
            this.setVerticalRange(nbt.getFloat("vertical_range"));
        }
        this.setPhase(nbt.getInt("phase"));
        this.setSeed(nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        UUID breachId = this.getBreachId();
        if (breachId != null) {
            nbt.putUuid("breach_id", breachId);
        }
        nbt.putFloat("radius", this.getRadius());
        nbt.putFloat("max_radius", this.getMaxRadius());
        nbt.putFloat("vertical_range", this.getVerticalRange());
        nbt.putInt("phase", this.getPhase());
        nbt.putInt("seed", this.getSeed());
    }
}
