package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.sweenus.simplyswords.entity.goal.MinionTargetPriorityGoal;
import net.sweenus.simplyswords.util.MinionTargeting;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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

public class SimplySwordsSkeletonMinionEntity extends SkeletonEntity implements Tameable {

    private static final double OWNER_FOLLOW_DISTANCE = 14.0;
    private static final double HEALTH_PER_ATTACK = 8.0;
    private static final double OWNER_TELEPORT_DISTANCE = 32.0;
    private static final float ARMOR_SLOT_CHANCE = 0.55F;
    private static final Item[] HELMETS = {Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.GOLDEN_HELMET, Items.IRON_HELMET};
    private static final Item[] CHESTPLATES = {Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE};
    private static final Item[] LEGGINGS = {Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.IRON_LEGGINGS};
    private static final Item[] BOOTS = {Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.GOLDEN_BOOTS, Items.IRON_BOOTS};
    private UUID ownerUuid;
    private long expiresAtTick;
    private float weaponDamage;
    private int sourceWeaponSlot = -1;
    private long nextActiveAbilityCheckTick;
    private long activeAbilityCooldownUntilTick;

    public SimplySwordsSkeletonMinionEntity(EntityType<? extends SkeletonEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setCanPickUpLoot(false);
    }

    public static DefaultAttributeContainer.Builder createMinionAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.85));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new MinionTargetPriorityGoal(this, world -> getOwnerPlayer(world), this::isValidMinionTarget, 16.0, 20));
    }

    public void initializeMinion(ServerPlayerEntity owner, ItemStack stack, int sourceWeaponSlot, long expiresAtTick, float weaponDamage) {
        this.ownerUuid = owner.getUuid();
        this.expiresAtTick = expiresAtTick;
        this.weaponDamage = Math.max(0.0F, weaponDamage);
        this.sourceWeaponSlot = sourceWeaponSlot;
        this.equipStack(EquipmentSlot.MAINHAND, stack.copy());
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        EntityAttributeInstance health = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null) {
            double scaled = health.getBaseValue() + weaponDamage * HEALTH_PER_ATTACK;
            health.setBaseValue(scaled);
            this.setHealth((float) scaled);
        }
        equipRandomArmor();
        this.setPersistent();
        this.setCustomName(owner.getName().copy().append("'s Minion"));
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

        ServerPlayerEntity owner = getOwnerPlayer(world);
        if (owner == null || !owner.isAlive()) {
            expire();
            return;
        }

        followOwner(owner);
        tryActivateWeaponAbility(world, owner);
        if (this.age % MinionTargeting.tauntIntervalTicks() == 0) {
            MinionTargeting.tauntNearbyEnemies(world, this, owner, this::isValidMinionTarget);
        }
        if (world.getTime() % 12L == 0L) {
            world.spawnParticles(ParticleTypes.SOUL, this.getX(), this.getBodyY(0.65), this.getZ(), 2, 0.18, 0.22, 0.18, 0.01);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (!(target instanceof LivingEntity livingTarget) || !(this.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        ServerPlayerEntity owner = getOwnerPlayer(world);
        ItemStack visualStack = this.getMainHandStack();
        ItemStack effectStack = resolveEffectWeaponStack(owner, visualStack);
        if (owner == null || visualStack.isEmpty() || effectStack.isEmpty() || !isValidMinionTarget(livingTarget)) {
            return false;
        }

        float damage = (float) (this.weaponDamage * Config.gemPowers.necromanticArsenal.damageScaling);
        if (damage <= 0.0F) {
            return false;
        }

        this.swingHand(Hand.MAIN_HAND);
        boolean damaged = SimplySwordsAPI.applyDelegatedWeaponHit(effectStack, livingTarget, owner, this, damage);
        if (damaged) {
            Vec3d hitPos = livingTarget.getPos().add(0.0, Math.max(0.35, livingTarget.getHeight() * 0.55), 0.0);
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, hitPos.x, hitPos.y, hitPos.z, 5, 0.18, 0.12, 0.18, 0.025);
            world.playSound(null, livingTarget.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.28F, 0.75F + world.random.nextFloat() * 0.18F);
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

    public boolean trySetRetaliationTarget(LivingEntity attacker) {
        if (!isValidMinionTarget(attacker)) {
            return false;
        }
        this.setTarget(attacker);
        this.navigation.startMovingTo(attacker, 1.3);
        return true;
    }

    private void equipRandomArmor() {
        equipRandomArmorPiece(EquipmentSlot.HEAD, HELMETS);
        equipRandomArmorPiece(EquipmentSlot.CHEST, CHESTPLATES);
        equipRandomArmorPiece(EquipmentSlot.LEGS, LEGGINGS);
        equipRandomArmorPiece(EquipmentSlot.FEET, BOOTS);
    }

    private void equipRandomArmorPiece(EquipmentSlot slot, Item[] options) {
        if (this.random.nextFloat() > ARMOR_SLOT_CHANCE || options.length == 0) {
            return;
        }
        Item item = options[this.random.nextInt(options.length)];
        this.equipStack(slot, new ItemStack(item));
        this.setEquipmentDropChance(slot, 0.0F);
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

    private void tryActivateWeaponAbility(ServerWorld world, ServerPlayerEntity owner) {
        LivingEntity target = this.getTarget();
        int interval = Math.max(1, Config.gemPowers.necromanticArsenal.activeAbilityCheckInterval);
        long now = world.getTime();
        if (this.nextActiveAbilityCheckTick <= 0L) {
            this.nextActiveAbilityCheckTick = now + Math.max(0, Config.gemPowers.necromanticArsenal.activeAbilityInitialDelay);
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
        ItemStack effectStack = resolveEffectWeaponStack(owner, visualStack);
        WeaponAbilityContext context = WeaponAbilityContext.of(world, effectStack, this, owner, target, Hand.MAIN_HAND, WeaponAbilityActivationSource.MINION);
        if (!SimplySwordsAPI.canActivateWeaponAbility(context)) {
            return;
        }
        int chance = Math.clamp(Config.gemPowers.necromanticArsenal.activeAbilityChance, 0, 100);
        if (chance <= 0 || this.random.nextInt(100) >= chance) {
            return;
        }

        if (SimplySwordsAPI.tryActivateWeaponAbility(context)) {
            this.swingHand(Hand.MAIN_HAND);
            this.activeAbilityCooldownUntilTick = now + Math.max(1, SimplySwordsAPI.getWeaponAbilityCooldownTicks(context));
            Vec3d pos = this.getPos().add(0.0, this.getHeight() * 0.7, 0.0);
            world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 8, 0.25, 0.25, 0.25, 0.035);
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
        ServerPlayerEntity owner = getOwnerPlayer(world);
        return owner != null && target != owner && HelperMethods.checkFriendlyFire(target, owner);
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

    private void expire() {
        if (this.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.POOF, this.getX(), this.getBodyY(0.5), this.getZ(), 12, 0.35, 0.35, 0.35, 0.04);
            world.spawnParticles(ParticleTypes.SOUL, this.getX(), this.getBodyY(0.7), this.getZ(), 8, 0.28, 0.28, 0.28, 0.035);
        }
        this.discard();
    }

    @Nullable
    @Override
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    protected void dropEquipment(ServerWorld world, net.minecraft.entity.damage.DamageSource source, boolean causedByPlayer) {
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
