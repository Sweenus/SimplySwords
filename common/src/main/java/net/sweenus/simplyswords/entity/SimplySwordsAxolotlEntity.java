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
import net.sweenus.simplyswords.world.ChompolotlMasteryManager;
import net.sweenus.simplyswords.world.LivyatanWaveManager;
import net.sweenus.simplyswords.api.ability.NatureSwarmMasteryTuning;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SimplySwordsAxolotlEntity extends AxolotlEntity implements Tameable, net.minecraft.entity.JumpingMount {
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
    private static final TrackedData<Boolean> RAVAGER = DataTracker.registerData(SimplySwordsAxolotlEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> CHARGE_TICKS = DataTracker.registerData(SimplySwordsAxolotlEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> WAVE_SPENT = DataTracker.registerData(SimplySwordsAxolotlEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private ItemStack castingStack = ItemStack.EMPTY;
    private int activeCooldown;
    private long expiresAt;
    private String summonWorld = "";
    private float waveDamage;
    private long chargingSince = -1;
    private UUID chargingRider;
    private double interception = .5;
    private double interceptionRange = 6;
    private long nextBite;
    private long nextLeap;
    private boolean wasInWater = true;
    private boolean diving;
    private boolean leaping;
    private int leapTicks;
    private static final double RIDER_HEIGHT = .42;
    private static final int DEFAULT_WAVE_CHARGE_TICKS = 10;
    private static final int LEAP_COOLDOWN_TICKS = 30;
    private static final int LEAP_MIN_AIR_TICKS = 3;
    private static final double LEAP_HORIZONTAL_VELOCITY = .55;
    private static final double LEAP_VERTICAL_VELOCITY = .6;
    private static final double SWIM_FLUID_HEIGHT = .2;
    private static final double DIVE_LAUNCH_VELOCITY = .42;

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(RAVAGER, false);
        builder.add(CHARGE_TICKS, DEFAULT_WAVE_CHARGE_TICKS);
        builder.add(WAVE_SPENT, false);
    }

    public boolean isRavager() { return dataTracker.get(RAVAGER); }
    public boolean isEternalGuardian() { return masteryEternalAura; }
    public boolean canMasteryAttack() { return masteryCanAttack; }
    public boolean hasCoordinatedBite() { return masteryCoordinatedBonus > 0; }
    public int getWaveChargeTicks() { return dataTracker.get(CHARGE_TICKS); }
    public double getInterceptionRange() { return interceptionRange; }
    public ItemStack getCastingStack() { return castingStack; }
    public int getActiveCooldown() { return activeCooldown; }
    public boolean isExpired() { return expiresAt > 0 && getWorld().getTime() >= expiresAt; }

    public void configureSummon(ItemStack stack, int cooldown, NatureSwarmMasteryTuning tuning, boolean active) {
        castingStack = stack.copy();
        activeCooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, getOwner(), cooldown);
        expiresAt = getWorld().getTime() + masteryLifespan;
        summonWorld = getWorld().getRegistryKey().getValue().toString();
        dataTracker.set(RAVAGER, active && tuning.flag(1 << 26));
        dataTracker.set(CHARGE_TICKS, tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_WAVE_CHARGE_TICKS,
                DEFAULT_WAVE_CHARGE_TICKS));
        waveDamage = (float) getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (isRavager()) {
            waveDamage /= (float) tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_RAVAGER_DAMAGE_MULTIPLIER, 1.25);
            getAttributeInstance(EntityAttributes.GENERIC_SCALE).setBaseValue(
                    tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_RAVAGER_SCALE, 2));
        }
        if (masteryEternalAura) {
            double health = tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_ETERNAL_HEALTH, 70);
            getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(health);
            setHealth((float) health);
            interception = tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_ETERNAL_INTERCEPTION, .5);
            interceptionRange = tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_ETERNAL_AURA_RADIUS, 6);
        }
    }

    public float interceptDamage(DamageSource source, float amount) {
        float transferred = Math.min(getHealth(), amount * (float) interception);
        if (transferred <= 0) return 0;
        setHealth(getHealth() - transferred);
        if (getHealth() <= 0) {
            onDeath(source);
            discard();
        }
        return transferred;
    }

    public void handleWaveCharge(ServerPlayerEntity rider, int action) {
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || isExpired()) return;
        if (action == 0) {
            if (chargingSince < 0) {
                chargingSince = getWorld().getTime();
                chargingRider = rider.getUuid();
                playChargeTick(0);
            }
            return;
        }
        chargingSince = -1;
        chargingRider = null;
        dataTracker.set(WAVE_SPENT, false);
    }

    private void playChargeTick(long held) {
        float progress = Math.min(1, (float) held / Math.max(1, getWaveChargeTicks()));
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP,
                getSoundCategory(), .6F, .8F + progress * .8F);
    }

    private void tickDiveEffects(ServerWorld world) {
        boolean inWater = isSwimming(this);
        if (inWater == wasInWater) return;
        if (inWater) {
            world.playSound(null, getBlockPos(), SoundEvents.ENTITY_DOLPHIN_SPLASH, getSoundCategory(), .7F, 1.1F);
        } else {
            world.playSound(null, getBlockPos(), SoundEvents.ENTITY_DOLPHIN_JUMP, getSoundCategory(), .8F, 1F);
        }
        world.spawnParticles(ParticleTypes.SPLASH, getX(), getY() + .2, getZ(), 24, .5, .1, .5, .1);
        wasInWater = inWater;
    }

    private void fireChargedWave(LivingEntity rider) {
        LivingEntity owner = getOwner();
        chargingSince = -1;
        chargingRider = null;
        dataTracker.set(WAVE_SPENT, true);
        if (owner == null) return;
        LivyatanWaveManager.fireAxolotlWave((ServerWorld) getWorld(), this, owner, rider, castingStack, waveDamage);
    }

    public void tryMountLeap(ServerPlayerEntity rider) {
        if (!isRavager() || isExpired() || !(getWorld() instanceof ServerWorld world)
                || world.getTime() < nextLeap) return;
        nextLeap = world.getTime() + LEAP_COOLDOWN_TICKS;
        net.sweenus.simplyswords.api.PlayerMovementIntent intent = SimplySwordsAPI.getPlayerMovementIntent(rider);
        Vec3d direction = intent.isNeutral() ? Vec3d.fromPolar(0, rider.getYaw())
                : intent.toDirection(rider.getYaw());
        Vec3d launch = direction.multiply(LEAP_HORIZONTAL_VELOCITY).add(0, LEAP_VERTICAL_VELOCITY, 0);
        new net.sweenus.simplyswords.network.ChompolotlMountLaunchPacket(getId(), launch).sendTo(rider);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_AXOLOTL_SPLASH, getSoundCategory(), .6F, 1.2F);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_AXOLOTL_IDLE_WATER, getSoundCategory(), .7F, 1.4F);
        world.spawnParticles(ParticleTypes.SPLASH, getX(), getY() + .15, getZ(), 12, .55, .08, .55, .03);
    }

    public void applyMountLeap(Vec3d launch) {
        if (!getWorld().isClient() || !isRavager() || !Double.isFinite(launch.x)
                || !Double.isFinite(launch.y) || !Double.isFinite(launch.z)) return;
        setVelocity(launch);
        leaping = true;
        leapTicks = LEAP_MIN_AIR_TICKS;
        diving = false;
    }

    @Override
    public void remove(RemovalReason reason) {
        chargingSince = -1;
        chargingRider = null;
        if (!getWorld().isClient()) dataTracker.set(WAVE_SPENT, false);
        if (!getWorld().isClient()) ChompolotlMasteryManager.remove(this);
        removeAllPassengers();
        super.remove(reason);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return isRavager() && getFirstPassenger() instanceof PlayerEntity player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        LivingEntity owner = getOwner();
        return isRavager() && !hasPassengers() && passenger instanceof PlayerEntity player
                && owner != null && (owner == player || !HelperMethods.checkFriendlyFire(player, owner));
    }

    @Override
    public Vec3d getPassengerRidingPos(Entity passenger) {
        return getPos().add(0, RIDER_HEIGHT * getScale(), 0);
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity player, Vec3d input) {
        double forward = player.forwardSpeed < 0 ? player.forwardSpeed * .5 : player.forwardSpeed;
        return new Vec3d(player.sidewaysSpeed * .75, 0, forward);
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity player) { return isSwimming(this) ? .4F : .1F; }

    @Override
    protected void tickControlled(PlayerEntity player, Vec3d input) {
        setYaw(player.getYaw());
        prevYaw = getYaw();
        bodyYaw = getYaw();
        headYaw = getYaw();
        setMovementSpeed(getSaddledSpeed(player));
        super.tickControlled(player, input);
    }

    @Override
    public void travel(Vec3d input) {
        if (getControllingPassenger() instanceof PlayerEntity rider && isLogicalSideForUpdatingMovement()) {
            boolean inWater = isSwimming(this);
            if (leapTicks > 0) leapTicks--;
            if (leaping && leapTicks <= 0 && (inWater || isOnGround())) leaping = false;
            if (inWater || isOnGround()) diving = false;
            else if (wasInWater && getVelocity().y > 0) {
                diving = true;
                setVelocity(getVelocity().x, Math.max(getVelocity().y, DIVE_LAUNCH_VELOCITY), getVelocity().z);
            }
            wasInWater = inWater;
            if (leaping || diving) {
                Vec3d velocity = getVelocity();
                setVelocity(velocity.x * .98, velocity.y - .08, velocity.z * .98);
                move(net.minecraft.entity.MovementType.SELF, getVelocity());
            } else {
                Vec3d movement = input.lengthSquared() > 1 ? input.normalize() : input;
                Vec3d forward = Vec3d.fromPolar(0, rider.getYaw());
                Vec3d right = new Vec3d(forward.z, 0, -forward.x);
                double speed = getSaddledSpeed(rider);
                Vec3d horizontal = forward.multiply(movement.z * speed).add(right.multiply(movement.x * speed));
                double vertical = inWater ? -Math.sin(Math.toRadians(rider.getPitch())) * movement.z * speed
                        : getVelocity().y - .08;
                setVelocity(horizontal.x, vertical, horizontal.z);
                move(net.minecraft.entity.MovementType.SELF, getVelocity());
                setVelocity(getVelocity().multiply(.8));
            }
            updateLimbs(false);
        } else super.travel(input);
    }

    private static boolean isSwimming(SimplySwordsAxolotlEntity axolotl) {
        return axolotl.getFluidHeight(net.minecraft.registry.tag.FluidTags.WATER) > SWIM_FLUID_HEIGHT;
    }

    @Override
    public boolean canJump() { return isRavager() && !dataTracker.get(WAVE_SPENT); }
    @Override
    public void setJumpStrength(int strength) { }
    @Override
    public void startJumping(int height) { }
    @Override
    public void stopJumping() { }

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
        if (!getWorld().isClient()) {
            LivingEntity owner = getOwner();
            if (expiresAt == 0) expiresAt = getWorld().getTime() + Math.max(1, masteryLifespan > 0 ? masteryLifespan : lifespan);
            if (summonWorld.isEmpty()) summonWorld = getWorld().getRegistryKey().getValue().toString();
            if (owner == null || !owner.isAlive() || owner.isRemoved() || isExpired()
                    || !summonWorld.equals(getWorld().getRegistryKey().getValue().toString())) {
                discard();
                return;
            }
            ChompolotlMasteryManager.register(this, false);
            LivingEntity rider = getControllingPassenger();
            if (rider != null && (rider.isRemoved() || !rider.isAlive()
                    || rider != owner && HelperMethods.checkFriendlyFire(rider, owner))) {
                removeAllPassengers();
                rider = null;
            }
            if (chargingRider != null && (rider == null || !chargingRider.equals(rider.getUuid())
                    || net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry.isIncapacitated(rider))) {
                chargingSince = -1;
                chargingRider = null;
            }
            LivingEntity mounted = rider;
            if (mounted != null) tickDiveEffects((ServerWorld) getWorld());
            else {
                wasInWater = isSwimming(this);
                if (dataTracker.get(WAVE_SPENT)) dataTracker.set(WAVE_SPENT, false);
            }
            if (chargingSince >= 0 && mounted != null) {
                long held = getWorld().getTime() - chargingSince;
                if (held >= getWaveChargeTicks()) fireChargedWave(mounted);
                else if (held > 0 && held % 3 == 0) playChargeTick(held);
            }
            if (mounted != null) {
                getNavigation().stop();
                if (masteryCanAttack && getWorld().getTime() >= nextBite) {
                    ((ServerWorld) getWorld()).getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(2),
                            target -> target.isAlive() && target != mounted && target != this && target != owner
                                    && squaredDistanceTo(target) <= 4 && HelperMethods.checkAbilityTarget(target, owner)).stream()
                            .min(java.util.Comparator.comparingDouble((LivingEntity target) -> squaredDistanceTo(target))
                                    .thenComparing(target -> target.getUuid().toString())).ifPresent(this::tryAttack);
                }
            }
        }

        super.tick();

        if (!getWorld().isClient() && !hasPassengers() && masteryCanAttack && masteryPounceRange > 0 && masteryPounceInterval > 0
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
                if (masteryAuraRadius > 0 && (!isRavager() || hasPassengers()) && this.isTouchingWater()
                        && Config.uniqueEffects.chompolotl.dolphinsGrace && this.age % 20 == 0) {

                    double radius = masteryAuraRadius;
                    Box box = new Box(
                            this.getPos().add(-radius, -radius, -radius),
                            this.getPos().add(radius, radius, radius)
                    );

                    List<PlayerEntity> nearbyPlayers = serverWorld.getEntitiesByClass(
                            PlayerEntity.class,
                            box,
                            player -> getOwner() != null && player.squaredDistanceTo(this) <= radius * radius
                                    && (player == getOwner() || !HelperMethods.checkFriendlyFire(player, getOwner()))
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
                    masteryRescue = false;
                    getNavigation().stop();
                    setTarget(null);
                    refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), getYaw(), getPitch());
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
        if (isRavager()) {
            if (getWorld().isClient()) return ActionResult.SUCCESS;
            return canAddPassenger(player) && player.startRiding(this) ? ActionResult.SUCCESS : ActionResult.FAIL;
        }
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
        LivingEntity owner = getOwner();
        if (!masteryCanAttack || owner == null || !(getWorld() instanceof ServerWorld world)
                || !(target instanceof LivingEntity living) || !living.isAlive() || target == owner
                || hasPassenger(target) || !HelperMethods.checkAbilityTarget(living, owner)
                || world.getTime() < nextBite) return false;
        nextBite = world.getTime() + 10;
        double multiplier = 1;
        if (masteryLowHealthBonus > 0 && living.getHealth() / living.getMaxHealth() < masteryLowHealthThreshold)
            multiplier *= 1 + masteryLowHealthBonus;
        if (target == ChompolotlMasteryManager.currentTarget(owner)) multiplier *= 1 + masteryCoordinatedBonus;
        if (masteryPackBonus > 0 && masteryPackRange > 0) {
            long allies = ChompolotlMasteryManager.owned(owner).stream().filter(other -> other != this
                    && squaredDistanceTo(other) <= masteryPackRange * masteryPackRange).limit(masteryPackCap).count();
            multiplier *= 1 + masteryPackBonus * allies;
        }
        float strikeDamage = (float) (getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * multiplier);
        DamageSource source = getDamageSources().mobAttack(this);
        boolean attacked = HelperMethods.damageThroughIframes(living, source,
                HelperMethods.applyAbilityDamageEnchantments(world, castingStack, living, source, strikeDamage));
        if (!attacked) return false;
        if (masteryCooldownRefundPercent > 0) {
            int percent = masteryCooldownRefundPercent;
            masteryCooldownRefundPercent = 0;
            ChompolotlMasteryManager.firstBite(this, percent);
        }
        if (masterySplashMultiplier > 0 && masterySplashRadius > 0) {
            world.getEntitiesByClass(LivingEntity.class, living.getBoundingBox().expand(masterySplashRadius),
                    other -> other != living && other != owner && other != this && !hasPassenger(other) && other.isAlive()
                            && other.squaredDistanceTo(living) <= masterySplashRadius * masterySplashRadius
                            && HelperMethods.checkAbilityTarget(other, owner)).stream()
                    .sorted(java.util.Comparator.comparingDouble((LivingEntity other) -> other.squaredDistanceTo(living))
                            .thenComparing(other -> other.getUuid().toString())).limit(masterySplashCap).forEach(other -> {
                        DamageSource splash = getDamageSources().indirectMagic(this, owner);
                        HelperMethods.damageThroughIframes(other, splash, HelperMethods.applyAbilityDamageEnchantments(
                                world, castingStack, other, splash, strikeDamage * masterySplashMultiplier));
                    });
        }
        return true;
    }

    public void onMasteryKill(LivingEntity victim) {
        LivingEntity owner = getOwner();
        if (owner == null || !(getWorld() instanceof ServerWorld world)) return;
        if (!masteryChainUsed && masteryChainRange > 0) {
            world.getEntitiesByClass(LivingEntity.class, victim.getBoundingBox().expand(masteryChainRange),
                    other -> other != victim && other != this && other.isAlive()
                            && other.squaredDistanceTo(victim) <= masteryChainRange * masteryChainRange
                            && HelperMethods.checkAbilityTarget(other, owner)).stream()
                    .min(java.util.Comparator.comparingDouble((LivingEntity other) -> other.squaredDistanceTo(victim))
                            .thenComparing(other -> other.getUuid().toString())).ifPresent(next -> {
                        masteryChainUsed = true;
                        setTarget(next);
                        expiresAt += masteryChainExtension;
                    });
        }
        ChompolotlMasteryManager.victory(this, masteryVictoryRequired, masteryVictoryWindow, masteryVictoryRefund);
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
        if (getWorld() instanceof ServerWorld world && world.getEntity(ownerUuid) instanceof LivingEntity owner) return owner;
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
        nbt.putLong("ChompExpiresAt", expiresAt);
        nbt.putString("ChompWorld", summonWorld);
        nbt.putInt("ChompActiveCooldown", activeCooldown);
        if (!castingStack.isEmpty()) nbt.put("ChompStack", castingStack.encode(getRegistryManager()));
        nbt.putBoolean("ChompRavager", isRavager());
        nbt.putFloat("ChompWaveDamage", waveDamage);
        nbt.putInt("ChompChargeTicks", getWaveChargeTicks());
        nbt.putDouble("ChompInterception", interception);
        nbt.putDouble("ChompInterceptionRange", interceptionRange);
        nbt.putBoolean("ChompGuardianVersion", true);
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
        expiresAt = nbt.getLong("ChompExpiresAt");
        summonWorld = nbt.getString("ChompWorld");
        activeCooldown = nbt.contains("ChompActiveCooldown") ? nbt.getInt("ChompActiveCooldown") : Config.uniqueEffects.chompolotl.cooldown * 10;
        castingStack = ItemStack.fromNbt(getRegistryManager(), nbt.getCompound("ChompStack")).orElseGet(() -> new ItemStack(ItemsRegistry.CHOMPOLOTL.get()));
        dataTracker.set(RAVAGER, nbt.getBoolean("ChompRavager"));
        dataTracker.set(CHARGE_TICKS, Math.max(1, nbt.contains("ChompChargeTicks")
                ? nbt.getInt("ChompChargeTicks") : DEFAULT_WAVE_CHARGE_TICKS));
        waveDamage = nbt.getFloat("ChompWaveDamage");
        if (nbt.contains("ChompInterception")) interception = Math.clamp(nbt.getDouble("ChompInterception"), 0, 1);
        if (nbt.contains("ChompInterceptionRange")) interceptionRange = Math.max(0, nbt.getDouble("ChompInterceptionRange"));
        if (masteryEternalAura) {
            masteryCanAttack = false;
            masteryCanPerch = false;
            masteryAuraRadius = 0;
            masteryShoulderAuraRadius = 0;
            if (!nbt.contains("ChompGuardianVersion")) {
                getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(70);
                setHealth(70);
                masteryLifespan = 600;
            }
        }
    }


}
