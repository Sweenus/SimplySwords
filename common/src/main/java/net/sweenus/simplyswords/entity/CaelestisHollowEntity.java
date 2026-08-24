package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieEntity;
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

public class CaelestisHollowEntity extends ZombieEntity implements CaelestisBreachCreature {

    private static final TrackedData<Boolean> UNBOUND =
            DataTracker.registerData(CaelestisHollowEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> CORRUPTION_SEED =
            DataTracker.registerData(CaelestisHollowEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private UUID breachId;
    private UUID breachActorId;
    private UUID breachPrincipalId;

    public CaelestisHollowEntity(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
        this.setCanPickUpLoot(false);
        this.setBaby(false);
    }

    public static DefaultAttributeContainer.Builder createBreachAttributes() {
        return ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 28.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.05, true));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.72));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(UNBOUND, false);
        builder.add(CORRUPTION_SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        this.setBaby(false);
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
        boolean damaged = super.damage(source, amount);
        if (damaged && this.getWorld() instanceof ServerWorld world) {
            world.playSound(null, this.getBlockPos(), SoundRegistry.CAELESTIS_CREATURE_HURT.get(),
                    SoundCategory.HOSTILE, this.isUnbound() ? 0.52F : 0.28F,
                    this.isUnbound() ? 0.52F : 0.78F);
        }
        return damaged;
    }

    @Override
    public void onDeath(DamageSource source) {
        CaelestisBreachManager.handleCreatureDeath(this, this);
        super.onDeath(source);
    }

    private void playWarpedAmbient() {
        if (this.age % 145 != Math.floorMod(this.getId(), 145)
                || !(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        world.playSound(null, this.getBlockPos(), SoundRegistry.CAELESTIS_CREATURE_AMBIENT.get(),
                SoundCategory.HOSTILE, this.isUnbound() ? 0.4F : 0.2F,
                this.isUnbound() ? 0.5F : 0.72F);
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    protected boolean canConvertInWater() {
        return false;
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
    public void configureBreachCreature(UUID breachId, UUID actorId, UUID principalId,
                                        boolean unbound, int corruptionSeed) {
        this.breachId = breachId;
        this.breachActorId = actorId;
        this.breachPrincipalId = principalId;
        this.dataTracker.set(UNBOUND, unbound);
        this.dataTracker.set(CORRUPTION_SEED, corruptionSeed);
        this.setBaby(false);
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
        this.setBaby(false);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.breachId != null) nbt.putUuid("breach_id", this.breachId);
        if (this.breachActorId != null) nbt.putUuid("breach_actor_id", this.breachActorId);
        if (this.breachPrincipalId != null) nbt.putUuid("breach_principal_id", this.breachPrincipalId);
        nbt.putBoolean("unbound", this.isUnbound());
        nbt.putInt("corruption_seed", this.getCorruptionSeed());
    }
}
