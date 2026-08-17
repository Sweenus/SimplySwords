package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class DawnquiverBowVisualEntity extends Entity {

    public static final int PHASE_DRAWING = 0;
    public static final int PHASE_RELEASED = 1;
    public static final int MODE_ACTIVE = 0;
    public static final int MODE_PASSIVE = 1;

    private static final TrackedData<Integer> OWNER_ID =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> HAND =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MODE =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ID =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SCALE =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DRAW_PROGRESS =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> PHASE =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> RELEASE_AGE =
            DataTracker.registerData(DawnquiverBowVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public DawnquiverBowVisualEntity(EntityType<? extends DawnquiverBowVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public DawnquiverBowVisualEntity(World world, LivingEntity owner, Hand hand,
                                     double x, double y, double z, float yaw, float pitch,
                                     int lifetime, float scale, int seed, int mode) {
        this(EntityRegistry.DAWNQUIVER_BOW_VISUAL.get(), world);
        this.dataTracker.set(OWNER_ID, owner.getId());
        this.dataTracker.set(HAND, hand == Hand.OFF_HAND ? 1 : 0);
        this.dataTracker.set(MODE, mode == MODE_PASSIVE ? MODE_PASSIVE : MODE_ACTIVE);
        this.setPosition(x, y, z);
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.prevYaw = yaw;
        this.prevPitch = pitch;
        this.setLifetime(lifetime);
        this.setScale(scale);
        this.setSeed(seed);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ID, -1);
        builder.add(HAND, 0);
        builder.add(MODE, MODE_ACTIVE);
        builder.add(TARGET_ID, -1);
        builder.add(LIFETIME, 60);
        builder.add(SEED, 0);
        builder.add(SCALE, 1.0F);
        builder.add(DRAW_PROGRESS, 0.0F);
        builder.add(PHASE, PHASE_DRAWING);
        builder.add(RELEASE_AGE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (!this.getWorld().isClient() && this.age >= this.getLifetime()) {
            this.discard();
        }
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public LivingEntity getOwner() {
        Entity entity = this.getWorld().getEntityById(this.dataTracker.get(OWNER_ID));
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    public Hand getHand() {
        return this.dataTracker.get(HAND) == 1 ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    public int getMode() {
        return this.dataTracker.get(MODE);
    }

    public LivingEntity getTarget() {
        Entity entity = this.getWorld().getEntityById(this.dataTracker.get(TARGET_ID));
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    public void setTarget(LivingEntity target) {
        this.dataTracker.set(TARGET_ID, target == null ? -1 : target.getId());
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    public void setSeed(int seed) {
        this.dataTracker.set(SEED, seed);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, Math.max(0.2F, scale));
    }

    public float getDrawProgress() {
        return this.dataTracker.get(DRAW_PROGRESS);
    }

    public void setDrawProgress(float progress) {
        this.dataTracker.set(DRAW_PROGRESS, MathHelper.clamp(progress, 0.0F, 1.0F));
    }

    public int getPhase() {
        return this.dataTracker.get(PHASE);
    }

    public int getReleaseAge() {
        return this.dataTracker.get(RELEASE_AGE);
    }

    public void markReleased() {
        this.dataTracker.set(PHASE, PHASE_RELEASED);
        this.dataTracker.set(RELEASE_AGE, this.age);
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
    public Box getVisibilityBoundingBox() {
        LivingEntity owner = getOwner();
        return owner == null ? this.getBoundingBox().expand(6.0)
                : new Box(this.getPos(), owner.getPos()).expand(4.0);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
