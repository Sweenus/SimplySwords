package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SimplySwordsGoatStampedeEntity extends GoatEntity implements SimplySwordsMinion {

    private static final double HIT_SEARCH_RADIUS = 1.1;

    private UUID ownerUuid;
    private UUID stampedeId;
    private long expiresAtTick;
    private float damage;
    private double knockbackStrength;
    private double chargeSpeed;
    private Vec3d direction = Vec3d.ZERO;
    private ItemStack sourceStack = ItemStack.EMPTY;
    private final Set<UUID> hitTargets = new HashSet<>();

    public SimplySwordsGoatStampedeEntity(EntityType<? extends GoatEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder createStampedeAttributes() {
        return GoatEntity.createGoatAttributes()
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    public void initializeStampede(LivingEntity owner, ItemStack stack, UUID stampedeId, Vec3d direction, long expiresAtTick,
                                    float damage, double knockbackStrength, double chargeSpeed, boolean screaming) {
        this.ownerUuid = owner.getUuid();
        this.stampedeId = stampedeId;
        this.sourceStack = stack.copy();
        this.direction = direction.lengthSquared() > 1.0E-4 ? direction.normalize() : Vec3d.fromPolar(0.0F, owner.getYaw());
        this.expiresAtTick = expiresAtTick;
        this.damage = Math.max(0.0F, damage);
        this.knockbackStrength = Math.max(0.0, knockbackStrength);
        this.chargeSpeed = Math.max(0.0, chargeSpeed);
        this.setScreaming(screaming);
        this.setAiDisabled(true);
        this.setPersistent();

        float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(-this.direction.x, this.direction.z)));
        this.setYaw(yaw);
        this.setBodyYaw(yaw);
        this.prevYaw = yaw;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld world) || world.getTime() > this.expiresAtTick) {
            expire();
            return;
        }

        if (this.isOnFire()) {
            this.extinguish();
        }

        Vec3d velocity = new Vec3d(this.direction.x * this.chargeSpeed, this.getVelocity().y, this.direction.z * this.chargeSpeed);
        this.setVelocity(velocity);
        this.velocityModified = true;
        this.move(MovementType.SELF, velocity);

        LivingEntity owner = getOwner(world);
        Box searchBox = this.getBoundingBox().expand(HIT_SEARCH_RADIUS);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox, this::isValidTarget)) {
            if (!this.hitTargets.add(target.getUuid())) {
                continue;
            }
            boolean damaged = owner != null && SimplySwordsAPI.applyDelegatedWeaponHit(this.sourceStack, target, owner, this, this.damage);
            if (damaged) {
                target.takeKnockback(this.knockbackStrength, this.getX() - target.getX(), this.getZ() - target.getZ());
                Vec3d hitPos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
                world.spawnParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 6, 0.2, 0.15, 0.2, 0.03);
                world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GOAT_HORN_BREAK, SoundCategory.PLAYERS, 0.4F, 1.0F);
            }
        }
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target == this) {
            return false;
        }
        if (target instanceof SimplySwordsGoatStampedeEntity other && this.ownerUuid != null && this.ownerUuid.equals(other.ownerUuid)) {
            return false;
        }
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        LivingEntity owner = getOwner(world);
        if (owner == null || target == owner) {
            return false;
        }
        return HelperMethods.checkFriendlyFire(target, owner);
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Nullable
    public UUID getStampedeId() {
        return this.stampedeId;
    }

    @Nullable
    private LivingEntity getOwner(ServerWorld world) {
        if (this.ownerUuid == null) return null;
        Entity entity = world.getEntity(this.ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void expire() {
        if (this.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.POOF, this.getX(), this.getBodyY(0.5), this.getZ(), 10, 0.3, 0.3, 0.3, 0.04);
        }
        this.discard();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
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
    public boolean cannotDespawn() {
        return true;
    }
}
