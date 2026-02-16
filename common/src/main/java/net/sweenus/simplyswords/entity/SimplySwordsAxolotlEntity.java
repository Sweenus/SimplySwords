package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.entity.goal.AttackHostileMobsGoal;
import net.sweenus.simplyswords.entity.goal.FollowNearestPlayerGoal;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.ShoulderAxolotlData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public class SimplySwordsAxolotlEntity extends AxolotlEntity {
    private UUID ownerUuid;
    public static int lifespan = 500;
    private static final int READY_TO_SIT_COOLDOWN = 20;
    private int ticksSinceSitAttempt = 0;

    public SimplySwordsAxolotlEntity(EntityType<? extends AxolotlEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createSimplyAxolotlAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 35.0)
                .add(EntityAttributes.FLYING_SPEED, 1.0f)
                .add(EntityAttributes.MOVEMENT_SPEED, 1.0f)
                .add(EntityAttributes.WATER_MOVEMENT_EFFICIENCY, 10.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.ATTACK_KNOCKBACK, 0.1)
                .add(EntityAttributes.STEP_HEIGHT, 1.0)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new AttackHostileMobsGoal(this, 8.0, 1.0, 16.0));
        this.goalSelector.add(2, new FollowNearestPlayerGoal(this, 0.5, 32.0, 2.0));
    }

    @Override
    public void tick() {
        if (ticksSinceSitAttempt < READY_TO_SIT_COOLDOWN) ticksSinceSitAttempt++;

        this.setInvulnerable(true);
        if (this.age > lifespan) this.discard();
        super.tick();

        if (!this.getEntityWorld().isClient() && this.getVariant() == Variant.BLUE) {
            ServerWorld serverWorld = (ServerWorld) this.getEntityWorld();
            HelperMethods.spawnParticle(serverWorld, ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.5, this.getZ(), 0.2, 0.1, 0.2);

            if (this.isTouchingWater() && (this.age % 20 == 0)) {
                double radius = 16.0;
                Box box = new Box(this.getEntityPos().add(-radius, -radius, -radius), this.getEntityPos().add(radius, radius, radius));
                List<PlayerEntity> nearbyPlayers = serverWorld.getEntitiesByClass(PlayerEntity.class, box, player -> true);
                for (PlayerEntity player : nearbyPlayers) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 200, 0, true, false, true));
                }
            }
        }
    }

    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getEntityWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            boolean hasChompolotlItem = HelperMethods.hasItemInInventory(serverPlayer, ItemsRegistry.CHOMPOLOTL.get());
            if (hasChompolotlItem && this.ticksSinceSitAttempt >= READY_TO_SIT_COOLDOWN) {
                OptionalInt leftTagged = ShoulderAxolotlData.getLeftVariant(serverPlayer);
                OptionalInt rightTagged = ShoulderAxolotlData.getRightVariant(serverPlayer);
                Optional<ParrotEntity.Variant> leftParrot = serverPlayer.getLeftShoulderParrotVariant();
                Optional<ParrotEntity.Variant> rightParrot = serverPlayer.getRightShoulderParrotVariant();
                int variantId = this.getVariant().ordinal();

                if (leftParrot.isEmpty() && leftTagged.isEmpty()) {
                    serverPlayer.setLeftShoulderParrotVariant(Optional.of(ShoulderAxolotlData.toParrotVariant(variantId)));
                    ShoulderAxolotlData.setLeftVariant(serverPlayer, variantId);
                    this.discard();
                    return ActionResult.SUCCESS;
                }

                if (rightParrot.isEmpty() && rightTagged.isEmpty()) {
                    serverPlayer.setRightShoulderParrotVariant(Optional.of(ShoulderAxolotlData.toParrotVariant(variantId)));
                    ShoulderAxolotlData.setRightVariant(serverPlayer, variantId);
                    this.discard();
                    return ActionResult.SUCCESS;
                }

                this.ticksSinceSitAttempt = 0;
                return ActionResult.PASS;
            }
        }
        return ActionResult.FAIL;
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        target.timeUntilRegen = 0;
        return super.tryAttack(world, target);
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.ownerUuid == null) return null;
        try {
            Entity entity = ((ServerWorld) this.getEntityWorld()).getEntity(this.ownerUuid);
            if (entity instanceof LivingEntity livingEntity) return livingEntity;
        } catch (Exception ignored) {
        }
        return null;
    }

    public void setOwner(LivingEntity livingEntity) {
        this.ownerUuid = livingEntity != null ? livingEntity.getUuid() : null;
    }
}
