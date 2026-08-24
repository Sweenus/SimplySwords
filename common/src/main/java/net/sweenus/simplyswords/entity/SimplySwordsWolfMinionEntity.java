package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.DyeColor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.sweenus.simplyswords.entity.goal.MinionTargetPriorityGoal;
import net.sweenus.simplyswords.util.MinionTargeting;
import net.sweenus.simplyswords.entity.goal.WolfLungeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SimplySwordsWolfMinionEntity extends WolfEntity implements Tameable, SimplySwordsMinion {

    private static final double OWNER_FOLLOW_DISTANCE = 14.0;
    private static final double HEALTH_PER_ATTACK = 8.0;
    private static final float ARMOR_CHANCE = 0.55F;
    private static final double OWNER_TELEPORT_DISTANCE = 32.0;
    private UUID ownerUuid;
    private long expiresAtTick;
    private float weaponDamage;
    private int sourceWeaponSlot = -1;
    private long nextActiveAbilityCheckTick;
    private long activeAbilityCooldownUntilTick;

    public SimplySwordsWolfMinionEntity(EntityType<? extends WolfEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setCanPickUpLoot(false);
    }

    public static DefaultAttributeContainer.Builder createMinionAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new WolfLungeAttackGoal(this, 1.15, 14.0));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.85));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new MinionTargetPriorityGoal(this, world -> getOwner(world), this::isValidMinionTarget, 16.0, 20));
    }

    public void initializeMinion(LivingEntity owner, ItemStack stack, int sourceWeaponSlot, long expiresAtTick, float weaponDamage) {
        this.ownerUuid = owner.getUuid();
        this.expiresAtTick = expiresAtTick;
        this.weaponDamage = Math.max(0.0F, weaponDamage);
        this.sourceWeaponSlot = sourceWeaponSlot;
        this.setTamed(true, false);
        this.setSitting(false);
        this.equipStack(EquipmentSlot.MAINHAND, stack.copy());
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        EntityAttributeInstance health = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null) {
            double scaled = health.getBaseValue() + weaponDamage * HEALTH_PER_ATTACK;
            health.setBaseValue(scaled);
            this.setHealth((float) scaled);
        }
        this.setPersistent();
        this.setCustomName(owner.getName().copy().append("'s Wolf"));
        this.setVariant(this.getRegistryManager()
                .get(RegistryKeys.WOLF_VARIANT)
                .getRandom(this.getRandom())
                .orElseThrow());
        if (this.random.nextFloat() <= ARMOR_CHANCE) {
            net.minecraft.util.DyeColor dye = net.minecraft.util.DyeColor.values()[this.random.nextInt(net.minecraft.util.DyeColor.values().length)];
            ItemStack wolfArmor = new ItemStack(net.minecraft.item.Items.WOLF_ARMOR);
            wolfArmor.set(net.minecraft.component.DataComponentTypes.DYED_COLOR, new net.minecraft.component.type.DyedColorComponent(dye.getSignColor(), false));
            this.equipStack(EquipmentSlot.BODY, wolfArmor);
            this.setEquipmentDropChance(EquipmentSlot.BODY, 0.0F);
        }
    }

    public void initializeMinion(ServerPlayerEntity owner, ItemStack stack, int sourceWeaponSlot, long expiresAtTick, float weaponDamage) {
        this.ownerUuid = owner.getUuid();
        this.expiresAtTick = expiresAtTick;
        this.weaponDamage = Math.max(0.0F, weaponDamage);
        this.sourceWeaponSlot = sourceWeaponSlot;
        this.setTamed(true, false);
        this.setSitting(false);
        this.equipStack(EquipmentSlot.MAINHAND, stack.copy());
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        EntityAttributeInstance health = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null) {
            double scaled = health.getBaseValue() + weaponDamage * HEALTH_PER_ATTACK;
            health.setBaseValue(scaled);
            this.setHealth((float) scaled);
        }
        this.setPersistent();
        this.setCustomName(owner.getName().copy().append("'s Wolf"));
        this.setVariant(this.getRegistryManager()
                .get(RegistryKeys.WOLF_VARIANT)
                .getRandom(this.getRandom())
                .orElseThrow());
        if (this.random.nextFloat() <= ARMOR_CHANCE) {
            DyeColor dye = DyeColor.values()[this.random.nextInt(DyeColor.values().length)];
            ItemStack wolfArmor = new ItemStack(Items.WOLF_ARMOR);
            wolfArmor.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(dye.getSignColor(), false));
            this.equipStack(EquipmentSlot.BODY, wolfArmor);
            this.setEquipmentDropChance(EquipmentSlot.BODY, 0.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld world) || this.expiresAtTick <= 0L || world.getTime() > this.expiresAtTick) {
            expire();
            return;
        }

        if (this.isOnFire()) {
            this.extinguish();
        }

        LivingEntity owner = getOwner(world);
        if (owner == null || !owner.isAlive()) {
            expire();
            return;
        }

        followOwner(owner);
        tryActivateWeaponAbility(world, owner);
        if (this.age % MinionTargeting.tauntIntervalTicks() == 0) {
            MinionTargeting.tauntNearbyEnemies(world, this, owner, this::isValidMinionTarget);
            MinionTargeting.cleanupMarkedTargets(world, owner);
        }
        if (world.getTime() % 12L == 0L) {
            world.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getBodyY(0.65), this.getZ(), 2, 0.18, 0.22, 0.18, 0.01);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (!(target instanceof LivingEntity livingTarget) || !(this.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        LivingEntity owner = getOwner(world);
        ItemStack visualStack = this.getMainHandStack();
        ItemStack effectStack = resolveEffectWeaponStack(owner instanceof ServerPlayerEntity sp ? sp : null, visualStack);
        if (owner == null || visualStack.isEmpty() || effectStack.isEmpty() || !isValidMinionTarget(livingTarget)) {
            return false;
        }

        float damage = (float) (this.weaponDamage * Config.gemPowers.wolfPack.damageScaling);
        if (damage <= 0.0F) {
            return false;
        }

        this.swingHand(Hand.MAIN_HAND);
        boolean damaged = SimplySwordsAPI.applyDelegatedWeaponHit(effectStack, livingTarget, owner, this, damage);
        if (damaged) {
            Vec3d hitPos = livingTarget.getPos().add(0.0, Math.max(0.35, livingTarget.getHeight() * 0.55), 0.0);
            world.spawnParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 5, 0.18, 0.12, 0.18, 0.025);
            world.playSound(null, livingTarget.getBlockPos(), SoundEvents.ENTITY_WOLF_SHAKE, SoundCategory.PLAYERS, 0.28F, 0.9F + world.random.nextFloat() * 0.2F);
        }
        return damaged;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity player && isProtectedFromPlayer(player)) {
            return false;
        }
        if (isSiblingMinion(attacker)) {
            return false;
        }
        if (attacker != null && this.ownerUuid != null && this.ownerUuid.equals(attacker.getUuid())) {
            return false;
        }
        if (attacker instanceof LivingEntity livingAttacker && isValidMinionTarget(livingAttacker)) {
            this.setTarget(livingAttacker);
        }
        return super.damage(source, amount);
    }

    private boolean isSiblingMinion(Entity attacker) {
        if (this.ownerUuid == null || attacker == null || attacker == this) {
            return false;
        }
        if (attacker instanceof Tameable tameable) {
            return this.ownerUuid.equals(tameable.getOwnerUuid());
        }
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return ActionResult.FAIL;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    public boolean trySetRetaliationTarget(LivingEntity attacker) {
        if (!isValidMinionTarget(attacker)) {
            return false;
        }
        this.setTarget(attacker);
        this.navigation.startMovingTo(attacker, 1.3);
        return true;
    }

    private void followOwner(LivingEntity owner) {
        double distance = this.squaredDistanceTo(owner);
        if (distance > OWNER_TELEPORT_DISTANCE * OWNER_TELEPORT_DISTANCE) {
            this.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), owner.getYaw(), owner.getPitch());
            this.navigation.stop();
            return;
        }
        if (distance > OWNER_FOLLOW_DISTANCE * OWNER_FOLLOW_DISTANCE && this.getTarget() == null) {
            this.navigation.startMovingTo(owner, 1.15);
        }
    }

    private void followOwner(ServerPlayerEntity owner) {
        double distance = this.squaredDistanceTo(owner);
        if (distance > OWNER_TELEPORT_DISTANCE * OWNER_TELEPORT_DISTANCE) {
            this.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), owner.getYaw(), owner.getPitch());
            this.navigation.stop();
            return;
        }
        if (distance > OWNER_FOLLOW_DISTANCE * OWNER_FOLLOW_DISTANCE && this.getTarget() == null) {
            this.navigation.startMovingTo(owner, 1.15);
        }
    }

    private void tryActivateWeaponAbility(ServerWorld world, LivingEntity owner) {
        LivingEntity target = this.getTarget();
        int interval = Math.max(1, Config.gemPowers.wolfPack.activeAbilityCheckInterval);
        long now = world.getTime();
        if (this.nextActiveAbilityCheckTick <= 0L) {
            this.nextActiveAbilityCheckTick = now + Math.max(0, Config.gemPowers.wolfPack.activeAbilityInitialDelay);
            return;
        }
        if (now < this.nextActiveAbilityCheckTick) {
            return;
        }
        this.nextActiveAbilityCheckTick = now + interval;
        if (now < this.activeAbilityCooldownUntilTick || !isValidMinionTarget(target)) {
            return;
        }

        ItemStack visualStack = this.getMainHandStack();
        ItemStack effectStack = resolveEffectWeaponStack(owner instanceof ServerPlayerEntity sp ? sp : null, visualStack);
        WeaponAbilityContext context = WeaponAbilityContext.of(world, effectStack, this, owner instanceof ServerPlayerEntity sp ? sp : null, target, Hand.MAIN_HAND, WeaponAbilityActivationSource.MINION);
        if (!SimplySwordsAPI.canActivateWeaponAbility(context)) {
            return;
        }
        int chance = Math.clamp(Config.gemPowers.wolfPack.activeAbilityChance, 0, 100);
        if (chance <= 0 || this.random.nextInt(100) >= chance) {
            return;
        }

        if (SimplySwordsAPI.tryActivateWeaponAbility(context)) {
            this.swingHand(Hand.MAIN_HAND);
            this.activeAbilityCooldownUntilTick = now + Math.max(1, SimplySwordsAPI.getWeaponAbilityCooldownTicks(context));
            Vec3d pos = this.getPos().add(0.0, this.getHeight() * 0.7, 0.0);
            world.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 8, 0.25, 0.25, 0.25, 0.035);
        }
    }

    private ItemStack resolveEffectWeaponStack(@Nullable ServerPlayerEntity owner, ItemStack visualStack) {
        if (owner == null || visualStack == null || visualStack.isEmpty() || this.sourceWeaponSlot < 0
                || this.sourceWeaponSlot >= owner.getInventory().size()) {
            return visualStack;
        }
        ItemStack sourceStack = owner.getInventory().getStack(this.sourceWeaponSlot);
        return !sourceStack.isEmpty() && sourceStack.isOf(visualStack.getItem()) ? sourceStack : visualStack;
    }

    private boolean isValidMinionTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target == this) {
            return false;
        }
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        LivingEntity owner = getOwner(world);
        if (owner == null || target == owner) {
            return false;
        }
        if (isSiblingMinion(target)) {
            return false;
        }
        if (owner instanceof ServerPlayerEntity) {
            if (!HelperMethods.checkFriendlyFire(target, owner)) {
                return false;
            }
            if (!HelperMethods.isMonsterFaction(target)) {
                return MinionTargeting.isMarkedTarget(world, owner, target);
            }
            return true;
        }
        if (owner instanceof MobEntity mobOwner && target == mobOwner.getTarget()) {
            return true;
        }
        if (MinionTargeting.getRecentAttackTarget(world, owner) == target) {
            return true;
        }
        if (HelperMethods.isMonsterFaction(owner)) {
            if (HelperMethods.isMonsterFaction(target)) {
                return false;
            }
            return HelperMethods.checkFriendlyFire(target, owner);
        }
        return HelperMethods.checkFriendlyFire(target, owner);
    }

    private boolean isProtectedFromPlayer(PlayerEntity player) {
        if (this.ownerUuid != null && this.ownerUuid.equals(player.getUuid())) {
            return true;
        }
        return !HelperMethods.checkFriendlyFire(this, player);
    }

    @Nullable
    private ServerPlayerEntity getOwnerPlayer(ServerWorld world) {
        return this.ownerUuid == null ? null : world.getServer().getPlayerManager().getPlayer(this.ownerUuid);
    }

    @Nullable
    private LivingEntity getOwner(ServerWorld world) {
        if (this.ownerUuid == null) return null;
        ServerPlayerEntity player = getOwnerPlayer(world);
        if (player != null) return player;
        Entity entity = world.getEntity(this.ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void expire() {
        if (this.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.POOF, this.getX(), this.getBodyY(0.5), this.getZ(), 12, 0.35, 0.35, 0.35, 0.04);
            world.spawnParticles(ParticleTypes.CLOUD, this.getX(), this.getBodyY(0.7), this.getZ(), 8, 0.28, 0.28, 0.28, 0.035);
        }
        this.discard();
    }

    @Nullable
    @Override
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public LivingEntity getOwner() {
        if (this.ownerUuid == null) return null;
        if (this.getWorld() instanceof ServerWorld world) {
            ServerPlayerEntity player = getOwnerPlayer(world);
            if (player != null) return player;
            Entity entity = world.getEntity(this.ownerUuid);
            return entity instanceof LivingEntity living ? living : null;
        }
        return null;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("owner_uuid")) {
            this.ownerUuid = nbt.getUuid("owner_uuid");
        }
        this.expiresAtTick = nbt.getLong("expires_at_tick");
        this.weaponDamage = nbt.getFloat("weapon_damage");
        this.sourceWeaponSlot = nbt.contains("source_weapon_slot") ? nbt.getInt("source_weapon_slot") : -1;
        this.nextActiveAbilityCheckTick = nbt.getLong("next_active_ability_check_tick");
        this.activeAbilityCooldownUntilTick = nbt.getLong("active_ability_cooldown_until_tick");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        nbt.putLong("expires_at_tick", this.expiresAtTick);
        nbt.putFloat("weapon_damage", this.weaponDamage);
        nbt.putInt("source_weapon_slot", this.sourceWeaponSlot);
        nbt.putLong("next_active_ability_check_tick", this.nextActiveAbilityCheckTick);
        nbt.putLong("active_ability_cooldown_until_tick", this.activeAbilityCooldownUntilTick);
    }
}
