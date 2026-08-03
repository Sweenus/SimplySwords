package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.world.DragonMawManager;

import java.util.Comparator;
import java.util.UUID;

public class DragonMawHeadVisualEntity extends Entity {

    public static final float EFFECTIVE_SCALE_MULTIPLIER = 0.5F;

    private static final TrackedData<Integer> OWNER_ENTITY_ID = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ENTITY_ID = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<ItemStack> WEAPON_STACK = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> BREATH_DELAY = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> BREATH_REPEAT = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SCALE = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TARGET_YAW = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TARGET_PITCH = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DAMAGE = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TURN_SPEED = DataTracker.registerData(DragonMawHeadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private UUID ownerUuid;
    private UUID targetUuid;
    private int nextBreathAge;

    public DragonMawHeadVisualEntity(EntityType<? extends DragonMawHeadVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public DragonMawHeadVisualEntity(ServerWorld world, LivingEntity owner, LivingEntity target, ItemStack stack,
                                     float damage, int lifetime, int breathDelay, int breathRepeat, float scale, float turnSpeed) {
        this(EntityRegistry.DRAGON_MAW_HEAD_VISUAL.get(), world);
        this.ownerUuid = owner.getUuid();
        this.targetUuid = target.getUuid();
        this.dataTracker.set(OWNER_ENTITY_ID, owner.getId());
        this.dataTracker.set(TARGET_ENTITY_ID, target.getId());
        this.dataTracker.set(WEAPON_STACK, stack.copy());
        this.dataTracker.set(DAMAGE, Math.max(0.0F, damage));
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
        this.dataTracker.set(BREATH_DELAY, Math.max(0, breathDelay));
        this.dataTracker.set(BREATH_REPEAT, Math.max(1, breathRepeat));
        this.dataTracker.set(SCALE, Math.max(0.1F, scale));
        this.dataTracker.set(TURN_SPEED, Math.max(0.1F, turnSpeed));
        this.nextBreathAge = Math.max(0, breathDelay);
        this.setPosition(getAnchorPosition(owner));
        this.setYaw(owner.getHeadYaw());
        this.dataTracker.set(TARGET_YAW, owner.getHeadYaw());
        this.dataTracker.set(TARGET_PITCH, owner.getPitch());
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(OWNER_ENTITY_ID, -1);
        this.dataTracker.startTracking(TARGET_ENTITY_ID, -1);
        this.dataTracker.startTracking(WEAPON_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(LIFETIME, 300);
        this.dataTracker.startTracking(BREATH_DELAY, 12);
        this.dataTracker.startTracking(BREATH_REPEAT, 60);
        this.dataTracker.startTracking(SCALE, 2.35F);
        this.dataTracker.startTracking(TARGET_YAW, 0.0F);
        this.dataTracker.startTracking(TARGET_PITCH, 0.0F);
        this.dataTracker.startTracking(DAMAGE, 0.0F);
        this.dataTracker.startTracking(TURN_SPEED, 6.0F);
    }

    @Override
    public void tick() {
        super.tick();

        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
            this.setPosition(getAnchorPosition(livingOwner));
        } else if (!this.getWorld().isClient()) {
            this.discard();
            return;
        }

        LivingEntity target = null;
        if (!this.getWorld().isClient() && owner instanceof LivingEntity livingOwner) {
            target = this.retarget(livingOwner);
            if (target == null && this.age > this.getLifetime()) {
                this.discard();
                return;
            }
        }

        this.updateRotation(owner);

        if (!this.getWorld().isClient()
                && target != null
                && this.age >= this.nextBreathAge) {
            this.spawnBreath();
            this.nextBreathAge = this.age + this.getBreathRepeat();
        }
    }

    private LivingEntity retarget(LivingEntity owner) {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return null;
        }

        Entity currentTarget = this.getTarget();
        if (currentTarget instanceof LivingEntity livingTarget && this.isValidTarget(owner, livingTarget)) {
            return livingTarget;
        }

        LivingEntity newTarget = this.findNearestTarget(world, owner);
        if (newTarget == null) {
            this.dataTracker.set(TARGET_ENTITY_ID, -1);
            this.targetUuid = null;
            return null;
        }

        this.dataTracker.set(TARGET_ENTITY_ID, newTarget.getId());
        this.targetUuid = newTarget.getUuid();
        return newTarget;
    }

    private LivingEntity findNearestTarget(ServerWorld world, LivingEntity owner) {
        double range = Math.max(0.5, Config.gemPowers.dragonMaw.range);
        Box searchBox = owner.getBoundingBox().expand(range, Math.max(2.0, range * 0.5), range);
        return world.getEntitiesByClass(LivingEntity.class, searchBox, target -> this.isValidTarget(owner, target))
                .stream()
                .min(Comparator.comparingDouble(owner::squaredDistanceTo))
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity owner, LivingEntity target) {
        double range = Math.max(0.5, Config.gemPowers.dragonMaw.range);
        return DragonMawManager.isValidTarget(owner, target)
                && target.squaredDistanceTo(owner) <= range * range;
    }

    private void updateRotation(Entity owner) {
        Entity target = this.getTarget();
        Vec3d from = this.getPos();
        Vec3d to;
        if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
            to = livingTarget.getPos().add(0.0, livingTarget.getHeight() * 0.55, 0.0);
        } else if (owner instanceof LivingEntity livingOwner) {
            to = from.add(livingOwner.getRotationVec(1.0F).multiply(4.0));
        } else {
            return;
        }

        Vec3d direction = to.subtract(from);
        if (direction.lengthSquared() < 1.0E-5) {
            return;
        }

        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float desiredYaw = (float) (MathHelper.atan2(direction.z, direction.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
        float desiredPitch = (float) (-(MathHelper.atan2(direction.y, horizontal) * MathHelper.DEGREES_PER_RADIAN));
        float turnSpeed = this.getTurnSpeed();
        float nextYaw = MathHelper.stepUnwrappedAngleTowards(this.getTargetYaw(), desiredYaw, turnSpeed);
        float nextPitch = MathHelper.stepTowards(this.getTargetPitch(), MathHelper.clamp(desiredPitch, -55.0F, 55.0F), turnSpeed);

        this.dataTracker.set(TARGET_YAW, nextYaw);
        this.dataTracker.set(TARGET_PITCH, nextPitch);
        this.setYaw(nextYaw);
        this.setPitch(nextPitch);
    }

    private void spawnBreath() {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Entity owner = this.getOwner();
        Entity target = this.getTarget();
        if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()) {
            return;
        }

        Vec3d mouth = this.getMouthPosition();
        Vec3d breathPos;
        if (target instanceof LivingEntity livingTarget && this.isValidTarget(livingOwner, livingTarget)) {
            breathPos = livingTarget.getPos();
        } else {
            return;
        }

        DragonMawBreathCloudEntity cloud = new DragonMawBreathCloudEntity(world, livingOwner, this.dataTracker.get(WEAPON_STACK),
                this.dataTracker.get(DAMAGE), mouth, breathPos,
                Math.max(0.25, Config.gemPowers.dragonMaw.breathRadius),
                Math.max(1, Config.gemPowers.dragonMaw.breathCloudDurationTicks),
                Math.max(1, Config.gemPowers.dragonMaw.damageIntervalTicks));
        world.spawnEntity(cloud);

        Vec3d direction = breathPos.subtract(mouth);
        if (direction.lengthSquared() > 1.0E-5) {
            direction = direction.normalize();
        }
        for (int i = 1; i <= 8; i++) {
            Vec3d pos = mouth.add(direction.multiply(i * 0.45));
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 1, 0.08, 0.05, 0.08, 0.012);
            }
            if (i % 4 == 0) {
                world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.12, 0.08, 0.12, 0.01);
            }
        }

        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 0.9F, 0.85F);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.55F, 1.45F);
    }

    public static Vec3d getAnchorPosition(LivingEntity owner) {
        return new Vec3d(owner.getX(), owner.getEyeY() + Config.gemPowers.dragonMaw.headVerticalOffset, owner.getZ());
    }

    public Vec3d getMouthPosition() {
        return this.getPos().add(this.getRotationVec(1.0F).multiply(1.35 * this.getEffectiveScale()));
    }

    public Entity getOwner() {
        int ownerId = this.getOwnerEntityId();
        Entity owner = ownerId >= 0 ? this.getWorld().getEntityById(ownerId) : null;
        if (owner != null || this.ownerUuid == null || !(this.getWorld() instanceof ServerWorld world)) {
            return owner;
        }
        return world.getEntity(this.ownerUuid);
    }

    public Entity getTarget() {
        int targetId = this.dataTracker.get(TARGET_ENTITY_ID);
        Entity target = targetId >= 0 ? this.getWorld().getEntityById(targetId) : null;
        if (target != null || this.targetUuid == null || !(this.getWorld() instanceof ServerWorld world)) {
            return target;
        }
        return world.getEntity(this.targetUuid);
    }

    public int getOwnerEntityId() {
        return this.dataTracker.get(OWNER_ENTITY_ID);
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public int getBreathDelay() {
        return this.dataTracker.get(BREATH_DELAY);
    }

    public int getBreathRepeat() {
        return this.dataTracker.get(BREATH_REPEAT);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public float getEffectiveScale() {
        return this.getScale() * EFFECTIVE_SCALE_MULTIPLIER;
    }

    public float getTargetYaw() {
        return this.dataTracker.get(TARGET_YAW);
    }

    public float getTargetPitch() {
        return this.dataTracker.get(TARGET_PITCH);
    }

    public float getTurnSpeed() {
        return this.dataTracker.get(TURN_SPEED);
    }

    public float getMouthOpenProgress(float tickDelta) {
        float ageWithDelta = this.age + tickDelta;
        float firstDelay = Math.max(1.0F, this.getBreathDelay());
        if (ageWithDelta < firstDelay) {
            return MathHelper.clamp(ageWithDelta / firstDelay, 0.0F, 1.0F);
        }

        float repeat = Math.max(1.0F, this.getBreathRepeat());
        float phase = (ageWithDelta - firstDelay) % repeat;
        if (phase < 8.0F) {
            return 1.0F;
        }
        if (phase < 16.0F) {
            return 1.0F - MathHelper.clamp((phase - 8.0F) / 8.0F, 0.0F, 1.0F);
        }
        return 0.0F;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.ownerUuid = nbt.containsUuid("owner_uuid") ? nbt.getUuid("owner_uuid") : null;
        this.targetUuid = nbt.containsUuid("target_uuid") ? nbt.getUuid("target_uuid") : null;
        if (nbt.contains("owner_id")) {
            this.dataTracker.set(OWNER_ENTITY_ID, nbt.getInt("owner_id"));
        }
        if (nbt.contains("target_id")) {
            this.dataTracker.set(TARGET_ENTITY_ID, nbt.getInt("target_id"));
        }
        if (nbt.contains("weapon_stack")) {
            this.dataTracker.set(WEAPON_STACK, ItemStack.fromNbt(nbt.getCompound("weapon_stack")));
        }
        this.dataTracker.set(LIFETIME, nbt.getInt("lifetime"));
        this.dataTracker.set(BREATH_DELAY, nbt.getInt("breath_delay"));
        if (nbt.contains("breath_repeat")) {
            this.dataTracker.set(BREATH_REPEAT, nbt.getInt("breath_repeat"));
        }
        this.dataTracker.set(SCALE, nbt.getFloat("scale"));
        this.dataTracker.set(TARGET_YAW, nbt.getFloat("target_yaw"));
        this.dataTracker.set(TARGET_PITCH, nbt.getFloat("target_pitch"));
        this.dataTracker.set(DAMAGE, nbt.getFloat("damage"));
        this.dataTracker.set(TURN_SPEED, nbt.getFloat("turn_speed"));
        this.nextBreathAge = nbt.contains("next_breath_age") ? nbt.getInt("next_breath_age") : this.getBreathDelay();
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            nbt.putUuid("target_uuid", this.targetUuid);
        }
        nbt.putInt("owner_id", this.dataTracker.get(OWNER_ENTITY_ID));
        nbt.putInt("target_id", this.dataTracker.get(TARGET_ENTITY_ID));
        ItemStack stack = this.dataTracker.get(WEAPON_STACK);
        if (!stack.isEmpty()) {
            nbt.put("weapon_stack", stack.writeNbt(new NbtCompound()));
        }
        nbt.putInt("lifetime", this.dataTracker.get(LIFETIME));
        nbt.putInt("breath_delay", this.dataTracker.get(BREATH_DELAY));
        nbt.putInt("breath_repeat", this.dataTracker.get(BREATH_REPEAT));
        nbt.putFloat("scale", this.dataTracker.get(SCALE));
        nbt.putFloat("target_yaw", this.dataTracker.get(TARGET_YAW));
        nbt.putFloat("target_pitch", this.dataTracker.get(TARGET_PITCH));
        nbt.putFloat("damage", this.dataTracker.get(DAMAGE));
        nbt.putFloat("turn_speed", this.dataTracker.get(TURN_SPEED));
        nbt.putInt("next_breath_age", this.nextBreathAge);
    }
}
