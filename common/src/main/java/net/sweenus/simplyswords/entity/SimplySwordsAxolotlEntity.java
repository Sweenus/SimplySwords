package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.goal.AttackHostileMobsGoal;
import net.sweenus.simplyswords.entity.goal.FollowNearestPlayerGoal;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SimplySwordsAxolotlEntity extends AxolotlEntity implements Tameable {
    private UUID ownerUuid;
    public static int lifespan = Config.uniqueEffects.chompolotl.duration;
    private static final int READY_TO_SIT_COOLDOWN = 20;
    private int ticksSinceSitAttempt = 0;
    public SimplySwordsAxolotlEntity(EntityType<? extends AxolotlEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createSimplyAxolotlAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 1.0f)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1.0f)

                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.1)

                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new AttackHostileMobsGoal(this, 8.0, 1.0, 16.0));
        this.goalSelector.add(2, new FollowNearestPlayerGoal(this, 0.5, 32.0, 2.0));
    }


    @Override
    public void tick() {

        if (ticksSinceSitAttempt < READY_TO_SIT_COOLDOWN) {
            ticksSinceSitAttempt++;
        }

        this.setInvulnerable(true);
        if (this.age > lifespan)
            this.discard();

        super.tick();

        if (!this.getWorld().isClient) {
            if (this.getVariant() == Variant.BLUE) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                HelperMethods.spawnParticle(
                        serverWorld,
                        ParticleTypes.ELECTRIC_SPARK,
                        this.getX(),
                        this.getY() + 0.5,
                        this.getZ(),
                        0.2,
                        0.1,
                        0.2
                );
                if (this.isTouchingWater() && Config.uniqueEffects.chompolotl.dolphinsGrace && (this.age % 20 == 0)) {

                    double radius = 16.0;
                    Box box = new Box(
                            this.getPos().add(-radius, -radius, -radius),
                            this.getPos().add(radius, radius, radius)
                    );

                    List<PlayerEntity> nearbyPlayers = serverWorld.getEntitiesByClass(
                            PlayerEntity.class,
                            box,
                            player -> true
                    );

                    for (PlayerEntity player : nearbyPlayers) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 200, 0, true, false, true));
                    }
                }
            }
        }
    }



    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            UUID ownerUuid = this.getOwnerUuid();
            if (ownerUuid == null || !ownerUuid.equals(serverPlayer.getUuid())) {
                return ActionResult.FAIL;
            }
            boolean hasChompolotlItem = HelperMethods.hasItemInInventory(serverPlayer, ItemsRegistry.CHOMPOLOTL.get());
            if (hasChompolotlItem && this.ticksSinceSitAttempt >= READY_TO_SIT_COOLDOWN) {
                boolean mounted = this.mountOnto(serverPlayer);
                //System.out.println("Mount result: " + mounted);
                this.ticksSinceSitAttempt = 0;
                return mounted ? ActionResult.SUCCESS : ActionResult.FAIL;
            }
        }
        return ActionResult.FAIL;
    }




    @Override
    public boolean tryAttack(Entity target) {
        target.timeUntilRegen = 0;
        return super.tryAttack(target);
    }

    public boolean mountOnto(ServerPlayerEntity player) {
        // NBT tag to save the Axolotl's state
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putString("id", this.getSavedEntityId());
        this.writeNbt(nbtCompound);

        // Try adding the axolotl to the shoulder
        boolean success = player.addShoulderEntity(nbtCompound);

        // Discard the axolotl if the mounting was successful
        if (success) {
            player.getWorld().playSoundFromEntity(null, player, SoundEvents.ENTITY_AXOLOTL_IDLE_AIR,
                    player.getSoundCategory(), 1.0f, 1.0f);
            //System.out.println("Mounted Axolotl with ID:" + this.getSavedEntityId());
            this.discard();
        }
        return success;
    }


    @Nullable
    @Override
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public net.minecraft.world.EntityView method_48926() {
        return this.getWorld();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.ownerUuid == null) {
            return null;
        }
        try {
            Entity entity = ((ServerWorld) this.getWorld()).getEntity(this.ownerUuid);
            if (entity instanceof LivingEntity) {
                return (LivingEntity) entity;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public void setOwner(LivingEntity livingEntity) {
        this.ownerUuid = livingEntity != null ? livingEntity.getUuid() : null;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (this.ownerUuid != null) {
            nbt.putUuid("Owner", this.ownerUuid);
        }
        return nbt;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.ownerUuid = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
    }


}
