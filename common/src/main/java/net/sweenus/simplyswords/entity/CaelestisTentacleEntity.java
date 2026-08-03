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

public class CaelestisTentacleEntity extends Entity {

    public static final int SIZE_SMALL = 0;
    public static final int SIZE_MEDIUM = 1;
    public static final int SIZE_LARGE = 2;
    public static final int EMERGE_TICKS = 12;
    public static final int RETRACT_TICKS = 10;

    private static final TrackedData<Optional<UUID>> BREACH_ID =
            DataTracker.registerData(CaelestisTentacleEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> SIZE =
            DataTracker.registerData(CaelestisTentacleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CORRUPTION_SEED =
            DataTracker.registerData(CaelestisTentacleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> RETRACTING =
            DataTracker.registerData(CaelestisTentacleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> EMERGENCE_TICKS_ELAPSED =
            DataTracker.registerData(CaelestisTentacleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> RETRACT_TICKS_ELAPSED =
            DataTracker.registerData(CaelestisTentacleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> CONTACT_INTENSITY =
            DataTracker.registerData(CaelestisTentacleEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public CaelestisTentacleEntity(EntityType<? extends CaelestisTentacleEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public CaelestisTentacleEntity(World world, UUID breachId, double x, double y, double z,
                                   int size, int corruptionSeed) {
        this(EntityRegistry.CAELESTIS_TENTACLE.get(), world);
        this.setPosition(x, y, z);
        this.setBreachId(breachId);
        this.setTentacleSize(size);
        this.setCorruptionSeed(corruptionSeed);
        this.setYaw(Math.floorMod(corruptionSeed, 360));
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(BREACH_ID, Optional.empty());
        this.dataTracker.startTracking(SIZE, SIZE_MEDIUM);
        this.dataTracker.startTracking(CORRUPTION_SEED, 0);
        this.dataTracker.startTracking(RETRACTING, false);
        this.dataTracker.startTracking(EMERGENCE_TICKS_ELAPSED, 0);
        this.dataTracker.startTracking(RETRACT_TICKS_ELAPSED, 0);
        this.dataTracker.startTracking(CONTACT_INTENSITY, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (this.getWorld().isClient()) {
            return;
        }
        if (this.getEmergenceTicksElapsed() < EMERGE_TICKS) {
            this.dataTracker.set(
                    EMERGENCE_TICKS_ELAPSED, this.getEmergenceTicksElapsed() + 1);
        }
        if (this.isRetracting()) {
            this.dataTracker.set(RETRACT_TICKS_ELAPSED, this.getRetractTicksElapsed() + 1);
            this.setContactIntensity(Math.max(0.0F, this.getContactIntensity() - 0.25F));
            if (this.getRetractTicksElapsed() >= RETRACT_TICKS) {
                this.discard();
            }
            return;
        }
        if (!CaelestisBreachManager.tickTentacle(this)) {
            this.beginRetraction();
        }
    }

    public void beginRetraction() {
        if (this.isRetracting()) {
            return;
        }
        this.dataTracker.set(RETRACTING, true);
        this.dataTracker.set(RETRACT_TICKS_ELAPSED, 0);
        this.setContactIntensity(0.0F);
    }

    public UUID getBreachId() {
        return this.dataTracker.get(BREACH_ID).orElse(null);
    }

    public void setBreachId(UUID breachId) {
        this.dataTracker.set(BREACH_ID, Optional.ofNullable(breachId));
    }

    public int getTentacleSize() {
        return this.dataTracker.get(SIZE);
    }

    public void setTentacleSize(int size) {
        this.dataTracker.set(SIZE, net.minecraft.util.math.MathHelper.clamp(size, SIZE_SMALL, SIZE_LARGE));
    }

    public int getCorruptionSeed() {
        return this.dataTracker.get(CORRUPTION_SEED);
    }

    public void setCorruptionSeed(int corruptionSeed) {
        this.dataTracker.set(CORRUPTION_SEED, corruptionSeed);
    }

    public boolean isRetracting() {
        return this.dataTracker.get(RETRACTING);
    }

    public int getEmergenceTicksElapsed() {
        return this.dataTracker.get(EMERGENCE_TICKS_ELAPSED);
    }

    public int getRetractTicksElapsed() {
        return this.dataTracker.get(RETRACT_TICKS_ELAPSED);
    }

    public float getContactIntensity() {
        return this.dataTracker.get(CONTACT_INTENSITY);
    }

    public void setContactIntensity(float contactIntensity) {
        this.dataTracker.set(CONTACT_INTENSITY, net.minecraft.util.math.MathHelper.clamp(contactIntensity, 0.0F, 1.0F));
    }

    public float getContactRadius() {
        return switch (this.getTentacleSize()) {
            case SIZE_SMALL -> 0.45F;
            case SIZE_LARGE -> 0.80F;
            default -> 0.60F;
        };
    }

    public float getTentacleHeight() {
        return switch (this.getTentacleSize()) {
            case SIZE_SMALL -> 1.8F;
            case SIZE_LARGE -> 4.0F;
            default -> 2.8F;
        };
    }

    public float getRenderScale() {
        return switch (this.getTentacleSize()) {
            case SIZE_SMALL -> 0.75F;
            case SIZE_LARGE -> 1.64F;
            default -> 1.15F;
        };
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
        this.setTentacleSize(nbt.getInt("tentacle_size"));
        this.setCorruptionSeed(nbt.getInt("corruption_seed"));
        this.dataTracker.set(RETRACTING, nbt.getBoolean("retracting"));
        this.dataTracker.set(
                EMERGENCE_TICKS_ELAPSED, nbt.getInt("emergence_ticks_elapsed"));
        this.dataTracker.set(
                RETRACT_TICKS_ELAPSED, nbt.getInt("retract_ticks_elapsed"));
        this.setContactIntensity(nbt.getFloat("contact_intensity"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        UUID breachId = this.getBreachId();
        if (breachId != null) {
            nbt.putUuid("breach_id", breachId);
        }
        nbt.putInt("tentacle_size", this.getTentacleSize());
        nbt.putInt("corruption_seed", this.getCorruptionSeed());
        nbt.putBoolean("retracting", this.isRetracting());
        nbt.putInt("emergence_ticks_elapsed", this.getEmergenceTicksElapsed());
        nbt.putInt("retract_ticks_elapsed", this.getRetractTicksElapsed());
        nbt.putFloat("contact_intensity", this.getContactIntensity());
    }
}
