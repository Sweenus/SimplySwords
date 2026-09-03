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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.goal.AttackHostileMobsGoal;
import net.sweenus.simplyswords.entity.goal.FollowNearestPlayerGoal;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.MinionTargeting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SimplySwordsAxolotlEntity extends AxolotlEntity implements Tameable {
    private UUID ownerUuid;
    public static int lifespan = Config.uniqueEffects.chompolotl.duration;
    private static final int READY_TO_SIT_COOLDOWN = 20;
    private int ticksSinceSitAttempt = 0;
    private int masteryLifespan;
    private double masteryLowHealthThreshold;
    private float masteryLowHealthBonus;
    private float masterySplashMultiplier;
    private double masterySplashRadius;
    private int masterySplashCap;
    private boolean masteryCanPerch = true;
    private boolean masteryCanAttack = true;
    private double masteryAuraRadius = 16.0;
    private int masteryGraceDuration = 200;
    private double masteryGuardRadius;
    private float masteryGuardMultiplier = 1;
    private boolean masteryRescue;
    private double masteryRescueThreshold;
    private int masteryRescueDuration;
    private int masteryRescueAmplifier;
    private double masteryPackRange;
    private float masteryPackBonus;
    private int masteryPackCap;
    private double masteryChainRange;
    private int masteryChainExtension;
    private boolean masteryChainUsed;
    private int masteryCooldownRefundPercent;
    private int masteryVictoryRequired;
    private int masteryVictoryWindow;
    private int masteryVictoryRefund;
    private double masteryPounceRange;
    private int masteryPounceInterval;
    private double masteryTargetRange = 8;
    private int masteryTargetSearchCap;
    private float masteryCoordinatedBonus;
    private double masteryShoulderAuraRadius = 5;
    private int masteryHelpfulAbsorption;
    private int masteryHelpfulDuration;
    private int masteryHelpfulLockout;
    private boolean masteryEternalAura;
    public SimplySwordsAxolotlEntity(EntityType<? extends AxolotlEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createSimplyAxolotlAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 1.0f)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1.0f)
                .add(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, 10.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.1)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0)
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
        if (this.age > (masteryLifespan > 0 ? masteryLifespan : lifespan))
            this.discard();

        super.tick();

        if (!getWorld().isClient() && masteryPounceRange > 0 && masteryPounceInterval > 0
                && age % masteryPounceInterval == 0 && getTarget() != null && getTarget().isAlive()) {
            Vec3d direction = getTarget().getPos().subtract(getPos());
            if (direction.lengthSquared() <= masteryPounceRange * masteryPounceRange
                    && direction.lengthSquared() > .01) {
                Vec3d velocity = direction.normalize().multiply(.9).add(0, .25, 0);
                setVelocity(velocity);
                velocityModified = true;
            }
        }

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
                if (masteryAuraRadius > 0 && this.isTouchingWater()
                        && Config.uniqueEffects.chompolotl.dolphinsGrace && this.age % 20 == 0) {

                    double radius = masteryAuraRadius;
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
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE,
                                masteryGraceDuration, 0, true, false, true));
                    }
                }
                LivingEntity owner = getOwner();
                if (owner != null && masteryRescue && owner.getHealth() / owner.getMaxHealth()
                        < masteryRescueThreshold) {
                    owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                            masteryRescueDuration, masteryRescueAmplifier,
                            false, true, true));
                    discard();
                } else if (masteryEternalAura && masteryAuraRadius > 0 && age % 20 == 0) {
                    Box aura = getBoundingBox().expand(masteryAuraRadius);
                    for (PlayerEntity player : serverWorld.getEntitiesByClass(
                            PlayerEntity.class, aura, player -> true)) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, 0,
                                false, false, true));
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
        if (!masteryCanAttack) return false;
        target.timeUntilRegen = 0;
        EntityAttributeInstance attackDamage = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        double baseDamage = attackDamage == null ? 0 : attackDamage.getBaseValue();
        double multiplier = 1;
        LivingEntity owner = getOwner();
        if (target instanceof LivingEntity living && masteryLowHealthBonus > 0
                && living.getHealth() / living.getMaxHealth() < masteryLowHealthThreshold) {
            multiplier *= 1 + masteryLowHealthBonus;
        }
        if (owner != null && getWorld() instanceof ServerWorld serverWorld
                && target == MinionTargeting.getOwnerCurrentTarget(serverWorld, owner)) {
            multiplier *= 1 + masteryCoordinatedBonus;
        }
        double strikeDamage = baseDamage * multiplier;
        if (attackDamage != null) attackDamage.setBaseValue(strikeDamage);
        boolean attacked = super.tryAttack(target);
        if (attackDamage != null) attackDamage.setBaseValue(baseDamage);
        if (attacked && target instanceof LivingEntity living && getWorld() instanceof ServerWorld world) {
            if (owner != null && masteryPackBonus > 0 && masteryPackRange > 0) {
                long allies = world.getEntitiesByClass(SimplySwordsAxolotlEntity.class,
                                getBoundingBox().expand(masteryPackRange), other -> other != this
                                        && owner.getUuid().equals(other.getOwnerUuid()))
                        .stream().limit(masteryPackCap).count();
                if (allies > 0) {
                        living.timeUntilRegen = 0;
                    living.damage(world.getDamageSources().indirectMagic(this, owner),
                            (float) strikeDamage * masteryPackBonus * allies);
                }
            }
            if (owner != null && masterySplashMultiplier > 0 && masterySplashRadius > 0) {
                world.getEntitiesByClass(LivingEntity.class, living.getBoundingBox().expand(masterySplashRadius),
                                other -> other != living && other != owner && HelperMethods.checkAbilityTarget(other, owner))
                        .stream().limit(masterySplashCap).forEach(other -> {
                            other.timeUntilRegen = 0;
                            other.damage(world.getDamageSources().indirectMagic(this, owner),
                                    (float) strikeDamage * masterySplashMultiplier);
                        });
            }
            if (owner != null && !living.isAlive() && !masteryChainUsed && masteryChainRange > 0) {
                world.getEntitiesByClass(LivingEntity.class, living.getBoundingBox().expand(masteryChainRange),
                                other -> other.isAlive() && HelperMethods.checkAbilityTarget(other, owner))
                        .stream().min(java.util.Comparator.comparingDouble(this::squaredDistanceTo))
                        .ifPresent(next -> {
                            masteryChainUsed = true;
                            setTarget(next);
                            masteryLifespan += masteryChainExtension;
                        });
            }
            if (owner != null && !living.isAlive() && masteryVictoryRequired > 0) {
                net.sweenus.simplyswords.world.NatureSwarmMasteryCombatManager.onAxolotlKill(world, owner,
                        masteryVictoryRequired, masteryVictoryWindow, masteryVictoryRefund);
            }
            if (owner instanceof PlayerEntity player && masteryCooldownRefundPercent > 0) {
                int total = net.sweenus.simplyswords.api.SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                        new net.minecraft.item.ItemStack(ItemsRegistry.CHOMPOLOTL.get()), owner,
                        Config.uniqueEffects.chompolotl.cooldown * 10);
                net.sweenus.simplyswords.api.SimplySwordsAPI.reduceWeaponCooldown(player,
                        new net.minecraft.item.ItemStack(ItemsRegistry.CHOMPOLOTL.get()),
                        Config.uniqueEffects.chompolotl.cooldown * 10,
                        total * masteryCooldownRefundPercent / 100);
                masteryCooldownRefundPercent = 0;
            }
        }
        return attacked;
    }

    public boolean mountOnto(ServerPlayerEntity player) {
        if (!masteryCanPerch) return false;
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

    public void configureMastery(int lifespan, double lowHealthThreshold, float lowHealthBonus,
                                 float splashMultiplier,
                                 double splashRadius, int splashCap, boolean canPerch,
                                 boolean canAttack, double auraRadius, double shoulderAuraRadius,
                                 int graceDuration, double targetRange, int targetSearchCap,
                                 float coordinatedBonus, int helpfulAbsorption,
                                 int helpfulDuration, int helpfulLockout, boolean eternalAura) {
        this.masteryLifespan = Math.max(0, lifespan);
        this.masteryLowHealthThreshold = Math.clamp(lowHealthThreshold, 0, 1);
        this.masteryLowHealthBonus = Math.max(0, lowHealthBonus);
        this.masterySplashMultiplier = Math.max(0, splashMultiplier);
        this.masterySplashRadius = Math.max(0, splashRadius);
        this.masterySplashCap = Math.max(0, splashCap);
        this.masteryCanPerch = canPerch;
        this.masteryCanAttack = canAttack;
        this.masteryAuraRadius = Math.max(0, auraRadius);
        this.masteryShoulderAuraRadius = Math.max(0, shoulderAuraRadius);
        this.masteryGraceDuration = Math.max(1, graceDuration);
        this.masteryTargetRange = Math.max(0, targetRange);
        this.masteryTargetSearchCap = Math.max(0, targetSearchCap);
        this.masteryCoordinatedBonus = Math.max(0, coordinatedBonus);
        this.masteryHelpfulAbsorption = Math.max(0, helpfulAbsorption);
        this.masteryHelpfulDuration = Math.max(0, helpfulDuration);
        this.masteryHelpfulLockout = Math.max(0, helpfulLockout);
        this.masteryEternalAura = eternalAura;
    }

    public void configureGuardian(double guardRadius, double guardMultiplier, boolean rescue,
                                  double rescueThreshold, int rescueDuration, int rescueAmplifier) {
        this.masteryGuardRadius = Math.max(0, guardRadius);
        this.masteryGuardMultiplier = (float) Math.clamp(guardMultiplier, 0, 1);
        this.masteryRescue = rescue;
        this.masteryRescueThreshold = Math.clamp(rescueThreshold, 0, 1);
        this.masteryRescueDuration = Math.max(0, rescueDuration);
        this.masteryRescueAmplifier = Math.max(0, rescueAmplifier);
    }

    public void configurePack(double range, float bonus, int cap, double chainRange,
                              int chainExtension, int cooldownRefundPercent) {
        this.masteryPackRange = Math.max(0, range);
        this.masteryPackBonus = Math.max(0, bonus);
        this.masteryPackCap = Math.max(0, cap);
        this.masteryChainRange = Math.max(0, chainRange);
        this.masteryChainExtension = Math.max(0, chainExtension);
        this.masteryCooldownRefundPercent = Math.max(0, cooldownRefundPercent);
    }

    public void configureRally(int victoryRequired, int victoryWindow, int victoryRefund,
                               double pounceRange, int pounceInterval) {
        this.masteryVictoryRequired = Math.max(0, victoryRequired);
        this.masteryVictoryWindow = Math.max(0, victoryWindow);
        this.masteryVictoryRefund = Math.max(0, victoryRefund);
        this.masteryPounceRange = Math.max(0, pounceRange);
        this.masteryPounceInterval = Math.max(0, pounceInterval);
    }

    public double getMasteryGuardRadius() {
        return masteryGuardRadius;
    }

    public float getMasteryGuardMultiplier() { return masteryGuardMultiplier; }
    public double getMasteryTargetRange(double fallback) {
        return masteryTargetRange > 0 ? masteryTargetRange : fallback;
    }
    public int getMasteryTargetSearchCap() { return masteryTargetSearchCap; }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (this.ownerUuid != null) {
            nbt.putUuid("Owner", this.ownerUuid);
        }
        nbt.putInt("MasteryLifespan", masteryLifespan);
        nbt.putDouble("MasteryLowHealthThreshold", masteryLowHealthThreshold);
        nbt.putFloat("MasteryLowHealthBonus", masteryLowHealthBonus);
        nbt.putFloat("MasterySplashMultiplier", masterySplashMultiplier);
        nbt.putDouble("MasterySplashRadius", masterySplashRadius);
        nbt.putInt("MasterySplashCap", masterySplashCap);
        nbt.putBoolean("MasteryCanPerch", masteryCanPerch);
        nbt.putBoolean("MasteryCanAttack", masteryCanAttack);
        nbt.putDouble("MasteryAuraRadius", masteryAuraRadius);
        nbt.putDouble("MasteryShoulderAuraRadius", masteryShoulderAuraRadius);
        nbt.putInt("MasteryGraceDuration", masteryGraceDuration);
        nbt.putDouble("MasteryGuardRadius", masteryGuardRadius);
        nbt.putFloat("MasteryGuardMultiplier", masteryGuardMultiplier);
        nbt.putBoolean("MasteryRescue", masteryRescue);
        nbt.putDouble("MasteryRescueThreshold", masteryRescueThreshold);
        nbt.putInt("MasteryRescueDuration", masteryRescueDuration);
        nbt.putInt("MasteryRescueAmplifier", masteryRescueAmplifier);
        nbt.putDouble("MasteryPackRange", masteryPackRange);
        nbt.putFloat("MasteryPackBonus", masteryPackBonus);
        nbt.putInt("MasteryPackCap", masteryPackCap);
        nbt.putDouble("MasteryChainRange", masteryChainRange);
        nbt.putInt("MasteryChainExtension", masteryChainExtension);
        nbt.putBoolean("MasteryChainUsed", masteryChainUsed);
        nbt.putInt("MasteryCooldownRefundPercent", masteryCooldownRefundPercent);
        nbt.putInt("MasteryVictoryRequired", masteryVictoryRequired);
        nbt.putInt("MasteryVictoryWindow", masteryVictoryWindow);
        nbt.putInt("MasteryVictoryRefund", masteryVictoryRefund);
        nbt.putDouble("MasteryPounceRange", masteryPounceRange);
        nbt.putInt("MasteryPounceInterval", masteryPounceInterval);
        nbt.putDouble("MasteryTargetRange", masteryTargetRange);
        nbt.putInt("MasteryTargetSearchCap", masteryTargetSearchCap);
        nbt.putFloat("MasteryCoordinatedBonus", masteryCoordinatedBonus);
        nbt.putInt("MasteryHelpfulAbsorption", masteryHelpfulAbsorption);
        nbt.putInt("MasteryHelpfulDuration", masteryHelpfulDuration);
        nbt.putInt("MasteryHelpfulLockout", masteryHelpfulLockout);
        nbt.putBoolean("MasteryEternalAura", masteryEternalAura);
        return nbt;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.ownerUuid = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        masteryLifespan = nbt.getInt("MasteryLifespan");
        masteryLowHealthThreshold = nbt.getDouble("MasteryLowHealthThreshold");
        masteryLowHealthBonus = nbt.getFloat("MasteryLowHealthBonus");
        masterySplashMultiplier = nbt.getFloat("MasterySplashMultiplier");
        masterySplashRadius = nbt.getDouble("MasterySplashRadius");
        masterySplashCap = nbt.getInt("MasterySplashCap");
        if (nbt.contains("MasteryCanPerch")) masteryCanPerch = nbt.getBoolean("MasteryCanPerch");
        if (nbt.contains("MasteryCanAttack")) masteryCanAttack = nbt.getBoolean("MasteryCanAttack");
        if (nbt.contains("MasteryAuraRadius")) masteryAuraRadius = nbt.getDouble("MasteryAuraRadius");
        if (nbt.contains("MasteryShoulderAuraRadius")) {
            masteryShoulderAuraRadius = nbt.getDouble("MasteryShoulderAuraRadius");
        }
        if (nbt.contains("MasteryGraceDuration")) masteryGraceDuration = nbt.getInt("MasteryGraceDuration");
        masteryGuardRadius = nbt.getDouble("MasteryGuardRadius");
        if (nbt.contains("MasteryGuardMultiplier")) masteryGuardMultiplier = nbt.getFloat("MasteryGuardMultiplier");
        masteryRescue = nbt.getBoolean("MasteryRescue");
        masteryRescueThreshold = nbt.getDouble("MasteryRescueThreshold");
        masteryRescueDuration = nbt.getInt("MasteryRescueDuration");
        masteryRescueAmplifier = nbt.getInt("MasteryRescueAmplifier");
        masteryPackRange = nbt.getDouble("MasteryPackRange");
        masteryPackBonus = nbt.getFloat("MasteryPackBonus");
        masteryPackCap = nbt.getInt("MasteryPackCap");
        masteryChainRange = nbt.getDouble("MasteryChainRange");
        masteryChainExtension = nbt.getInt("MasteryChainExtension");
        masteryChainUsed = nbt.getBoolean("MasteryChainUsed");
        masteryCooldownRefundPercent = nbt.getInt("MasteryCooldownRefundPercent");
        masteryVictoryRequired = nbt.getInt("MasteryVictoryRequired");
        masteryVictoryWindow = nbt.getInt("MasteryVictoryWindow");
        masteryVictoryRefund = nbt.getInt("MasteryVictoryRefund");
        masteryPounceRange = nbt.getDouble("MasteryPounceRange");
        masteryPounceInterval = nbt.getInt("MasteryPounceInterval");
        if (nbt.contains("MasteryTargetRange")) masteryTargetRange = nbt.getDouble("MasteryTargetRange");
        masteryTargetSearchCap = nbt.getInt("MasteryTargetSearchCap");
        masteryCoordinatedBonus = nbt.getFloat("MasteryCoordinatedBonus");
        masteryHelpfulAbsorption = nbt.getInt("MasteryHelpfulAbsorption");
        masteryHelpfulDuration = nbt.getInt("MasteryHelpfulDuration");
        masteryHelpfulLockout = nbt.getInt("MasteryHelpfulLockout");
        masteryEternalAura = nbt.getBoolean("MasteryEternalAura");
    }


}
