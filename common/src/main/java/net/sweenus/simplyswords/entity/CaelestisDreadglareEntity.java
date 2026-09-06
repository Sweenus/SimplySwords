package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.world.CaelestisBreachManager;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CaelestisDreadglareEntity extends VexEntity implements CaelestisBreachCreature {

    public static final int STATE_ORBIT = 0;
    public static final int STATE_DIVE = 1;
    public static final int STATE_RECOVER = 2;

    private static final TrackedData<Boolean> UNBOUND =
            DataTracker.registerData(CaelestisDreadglareEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> CORRUPTION_SEED =
            DataTracker.registerData(CaelestisDreadglareEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> OPEN_INVITATION_UNBOUND =
            DataTracker.registerData(CaelestisDreadglareEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Long> OWNER_MARK_UNTIL =
            DataTracker.registerData(CaelestisDreadglareEntity.class, TrackedDataHandlerRegistry.LONG);
    private static final TrackedData<Integer> UNBOUND_REFUND_TICKS =
            DataTracker.registerData(CaelestisDreadglareEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> UNBOUND_ATTACK_DAMAGE =
            DataTracker.registerData(CaelestisDreadglareEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> FLIGHT_STATE =
            DataTracker.registerData(CaelestisDreadglareEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private UUID breachId;
    private UUID breachActorId;
    private UUID breachPrincipalId;
    private int stateTicks;
    private int orbitTicks;
    private float orbitAngle;

    public CaelestisDreadglareEntity(EntityType<? extends VexEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
        this.noClip = true;
        this.setNoGravity(true);
        this.setCanPickUpLoot(false);
    }

    public static DefaultAttributeContainer.Builder createBreachAttributes() {
        return VexEntity.createVexAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 14.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.40)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(UNBOUND, false);
        builder.add(CORRUPTION_SEED, 0);
        builder.add(OPEN_INVITATION_UNBOUND, false);
        builder.add(OWNER_MARK_UNTIL, 0L);
        builder.add(UNBOUND_REFUND_TICKS, 0);
        builder.add(UNBOUND_ATTACK_DAMAGE, 0.0F);
        builder.add(FLIGHT_STATE, STATE_ORBIT);
    }

    @Override
    public void tick() {
        this.noClip = true;
        this.setNoGravity(true);
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);

        if (!this.getWorld().isClient() && this.getWorld() instanceof ServerWorld world) {
            if (!CaelestisBreachManager.tickCreature(this, this)) {
                return;
            }
            tickFlight(world);
            playWarpedAmbient(world);
        }
    }

    private void tickFlight(ServerWorld world) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            setFlightState(STATE_ORBIT);
            this.stateTicks = 0;
            Vec3d center = CaelestisBreachManager.getBreachCenter(world, this.breachId);
            if (center != null) {
                this.orbitAngle += orbitDirection() * 0.045F;
                Vec3d idlePoint = center.add(
                        Math.cos(this.orbitAngle) * 3.0,
                        3.25 + Math.sin(this.age * 0.08F + this.orbitAngle) * 0.35,
                        Math.sin(this.orbitAngle) * 3.0
                );
                steerToward(constrainFlightPoint(world, idlePoint), 0.42);
            }
            return;
        }

        this.stateTicks++;
        switch (getFlightState()) {
            case STATE_DIVE -> tickDive(world, target);
            case STATE_RECOVER -> tickRecover(target);
            default -> tickOrbit(world, target);
        }
    }

    private void tickOrbit(ServerWorld world, LivingEntity target) {
        this.orbitAngle += orbitDirection() * 0.07F;
        double radius = 3.0 + Math.floorMod(this.getCorruptionSeed() >>> 4, 9) * 0.25;
        double height = target.getHeight() + 3.0
                + Math.floorMod(this.getCorruptionSeed() >>> 9, 7) * 0.22;
        Vec3d orbitPoint = target.getPos().add(
                Math.cos(this.orbitAngle) * radius,
                height + Math.sin(this.age * 0.11F + this.orbitAngle) * 0.45,
                Math.sin(this.orbitAngle) * radius
        );
        steerToward(constrainFlightPoint(world, orbitPoint), 0.51);

        if (this.stateTicks >= this.orbitTicks && this.squaredDistanceTo(target) <= 196.0) {
            setFlightState(STATE_DIVE);
            this.stateTicks = 0;
            this.setCharging(true);
            world.playSound(null, this.getBlockPos(), SoundRegistry.CAELESTIS_CREATURE_ATTACK.get(),
                    SoundCategory.HOSTILE, this.isUnbound() ? 0.48F : 0.3F,
                    this.isUnbound() ? 0.72F : 1.45F);
        }
    }

    private void tickDive(ServerWorld world, LivingEntity target) {
        Vec3d strikePoint = target.getPos().add(0.0, target.getHeight() * 0.58, 0.0);
        steerToward(constrainFlightPoint(world, strikePoint), 1.15);
        double reach = 0.75 + (this.getWidth() + target.getWidth()) * 0.5;
        if (this.squaredDistanceTo(strikePoint) <= reach * reach) {
            this.tryAttack(target);
            beginRecovery();
        } else if (this.stateTicks >= 24) {
            beginRecovery();
        }
    }

    private void tickRecover(LivingEntity target) {
        double height = target.getHeight() + 4.0;
        Vec3d recoverPoint = target.getPos().add(
                Math.cos(this.orbitAngle) * 2.0,
                height,
                Math.sin(this.orbitAngle) * 2.0
        );
        steerToward(constrainFlightPoint((ServerWorld) this.getWorld(), recoverPoint), 0.70);
        if (this.squaredDistanceTo(recoverPoint) <= 1.8 || this.stateTicks >= 28) {
            setFlightState(STATE_ORBIT);
            this.stateTicks = 0;
            this.orbitTicks = nextOrbitDuration();
            this.setCharging(false);
        }
    }

    private void beginRecovery() {
        setFlightState(STATE_RECOVER);
        this.stateTicks = 0;
        this.setCharging(false);
    }

    private void steerToward(Vec3d point, double speed) {
        Vec3d delta = point.subtract(this.getPos());
        if (delta.lengthSquared() < 0.0001) {
            return;
        }
        Vec3d desired = delta.normalize().multiply(speed);
        Vec3d velocity = this.getVelocity().multiply(0.58).add(desired.multiply(0.42));
        double length = velocity.length();
        if (length > speed) {
            velocity = velocity.multiply(speed / length);
        }
        this.setVelocity(velocity);
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * 57.295776) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(velocity.y, horizontal) * 57.295776);
        this.setYaw(yaw);
        this.bodyYaw = yaw;
        this.headYaw = yaw;
        this.setPitch(MathHelper.clamp(pitch, -70.0F, 70.0F));
    }

    private Vec3d constrainFlightPoint(ServerWorld world, Vec3d point) {
        return CaelestisBreachManager.constrainCreatureFlightPoint(
                world, this.breachId, point, this.getWidth() * 0.5 + 0.45);
    }

    private float orbitDirection() {
        return (this.getCorruptionSeed() & 1) == 0 ? 1.0F : -1.0F;
    }

    private int nextOrbitDuration() {
        return 45 + Math.floorMod(this.getCorruptionSeed() + this.age * 17, 31);
    }

    @Override
    public boolean tryAttack(Entity target) {
        this.swingHand(this.getActiveHand());
        return CaelestisBreachManager.tryCreatureAttack(this, target);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (CaelestisBreachManager.shouldIgnoreCreatureDamage(this, this, source)) {
            return false;
        }
        boolean damaged = super.damage(source, CaelestisBreachManager.modifyUnboundDamage(this, this, source, amount));
        if (damaged && this.getWorld() instanceof ServerWorld world) {
            world.playSound(null, this.getBlockPos(), SoundRegistry.CAELESTIS_CREATURE_HURT.get(),
                    SoundCategory.HOSTILE, this.isUnbound() ? 0.5F : 0.28F,
                    this.isUnbound() ? 0.7F : 1.55F);
        }
        return damaged;
    }

    @Override
    public void onDeath(DamageSource source) {
        CaelestisBreachManager.handleCreatureDeath(this, this, source);
        super.onDeath(source);
    }

    private void playWarpedAmbient(ServerWorld world) {
        if (this.age % 132 != Math.floorMod(this.getId(), 132)) {
            return;
        }
        world.playSound(null, this.getBlockPos(), SoundRegistry.CAELESTIS_CREATURE_AMBIENT.get(),
                SoundCategory.HOSTILE, this.isUnbound() ? 0.36F : 0.18F,
                this.isUnbound() ? 0.68F : 1.62F);
    }

    public int getFlightState() {
        return this.dataTracker.get(FLIGHT_STATE);
    }

    private void setFlightState(int state) {
        this.dataTracker.set(FLIGHT_STATE, Math.clamp(state, STATE_ORBIT, STATE_RECOVER));
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected int getXpToDrop() {
        return 0;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected boolean isDisallowedInPeaceful() {
        return false;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
    }

    @Override
    public UUID getBreachId() {
        return this.breachId;
    }

    @Override
    public UUID getBreachActorId() {
        return this.breachActorId;
    }

    @Override
    public UUID getBreachPrincipalId() {
        return this.breachPrincipalId;
    }

    @Override
    public boolean isUnbound() {
        return this.dataTracker.get(UNBOUND);
    }

    @Override
    public int getCorruptionSeed() {
        return this.dataTracker.get(CORRUPTION_SEED);
    }

    @Override
    public boolean isOpenInvitationUnbound() { return this.dataTracker.get(OPEN_INVITATION_UNBOUND); }

    @Override
    public void setOpenInvitationUnbound(boolean value) { this.dataTracker.set(OPEN_INVITATION_UNBOUND, value); }

    @Override
    public long getOwnerMarkUntil() { return this.dataTracker.get(OWNER_MARK_UNTIL); }

    @Override
    public void setOwnerMarkUntil(long value) { this.dataTracker.set(OWNER_MARK_UNTIL, value); }

    @Override
    public int getUnboundRefundTicks() { return this.dataTracker.get(UNBOUND_REFUND_TICKS); }

    @Override
    public void setUnboundRefundTicks(int value) { this.dataTracker.set(UNBOUND_REFUND_TICKS, value); }

    @Override
    public float getUnboundAttackDamage() { return this.dataTracker.get(UNBOUND_ATTACK_DAMAGE); }

    @Override
    public void setUnboundAttackDamage(float value) { this.dataTracker.set(UNBOUND_ATTACK_DAMAGE, value); }

    @Override
    public void configureBreachCreature(UUID breachId, UUID actorId, UUID principalId,
                                        boolean unbound, int corruptionSeed) {
        this.breachId = breachId;
        this.breachActorId = actorId;
        this.breachPrincipalId = principalId;
        this.dataTracker.set(UNBOUND, unbound);
        this.dataTracker.set(CORRUPTION_SEED, corruptionSeed);
        this.setFlightState(STATE_ORBIT);
        this.stateTicks = 0;
        this.orbitTicks = nextOrbitDuration();
        this.orbitAngle = Math.floorMod(corruptionSeed, 628) / 100.0F;
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.breachId = nbt.containsUuid("breach_id") ? nbt.getUuid("breach_id") : null;
        this.breachActorId = nbt.containsUuid("breach_actor_id") ? nbt.getUuid("breach_actor_id") : null;
        this.breachPrincipalId = nbt.containsUuid("breach_principal_id") ? nbt.getUuid("breach_principal_id") : null;
        this.dataTracker.set(UNBOUND, nbt.getBoolean("unbound"));
        this.dataTracker.set(CORRUPTION_SEED, nbt.getInt("corruption_seed"));
        this.setOpenInvitationUnbound(nbt.getBoolean("open_invitation_unbound"));
        this.setOwnerMarkUntil(nbt.getLong("owner_mark_until"));
        this.setUnboundRefundTicks(nbt.getInt("unbound_refund_ticks"));
        this.setUnboundAttackDamage(nbt.getFloat("unbound_attack_damage"));
        this.setFlightState(nbt.getInt("flight_state"));
        this.stateTicks = nbt.getInt("flight_state_ticks");
        this.orbitTicks = nbt.getInt("orbit_ticks");
        this.orbitAngle = nbt.getFloat("orbit_angle");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.breachId != null) nbt.putUuid("breach_id", this.breachId);
        if (this.breachActorId != null) nbt.putUuid("breach_actor_id", this.breachActorId);
        if (this.breachPrincipalId != null) nbt.putUuid("breach_principal_id", this.breachPrincipalId);
        nbt.putBoolean("unbound", this.isUnbound());
        nbt.putInt("corruption_seed", this.getCorruptionSeed());
        nbt.putBoolean("open_invitation_unbound", this.isOpenInvitationUnbound());
        nbt.putLong("owner_mark_until", this.getOwnerMarkUntil());
        nbt.putInt("unbound_refund_ticks", this.getUnboundRefundTicks());
        nbt.putFloat("unbound_attack_damage", this.getUnboundAttackDamage());
        nbt.putInt("flight_state", this.getFlightState());
        nbt.putInt("flight_state_ticks", this.stateTicks);
        nbt.putInt("orbit_ticks", this.orbitTicks);
        nbt.putFloat("orbit_angle", this.orbitAngle);
    }
}
