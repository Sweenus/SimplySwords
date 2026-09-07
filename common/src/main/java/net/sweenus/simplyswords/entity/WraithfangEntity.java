package net.sweenus.simplyswords.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.ability.*;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.GloampiercerTuningSnapshot;
import net.sweenus.simplyswords.world.WraithfangAbilityManager;
import net.sweenus.simplyswords.world.WraithfangTuningSnapshot;
import java.util.*;
import static net.sweenus.simplyswords.world.WraithfangTuningSnapshot.*;

public class WraithfangEntity extends ThrownSpearEntity {
    private static final TrackedData<Boolean> WRAITHRIDE = DataTracker.registerData(WraithfangEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FLIGHT_POSE_ACTIVE = DataTracker.registerData(WraithfangEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TagKey<EntityType<?>> PIERCE_IMMUNE = TagKey.of(RegistryKeys.ENTITY_TYPE,
            Identifier.of("simplyswords", "wraithfang_pierce_immune"));
    private UniqueAbilityExecution execution;
    private WraithfangTuningSnapshot tuning = WraithfangTuningSnapshot.from(null);
    private final Set<UUID> hits = new HashSet<>();
    private final Set<UUID> contacts = new HashSet<>();
    private UUID anchorTarget, chainTarget;
    private Vec3d anchorPosition, lastOwnerPosition, groundedBlinkPosition;
    private boolean blinkUsed, flightPoseInitialized;
    private float flightYaw, flightPitch, previousFlightYaw, previousFlightPitch;
    private int elapsed, anchorAge, contactRefund;
    private double launchSpeed, castMultiplier = 1, echoDistance;
    private float anchorDamage;
    private boolean recalling, anchored, completed, arrivalUsed, wasSneaking, echo, second;
    private boolean grantNpcHaste;

    public WraithfangEntity(EntityType<? extends WraithfangEntity> type, World world) { super(type, world); }

    public WraithfangEntity(World world, LivingEntity owner, ItemStack stack) {
        super(EntityRegistry.WRAITHFANG.get(), world, owner, stack);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(WRAITHRIDE, false);
        builder.add(FLIGHT_POSE_ACTIVE, true);
    }

    public boolean hasWraithridePresentation() { return dataTracker.get(WRAITHRIDE); }

    public float flightYaw(float tickDelta) { return MathHelper.lerpAngleDegrees(tickDelta, previousFlightYaw, flightYaw); }

    public float flightPitch(float tickDelta) { return MathHelper.lerp(tickDelta, previousFlightPitch, flightPitch); }

    private void updateFlightPose() {
        previousFlightYaw = flightYaw;
        previousFlightPitch = flightPitch;
        Vec3d velocity = getVelocity();
        if (!hasWraithridePresentation() || !dataTracker.get(FLIGHT_POSE_ACTIVE)
                || inGround && !isNoClip() || velocity.lengthSquared() < 1.0E-6) return;
        double horizontal = velocity.horizontalLength();
        if (horizontal > 1.0E-6) flightYaw = (float) Math.toDegrees(Math.atan2(velocity.x, velocity.z)) - 90;
        flightPitch = (float) Math.toDegrees(Math.atan2(velocity.y, horizontal)) - 45;
        if (!flightPoseInitialized) {
            previousFlightYaw = flightYaw;
            previousFlightPitch = flightPitch;
            flightPoseInitialized = true;
        }
    }

    public void setAbilityExecution(UniqueAbilityExecution execution) {
        this.execution = execution;
        this.tuning = WraithfangTuningSnapshot.from(execution);
        dataTracker.set(WRAITHRIDE, tuning.hasMode(RIDE) && getOwner() instanceof PlayerEntity);
        hasLoyalty = tuning.loyalty();
    }

    public WraithfangTuningSnapshot masteryTuning() { return tuning; }

    public void configureMastery(LivingEntity target, double multiplier, boolean grantHasteOnDashComplete) {
        castMultiplier = multiplier;
        grantNpcHaste = grantHasteOnDashComplete;
        if (getOwner() instanceof LivingEntity owner) {
            wasSneaking = owner.isSneaking();
            lastOwnerPosition = owner.getPos();
            if (getWorld() instanceof ServerWorld world) second = WraithfangAbilityManager.isSecond(world, owner);
        }
    }

    public void configureEcho(float damage) {
        echo = true;
        dataTracker.set(WRAITHRIDE, false);
        primaryBaseDamage = damage;
        markNonReturning(40);
        pickupType = PickupPermission.DISALLOWED;
        setNoGravity(true);
        weightValue = 0;
    }

    @Override
    public void tick() {
        if (!(getWorld() instanceof ServerWorld world)) {
            if (hasWraithridePresentation()) weightValue = 0;
            super.tick();
            updateFlightPose();
            return;
        }
        if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || owner.getWorld() != world) {
            if (echo || nonReturning) discard();
            else if (getOwner() != null) {
                if (pickupType == PickupPermission.ALLOWED) dropStack(stack.copy(), .1F);
                discard();
            } else { recalling = true; completed = true; hasLoyalty = tuning.loyalty(); beginReturn(); super.tick(); }
            return;
        }
        elapsed++;
        if (launchSpeed == 0) launchSpeed = Math.max(.1, getVelocity().length());
        if (echo) {
            echoDistance += getVelocity().length();
            if (echoDistance > 12) { discard(); return; }
            world.spawnParticles(ParticleTypes.SOUL, getX(), getY(), getZ(), 2, .08, .08, .08, .01);
            super.tick();
            return;
        }
        WraithfangAbilityManager.launched(world, owner, this);
        if (inGroundTime > 4 && !anchored && !recalling) recall(world, owner);
        if (net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry.isIncapacitated(owner)) {
            complete(world, owner);
            if (anchored) detonate(world, owner); else if (!recalling) recall(world, owner);
        }
        if (elapsed >= tuning.dashDurationTicks() && !completed) complete(world, owner);
        if (elapsed >= tuning.dashDurationTicks() && !anchored && !recalling) recall(world, owner);
        if (anchored) tickAnchor(world, owner);
        if (remote(owner)) tickPossession(world, owner);
        else if (!completed) tickPursuit(world, owner);
        if (!anchored && !recalling && !inGround) guide(world, owner);
        if ((tuning.hasMode(RIDE) && owner instanceof PlayerEntity) || anchored) {
            weightValue = 0;
            setNoGravity(true);
        }
        Vec3d fixed = anchored ? getPos() : null;
        dataTracker.set(FLIGHT_POSE_ACTIVE, !anchored && (!inGround || recalling));
        super.tick();
        if (anchored && fixed != null) { setPosition(fixed); setVelocity(Vec3d.ZERO); }
        if (isRemoved() && execution != null && !execution.isTerminal()) UniqueAbilityApi.finish(execution, execution.definition().id(), hits.size());
    }

    private boolean remote(LivingEntity owner) { return tuning.hasMode(POSSESSION) && owner instanceof PlayerEntity; }

    private void guide(ServerWorld world, LivingEntity owner) {
        LivingEntity target = chainTarget == null ? null : world.getEntity(chainTarget) instanceof LivingEntity living ? living : null;
        Vec3d desired = null;
        double turn = tuning.steeringDegrees();
        if (chainTarget != null) {
            if (target == null || !target.isAlive() || !eligible(owner, target)
                    || !visible(world, getPos(), target.getBoundingBox().getCenter())) {
                recall(world, owner);
                return;
            }
            desired = target.getBoundingBox().getCenter().subtract(getPos());
            turn = 30;
        } else if (tuning.hasMode(RIDE) && owner instanceof PlayerEntity) desired = owner.getRotationVec(1);
        if (desired != null && desired.lengthSquared() > 1.0E-6) {
            Vec3d current = getVelocity().lengthSquared() > 1.0E-6 ? getVelocity().normalize() : desired.normalize();
            setVelocity(GloampiercerTuningSnapshot.turnToward(current, desired.normalize(), Math.toRadians(turn)).multiply(launchSpeed));
            velocityDirty = true;
        }
    }

    private void tickPossession(ServerWorld world, LivingEntity owner) {
        boolean pressed = owner.isSneaking() && !wasSneaking;
        wasSneaking = owner.isSneaking();
        if (blinkUsed || net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry.isIncapacitated(owner)) return;
        if (elapsed % 20 == 1) WraithfangAbilityManager.message(owner, "message.simplyswords.wraithfang.possession");
        if (pressed) blink(world, owner);
    }

    private void tickPursuit(ServerWorld world, LivingEntity owner) {
        Vec3d direction = getPos().subtract(owner.getPos());
        double distance = direction.length();
        if (distance > 1 && distance < 500) {
            double speed = owner instanceof PlayerEntity ? distance / 8 * (tuning.dashSpeed() / 1.35) : tuning.dashSpeed();
            owner.setVelocity(direction.normalize().multiply(speed));
            owner.velocityModified = true;
            owner.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.RESILIENCE), 20, 4, false, false, true));
            world.spawnParticles(ParticleTypes.OMINOUS_SPAWNING, owner.getX(), owner.getBodyY(.5), owner.getZ(), 3, .2, .2, .2, .02);
        }
        if (tuning.hasMode(SAFE_LANDING)) WraithfangAbilityManager.safeLanding(world, owner);
        passage(world, owner, lastOwnerPosition == null ? owner.getPos() : lastOwnerPosition, owner.getPos());
        lastOwnerPosition = owner.getPos();
    }

    private void blink(ServerWorld world, LivingEntity owner) {
        Vec3d blinkTarget = inGround && !recalling && groundedBlinkPosition != null ? groundedBlinkPosition : getPos();
        if (owner.squaredDistanceTo(this) > tuning.dashTargetRange() * tuning.dashTargetRange()
                || !visible(world, owner.getEyePos(), blinkTarget)) return;
        Vec3d toward = owner.getPos().subtract(blinkTarget).multiply(1, 0, 1).normalize();
        Vec3d destination = null;
        for (double radius : new double[]{1, 1.5, 2, 0}) {
            for (double height : new double[]{0, 1, -1}) {
                Vec3d candidate = blinkTarget.add(toward.multiply(radius)).add(0, height, 0);
                Box box = owner.getBoundingBox().offset(candidate.subtract(owner.getPos()));
                BlockPos block = BlockPos.ofFloored(candidate);
                if (candidate.y < world.getBottomY() || candidate.y + owner.getHeight() >= world.getTopY()
                        || !world.isChunkLoaded(block) || !world.getWorldBorder().contains(box)
                        || !world.isSpaceEmpty(owner, box) || world.containsFluid(box)
                        || world.getBlockState(block).isIn(net.minecraft.registry.tag.BlockTags.FIRE)
                        || !visible(world, owner.getEyePos(), candidate.add(0, owner.getStandingEyeHeight(), 0))) continue;
                destination = candidate;
                break;
            }
            if (destination != null) break;
        }
        if (destination == null) return;
        blinkUsed = true;
        Vec3d origin = owner.getPos();
        if (owner instanceof ServerPlayerEntity player) player.networkHandler.requestTeleport(destination.x, destination.y, destination.z, owner.getYaw(), owner.getPitch());
        else owner.refreshPositionAndAngles(destination.x, destination.y, destination.z, owner.getYaw(), owner.getPitch());
        owner.setVelocity(Vec3d.ZERO);
        owner.velocityModified = true;
        passage(world, owner, origin, destination);
        arrival(world, owner);
        complete(world, owner);
        if (tuning.hasMode(SAFE_LANDING)) WraithfangAbilityManager.safeLanding(world, owner);
        if (anchored) detonate(world, owner); else if (!recalling) recall(world, owner);
        world.spawnParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1, destination.z, 30, .4, .6, .4, .1);
    }

    private void complete(ServerWorld world, LivingEntity owner) {
        if (completed) return;
        completed = true;
        if (tuning.hasMode(SAFE_LANDING)) WraithfangAbilityManager.safeLanding(world, owner);
        if (grantNpcHaste) WraithfangAbilityManager.onPursuitComplete(world, owner, stack, tuning);
    }

    private void recall(ServerWorld world, LivingEntity owner) {
        groundedBlinkPosition = null;
        anchored = false;
        recalling = true;
        chainTarget = null;
        hasLoyalty = Math.max(1, tuning.loyalty());
        setVelocity(Vec3d.ZERO);
        setNoGravity(true);
        weightValue = 0;
        beginReturn();
        if (nonReturning) { complete(world, owner); discard(); }
    }

    private float baseDamage() { return primaryBaseDamage * (float) castMultiplier; }

    private float throwDamage() {
        double growth = Math.max(Math.min(elapsed, 1200) * .5,
                Math.min(elapsed, tuning.flightDamageCapTicks()) * tuning.flightDamagePerTick());
        return (primaryBaseDamage + (float) growth) * (float) tuning.projectileDamageMultiplier() * (float) castMultiplier;
    }

    @Override
    protected boolean canHit(Entity entity) {
        return !recalling && !anchored && !hits.contains(entity.getUuid()) && super.canHit(entity)
                && (!(getOwner() instanceof LivingEntity owner) || !(entity instanceof LivingEntity target) || eligible(owner, target));
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        if (!(getWorld() instanceof ServerWorld world) || !(getOwner() instanceof LivingEntity owner)
                || !(result.getEntity() instanceof LivingEntity target) || !eligible(owner, target) || hits.contains(target.getUuid())) return;
        float raw = echo ? primaryBaseDamage : throwDamage() * (tuning.hasMode(BANSHEE) ? (float) tuning.pierceDamageMultiplier() : 1);
        if (!echo && tuning.hasMode(HAUNTED)) raw *= WraithfangAbilityManager.hauntedMultiplier(world, owner, target);
        boolean damaged;
        if (echo) damaged = magic(world, owner, target, raw, false);
        else {
            var source = getDamageSources().trident(this, owner);
            float adjusted = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, raw);
            damaged = HelperMethods.damageThroughIframes(target, source, adjusted);
            if (damaged) EnchantmentHelper.onTargetDamaged(world, target, source, stack);
        }
        if (echo) { discard(); return; }
        if (!damaged) { recall(world, owner); return; }
        boolean first = hits.isEmpty();
        hits.add(target.getUuid());
        if (stack.getItem() instanceof net.minecraft.item.SwordItem sword) sword.postHit(stack, target, owner);
        if (tuning.weaknessDurationTicks() > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, tuning.weaknessDurationTicks(), tuning.weaknessAmplifier()), owner);
        if (tuning.hasMode(HAUNTED)) WraithfangAbilityManager.markHaunted(world, owner, target);
        if (first && tuning.hasMode(DESTINATION)) WraithfangAbilityManager.markDestination(world, owner, target, stack);
        if (execution != null) UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, AbyssalSpectralMasteryAbilities.HIT, target, 1, raw);
        if (!target.isAlive()) {
            WraithfangAbilityManager.onThrownKill(world, owner, stack, tuning);
            if (execution != null) UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, AbyssalSpectralMasteryAbilities.KILL, target, 1, raw);
        }
        if (tuning.burstRadius() > 0 && WraithfangAbilityManager.tryBurst(world, owner, tuning.burstLockoutTicks()))
            area(world, owner, target.getPos(), tuning.burstRadius(), tuning.burstTargetCap(), raw * (float) tuning.burstDamageMultiplier(), target, false);
        playSound(SoundRegistry.ELEMENTAL_BOW_WIND_SHOOT_IMPACT_02.get(), .4F, 1.1F);
        if (tuning.hasMode(HARPOON)) { embed(target.isAlive() ? target : null, result.getPos(), raw); return; }
        if (tuning.hasMode(BANSHEE) && hits.size() < tuning.pierceCount() && !target.getType().isIn(PIERCE_IMMUNE)) {
            List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(10),
                    candidate -> eligible(owner, candidate) && !hits.contains(candidate.getUuid())
                            && candidate.squaredDistanceTo(target) <= 100 && visible(world, result.getPos(), candidate.getBoundingBox().getCenter()));
            targets.sort(Comparator.comparingDouble(target::squaredDistanceTo));
            if (!targets.isEmpty()) {
                chainTarget = targets.getFirst().getUuid();
                setPosition(result.getPos());
                setVelocity(targets.getFirst().getBoundingBox().getCenter().subtract(getPos()).normalize().multiply(launchSpeed));
                velocityDirty = true;
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 12, .2, .2, .2, .04);
                return;
            }
        }
        recall(world, owner);
    }

    @Override
    protected void onBlockHit(BlockHitResult result) {
        if (!(getWorld() instanceof ServerWorld world) || !(getOwner() instanceof LivingEntity owner)) { super.onBlockHit(result); return; }
        if (echo) { discard(); return; }
        groundedBlinkPosition = result.getPos().add(Vec3d.of(result.getSide().getVector()).multiply(.15));
        if (tuning.hasMode(HARPOON) && !recalling) {
            super.onBlockHit(result);
            embed(null, result.getPos().add(Vec3d.of(result.getSide().getVector()).multiply(.15)), throwDamage());
        } else {
            super.onBlockHit(result);
            if (tuning.hasMode(BANSHEE)) recall(world, owner);
        }
    }

    private void embed(LivingEntity target, Vec3d position, float damage) {
        anchored = true;
        anchorAge = 0;
        anchorTarget = target == null ? null : target.getUuid();
        anchorPosition = position;
        anchorDamage = damage;
        hasLoyalty = 0;
        setPosition(position);
        setVelocity(Vec3d.ZERO);
        setNoGravity(true);
        weightValue = 0;
    }

    private void tickAnchor(ServerWorld world, LivingEntity owner) {
        anchorAge++;
        if (anchorTarget != null && world.getEntity(anchorTarget) instanceof LivingEntity target && target.isAlive()) anchorPosition = target.getBoundingBox().getCenter();
        if (anchorPosition != null) setPosition(anchorPosition);
        if (anchorAge >= 40 || anchorAge >= 10 && owner.squaredDistanceTo(this) <= 4) { detonate(world, owner); return; }
        if (anchorAge % 10 == 0) area(world, owner, getPos(), 3, 6, anchorDamage * .25F, null, false);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 4, .3, .3, .3, .01);
    }

    private void detonate(ServerWorld world, LivingEntity owner) {
        if (!anchored) return;
        area(world, owner, getPos(), 4, 8, anchorDamage * 1.5F, null, false);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 60, 1.5, 1, 1.5, .12);
        playSound(SoundRegistry.DISTORTION_ARC_03.get(), .6F, .7F);
        recall(world, owner);
    }

    private void passage(ServerWorld world, LivingEntity owner, Vec3d start, Vec3d end) {
        if (!tuning.hasMode(PASSAGE) || contacts.size() >= tuning.dashContactTargetCap()) return;
        double width = owner.getWidth() / 2.0;
        Box broad = owner.getBoundingBox().offset(start.subtract(owner.getPos())).union(owner.getBoundingBox().offset(end.subtract(owner.getPos())));
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, broad.expand(.1), target -> eligible(owner, target) && !contacts.contains(target.getUuid()));
        targets.sort(Comparator.comparingDouble(target -> target.squaredDistanceTo(start)));
        for (LivingEntity target : targets) {
            Box expanded = target.getBoundingBox().expand(width, 0, width);
            expanded = new Box(expanded.minX, expanded.minY - owner.getHeight(), expanded.minZ, expanded.maxX, expanded.maxY, expanded.maxZ);
            if (!expanded.contains(start) && !expanded.contains(end) && expanded.raycast(start, end).isEmpty()) continue;
            if (!visible(world, end.add(0, owner.getHeight() / 2, 0), target.getBoundingBox().getCenter())) continue;
            if (magic(world, owner, target, baseDamage() * (float) tuning.dashContactDamageMultiplier(), true)) contacts.add(target.getUuid());
            if (contacts.size() >= tuning.dashContactTargetCap()) break;
        }
    }

    private void arrival(ServerWorld world, LivingEntity owner) {
        if (arrivalUsed || !tuning.hasMode(ARRIVAL)) return;
        arrivalUsed = true;
        area(world, owner, owner.getPos(), tuning.arrivalRange(), 5, baseDamage() * (float) tuning.arrivalDamageMultiplier(), null, true);
    }

    private boolean magic(ServerWorld world, LivingEntity owner, LivingEntity target, float damage, boolean refund) {
        boolean applied = SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, target, damage, SpellScalingProfile.SOUL);
        if (applied && refund && !target.isAlive() && tuning.hasMode(RELENTLESS) && contactRefund < 40) {
            SimplySwordsAPI.reduceWeaponCooldown(owner, stack, 1, 10);
            contactRefund += 10;
        }
        return applied;
    }

    private void area(ServerWorld world, LivingEntity owner, Vec3d center, double radius, int cap, float damage, LivingEntity exclude, boolean refund) {
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(radius),
                target -> target != exclude && eligible(owner, target) && target.squaredDistanceTo(center) <= radius * radius
                        && visible(world, center.add(0, .25, 0), target.getBoundingBox().getCenter()));
        targets.sort(Comparator.comparingDouble(target -> target.squaredDistanceTo(center)));
        int applied = 0;
        for (LivingEntity target : targets) if (magic(world, owner, target, damage, refund) && ++applied >= cap) break;
    }

    private boolean visible(ServerWorld world, Vec3d start, Vec3d end) {
        return world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private static boolean eligible(LivingEntity owner, LivingEntity target) {
        return owner != target && target.isAlive() && HelperMethods.checkAbilityTarget(target, owner);
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        if (echo) return false;
        ItemStack returned = asItemStack().copy();
        boolean catchReturn = recalling && isNoClip();
        boolean picked = super.tryPickup(player);
        if (picked && player.getWorld() instanceof ServerWorld world) {
            stack = returned;
            if (catchReturn) {
                arrival(world, player);
                WraithfangAbilityManager.onReturn(world, player, returned, tuning, baseDamage(), second);
                if (execution != null) UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, AbyssalSpectralMasteryAbilities.RETURN, player, 1, 0);
            } else WraithfangAbilityManager.onGroundPickup(world, player, returned, tuning);
            complete(world, player);
            playSound(SoundRegistry.ELEMENTAL_BOW_WIND_SHOOT_IMPACT_02.get(), .3F, 1.2F);
            if (execution != null) UniqueAbilityApi.finish(execution, execution.definition().id(), hits.size());
        }
        return picked;
    }

    @Override protected int getReturnPickupCooldownTicks() { return -1; }
    @Override protected ItemStack asItemStack() { return stack == null ? getDefaultItemStack() : stack.copy(); }
    @Override protected ItemStack getDefaultItemStack() { return ItemsRegistry.WRAITHFANG.get().getDefaultStack(); }
    @Override protected byte getLoyalty() { return (byte) Math.max(0, hasLoyalty); }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        tuning.write(nbt);
        nbt.putBoolean("wf_blink_used", blinkUsed);
        if (groundedBlinkPosition != null) {
            nbt.putDouble("wf_ground_x", groundedBlinkPosition.x);
            nbt.putDouble("wf_ground_y", groundedBlinkPosition.y);
            nbt.putDouble("wf_ground_z", groundedBlinkPosition.z);
        }
        nbt.putInt("wf_elapsed", elapsed); nbt.putInt("wf_anchor_age", anchorAge);
        nbt.putFloat("wf_damage", primaryBaseDamage); nbt.putFloat("wf_anchor_damage", anchorDamage);
        nbt.putDouble("wf_multiplier", castMultiplier); nbt.putDouble("wf_speed", launchSpeed); nbt.putDouble("wf_echo_distance", echoDistance);
        nbt.putBoolean("wf_recall", recalling); nbt.putBoolean("wf_anchor", anchored); nbt.putBoolean("wf_complete", completed);
        nbt.putBoolean("wf_arrival", arrivalUsed); nbt.putBoolean("wf_sneak", wasSneaking); nbt.putBoolean("wf_echo", echo);
        nbt.putBoolean("wf_second", second); nbt.putBoolean("wf_npc_haste", grantNpcHaste); nbt.putBoolean("wf_offhand", offhandThrow);
        nbt.putBoolean("wf_nonreturning", nonReturning); nbt.putInt("wf_npc_lifetime", nonReturningMaxAge); nbt.putInt("wf_refund", contactRefund);
        if (anchorTarget != null) nbt.putUuid("wf_target", anchorTarget);
        if (chainTarget != null) nbt.putUuid("wf_chain", chainTarget);
        if (anchorPosition != null) { nbt.putDouble("wf_x", anchorPosition.x); nbt.putDouble("wf_y", anchorPosition.y); nbt.putDouble("wf_z", anchorPosition.z); }
        int index = 0; for (UUID id : hits) nbt.putUuid("wf_hit_" + index++, id); nbt.putInt("wf_hits", index);
        index = 0; for (UUID id : contacts) nbt.putUuid("wf_contact_" + index++, id); nbt.putInt("wf_contacts", index);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        tuning = WraithfangTuningSnapshot.read(nbt);
        blinkUsed = nbt.getBoolean("wf_blink_used");
        groundedBlinkPosition = nbt.contains("wf_ground_x")
                ? new Vec3d(nbt.getDouble("wf_ground_x"), nbt.getDouble("wf_ground_y"), nbt.getDouble("wf_ground_z")) : null;
        elapsed = nbt.getInt("wf_elapsed"); age = elapsed; anchorAge = nbt.getInt("wf_anchor_age");
        primaryBaseDamage = nbt.getFloat("wf_damage"); anchorDamage = nbt.getFloat("wf_anchor_damage");
        castMultiplier = nbt.contains("wf_multiplier") ? nbt.getDouble("wf_multiplier") : 1;
        launchSpeed = nbt.getDouble("wf_speed"); echoDistance = nbt.getDouble("wf_echo_distance");
        recalling = nbt.getBoolean("wf_recall"); anchored = nbt.getBoolean("wf_anchor"); completed = nbt.getBoolean("wf_complete");
        arrivalUsed = nbt.getBoolean("wf_arrival"); wasSneaking = nbt.getBoolean("wf_sneak"); echo = nbt.getBoolean("wf_echo");
        second = nbt.getBoolean("wf_second"); grantNpcHaste = nbt.getBoolean("wf_npc_haste"); offhandThrow = nbt.getBoolean("wf_offhand");
        nonReturning = nbt.getBoolean("wf_nonreturning"); nonReturningMaxAge = nbt.getInt("wf_npc_lifetime"); contactRefund = nbt.getInt("wf_refund");
        dataTracker.set(WRAITHRIDE, tuning.hasMode(RIDE) && !echo && !nonReturning);
        dataTracker.set(FLIGHT_POSE_ACTIVE, !anchored && (!inGround || recalling));
        returnToPlayer = !nonReturning;
        if (nbt.containsUuid("wf_target")) anchorTarget = nbt.getUuid("wf_target");
        if (nbt.containsUuid("wf_chain")) chainTarget = nbt.getUuid("wf_chain");
        if (nbt.contains("wf_x")) anchorPosition = new Vec3d(nbt.getDouble("wf_x"), nbt.getDouble("wf_y"), nbt.getDouble("wf_z"));
        hits.clear(); for (int i = 0; i < Math.min(16, nbt.getInt("wf_hits")); i++) if (nbt.containsUuid("wf_hit_" + i)) hits.add(nbt.getUuid("wf_hit_" + i));
        contacts.clear(); for (int i = 0; i < Math.min(64, nbt.getInt("wf_contacts")); i++) if (nbt.containsUuid("wf_contact_" + i)) contacts.add(nbt.getUuid("wf_contact_" + i));
        hasLoyalty = echo || anchored || nonReturning ? 0 : tuning.loyalty();
        if (recalling) beginReturn();
    }

}
