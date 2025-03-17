package net.sweenus.simplyswords.entity;

import com.google.common.base.Suppliers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.function.Supplier;

public class BattleStandardDarkEntity extends PathAwareEntity {
    public static final Supplier<EntityType<BattleStandardDarkEntity>> TYPE = Suppliers.memoize(() ->
            EntityType.Builder.create(BattleStandardDarkEntity::new, SpawnGroup.MISC).build("battlestandarddark"));
    public PlayerEntity ownerEntity;
    public String standardType;
    public int decayRate;

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
                                    return HelperMethods.checkFriendlyFire(livingEntity, ownerEntity);
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
                }

                float abilityDamage = standardType.equals("enigma") ? 1f : HelperMethods.spellScaledDamage("soul", ownerEntity, Config.uniqueEffects.harbinger.spellScaling, Config.uniqueEffects.harbinger.damage);

                //AOE Aura
                if (this.age % 10 == 0) {
                    Box box = new Box(this.getX() + radius, this.getY() + (float) radius / 3, this.getZ() + radius,
                            this.getX() - radius, this.getY() - (float) radius / 3, this.getZ() - radius);
                    for (Entity entities : this.getWorld().getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, ownerEntity)
                                && le != ownerEntity && !(le instanceof BattleStandardEntity)
                                && !(le instanceof BattleStandardDarkEntity)) {
                            le.timeUntilRegen = 0;
                            le.damage(this.getDamageSources().indirectMagic(ownerEntity, ownerEntity), abilityDamage);
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
                        if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, ownerEntity) && le != ownerEntity) {
                            le.damage(this.getDamageSources().indirectMagic(ownerEntity, ownerEntity), abilityDamage * 3);
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
}