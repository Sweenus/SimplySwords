package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;
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
    private double masterySwarmRadius;
    private int masteryStingInterval;
    private int masterySlowAmplifier = -1;
    private int masteryPoisonTicks;
    private int masteryMode;
    private int masteryCooldownRefund;
    private int masterySearchCap;
    private UUID masteryReleaseId;
    private UUID masteryPreferredTargetUuid;
    private long masteryPreferredTargetExpiry;
    private double masteryPreferredTargetMultiplier = 1;
    private int masteryFocusStings;
    private float masteryFocusMultiplier = 1;
    private int masteryFocusTicks;
    private double masteryGuardRange;
    private float masteryGuardMultiplier = 1;
    private int masteryGuardLockout;
    private double masteryWarningRadius;
    private int masteryWarningDuration;
    private int masteryWarningLockout;
    private float masteryRetortMultiplier;
    private int masteryRetortLockout;
    private int masteryRallyStings;
    private int masteryRallyDuration;
    private int masteryEscortCount;
    private float masteryEscortSpeedMultiplier = 1;
    private float masterySaveThreshold;
    private int masterySaveAbsorption;
    private int masterySaveCap;
    private double masteryPhalanxRange;
    private int masteryPhalanxCount;
    private int masteryPhalanxAmplifier;
    private boolean masteryGuardDrone;

    public SimplySwordsBeeEntity(EntityType<? extends BeeEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setCanPickUpLoot(false);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HIVEMIND_SWARM, false);
        builder.add(BLOODWAKE_FLY, false);
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
        EntityAttributeInstance attackDamage = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        double baseDamage = attackDamage == null ? 0 : attackDamage.getBaseValue();
        if (attackDamage != null && masteryPreferredTargetUuid != null
                && masteryPreferredTargetUuid.equals(target.getUuid())
                && getWorld().getTime() <= masteryPreferredTargetExpiry) {
            attackDamage.setBaseValue(baseDamage * masteryPreferredTargetMultiplier);
        }
        boolean attacked = super.tryAttack(target);
        if (attackDamage != null) attackDamage.setBaseValue(baseDamage);
        if (attacked && masteryPoisonTicks > 0 && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,
                    masteryPoisonTicks, 0, false, true, true), this);
        }
        Entity owner = ownerUuid != null && getWorld() instanceof ServerWorld serverWorld
                ? serverWorld.getEntity(ownerUuid) : null;
        if (attacked && masteryCooldownRefund > 0 && target instanceof LivingEntity living
                && !living.isAlive() && owner instanceof PlayerEntity player
                && getWorld() instanceof ServerWorld world
                && net.sweenus.simplyswords.world.Phase7CombatManager.claimHiveRefund(
                        world, masteryReleaseId, world.getTime() + lifespan)) {
            SimplySwordsAPI.reduceWeaponCooldown(player,
                    new net.minecraft.item.ItemStack(ItemsRegistry.HIVEHEART.get()),
                    Config.uniqueEffects.hiveheart.cooldown, masteryCooldownRefund);
            masteryCooldownRefund = 0;
        }
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
    public boolean shouldSave() {
        return !isBloodwakeFly() && super.shouldSave();
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

    //I think this is just Entity.getWorld()? What even are mappings
    //@Override
    //public EntityView method_48926() {
        //return this.getWorld();
    //} 1.21

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
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        if (!isHivemindSwarmBee()) {
            super.dropEquipment(world, source, causedByPlayer);
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

    public double getMasterySwarmRadius() {
        return masterySwarmRadius;
    }

    public void setMasterySwarmRadius(double masterySwarmRadius) {
        this.masterySwarmRadius = Math.max(0, masterySwarmRadius);
    }

    public int getMasteryStingInterval() {
        return masteryStingInterval;
    }

    public void setMasteryStingInterval(int masteryStingInterval) {
        this.masteryStingInterval = Math.max(0, masteryStingInterval);
    }

    public int getMasterySlowAmplifier() {
        return masterySlowAmplifier;
    }

    public void setMasterySlowAmplifier(int masterySlowAmplifier) {
        this.masterySlowAmplifier = Math.max(-1, masterySlowAmplifier);
    }

    public void setMasteryPoisonTicks(int masteryPoisonTicks) {
        this.masteryPoisonTicks = Math.max(0, masteryPoisonTicks);
    }

    public int getMasteryMode() {
        return masteryMode;
    }

    public void setMasteryMode(int masteryMode) {
        this.masteryMode = Math.max(0, masteryMode);
    }

    public void setMasteryCooldownRefund(int masteryCooldownRefund) {
        this.masteryCooldownRefund = Math.max(0, masteryCooldownRefund);
    }

    public int getMasterySearchCap() {
        return masterySearchCap;
    }

    public void setMasterySearchCap(int masterySearchCap) {
        this.masterySearchCap = Math.max(0, masterySearchCap);
    }

    public void setMasteryReleaseId(@Nullable UUID masteryReleaseId) {
        this.masteryReleaseId = masteryReleaseId;
    }

    public void setMasteryTargetRangeBonus(double bonus) {
        EntityAttributeInstance range = getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
        if (range != null) range.setBaseValue(48 + Math.max(0, bonus));
    }

    public void configureMasteryPreferredTarget(@Nullable UUID targetUuid, long expiry,
                                                double damageMultiplier) {
        this.masteryPreferredTargetUuid = targetUuid;
        this.masteryPreferredTargetExpiry = Math.max(0, expiry);
        this.masteryPreferredTargetMultiplier = Math.max(1, damageMultiplier);
    }

    public void configureMasterySwarmCombat(int focusStings, double focusMultiplier, int focusTicks,
                                            double guardRange, double guardMultiplier, int guardLockout,
                                            double warningRadius, int warningDuration, int warningLockout,
                                            double retortMultiplier, int retortLockout) {
        masteryFocusStings = Math.max(0, focusStings);
        masteryFocusMultiplier = (float) Math.max(1, focusMultiplier);
        masteryFocusTicks = Math.max(0, focusTicks);
        masteryGuardRange = Math.max(0, guardRange);
        masteryGuardMultiplier = (float) Math.clamp(guardMultiplier, 0, 1);
        masteryGuardLockout = Math.max(0, guardLockout);
        masteryWarningRadius = Math.max(0, warningRadius);
        masteryWarningDuration = Math.max(0, warningDuration);
        masteryWarningLockout = Math.max(0, warningLockout);
        masteryRetortMultiplier = (float) Math.max(0, retortMultiplier);
        masteryRetortLockout = Math.max(0, retortLockout);
    }

    public void configureMasteryRoyalGuard(int rallyStings, int rallyDuration, int escortCount,
                                           double escortSpeedMultiplier, double saveThreshold,
                                           int saveAbsorption, int saveCap, double phalanxRange,
                                           int phalanxCount, int phalanxAmplifier) {
        masteryRallyStings = Math.max(0, rallyStings);
        masteryRallyDuration = Math.max(0, rallyDuration);
        masteryEscortCount = Math.max(0, escortCount);
        masteryEscortSpeedMultiplier = (float) Math.max(1, escortSpeedMultiplier);
        masterySaveThreshold = (float) Math.clamp(saveThreshold, 0, 1);
        masterySaveAbsorption = Math.max(0, saveAbsorption);
        masterySaveCap = Math.max(0, saveCap);
        masteryPhalanxRange = Math.max(0, phalanxRange);
        masteryPhalanxCount = Math.max(0, phalanxCount);
        masteryPhalanxAmplifier = Math.max(0, phalanxAmplifier);
    }

    public int getMasteryFocusStings() { return masteryFocusStings; }
    public float getMasteryFocusMultiplier() { return masteryFocusMultiplier; }
    public int getMasteryFocusTicks() { return masteryFocusTicks; }
    public double getMasteryGuardRange() { return masteryGuardRange; }
    public float getMasteryGuardMultiplier() { return masteryGuardMultiplier; }
    public int getMasteryGuardLockout() { return masteryGuardLockout; }
    public double getMasteryWarningRadius() { return masteryWarningRadius; }
    public int getMasteryWarningDuration() { return masteryWarningDuration; }
    public int getMasteryWarningLockout() { return masteryWarningLockout; }
    public float getMasteryRetortMultiplier() { return masteryRetortMultiplier; }
    public int getMasteryRetortLockout() { return masteryRetortLockout; }
    public int getMasteryRallyStings() { return masteryRallyStings; }
    public int getMasteryRallyDuration() { return masteryRallyDuration; }
    public int getMasteryEscortCount() { return masteryEscortCount; }
    public float getMasteryEscortSpeedMultiplier() { return masteryEscortSpeedMultiplier; }
    public float getMasterySaveThreshold() { return masterySaveThreshold; }
    public int getMasterySaveAbsorption() { return masterySaveAbsorption; }
    public int getMasterySaveCap() { return masterySaveCap; }
    public double getMasteryPhalanxRange() { return masteryPhalanxRange; }
    public int getMasteryPhalanxCount() { return masteryPhalanxCount; }
    public int getMasteryPhalanxAmplifier() { return masteryPhalanxAmplifier; }
    public boolean isMasteryGuardDrone() { return masteryGuardDrone; }
    public void setMasteryGuardDrone(boolean guardDrone) { masteryGuardDrone = guardDrone; }

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
        this.masterySwarmRadius = nbt.getDouble("mastery_swarm_radius");
        this.masteryStingInterval = nbt.getInt("mastery_sting_interval");
        this.masterySlowAmplifier = nbt.contains("mastery_slow_amplifier")
                ? nbt.getInt("mastery_slow_amplifier") : -1;
        this.masteryPoisonTicks = nbt.getInt("mastery_poison_ticks");
        this.masteryMode = nbt.getInt("mastery_mode");
        this.masteryCooldownRefund = nbt.getInt("mastery_cooldown_refund");
        this.masterySearchCap = nbt.getInt("mastery_search_cap");
        this.masteryReleaseId = nbt.containsUuid("mastery_release_id")
                ? nbt.getUuid("mastery_release_id") : null;
        this.masteryPreferredTargetUuid = nbt.containsUuid("mastery_preferred_target_uuid")
                ? nbt.getUuid("mastery_preferred_target_uuid") : null;
        this.masteryPreferredTargetExpiry = nbt.getLong("mastery_preferred_target_expiry");
        this.masteryPreferredTargetMultiplier = nbt.contains("mastery_preferred_target_multiplier")
                ? nbt.getDouble("mastery_preferred_target_multiplier") : 1;
        masteryFocusStings = nbt.getInt("mastery_focus_stings");
        masteryFocusMultiplier = nbt.contains("mastery_focus_multiplier") ? nbt.getFloat("mastery_focus_multiplier") : 1;
        masteryFocusTicks = nbt.getInt("mastery_focus_ticks");
        masteryGuardRange = nbt.getDouble("mastery_guard_range");
        masteryGuardMultiplier = nbt.contains("mastery_guard_multiplier") ? nbt.getFloat("mastery_guard_multiplier") : 1;
        masteryGuardLockout = nbt.getInt("mastery_guard_lockout");
        masteryWarningRadius = nbt.getDouble("mastery_warning_radius");
        masteryWarningDuration = nbt.getInt("mastery_warning_duration");
        masteryWarningLockout = nbt.getInt("mastery_warning_lockout");
        masteryRetortMultiplier = nbt.getFloat("mastery_retort_multiplier");
        masteryRetortLockout = nbt.getInt("mastery_retort_lockout");
        masteryRallyStings = nbt.getInt("mastery_rally_stings");
        masteryRallyDuration = nbt.getInt("mastery_rally_duration");
        masteryEscortCount = nbt.getInt("mastery_escort_count");
        masteryEscortSpeedMultiplier = nbt.contains("mastery_escort_speed") ? nbt.getFloat("mastery_escort_speed") : 1;
        masterySaveThreshold = nbt.getFloat("mastery_save_threshold");
        masterySaveAbsorption = nbt.getInt("mastery_save_absorption");
        masterySaveCap = nbt.getInt("mastery_save_cap");
        masteryPhalanxRange = nbt.getDouble("mastery_phalanx_range");
        masteryPhalanxCount = nbt.getInt("mastery_phalanx_count");
        masteryPhalanxAmplifier = nbt.getInt("mastery_phalanx_amplifier");
        masteryGuardDrone = nbt.getBoolean("mastery_guard_drone");
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
        nbt.putDouble("mastery_swarm_radius", this.masterySwarmRadius);
        nbt.putInt("mastery_sting_interval", this.masteryStingInterval);
        nbt.putInt("mastery_slow_amplifier", this.masterySlowAmplifier);
        nbt.putInt("mastery_poison_ticks", this.masteryPoisonTicks);
        nbt.putInt("mastery_mode", this.masteryMode);
        nbt.putInt("mastery_cooldown_refund", this.masteryCooldownRefund);
        nbt.putInt("mastery_search_cap", this.masterySearchCap);
        if (this.masteryReleaseId != null) nbt.putUuid("mastery_release_id", this.masteryReleaseId);
        if (this.masteryPreferredTargetUuid != null) {
            nbt.putUuid("mastery_preferred_target_uuid", this.masteryPreferredTargetUuid);
        }
        nbt.putLong("mastery_preferred_target_expiry", this.masteryPreferredTargetExpiry);
        nbt.putDouble("mastery_preferred_target_multiplier", this.masteryPreferredTargetMultiplier);
        nbt.putInt("mastery_focus_stings", masteryFocusStings);
        nbt.putFloat("mastery_focus_multiplier", masteryFocusMultiplier);
        nbt.putInt("mastery_focus_ticks", masteryFocusTicks);
        nbt.putDouble("mastery_guard_range", masteryGuardRange);
        nbt.putFloat("mastery_guard_multiplier", masteryGuardMultiplier);
        nbt.putInt("mastery_guard_lockout", masteryGuardLockout);
        nbt.putDouble("mastery_warning_radius", masteryWarningRadius);
        nbt.putInt("mastery_warning_duration", masteryWarningDuration);
        nbt.putInt("mastery_warning_lockout", masteryWarningLockout);
        nbt.putFloat("mastery_retort_multiplier", masteryRetortMultiplier);
        nbt.putInt("mastery_retort_lockout", masteryRetortLockout);
        nbt.putInt("mastery_rally_stings", masteryRallyStings);
        nbt.putInt("mastery_rally_duration", masteryRallyDuration);
        nbt.putInt("mastery_escort_count", masteryEscortCount);
        nbt.putFloat("mastery_escort_speed", masteryEscortSpeedMultiplier);
        nbt.putFloat("mastery_save_threshold", masterySaveThreshold);
        nbt.putInt("mastery_save_absorption", masterySaveAbsorption);
        nbt.putInt("mastery_save_cap", masterySaveCap);
        nbt.putDouble("mastery_phalanx_range", masteryPhalanxRange);
        nbt.putInt("mastery_phalanx_count", masteryPhalanxCount);
        nbt.putInt("mastery_phalanx_amplifier", masteryPhalanxAmplifier);
        nbt.putBoolean("mastery_guard_drone", masteryGuardDrone);
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
