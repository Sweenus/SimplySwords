package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SimplySwordsBeeEntity extends BeeEntity implements Tameable {
    private static final TrackedData<Boolean> HIVEMIND_SWARM = DataTracker.registerData(SimplySwordsBeeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BLOODWAKE_FLY = DataTracker.registerData(SimplySwordsBeeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    public UUID ownerUuid;
    public static int lifespan = 200;
    private UUID swarmTargetUuid;
    private int swarmStingsRemaining;
    private float swarmStingDamage;
    private long swarmExpiryTick;
    private long swarmNextStingTick;
    private long swarmNextDiveTick;
    private UUID swarmAnchorUuid;
    private UUID swarmPassTargetUuid;
    private Vec3d swarmPassStartPos;
    private Vec3d swarmPassExitPos;
    private boolean swarmPassStung;
    private UUID swarmLineupTargetUuid;
    private Vec3d swarmLineupPos;

    public SimplySwordsBeeEntity(EntityType<? extends BeeEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setCanPickUpLoot(false);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(HIVEMIND_SWARM, false);
        this.dataTracker.startTracking(BLOODWAKE_FLY, false);
    }

    public static DefaultAttributeContainer.Builder createSimplyBeeAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 1.6f)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.6f)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
    }
    @Override
    public void tick() {
        this.setInvulnerable(true);
        if (isHivemindSwarmBee()) {
            this.noClip = true;
            if (!this.getWorld().isClient() && (swarmStingsRemaining <= 0 || this.getWorld().getTime() > swarmExpiryTick)) {
                this.discard();
                return;
            }
        }
        if (!isHivemindSwarmBee() && (hasStung() || this.age > lifespan))
            this.discard();

        super.tick();
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (isHivemindSwarmBee()) {
            return false;
        }
        Vec3d velocity = target.getVelocity();
        target.timeUntilRegen = 0;
        boolean attacked = super.tryAttack(target);
        target.setVelocity(velocity);
        target.velocityModified = true;
        return attacked;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (isHivemindSwarmBee() && source.getAttacker() instanceof PlayerEntity) {
            return false;
        }
        return super.damage(source, amount);
    }

    @Override
    public boolean isPushable() {
        return !isHivemindSwarmBee() && super.isPushable();
    }

    @Override
    public boolean isCollidable() {
        return !isHivemindSwarmBee() && super.isCollidable();
    }

    @Override
    public boolean collidesWith(Entity other) {
        return !isHivemindSwarmBee() && super.collidesWith(other);
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        if (!isHivemindSwarmBee()) {
            super.pushAwayFrom(entity);
        }
    }

    @Nullable
    @Override
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    @Override
    public net.minecraft.world.EntityView method_48926() {
        return this.getWorld();
    }

    public void setOwner(LivingEntity livingEntity) {
        this.ownerUuid = livingEntity.getUuid();
    }

    public boolean isHivemindSwarmBee() {
        return this.dataTracker.get(HIVEMIND_SWARM);
    }

    public void setHivemindSwarmBee(boolean hivemindSwarmBee) {
        this.dataTracker.set(HIVEMIND_SWARM, hivemindSwarmBee);
        this.noClip = hivemindSwarmBee;
        if (hivemindSwarmBee) {
            this.experiencePoints = 0;
            this.setCanPickUpLoot(false);
        }
    }

    public boolean isBloodwakeFly() {
        return this.dataTracker.get(BLOODWAKE_FLY);
    }

    public void setBloodwakeFly(boolean bloodwakeFly) {
        this.dataTracker.set(BLOODWAKE_FLY, bloodwakeFly);
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean causedByPlayer) {
        if (!isHivemindSwarmBee()) {
            super.dropEquipment(source, lootingMultiplier, causedByPlayer);
        }
    }

    @Nullable
    public UUID getSwarmTargetUuid() {
        return swarmTargetUuid;
    }

    public void setSwarmTargetUuid(@Nullable UUID swarmTargetUuid) {
        this.swarmTargetUuid = swarmTargetUuid;
    }

    public int getSwarmStingsRemaining() {
        return swarmStingsRemaining;
    }

    public void setSwarmStingsRemaining(int swarmStingsRemaining) {
        this.swarmStingsRemaining = swarmStingsRemaining;
    }

    public void decrementSwarmStingsRemaining() {
        this.swarmStingsRemaining--;
    }

    public float getSwarmStingDamage() {
        return swarmStingDamage;
    }

    public void setSwarmStingDamage(float swarmStingDamage) {
        this.swarmStingDamage = swarmStingDamage;
    }

    public long getSwarmExpiryTick() {
        return swarmExpiryTick;
    }

    public void setSwarmExpiryTick(long swarmExpiryTick) {
        this.swarmExpiryTick = swarmExpiryTick;
    }

    public long getSwarmNextStingTick() {
        return swarmNextStingTick;
    }

    public void setSwarmNextStingTick(long swarmNextStingTick) {
        this.swarmNextStingTick = swarmNextStingTick;
    }

    public long getSwarmNextDiveTick() {
        return swarmNextDiveTick;
    }

    public void setSwarmNextDiveTick(long swarmNextDiveTick) {
        this.swarmNextDiveTick = swarmNextDiveTick;
    }

    @Nullable
    public UUID getSwarmAnchorUuid() {
        return swarmAnchorUuid;
    }

    public void setSwarmAnchorUuid(@Nullable UUID swarmAnchorUuid) {
        this.swarmAnchorUuid = swarmAnchorUuid;
    }

    @Nullable
    public UUID getSwarmPassTargetUuid() {
        return swarmPassTargetUuid;
    }

    public void setSwarmPassTargetUuid(@Nullable UUID swarmPassTargetUuid) {
        this.swarmPassTargetUuid = swarmPassTargetUuid;
    }

    @Nullable
    public Vec3d getSwarmPassStartPos() {
        return swarmPassStartPos;
    }

    public void setSwarmPassStartPos(@Nullable Vec3d swarmPassStartPos) {
        this.swarmPassStartPos = swarmPassStartPos;
    }

    @Nullable
    public Vec3d getSwarmPassExitPos() {
        return swarmPassExitPos;
    }

    public void setSwarmPassExitPos(@Nullable Vec3d swarmPassExitPos) {
        this.swarmPassExitPos = swarmPassExitPos;
    }

    public boolean hasSwarmPassStung() {
        return swarmPassStung;
    }

    public void setSwarmPassStung(boolean swarmPassStung) {
        this.swarmPassStung = swarmPassStung;
    }

    public void clearSwarmPass() {
        this.swarmPassTargetUuid = null;
        this.swarmPassStartPos = null;
        this.swarmPassExitPos = null;
        this.swarmPassStung = false;
    }

    @Nullable
    public UUID getSwarmLineupTargetUuid() {
        return swarmLineupTargetUuid;
    }

    public void setSwarmLineupTargetUuid(@Nullable UUID swarmLineupTargetUuid) {
        this.swarmLineupTargetUuid = swarmLineupTargetUuid;
    }

    @Nullable
    public Vec3d getSwarmLineupPos() {
        return swarmLineupPos;
    }

    public void setSwarmLineupPos(@Nullable Vec3d swarmLineupPos) {
        this.swarmLineupPos = swarmLineupPos;
    }

    public void clearSwarmLineup() {
        this.swarmLineupTargetUuid = null;
        this.swarmLineupPos = null;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("owner_uuid")) {
            this.ownerUuid = nbt.getUuid("owner_uuid");
        }
        this.setHivemindSwarmBee(nbt.getBoolean("hivemind_swarm"));
        this.setBloodwakeFly(nbt.getBoolean("bloodwake_fly"));
        if (nbt.containsUuid("swarm_target_uuid")) {
            this.swarmTargetUuid = nbt.getUuid("swarm_target_uuid");
        }
        this.swarmStingsRemaining = nbt.getInt("swarm_stings_remaining");
        this.swarmStingDamage = nbt.getFloat("swarm_sting_damage");
        this.swarmExpiryTick = nbt.getLong("swarm_expiry_tick");
        this.swarmNextStingTick = nbt.getLong("swarm_next_sting_tick");
        this.swarmNextDiveTick = nbt.getLong("swarm_next_dive_tick");
        if (nbt.containsUuid("swarm_anchor_uuid")) {
            this.swarmAnchorUuid = nbt.getUuid("swarm_anchor_uuid");
        }
        if (nbt.containsUuid("swarm_pass_target_uuid")) {
            this.swarmPassTargetUuid = nbt.getUuid("swarm_pass_target_uuid");
        }
        if (nbt.contains("swarm_pass_start_x") && nbt.contains("swarm_pass_start_y") && nbt.contains("swarm_pass_start_z")) {
            this.swarmPassStartPos = new Vec3d(
                    nbt.getDouble("swarm_pass_start_x"),
                    nbt.getDouble("swarm_pass_start_y"),
                    nbt.getDouble("swarm_pass_start_z")
            );
        }
        if (nbt.contains("swarm_pass_exit_x") && nbt.contains("swarm_pass_exit_y") && nbt.contains("swarm_pass_exit_z")) {
            this.swarmPassExitPos = new Vec3d(
                    nbt.getDouble("swarm_pass_exit_x"),
                    nbt.getDouble("swarm_pass_exit_y"),
                    nbt.getDouble("swarm_pass_exit_z")
            );
        }
        this.swarmPassStung = nbt.getBoolean("swarm_pass_stung");
        if (nbt.containsUuid("swarm_lineup_target_uuid")) {
            this.swarmLineupTargetUuid = nbt.getUuid("swarm_lineup_target_uuid");
        }
        if (nbt.contains("swarm_lineup_x") && nbt.contains("swarm_lineup_y") && nbt.contains("swarm_lineup_z")) {
            this.swarmLineupPos = new Vec3d(
                    nbt.getDouble("swarm_lineup_x"),
                    nbt.getDouble("swarm_lineup_y"),
                    nbt.getDouble("swarm_lineup_z")
            );
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        nbt.putBoolean("hivemind_swarm", this.isHivemindSwarmBee());
        nbt.putBoolean("bloodwake_fly", this.isBloodwakeFly());
        if (this.swarmTargetUuid != null) {
            nbt.putUuid("swarm_target_uuid", this.swarmTargetUuid);
        }
        nbt.putInt("swarm_stings_remaining", this.swarmStingsRemaining);
        nbt.putFloat("swarm_sting_damage", this.swarmStingDamage);
        nbt.putLong("swarm_expiry_tick", this.swarmExpiryTick);
        nbt.putLong("swarm_next_sting_tick", this.swarmNextStingTick);
        nbt.putLong("swarm_next_dive_tick", this.swarmNextDiveTick);
        if (this.swarmAnchorUuid != null) {
            nbt.putUuid("swarm_anchor_uuid", this.swarmAnchorUuid);
        }
        if (this.swarmPassTargetUuid != null) {
            nbt.putUuid("swarm_pass_target_uuid", this.swarmPassTargetUuid);
        }
        if (this.swarmPassStartPos != null) {
            nbt.putDouble("swarm_pass_start_x", this.swarmPassStartPos.x);
            nbt.putDouble("swarm_pass_start_y", this.swarmPassStartPos.y);
            nbt.putDouble("swarm_pass_start_z", this.swarmPassStartPos.z);
        }
        if (this.swarmPassExitPos != null) {
            nbt.putDouble("swarm_pass_exit_x", this.swarmPassExitPos.x);
            nbt.putDouble("swarm_pass_exit_y", this.swarmPassExitPos.y);
            nbt.putDouble("swarm_pass_exit_z", this.swarmPassExitPos.z);
        }
        nbt.putBoolean("swarm_pass_stung", this.swarmPassStung);
        if (this.swarmLineupTargetUuid != null) {
            nbt.putUuid("swarm_lineup_target_uuid", this.swarmLineupTargetUuid);
        }
        if (this.swarmLineupPos != null) {
            nbt.putDouble("swarm_lineup_x", this.swarmLineupPos.x);
            nbt.putDouble("swarm_lineup_y", this.swarmLineupPos.y);
            nbt.putDouble("swarm_lineup_z", this.swarmLineupPos.z);
        }
    }
}
