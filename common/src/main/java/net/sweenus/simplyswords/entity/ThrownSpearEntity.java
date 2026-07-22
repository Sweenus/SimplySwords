package net.sweenus.simplyswords.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

// This thing is honestly so cursed (many issues with saving/load item stack from entity nbt). It finally seems to work, but the code needs cleaning up and I don't want to touch it
public class ThrownSpearEntity extends PersistentProjectileEntity {
    private boolean dealtDamage;
    private static final TrackedData<Byte> LOYALTY;
    private static final TrackedData<Boolean> ENCHANTED;
    private static final TrackedData<ItemStack> ITEM_STACK;
    public ItemStack stack;
    public int returnTimer;
    public boolean hasYaw = false;
    private Float initialYaw = null; // Store the initial yaw
    public float keepYaw;
    public float keepPitch = 0;
    public boolean offhandThrow = false;
    public float primaryBaseDamage;
    public float primaryReturnDamage;
    public double primaryReturnDamageRadius;
    public boolean returnToPlayer = true;
    public static int offset = 210;
    public int hasLoyalty;
    public float weightValue = 0.05f;
    public boolean nonReturning = false;
    public int nonReturningMaxAge = 80;





    // Base Constructor
    public ThrownSpearEntity(EntityType<? extends ThrownSpearEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public ThrownSpearEntity(World world, LivingEntity owner, ItemStack stack) {
        super(EntityRegistry.SPEAR.get(), owner, world, stack, (ItemStack) null);
        this.dataTracker.set(LOYALTY, getLoyalty());
        this.dataTracker.set(ENCHANTED, stack.hasEnchantments());
        this.dataTracker.set(ITEM_STACK, stack);

        this.pickupType = owner instanceof PlayerEntity && owner.isInCreativeMode() ?
                PickupPermission.CREATIVE_ONLY :
                PickupPermission.ALLOWED;

        this.stack = stack;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(LOYALTY, (byte)0);
        builder.add(ENCHANTED, false);
        builder.add(ITEM_STACK, ItemStack.EMPTY);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.LIVYATAN.get());
    }

    protected boolean tryPickup(PlayerEntity player) {
        if (!this.isOwner(player)) return false;

        if (this.isNoClip()) {
            int cooldown = 1;
            if (offhandThrow) cooldown = 4;
            player.getItemCooldownManager().set(this.asItemStack().getItem(), cooldown);

            if (this.pickupType != PickupPermission.ALLOWED) return true;

            if (offhandThrow && player.getOffHandStack().isEmpty()) {
                // Send the ItemStack to the player's offhand slot if it's free
                player.setStackInHand(Hand.OFF_HAND, this.asItemStack());
                return true;
            } else {
                return player.getInventory().insertStack(this.asItemStack());
            }
        }
        return super.tryPickup(player);
    }


    @Override
    public void tick() {
        if (!this.getWorld().isClient && nonReturning && this.age > nonReturningMaxAge) {
            damageOnReturn(primaryReturnDamageRadius, primaryReturnDamage);
            this.discard();
            return;
        }

        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
            this.setVelocity(getVelocity().x, 0.07, getVelocity().z);
        }

        if (keepPitch == 0 && !this.isOnGround())
            this.setVelocity(getVelocity().x, getVelocity().y - weightValue, getVelocity().z);
        else if (keepPitch > 0 && this.isOnGround())
            this.setVelocity(getVelocity().x, 0, getVelocity().z); // Stop falling through ground

        Entity entity = this.getOwner();
        int i = hasLoyalty;
        if (i > 0 && (this.dealtDamage || this.isNoClip()) && entity != null && returnToPlayer) {
            if (!this.isOwnerAlive()) {
                if (!this.getWorld().isClient && this.pickupType == PickupPermission.ALLOWED) {
                    this.dropStack(this.asItemStack(), 0.1F);
                }

                this.discard();
            } else {
                this.setNoClip(true);
                Vec3d vec3d = entity.getEyePos().subtract(this.getPos());
                this.setPos(this.getX(), this.getY() + vec3d.y * 0.015 * (double)i, this.getZ());
                if (this.getWorld().isClient) {
                    this.lastRenderY = this.getY();
                }

                double d = 0.05 * (double)i;
                this.setVelocity(this.getVelocity().multiply(0.95).add(vec3d.normalize().multiply(d)));
                if (this.returnTimer == 0) {
                    this.playSound(getReturnSound(), 0.2F, 1.2F);
                }
                damageOnReturn(primaryReturnDamageRadius, primaryReturnDamage);
                ++this.returnTimer;
            }
        }

        super.tick();

        // Get the velocity vector
        Vec3d velocity = this.getVelocity();

        // Ensure we set the initialYaw only once, at the moment the spear is launched
        if (initialYaw == null) {
            initialYaw = (float) (-Math.toDegrees(Math.atan2(velocity.x, velocity.z)));
        }

        // Calculate pitch (rotation around X-axis, up/down)
        float calculatedPitch = (float) (-Math.toDegrees(Math.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z))));

        // Use the spear's initial forward yaw for rotation, instead of recalculating it
        float fixedYaw = initialYaw;

        // Check if the spear is in the ground
        if (this.inGround) {
            // If the spear is in the ground, don't update the angles further
            this.setRotation(fixedYaw + 90, keepPitch);
        } else {
            // Set the rotation to match the forward yaw and the dynamically calculated pitch
            this.setRotation(fixedYaw + 90, calculatedPitch +45);
        }


        doOnTick(entity);
    }

    @Override
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        // Update the position
        this.setPosition(x, y, z);

        // Get the velocity vector
        Vec3d velocity = this.getVelocity();

        // Ensure we set the initialYaw only once, at the moment the spear is launched
        if (initialYaw == null) {
            initialYaw = (float) (-Math.toDegrees(Math.atan2(velocity.x, velocity.z)));
        }

        // Calculate pitch (rotation around X-axis, up/down)
        float calculatedPitch = (float) (-Math.toDegrees(Math.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z))));

        // Use the spear's initial forward yaw for rotation, instead of recalculating it
        float fixedYaw = initialYaw;

        // Check if the spear is in the ground
        if (this.inGround) {
            // If the spear is in the ground, don't update the angles further
            this.setRotation(fixedYaw + 90, keepPitch);
        } else {
            // Set the rotation to match the forward yaw and the dynamically calculated pitch
            this.setRotation(fixedYaw + 90, calculatedPitch +45);
        }
    }

    private boolean isOwnerAlive() {
        Entity entity = this.getOwner();
        if (entity != null && entity.isAlive()) {
            return !(entity instanceof ServerPlayerEntity) || !entity.isSpectator();
        } else {
            return false;
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        keepPitch = this.getPitch();
        super.onBlockHit(blockHitResult);
    }

    protected void doOnTick(Entity owner) {
    }

    public void markNonReturning(int maxAgeTicks) {
        this.nonReturning = true;
        this.nonReturningMaxAge = Math.max(1, maxAgeTicks);
        this.returnToPlayer = false;
        this.hasLoyalty = 0;
        this.pickupType = PickupPermission.CREATIVE_ONLY;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        if (this.nonReturning && this.getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity target
                && !HelperMethods.checkAbilityTarget(target, owner)) {
            return;
        }
        keepPitch = this.getPitch();
        float baseDamage = primaryBaseDamage;
        Entity entity2 = this.getOwner();
        DamageSource damageSource = this.getDamageSources().trident(this, (Entity) (entity2 == null ? this : entity2));
        float agedDamage = doExtraDamage(entity, baseDamage, damageSource);
        World world = this.getWorld();

        this.dealtDamage = true;
        if (HelperMethods.damageThroughIframes(entity, damageSource, agedDamage)) {
            if (entity.getType() == EntityType.ENDERMAN) {
                return;
            }

            if (world instanceof ServerWorld serverWorld) {
                EnchantmentHelper.onTargetDamaged(serverWorld, entity, damageSource, stack);
            }

            if (entity instanceof LivingEntity livingEntity) {
                this.knockback(livingEntity, damageSource);
                this.onHit(livingEntity);
                // Get the ItemStack and call postHit if it's defined on the associated Item
                if (stack != null &&  !stack.isEmpty() && this.getOwner() instanceof LivingEntity livingOwner) {
                    Item weaponItem = stack.getItem();
                    if (weaponItem instanceof SwordItem) {
                        ((SwordItem) weaponItem).postHit(stack, livingEntity, livingOwner);
                    }
                }

            }
        }

        this.setVelocity(this.getVelocity().multiply(-0.01, -0.1, -0.01));
        this.playSound(getEntityHitSound(), 1.0F, 1.0F);
        if (!this.getWorld().isClient && nonReturning) {
            damageOnReturn(primaryReturnDamageRadius, primaryReturnDamage);
            this.discard();
        }
    }

    protected float doExtraDamage(Entity entity, float baseDamage, DamageSource damageSource) {
        float agedDamage = baseDamage + ((float) age / 2);
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            agedDamage = EnchantmentHelper.getDamage(serverWorld, stack, entity, damageSource, agedDamage);
            doEffects(serverWorld, baseDamage, entity);
        }
        return agedDamage;
    }

    protected void doEffects(ServerWorld serverWorld, float baseDamage, Entity entity) {
        // For Override
    }

    @Nullable
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        return this.dealtDamage ? null : super.getEntityCollision(currentPosition, nextPosition);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dealtDamage = nbt.getBoolean("DealtDamage");
        this.dataTracker.set(LOYALTY, this.getLoyalty());
        if (nbt.contains("Stack")) {
            this.dataTracker.set(ITEM_STACK, this.stack);
            this.stack = ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("item")).orElse(this.getDefaultItemStack());
        } else {
            this.stack = ItemStack.EMPTY;
        }

    }


    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("DealtDamage", this.dealtDamage);
        if (!this.stack.isEmpty())
            nbt.put("item", this.stack.encode(this.getRegistryManager()));
    }


    protected byte getLoyalty() {
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            return (byte) hasLoyalty;
        } else {
            return 0;
        }
    }

    @Override
    public ItemStack getItemStack() {
        //System.out.println("ThrownSwordEntity: getItemStack called, stack = " + this.stack);
        return this.dataTracker.get(ITEM_STACK);
    }

    public ItemStack getWeaponStack() {
        return this.getItemStack();
    }



    protected void damageOnReturn(double radius, float damage) {
        // For Override
    }


    public boolean isEnchanted() {
        return (Boolean)this.dataTracker.get(ENCHANTED);
    }
    @Override
    protected SoundEvent getHitSound() {
        return SoundEvents.ITEM_TRIDENT_HIT_GROUND;
    }

    protected SoundEvent getReturnSound() {
        return SoundEvents.ITEM_TRIDENT_RETURN;
    }

    protected SoundEvent getEntityHitSound() {
        return SoundEvents.ITEM_TRIDENT_HIT;
    }

    @Override
    public void age() {
        int i = (Byte)this.dataTracker.get(LOYALTY);
        if (this.pickupType != PickupPermission.ALLOWED || i <= 0) {
            super.age();
        }

    }
    static {
        LOYALTY = DataTracker.registerData(ThrownSpearEntity.class, TrackedDataHandlerRegistry.BYTE);
        ENCHANTED = DataTracker.registerData(ThrownSpearEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        ITEM_STACK = DataTracker.registerData(ThrownSpearEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    }
}
