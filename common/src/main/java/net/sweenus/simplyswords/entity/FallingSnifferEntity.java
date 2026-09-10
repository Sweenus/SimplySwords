package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.power.powers.SnifferSlamPower;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FallingSnifferEntity extends SnifferEntity implements SimplySwordsMinion {

    private static final int DIGGING_POSE_SKIP_TICKS = 32;
    private static final TrackedData<ItemStack> WEAPON_STACK = DataTracker.registerData(FallingSnifferEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(FallingSnifferEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ID = DataTracker.registerData(FallingSnifferEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IMPACTED = DataTracker.registerData(FallingSnifferEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> LINGER_TICKS = DataTracker.registerData(FallingSnifferEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private UUID ownerUuid;
    private UUID targetUuid;
    private float damage;
    private double fallSpeed = 0.75;
    private double impactRadius = 2.0;
    private int immobiliseDurationTicks = 20;
    private long discardAtTick;
    private final Set<UUID> hitTargets = new HashSet<>();

    public FallingSnifferEntity(EntityType<? extends FallingSnifferEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setAiDisabled(true);
        this.setPersistent();
        this.startState(SnifferEntity.State.DIGGING);
    }

    public FallingSnifferEntity(ServerWorld world, LivingEntity owner, LivingEntity target, ItemStack stack,
                                float damage, double fallHeight, double fallSpeed, double impactRadius,
                                int immobiliseDurationTicks, int lingerTicks) {
        this(EntityRegistry.FALLING_SNIFFER.get(), world);
        this.ownerUuid = owner.getUuid();
        this.targetUuid = target.getUuid();
        this.dataTracker.set(OWNER_ID, owner.getId());
        this.dataTracker.set(TARGET_ID, target.getId());
        this.dataTracker.set(WEAPON_STACK, stack.copy());
        CombatProvenanceApi.attach(this, stack);
        this.dataTracker.set(LINGER_TICKS, Math.max(1, lingerTicks));
        this.damage = Math.max(0.0F, damage);
        this.fallSpeed = Math.max(0.05, fallSpeed);
        this.impactRadius = Math.max(0.25, impactRadius);
        this.immobiliseDurationTicks = Math.max(1, immobiliseDurationTicks);
        this.refreshPositionAndAngles(target.getX(), target.getY() + Math.max(1.0, fallHeight), target.getZ(), target.getYaw(), 0.0F);
        this.setYaw(target.getYaw());
        this.setBodyYaw(target.getYaw());
        this.setHeadYaw(target.getYaw());
    }

    public static DefaultAttributeContainer.Builder createFallingSnifferAttributes() {
        return SnifferEntity.createSnifferAttributes();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(WEAPON_STACK, ItemStack.EMPTY);
        builder.add(OWNER_ID, -1);
        builder.add(TARGET_ID, -1);
        builder.add(IMPACTED, false);
        builder.add(LINGER_TICKS, 20);
    }

    @Override
    public void tick() {
        super.tick();
        this.forceFlatDiggingPose(0.0F);

        if (this.getWorld().isClient()) {
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld world) || getOwner(world) == null) {
            this.discard();
            return;
        }

        if (this.dataTracker.get(IMPACTED)) {
            this.setVelocity(Vec3d.ZERO);
            if (world.getTime() >= this.discardAtTick) {
                expire(world);
            }
            return;
        }

        Entity target = getTarget(world);
        if (target != null && target.isAlive()) {
            this.setPosition(target.getX(), this.getY(), target.getZ());
            this.setYaw(target.getYaw());
            this.setBodyYaw(target.getYaw());
            this.setHeadYaw(target.getYaw());
        }

        Vec3d velocity = new Vec3d(0.0, -this.fallSpeed, 0.0);
        this.setVelocity(velocity);
        this.move(MovementType.SELF, velocity);

        double impactY = target == null ? this.getY() : target.getY();
        if (this.isOnGround() || this.getY() <= impactY + 0.05) {
            this.setPosition(this.getX(), impactY, this.getZ());
            impact(world);
        }
    }

    public void forceFlatDiggingPose(float tickDelta) {
        if (!this.isDiggingOrSearching()) {
            this.startState(SnifferEntity.State.DIGGING);
        }
        int renderTick = MathHelper.floor(this.age + tickDelta);
        this.diggingAnimationState.start(renderTick - DIGGING_POSE_SKIP_TICKS);
    }

    private void impact(ServerWorld world) {
        this.dataTracker.set(IMPACTED, true);
        this.discardAtTick = world.getTime() + Math.max(1, this.dataTracker.get(LINGER_TICKS));
        this.setVelocity(Vec3d.ZERO);
        this.velocityModified = true;

        LivingEntity owner = getOwner(world);
        if (owner != null) {
            ItemStack stack = this.dataTracker.get(WEAPON_STACK);
            Box searchBox = this.getBoundingBox().expand(this.impactRadius, 0.5, this.impactRadius);
            for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox, this::isValidTarget)) {
                if (!this.hitTargets.add(target.getUuid())) {
                    continue;
                }
                boolean[] damaged = {false};
                SnifferSlamPower.runSuppressed(() -> damaged[0] = SimplySwordsAPI.applyDelegatedWeaponHit(stack, target, owner, this, this.damage));
                if (damaged[0]) {
                    target.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FREEZE),
                            this.immobiliseDurationTicks, 0, false, false, true), owner);
                }
            }
        }

        playImpactEffects(world);
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target == this) {
            return false;
        }
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        LivingEntity owner = getOwner(world);
        return owner != null && target != owner && HelperMethods.checkFriendlyFire(target, owner);
    }

    private void playImpactEffects(ServerWorld world) {
        world.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getBodyY(0.15), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.POOF, this.getX(), this.getBodyY(0.35), this.getZ(), 24, 0.8, 0.18, 0.8, 0.06);
        world.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getBodyY(0.35), this.getZ(), 16, 0.7, 0.2, 0.7, 0.04);
        spawnBodyImpactParticles(world);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.45F, 0.6F + world.random.nextFloat() * 0.08F);
    }

    private void spawnBodyImpactParticles(ServerWorld world) {
        double radiusX = Math.max(0.9, this.getWidth() * 0.85);
        double radiusZ = Math.max(1.4, this.getWidth() * 1.35);
        double y = this.getY() + 0.12;

        for (int i = 0; i < 28; i++) {
            double angle = MathHelper.TAU * i / 28.0;
            double x = this.getX() + Math.cos(angle) * radiusX;
            double z = this.getZ() + Math.sin(angle) * radiusZ;
            world.spawnParticles(ParticleTypes.POOF, x, y, z, 3, 0.15, 0.04, 0.15, 0.04);
            world.spawnParticles(ParticleTypes.CLOUD, x, y, z, 2, 0.12, 0.03, 0.12, 0.025);
        }

        for (int i = 0; i < 18; i++) {
            double x = this.getX() + (world.random.nextDouble() * 2.0 - 1.0) * radiusX;
            double z = this.getZ() + (world.random.nextDouble() * 2.0 - 1.0) * radiusZ;
            world.spawnParticles(ParticleTypes.CRIT, x, y + 0.1, z, 2, 0.1, 0.05, 0.1, 0.03);
        }
    }

    private void expire(ServerWorld world) {
        world.spawnParticles(ParticleTypes.POOF, this.getX(), this.getBodyY(0.5), this.getZ(), 14, 0.5, 0.25, 0.5, 0.04);
        this.discard();
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Nullable
    private LivingEntity getOwner(ServerWorld world) {
        int ownerId = this.dataTracker.get(OWNER_ID);
        Entity owner = ownerId >= 0 ? world.getEntityById(ownerId) : null;
        if (owner instanceof LivingEntity livingOwner) {
            return livingOwner;
        }
        if (this.ownerUuid == null) {
            return null;
        }
        Entity entity = world.getEntity(this.ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private Entity getTarget(ServerWorld world) {
        int targetId = this.dataTracker.get(TARGET_ID);
        Entity target = targetId >= 0 ? world.getEntityById(targetId) : null;
        if (target != null || this.targetUuid == null) {
            return target;
        }
        return world.getEntity(this.targetUuid);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return ActionResult.FAIL;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    public void breed(ServerWorld world, AnimalEntity other) {
    }

    @Override
    public void breed(ServerWorld world, AnimalEntity other, @Nullable PassiveEntity baby) {
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.ownerUuid = nbt.containsUuid("owner_uuid") ? nbt.getUuid("owner_uuid") : null;
        this.targetUuid = nbt.containsUuid("target_uuid") ? nbt.getUuid("target_uuid") : null;
        this.damage = nbt.getFloat("damage");
        this.fallSpeed = nbt.getDouble("fall_speed");
        this.impactRadius = nbt.getDouble("impact_radius");
        this.immobiliseDurationTicks = nbt.getInt("immobilise_duration_ticks");
        this.discardAtTick = nbt.getLong("discard_at_tick");
        if (nbt.contains("weapon_stack")) {
            this.dataTracker.set(WEAPON_STACK, ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("weapon_stack")).orElse(ItemStack.EMPTY));
        }
        if (nbt.contains("owner_id")) {
            this.dataTracker.set(OWNER_ID, nbt.getInt("owner_id"));
        }
        if (nbt.contains("target_id")) {
            this.dataTracker.set(TARGET_ID, nbt.getInt("target_id"));
        }
        if (nbt.contains("impacted")) {
            this.dataTracker.set(IMPACTED, nbt.getBoolean("impacted"));
        }
        if (nbt.contains("linger_ticks")) {
            this.dataTracker.set(LINGER_TICKS, nbt.getInt("linger_ticks"));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            nbt.putUuid("target_uuid", this.targetUuid);
        }
        nbt.putFloat("damage", this.damage);
        nbt.putDouble("fall_speed", this.fallSpeed);
        nbt.putDouble("impact_radius", this.impactRadius);
        nbt.putInt("immobilise_duration_ticks", this.immobiliseDurationTicks);
        nbt.putLong("discard_at_tick", this.discardAtTick);
        ItemStack stack = this.dataTracker.get(WEAPON_STACK);
        if (!stack.isEmpty()) {
            nbt.put("weapon_stack", stack.encode(this.getRegistryManager()));
        }
        nbt.putInt("owner_id", this.dataTracker.get(OWNER_ID));
        nbt.putInt("target_id", this.dataTracker.get(TARGET_ID));
        nbt.putBoolean("impacted", this.dataTracker.get(IMPACTED));
        nbt.putInt("linger_ticks", this.dataTracker.get(LINGER_TICKS));
    }
}
