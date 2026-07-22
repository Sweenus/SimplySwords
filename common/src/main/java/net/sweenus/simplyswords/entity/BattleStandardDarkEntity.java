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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class BattleStandardDarkEntity extends PathAwareEntity {
    public static final Supplier<EntityType<BattleStandardDarkEntity>> TYPE = Suppliers.memoize(() ->
            EntityType.Builder.create(BattleStandardDarkEntity::new, SpawnGroup.MISC).build("battlestandarddark"));
    private static final TrackedData<String> TRACKED_STANDARD_TYPE = DataTracker.registerData(BattleStandardDarkEntity.class, TrackedDataHandlerRegistry.STRING);
    public LivingEntity ownerEntity;
    public String standardType;
    public int decayRate;
    private final Map<UUID, EnigmaTornadoTarget> enigmaTornadoTargets = new HashMap<>();
    private final Map<UUID, Long> enigmaTornadoCooldowns = new HashMap<>();
    private static final double ENIGMA_PULL_STRENGTH = 0.18;
    private static final double ENIGMA_ORBIT_RADIUS = 1.6;
    private static final double ENIGMA_ORBIT_SPEED = 0.38;
    private static final int ENIGMA_RECAPTURE_COOLDOWN_TICKS = 25;

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
            }
            if (ownerEntity != null && standardType != null) {
                if (!ownerEntity.isAlive())
                    this.setHealth(this.getHealth() - 1000);
                int radius = 6;
                if (standardType.equals("enigma") && !this.isInvisible())
                    this.setInvisible(true);

                if (standardType.equals("enigma")) {
                    radius = 2;
                    double moveRadius = Config.uniqueEffects.enigma.enigmaChaseRadius;
                    Box box = HelperMethods.createBox(this, moveRadius);
                    Entity closestEntity = this.getWorld().getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                            .filter(entity -> {
                                if (entity instanceof LivingEntity livingEntity)
                                    return HelperMethods.checkAbilityTarget(livingEntity, ownerEntity);
                                return false;
                            })
                            .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(this)))
                            .orElse(null);

                    if (closestEntity != null) {
                        if ((closestEntity instanceof LivingEntity le)) {
                            if (!(le instanceof BattleStandardEntity) && !(le instanceof BattleStandardDarkEntity)) {
                                if (le.distanceTo(this) > 1 && this.isOnGround())
                                    this.setVelocity((le.getX() - this.getX()) / 20, 0, (le.getZ() - this.getZ()) / 20);
                            }
                        }
                    }
                    tickEnigmaTornado((ServerWorld) this.getWorld());
                }

                float abilityDamage = standardType.equals("enigma") ? 1f : HelperMethods.abilityScaledDamage("soul", ownerEntity, ownerEntity.getMainHandStack(),
                        Config.uniqueEffects.harbinger.damageScaling, Config.uniqueEffects.harbinger.spellScaling);

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
                            le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments((ServerWorld) getWorld(), ownerEntity.getMainHandStack(), le, damageSource, abilityDamage));
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
        double radius = Math.max(1.0, Config.uniqueEffects.enigma.enigmaTornadoRadius);
        Box box = new Box(this.getX() + radius, this.getY() + radius, this.getZ() + radius,
                this.getX() - radius, this.getY() - radius, this.getZ() - radius);
        for (Entity entity : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(entity instanceof LivingEntity target)
                    || target == ownerEntity
                    || target instanceof BattleStandardEntity
                    || target instanceof BattleStandardDarkEntity
                    || !HelperMethods.checkAbilityTarget(target, ownerEntity)
                    || enigmaTornadoCooldowns.containsKey(target.getUuid())) {
                continue;
            }
            enigmaTornadoTargets.computeIfAbsent(target.getUuid(), ignored -> new EnigmaTornadoTarget(now, target.getY() + target.getHeight() * 0.5));
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
            if (age >= Math.max(1, Config.uniqueEffects.enigma.enigmaOrbitTicks)) {
                flingEnigmaTarget(target);
                enigmaTornadoCooldowns.put(target.getUuid(), now + ENIGMA_RECAPTURE_COOLDOWN_TICKS);
                iterator.remove();
                continue;
            }
            orbitEnigmaTarget(target, state, age);
        }
    }

    private void orbitEnigmaTarget(LivingEntity target, EnigmaTornadoTarget state, long age) {
        Vec3d center = this.getPos().add(0.0, 1.15, 0.0);
        Vec3d toCenter = center.subtract(target.getPos());
        double pullScale = Math.min(1.0, toCenter.length() / Math.max(0.1, Config.uniqueEffects.enigma.enigmaTornadoRadius));
        double angle = state.initialAngle() + age * ENIGMA_ORBIT_SPEED;
        double heightOffset = Math.sin(age * 0.24) * 0.55;
        Vec3d orbitPoint = new Vec3d(
                center.x + Math.cos(angle) * ENIGMA_ORBIT_RADIUS,
                state.anchorY() + heightOffset,
                center.z + Math.sin(angle) * ENIGMA_ORBIT_RADIUS
        );
        Vec3d desiredVelocity = orbitPoint.subtract(target.getPos()).multiply(0.32)
                .add(toCenter.normalize().multiply(ENIGMA_PULL_STRENGTH * pullScale));
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
                .add(0.0, Config.uniqueEffects.enigma.enigmaFlingUpwardStrength, 0.0));
        target.velocityModified = true;
        target.fallDistance = 0.0F;
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
