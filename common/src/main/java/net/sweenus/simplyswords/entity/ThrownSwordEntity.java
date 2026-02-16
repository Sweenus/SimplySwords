package net.sweenus.simplyswords.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

// This thing is honestly so cursed (many issues with saving/load item stack from entity nbt). It finally seems to work, but the code needs cleaning up and I don't want to touch it
public class ThrownSwordEntity extends PersistentProjectileEntity implements FlyingItemEntity {
    private boolean dealtDamage;
    private static final TrackedData<Byte> LOYALTY;
    private static final TrackedData<Boolean> ENCHANTED;
    private static final TrackedData<ItemStack> ITEM_STACK;
    public ItemStack stack;
    public int returnTimer;
    public PickupPermission pickupType;
    public boolean hasYaw = false;
    public float keepYaw;
    public float keepPitch = 0;
    public boolean offhandThrow = false;
    public float primaryBaseDamage;
    public float primaryReturnDamage;
    public double primaryReturnDamageRadius;
    public boolean returnToPlayer = false;
    public float weightValue = 0.09f;





    // Base Constructor
    public ThrownSwordEntity(EntityType<? extends ThrownSwordEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public ThrownSwordEntity(World world, LivingEntity owner, ItemStack stack) {
        super(EntityRegistry.THROWNSWORDENTITY.get(), owner, world, stack, (ItemStack) null);
        this.dataTracker.set(LOYALTY, getLoyalty());
        this.dataTracker.set(ENCHANTED, stack.hasEnchantments());
        this.dataTracker.set(ITEM_STACK, stack);

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
        if (this.isNoClip() && this.isOwner(player)) {
            int cooldown = offhandThrow ? 4 : 0;
            if (cooldown > 0) player.getItemCooldownManager().set(this.asItemStack(), cooldown);
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

            if (this.inGroundTime > 4) {
            this.dealtDamage = true;
            this.setVelocity(getVelocity().x, 0.07, getVelocity().z);
        }

        if (keepPitch == 0 && !this.isOnGround())
            this.setVelocity(getVelocity().x, getVelocity().y - weightValue, getVelocity().z);
        else if (keepPitch > 0 && this.isOnGround())
            this.setVelocity(getVelocity().x, 0, getVelocity().z); // Stop falling through ground

        Entity entity = this.getOwner();
        int i = (Byte)this.dataTracker.get(LOYALTY);
        if (i > 0 && (this.dealtDamage || this.isNoClip()) && entity != null && returnToPlayer) {
            if (!this.isOwnerAlive()) {
                if (!this.getEntityWorld().isClient() && this.pickupType == PickupPermission.ALLOWED) {
                    this.dropStack((ServerWorld) this.getEntityWorld(), this.asItemStack());
                }

                this.discard();
            } else {
                this.setNoClip(true);
                Vec3d vec3d = entity.getEyePos().subtract(this.getEntityPos());
                this.setPos(this.getX(), this.getY() + vec3d.y * 0.015 * (double)i, this.getZ());
                if (this.getEntityWorld().isClient()) {
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
        float pitchAcceleration = (age * 18);
        if (age > 10) pitchAcceleration = (age * 40);
        if (!hasYaw && getOwner() != null) {
            float tempPitch = getOwner().getPitch() - pitchAcceleration;
            if (keepPitch != 0) {tempPitch = keepPitch;}
            this.setYaw(this.getYaw());
            this.setRotation(getOwner().getYaw() - 90, tempPitch);
            keepYaw = getOwner().getYaw();
            hasYaw = true;
        }
        else if (getOwner() != null) {
            this.setYaw(keepYaw);
            float tempPitch = getOwner().getPitch() - pitchAcceleration;
            if (keepPitch != 0) {tempPitch = keepPitch;}
            this.setRotation(keepYaw - 90, tempPitch);
        }

        doOnTick(entity);
    }


    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        this.setPosition(x, y, z);
        float pitchAcceleration = (age * 18);
        if (age > 10) pitchAcceleration = (age * 40);
        if (!hasYaw&& getOwner() != null) {
            float tempPitch = getOwner().getPitch() - pitchAcceleration;
            if (keepPitch != 0) {tempPitch = keepPitch;}
            this.setRotation(getOwner().getYaw() - 90, tempPitch);
            keepYaw = getOwner().getYaw();
            hasYaw = true;
        }
        else if (getOwner() != null) {
            float tempPitch = getOwner().getPitch() - pitchAcceleration;
            if (keepPitch != 0) {tempPitch = keepPitch;}
            this.setRotation(keepYaw - 90, tempPitch);
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

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        keepPitch = this.getPitch();
        float baseDamage = primaryBaseDamage;
        Entity entity2 = this.getOwner();
        DamageSource damageSource = this.getDamageSources().trident(this, (Entity) (entity2 == null ? this : entity2));
        float agedDamage = doExtraDamage(entity, baseDamage, damageSource);
        World world = this.getEntityWorld();

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
                if (stack != null && !stack.isEmpty() && this.getOwner() instanceof LivingEntity livingOwner) {
                    Item weaponItem = stack.getItem();
                    if (weaponItem instanceof Item) {
                        ((Item) weaponItem).postHit(stack, livingEntity, livingOwner);
                    }
                }
            }
        }

        this.setVelocity(this.getVelocity().multiply(-0.01, -0.1, -0.01));
        this.playSound(getEntityHitSound(), 1.0F, 1.0F);
    }

    protected float doExtraDamage(Entity entity, float baseDamage, DamageSource damageSource) {
        if (this.getPitch() > -220 && this.getPitch() < -90) {
            baseDamage = baseDamage * 1.5f;
            //System.out.println("Perfect hit dmg+ " + baseDamage);
        }
        float agedDamage = baseDamage + ((float) age / 3);
        World world = this.getEntityWorld();
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
    protected void readCustomData(ReadView readView) {
        super.readCustomData(readView);
        this.dealtDamage = readView.getBoolean("DealtDamage", false);
        this.dataTracker.set(LOYALTY, this.getLoyalty());
        this.stack = readView.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.dataTracker.set(ITEM_STACK, this.stack);
    }


    @Override
    protected void writeCustomData(WriteView writeView) {
        super.writeCustomData(writeView);
        writeView.putBoolean("DealtDamage", this.dealtDamage);
        if (!this.stack.isEmpty()) {
            writeView.put("item", ItemStack.CODEC, this.stack);
        }
    }


    protected byte getLoyalty() {
        World world = this.getEntityWorld();
        if (world instanceof ServerWorld serverWorld) {
            return (byte) MathHelper.clamp(EnchantmentHelper.getTridentReturnAcceleration(serverWorld, dataTracker.get(ITEM_STACK), this), 0, 127);
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

    @Override
    public ItemStack getStack() {
        return this.getWeaponStack();
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
        LOYALTY = DataTracker.registerData(ThrownSwordEntity.class, TrackedDataHandlerRegistry.BYTE);
        ENCHANTED = DataTracker.registerData(ThrownSwordEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        ITEM_STACK = DataTracker.registerData(ThrownSwordEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    }
}
