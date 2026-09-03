package net.sweenus.simplyswords.entity;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.world.WraithfangAbilityManager;
import net.sweenus.simplyswords.world.WraithfangTuningSnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WraithfangEntity extends ThrownSpearEntity {
    private static final int BANSHEE_CAST = 1;
    private static final int PASSING_HAUNT = 4;
    private static final int UNERRING_CHASE = 8;
    private static final int ARRIVAL_STRIKE = 16;
    private static final int SPECTRAL_STAMPEDE = 32;
    private static final int POSSESSING_LUNGE = 64;
    private static final TagKey<net.minecraft.entity.EntityType<?>> PIERCE_IMMUNE = TagKey.of(
            RegistryKeys.ENTITY_TYPE, Identifier.of("simplyswords", "wraithfang_pierce_immune"));
    public int slownessDuration;
    private @Nullable UniqueAbilityExecution abilityExecution;
    private WraithfangTuningSnapshot tuning = WraithfangTuningSnapshot.from(null);
    private boolean impactBurstUsed;
    private boolean forcedReturnStarted;
    private boolean stopPiercing;
    private boolean arrivalUsed;
    private boolean possessionUsed;
    private boolean dashComplete;
    private boolean grantHasteOnDashComplete;
    private int desiredLoyalty = 1;
    private int contactCount;
    private double alternationDamageMultiplier = 1;
    private @Nullable UUID pursuitTargetId;
    private @Nullable Vec3d lastOwnerPos;
    private final Set<UUID> contactedTargets = new HashSet<>();

    // Base Constructor
    public WraithfangEntity(EntityType<? extends WraithfangEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public WraithfangEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }
    @Override
    public void tick() {
        Entity owner = getOwner();
        if (!forcedReturnStarted && tuning.returnDelayTicks() > 0 && age >= tuning.returnDelayTicks()) {
            forcedReturnStarted = true;
            hasLoyalty = tuning.hasMode(BANSHEE_CAST) ? 3 : desiredLoyalty;
            beginReturn();
        }
        if (tuning.returnDelayTicks() > 0 && !forcedReturnStarted) hasLoyalty = 0;
        if (owner instanceof LivingEntity livingOwner && getWorld() instanceof ServerWorld serverWorld) {
            tickPursuit(serverWorld, livingOwner);
        }

        super.tick();
        if (isRemoved() && abilityExecution != null && !abilityExecution.isTerminal()) {
            UniqueAbilityApi.finish(abilityExecution, abilityExecution.definition().id(), 0);
        }
    }

    public void setAbilityExecution(UniqueAbilityExecution execution) {
        this.abilityExecution = execution;
        this.tuning = WraithfangTuningSnapshot.from(execution);
        this.desiredLoyalty = tuning.loyalty();
        this.hasLoyalty = tuning.returnDelayTicks() > 0 ? 0 : desiredLoyalty;
    }

    public void configureMastery(@Nullable LivingEntity target, double alternationDamageMultiplier,
                                 boolean grantHasteOnDashComplete) {
        this.pursuitTargetId = target == null ? null : target.getUuid();
        this.alternationDamageMultiplier = Math.max(0, alternationDamageMultiplier);
        this.grantHasteOnDashComplete = grantHasteOnDashComplete;
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target, float damage) {
        if (!(getWorld() instanceof ServerWorld)) return;
        int duration = tuning.weaknessDurationTicks();
        if (duration > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,
                duration, tuning.weaknessAmplifier(),
                false, false, true), getOwner());
        if (abilityExecution != null) {
            UniqueAbilityApi.emit(abilityExecution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                    AbyssalSpectralMasteryAbilities.HIT, target, 1, damage);
        }
        stopPiercing = target.getType().isIn(PIERCE_IMMUNE);
        burst(target, damage, tuning);
        if (!target.isAlive() && getWorld() instanceof ServerWorld world
                && getOwner() instanceof LivingEntity actor) {
            if (abilityExecution != null) {
                UniqueAbilityApi.emit(abilityExecution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                        AbyssalSpectralMasteryAbilities.KILL, target, 1, damage);
            }
            WraithfangAbilityManager.onThrownKill(world, actor, stack, tuning);
        }
    }

    @Override
    protected float doExtraDamage(Entity entity, float baseDamage, DamageSource source) {
        float damage = super.doExtraDamage(entity, baseDamage, source);
        double multiplier = getOwner() instanceof LivingEntity actor
                ? WraithfangAbilityManager.projectileDamageMultiplier(actor, tuning, alternationDamageMultiplier)
                : alternationDamageMultiplier;
        return (damage - age * .5F + (float) (Math.min(age, tuning.flightDamageCapTicks())
                * tuning.flightDamagePerTick())) * (float) multiplier;
    }

    @Override
    protected int getAdditionalPierces() {
        return stopPiercing ? 0 : tuning.pierceCount();
    }

    @Override
    protected float getPierceDamageMultiplier() {
        return (float) tuning.pierceDamageMultiplier();
    }

    private void burst(LivingEntity directTarget, float damage, WraithfangTuningSnapshot tuning) {
        double radius = tuning.burstRadius();
        int cap = tuning.burstTargetCap();
        float burstDamage = damage * (float) tuning.burstDamageMultiplier();
        if (impactBurstUsed || radius <= 0 || cap <= 0 || burstDamage <= 0
                || !(getWorld() instanceof ServerWorld world) || !(getOwner() instanceof LivingEntity owner)) return;
        if (!WraithfangAbilityManager.tryBurst(world, owner, tuning.burstLockoutTicks())) return;
        impactBurstUsed = true;
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                new Box(directTarget.getPos(), directTarget.getPos()).expand(radius),
                candidate -> candidate != directTarget && candidate != owner && candidate.isAlive()
                        && candidate.squaredDistanceTo(directTarget) <= radius * radius
                        && HelperMethods.checkAbilityTarget(candidate, owner));
        targets.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(directTarget)));
        for (int i = 0; i < Math.min(cap, targets.size()); i++) {
            SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, targets.get(i),
                    burstDamage, SpellScalingProfile.SOUL);
        }
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        boolean canPickup = super.tryPickup(player);

        if (canPickup) {
            player.getWorld().playSound(this, this.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_WIND_SHOOT_IMPACT_02.get(),
                    this.getSoundCategory(), 0.1f, 1.2f);
            if (player.getWorld() instanceof ServerWorld world) {
                WraithfangAbilityManager.onReturn(world, player, this.asItemStack(), tuning);
            }
            if (abilityExecution != null) {
                UniqueAbilityApi.emit(abilityExecution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                        AbyssalSpectralMasteryAbilities.RETURN, player, 1, 0);
                UniqueAbilityApi.finish(abilityExecution, abilityExecution.definition().id(), 1);
            }
        }

        return canPickup;
    }

    @Override
    protected int getReturnPickupCooldownTicks() {
        return -1;
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.WRAITHFANG.get());
    }

    @Override
    protected byte getLoyalty() {
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            return 3;
        } else {
            return 0;
        }
    }

    private void tickPursuit(ServerWorld world, LivingEntity owner) {
        if (dashComplete) return;
        if (age >= tuning.dashDurationTicks()) {
            completePursuit(world, owner);
            return;
        }
        LivingEntity target = pursuitTarget();
        if (target != null && owner.squaredDistanceTo(target) > tuning.dashTargetRange() * tuning.dashTargetRange()) {
            target = null;
        }
        if (target != null && tuning.hasMode(POSSESSING_LUNGE) && !possessionUsed) {
            possessionUsed = true;
            Vec3d facing = target.getRotationVec(1).multiply(1, 0, 1);
            if (facing.lengthSquared() < 1.0E-4) facing = owner.getRotationVec(1).multiply(1, 0, 1);
            Vec3d destination = target.getPos().subtract(facing.normalize().multiply(1.5));
            if (owner instanceof ServerPlayerEntity player) {
                player.networkHandler.requestTeleport(destination.x, destination.y, destination.z,
                        owner.getYaw(), owner.getPitch());
            } else {
                owner.refreshPositionAndAngles(destination.x, destination.y, destination.z,
                        owner.getYaw(), owner.getPitch());
            }
            strikeArrival(world, owner, target);
            completePursuit(world, owner);
            return;
        }
        Vec3d direction = getPos().subtract(owner.getPos());
        if (target != null && tuning.hasMode(UNERRING_CHASE) && age < tuning.steeringTicks()) {
            Vec3d desired = target.getPos().subtract(owner.getPos());
            if (direction.lengthSquared() > 1.0E-4 && desired.lengthSquared() > 1.0E-4) {
                direction = net.sweenus.simplyswords.world.GloampiercerTuningSnapshot.turnToward(
                        direction.normalize(), desired.normalize(), Math.toRadians(tuning.steeringDegrees()));
            }
        }
        if (direction.lengthSquared() > 1.0E-4 && owner.distanceTo(this) > 1) {
            Vec3d velocity = direction.normalize().multiply(tuning.dashSpeed());
            owner.setVelocity(velocity);
            owner.velocityModified = true;
            HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.OMINOUS_SPAWNING,
                    this, owner, (int) distanceTo(owner));
            owner.addStatusEffect(new StatusEffectInstance(
                    EffectRegistry.getReference(EffectRegistry.RESILIENCE), 20, 4, false, false, true));
        }
        damagePursuitContacts(world, owner);
        if (target != null && tuning.hasMode(ARRIVAL_STRIKE) && !tuning.hasMode(SPECTRAL_STAMPEDE)
                && owner.squaredDistanceTo(target) <= tuning.arrivalRange() * tuning.arrivalRange()) {
            strikeArrival(world, owner, target);
            completePursuit(world, owner);
            owner.setVelocity(0, owner.getVelocity().y, 0);
            owner.velocityModified = true;
        }
        lastOwnerPos = owner.getPos();
    }

    private void completePursuit(ServerWorld world, LivingEntity owner) {
        dashComplete = true;
        if (grantHasteOnDashComplete) {
            WraithfangAbilityManager.onPursuitComplete(world, owner, stack, tuning);
        }
    }

    private void damagePursuitContacts(ServerWorld world, LivingEntity owner) {
        if ((!tuning.hasMode(PASSING_HAUNT) && !tuning.hasMode(SPECTRAL_STAMPEDE))
                || tuning.dashContactTargetCap() <= 0 || contactCount >= tuning.dashContactTargetCap()) return;
        Vec3d start = lastOwnerPos == null ? owner.getPos() : lastOwnerPos;
        Box path = new Box(start, owner.getPos()).expand(Math.max(.5, owner.getWidth() * .5));
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, path,
                candidate -> candidate != owner && candidate.isAlive() && !contactedTargets.contains(candidate.getUuid())
                        && HelperMethods.checkAbilityTarget(candidate, owner));
        targets.sort(Comparator.comparingDouble(owner::squaredDistanceTo));
        for (LivingEntity target : targets) {
            if (contactCount >= tuning.dashContactTargetCap()) break;
            contactedTargets.add(target.getUuid());
            contactCount++;
            SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, target,
                    primaryBaseDamage * (float) tuning.dashContactDamageMultiplier()
                            * masteryDamageMultiplier(owner), SpellScalingProfile.SOUL);
            if (tuning.dashStatusDurationTicks() > 0) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                        tuning.dashStatusDurationTicks(), tuning.dashStatusAmplifier(), false, false, true), owner);
            }
        }
    }

    private void strikeArrival(ServerWorld world, LivingEntity owner, LivingEntity target) {
        if (arrivalUsed || tuning.arrivalDamageMultiplier() <= 0 || !target.isAlive()
                || !HelperMethods.checkAbilityTarget(target, owner)) return;
        arrivalUsed = true;
        SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, target,
                primaryBaseDamage * (float) tuning.arrivalDamageMultiplier()
                        * masteryDamageMultiplier(owner), SpellScalingProfile.SOUL);
    }

    private float masteryDamageMultiplier(LivingEntity owner) {
        return (float) WraithfangAbilityManager.projectileDamageMultiplier(
                owner, tuning, alternationDamageMultiplier);
    }

    private @Nullable LivingEntity pursuitTarget() {
        if (pursuitTargetId == null || !(getWorld() instanceof ServerWorld world)) return null;
        return world.getEntity(pursuitTargetId) instanceof LivingEntity target && target.isAlive()
                && getOwner() instanceof LivingEntity owner && HelperMethods.checkAbilityTarget(target, owner)
                ? target : null;
    }

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        tuning.write(nbt);
        nbt.putFloat("wraithfang_base_damage", primaryBaseDamage);
        nbt.putInt("wraithfang_desired_loyalty", desiredLoyalty);
        nbt.putDouble("wraithfang_alternation_damage", alternationDamageMultiplier);
        nbt.putBoolean("wraithfang_burst_used", impactBurstUsed);
        nbt.putBoolean("wraithfang_forced_return", forcedReturnStarted);
        nbt.putBoolean("wraithfang_stop_piercing", stopPiercing);
        nbt.putBoolean("wraithfang_arrival_used", arrivalUsed);
        nbt.putBoolean("wraithfang_possession_used", possessionUsed);
        nbt.putBoolean("wraithfang_dash_complete", dashComplete);
        nbt.putBoolean("wraithfang_dash_haste", grantHasteOnDashComplete);
        nbt.putInt("wraithfang_contact_count", contactCount);
        if (pursuitTargetId != null) nbt.putUuid("wraithfang_target", pursuitTargetId);
        if (lastOwnerPos != null) {
            nbt.putDouble("wraithfang_owner_x", lastOwnerPos.x);
            nbt.putDouble("wraithfang_owner_y", lastOwnerPos.y);
            nbt.putDouble("wraithfang_owner_z", lastOwnerPos.z);
        }
        int index = 0;
        for (UUID id : contactedTargets) nbt.putUuid("wraithfang_contact_" + index++, id);
        nbt.putInt("wraithfang_contacts", index);
    }

    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        tuning = WraithfangTuningSnapshot.read(nbt);
        primaryBaseDamage = nbt.getFloat("wraithfang_base_damage");
        desiredLoyalty = nbt.getInt("wraithfang_desired_loyalty");
        alternationDamageMultiplier = nbt.getDouble("wraithfang_alternation_damage");
        impactBurstUsed = nbt.getBoolean("wraithfang_burst_used");
        forcedReturnStarted = nbt.getBoolean("wraithfang_forced_return");
        stopPiercing = nbt.getBoolean("wraithfang_stop_piercing");
        arrivalUsed = nbt.getBoolean("wraithfang_arrival_used");
        possessionUsed = nbt.getBoolean("wraithfang_possession_used");
        dashComplete = nbt.getBoolean("wraithfang_dash_complete");
        grantHasteOnDashComplete = nbt.getBoolean("wraithfang_dash_haste");
        contactCount = nbt.getInt("wraithfang_contact_count");
        if (nbt.containsUuid("wraithfang_target")) pursuitTargetId = nbt.getUuid("wraithfang_target");
        if (nbt.contains("wraithfang_owner_x")) lastOwnerPos = new Vec3d(
                nbt.getDouble("wraithfang_owner_x"), nbt.getDouble("wraithfang_owner_y"),
                nbt.getDouble("wraithfang_owner_z"));
        contactedTargets.clear();
        for (int index = 0; index < nbt.getInt("wraithfang_contacts"); index++) {
            String key = "wraithfang_contact_" + index;
            if (nbt.containsUuid(key)) contactedTargets.add(nbt.getUuid(key));
        }
        if (nbt.contains("item")) {
            stack = ItemStack.fromNbt(getRegistryManager(), nbt.getCompound("item")).orElse(getDefaultItemStack());
        }
        hasLoyalty = forcedReturnStarted || tuning.returnDelayTicks() == 0 ? desiredLoyalty : 0;
    }

}
