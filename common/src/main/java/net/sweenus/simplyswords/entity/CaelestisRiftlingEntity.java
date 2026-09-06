package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.world.CaelestisBreachManager;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CaelestisRiftlingEntity extends SpiderEntity implements CaelestisBreachCreature {

    private static final TrackedData<Boolean> UNBOUND =
            DataTracker.registerData(CaelestisRiftlingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> CORRUPTION_SEED =
            DataTracker.registerData(CaelestisRiftlingEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> OPEN_INVITATION_UNBOUND =
            DataTracker.registerData(CaelestisRiftlingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Long> OWNER_MARK_UNTIL =
            DataTracker.registerData(CaelestisRiftlingEntity.class, TrackedDataHandlerRegistry.LONG);
    private static final TrackedData<Integer> UNBOUND_REFUND_TICKS =
            DataTracker.registerData(CaelestisRiftlingEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> UNBOUND_ATTACK_DAMAGE =
            DataTracker.registerData(CaelestisRiftlingEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private UUID breachId;
    private UUID breachActorId;
    private UUID breachPrincipalId;

    public CaelestisRiftlingEntity(EntityType<? extends SpiderEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
        this.setCanPickUpLoot(false);
    }

    public static DefaultAttributeContainer.Builder createBreachAttributes() {
        return SpiderEntity.createSpiderAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.42)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new PounceAtTargetGoal(this, 0.42F));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.85));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
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
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient()) {
            if (!CaelestisBreachManager.tickCreature(this, this)) {
                return;
            }
            playWarpedAmbient();
        }
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
                    SoundCategory.HOSTILE, this.isUnbound() ? 0.44F : 0.22F,
                    this.isUnbound() ? 0.64F : 1.9F);
        }
        return damaged;
    }

    @Override
    public void onDeath(DamageSource source) {
        CaelestisBreachManager.handleCreatureDeath(this, this, source);
        super.onDeath(source);
    }

    private void playWarpedAmbient() {
        if (this.age % 120 != Math.floorMod(this.getId(), 120)
                || !(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        world.playSound(null, this.getBlockPos(), SoundRegistry.CAELESTIS_CREATURE_AMBIENT.get(),
                SoundCategory.HOSTILE, this.isUnbound() ? 0.34F : 0.16F,
                this.isUnbound() ? 0.68F : 1.92F);
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
    }
}
