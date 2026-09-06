package net.sweenus.simplyswords.entity;

import com.google.common.base.Suppliers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryTuning;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.world.BattleStandardMasteryManager;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class BattleStandardDarkEntity extends PathAwareEntity {
    public static final Supplier<EntityType<BattleStandardDarkEntity>> TYPE = Suppliers.memoize(() ->
            EntityType.Builder.create(BattleStandardDarkEntity::new, SpawnGroup.MISC).build("battlestandarddark"));
    private static final TrackedData<String> TRACKED_STANDARD_TYPE = DataTracker.registerData(BattleStandardDarkEntity.class, TrackedDataHandlerRegistry.STRING);
    public LivingEntity ownerEntity;
    public String standardType;
    public int decayRate;
    public ItemStack abilityStack = ItemStack.EMPTY;
    private UniqueAbilityExecution masteryExecution;
    private ArcaneCosmicMasteryTuning enigmaTuning = ArcaneCosmicMasteryTuning.EMPTY;
    private ArcaneCosmicMasteryTuning enigmaVortexTuning = ArcaneCosmicMasteryTuning.EMPTY;
    private ArcaneCosmicMasteryTuning enigmaAuraTuning = ArcaneCosmicMasteryTuning.EMPTY;
    private boolean enigmaOwnerNear;
    private int enigmaRefunded;
    private final Map<UUID, EnigmaTornadoTarget> enigmaTornadoTargets = new HashMap<>();
    private final Map<UUID, Long> enigmaTornadoCooldowns = new HashMap<>();
    private final Map<UUID, Long> enigmaFlungVictims = new HashMap<>();
    private UUID enigmaChaseTarget;
    private long enigmaTargetPickedAt;
    private long enigmaVeerReadyAt;
    private long enigmaBonusUntil;
    private long enigmaCarryUntil;
    private boolean enigmaRideConsumed;
    private static final double ENIGMA_PULL_STRENGTH = 0.18;
    private static final double ENIGMA_ORBIT_RADIUS = 1.6;
    private static final double ENIGMA_ORBIT_SPEED = 0.38;
    private static final int ENIGMA_RECAPTURE_COOLDOWN_TICKS = 25;
    private static final int ENIGMA_LANDING_TIMEOUT_TICKS = 60;

    public static DefaultAttributeContainer.Builder createBattleStandardDarkAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 150.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0f)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 100.0f)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 3.0);
    }

    public BattleStandardDarkEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        setInvisible(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TRACKED_STANDARD_TYPE, "");
    }

    public String getStandardType() {
        String trackedType = this.dataTracker.get(TRACKED_STANDARD_TYPE);
        return trackedType == null || trackedType.isBlank() ? this.standardType : trackedType;
    }

    public void configureMastery(UniqueAbilityExecution execution, ItemStack stack) {
        this.masteryExecution = execution;
        this.abilityStack = stack.copy();
    }

    public void configureEnigmaMastery(ArcaneCosmicMasteryTuning tuning) {
        this.enigmaTuning = tuning == null ? ArcaneCosmicMasteryTuning.EMPTY : tuning;
    }

    public void configureEnigmaMastery(ArcaneCosmicMasteryTuning chase, ArcaneCosmicMasteryTuning vortex,
                                ArcaneCosmicMasteryTuning aura) {
        this.enigmaTuning = chase == null ? ArcaneCosmicMasteryTuning.EMPTY : chase;
        this.enigmaVortexTuning = vortex == null ? ArcaneCosmicMasteryTuning.EMPTY : vortex;
        this.enigmaAuraTuning = aura == null ? ArcaneCosmicMasteryTuning.EMPTY : aura;
    }

    @Override
    protected boolean isImmobile() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return ownerEntity == null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.getWorld().isClient()) BattleStandardMasteryManager.onStandardRemoved(this.getUuid());
        super.remove(reason);
    }

    @Override
    public void baseTick() {
        if (!this.getWorld().isClient()) {
            if (this.standardType != null && !this.standardType.equals(this.dataTracker.get(TRACKED_STANDARD_TYPE))) {
                this.dataTracker.set(TRACKED_STANDARD_TYPE, this.standardType);
            }
            if (this.age % 10 == 0) {
                this.setHealth(this.getHealth() - decayRate);
                if (ownerEntity == null)
                    this.setHealth(this.getHealth() - 1000);
                HelperMethods.spawnOrbitParticles((ServerWorld) this.getWorld(), this.getPos(), ParticleTypes.CAMPFIRE_COSY_SMOKE, 0.5, 6);
                if (ownerEntity != null && this.distanceTo(ownerEntity) < 3)
                    HelperMethods.incrementStatusEffect(ownerEntity, StatusEffects.HASTE, 60, 1, 7);
                if (ownerEntity != null && standardType != null && standardType.equals("enigma")) {
                    double proximity = enigmaRadius();
                    boolean near = this.distanceTo(ownerEntity) <= proximity;
                    if (near && enigmaAuraTuning.flag(1 << 19)) ownerEntity.addStatusEffect(
                            new StatusEffectInstance(StatusEffects.SPEED,
                                    enigmaAuraTuning.integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DURATION_TICKS, 20), 0), this);
                    if (near && this.distanceTo(ownerEntity)
                            <= enigmaAuraTuning.get(ArcaneCosmicMasteryTuning.Setting.RADIUS, 2) && enigmaAuraTuning.flag(1 << 22))
                        ownerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                                enigmaAuraTuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 20), 0), this);
                    if (!near && enigmaOwnerNear && enigmaAuraTuning.flag(1 << 18)) ownerEntity.addStatusEffect(
                            new StatusEffectInstance(StatusEffects.SPEED,
                                    enigmaAuraTuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 40), 0), this);
                    if (near && enigmaAuraTuning.flag(1 << 20)) ownerEntity.addStatusEffect(
                            new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 0, false, false, false), this);
                    if (near && enigmaAuraTuning.flag(1 << 26)) {
                        ownerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 60, 1), this);
                        ownerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0), this);
                    }
                    enigmaOwnerNear = near;
                }
            }
            if (ownerEntity != null && standardType != null) {
                if (standardType.equals("harbinger") && masteryExecution != null
                        && BattleStandardMasteryManager.tickHarbinger(this, masteryExecution, abilityStack)) {
                    super.baseTick();
                    return;
                }
                if (!ownerEntity.isAlive())
                    this.setHealth(this.getHealth() - 1000);
                int radius = 6;
                if (standardType.equals("enigma") && !this.isInvisible())
                    this.setInvisible(true);

                if (standardType.equals("enigma")) {
                    radius = 2;
                    if (ownerEntity != null && this.getWorld().getTime() <= enigmaCarryUntil) {
                        this.setVelocity(Vec3d.ZERO);
                        this.velocityModified = true;
                        this.refreshPositionAfterTeleport(ownerEntity.getX(), ownerEntity.getY(), ownerEntity.getZ());
                    } else {
                        LivingEntity chased = resolveEnigmaChaseTarget();
                        if (chased != null && !enigmaAuraTuning.flag(1 << 26)
                                && chased.distanceTo(this) > 1 && this.isOnGround())
                            this.setVelocity((chased.getX() - this.getX()) / 20
                                            * enigmaTuning.get(ArcaneCosmicMasteryTuning.Setting.SPEED, 1),
                                    0, (chased.getZ() - this.getZ()) / 20
                                            * enigmaTuning.get(ArcaneCosmicMasteryTuning.Setting.SPEED, 1));
                    }
                    tickEnigmaTornado((ServerWorld) this.getWorld());
                }

                ItemStack damageStack = abilityStack.isEmpty() ? ownerEntity.getMainHandStack() : abilityStack;
                float abilityDamage = standardType.equals("enigma")
                        ? enigmaAbilityDamage()
                        : HelperMethods.abilityScaledDamage(SpellScalingComponents.id("harbinger"), ownerEntity, damageStack,
                                Config.uniqueEffects.harbinger.damageScaling, Config.uniqueEffects.harbinger.spellScaling);
                Set<UUID> sharedCurrent = resolveSharedCurrentTargets();

                //AOE Aura
                if (this.age % 10 == 0) {
                    Box box = new Box(this.getX() + radius, this.getY() + (float) radius / 3, this.getZ() + radius,
                            this.getX() - radius, this.getY() - (float) radius / 3, this.getZ() - radius);
                    for (Entity entities : this.getWorld().getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((entities instanceof LivingEntity le) && HelperMethods.checkAbilityTarget(le, ownerEntity)
                                && le != ownerEntity && !(le instanceof BattleStandardEntity)
                                && !(le instanceof BattleStandardDarkEntity)) {
                            le.timeUntilRegen = 0;
                            DamageSource damageSource = this.getDamageSources().indirectMagic(ownerEntity, ownerEntity);
                            le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments((ServerWorld) getWorld(), damageStack, le, damageSource,
                                    enigmaTargetDamage(le, abilityDamage, sharedCurrent)));
                            le.timeUntilRegen = 0;
                            if (le.distanceTo(this) > radius - 1)
                                le.setVelocity((this.getX() - le.getX()) / 4, (this.getY() - le.getY()) / 4, (this.getZ() - le.getZ()) / 4);
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 0), this);
                            if (standardType.equals("enigma")) {
                                SimplySwordsStatusEffectInstance effect = HelperMethods.incrementSimplySwordsStatusEffect(
                                        le, EffectRegistry.getReference(EffectRegistry.PAIN), 60, 1, 49);
                                effect.setSourceEntity(ownerEntity);
                                effect.setAdditionalData(0);
                                le.addStatusEffect(effect);
                            }
                        }
                    }
                    if (!standardType.equals("enigma"))
                        HelperMethods.spawnParticle(this.getWorld(), ParticleTypes.SCULK_SOUL, this.getX(), this.getY(), this.getZ(),
                            0, 0, 0);
                }
                //Landing effects
                if (this.getHealth() > this.getMaxHealth() - 2 && this.isOnGround()) {

                    if (!standardType.equals("enigma")) {
                        HelperMethods.spawnParticle(this.getWorld(), ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(),
                                0, 0.3, 0);
                        HelperMethods.spawnParticle(this.getWorld(), ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY(), this.getZ(),
                                0, 0, 0);
                    }

                    //Launch nearby entities on land
                    Box box = new Box(this.getX() + 1, this.getY() + 1, this.getZ() + 1,
                            this.getX() - 1, this.getY() - (float) 1, this.getZ() - 1);
                    for (Entity entity : this.getWorld().getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((entity instanceof LivingEntity le) && HelperMethods.checkAbilityTarget(le, ownerEntity) && le != ownerEntity) {
                            DamageSource damageSource = this.getDamageSources().indirectMagic(ownerEntity, ownerEntity);
                            le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments((ServerWorld) getWorld(), ownerEntity.getMainHandStack(), le, damageSource, abilityDamage * 3));
                            le.setVelocity((le.getX() - this.getX()) / 4, 0.5, (le.getZ() - this.getZ()) / 4);
                        }
                    }
                }
                if (this.age % 80 == 0 &&  (!standardType.equals("enigma"))) {
                    //AOE Heal
                    Box box = new Box(this.getX() + radius, this.getY() + (float) radius / 3, this.getZ() + radius,
                            this.getX() - radius, this.getY() - (float) radius / 3, this.getZ() - radius);
                    for (Entity entity : this.getWorld().getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((entity instanceof LivingEntity le) && !HelperMethods.checkFriendlyFire(le, ownerEntity)) {
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 90, 2), this);
                        }
                    }

                    this.getWorld().playSoundFromEntity(null, this, SoundRegistry.DARK_SWORD_WHOOSH_01.get(),
                            this.getSoundCategory(), 0.1f, 0.6f);
                    double xpos = this.getX() - (radius + 1);
                    double ypos = this.getY();
                    double zpos = this.getZ() - (radius + 1);

                    for (int i = radius * 2; i > 0; i--) {
                        for (int j = radius * 2; j > 0; j--) {
                            float choose = (float) (Math.random() * 1);
                            if (choose > 0.5)
                                HelperMethods.spawnParticle(this.getWorld(), ParticleTypes.SOUL,
                                        xpos + i + choose, ypos + 0.1, zpos + j + choose,
                                        0, -0.1, 0);
                        }
                    }
                }
            }
        }
        super.baseTick();
    }

    private void tickEnigmaTornado(ServerWorld world) {
        if (ownerEntity == null || !ownerEntity.isAlive()) {
            enigmaTornadoTargets.clear();
            enigmaTornadoCooldowns.clear();
            return;
        }

        long now = world.getTime();
        enigmaTornadoCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
        double radius = Math.max(1.0, enigmaRadius());
        Box box = new Box(this.getX() + radius, this.getY() + radius, this.getZ() + radius,
                this.getX() - radius, this.getY() - radius, this.getZ() - radius);
        int targetCap = enigmaVortexTuning.integer(ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, 64);
        for (Entity entity : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (enigmaTornadoTargets.size() >= targetCap) break;
            if (!(entity instanceof LivingEntity target)
                    || target == ownerEntity
                    || target instanceof BattleStandardEntity
                    || target instanceof BattleStandardDarkEntity
                    || !HelperMethods.checkAbilityTarget(target, ownerEntity)
                    || enigmaTornadoCooldowns.containsKey(target.getUuid())) {
                continue;
            }
            enigmaTornadoTargets.computeIfAbsent(target.getUuid(), ignored -> {
                if (enigmaTuning.flag(1 << 5)) target.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.SLOWNESS,
                                enigmaTuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 30),
                                enigmaTuning.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_AMPLIFIER, 1)), ownerEntity);
                return new EnigmaTornadoTarget(now, target.getY() + target.getHeight() * 0.5);
            });
        }

        Iterator<Map.Entry<UUID, EnigmaTornadoTarget>> iterator = enigmaTornadoTargets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, EnigmaTornadoTarget> entry = iterator.next();
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target)
                    || !target.isAlive()
                    || !HelperMethods.checkAbilityTarget(target, ownerEntity)
                    || target.squaredDistanceTo(this) > radius * radius * 2.25) {
                iterator.remove();
                continue;
            }

            EnigmaTornadoTarget state = entry.getValue();
            long age = now - state.startTick();
            if (!enigmaVortexTuning.flag(1 << 16)
                    && age >= Math.max(1, enigmaVortexTuning.integer(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS,
                    Config.uniqueEffects.enigma.enigmaOrbitTicks))) {
                flingEnigmaTarget(target);
                enigmaTornadoCooldowns.put(target.getUuid(), now + ENIGMA_RECAPTURE_COOLDOWN_TICKS);
                iterator.remove();
                continue;
            }
            orbitEnigmaTarget(target, state, age);
        }
        tickEnigmaFlungVictims(world, now);
    }

    private void tickEnigmaFlungVictims(ServerWorld world, long now) {
        if (enigmaFlungVictims.isEmpty()) return;
        Iterator<Map.Entry<UUID, Long>> flung = enigmaFlungVictims.entrySet().iterator();
        while (flung.hasNext()) {
            Map.Entry<UUID, Long> entry = flung.next();
            boolean expired = now >= entry.getValue();
            if (!(world.getEntity(entry.getKey()) instanceof LivingEntity victim) || !victim.isAlive()) {
                flung.remove();
                continue;
            }
            if (!victim.isOnGround() && !expired) continue;
            flung.remove();
            enigmaLandingPulse(world, victim);
        }
    }

    private void enigmaLandingPulse(ServerWorld world, LivingEntity victim) {
        if (ownerEntity == null) return;
        float damage = enigmaAbilityDamage()
                * (float) enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1)
                * (float) enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .4);
        double pulseRadius = enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.SECONDARY_RADIUS, 2);
        int cap = enigmaVortexTuning.integer(ArcaneCosmicMasteryTuning.Setting.TERTIARY_TARGET_CAP, 4);
        if (damage <= 0 || pulseRadius <= 0 || cap <= 0) return;
        DamageSource source = this.getDamageSources().indirectMagic(ownerEntity, ownerEntity);
        world.getEntitiesByClass(LivingEntity.class, victim.getBoundingBox().expand(pulseRadius),
                        other -> other != ownerEntity && !(other instanceof BattleStandardEntity)
                                && !(other instanceof BattleStandardDarkEntity)
                                && HelperMethods.checkAbilityTarget(other, ownerEntity))
                .stream().sorted(Comparator.comparingDouble(victim::squaredDistanceTo)).limit(cap)
                .forEach(other -> HelperMethods.damageThroughIframes(other, source, damage));
        world.spawnParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 0.2, victim.getZ(),
                12, 0.4, 0.1, 0.4, 0.05);
    }

    private double enigmaRadius() {
        double value = enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.RADIUS,
                Config.uniqueEffects.enigma.enigmaTornadoRadius);
        if (enigmaAuraTuning.flag(1 << 26)) value *= enigmaAuraTuning.get(
                ArcaneCosmicMasteryTuning.Setting.TERTIARY_DAMAGE_MULTIPLIER, .8);
        return value;
    }

    private float enigmaAbilityDamage() {
        if (ownerEntity == null) return 0.0F;
        ItemStack damageStack = abilityStack.isEmpty() ? ownerEntity.getMainHandStack() : abilityStack;
        float damage = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("enigma"), ownerEntity, damageStack,
                Config.uniqueEffects.enigma.damageScaling, Config.uniqueEffects.enigma.spellScaling)
                * (float) enigmaTuning.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                * (float) enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
        if (enigmaTuning.flag(1 << 2) && this.getWorld().getTime() <= enigmaBonusUntil)
            damage *= (float) enigmaTuning.get(ArcaneCosmicMasteryTuning.Setting.OUTGOING_MULTIPLIER, 1.1);
        return damage;
    }

    private Set<UUID> resolveSharedCurrentTargets() {
        if (!standardType.equals("enigma") || !enigmaVortexTuning.flag(1 << 14)
                || !(this.getWorld() instanceof ServerWorld world)
                || enigmaTornadoTargets.size() < Math.max(1, enigmaVortexTuning.integer(ArcaneCosmicMasteryTuning.Setting.COUNT, 2))) {
            return Set.of();
        }
        return enigmaTornadoTargets.keySet().stream()
                .map(world::getEntity)
                .filter(entity -> entity instanceof LivingEntity)
                .sorted(Comparator.comparingDouble(this::squaredDistanceTo))
                .limit(Math.max(1, enigmaVortexTuning.integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_TARGET_CAP, 6)))
                .map(Entity::getUuid)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    private float enigmaTargetDamage(LivingEntity target, float base, Set<UUID> sharedCurrent) {
        if (!standardType.equals("enigma")) return base;
        float damage = base;
        EnigmaTornadoTarget captured = enigmaTornadoTargets.get(target.getUuid());
        if (captured != null && enigmaVortexTuning.flag(1 << 13)) {
            long cycle = Math.max(1, enigmaVortexTuning.integer(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS,
                    Config.uniqueEffects.enigma.enigmaOrbitTicks));
            long cycles = Math.max(0, (this.getWorld().getTime() - captured.startTick()) / cycle);
            damage *= 1 + Math.min(enigmaVortexTuning.integer(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, 6), cycles)
                    * (float) enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.PER_STACK_MULTIPLIER, .04);
        }
        if (sharedCurrent.contains(target.getUuid()))
            damage *= (float) enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.OUTGOING_MULTIPLIER, 1.1);
        return damage;
    }

    private LivingEntity resolveEnigmaChaseTarget() {
        if (!(this.getWorld() instanceof ServerWorld world)) return null;
        long now = world.getTime();
        double moveRadius = enigmaTuning.get(ArcaneCosmicMasteryTuning.Setting.RANGE,
                Config.uniqueEffects.enigma.enigmaChaseRadius);
        LivingEntity held = null;
        if (enigmaChaseTarget != null) {
            LivingEntity previous = world.getEntity(enigmaChaseTarget) instanceof LivingEntity found
                    ? found : null;
            boolean gone = previous == null || !previous.isAlive();
            if (!gone && HelperMethods.checkAbilityTarget(previous, ownerEntity)
                    && previous.distanceTo(this) <= moveRadius) {
                held = previous;
            } else {
                enigmaChaseTarget = null;
                if (gone && enigmaTuning.flag(1 << 2)) enigmaBonusUntil = now
                        + Math.max(1, enigmaTuning.integer(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, 40));
            }
        }
        boolean rotate = held != null && enigmaTuning.flag(1 << 8) && now - enigmaTargetPickedAt
                >= Math.max(1, enigmaTuning.integer(ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, 30));
        boolean veer = held != null && enigmaTuning.flag(1 << 4) && now >= enigmaVeerReadyAt;
        if (held != null && !rotate && !veer) return held;

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity entity : world.getOtherEntities(this, HelperMethods.createBox(this, moveRadius),
                EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(entity instanceof LivingEntity living)
                    || living instanceof BattleStandardEntity
                    || living instanceof BattleStandardDarkEntity
                    || !HelperMethods.checkAbilityTarget(living, ownerEntity)
                    || (rotate && living == held)) {
                continue;
            }
            double score = enigmaTuning.flag(1 << 3)
                    && living.getHealth() < living.getMaxHealth() * .5F
                    ? living.squaredDistanceTo(this) - moveRadius * moveRadius
                    : living.squaredDistanceTo(this);
            if (score < bestScore) {
                bestScore = score;
                best = living;
            }
        }
        if (best == null) {
            enigmaTargetPickedAt = now;
            return held;
        }
        if (held != null && !rotate) {
            if (best.distanceTo(this) > held.distanceTo(this)
                    - enigmaTuning.get(ArcaneCosmicMasteryTuning.Setting.SECONDARY_RADIUS, 6)) {
                return held;
            }
            enigmaVeerReadyAt = now + Math.max(1,
                    enigmaTuning.integer(ArcaneCosmicMasteryTuning.Setting.LOCKOUT_TICKS, 20));
        }
        enigmaChaseTarget = best.getUuid();
        enigmaTargetPickedAt = now;
        return best;
    }

    public static boolean tryRideTheGale(ServerWorld world, PlayerEntity user, ItemStack stack) {
        if (world == null || user == null || stack == null || stack.isEmpty() || !user.isSneaking()) return false;
        for (BattleStandardDarkEntity standard : world.getEntitiesByClass(BattleStandardDarkEntity.class,
                user.getBoundingBox().expand(Config.uniqueEffects.enigma.enigmaChaseRadius * 2),
                candidate -> candidate.ownerEntity == user && "enigma".equals(candidate.getStandardType()))) {
            if (standard.beginGaleRide(user)) return true;
        }
        return false;
    }

    private boolean beginGaleRide(PlayerEntity user) {
        if (enigmaRideConsumed || !enigmaAuraTuning.flag(1 << 25)) return false;
        enigmaRideConsumed = true;
        enigmaCarryUntil = this.getWorld().getTime() + Math.max(1,
                enigmaAuraTuning.integer(ArcaneCosmicMasteryTuning.Setting.TERTIARY_DURATION_TICKS, 60));
        user.requestTeleport(this.getX(), this.getY(), this.getZ());
        user.fallDistance = 0.0F;
        this.getWorld().playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_WHOOSH_02.get(),
                user.getSoundCategory(), 0.45F, 1.4F);
        return true;
    }

    private void orbitEnigmaTarget(LivingEntity target, EnigmaTornadoTarget state, long age) {
        Vec3d center = this.getPos().add(0.0, 1.15, 0.0);
        Vec3d toCenter = center.subtract(target.getPos());
        double pullScale = Math.min(1.0, toCenter.length() / Math.max(0.1, enigmaRadius()));
        double angle = state.initialAngle() + age * ENIGMA_ORBIT_SPEED;
        double heightOffset = Math.sin(age * 0.24) * 0.55;
        Vec3d orbitPoint = new Vec3d(
                center.x + Math.cos(angle) * ENIGMA_ORBIT_RADIUS,
                state.anchorY() + heightOffset,
                center.z + Math.sin(angle) * ENIGMA_ORBIT_RADIUS
        );
        Vec3d desiredVelocity = orbitPoint.subtract(target.getPos()).multiply(0.32)
                .add(toCenter.normalize().multiply(ENIGMA_PULL_STRENGTH * pullScale
                        * enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.PULL_STRENGTH, 1)));
        target.setVelocity(desiredVelocity.x, Math.clamp(desiredVelocity.y, -0.35, 0.55), desiredVelocity.z);
        target.velocityModified = true;
        target.fallDistance = 0.0F;
    }

    private void flingEnigmaTarget(LivingEntity target) {
        Vec3d away = target.getPos().subtract(this.getPos());
        double horizontalLength = Math.sqrt(away.x * away.x + away.z * away.z);
        Vec3d horizontal = horizontalLength < 0.001
                ? new Vec3d(1.0, 0.0, 0.0)
                : new Vec3d(away.x / horizontalLength, 0.0, away.z / horizontalLength);
        target.setVelocity(horizontal.multiply(Config.uniqueEffects.enigma.enigmaFlingStrength)
                .add(0.0, Config.uniqueEffects.enigma.enigmaFlingUpwardStrength
                        * enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.KNOCKBACK, 1), 0.0));
        target.velocityModified = true;
        target.fallDistance = 0.0F;
        float flingDamage = enigmaAbilityDamage()
                * (float) enigmaVortexTuning.get(ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1);
        if (flingDamage > 0 && ownerEntity != null) HelperMethods.damageThroughIframes(target,
                this.getDamageSources().indirectMagic(ownerEntity, ownerEntity), flingDamage);
        if (enigmaVortexTuning.flag(1 << 15)) enigmaFlungVictims.put(target.getUuid(),
                this.getWorld().getTime() + ENIGMA_LANDING_TIMEOUT_TICKS);
        if (enigmaAuraTuning.flag(1 << 23) && ownerEntity != null
                && enigmaRefunded < enigmaAuraTuning.integer(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, 48)) {
            int refund = Math.min(enigmaAuraTuning.integer(ArcaneCosmicMasteryTuning.Setting.REFUND_TICKS, 8),
                    enigmaAuraTuning.integer(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, 48) - enigmaRefunded);
            enigmaRefunded += refund;
            SimplySwordsAPI.reduceWeaponCooldown(ownerEntity, abilityStack,
                    Config.uniqueEffects.enigma.enigmaCooldown, refund);
        }
        this.getWorld().playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_WHOOSH_02.get(),
                target.getSoundCategory(), 0.16F, 0.55F + this.getRandom().nextFloat() * 0.25F);
    }

    private static final class EnigmaTornadoTarget {
        private final long startTick;
        private final double anchorY;
        private final double initialAngle;

        private EnigmaTornadoTarget(long startTick, double anchorY) {
            this.startTick = startTick;
            this.anchorY = anchorY;
            this.initialAngle = Math.random() * Math.PI * 2.0;
        }

        private long startTick() {
            return this.startTick;
        }

        private double anchorY() {
            return this.anchorY;
        }

        private double initialAngle() {
            return this.initialAngle;
        }
    }
}
