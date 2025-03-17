package net.sweenus.simplyswords.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

// This thing is honestly so cursed (many issues with saving/load item stack from entity nbt). It finally seems to work, but the code needs cleaning up and I don't want to touch it
public class ThrownSwordEntity extends PersistentProjectileEntity {
    private boolean dealtDamage;
    private static final TrackedData<Byte> LOYALTY;
    private static final TrackedData<Boolean> ENCHANTED;
    public ItemStack stack;
    public int returnTimer;
    public PickupPermission pickupType;
    public boolean hasYaw = false;
    public float keepYaw;
    public float keepPitch = 0;
    public boolean offhandThrow = false;
    public float float1;
    public float float2;
    public float float3;
    public int int1;
    public double double1;




    // Base Constructor
    public ThrownSwordEntity(EntityType<? extends ThrownSwordEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public ThrownSwordEntity(World world, LivingEntity owner, ItemStack stack) {
        super(EntityRegistry.THROWNSWORDENTITY.get(), owner, world, stack, (ItemStack) null);
        this.stack = stack;
        this.dataTracker.set(LOYALTY, getLoyalty());
        this.dataTracker.set(ENCHANTED, stack.hasEnchantments());
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(LOYALTY, (byte)0);
        builder.add(ENCHANTED, false);

    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.LIVYATAN.get());
    }

    protected boolean tryPickup(PlayerEntity player) {
        if (this.isNoClip() && this.isOwner(player)) {
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
            this.setVelocity(getVelocity().x, getVelocity().y - 0.09, getVelocity().z);
        else if (keepPitch > 0 && this.isOnGround())
            this.setVelocity(getVelocity().x, 0, getVelocity().z); // Stop falling through ground

        Entity entity = this.getOwner();
        int i = (Byte)this.dataTracker.get(LOYALTY);
        if (i > 0 && (this.dealtDamage || this.isNoClip()) && entity != null) {
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
                    this.playSound(SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_01.get(), 0.2F, 1.2F);
                }
                damageOnReturn(double1, float3);
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
    }


    @Override
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

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        keepPitch = this.getPitch();
        float baseDamage = float1;
        if (this.getPitch() > -220 && this.getPitch() < -90) {
            baseDamage = baseDamage * 1.5f;
            //System.out.println("Perfect hit dmg+ " + baseDamage);
        }
        float agedDamage = baseDamage + ((float) age / 3);
        Entity entity2 = this.getOwner();
        DamageSource damageSource = this.getDamageSources().trident(this, (Entity)(entity2 == null ? this : entity2));
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            int bonusParticles =  ((int) baseDamage / 2);
            agedDamage = EnchantmentHelper.getDamage(serverWorld, stack, entity, damageSource, agedDamage);
            HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.POOF, 0.5f, 3+bonusParticles);
            HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.CRIT, 0.5f, 5+bonusParticles);
            HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.SNOWFLAKE, 0.5f, 2+bonusParticles);
            if (baseDamage > 8)
                world.playSoundFromEntity(null, this, SoundRegistry.MAGIC_SWORD_PARRY_VARIOUS_HITS.get(),
                        this.getSoundCategory(), 0.3f, 1.2f);
        }

        this.dealtDamage = true;
        if (entity.damage(damageSource, agedDamage)) {
            if (entity.getType() == EntityType.ENDERMAN) {
                return;
            }

            world = this.getWorld();
            if (world instanceof ServerWorld serverWorld) {
                EnchantmentHelper.onTargetDamaged(serverWorld, entity, damageSource, stack);
            }

            if (entity instanceof LivingEntity livingEntity) {
                this.knockback(livingEntity, damageSource);
                this.onHit(livingEntity);
            }
        }

        this.setVelocity(this.getVelocity().multiply(-0.01, -0.1, -0.01));
        this.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0F, 1.0F);
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
            this.stack = ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("item")).orElse(this.getDefaultItemStack());
        } else {
            this.stack = ItemStack.EMPTY;
        }

    }


    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("DealtDamage", this.dealtDamage);
        nbt.put("item", this.stack.encode(this.getRegistryManager()));
    }


    private byte getLoyalty() {
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            return 3;
        } else {
            return 0;
        }
    }

    @Override
    public ItemStack getItemStack() {
        System.out.println("ThrownSwordEntity: getItemStack called, stack = " + this.stack);
        return this.stack;
        }

    public ItemStack getWeaponStack() {
        return this.getItemStack();
    }



    public void damageOnReturn(double radius, float damage) {
        if (getWorld().isClient()) return;
        if (this.stack == null || this.stack.isEmpty()) return;

        ServerWorld world = (ServerWorld) this.getWorld();
        if (this.getOwner() != null && this.getOwner() instanceof ServerPlayerEntity user) {
            DamageSource damageSource = user.getDamageSources().trident(this, user);
            float returnDamage = EnchantmentHelper.getDamage(world, stack, this, damageSource, damage);

            Box box = new Box(this.getX() + radius, this.getY() + radius, this.getZ() + radius,
                    this.getX() - radius, this.getY() - radius, this.getZ() - radius);

            for (Entity entity : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user) && le.timeUntilRegen == 0) {

                    le.damage(damageSource, returnDamage);
                    world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_ICE_ATTACK_01.get(),
                            user.getSoundCategory(), 0.2f, 1.5f);
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, int1, 2), user);
                    HelperMethods.spawnOrbitParticles(world, this.getPos(), ParticleTypes.POOF, 0.5f, 3);
                }
            }
        }
    }


    public boolean isEnchanted() {
        return (Boolean)this.dataTracker.get(ENCHANTED);
    }
    @Override
    protected SoundEvent getHitSound() {
        return SoundEvents.ITEM_TRIDENT_HIT_GROUND;
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
    }
}
