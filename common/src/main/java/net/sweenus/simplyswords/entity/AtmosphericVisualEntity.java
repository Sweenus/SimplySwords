package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.render.ShockFrontStyle;
import net.sweenus.simplyswords.api.render.StormVolumeShape;
import net.sweenus.simplyswords.api.render.StormVolumeStyle;
import net.sweenus.simplyswords.api.render.SurfaceDischargeStyle;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class AtmosphericVisualEntity extends Entity {

    public static final int STORM_VOLUME = 0;
    public static final int SURFACE_DISCHARGE = 1;
    public static final int SHOCK_FRONT = 2;

    private static final TrackedData<Integer> ANCHOR_ID = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> KIND = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SHAPE = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PRIMARY = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SECONDARY = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> RADIUS = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEIGHT = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> DENSITY = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> PARAM_A = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> PARAM_B = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> INT_A = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> INT_B = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> END_X = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Y = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Z = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> ILLUMINATE = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(AtmosphericVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public AtmosphericVisualEntity(EntityType<? extends AtmosphericVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public AtmosphericVisualEntity(World world, Vec3d position, StormVolumeStyle style, int seed) {
        this(EntityRegistry.ATMOSPHERIC_VISUAL.get(), world);
        this.setPosition(position);
        this.dataTracker.set(KIND, STORM_VOLUME);
        this.dataTracker.set(SHAPE, style.shape().ordinal());
        this.dataTracker.set(PRIMARY, style.cloudColor());
        this.dataTracker.set(SECONDARY, style.accentColor());
        this.dataTracker.set(LIFETIME, style.lifetimeTicks());
        this.dataTracker.set(RADIUS, style.radius());
        this.dataTracker.set(HEIGHT, style.height());
        this.dataTracker.set(DENSITY, style.density());
        this.dataTracker.set(PARAM_A, style.rotationSpeed());
        this.dataTracker.set(PARAM_B, style.precipitation());
        this.dataTracker.set(ILLUMINATE, style.illuminate());
        this.dataTracker.set(SEED, seed);
    }

    public AtmosphericVisualEntity(World world, Vec3d origin, Vec3d end,
                                   SurfaceDischargeStyle style, int seed) {
        this(EntityRegistry.ATMOSPHERIC_VISUAL.get(), world);
        this.setPosition(origin);
        this.dataTracker.set(KIND, SURFACE_DISCHARGE);
        this.dataTracker.set(PRIMARY, style.primaryColor());
        this.dataTracker.set(SECONDARY, style.coreColor());
        this.dataTracker.set(LIFETIME, style.lifetimeTicks());
        this.dataTracker.set(RADIUS, style.coreWidth());
        this.dataTracker.set(HEIGHT, style.spread());
        this.dataTracker.set(DENSITY, style.branches());
        this.dataTracker.set(INT_A, style.crawlTicks());
        this.dataTracker.set(INT_B, style.pulseInterval());
        this.setEnd(end);
        this.dataTracker.set(SEED, seed);
    }

    public AtmosphericVisualEntity(World world, Vec3d origin, Vec3d direction,
                                   ShockFrontStyle style, int seed) {
        this(EntityRegistry.ATMOSPHERIC_VISUAL.get(), world);
        this.setPosition(origin);
        this.dataTracker.set(KIND, SHOCK_FRONT);
        this.dataTracker.set(PRIMARY, style.color());
        this.dataTracker.set(LIFETIME, style.lifetimeTicks());
        this.dataTracker.set(RADIUS, style.startRadius());
        this.dataTracker.set(HEIGHT, style.endRadius());
        this.dataTracker.set(PARAM_A, style.width());
        this.dataTracker.set(PARAM_B, style.height());
        this.setEnd(origin.add(direction == null ? Vec3d.ZERO : direction.normalize()));
        this.dataTracker.set(SEED, seed);
    }

    public void anchor(Entity anchor) {
        this.dataTracker.set(ANCHOR_ID, anchor == null ? -1 : anchor.getId());
        if (anchor != null) {
            this.setPosition(anchor.getPos());
            this.setYaw(anchor.getYaw());
        }
    }

    private void setEnd(Vec3d end) {
        this.dataTracker.set(END_X, (float) end.x);
        this.dataTracker.set(END_Y, (float) end.y);
        this.dataTracker.set(END_Z, (float) end.z);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(ANCHOR_ID, -1);
        builder.add(KIND, STORM_VOLUME);
        builder.add(SHAPE, 0);
        builder.add(PRIMARY, 0x263E53);
        builder.add(SECONDARY, 0x8FE7FF);
        builder.add(LIFETIME, 20);
        builder.add(RADIUS, 1.0F);
        builder.add(HEIGHT, 1.0F);
        builder.add(DENSITY, 8);
        builder.add(PARAM_A, 0.0F);
        builder.add(PARAM_B, 0.0F);
        builder.add(INT_A, 1);
        builder.add(INT_B, 1);
        builder.add(END_X, 0.0F);
        builder.add(END_Y, 0.0F);
        builder.add(END_Z, 0.0F);
        builder.add(ILLUMINATE, false);
        builder.add(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            emitAmbientParticles();
            return;
        }
        Entity anchor = getAnchor();
        if (getAnchorId() >= 0 && (anchor == null || !anchor.isAlive())) {
            this.discard();
            return;
        }
        if (anchor != null) {
            this.setPosition(anchor.getPos());
            this.setYaw(anchor.getYaw());
        }
        if (this.age > getLifetime()) this.discard();
    }

    private void emitAmbientParticles() {
        if (!Config.general.enableModernFieldEffects || Config.general.stormEffectDetail <= 0 || this.age > getLifetime()) return;
        if (getKind() == STORM_VOLUME) {
            emitStormParticles();
        } else if (getKind() == SURFACE_DISCHARGE && this.age % Math.max(2, getIntB()) == 0) {
            Vec3d direction = getEnd().subtract(this.getPos());
            double progress = unit(this.age + 3L);
            Vec3d point = this.getPos().add(direction.multiply(progress));
            this.getWorld().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    point.x, point.y + 0.08, point.z, 0.0, 0.018, 0.0);
        } else if (getKind() == SHOCK_FRONT && this.age % 2 == 0) {
            emitShockParticles();
        }
    }

    private void emitShockParticles() {
        double progress = MathHelper.clamp(this.age / (double) Math.max(1, getLifetime()), 0.0, 1.0);
        double radius = MathHelper.lerp(progress, getRadius(), getVisualHeight());
        double push = getParamA() * 0.06;
        int arc = detailCount(6);
        double base = unit(this.age + 7L) * MathHelper.TAU;
        for (int i = 0; i < arc; i++) {
            double angle = base + i * MathHelper.TAU / arc + (unit(this.age * 5L + i) - 0.5) * 0.25;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double y = 0.12 + (unit(this.age * 7L + i) - 0.5) * getParamB() * 0.6;
            this.getWorld().addParticle(ParticleTypes.POOF,
                    this.getX() + cos * radius, this.getY() + y, this.getZ() + sin * radius,
                    cos * push, 0.01, sin * push);
        }
    }

    private static int detailCount(int requested) {
        int detail = MathHelper.clamp(Config.general.stormEffectDetail, 0, 2);
        float scale = detail == 0 ? 0.34F : detail == 1 ? 0.64F : 1.0F;
        return Math.max(1, Math.round(requested * scale));
    }

    private static boolean isSparkShape(StormVolumeShape shape) {
        return shape == StormVolumeShape.LIGHTNING_CUBE || shape == StormVolumeShape.LODESTONE_CORE
                || shape == StormVolumeShape.ROD_STAKE;
    }

    private void emitStormParticles() {
        StormVolumeShape shape = getStormShape();
        if (!isSparkShape(shape) && getParamB() > 0.05F) emitRainParticles();
        int interval = isSparkShape(shape) ? 2 : 3;
        if (this.age % interval != 0) return;
        double angle = unit(this.age * 3L + 1L) * MathHelper.TAU;
        double radius = Math.sqrt(unit(this.age * 3L + 2L)) * getRadius();
        double x = this.getX() + Math.cos(angle) * radius;
        double z = this.getZ() + Math.sin(angle) * radius;
        if (isSparkShape(shape)) {
            double particleY = shape == StormVolumeShape.ROD_STAKE
                    ? unit(this.age * 3L + 3L) * getVisualHeight()
                    : (unit(this.age * 3L + 3L) - 0.5) * getVisualHeight();
            this.getWorld().addParticle(ParticleTypes.ELECTRIC_SPARK, x,
                    this.getY() + particleY, z,
                    0.0, 0.012, 0.0);
            return;
        }
        double y = this.getY() + Math.max(0.4, getVisualHeight() * (0.28 + unit(this.age * 3L + 3L) * 0.58));
        if (this.age % 6 == 0) {
            this.getWorld().addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.006, 0.0);
        }
        if (this.age % 5 == 0) {
            double drift = Math.toRadians(this.getYaw());
            this.getWorld().addParticle(ParticleTypes.SMALL_GUST, x, y - 0.25, z,
                    Math.cos(drift) * 0.05, 0.004, Math.sin(drift) * 0.05);
        }
    }

    private void emitRainParticles() {
        int count = detailCount(Math.max(1, Math.round(getDensity() * getParamB() * 0.12F)));
        double base = this.getY() + Math.max(0.3, getVisualHeight() * 0.26);
        for (int i = 0; i < count; i++) {
            long salt = this.age * 11L + i * 3L;
            double angle = unit(salt) * MathHelper.TAU;
            double distance = Math.sqrt(unit(salt + 1L)) * getRadius();
            this.getWorld().addParticle(ParticleTypes.FALLING_WATER,
                    this.getX() + Math.cos(angle) * distance,
                    base + unit(salt + 2L) * getVisualHeight() * 0.2,
                    this.getZ() + Math.sin(angle) * distance, 0.0, 0.0, 0.0);
        }
    }

    private float unit(long salt) {
        long hash = getSeed() * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;
        hash ^= hash >>> 30;
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= hash >>> 27;
        hash *= 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash >>> 40) / (float) (1 << 24);
    }

    public int getAnchorId() { return this.dataTracker.get(ANCHOR_ID); }
    public int getKind() { return this.dataTracker.get(KIND); }
    public int getPrimaryColor() { return this.dataTracker.get(PRIMARY); }
    public int getSecondaryColor() { return this.dataTracker.get(SECONDARY); }
    public int getLifetime() { return this.dataTracker.get(LIFETIME); }
    public float getRadius() { return this.dataTracker.get(RADIUS); }
    public float getVisualHeight() { return this.dataTracker.get(HEIGHT); }
    public int getDensity() { return this.dataTracker.get(DENSITY); }
    public float getParamA() { return this.dataTracker.get(PARAM_A); }
    public float getParamB() { return this.dataTracker.get(PARAM_B); }
    public int getIntA() { return this.dataTracker.get(INT_A); }
    public int getIntB() { return this.dataTracker.get(INT_B); }
    public int getSeed() { return this.dataTracker.get(SEED); }
    public boolean illuminates() { return this.dataTracker.get(ILLUMINATE); }
    public Vec3d getEnd() { return new Vec3d(this.dataTracker.get(END_X), this.dataTracker.get(END_Y), this.dataTracker.get(END_Z)); }
    public StormVolumeShape getStormShape() { return StormVolumeShape.values()[MathHelper.clamp(this.dataTracker.get(SHAPE), 0, StormVolumeShape.values().length - 1)]; }

    private Entity getAnchor() { return getAnchorId() < 0 ? null : this.getWorld().getEntityById(getAnchorId()); }

    @Override public boolean isAttackable() { return false; }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.dataTracker.set(ANCHOR_ID, nbt.getInt("anchor"));
        this.dataTracker.set(KIND, nbt.getInt("kind"));
        this.dataTracker.set(SHAPE, nbt.getInt("shape"));
        this.dataTracker.set(PRIMARY, nbt.getInt("primary"));
        this.dataTracker.set(SECONDARY, nbt.getInt("secondary"));
        this.dataTracker.set(LIFETIME, nbt.getInt("lifetime"));
        this.dataTracker.set(RADIUS, nbt.getFloat("radius"));
        this.dataTracker.set(HEIGHT, nbt.getFloat("height"));
        this.dataTracker.set(DENSITY, nbt.getInt("density"));
        this.dataTracker.set(PARAM_A, nbt.getFloat("param_a"));
        this.dataTracker.set(PARAM_B, nbt.getFloat("param_b"));
        this.dataTracker.set(INT_A, nbt.getInt("int_a"));
        this.dataTracker.set(INT_B, nbt.getInt("int_b"));
        this.dataTracker.set(END_X, nbt.getFloat("end_x"));
        this.dataTracker.set(END_Y, nbt.getFloat("end_y"));
        this.dataTracker.set(END_Z, nbt.getFloat("end_z"));
        this.dataTracker.set(ILLUMINATE, nbt.getBoolean("illuminate"));
        this.dataTracker.set(SEED, nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("anchor", getAnchorId());
        nbt.putInt("kind", getKind());
        nbt.putInt("shape", this.dataTracker.get(SHAPE));
        nbt.putInt("primary", getPrimaryColor());
        nbt.putInt("secondary", getSecondaryColor());
        nbt.putInt("lifetime", getLifetime());
        nbt.putFloat("radius", getRadius());
        nbt.putFloat("height", getVisualHeight());
        nbt.putInt("density", getDensity());
        nbt.putFloat("param_a", getParamA());
        nbt.putFloat("param_b", getParamB());
        nbt.putInt("int_a", getIntA());
        nbt.putInt("int_b", getIntB());
        nbt.putFloat("end_x", (float) getEnd().x);
        nbt.putFloat("end_y", (float) getEnd().y);
        nbt.putFloat("end_z", (float) getEnd().z);
        nbt.putBoolean("illuminate", illuminates());
        nbt.putInt("seed", getSeed());
    }
}
