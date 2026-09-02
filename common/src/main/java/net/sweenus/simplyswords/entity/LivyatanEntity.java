package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.LivyatanWaveManager;
import net.sweenus.simplyswords.world.LivyatanAbilityManager;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LivyatanEntity extends ThrownSwordEntity {
    public int slownessDuration;
    private final Set<UUID> returnLightningRolledTargets = new HashSet<>();
    private final Set<UUID> returnHitTargets = new HashSet<>();
    private final Map<UUID, Integer> maelstromRounds = new HashMap<>();
    private Phase6AbilityTuning throwTuning = Phase6AbilityTuning.EMPTY;
    private Phase6AbilityTuning returnTuning = Phase6AbilityTuning.EMPTY;
    private UniqueAbilityExecution throwExecution;
    private UniqueAbilityExecution returnExecution;
    private int configuredCooldown;
    private float lastImpactDamage;
    private boolean returnStarted;
    private boolean caught;

    // Base Constructor
    public LivyatanEntity(EntityType<? extends LivyatanEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public LivyatanEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }

    public void setMastery(Phase6AbilityTuning tuning, UniqueAbilityExecution execution,
                           Phase6AbilityTuning tunedReturn, UniqueAbilityExecution returnExecution,
                           int cooldown) {
        throwTuning = tuning == null ? Phase6AbilityTuning.EMPTY : tuning;
        throwExecution = execution;
        returnTuning = tunedReturn == null ? Phase6AbilityTuning.EMPTY : tunedReturn;
        this.returnExecution = returnExecution;
        configuredCooldown = Math.max(0, cooldown);
    }
    @Override
    public void tick() {
        if (!nonReturning) {
            returnToPlayer = true;
        }
        super.tick();
    }

    @Override
    protected void damageOnReturn(double radius, float damage) {
        if (getWorld().isClient() || this.stack == null || this.stack.isEmpty()) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld world) || !(this.getOwner() instanceof LivingEntity user) || !user.isAlive()) {
            return;
        }

        if (!returnStarted) {
            returnStarted = true;
            if (returnExecution != null) UniqueAbilityApi.start(returnExecution);
        }

        Vec3d toOwner = user.getEyePos().subtract(this.getPos());
        Vec3d horizontalToOwner = new Vec3d(toOwner.x, 0.0, toOwner.z);
        if (horizontalToOwner.lengthSquared() <= 1.0E-6) {
            horizontalToOwner = Vec3d.fromPolar(0.0F, user.getYaw());
        } else {
            horizontalToOwner = horizontalToOwner.normalize();
        }
        int maelstromDuration = returnTuning.integer(s("LIVYATAN_MAELSTROM_DURATION_TICKS"), 0);
        int rotations = returnTuning.integer(s("LIVYATAN_MAELSTROM_ROTATIONS"), 0);
        boolean maelstrom = maelstromDuration > 0 && rotations > 0;
        Vec3d waveCenter = this.getPos();
        if (maelstrom) {
            double progress = Math.min(1, (double) returnTimer / maelstromDuration);
            double angle = progress * rotations * Math.PI * 2;
            double orbitRadius = Math.max(1.5, primaryReturnDamageRadius * .55);
            waveCenter = user.getPos().add(Math.cos(angle) * orbitRadius, .2, Math.sin(angle) * orbitRadius);
        }
        LivyatanWaveManager.spawnReturnPulse(world, waveCenter, horizontalToOwner, this.returnTimer,
                Math.max(2, primaryReturnDamageRadius));

        double waveRadius = Math.max(.5, radius);
        Box box = Box.of(waveCenter.add(0.0, 0.5, 0.0), waveRadius * 2.0, 3.0, waveRadius * 2.0);
        DamageSource damageSource = user.getDamageSources().trident(this, user);
        boolean damageTick = this.returnTimer % 5 == 0;
        int cap = Config.uniqueEffects.livyatan.returnWaveTargetCap;
        int thunderheadCap = returnTuning.integer(s("LIVYATAN_THUNDERHEAD_TARGET_CAP"), Integer.MAX_VALUE);
        int round = maelstrom ? Math.min(rotations - 1,
                (int) ((long) returnTimer * rotations / Math.max(1, maelstromDuration))) : -1;
        Vec3d resolvedWaveCenter = waveCenter;
        var targets = world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive).stream()
                .filter(target -> HelperMethods.checkAbilityTarget(target, user))
                .filter(target -> horizontalDistanceSquared(target.getPos(), resolvedWaveCenter) <= waveRadius * waveRadius)
                .sorted(Comparator.comparingDouble(target -> target.squaredDistanceTo(resolvedWaveCenter)))
                .limit(Math.max(1, cap)).toList();
        for (LivingEntity target : targets) {
            double pull = LivyatanAbilityManager.returnPull(
                    Config.uniqueEffects.livyatan.returnWavePullStrength, returnTuning);
            pullTargetTowardOwner(target, user, pull);
            LivyatanAbilityManager.recordPull(world, user, target, returnTuning);
            boolean canDamageRound = !maelstrom || maelstromRounds.getOrDefault(target.getUuid(), -1) < round;
            if (damageTick && canDamageRound) {
                float tunedDamage = damage * (float) returnTuning.get(s("LIVYATAN_MAELSTROM_DAMAGE_MULTIPLIER"), 1);
                float returnDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, tunedDamage);
                if (HelperMethods.damageThroughIframes(target, damageSource, returnDamage)) {
                    if (maelstrom) maelstromRounds.put(target.getUuid(), round);
                    returnHitTargets.add(target.getUuid());
                    world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_ICE_ATTACK_01.get(), user.getSoundCategory(), 0.2f, 1.5f);
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slownessDuration, 2), user);
                    HelperMethods.spawnOrbitParticles(world, waveCenter, ParticleTypes.POOF, 0.5f, 3);
                    if (returnExecution != null) UniqueAbilityApi.emit(returnExecution, UniqueAbilityPhase.HIT,
                            Phase6UniqueAbilities.RETURN_HIT, target, 1, returnDamage);
                    LivyatanAbilityManager.recordReturnHit(world, user, stack, target, returnTuning);
                }
            }
            int chance = LivyatanAbilityManager.returnLightningChance(
                    Config.uniqueEffects.livyatan.returnLightningChance, returnTuning);
            if (this.returnLightningRolledTargets.add(target.getUuid())
                    && this.returnLightningRolledTargets.size() <= thunderheadCap
                    && (chance >= 100 || chance > 0 && world.random.nextInt(100) < chance)) {
                LivyatanAbilityManager.strikeLightning(world, user, stack, target,
                        LivyatanAbilityManager.returnLightningDamage(user, stack, returnTuning), returnTuning);
            }
        }
        pullLooseEntitiesTowardOwner(world, user, box, waveCenter, waveRadius);
        if (nonReturning) finishExecutions();
    }

    private static void pullTargetTowardOwner(LivingEntity target, LivingEntity owner, double rawStrength) {
        double strength = Math.max(0.0, rawStrength) * getLivingPullScale(target);
        if (strength <= 0) return;
        pullEntityTowardOwner(target, owner, strength, true);
        target.fallDistance = 0.0F;
    }

    private static double getLivingPullScale(LivingEntity target) {
        double sizeScore = Math.max(target.getHeight() / 1.25, target.getWidth() / 0.75);
        if (sizeScore <= 1.0) {
            return 1.0;
        }
        return MathHelper.clamp(1.0 / sizeScore, 0.25, 1.0);
    }

    private static void pullLooseEntitiesTowardOwner(ServerWorld world, LivingEntity owner, Box box, Vec3d waveCenter, double waveRadius) {
        double strength = Math.max(0.0, Config.uniqueEffects.livyatan.returnWavePullStrength) * 1.35;
        if (strength <= 0.0) {
            return;
        }

        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, box, entity -> entity.isAlive() && entity.isOnGround())) {
            if (horizontalDistanceSquared(item.getPos(), waveCenter) <= waveRadius * waveRadius) {
                pullEntityTowardOwner(item, owner, strength, false);
            }
        }
        for (ExperienceOrbEntity orb : world.getEntitiesByClass(ExperienceOrbEntity.class, box, entity -> entity.isAlive() && entity.isOnGround())) {
            if (horizontalDistanceSquared(orb.getPos(), waveCenter) <= waveRadius * waveRadius) {
                pullEntityTowardOwner(orb, owner, strength, false);
            }
        }
    }

    private static void pullEntityTowardOwner(Entity target, LivingEntity owner, double strength, boolean matchOwnerHeight) {
        Vec3d pull = owner.getPos().subtract(target.getPos());
        Vec3d horizontal = new Vec3d(pull.x, 0.0, pull.z);
        if (horizontal.lengthSquared() <= 1.0E-6) {
            return;
        }
        Vec3d velocity = horizontal.normalize().multiply(strength);
        double upward = matchOwnerHeight && target instanceof LivingEntity livingTarget
                ? MathHelper.clamp((owner.getBodyY(0.55) - livingTarget.getBodyY(0.45)) * 0.08, -0.08, 0.16)
                : 0.08;
        target.addVelocity(velocity.x, upward, velocity.z);
        target.velocityModified = true;
        target.velocityDirty = true;
    }

    private static double horizontalDistanceSquared(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    @Override
    protected void doEffects(ServerWorld serverWorld, float baseDamage, Entity entity) {
        int bonusParticles =  ((int) baseDamage / 2);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.POOF, 0.5f, 3+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.CRIT, 0.5f, 5+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.SNOWFLAKE, 0.5f, 2+bonusParticles);
        if (baseDamage > primaryBaseDamage)
            serverWorld.playSoundFromEntity(null, entity, SoundRegistry.MAGIC_SWORD_PARRY_VARIOUS_HITS.get(),
                    this.getSoundCategory(), 0.3f, 1.2f);
    }

    @Override
    protected float doExtraDamage(Entity entity, float baseDamage, DamageSource damageSource) {
        lastImpactDamage = super.doExtraDamage(entity, baseDamage, damageSource);
        return lastImpactDamage;
    }

    @Override
    protected void onHit(LivingEntity target) {
        super.onHit(target);
        if (!(getWorld() instanceof ServerWorld world) || !(getOwner() instanceof LivingEntity owner)
                || !HelperMethods.checkAbilityTarget(target, owner)) return;
        if (throwExecution != null) UniqueAbilityApi.emit(throwExecution, UniqueAbilityPhase.HIT,
                Phase6UniqueAbilities.HIT, target, 1, lastImpactDamage);
        LivyatanAbilityManager.recordThrowHit(world, owner, stack, target, throwTuning);
        double splashRadius = throwTuning.get(s("LIVYATAN_SPLASH_RADIUS"), 0);
        int splashCap = throwTuning.integer(s("LIVYATAN_SPLASH_TARGET_CAP"), 0);
        float splashDamage = lastImpactDamage
                * (float) throwTuning.get(s("LIVYATAN_SPLASH_DAMAGE_MULTIPLIER"), 0);
        DamageSource source = owner.getDamageSources().trident(this, owner);
        for (LivingEntity splash : LivyatanAbilityManager.targets(world, target.getPos(), splashRadius,
                owner, splashCap, target.getUuid())) {
            WeaponImplicitRegistry.runSuppressed(() -> HelperMethods.damageThroughIframes(splash, source, splashDamage));
        }
    }

    @Override
    protected SoundEvent getReturnSound() {
        return SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_01.get();
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.LIVYATAN.get());
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

    @Override
    protected double getReturnSpeedMultiplier() {
        return returnTuning.get(s("LIVYATAN_RETURN_SPEED_MULTIPLIER"), 1);
    }

    @Override
    protected boolean tryPickup(net.minecraft.entity.player.PlayerEntity player) {
        int maelstromDuration = returnTuning.integer(s("LIVYATAN_MAELSTROM_DURATION_TICKS"), 0);
        if (maelstromDuration > 0 && returnTimer < maelstromDuration) return false;
        int totalCooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(asItemStack(), player, configuredCooldown);
        int remainingCooldown = Math.round(player.getItemCooldownManager()
                .getCooldownProgress(asItemStack().getItem(), 0) * totalCooldown);
        boolean pickedUp = super.tryPickup(player);
        if (pickedUp && !caught && isOwner(player)) {
            caught = true;
            int refund = returnHitTargets.size() >= returnTuning.integer(s("LIVYATAN_CATCH_HIT_THRESHOLD"), Integer.MAX_VALUE)
                    ? returnTuning.integer(s("LIVYATAN_CATCH_REFUND_TICKS"), 0) : 0;
            player.getItemCooldownManager().set(asItemStack().getItem(), Math.max(0, remainingCooldown - refund));
            if (returnExecution != null) UniqueAbilityApi.emit(returnExecution, UniqueAbilityPhase.HIT,
                    Phase6UniqueAbilities.CATCH, player, returnHitTargets.size(), refund);
            finishExecutions();
            if (getWorld() instanceof ServerWorld world) LivyatanAbilityManager.finishReturn(world, player.getUuid());
        }
        return pickedUp;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if ((this.stack == null || this.stack.isEmpty()) && nbt.contains("item")) {
            this.stack = ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("item")).orElse(this.getDefaultItemStack());
        }
        throwTuning = Phase6AbilityTuning.fromNbt(nbt.getCompound("LivyatanThrowTuning"));
        returnTuning = Phase6AbilityTuning.fromNbt(nbt.getCompound("LivyatanReturnTuning"));
        primaryBaseDamage = nbt.getFloat("LivyatanPrimaryDamage");
        primaryReturnDamage = nbt.getFloat("LivyatanReturnDamage");
        primaryReturnDamageRadius = nbt.getDouble("LivyatanReturnRadius");
        slownessDuration = nbt.getInt("LivyatanSlowTicks");
        configuredCooldown = nbt.getInt("LivyatanCooldown");
        returnTimer = nbt.getInt("LivyatanReturnTimer");
        returnStarted = nbt.getBoolean("LivyatanReturnStarted");
        returnToPlayer = nbt.getBoolean("LivyatanReturning");
        nonReturning = nbt.getBoolean("LivyatanNonReturning");
        if (nbt.contains("LivyatanNonReturningAge")) nonReturningMaxAge = nbt.getInt("LivyatanNonReturningAge");
        readUuidSet(nbt, "LivyatanLightning", returnLightningRolledTargets);
        readUuidSet(nbt, "LivyatanReturnHits", returnHitTargets);
        readRoundMap(nbt);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.put("LivyatanThrowTuning", throwTuning.toNbt());
        nbt.put("LivyatanReturnTuning", returnTuning.toNbt());
        nbt.putFloat("LivyatanPrimaryDamage", primaryBaseDamage);
        nbt.putFloat("LivyatanReturnDamage", primaryReturnDamage);
        nbt.putDouble("LivyatanReturnRadius", primaryReturnDamageRadius);
        nbt.putInt("LivyatanSlowTicks", slownessDuration);
        nbt.putInt("LivyatanCooldown", configuredCooldown);
        nbt.putInt("LivyatanReturnTimer", returnTimer);
        nbt.putBoolean("LivyatanReturnStarted", returnStarted);
        nbt.putBoolean("LivyatanReturning", returnToPlayer);
        nbt.putBoolean("LivyatanNonReturning", nonReturning);
        nbt.putInt("LivyatanNonReturningAge", nonReturningMaxAge);
        writeUuidSet(nbt, "LivyatanLightning", returnLightningRolledTargets);
        writeUuidSet(nbt, "LivyatanReturnHits", returnHitTargets);
        nbt.putInt("LivyatanRoundCount", maelstromRounds.size());
        int roundIndex = 0;
        for (Map.Entry<UUID, Integer> entry : maelstromRounds.entrySet()) {
            nbt.putUuid("LivyatanRoundTarget" + roundIndex, entry.getKey());
            nbt.putInt("LivyatanRoundValue" + roundIndex, entry.getValue());
            roundIndex++;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!getWorld().isClient()) {
            if (throwExecution != null) UniqueAbilityApi.cancel(throwExecution);
            if (returnExecution != null) UniqueAbilityApi.cancel(returnExecution);
            throwExecution = null;
            returnExecution = null;
        }
        super.remove(reason);
    }

    private void finishExecutions() {
        if (throwExecution != null) {
            UniqueAbilityApi.finish(throwExecution, Phase6UniqueAbilities.FINISH, returnHitTargets.size());
            throwExecution = null;
        }
        if (returnExecution != null) {
            UniqueAbilityApi.finish(returnExecution, Phase6UniqueAbilities.FINISH, returnHitTargets.size());
            returnExecution = null;
        }
    }

    private static void writeUuidSet(NbtCompound nbt, String key, Set<UUID> values) {
        nbt.putInt(key + "Count", values.size());
        int index = 0;
        for (UUID value : values) nbt.putUuid(key + index++, value);
    }

    private static void readUuidSet(NbtCompound nbt, String key, Set<UUID> values) {
        values.clear();
        int count = nbt.getInt(key + "Count");
        for (int index = 0; index < count; index++) {
            String entry = key + index;
            if (nbt.containsUuid(entry)) values.add(nbt.getUuid(entry));
        }
    }

    private void readRoundMap(NbtCompound nbt) {
        maelstromRounds.clear();
        int count = nbt.getInt("LivyatanRoundCount");
        for (int index = 0; index < count; index++) {
            String target = "LivyatanRoundTarget" + index;
            if (nbt.containsUuid(target)) {
                maelstromRounds.put(nbt.getUuid(target), nbt.getInt("LivyatanRoundValue" + index));
            }
        }
    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }

}
